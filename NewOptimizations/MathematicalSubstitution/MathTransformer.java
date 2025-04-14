package github.kasuminova.stellarcore.common.NewOptimizations.MathematicalSubstitution;

import github.kasuminova.stellarcore.StellarCore;
import org.objectweb.asm.*;
import net.minecraft.launchwrapper.IClassTransformer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MathTransformer implements IClassTransformer {
    @Override
    public byte[] transform(String name, String transformedName, byte[] classBytes) {
            // 仅处理原版或指定模组的类（可选）
            ClassReader cr = new ClassReader(classBytes);
            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
            ClassVisitor cv = new ClassVisitor(Opcodes.ASM5, cw) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                    return new MethodVisitor(Opcodes.ASM5, mv) {
                        @Override
                        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                            if (owner.equals("java/lang/Math")) {
                                String newOwner = "github/kasuminova/stellarcore/common/NewOptimizations/MathematicalSubstitution/CustomMath";

                                Set<String> allowedMethods = new HashSet<>(Arrays.asList(
                                        "sin", "cos", "tan", "asin", "acos", "atan", "atan2"
                                ));

                                if (allowedMethods.contains(name)) {
                                    super.visitMethodInsn(opcode, newOwner, name, desc, itf);
                                    return;
                                }
                                StellarCore.log.debug("succesful");
                            }
                            super.visitMethodInsn(opcode, owner, name, desc, itf);
                        }
                    };
                }
            };
            cr.accept(cv, ClassReader.EXPAND_FRAMES);
            return cw.toByteArray();
    }

    private boolean isTargetModClass(String name) {
        return name.startsWith("com.targetmod.package");
    }
}