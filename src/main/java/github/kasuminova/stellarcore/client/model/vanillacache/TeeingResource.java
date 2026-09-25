package github.kasuminova.stellarcore.client.model.vanillacache;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.io.InputStream;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public final class TeeingResource implements IResource {

    private final IResource delegate;

    public TeeingResource(final IResource delegate) {
        this.delegate = delegate;
    }

    @Override
    public InputStream getInputStream() {
        return BlockstateCapture.tee(delegate.getInputStream());
    }

    @Override
    public ResourceLocation getResourceLocation() {
        return delegate.getResourceLocation();
    }

    @Override
    public boolean hasMetadata() {
        return delegate.hasMetadata();
    }

    @Nullable
    @Override
    public <T extends IMetadataSection> T getMetadata(final String sectionName) {
        return delegate.getMetadata(sectionName);
    }

    @Override
    public String getResourcePackName() {
        return delegate.getResourcePackName();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
