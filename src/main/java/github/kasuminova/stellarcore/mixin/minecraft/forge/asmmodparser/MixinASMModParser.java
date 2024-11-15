package github.kasuminova.stellarcore.mixin.minecraft.forge.asmmodparser;

import net.minecraftforge.fml.common.discovery.asm.ASMModParser;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(value = ASMModParser.class, remap = false)
public class MixinASMModParser {

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/objectweb/asm/ClassReader;accept(Lorg/objectweb/asm/ClassVisitor;I)V"))
    private void redirectInit(final ClassReader instance, final ClassVisitor classVisitor, final int flags) {
        instance.accept(classVisitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    }

}
