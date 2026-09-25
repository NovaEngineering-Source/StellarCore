package github.kasuminova.stellarcore.mixin.minecraft.forge.vanillamodeldiskcache;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.ResourcePackRepository;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/**
 * Read access to the parts of {@link Minecraft} that describe the resource
 * environment.
 *
 * <p>Both fields are private with no Forge getter, and both are needed to
 * fingerprint the environment: {@code defaultResourcePacks} holds the built-in,
 * classpath and mod-provided packs, while {@code resourcePackRepository} holds
 * the user-selected packs <em>in priority order</em> &mdash; the ordering is part
 * of the cache validity contract, since a pack earlier in the list overrides one
 * later.</p>
 *
 * <p>Using accessors keeps this on the same footing as any other mixin in the
 * codebase: the compiler checks the field types, and a rename in the target
 * surfaces at mixin-apply time rather than as a silent miss.</p>
 */
@Mixin(Minecraft.class)
public interface AccessorMinecraftResourcePacks {

    @Accessor("defaultResourcePacks")
    List<IResourcePack> stellar_core$getDefaultResourcePacks();

    @Accessor("resourcePackRepository")
    ResourcePackRepository stellar_core$getResourcePackRepository();
}
