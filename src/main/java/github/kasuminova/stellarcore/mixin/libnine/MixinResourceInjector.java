package github.kasuminova.stellarcore.mixin.libnine;

import github.kasuminova.stellarcore.mixin.util.ConcurrentResourceInjector;
import github.kasuminova.stellarcore.shaded.org.jctools.maps.NonBlockingHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Set;

@Mixin(targets = "io.github.phantamanta44.libnine.client.model.ParameterizedItemModelLoader$ResourceInjector", remap = false)
public class MixinResourceInjector implements ConcurrentResourceInjector {

    @Unique
    private static final char[] stellar_core$HEX_DIGITS = "0123456789abcdef".toCharArray();

    @Unique
    private static final ThreadLocal<MessageDigest> stellar_core$SHA_256 = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new ExceptionInInitializerError(e);
        }
    });

    @Final
    @Shadow
    @Mutable
    private static Set<String> RESOURCE_DOMAINS;

    @Final
    @Shadow
    @Mutable
    private Map<String, String> resources;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void injectClinit(CallbackInfo ci) {
        final ObjectOpenHashSet<String> domains = new ObjectOpenHashSet<>(1);
        domains.add("libnine_pi");
        RESOURCE_DOMAINS = domains;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void injectInit(final CallbackInfo ci) {
        this.resources = new Object2ObjectOpenHashMap<>();
    }

    @Override
    @Unique
    public void stellar_core$toConcurrent() {
        if (!(this.resources instanceof NonBlockingHashMap)) {
            this.resources = new NonBlockingHashMap<>(this.resources);
        }
    }

    @Override
    @Unique
    public void stellar_core$toDefault() {
        if (!(this.resources instanceof Object2ObjectOpenHashMap)) {
            this.resources = new Object2ObjectOpenHashMap<>(this.resources);
        }
    }

    @Unique
    private static String stellar_core$resourceId(final String resource) {
        final byte[] digest = stellar_core$SHA_256.get().digest(resource.getBytes(StandardCharsets.UTF_8));
        final char[] encoded = new char[digest.length * 2];
        for (int i = 0; i < digest.length; i++) {
            final int value = digest[i] & 0xff;
            encoded[i * 2] = stellar_core$HEX_DIGITS[value >>> 4];
            encoded[i * 2 + 1] = stellar_core$HEX_DIGITS[value & 0x0f];
        }
        return new String(encoded);
    }

    /**
     * @author Kasumi_Nova
     * @reason Thread safe.
     */
    @Overwrite
    ResourceLocation injectResource(String resource) {
        // The location is also used by persistent model caches, so it must not depend on load order.
        final String id = stellar_core$resourceId(resource);
        this.resources.put("models/" + id + ".json", resource);
        return new ResourceLocation("libnine_pi", id);
    }

}
