package github.kasuminova.stellarcore.mixin.minecraft.resources;

import github.kasuminova.stellarcore.mixin.util.StellarCoreResourcePackReloadListener;
import net.minecraft.client.resources.DefaultResourcePack;
import net.minecraft.client.resources.ResourceIndex;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.*;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(DefaultResourcePack.class)
public abstract class MixinDefaultResourcePack implements StellarCoreResourcePackReloadListener {

    @Shadow
    @Nullable
    protected abstract InputStream getResourceStream(final ResourceLocation location);

    @Final
    @Shadow
    private ResourceIndex resourceIndex;

    @Unique
    private final Map<ResourceLocation, Boolean> stellar_core$resourceExistsCache = new ConcurrentHashMap<>();

    /**
     * @author Kasumi_Nova
     * @reason Cache
     */
    @Overwrite
    public boolean resourceExists(ResourceLocation location) {
        return stellar_core$resourceExistsCache.computeIfAbsent(location, (key) -> 
                this.getResourceStream(location) != null || this.resourceIndex.isFileExisting(location));
    }

    @Unique
    @Override
    public void stellar_core$onReload() {
        stellar_core$resourceExistsCache.clear();
    }

}
