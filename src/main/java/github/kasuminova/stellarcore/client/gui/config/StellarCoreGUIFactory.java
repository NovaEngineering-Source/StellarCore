package github.kasuminova.stellarcore.client.gui.config;

import github.kasuminova.stellarcore.common.mod.Mods;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.IModGuiFactory;

import java.util.Collections;
import java.util.Set;

public class StellarCoreGUIFactory implements IModGuiFactory {

    @Override
    public void initialize(final Minecraft minecraftInstance) {
        
    }

    @Override
    public boolean hasConfigGui() {
        return Mods.MMCE.loaded();
    }

    @Override
    public GuiScreen createConfigGui(final GuiScreen parentScreen) {
        return null;
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return Collections.emptySet();
    }

}
