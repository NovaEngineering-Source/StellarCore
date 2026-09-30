package github.kasuminova.stellarcore.client.model.vanillacache;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * A resource manager that lets the model loader's own armature lookup be
 * observed, and reports back the one thing a tap cannot see: that the lookup
 * failed with "file not found".
 *
 * <p>Everything is delegated unchanged, so Forge behaves exactly as it would
 * without the wrapper — including how it reacts to a missing armature. Only the
 * tapped location's stream is copied into the current {@link ModelCapture}
 * scope, and only a "file not found" for that location is recorded as a verdict
 * that the model has no armature at all.</p>
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public final class TeeingResourceManager implements IResourceManager {

    private final IResourceManager delegate;
    private final ResourceLocation tapped;

    public TeeingResourceManager(final IResourceManager delegate, final ResourceLocation tapped) {
        this.delegate = delegate;
        this.tapped = tapped;
    }

    @Override
    public Set<String> getResourceDomains() {
        return delegate.getResourceDomains();
    }

    @Override
    public IResource getResource(final ResourceLocation location) throws IOException {
        final IResource resource;
        try {
            resource = delegate.getResource(location);
        } catch (FileNotFoundException absent) {
            if (tapped.equals(location)) {
                ModelCapture.markArmatureAbsent();
            }
            throw absent;
        }
        if (!tapped.equals(location)) {
            return resource;
        }
        return new TeeingResource(resource, ModelCapture::teeArmature);
    }

    @Override
    public List<IResource> getAllResources(final ResourceLocation location) throws IOException {
        return delegate.getAllResources(location);
    }
}
