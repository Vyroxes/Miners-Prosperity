package net.vyroxes.minersprosperity.enchantment;

import com.github.bsideup.jabel.Desugar;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.*;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.oredict.OreDictionary;
import net.vyroxes.minersprosperity.MinersProsperity;
import net.vyroxes.minersprosperity.Tags;

public class QuickMiningLoader
{
    private static final List<String> TOOL_TYPES = Arrays.asList("pickaxe", "axe", "shovel", "hoe", "sword", "shears", "other");
    private static JsonObject jsonRules = defaultRules();
    private static volatile Map<String, Rules> rules = Collections.emptyMap();

    public static void loadFromJSON()
    {
        File file = new File(Loader.instance().getConfigDir(), Tags.MODID + "/quick_mining.json");
        load(file);
    }

    static void load(File file)
    {
        try
        {
            if (!file.exists()) write(file, defaultRules());
            JsonElement document;
            try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))
            {
                document = new JsonParser().parse(reader);
            }
            configure(document);
        }
        catch (IOException | RuntimeException error)
        {
            MinersProsperity.logger.error("Could not load Quick Mining configuration; using the last valid rules", error);
            compileRules();
        }
        try { writeDetectedTools(new File(file.getParentFile(), "quick_mining_detected_tools.json")); }
        catch (IOException | RuntimeException error) { MinersProsperity.logger.error("Could not write Quick Mining detected-tool report", error); }
    }

    static void configure(JsonElement document)
    {
        if (!document.isJsonObject()) throw new JsonParseException("Quick Mining configuration must contain tool category objects");
        JsonObject object = document.getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : object.entrySet())
        {
            if (!TOOL_TYPES.contains(entry.getKey())) throw new JsonParseException("Unknown Quick Mining tool type: " + entry.getKey());
            if (!entry.getValue().isJsonObject()) throw new JsonParseException("Expected an object for tool type " + entry.getKey());
            JsonObject group = entry.getValue().getAsJsonObject();
            for (String field : Arrays.asList("blocks", "excludedBlocks", "tools", "excludedTools"))
                strings(group, field);
            for (String field : Arrays.asList("enabled", "autoDetectTools"))
                if (group.has(field) && (!group.get(field).isJsonPrimitive() || !group.get(field).getAsJsonPrimitive().isBoolean()))
                    throw new JsonParseException(entry.getKey() + "." + field + " must be true or false");
        }
        jsonRules = object;
        compileRules();
    }

    private static void compileRules()
    {
        Map<String, Rules> loaded = new LinkedHashMap<>();
        for (String type : TOOL_TYPES)
        {
            JsonObject group = jsonRules.has(type) ? jsonRules.getAsJsonObject(type) : new JsonObject();
            Rules rule = new Rules();
            rule.enabled = !group.has("enabled") || group.get("enabled").getAsBoolean();
            rule.autoDetect = !group.has("autoDetectTools") || group.get("autoDetectTools").getAsBoolean();
            for (String entry : strings(group, "blocks")) addBlock(rule.blocks, rule.ores, entry);
            for (String entry : strings(group, "excludedBlocks")) addBlock(rule.excludedBlocks, rule.excludedOres, entry);
            for (String entry : strings(group, "tools")) addTool(rule.tools, entry);
            for (String entry : strings(group, "excludedTools")) addTool(rule.excludedTools, entry);
            loaded.put(type, rule);
        }
        rules = Collections.unmodifiableMap(loaded);
    }

    private static List<String> strings(JsonObject group, String field)
    {
        List<String> result = new ArrayList<>();
        if (!group.has(field)) return result;
        if (!group.get(field).isJsonArray()) throw new JsonParseException(field + " must be an array of strings");
        for (JsonElement value : group.getAsJsonArray(field))
        {
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString() || value.getAsString().trim().isEmpty())
                throw new JsonParseException(field + " entries must be non-empty strings");
            result.add(value.getAsString().trim());
        }
        return result;
    }

    private static void addBlock(Set<BlockMeta> blocks, List<Pattern> ores, String entry)
    {
        try
        {
            if (!entry.contains(":"))
            {
                StringBuilder regex = new StringBuilder("^");
                for (String part : entry.split("\\*", -1))
                {
                    if (regex.length() > 1) regex.append(".*");
                    regex.append(Pattern.quote(part));
                }
                ores.add(Pattern.compile(regex.append('$').toString()));
                return;
            }
            String[] parts = entry.split("/", -1);
            if (parts.length > 2) throw new IllegalArgumentException("Invalid block/metadata format");
            Block block = Block.getBlockFromName(parts[0]);
            if (block == null || block == Blocks.AIR) throw new IllegalArgumentException("Unknown block");
            int meta = parts.length == 1 ? 0 : parts[1].equals("*") ? OreDictionary.WILDCARD_VALUE : Integer.parseInt(parts[1]);
            if (meta != OreDictionary.WILDCARD_VALUE && (meta < 0 || meta > 15)) throw new IllegalArgumentException("Block metadata must be 0-15 or *");
            blocks.add(new BlockMeta(block, meta));
        }
        catch (RuntimeException error) { MinersProsperity.logger.warn("Invalid Quick Mining block entry '{}': {}", entry, error.getMessage()); }
    }

    private static void addTool(Set<ItemMeta> tools, String entry)
    {
        try
        {
            String[] parts = entry.split("/", -1);
            if (parts.length > 2 || !parts[0].contains(":")) throw new IllegalArgumentException("Use namespace:item_id or namespace:item_id/metadata");
            Item item = Item.getByNameOrId(parts[0]);
            if (item == null || item == Item.getItemFromBlock(Blocks.AIR)) throw new IllegalArgumentException("Unknown item");
            int meta = parts.length == 1 || parts[1].equals("*") ? OreDictionary.WILDCARD_VALUE : Integer.parseInt(parts[1]);
            if (meta < 0) throw new IllegalArgumentException("Tool metadata must be non-negative or *");
            tools.add(new ItemMeta(item, meta));
        }
        catch (RuntimeException error) { MinersProsperity.logger.warn("Invalid Quick Mining tool entry '{}': {}", entry, error.getMessage()); }
    }

    static Set<String> toolTypes(ItemStack tool)
    {
        if (tool.isEmpty()) return Collections.emptySet();
        Map<String, Rules> current = rules;
        Set<String> explicit = new LinkedHashSet<>();
        boolean assigned = false;
        for (Map.Entry<String, Rules> entry : current.entrySet())
        {
            Rules rule = entry.getValue();
            if (matchesTool(rule.tools, tool))
            {
                assigned = true;
                if (rule.enabled && !matchesTool(rule.excludedTools, tool)) explicit.add(entry.getKey());
            }
        }
        if (assigned) return explicit;
        Set<String> detected = detectedTypes(tool);
        detected.removeIf(type -> !current.containsKey(type) || !current.get(type).enabled || !current.get(type).autoDetect || matchesTool(current.get(type).excludedTools, tool));
        return detected;
    }

    private static Set<String> detectedTypes(ItemStack stack)
    {
        Item item = stack.getItem();
        Set<String> result = new LinkedHashSet<>();
        for (String type : item.getToolClasses(stack)) if (TOOL_TYPES.contains(type)) result.add(type);
        if (item instanceof ItemPickaxe) result.add("pickaxe");
        if (item instanceof ItemAxe) result.add("axe");
        if (item instanceof ItemSpade) result.add("shovel");
        if (item instanceof ItemHoe) result.add("hoe");
        if (item instanceof ItemSword) result.add("sword");
        if (item instanceof ItemShears) result.add("shears");
        if (result.isEmpty() && (item instanceof ItemTool || !item.getToolClasses(stack).isEmpty())) result.add("other");
        return result;
    }

    private static boolean matchesTool(Set<ItemMeta> entries, ItemStack stack)
    {
        return entries.contains(new ItemMeta(stack.getItem(), stack.getMetadata())) || entries.contains(new ItemMeta(stack.getItem(), OreDictionary.WILDCARD_VALUE));
    }

    public static boolean isSupportedTool(ItemStack stack)
    {
        return !stack.isEmpty() && !toolTypes(stack).isEmpty();
    }

    public static boolean canQuickMine(IBlockState state, ItemStack tool)
    {
        Map<String, Rules> current = rules;
        for (String type : toolTypes(tool))
        {
            Rules rule = current.get(type);
            if (rule != null && !matchesBlock(rule.excludedBlocks, rule.excludedOres, state.getBlock(), state.getBlock().getMetaFromState(state))
                    && matchesBlock(rule.blocks, rule.ores, state.getBlock(), state.getBlock().getMetaFromState(state))) return true;
        }
        return false;
    }

    public static boolean canQuickMine(ItemStack stack)
    {
        if (stack == null || stack.isEmpty()) return false;
        Block block = Block.getBlockFromItem(stack.getItem());
        for (Rules rule : rules.values())
            if (rule.enabled && !matchesBlock(rule.excludedBlocks, rule.excludedOres, block, stack.getMetadata()) && matchesBlock(rule.blocks, rule.ores, block, stack.getMetadata())) return true;
        return false;
    }

    private static boolean matchesBlock(Set<BlockMeta> blocks, List<Pattern> ores, Block block, int meta)
    {
        if (block == Blocks.AIR) return false;
        if (blocks.contains(new BlockMeta(block, meta)) || blocks.contains(new BlockMeta(block, OreDictionary.WILDCARD_VALUE))) return true;
        ItemStack stack = new ItemStack(block, 1, meta);
        if (stack.isEmpty()) return false;
        for (int id : OreDictionary.getOreIDs(stack))
            for (Pattern pattern : ores) if (pattern.matcher(OreDictionary.getOreName(id)).matches()) return true;
        return false;
    }

    static JsonObject defaultRules()
    {
        JsonObject document = new JsonObject();
        for (String type : TOOL_TYPES)
        {
            JsonObject group = new JsonObject();
            group.addProperty("enabled", true);
            group.addProperty("autoDetectTools", true);
            JsonArray blocks = new JsonArray();
            if (type.equals("pickaxe")) blocks.add("ore*");
            group.add("blocks", blocks);
            group.add("excludedBlocks", new JsonArray());
            group.add("tools", new JsonArray());
            group.add("excludedTools", new JsonArray());
            document.add(type, group);
        }
        return document;
    }

    private static void write(File file, JsonObject document) throws IOException
    {
        Files.createDirectories(file.toPath().toAbsolutePath().getParent());
        Path temporary = Files.createTempFile(file.toPath().toAbsolutePath().getParent(), "quick_mining-", ".tmp");
        try
        {
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(temporary.toFile()), StandardCharsets.UTF_8))
            {
                writer.write("""
                        // Quick Mining - tool categories: pickaxe, axe, shovel, hoe, sword, shears, other.
                        // Blocks: namespace:block means metadata 0; /3 means metadata 3; /* means all metadata.
                        // Names without ':' are case-sensitive Ore Dictionary patterns: ore*, log*, treeLeaves, *Copper.
                        // '*' matches any number of characters, including zero; other characters are literal.
                        // Examples: axe.blocks = ["logWood"], shovel.blocks = ["minecraft:gravel/*"],
                        // hoe.blocks = ["minecraft:wheat/7"], sword.blocks = ["minecraft:web"], shears.blocks = ["treeLeaves"].
                        // These examples are suggestions; by default only pickaxe has blocks (ore*).
                        // tools explicitly assigns item IDs to a category, overriding automatic classification.
                        // A tool may be assigned to several categories. An item ID matches all its metadata;
                        // use namespace:item/3 for one metadata value or namespace:item/* for all values.
                        // Example: "tools": ["minecraft:diamond_pickaxe", "examplemod:custom_drill/*"].
                        // excludedTools removes an item from this category; exclusions take precedence.
                        // excludedBlocks removes block IDs/metadata or Ore Dictionary patterns from this category.
                        // enabled=false disables a category; autoDetectTools=false allows only explicit tools.
                        // Vanilla classes and Forge tool classes are detected automatically. Other covers unknown
                        // Forge tool classes and remaining ItemTool subclasses; any item can be assigned manually.
                        // Configure all per-tool rules here; the in-game config contains only the global
                        // Quick Mining switch and block limit.
                        // A multi-tool can use the union of its categories, subject to normal harvesting checks.
                        // quick_mining_detected_tools.json is an automatically regenerated, read-only inventory
                        // of automatic classification (base metadata 0); add missing/modded variants via tools.
                        // Restart Minecraft after editing this JSON.
                        // Sneaking suppresses Quick Mining. The shared block limit still applies.

                        """);
                new GsonBuilder().setPrettyPrinting().create().toJson(document, writer);
            }
            try { Files.move(temporary, file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ignored) { Files.move(temporary, file.toPath(), StandardCopyOption.REPLACE_EXISTING); }
        }
        finally { Files.deleteIfExists(temporary); }
    }

    private static void writeDetectedTools(File file) throws IOException
    {
        Map<String, SortedSet<String>> detected = new LinkedHashMap<>();
        for (String type : TOOL_TYPES) detected.put(type, new TreeSet<>());
        for (Item item : Item.REGISTRY)
        {
            ItemStack stack = new ItemStack(item);
            if (stack.isEmpty()) continue;
            for (String type : detectedTypes(stack)) detected.get(type).add(Objects.requireNonNull(item.getRegistryName()).toString());
        }
        Files.createDirectories(file.toPath().getParent());
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))
        {
            writer.write("// Generated automatic tool detection, base metadata 0. Changes here do not affect Quick Mining.\n// Assign missing tools or variants in quick_mining.json.\n");
            new GsonBuilder().setPrettyPrinting().create().toJson(detected, writer);
        }
    }

    private static class Rules
    {
        boolean enabled;
        boolean autoDetect;
        final Set<BlockMeta> blocks = new HashSet<>();
        final Set<BlockMeta> excludedBlocks = new HashSet<>();
        final List<Pattern> ores = new ArrayList<>();
        final List<Pattern> excludedOres = new ArrayList<>();
        final Set<ItemMeta> tools = new HashSet<>();
        final Set<ItemMeta> excludedTools = new HashSet<>();
    }

    @Desugar
    private record BlockMeta(Block block, int meta)
    {
    }
    @Desugar
    private record ItemMeta(Item item, int meta)
    {
    }
}