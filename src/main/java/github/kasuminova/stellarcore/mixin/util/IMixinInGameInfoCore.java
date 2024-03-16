package github.kasuminova.stellarcore.mixin.util;

public interface IMixinInGameInfoCore {

    void addPostDrawAction(Runnable action);

    boolean isPostDrawing();

}
