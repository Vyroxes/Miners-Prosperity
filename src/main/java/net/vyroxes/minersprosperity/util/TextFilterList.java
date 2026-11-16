package net.vyroxes.minersprosperity.util;

import java.util.LinkedHashSet;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

public final class TextFilterList
{
    public enum Type { ORE, MOD, FLUID }

    private TextFilterList()
    {
    }

    @Nullable
    public static String normalize(String text, Type type)
    {
        if (text.length() > 4096) return null;
        Set<String> entries = new LinkedHashSet<>();
        String allowed = switch (type)
        {
            case ORE -> "[A-Za-z0-9_*]+";
            case MOD -> "[a-z0-9_.-]+";
            case FLUID -> "[a-z0-9_.:/-]+";
        };
        for (String part : text.split(","))
        {
            String entry = part.trim();
            if (entry.isEmpty()) continue;
            if (entry.length() > 64 || !entry.matches(allowed)) return null;
            entries.add(entry);
            if (entries.size() > 32) return null;
        }
        return String.join(", ", entries);
    }

    public static String[] entries(String text)
    {
        return text.isEmpty() ? new String[0] : text.split(", ");
    }

    public static boolean matchesOreName(String pattern, String name)
    {
        int p = 0, n = 0, star = -1, retry = 0;
        while (n < name.length())
        {
            if (p < pattern.length() && pattern.charAt(p) == name.charAt(n))
            {
                p++;
                n++;
            }
            else if (p < pattern.length() && pattern.charAt(p) == '*')
            {
                star = p++;
                retry = n;
            }
            else if (star >= 0)
            {
                p = star + 1;
                n = ++retry;
            }
            else return false;
        }
        while (p < pattern.length() && pattern.charAt(p) == '*') p++;
        return p == pattern.length();
    }
}