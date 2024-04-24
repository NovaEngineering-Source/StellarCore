package github.kasuminova.stellarcore.common.util;

import com.github.bsideup.jabel.Desugar;
import net.minecraftforge.fluids.Fluid;

@Desugar
public record HashedStackFluidPair(HashedItemStack stack, Fluid fluid) {

    public HashedStackFluidPair copy() {
        return new HashedStackFluidPair(stack.copy(), fluid);
    }

}
