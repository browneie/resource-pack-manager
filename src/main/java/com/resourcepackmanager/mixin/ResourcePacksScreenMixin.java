package com.resourcepackmanager.mixin;

import com.resourcepackmanager.FolderEntry;
import com.resourcepackmanager.PackFolders;
import com.resourcepackmanager.PackManagerData;
import com.resourcepackmanager.PackScreenExt;
import com.resourcepackmanager.PackSorting;
import com.resourcepackmanager.ProfilesScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.menu.resourcepacks.AvailableResourcePackListWidget;
import net.minecraft.client.gui.screen.menu.resourcepacks.CustomResourcePackEntry;
import net.minecraft.client.gui.screen.menu.resourcepacks.ResourcePackEntry;
import net.minecraft.client.gui.screen.menu.resourcepacks.ResourcePackListWidget;
import net.minecraft.client.gui.screen.menu.resourcepacks.ResourcePacksScreen;
import net.minecraft.client.gui.screen.menu.resourcepacks.SelectedResourcePackListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Adds a search box, folder browsing, sorting, favorites and profiles to the vanilla pack screen.
 * The available column is rebuilt from {@link #rpm$allPacks} whenever something changes; vanilla keeps
 * managing the selected column and applies it on Done as usual.
 */
@Mixin(ResourcePacksScreen.class)
public abstract class ResourcePacksScreenMixin extends Screen implements PackScreenExt {

    @Unique private static final int RPM_SORT_BUTTON = 4240;
    @Unique private static final int RPM_PROFILES_BUTTON = 4241;
    /** Vanilla lists start at y=32; leave room for the search row. */
    @Unique private static final int RPM_LIST_TOP = 54;
    @Unique private static final int RPM_SEARCH_WIDTH = 128;
    @Unique private static final int RPM_SEARCH_HEIGHT = 18;
    @Unique private static final long RPM_STATUS_MILLIS = 4000L;

    @Shadow private List<ResourcePackEntry> availablePacks;
    @Shadow private List<ResourcePackEntry> selectedPacks;
    @Shadow private AvailableResourcePackListWidget availablePacksWidget;
    @Shadow private SelectedResourcePackListWidget selectedPacksWidget;

    @Shadow
    public abstract void setChanged();

    /** Every pack found on disk, whichever column it is in. */
    @Unique private final List<CustomResourcePackEntry> rpm$allPacks = new ArrayList<>();
    /** The list instance rpm$allPacks was built from; vanilla makes a new one when it rescans the folder. */
    @Unique private List<ResourcePackEntry> rpm$indexedList;
    @Unique private TextFieldWidget rpm$search;
    @Unique private String rpm$query = "";
    @Unique private String rpm$folder = "";
    @Unique private String rpm$status;
    @Unique private long rpm$statusUntil;

    @Inject(method = "init", at = @At("TAIL"))
    private void rpm$init(CallbackInfo ci) {
        if (this.availablePacks != this.rpm$indexedList) {
            this.rpm$indexedList = this.availablePacks;
            this.rpm$allPacks.clear();
            rpm$collectPacks(this.availablePacks, this.rpm$allPacks);
            rpm$collectPacks(this.selectedPacks, this.rpm$allPacks);
        }

        int listBottom = this.height - 51;
        this.availablePacksWidget.setBounds(200, this.height, RPM_LIST_TOP, listBottom);
        this.availablePacksWidget.setX(this.width / 2 - 204);
        this.selectedPacksWidget.setBounds(200, this.height, RPM_LIST_TOP, listBottom);
        this.selectedPacksWidget.setX(this.width / 2 + 4);

        Keyboard.enableRepeatEvents(true);
        this.rpm$search = new TextFieldWidget(0, this.textRenderer, this.width / 2 - 203, 31, RPM_SEARCH_WIDTH, RPM_SEARCH_HEIGHT);
        this.rpm$search.setMaxLength(64);
        this.rpm$search.setText(this.rpm$query);
        this.buttons.add(new ButtonWidget(RPM_SORT_BUTTON, this.width / 2 - 72, 30, 68, 20, rpm$sortLabel()));
        this.buttons.add(new ButtonWidget(RPM_PROFILES_BUTTON, this.width / 2 + 4, 30, 200, 20, "Profiles..."));

        this.rpm$refresh();
    }

    @Unique
    private static void rpm$collectPacks(List<ResourcePackEntry> from, List<CustomResourcePackEntry> into) {
        for (ResourcePackEntry entry : from) {
            if (entry instanceof CustomResourcePackEntry) into.add((CustomResourcePackEntry) entry);
        }
    }

    @Unique
    private static String rpm$sortLabel() {
        return "Sort: " + PackManagerData.getSort().label;
    }

    /** Rebuilds the available column: the current folder's subfolders and packs, or every match when searching. */
    @Unique
    private void rpm$refresh() {
        if (this.availablePacks == null) return;
        String query = this.rpm$query.trim().toLowerCase(Locale.ROOT);
        boolean searching = !query.isEmpty();

        List<CustomResourcePackEntry> packs = new ArrayList<>();
        Map<String, Integer> subfolders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (CustomResourcePackEntry entry : this.rpm$allPacks) {
            if (this.selectedPacks.contains(entry)) continue;
            String name = entry.getPack().getName();
            if (searching) {
                if (PackFolders.stripFormatting(name).toLowerCase(Locale.ROOT).contains(query)) packs.add(entry);
                continue;
            }
            String folder = PackFolders.folderOf(name);
            if (folder.equals(this.rpm$folder)) {
                packs.add(entry);
            } else {
                String child = PackFolders.childOnPath(this.rpm$folder, folder);
                if (child != null) {
                    Integer count = subfolders.get(child);
                    subfolders.put(child, count == null ? 1 : count + 1);
                }
            }
        }
        packs.sort(PackSorting.comparator(PackManagerData.getSort()));

        ResourcePacksScreen self = (ResourcePacksScreen) (Object) this;
        this.availablePacks.clear();
        if (!searching) {
            if (!this.rpm$folder.isEmpty()) this.availablePacks.add(FolderEntry.back(self, this.rpm$folder));
            for (Map.Entry<String, Integer> subfolder : subfolders.entrySet()) {
                this.availablePacks.add(FolderEntry.open(self, subfolder.getKey(), subfolder.getValue()));
            }
        }
        this.availablePacks.addAll(packs);
    }

    @Unique
    private void rpm$scrollAvailableToTop() {
        this.availablePacksWidget.scroll(-this.availablePacksWidget.getScrollAmount());
    }

    @Unique
    private boolean rpm$isOverSearch(int mouseX, int mouseY) {
        return this.rpm$search != null
                && mouseX >= this.rpm$search.x && mouseX < this.rpm$search.x + RPM_SEARCH_WIDTH
                && mouseY >= this.rpm$search.y && mouseY < this.rpm$search.y + RPM_SEARCH_HEIGHT;
    }

    @Unique
    private static ResourcePackEntry rpm$entryAt(ResourcePackListWidget list, int mouseX, int mouseY) {
        if (!list.isMouseInList(mouseY)) return null;
        int index = list.getEntryAt(mouseX, mouseY);
        return index < 0 ? null : list.getEntry(index);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void rpm$mouseClicked(int mouseX, int mouseY, int button, CallbackInfo ci) {
        if (this.rpm$search != null) {
            this.rpm$search.mouseClicked(mouseX, mouseY, button);
            if (button == 1 && rpm$isOverSearch(mouseX, mouseY)) {
                this.rpm$search.setText("");
                this.rpm$query = "";
                this.rpm$refresh();
                ci.cancel();
                return;
            }
        }
        if (button != 1) return;

        // Right-click toggles a favorite. Vanilla would treat it like a left click and move the pack.
        ResourcePackEntry entry = rpm$entryAt(this.availablePacksWidget, mouseX, mouseY);
        if (entry == null) entry = rpm$entryAt(this.selectedPacksWidget, mouseX, mouseY);
        if (entry instanceof CustomResourcePackEntry) {
            String name = ((CustomResourcePackEntry) entry).getPack().getName();
            boolean favorite = PackManagerData.toggleFavorite(name);
            this.rpm$showStatus((favorite ? "§6★ Favorited " : "Unfavorited ") + PackFolders.leafName(name));
            this.rpm$refresh();
            ci.cancel();
        }
    }

    /** A left click may have moved a pack between columns or opened a folder. */
    @Inject(method = "mouseClicked", at = @At("TAIL"))
    private void rpm$afterMouseClicked(int mouseX, int mouseY, int button, CallbackInfo ci) {
        this.rpm$refresh();
    }

    @Inject(method = "buttonClicked", at = @At("HEAD"), cancellable = true)
    private void rpm$buttonClicked(ButtonWidget button, CallbackInfo ci) {
        if (!button.active) return;
        if (button.id == RPM_SORT_BUTTON) {
            PackManagerData.setSort(PackManagerData.getSort().next());
            button.message = rpm$sortLabel();
            this.rpm$refresh();
            ci.cancel();
        } else if (button.id == RPM_PROFILES_BUTTON) {
            this.minecraft.openScreen(new ProfilesScreen((ResourcePacksScreen) (Object) this));
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void rpm$render(int mouseX, int mouseY, float tickDelta, CallbackInfo ci) {
        if (this.rpm$search == null) return;
        this.rpm$search.render();
        if (this.rpm$search.getText().isEmpty() && !this.rpm$search.isFocused()) {
            this.drawString(this.textRenderer, "Search packs...", this.rpm$search.x + 4, this.rpm$search.y + 5, 0x808080);
        }
        // Below vanilla's "(Place resource pack files here)" line, which is wider than its button.
        boolean showStatus = this.rpm$status != null && Minecraft.getTime() < this.rpm$statusUntil;
        this.drawCenteredString(this.textRenderer, showStatus ? this.rpm$status : "Right-click a pack to favorite it",
                this.width / 2, this.height - 14, 0x808080);
    }

    @Override
    protected void keyPressed(char chr, int key) {
        if (this.rpm$search != null) {
            if (this.rpm$search.isFocused()) {
                if (key == Keyboard.KEY_ESCAPE) {
                    this.rpm$search.setFocused(false);
                    return;
                }
                String before = this.rpm$search.getText();
                if (this.rpm$search.keyPressed(chr, key)) {
                    if (!before.equals(this.rpm$search.getText())) {
                        this.rpm$query = this.rpm$search.getText();
                        this.rpm$scrollAvailableToTop();
                        this.rpm$refresh();
                    }
                    return;
                }
            } else if (key == Keyboard.KEY_F && Screen.isControlDown()) {
                this.rpm$search.setFocused(true);
                return;
            }
        }
        super.keyPressed(chr, key);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.rpm$search != null) this.rpm$search.tick();
    }

    @Override
    public void removed() {
        super.removed();
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public List<String> rpm$getSelectedPackNames() {
        List<String> names = new ArrayList<>();
        for (ResourcePackEntry entry : this.selectedPacks) {
            if (entry instanceof CustomResourcePackEntry) names.add(((CustomResourcePackEntry) entry).getPack().getName());
        }
        return names;
    }

    @Override
    public int rpm$selectPacks(List<String> packNames) {
        List<ResourcePackEntry> chosen = new ArrayList<>();
        int missing = 0;
        for (String name : packNames) {
            CustomResourcePackEntry match = null;
            for (CustomResourcePackEntry entry : this.rpm$allPacks) {
                if (entry.getPack().getName().equals(name)) {
                    match = entry;
                    break;
                }
            }
            if (match == null) {
                missing++;
            } else if (!chosen.contains(match)) {
                chosen.add(match);
            }
        }
        // Keep the built-in Default pack (and anything else that isn't a file) at the bottom, as vanilla does.
        for (ResourcePackEntry entry : this.selectedPacks) {
            if (!(entry instanceof CustomResourcePackEntry)) chosen.add(entry);
        }
        this.selectedPacks.clear();
        this.selectedPacks.addAll(chosen);
        this.setChanged();
        this.rpm$refresh();
        return missing;
    }

    @Override
    public void rpm$openFolder(String folder) {
        this.rpm$folder = folder;
        this.rpm$scrollAvailableToTop();
        this.rpm$refresh();
    }

    @Override
    public boolean rpm$isSearching() {
        return !this.rpm$query.trim().isEmpty();
    }

    @Override
    public void rpm$showStatus(String message) {
        this.rpm$status = message;
        this.rpm$statusUntil = Minecraft.getTime() + RPM_STATUS_MILLIS;
    }
}
