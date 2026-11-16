package net.vyroxes.minersprosperity.machine.recipe;

import com.google.gson.*;
import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.oredict.OreDictionary;
import net.vyroxes.minersprosperity.MinersProsperity;
import net.vyroxes.minersprosperity.Tags;

@Mod.EventBusSubscriber(modid = Tags.MODID)
public final class RecipeFiles
{
    private static final Map<String, Integer> errors = new LinkedHashMap<>();
    private static final List<TextComponentTranslation> updates = new ArrayList<>();

    private RecipeFiles()
    {
    }

    static String oreDictionaryName(JsonObject input)
    {
        JsonElement value = input.get("oreDict");
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString())
            throw new JsonParseException("oreDict must be a string containing a registered Ore Dictionary name, not " + value);
        String name = value.getAsString();
        if (name.trim().isEmpty() || !OreDictionary.doesOreNameExist(name) || OreDictionary.getOres(name, false).isEmpty())
            throw new JsonParseException("Unknown or empty Ore Dictionary entry: " + name);
        return name;
    }

    @FunctionalInterface
    interface DefaultWriter
    {
        void write(File file) throws IOException;
    }

    static JsonObject document(String version, JsonArray recipes)
    {
        JsonObject document = new JsonObject();
        document.addProperty("version", version);
        document.add("recipes", recipes);
        return document;
    }

    static JsonArray read(File file, String version, DefaultWriter defaults) throws IOException
    {
        JsonElement current = file.exists() ? parse(file) : null;
        String previous = current == null ? "0" : version(current);
        int comparison = compareVersions(previous, version);
        if (current != null && comparison > 0)
            throw new IOException("Recipe version " + previous + " is newer than supported " + version + "; file left unchanged");
        if (current != null && comparison == 0) return recipes(current);

        Path target = file.toPath();
        Files.createDirectories(target.toAbsolutePath().getParent());
        Path temporary = Files.createTempFile(target.toAbsolutePath().getParent(), file.getName() + ".", ".tmp");
        Path backup = null;
        try
        {
            defaults.write(temporary.toFile());
            JsonElement replacement = parse(temporary.toFile());
            if (!version.equals(version(replacement))) throw new IOException("Default recipe file has an incorrect version");
            JsonArray result = recipes(replacement);
            if (current != null)
            {
                String base = file.getName() + ".v" + previous + ".bak";
                backup = target.resolveSibling(base);
                int suffix = 1;
                while (Files.exists(backup)) backup = target.resolveSibling(base + "." + suffix++);
                Files.copy(target, backup);
            }
            try { Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ignored) { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING); }
            if (backup != null)
            {
                MinersProsperity.logger.warn("Updated recipe file {} from version {} to {}. Backup: {}. New defaults installed; custom recipes can be copied from the backup.", file, previous, version, backup);
                updates.add(new TextComponentTranslation("chat.minersprosperity.recipes.updated", file.getName(), previous, version, backup.getFileName().toString()));
            }
            else MinersProsperity.logger.info("Created default recipe file {} (version {})", file, version);
            return result;
        }
        finally { Files.deleteIfExists(temporary); }
    }

    private static JsonElement parse(File file) throws IOException
    {
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))
        {
            JsonElement root = new JsonParser().parse(reader);
            recipes(root);
            return root;
        }
    }

    private static JsonArray recipes(JsonElement root)
    {
        if (!root.isJsonObject() || !root.getAsJsonObject().has("recipes") || !root.getAsJsonObject().get("recipes").isJsonArray())
            throw new JsonParseException("Expected a versioned object with a recipes array");
        version(root);
        return root.getAsJsonObject().getAsJsonArray("recipes");
    }

    private static String version(JsonElement root)
    {
        JsonElement value = root.getAsJsonObject().get("version");
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString())
            throw new JsonParseException("Recipe version must be a string such as 1.0");
        String version = value.getAsString();
        if (!version.matches("[0-9]+(\\.[0-9]+)*")) throw new JsonParseException("Invalid recipe version: " + version);
        return version;
    }

    static int compareVersions(String first, String second)
    {
        String[] left = first.split("\\.");
        String[] right = second.split("\\.");
        for (int i = 0; i < Math.max(left.length, right.length); i++)
        {
            int comparison = new BigInteger(i < left.length ? left[i] : "0").compareTo(new BigInteger(i < right.length ? right[i] : "0"));
            if (comparison != 0) return comparison;
        }
        return 0;
    }

    static void error(File file, String message, Object... arguments)
    {
        MinersProsperity.logger.error(message, arguments);
        errors.merge(file.getName(), 1, Integer::sum);
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (event.player.world.isRemote) return;
        for (TextComponentTranslation update : updates)
            event.player.sendMessage(update.createCopy().setStyle(new net.minecraft.util.text.Style().setColor(TextFormatting.YELLOW)));
        errors.forEach((file, count) -> event.player.sendMessage(new TextComponentTranslation("chat.minersprosperity.recipes.errors", file, count)
                .setStyle(new net.minecraft.util.text.Style().setColor(TextFormatting.RED))));
    }
}