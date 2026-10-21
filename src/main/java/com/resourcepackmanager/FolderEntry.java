package com.resourcepackmanager;

import net.minecraft.client.gui.GuiElement;
import net.minecraft.client.gui.screen.menu.resourcepacks.ResourcePackEntry;
import net.minecraft.client.gui.screen.menu.resourcepacks.ResourcePacksScreen;
import net.minecraft.client.render.TextRenderer;
import net.minecraft.client.render.platform.GlStateManager;

/** A row in the available column that opens a subfolder of resourcepacks/, or goes back up. */
public class FolderEntry extends ResourcePackEntry {

    private static final int MAX_TEXT_WIDTH = 157;

    private final String target;
    private final String title;
    private final String subtitle;

    private FolderEntry(ResourcePacksScreen parent, String target, String title, String subtitle) {
        super(parent);
        this.target = target;
        this.title = title;
        this.subtitle = subtitle;
    }

    public static FolderEntry open(ResourcePacksScreen parent, String folder, int packCount) {
        return new FolderEntry(parent, folder, "§e" + PackFolders.leafName(folder) + "/",
                packCount + (packCount == 1 ? " pack" : " packs"));
    }

    public static FolderEntry back(ResourcePacksScreen parent, String currentFolder) {
        String up = PackFolders.folderOf(currentFolder);
        return new FolderEntry(parent, up, "§e.. §7(back)", "Up to resourcepacks/" + (up.isEmpty() ? "" : up + "/"));
    }

    @Override
    public void render(int index, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered) {
        int tab = hovered ? 0xFFE0B54A : 0xFFC9962A;
        int back = hovered ? 0xFFC9962A : 0xFFAD7F1F;
        int front = hovered ? 0xFFF7DB7A : 0xFFE6B83C;
        GuiElement.fill(x + 2, y + 5, x + 14, y + 9, tab);
        GuiElement.fill(x + 2, y + 8, x + 30, y + 28, back);
        GuiElement.fill(x + 2, y + 11, x + 30, y + 28, front);
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);

        TextRenderer text = this.minecraft.textRenderer;
        text.drawWithShadow(text.trim(this.title, MAX_TEXT_WIDTH), x + 34, y + 1, 0xFFFFFF);
        text.drawWithShadow(text.trim(this.subtitle, MAX_TEXT_WIDTH), x + 34, y + 12, 0x808080);
    }

    @Override
    public boolean mouseClicked(int index, int mouseX, int mouseY, int button, int entryMouseX, int entryMouseY) {
        if (button != 0) return false;
        ((PackScreenExt) this.parent).rpm$openFolder(this.target);
        return true;
    }

    @Override
    protected int getFormat() {
        return 1;
    }

    @Override
    protected String getDescription() {
        return this.subtitle;
    }

    @Override
    protected String getName() {
        return this.title;
    }

    @Override
    protected void bindIcon() {
    }

    @Override
    protected boolean canMove() {
        return false;
    }
}
