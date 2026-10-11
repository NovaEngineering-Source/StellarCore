package github.kasuminova.stellarcore.client.texture;

import com.github.bsideup.jabel.Desugar;
import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingHashMap;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.IResource;
import org.apache.commons.io.IOUtils;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import java.util.Map;

public class SpriteBufferedImageCache {

    public static final SpriteBufferedImageCache INSTANCE = new SpriteBufferedImageCache();

    private final Map<TextureAtlasSprite, CachedImage> cache = new NonBlockingHashMap<>();
    private final Map<TextureAtlasSprite, IResource> resourceCache = new NonBlockingHashMap<>();

    public BufferedImage getImage(TextureAtlasSprite sprite) {
        CachedImage image = cache.get(sprite);
        return image == null ? null : image.createView();
    }

    public int[] getRGBAndRemove(TextureAtlasSprite sprite) {
        CachedImage image = cache.remove(sprite);
        return image == null ? null : image.rgb;
    }

    public IResource getResourceAndRemove(TextureAtlasSprite sprite) {
        return resourceCache.remove(sprite);
    }

    public void put(TextureAtlasSprite sprite, IResource resource) {
        resourceCache.put(sprite, resource);
    }

    public void put(TextureAtlasSprite sprite, int width, int height, int[] rgb) {
        cache.put(sprite, new CachedImage(width, height, rgb));
    }

    public void remove(TextureAtlasSprite sprite) {
        cache.remove(sprite);
        IOUtils.closeQuietly(resourceCache.remove(sprite));
    }

    public void clear() {
        cache.clear();
        resourceCache.values().forEach(IOUtils::closeQuietly);
        resourceCache.clear();
    }

    @Desugar
    private record CachedImage(int width, int height, int[] rgb) {
        private static final int[] RGB_MASKS = {0x00FF0000, 0x0000FF00, 0x000000FF, 0xFF000000};

        private BufferedImage createView() {
            DataBuffer dataBuffer = new DataBufferInt(rgb, rgb.length);
            SinglePixelPackedSampleModel sampleModel = new SinglePixelPackedSampleModel(DataBuffer.TYPE_INT, width, height, RGB_MASKS);
            WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuffer, null);
            return new BufferedImage(ColorModel.getRGBdefault(), raster, false, null);
        }
    }

}
