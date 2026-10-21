package com.resourcepackmanager.mixin;

import com.resourcepackmanager.PackFileAccess;
import com.resourcepackmanager.PackFolders;
import net.minecraft.client.resource.pack.ResourcePacks;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;

@Mixin(ResourcePacks.UnopenedPack.class)
public abstract class UnopenedPackMixin implements PackFileAccess {

    @Shadow @Final private File file;

    /** Nested packs are saved in options.txt by their path, e.g. "PvP/Faithful.zip". */
    @Inject(method = "getName", at = @At("HEAD"), cancellable = true)
    private void rpm$nameByPath(CallbackInfoReturnable<String> cir) {
        String nested = PackFolders.nestedName(this.file);
        if (nested != null) cir.setReturnValue(nested);
    }

    /** equals() and hashCode() go through toString(), which vanilla builds from the bare file name. */
    @Inject(method = "toString", at = @At("HEAD"), cancellable = true, remap = false)
    private void rpm$identityByPath(CallbackInfoReturnable<String> cir) {
        String nested = PackFolders.nestedName(this.file);
        if (nested != null) {
            cir.setReturnValue(String.format("%s:%s:%d", nested, this.file.isDirectory() ? "folder" : "zip", this.file.lastModified()));
        }
    }

    @Override
    public File rpm$getFile() {
        return this.file;
    }
}
