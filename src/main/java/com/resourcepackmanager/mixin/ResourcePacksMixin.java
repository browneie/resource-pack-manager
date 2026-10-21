package com.resourcepackmanager.mixin;

import com.resourcepackmanager.PackFolders;
import net.minecraft.client.resource.pack.ResourcePacks;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

@Mixin(ResourcePacks.class)
public abstract class ResourcePacksMixin {

    @Shadow @Final private static FileFilter FILE_FILTER;
    @Shadow @Final private File directory;

    /** Vanilla only looks at the top level of resourcepacks/; also pick up packs kept in subfolders. */
    @Inject(method = "discoverAvailablePacks", at = @At("RETURN"), cancellable = true)
    private void rpm$includeSubfolders(CallbackInfoReturnable<List<File>> cir) {
        if (!this.directory.isDirectory()) return;
        List<File> packs = new ArrayList<>(cir.getReturnValue());
        PackFolders.collectNested(this.directory, FILE_FILTER, packs);
        cir.setReturnValue(packs);
    }
}
