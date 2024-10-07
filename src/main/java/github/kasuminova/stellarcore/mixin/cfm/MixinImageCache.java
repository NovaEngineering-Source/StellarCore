package github.kasuminova.stellarcore.mixin.cfm;

import com.mrcrayfish.furniture.MrCrayfishFurnitureMod;
import com.mrcrayfish.furniture.client.ImageCache;
import com.mrcrayfish.furniture.client.Texture;
import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingHashMap;
import net.minecraft.client.Minecraft;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Map;

@Mixin(ImageCache.class)
public abstract class MixinImageCache {

    @Shadow(remap = false)
    private Map<String, Texture> cacheMap;

    @Shadow(remap = false)
    public abstract File getCache();

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    public void overwriteInit(final CallbackInfo ci) {
        cacheMap = new NonBlockingHashMap<>();
    }

    /**
     * @author Kasumi_Nova
     * @reason 这怎么写出来的死锁？
     */
    @Overwrite(remap = false)
    public boolean loadCached(String url) {
        if (cacheMap.containsKey(url)) {
            return true;
        }

        String id = DigestUtils.sha1Hex(url.getBytes());
        File file = new File(getCache(), id);
        if (file.exists()) {
            this.add(url, file);
            return true;
        }
        return false;
    }

    /**
     * @author Kasumi_Nova
     * @reason 这怎么写出来的死锁？
     */
    @Overwrite(remap = false)
    @Nullable
    public Texture get(String url) {
        if (url == null) {
            return null;
        }
        Texture texture = cacheMap.get(url);
        if (texture != null) {
            return texture;
        }
        synchronized (this) {
            return cacheMap.get(url);
        }
    }

    /**
     * @author Kasumi_Nova
     * @reason 这怎么写出来的死锁？
     */
    @Overwrite(remap = false)
    public void add(String url, File file) {
        if (cacheMap.containsKey(url)) {
            return;
        }
        synchronized (this) {
            if (!cacheMap.containsKey(url)) {
                Texture texture = new Texture(file);
                cacheMap.put(url, texture);
            }
        }
    }

    /**
     * @author Kasumi_Nova
     * @reason 这怎么写出来的死锁？
     */
    @Overwrite(remap = false)
    public boolean add(String url, byte[] data) {
        if (cacheMap.containsKey(url)) {
            return true;
        }

        Texture texture;
        String id = DigestUtils.sha1Hex(url.getBytes());
        File image = new File(getCache(), id);

        synchronized (this) {
            if (cacheMap.containsKey(url)) {
                return true;
            }
            cacheMap.put(url, texture = new Texture(image));
        }

        try {
            FileUtils.writeByteArrayToFile(image, data);
        } catch (IOException e) {
            MrCrayfishFurnitureMod.logger().warn(e);
        }
        Minecraft.getMinecraft().addScheduledTask(texture::update);
        return true;
    }

    /**
     * @author Kasumi_Nova
     * @reason 这怎么写出来的死锁？
     */
    @Overwrite(remap = false)
    private void tick() {
        cacheMap.values().forEach(Texture::update);
    }

    /**
     * @author Kasumi_Nova
     * @reason 这怎么写出来的死锁？
     */
    @Overwrite(remap = false)
    public boolean isCached(String url) {
        if (cacheMap.containsKey(url)) {
            return true;
        }
        synchronized (this) {
            return cacheMap.containsKey(url);
        }
    }

}
