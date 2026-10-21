package com.resourcepackmanager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Favorites, sort order and saved profiles, stored in config/resourcepackmanager.json.
 * Packs are identified by the name Minecraft saves in options.txt (e.g. "PvP/Faithful.zip").
 */
public final class PackManagerData {

    public static final Logger LOGGER = LogManager.getLogger("ResourcePackManager");

    public enum Sort {
        NAME_ASC("A-Z"),
        NAME_DESC("Z-A"),
        NEWEST("Newest"),
        OLDEST("Oldest");

        public final String label;

        Sort(String label) {
            this.label = label;
        }

        public Sort next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private static final class Stored {
        Sort sort = Sort.NAME_ASC;
        Set<String> favorites = new LinkedHashSet<>();
        Map<String, List<String>> profiles = new LinkedHashMap<>();
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("resourcepackmanager.json");

    private static Stored data = new Stored();

    static {
        load();
    }

    private PackManagerData() {
    }

    public static Sort getSort() {
        return data.sort;
    }

    public static void setSort(Sort sort) {
        data.sort = sort;
        save();
    }

    public static boolean isFavorite(String pack) {
        return data.favorites.contains(pack);
    }

    /** @return whether the pack is a favorite after toggling */
    public static boolean toggleFavorite(String pack) {
        boolean nowFavorite = !data.favorites.remove(pack);
        if (nowFavorite) data.favorites.add(pack);
        save();
        return nowFavorite;
    }

    public static List<String> getProfileNames() {
        return new ArrayList<>(data.profiles.keySet());
    }

    public static List<String> getProfile(String name) {
        List<String> packs = data.profiles.get(name);
        return packs == null ? Collections.<String>emptyList() : Collections.unmodifiableList(packs);
    }

    /** @param packs selected packs, top (highest priority) first */
    public static void saveProfile(String name, List<String> packs) {
        data.profiles.put(name, new ArrayList<>(packs));
        save();
    }

    public static void deleteProfile(String name) {
        if (data.profiles.remove(name) != null) save();
    }

    private static void load() {
        if (!Files.exists(FILE)) return;
        try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
            Stored loaded = GSON.fromJson(reader, Stored.class);
            if (loaded != null) {
                if (loaded.sort == null) loaded.sort = Sort.NAME_ASC;
                if (loaded.favorites == null) loaded.favorites = new LinkedHashSet<>();
                if (loaded.profiles == null) loaded.profiles = new LinkedHashMap<>();
                data = loaded;
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not read {}, starting fresh", FILE, e);
        }
    }

    private static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            LOGGER.warn("Could not save {}", FILE, e);
        }
    }
}
