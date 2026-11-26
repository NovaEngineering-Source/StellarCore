package github.kasuminova.stellarcore.mixin.geckolib;

import github.kasuminova.stellarcore.client.animation.AnimationTrackerFrames;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.processor.AnimationProcessor;
import software.bernie.geckolib3.model.AnimatedGeoModel;
import software.bernie.geckolib3.model.AnimatedTickingGeoModel;
import software.bernie.shadowed.eliotlash.molang.MolangParser;

@Mixin(value = {AnimatedGeoModel.class, AnimatedTickingGeoModel.class}, remap = false)
public abstract class MixinAnimationTrackerScope {
    @Redirect(method = "setLivingAnimations(Lsoftware/bernie/geckolib3/core/IAnimatable;Ljava/lang/Integer;Lsoftware/bernie/geckolib3/core/event/predicate/AnimationEvent;)V",
        at = @At(value = "INVOKE", target = "Lsoftware/bernie/geckolib3/core/processor/AnimationProcessor;tickAnimation(Lsoftware/bernie/geckolib3/core/IAnimatable;Ljava/lang/Integer;DLsoftware/bernie/geckolib3/core/event/predicate/AnimationEvent;Lsoftware/bernie/shadowed/eliotlash/molang/MolangParser;Z)V"), require = 1)
    private void stellar_core$scopedAnimationTrackers(final AnimationProcessor<?> processor, final IAnimatable entity,
                                              final Integer uniqueID, final double seekTime, final AnimationEvent<?> event,
                                              final MolangParser parser, final boolean crashWhenCantFindBone) {
        AnimationTrackerFrames.tick(processor, entity, uniqueID, seekTime, event, parser, crashWhenCantFindBone);
    }
}
