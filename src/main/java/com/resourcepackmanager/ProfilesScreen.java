package com.resourcepackmanager;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.menu.resourcepacks.ResourcePacksScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resource.language.I18n;
import org.lwjgl.input.Keyboard;

import java.util.List;

/** Save the selected packs under a name, or load a saved set back into the pack screen. */
public class ProfilesScreen extends Screen {

    private static final int DONE = 0;
    private static final int SAVE = 1;
    private static final int PREVIOUS_PAGE = 2;
    private static final int NEXT_PAGE = 3;
    private static final int LOAD_ROW = 100;
    private static final int DELETE_ROW = 200;
    private static final int ROW_HEIGHT = 24;
    private static final int FIRST_ROW_Y = 36;

    private final ResourcePacksScreen parent;
    private TextFieldWidget nameField;
    private String nameText = "";
    private List<String> profiles;
    private int page;
    private int rowsPerPage;
    private String pendingDelete;
    private String status = "";

    public ProfilesScreen(ResourcePacksScreen parent) {
        this.parent = parent;
    }

    private PackScreenExt packScreen() {
        return (PackScreenExt) this.parent;
    }

    @Override
    public void init() {
        Keyboard.enableRepeatEvents(true);
        this.profiles = PackManagerData.getProfileNames();
        this.rowsPerPage = Math.max(1, (this.height - FIRST_ROW_Y - 72) / ROW_HEIGHT);
        int pages = Math.max(1, (this.profiles.size() + this.rowsPerPage - 1) / this.rowsPerPage);
        this.page = Math.max(0, Math.min(this.page, pages - 1));

        int first = this.page * this.rowsPerPage;
        for (int row = 0; row < this.rowsPerPage && first + row < this.profiles.size(); row++) {
            String name = this.profiles.get(first + row);
            int count = PackManagerData.getProfile(name).size();
            int y = FIRST_ROW_Y + row * ROW_HEIGHT;
            this.buttons.add(new ButtonWidget(LOAD_ROW + row, this.width / 2 - 155, y, 230, 20,
                    name + " §7(" + count + (count == 1 ? " pack)" : " packs)")));
            this.buttons.add(new ButtonWidget(DELETE_ROW + row, this.width / 2 + 79, y, 76, 20,
                    name.equals(this.pendingDelete) ? "§cConfirm?" : "Delete"));
        }

        this.nameField = new TextFieldWidget(0, this.textRenderer, this.width / 2 - 154, this.height - 51, 148, 18);
        this.nameField.setMaxLength(32);
        this.nameField.setText(this.nameText);
        this.nameField.setFocused(true);
        this.buttons.add(new ButtonWidget(SAVE, this.width / 2 + 4, this.height - 52, 151, 20, "Save Selected Packs"));

        ButtonWidget previous = new ButtonWidget(PREVIOUS_PAGE, this.width / 2 - 155, this.height - 28, 40, 20, "<");
        previous.active = this.page > 0;
        this.buttons.add(previous);
        this.buttons.add(new ButtonWidget(DONE, this.width / 2 - 100, this.height - 28, 200, 20, I18n.translate("gui.done")));
        ButtonWidget next = new ButtonWidget(NEXT_PAGE, this.width / 2 + 115, this.height - 28, 40, 20, ">");
        next.active = this.page < pages - 1;
        this.buttons.add(next);
    }

    private void rebuild() {
        this.init(this.minecraft, this.width, this.height);
    }

    @Override
    protected void buttonClicked(ButtonWidget button) {
        if (!button.active) return;
        if (button.id == DONE) {
            this.minecraft.openScreen(this.parent);
        } else if (button.id == SAVE) {
            this.save();
        } else if (button.id == PREVIOUS_PAGE) {
            this.page--;
            this.rebuild();
        } else if (button.id == NEXT_PAGE) {
            this.page++;
            this.rebuild();
        } else if (button.id >= DELETE_ROW) {
            String name = this.profileAt(button.id - DELETE_ROW);
            if (name.equals(this.pendingDelete)) {
                PackManagerData.deleteProfile(name);
                this.pendingDelete = null;
                this.status = "Deleted \"" + name + "\"";
            } else {
                this.pendingDelete = name;
                this.status = "Click again to delete \"" + name + "\"";
            }
            this.rebuild();
        } else if (button.id >= LOAD_ROW) {
            this.load(this.profileAt(button.id - LOAD_ROW));
        }
    }

    private String profileAt(int row) {
        return this.profiles.get(this.page * this.rowsPerPage + row);
    }

    private void load(String name) {
        List<String> packs = PackManagerData.getProfile(name);
        int missing = this.packScreen().rpm$selectPacks(packs);
        this.packScreen().rpm$showStatus(missing == 0
                ? "§aLoaded \"" + name + "\", press Done to apply"
                : "§eLoaded \"" + name + "\", " + missing + " pack" + (missing == 1 ? " is" : "s are") + " missing");
        this.minecraft.openScreen(this.parent);
    }

    private void save() {
        String name = this.nameField.getText().trim();
        if (name.isEmpty()) {
            this.status = "§cType a name for the profile first";
            return;
        }
        boolean replaced = this.profiles.contains(name);
        List<String> packs = this.packScreen().rpm$getSelectedPackNames();
        PackManagerData.saveProfile(name, packs);
        this.status = "§a" + (replaced ? "Updated" : "Saved") + " \"" + name + "\" with " + packs.size()
                + (packs.size() == 1 ? " pack" : " packs");
        this.nameText = "";
        this.pendingDelete = null;
        this.rebuild();
    }

    @Override
    protected void keyPressed(char chr, int key) {
        if (key == Keyboard.KEY_ESCAPE) {
            this.minecraft.openScreen(this.parent);
            return;
        }
        if (this.nameField.isFocused() && (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER)) {
            this.save();
            return;
        }
        if (this.nameField.keyPressed(chr, key)) {
            this.nameText = this.nameField.getText();
            return;
        }
        super.keyPressed(chr, key);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        this.nameField.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void tick() {
        this.nameField.tick();
    }

    @Override
    public void removed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void render(int mouseX, int mouseY, float tickDelta) {
        this.renderBackground();
        this.drawCenteredString(this.textRenderer, "Resource Pack Profiles", this.width / 2, 15, 0xFFFFFF);
        if (this.profiles.isEmpty()) {
            this.drawCenteredString(this.textRenderer, "No profiles yet.", this.width / 2, FIRST_ROW_Y + 6, 0xA0A0A0);
            this.drawCenteredString(this.textRenderer, "Pick packs on the previous screen, then save them here.",
                    this.width / 2, FIRST_ROW_Y + 18, 0xA0A0A0);
        }
        this.drawCenteredString(this.textRenderer, this.status, this.width / 2, this.height - 66, 0xFFFFFF);

        this.nameField.render();
        if (this.nameField.getText().isEmpty()) {
            this.drawString(this.textRenderer, "Profile name...", this.nameField.x + 4, this.nameField.y + 5, 0x808080);
        }
        super.render(mouseX, mouseY, tickDelta);
    }
}
