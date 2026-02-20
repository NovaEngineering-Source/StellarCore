package github.kasuminova.stellarcore.mixin.minecraft.resources;

import github.kasuminova.stellarcore.mixin.util.StellarCoreAbstractResourcePackAccessor;
import net.minecraft.client.resources.FolderResourcePack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.io.File;

@Mixin(FolderResourcePack.class)
public class MixinFolderResourcePack {

    @Inject(method = "getFile", at = @At("HEAD"), cancellable = true)
    private void stellar_core$getFileFast(@Nullable final String name, final CallbackInfoReturnable<File> cir) {
        if (name == null || name.isEmpty() || stellar_core$isInvalidName(name)) {
            cir.setReturnValue(null);
            return;
        }

        final File root = ((StellarCoreAbstractResourcePackAccessor) (Object) this).stellar_core$getResourcePackFile();
        final File file = new File(root, name);
        cir.setReturnValue(file.isFile() ? file : null);
    }

    @Unique
    private static boolean stellar_core$isInvalidName(final String name) {
        final char first = name.charAt(0);
        if (first == '/' || first == '\\') {
            return true;
        }

        // Windows drive letter path.
        if (name.length() >= 2 && name.charAt(1) == ':' && Character.isLetter(name.charAt(0))) {
            return true;
        }

        // Keep path semantics consistent with ResourceLocation (which uses '/').
        if (name.indexOf('\\') >= 0) {
            return true;
        }

        // Prevent path traversal without expensive canonicalization.
        if (name.indexOf("..") < 0) {
            return false;
        }
        return name.equals("..") || name.startsWith("../") || name.endsWith("/..") || name.contains("/../");
    }
}
