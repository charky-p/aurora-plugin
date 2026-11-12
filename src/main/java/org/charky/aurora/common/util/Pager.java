package org.charky.aurora.common.util;

import org.bukkit.command.CommandSender;

import java.util.List;

// One page of a long list under a title bar, shared by the list commands so they
// all page and look the same
public final class Pager {
    private static final int PAGE_SIZE = 10;

    private Pager() {
    }

    // The entries on the page asked for in args[0], the first page without it, after
    // showing the title bar. Null after telling the sender why there is no such page.
    // barWidth is the length of the line each side of the title, to centre it
    public static <T> List<T> open(CommandSender sender, String[] args, List<T> entries, String title, int barWidth) {
        int page = 1;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                TextUtils.error(sender, "Invalid page number: " + args[0]);
                return null;
            }
        }

        int maxPages = pageCount(entries.size());
        if (page > maxPages || page < 1) {
            TextUtils.error(sender, "Page " + page + " does not exist. (Max: " + maxPages + ")");
            return null;
        }

        String bar = "<st>" + " ".repeat(barWidth) + "</st>";
        TextUtils.display(sender, "<aqua>" + bar + " <b>" + title + " (<white>" + page + "/" + maxPages + "</white>)</b> " + bar);

        int start = (page - 1) * PAGE_SIZE;
        return entries.subList(start, Math.min(start + PAGE_SIZE, entries.size()));
    }

    static int pageCount(int entries) {
        return Math.max(1, (entries + PAGE_SIZE - 1) / PAGE_SIZE);
    }
}
