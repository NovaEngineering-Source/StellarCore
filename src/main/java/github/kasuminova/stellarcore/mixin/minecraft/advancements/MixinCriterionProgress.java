package github.kasuminova.stellarcore.mixin.minecraft.advancements;

import net.minecraft.advancements.CriterionProgress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Mixin(CriterionProgress.class)
public class MixinCriterionProgress {

    @Unique
    private static final ThreadLocal<SimpleDateFormat> DATE_TIME_FORMATTER = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z"));

    @Redirect(
            method = "serialize",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/text/SimpleDateFormat;format(Ljava/util/Date;)Ljava/lang/String;",
                    remap = false
            ),
            remap = true
    )
    @SuppressWarnings("MethodMayBeStatic")
    private String redirectSerializeFormat(final SimpleDateFormat instance, final Date date) {
        return DATE_TIME_FORMATTER.get().format(date);
    }

    @Redirect(
            method = "fromDateTime",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/text/SimpleDateFormat;parse(Ljava/lang/String;)Ljava/util/Date;",
                    remap = false
            ),
            remap = true
    )
    @SuppressWarnings("UseOfObsoleteDateTimeApi")
    private static Date redirectFromDateTimeParse(final SimpleDateFormat instance, final String s) throws ParseException {
        return DATE_TIME_FORMATTER.get().parse(s);
    }

}
