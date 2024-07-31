package github.kasuminova.stellarcore.mixin.minecraft.classmultimap;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.util.ClassInheritanceMultiMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mixin(ClassInheritanceMultiMap.class)
public class MixinClassInheritanceMultiMap {

    @Final
    @Shadow
    @Mutable
    private Map<Class<?>, List<?>> map;

    @Final
    @Shadow
    @Mutable
    private Set<Class<?>> knownKeys;

    @Redirect(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Set;add(Ljava/lang/Object;)Z",
                    remap = false
            ),
            remap = false
    )
    private boolean injectInit(final Set instance, final Object e) {
        this.knownKeys = new ReferenceOpenHashSet<>();
        this.map = new IdentityHashMap<>();
        return knownKeys.add((Class<?>) e);
    }

}
