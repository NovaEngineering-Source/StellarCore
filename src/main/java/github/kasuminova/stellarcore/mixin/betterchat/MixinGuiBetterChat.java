package github.kasuminova.stellarcore.mixin.betterchat;

import com.llamalad7.betterchat.gui.GuiBetterChat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiUtilRenderComponents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 复制自 UniversalTweaks 的消息压缩，并兼容 BetterChat。
 */
@Mixin(GuiBetterChat.class)
public abstract class MixinGuiBetterChat {
    @Unique
    private final Pattern universalTweaks$pattern = Pattern.compile("(?:§7)?\\s+\\[+\\d+]");

    @Shadow(remap = false) @Final private List<ChatLine> drawnChatLines = new ArrayList<>();

    @Shadow(remap = false) @Final private List<ChatLine> chatLines = new ArrayList<>();

    @Shadow(remap = false) @Final private Minecraft mc;

    @Shadow
    public abstract float getChatScale();

    @Shadow
    public abstract int getChatWidth();

    @Inject(method = "setChatLine", at = @At("HEAD"), remap = false)
    private void compactMessage(ITextComponent chatComponent, int chatLineId, int updateCounter, boolean displayOnly, CallbackInfo ci) {
        int count = 1;
        int chatSize = MathHelper.floor((float) this.getChatWidth() / this.getChatScale());
        List<ITextComponent> split = GuiUtilRenderComponents.splitText(chatComponent, chatSize, this.mc.fontRenderer, false, false);
        ITextComponent textComponent = split.get(split.size() - 1);
        for (int i = 0; i < drawnChatLines.size(); i++) {
            ChatLine chatLine = drawnChatLines.get(i);
            ITextComponent lineComponent = chatLine.getChatComponent();
            if (lineComponent.getFormattedText().trim().isEmpty()) {
                continue;
            }
            if (!universalTweaks$isMessageEqual(lineComponent.createCopy(), textComponent.createCopy())) {
                continue;
            }
            if (!lineComponent.getSiblings().isEmpty()) {
                for (ITextComponent sibling : lineComponent.getSiblings()) {
                    if (universalTweaks$pattern.matcher(sibling.getUnformattedComponentText()).matches()) {
                        count += Integer.parseInt(sibling.getUnformattedComponentText().replaceAll("(?:§7)?\\D?", ""));
                        break;
                    }
                }
            }
            this.drawnChatLines.removeIf(chatLine1 -> split.contains(chatLine1.getChatComponent()) || chatLine1.equals(chatLine));
            this.chatLines.removeIf(chatLine1 -> chatLine1.getChatComponent().getUnformattedComponentText().equals(chatComponent.getUnformattedComponentText()));
            chatComponent.appendSibling(new TextComponentString(" [" + count + "]").setStyle(new Style().setColor(TextFormatting.GRAY)));
            break;
        }
    }

    @Unique
    private boolean universalTweaks$isMessageEqual(ITextComponent left, ITextComponent right) {
        if (left.equals(right) || left.getUnformattedText().equals(right.getUnformattedText())) {
            return true;
        }
        if (left.getSiblings().isEmpty()) {
            return false;
        }

        left.getSiblings().removeIf(sibling -> universalTweaks$pattern.matcher(sibling.getUnformattedComponentText()).matches());

        return left.equals(right) || left.getUnformattedText().equals(right.getUnformattedText());
    }
}
