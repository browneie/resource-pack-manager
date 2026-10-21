package com.resourcepackmanager;

import net.minecraft.client.gui.screen.menu.resourcepacks.CustomResourcePackEntry;

import java.util.Comparator;
import java.util.Locale;

/** Order of the left-hand (available) column. Favorites always come first. */
public final class PackSorting {

    private PackSorting() {
    }

    public static Comparator<CustomResourcePackEntry> comparator(PackManagerData.Sort sort) {
        Comparator<CustomResourcePackEntry> byName = Comparator.comparing(PackSorting::sortName);
        Comparator<CustomResourcePackEntry> byDate = Comparator.comparingLong(PackSorting::lastModified);
        Comparator<CustomResourcePackEntry> order;
        switch (sort) {
            case NAME_DESC:
                order = byName.reversed();
                break;
            case NEWEST:
                order = byDate.reversed().thenComparing(byName);
                break;
            case OLDEST:
                order = byDate.thenComparing(byName);
                break;
            case NAME_ASC:
            default:
                order = byName;
                break;
        }
        Comparator<CustomResourcePackEntry> favoritesFirst =
                Comparator.comparing(entry -> !PackManagerData.isFavorite(entry.getPack().getName()));
        return favoritesFirst.thenComparing(order);
    }

    private static String sortName(CustomResourcePackEntry entry) {
        return PackFolders.stripFormatting(PackFolders.leafName(entry.getPack().getName())).toLowerCase(Locale.ROOT);
    }

    private static long lastModified(CustomResourcePackEntry entry) {
        return ((PackFileAccess) (Object) entry.getPack()).rpm$getFile().lastModified();
    }
}
