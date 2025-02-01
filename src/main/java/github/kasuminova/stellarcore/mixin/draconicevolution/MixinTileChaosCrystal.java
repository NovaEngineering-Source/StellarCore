package github.kasuminova.stellarcore.mixin.draconicevolution;

import com.brandon3055.brandonscore.blocks.TileBCBase;
import com.brandon3055.brandonscore.lib.datamanager.ManagedBool;
import com.brandon3055.brandonscore.lib.datamanager.ManagedInt;
import com.brandon3055.brandonscore.lib.datamanager.ManagedLong;
import com.brandon3055.brandonscore.utils.Utils;
import com.brandon3055.draconicevolution.DEConfig;
import com.brandon3055.draconicevolution.DEFeatures;
import com.brandon3055.draconicevolution.blocks.tileentity.TileChaosCrystal;
import com.brandon3055.draconicevolution.entity.EntityChaosGuardian;
import com.brandon3055.draconicevolution.entity.EntityGuardianCrystal;
import com.brandon3055.draconicevolution.lib.DESoundHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Mixin(TileChaosCrystal.class)
public abstract class MixinTileChaosCrystal extends TileBCBase implements ITickable {

    @Shadow(remap = false)
    public int tick;
    @Final
    @Shadow(remap = false)
    private ManagedLong posLock;

    @Final
    @Shadow(remap = false)
    private ManagedInt dimLock;

    @Final
    @Shadow(remap = false)
    public ManagedBool guardianDefeated;

    @Shadow(remap = false)
    boolean validateOldHash;
    @Shadow(remap = false)
    int oldhash;
    @Shadow(remap = false)
    private int soundTimer;

    @Unique
    public boolean isChunkGenerated = false;
    @Unique
    public boolean isChunkGeneratedOK = false;

    /**
     * @author sddsd2332
     * @reason 添加区块生成判断
     */
    @Overwrite(remap = false)
    public void setLockPos() {
        posLock.value = pos.toLong();
        dimLock.value = world.provider.getDimension();
        isChunkGenerated = true;
    }

    /**
     * @author sddsd2332
     * @reason 覆写原来的更新，用于添加混沌水晶和混沌龙
     */
    @Override
    @Overwrite(remap = false)
    public void update() {
        tick++;

        //Prevent existing crystals breaking due to update
        if (validateOldHash) {
            int hash = (pos.toString() + String.valueOf(world.provider.getDimension())).hashCode();
            if (hash == oldhash) {
                setLockPos();
            } else {
                world.setBlockToAir(pos);
            }
            validateOldHash = false;
        }

        if (isChunkGenerated && !isChunkGeneratedOK) {
            isChunkGeneratedOK = true;
        }


        if (tick > 1 && !world.isRemote && hasBeenMoved()) {
            world.setBlockToAir(pos);
        }

        if (world.isRemote && soundTimer-- <= 0) {
            soundTimer = 3600 + world.rand.nextInt(1200);
            world.playSound(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, DESoundHandler.chaosChamberAmbient, SoundCategory.AMBIENT, 1.5F, world.rand.nextFloat() * 0.4F + 0.8F, false);
        }

        if (!world.isRemote && guardianDefeated.value && world.rand.nextInt(50) == 0) {
            int x = 5 - world.rand.nextInt(11);
            int z = 5 - world.rand.nextInt(11);
            EntityLightningBolt bolt = new EntityLightningBolt(world, pos.getX() + x, world.getTopSolidOrLiquidBlock(pos.add(x, 0, z)).getY(), pos.getZ() + z, false);
            bolt.ignoreFrustumCheck = true;
            world.addWeatherEffect(bolt);
        }

        if (!world.isRemote && isChunkGeneratedOK) {
            if (tick % 20 == 0 && !checkRangePlayer) {
                isCheckRangePlayer();
            }

            if (checkRangePlayer && !SummoningPillars) {
                isSummoningPillars();
            }
            if (SummoningPillars && !SummoningDragon) {
                EntityChaosGuardian guardian = new EntityChaosGuardian(world);
                guardian.setPosition(getPos().getX(), getPos().getY(), getPos().getZ());
                guardian.homeY = getPos().getY();
                world.spawnEntity(guardian);
                SummoningDragon = true;
            }
        }

    }

    @Unique
    public boolean SummoningDragon = false;

    @Unique
    public boolean checkRangePlayer = false;

    //检查范围内是否有玩家
    @Unique
    public void isCheckRangePlayer() {
        BlockPos pos1 = new BlockPos(getPos().up(15).getX() + 15, getPos().up(15).getY() + 15, getPos().up(15).getZ() + 15);
        BlockPos pos2 = new BlockPos(getPos().down(15).getX() - 15, getPos().down(15).getY() - 15, getPos().down(15).getZ() - 15);
        AxisAlignedBB entitys = new AxisAlignedBB(pos1, pos2);

        List<Entity> entitiesToDie = getWorld().getEntitiesWithinAABB(Entity.class, entitys);
        for (Entity entity : entitiesToDie) {
            if (entity instanceof EntityPlayer player) {
                if (pos.up(15).distanceSq(player.posX, player.posY, player.posZ) <= 15 * 15) {
                    checkRangePlayer = true;
                    break;
                }
            }
        }
    }


    @Unique
    public boolean SummoningPillars = false;

    @Unique
    private int respawnStateTicks;

    //生成混沌水晶柱子
    @Unique
    public void isSummoningPillars() {
        int spawnRate = 15;
        int i = this.respawnStateTicks++;
        boolean spawn = i % spawnRate == 0;
        if (spawn) {
            BlockPos nextSpawn = getNextCrystalPos(i == 0);
            if (nextSpawn != null) {
                for (BlockPos blockpos : BlockPos.getAllInBox(nextSpawn.add(-10, -10, -10), nextSpawn.add(10, 10, 10))) {
                    world.setBlockToAir(blockpos);
                }
                generateObelisk(world, nextSpawn, world.rand);
                world.setBlockState(nextSpawn, DEFeatures.infusedObsidian.getDefaultState());
                EntityGuardianCrystal crystal = new EntityGuardianCrystal(world);
                crystal.setPosition(nextSpawn.getX() + 0.5, nextSpawn.getY() + 1, nextSpawn.getZ() + 0.5);
                world.spawnEntity(crystal);
            } else {
                SummoningPillars = true;
            }
        }
    }


    @Unique
    private List<BlockPos> crystalSpawnList;

    @Unique
    private List<BlockPos> crystalsPosCache;


    @Unique
    public List<BlockPos> getCrystalPositions() {
        if (crystalsPosCache == null) {
            crystalsPosCache = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                double rotation = i * 0.9D;
                int sX = getPos().getX() + (int) (Math.sin(rotation) * 45);
                int sZ = getPos().getZ() + (int) (Math.cos(rotation) * 45);
                crystalsPosCache.add(new BlockPos(sX, getPos().getY() + 40, sZ));
            }
            for (int i = 0; i < 14; i++) {
                double rotation = i * 0.45D;
                int sX = getPos().getX() + (int) (Math.sin(rotation) * 90);
                int sZ = getPos().getZ() + (int) (Math.cos(rotation) * 90);
                crystalsPosCache.add(new BlockPos(sX, getPos().getY() + 45, sZ));
            }

        }
        return crystalsPosCache;
    }

    @Unique
    public BlockPos getNextCrystalPos(boolean initial) {
        if (initial) {
            crystalSpawnList = new ArrayList<>(getCrystalPositions());
        }
        if (crystalSpawnList.isEmpty()) {
            return null;
        }
        return crystalSpawnList.remove(0);
    }

    @Unique
    private static void generateObelisk(World world, BlockPos genPos, Random rand) {
        for (int i = 0; i < 20; i += 3) {
            EntityLightningBolt entity = new EntityLightningBolt(world, genPos.getX() - 2 + rand.nextInt(5), genPos.getY() - rand.nextInt(20), genPos.getZ() - 2 + rand.nextInt(5), false);
            world.addWeatherEffect(entity);
        }
        if (DEConfig.chaosIslandVoidMode) return;

        int r = 3;
        BlockPos.getAllInBox(genPos.add(-r, -25, -r), genPos.add(r, 4, r)).forEach(pos -> {
            if (pos.getY() < genPos.getY()) {
                double pct = (double) (genPos.getY() - pos.getY()) / 25D;
                if (Utils.getDistanceAtoB(pos.getX(), pos.getZ(), genPos.getX(), genPos.getZ()) <= r + 0.5) {
                    if (1D - pct > rand.nextDouble()) {
                        float block = rand.nextFloat();
                        if (block < 0.1) {
                            world.setBlockState(new BlockPos(pos), DEFeatures.infusedObsidian.getDefaultState());
                        } else if (block < 0.4) {
                            world.setBlockState(new BlockPos(pos), Blocks.NETHER_BRICK.getDefaultState());
                        } else {
                            world.setBlockState(new BlockPos(pos), Blocks.OBSIDIAN.getDefaultState());
                        }
                    }
                }
            }
            int relY = pos.getY() - genPos.getY();
            int absRelX = Math.abs(pos.getX() - genPos.getX());
            int absRelZ = Math.abs(pos.getZ() - genPos.getZ());
            if ((absRelX == 2 || absRelZ == 2) && absRelX <= 2 && absRelZ <= 2 && relY < 4 && relY > -1) {
                world.setBlockState(pos, Blocks.IRON_BARS.getDefaultState());
            }
            if (relY == 4 && absRelX <= 2 && absRelZ <= 2) {
                world.setBlockState(pos, Blocks.STONE_SLAB.getStateFromMeta(6));
            }
        });

    }


    @Shadow(remap = false)
    private boolean hasBeenMoved() {
        return posLock.value != pos.toLong() || dimLock.value != world.provider.getDimension();
    }


}
