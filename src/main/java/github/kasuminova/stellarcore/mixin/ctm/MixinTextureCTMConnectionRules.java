package github.kasuminova.stellarcore.mixin.ctm;

import com.google.gson.JsonElement;
import github.kasuminova.stellarcore.client.integration.ctm.ConnectionPredicateCache;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import team.chisel.ctm.client.texture.render.TextureCTM;
import team.chisel.ctm.client.util.BlockstatePredicateParser;

import java.util.function.BiPredicate;

@Mixin(value = TextureCTM.class, remap = false)
public abstract class MixinTextureCTMConnectionRules {

    @Redirect(method = "*", at = @At(value = "INVOKE",
        target = "Lteam/chisel/ctm/client/util/BlockstatePredicateParser;parse(Lcom/google/gson/JsonElement;)Ljava/util/function/BiPredicate;"), require = 1)
    private static BiPredicate<EnumFacing, IBlockState> stellar_core$reuseConnectionRule(
        final BlockstatePredicateParser parser, final JsonElement json) {
        return parser.getClass() == BlockstatePredicateParser.class
            ? ConnectionPredicateCache.parse(json) : parser.parse(json);
    }
}
