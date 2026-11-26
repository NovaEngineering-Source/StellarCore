package github.kasuminova.stellarcore.mixin.geckolib;

import github.kasuminova.stellarcore.common.util.GeckoZipResourcePaths;
import net.minecraft.client.resources.FileResourcePack;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import software.bernie.geckolib3.resource.GeckoLibCache;

import java.util.List;
import java.util.function.Predicate;
import java.util.zip.ZipFile;

@Mixin(value = GeckoLibCache.class, remap = false)
public abstract class MixinGeckoZipResourcePrefixes {
    /**
     * @author StellarCore
     * @reason Reuse domain prefixes while preserving resource discovery and predicate order.
     */
    @Overwrite
    private void enumerateZipFile(final FileResourcePack resourcePack, final String directory, final ZipFile zip,
                                  final Predicate<String> predicate, final List<ResourceLocation> locations) {
        GeckoZipResourcePaths.enumerate(zip, resourcePack.getResourceDomains(), directory, predicate,
                (domain, path) -> locations.add(new ResourceLocation(domain, path)));
    }
}
