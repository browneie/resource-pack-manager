package com.resourcepackmanager;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lets packs live in plain subfolders of resourcepacks/ (e.g. resourcepacks/PvP/Faithful.zip).
 * A nested pack is named by its path, "PvP/Faithful.zip", so packs with the same file name in
 * different folders stay distinct in options.txt. Top-level packs keep their vanilla names.
 */
public final class PackFolders {

    private static final int MAX_DEPTH = 4;
    private static final Map<File, String> NESTED_NAMES = new ConcurrentHashMap<>();

    private PackFolders() {
    }

    /** Adds every pack found inside organizing folders below {@code root} to {@code out}. */
    public static void collectNested(File root, FileFilter packFilter, List<File> out) {
        File[] children = root.listFiles();
        if (children == null) return;
        Arrays.sort(children);
        for (File child : children) {
            if (isOrganizingFolder(child, packFilter)) collect(root, child, packFilter, out, 1);
        }
    }

    private static void collect(File root, File dir, FileFilter packFilter, List<File> out, int depth) {
        File[] children = dir.listFiles();
        if (children == null) return;
        Arrays.sort(children);
        for (File child : children) {
            if (packFilter.accept(child)) {
                NESTED_NAMES.put(child, relativeName(root, child));
                out.add(child);
            } else if (depth < MAX_DEPTH && isOrganizingFolder(child, packFilter)) {
                collect(root, child, packFilter, out, depth + 1);
            }
        }
    }

    /** A folder that holds packs rather than being one. Skips unzipped packs missing pack.mcmeta. */
    private static boolean isOrganizingFolder(File file, FileFilter packFilter) {
        String name = file.getName();
        return file.isDirectory()
                && !name.startsWith(".")
                && !name.equals("__MACOSX")
                && !packFilter.accept(file)
                && !new File(file, "assets").isDirectory();
    }

    private static String relativeName(File root, File file) {
        return root.toPath().relativize(file.toPath()).toString().replace(File.separatorChar, '/');
    }

    /** @return "PvP/Faithful.zip" for a nested pack, or null for a top-level one */
    public static String nestedName(File packFile) {
        return NESTED_NAMES.get(packFile);
    }

    /** "PvP/Old/Faithful.zip" -> "PvP/Old"; "Faithful.zip" -> "" */
    public static String folderOf(String packName) {
        int slash = packName.lastIndexOf('/');
        return slash < 0 ? "" : packName.substring(0, slash);
    }

    /** "PvP/Old/Faithful.zip" -> "Faithful.zip" */
    public static String leafName(String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }

    /**
     * The direct child of {@code folder} on the way to {@code descendant}, or null when
     * {@code descendant} is not below {@code folder}.
     */
    public static String childOnPath(String folder, String descendant) {
        String prefix = folder.isEmpty() ? "" : folder + "/";
        if (descendant.isEmpty() || descendant.equals(folder) || !descendant.startsWith(prefix)) return null;
        String rest = descendant.substring(prefix.length());
        int slash = rest.indexOf('/');
        return prefix + (slash < 0 ? rest : rest.substring(0, slash));
    }

    public static String stripFormatting(String text) {
        return text.replaceAll("§.", "");
    }
}
