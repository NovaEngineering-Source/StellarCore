package github.kasuminova.stellarcore.client.model.vanillacache;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.UnaryOperator;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public final class TeeingResource implements IResource {

    private final IResource delegate;
    private final UnaryOperator<InputStream> tee;

    /** Taps whatever {@link BlockstateCapture} is collecting. */
    public TeeingResource(final IResource delegate) {
        this(delegate, BlockstateCapture::tee);
    }

    /**
     * @param tee the tap to install on the stream; it decides on its own whether
     *            it is currently collecting anything
     */
    public TeeingResource(final IResource delegate, final UnaryOperator<InputStream> tee) {
        this.delegate = delegate;
        this.tee = tee;
    }

    @Override
    public InputStream getInputStream() {
        return tee.apply(delegate.getInputStream());
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
