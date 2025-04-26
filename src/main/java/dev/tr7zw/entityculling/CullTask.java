package dev.tr7zw.entityculling;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.logisticscraft.occlusionculling.OcclusionCullingInstance;
import com.logisticscraft.occlusionculling.util.Vec3d;
import dev.tr7zw.entityculling.versionless.access.Cullable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;

import java.util.List;
import java.util.concurrent.*;

public class CullTask implements Runnable {

    private volatile boolean requestCull = false;
    private volatile boolean scheduleNext = true;
    private volatile boolean inited = false;

    private final OcclusionCullingInstance culling;
    private final EntityPlayer checkTarget;

    private final int hitboxLimit;

    public long lastCheckedTime = 0;

    // reused preallocated vars
    private final Vec3d lastPos = new Vec3d(0, 0, 0);
    private final Vec3d aabbMin = new Vec3d(0, 0, 0);
    private final Vec3d aabbMax = new Vec3d(0, 0, 0);

    private static final Executor backgroundWorker = Executors.newCachedThreadPool(
            new ThreadFactoryBuilder()
                    .setNameFormat("Raytrace Entity Tracker Thread - %d")
                    .setDaemon(true)
                    .build()
    );

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(
            Runtime.getRuntime().availableProcessors() / 2,
            new ThreadFactoryBuilder()
                    .setNameFormat("Raytrace Entity Tracker Scheduler Thread - %d")
                    .setDaemon(true)
                    .build()
    );

    private final Executor worker;

    public CullTask(
            OcclusionCullingInstance culling,
            EntityPlayer checkTarget,
            int hitboxLimit,
            long checkIntervalMs
    ) {
        this.culling = culling;
        this.checkTarget = checkTarget;
        this.hitboxLimit = hitboxLimit;
        this.worker = command -> scheduler.schedule(() -> {
            try {
                backgroundWorker.execute(command);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, checkIntervalMs, TimeUnit.MILLISECONDS);
    }

    public void requestCullSignal() {
        this.requestCull = true;
    }

    public void signalStop() {
        this.scheduleNext = false;
    }

    public void setup() {
        if (!this.inited)
            this.inited = true;
        else
            return;
        this.worker.execute(this);
    }

    @Override
    public void run() {
        try {
            if (this.checkTarget.ticksExisted > 10) {
                // getEyePosition can use a fixed delta as its debug only anyway
                net.minecraft.util.math.Vec3d cameraMC = this.checkTarget.getPositionEyes(0);
                if (requestCull || !(cameraMC.x == lastPos.x && cameraMC.y == lastPos.y && cameraMC.z == lastPos.z)) {
                    long start = System.currentTimeMillis();

                    requestCull = false;

                    lastPos.set(cameraMC.x, cameraMC.y, cameraMC.z);
                    culling.resetCache();

                    cullEntities(cameraMC, lastPos);

                    lastCheckedTime = (System.currentTimeMillis() - start);
                }
            }
        } finally {
            if (this.scheduleNext) {
                this.worker.execute(this);
            }
        }
    }

    private void cullEntities(net.minecraft.util.math.Vec3d cameraMC, Vec3d camera) {
        List<Entity> copy = ImmutableList.copyOf(this.checkTarget.getEntityWorld().loadedEntityList);
        for (Entity entity : copy) {
            if (!(entity instanceof Cullable cullable)) {
                continue; // Not sure how this could happen outside from mixin screwing up the inject into
                // Entity
            }

//            if (entity.getType().skipRaytracningCheck) {
//                continue;
//            }

            if (!cullable.isForcedVisible()) {
                if (entity.isGlowing() || isSkippableArmorstand(entity)) {
                    cullable.setCulled(false);
                    continue;
                }

                if (entity.getPositionVector().squareDistanceTo(cameraMC) > 64 * 64) {
                    cullable.setCulled(false); // If your entity view distance is larger than tracingDistance just
                    // render it
                    continue;
                }

                AxisAlignedBB boundingBox = entity.getEntityBoundingBox();
                if (boundingBox.maxX - boundingBox.minX > hitboxLimit || boundingBox.maxY - boundingBox.minY > hitboxLimit
                        || boundingBox.maxZ - boundingBox.minZ > hitboxLimit) {
                    cullable.setCulled(false); // To big to bother to cull
                    continue;
                }

                aabbMin.set(boundingBox.minX, boundingBox.minY, boundingBox.minZ);
                aabbMax.set(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ);

                boolean visible = culling.isAABBVisible(aabbMin, aabbMax, camera);

                cullable.setCulled(!visible);
            }
        }
    }

    private boolean isSkippableArmorstand(Entity entity) {
        return entity instanceof EntityArmorStand && entity.isInvisible();
    }
}