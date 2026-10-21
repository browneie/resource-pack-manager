package com.resourcepackmanager.mixin;

import com.resourcepackmanager.PackFolders;
import com.resourcepackmanager.PackManagerData;
import com.resourcepackmanager.PackScreenExt;
import net.minecraft.client.gui.screen.menu.resourcepacks.CustomResourcePackEntry;
import net.minecraft.client.gui.screen.menu.resourcepacks.ResourcePackEntry;
import net.minecraft.client.gui.screen.menu.resourcepacks.ResourcePacksScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CustomResourcePackEntry.class)
public abstract class CustomResourcePackEntryMixin extends ResourcePackEntry {

    private CustomResourcePackEntryMixin(ResourcePacksScreen parent) {
        super(parent);
    }

    /**
     * Only changes the label drawn in the list. Inside a folder the folder is already on screen, so the
     * path is shown only in the selected column and in search results. Favorites get a star.
     */
    @Inject(method = "getName", at = @At("RETURN"), cancellable = true)
    private void rpm$decorateName(CallbackInfoReturnable<String> cir) {
        String name = cir.getReturnValue();
        String label = PackFolders.leafName(name);
        String folder = PackFolders.folderOf(name);
        if (!folder.isEmpty() && (this.parent.isSelected(this) || ((PackScreenExt) this.parent).rpm$isSearching())) {
            label = "§7" + folder + "/§r" + label;
        }
        if (PackManagerData.isFavorite(name)) {
            label = "§6★§r " + label;
        }
        cir.setReturnValue(label);
    }
}
