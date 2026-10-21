package com.resourcepackmanager;

import java.util.List;

/** Implemented on ResourcePacksScreen by ResourcePacksScreenMixin. */
public interface PackScreenExt {

    /** Names of the packs in the right-hand column, top (highest priority) first. */
    List<String> rpm$getSelectedPackNames();

    /**
     * Replaces the right-hand column with these packs, top first. Takes effect on Done, like any other change.
     *
     * @return how many of the names matched no pack in resourcepacks/
     */
    int rpm$selectPacks(List<String> packNames);

    void rpm$openFolder(String folder);

    boolean rpm$isSearching();

    void rpm$showStatus(String message);
}
