package github.kasuminova.stellarcore.client.texture;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SpriteBufferedImageCache {

    public static final SpriteBufferedImageCache INSTANCE = new SpriteBufferedImageCache();

    private final Map<TextureAtlasSprite, BufferedImage> cache = new ConcurrentHashMap<>();

    public BufferedImage get(TextureAtlasSprite sprite) {
        return cache.get(sprite);
    }

    public void put(TextureAtlasSprite sprite, BufferedImage image) {
        cache.put(sprite, image);
    }

    public void clear() {
        cache.clear();
    }

}
