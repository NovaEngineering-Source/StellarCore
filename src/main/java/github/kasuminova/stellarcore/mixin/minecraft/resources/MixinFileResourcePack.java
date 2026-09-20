package github.kasuminova.stellarcore.mixin.minecraft.resources;

import github.kasuminova.stellarcore.client.resource.ZipEntryIndex;
import github.kasuminova.stellarcore.mixin.util.StellarCoreFileResourcePackAccessor;
import net.minecraft.client.resources.AbstractResourcePack;
import net.minecraft.client.resources.FileResourcePack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipFile;

@Mixin(FileResourcePack.class)
public abstract class MixinFileResourcePack extends AbstractResourcePack implements StellarCoreFileResourcePackAccessor {

    @Shadow
    private ZipFile resourcePackZipFile;

    @Unique
    private final Object stellar_core$zipFileLock = new Object();

    @Unique
    private Set<String> stellar_core$resourceDomains;

    @Unique
    private ZipFile stellar_core$resourceDomainsZip;

    public MixinFileResourcePack(final File resourcePackFileIn) {
        super(resourcePackFileIn);
    }

    @Inject(method = "getResourcePackZipFile", at = @At("HEAD"), cancellable = true)
    private void stellar_core$safeZipFile(final CallbackInfoReturnable<ZipFile> cir) throws IOException {
        cir.setReturnValue(stellar_core$getResourcePackZipFile());
    }

    @Inject(method = "hasResourceName", at = @At("HEAD"), cancellable = true)
    private void stellar_core$indexedResourceName(final String name, final CallbackInfoReturnable<Boolean> cir) {
        final int indexed = ZipEntryIndex.lookup(this.resourcePackFile, name);
        if (indexed != ZipEntryIndex.UNKNOWN) {
            cir.setReturnValue(indexed == ZipEntryIndex.PRESENT);
        }
    }

    @Inject(method = "getResourceDomains", at = @At("HEAD"), cancellable = true)
    private void stellar_core$cachedResourceDomains(final CallbackInfoReturnable<Set<String>> cir) {
        synchronized (stellar_core$zipFileLock) {
            final ZipFile zipFile;
            try {
                zipFile = stellar_core$getResourcePackZipFile();
            } catch (IOException ignored) {
                return;
            }
            if (stellar_core$resourceDomainsZip == zipFile && stellar_core$resourceDomains != null) {
                cir.setReturnValue(new HashSet<>(stellar_core$resourceDomains));
                return;
            }
            final Set<String> indexed = ZipEntryIndex.lookupResourceDomains(this.resourcePackFile);
            final Set<String> invalid = ZipEntryIndex.lookupInvalidResourceDomains(this.resourcePackFile);
            if (indexed == null || invalid == null) {
                return;
            }
            for (final String namespace : invalid) {
                this.logNameNotLowercase(namespace);
            }
            stellar_core$resourceDomainsZip = zipFile;
            stellar_core$resourceDomains = new HashSet<>(indexed);
            cir.setReturnValue(new HashSet<>(indexed));
        }
    }

    @Inject(method = "getResourceDomains", at = @At("RETURN"))
    private void stellar_core$rememberResourceDomains(final CallbackInfoReturnable<Set<String>> cir) {
        synchronized (stellar_core$zipFileLock) {
            if (this.resourcePackZipFile != null) {
                this.stellar_core$resourceDomainsZip = this.resourcePackZipFile;
                this.stellar_core$resourceDomains = new HashSet<>(cir.getReturnValue());
            }
        }
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void stellar_core$closeZipFile(final CallbackInfo ci) throws IOException {
        ZipEntryIndex.invalidate(this.resourcePackFile);
        synchronized (stellar_core$zipFileLock) {
            stellar_core$resourceDomains = null;
            stellar_core$resourceDomainsZip = null;
            final ZipFile current = this.resourcePackZipFile;
            this.resourcePackZipFile = null;
            if (current != null) {
                current.close();
            }
        }
    }

    @Override
    public ZipFile stellar_core$getResourcePackZipFile() throws IOException {
        synchronized (stellar_core$zipFileLock) {
            ZipFile current = this.resourcePackZipFile;
            if (current == null) {
                current = new ZipFile(this.resourcePackFile);
                this.resourcePackZipFile = current;
            }
            return current;
        }
    }

}
