package github.kasuminova.stellarcore.mixin.minecraft.entitytracker.raytrace;

import dev.tr7zw.entityculling.versionless.access.Cullable;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Entity.class)
public class MixinEntity implements Cullable {
    @Unique
    private long stellarCore$lastTime = 0;
    @Unique
    private boolean stellarCore$culled = false;
    @Unique
    private boolean stellarCore$outOfCamera = false;

    @Override
    public void setTimeout() {
        this.stellarCore$lastTime = System.currentTimeMillis() + 1000;
    }

    @Override
    public boolean isForcedVisible() {
        return this.stellarCore$lastTime > System.currentTimeMillis();
    }

    @Override
    public void setCulled(boolean value) {
        this.stellarCore$culled = value;
        if (!value) {
            setTimeout();
        }
    }

    @Override
    public boolean isCulled() {
        return this.stellarCore$culled;
    }

    @Override
    public void setOutOfCamera(boolean value) {
        this.stellarCore$outOfCamera = value;
    }

    @Override
    public boolean isOutOfCamera() {
        return this.stellarCore$outOfCamera;
    }
}
