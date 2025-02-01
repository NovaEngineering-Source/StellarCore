package github.kasuminova.stellarcore.mixin.draconicevolution;

import com.brandon3055.brandonscore.lib.PairXZ;
import com.brandon3055.brandonscore.registry.ModFeatureParser;
import com.brandon3055.brandonscore.utils.SimplexNoise;
import com.brandon3055.brandonscore.utils.Utils;
import com.brandon3055.draconicevolution.DEConfig;
import com.brandon3055.draconicevolution.DEFeatures;
import com.brandon3055.draconicevolution.blocks.tileentity.TileChaosCrystal;
import com.brandon3055.draconicevolution.world.ChaosWorldGenHandler;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Random;

import static com.brandon3055.draconicevolution.world.ChaosWorldGenHandler.getClosestChaosSpawn;

@Mixin(ChaosWorldGenHandler.class)
public class MixinChaosWorldGenHandler {

    /**
     * @author sddsd2332
     * @reason 区块生成 与高版本一致
     */
    @Overwrite(remap = false)
    public static void generateChunk(World world, int chunkX, int chunkZ, PairXZ<Integer, Integer> islandCenter, Random random) {
        PairXZ<Integer, Integer> closestSpawn = islandCenter == null ? getClosestChaosSpawn(chunkX, chunkZ) : islandCenter;
        if (closestSpawn.x == 0 && closestSpawn.z == 0) {
            return;
        }
        int posX = chunkX * 16;
        int posZ = chunkZ * 16;
        int copyStartDistance = 180;
        if (Math.abs(posX - closestSpawn.x) > copyStartDistance || Math.abs(posZ - closestSpawn.z) > copyStartDistance) {
            return;
        }


        if (closestSpawn.x > posX && closestSpawn.x <= posX + 16 && closestSpawn.z > posZ && closestSpawn.z <= posZ + 16) {
            generateStructures(world, closestSpawn, random);
        }

        if (!DEConfig.chaosIslandVoidMode) {
            for (int trueX = posX; trueX < posX + 16; trueX++) {
                for (int y = 0; y < 255; y++) {
                    for (int trueZ = posZ; trueZ < posZ + 16; trueZ++) {
                        int x = trueX - closestSpawn.x;
                        int z = trueZ - closestSpawn.z;
                        int size = 80;
                        double dist = Math.sqrt(x * x + (y - 16) * (y - 16) + z * z);
                        double xd, yd, zd;
                        double density, centerFalloff, plateauFalloff, heightMapFalloff;

                        xd = (double) x / size;
                        yd = (double) y / (32);
                        zd = (double) z / size;

                        //Calculate Center Falloff
                        centerFalloff = 1D / (dist * 0.05D);
                        if (centerFalloff < 0) centerFalloff = 0;

                        //Calculate Plateau Falloff
                        if (yd < 0.4D) {
                            plateauFalloff = yd * 2.5D;
                        } else if (yd <= 0.6D) {
                            plateauFalloff = 1D;
                        } else if (yd > 0.6D && yd < 1D) {
                            plateauFalloff = 1D - (yd - 0.6D) * 2.5D;
                        } else {
                            plateauFalloff = 0;
                        }

                        //Trim Further calculations
                        if (plateauFalloff == 0 || centerFalloff == 0) {
                            continue;
                        }

                        //Calculate heightMapFalloff
                        heightMapFalloff = 0;
                        for (int octave = 1; octave < 5; octave++) {
                            heightMapFalloff += ((SimplexNoise.noise(xd * octave + closestSpawn.x, zd * octave + closestSpawn.z) + 1) * 0.5D) * 0.01D * (octave * 10D * 1 - (dist * 0.001D));
                        }
                        if (heightMapFalloff <= 0) {
                            heightMapFalloff = 0;
                        }
                        heightMapFalloff += ((0.5D - Math.abs(yd - 0.5D)) * 0.15D);
                        if (heightMapFalloff == 0) {
                            continue;
                        }

                        density = centerFalloff * plateauFalloff * heightMapFalloff;

                        BlockPos pos = new BlockPos(x + closestSpawn.x, y + 64 + DEConfig.chaosIslandYOffset, z + closestSpawn.z);
                        if (density > 0.1 && (world.isAirBlock(pos) && world.getBlockState(pos).getBlock() != DEFeatures.chaosShardAtmos)) {
                            world.setBlockState(pos, (dist > 60 || dist > random.nextInt(60)) ? Blocks.END_STONE.getDefaultState() : Blocks.OBSIDIAN.getDefaultState());
                        }
                    }
                }
            }
        }
    }

    /**
     * @author sddsd2332
     * @reason 生成核心部分
     */
    @Overwrite(remap = false)
    public static void generateStructures(World world, PairXZ<Integer, Integer> islandCenter, Random random) {
        int outerRadius = 330;

        //Gen Chaos Cavern
        int shardY = 80 + DEConfig.chaosIslandYOffset;
        int coreHeight = 10;
        int coreWidth = 20;
        for (int y = shardY - coreHeight; y <= shardY + coreHeight; y++) {
            int h = Math.abs(y - shardY);
            int inRadius = h - 3;
            double yp = (coreHeight - h) / (double) coreHeight;
            int outRadius = (int) (yp * coreWidth);
            outRadius -= (outRadius * outRadius) / 100;

            genCoreSlice(world, islandCenter.x, y, islandCenter.z, inRadius, shardY, coreWidth, true, random);
            genCoreSlice(world, islandCenter.x, y, islandCenter.z, outRadius, shardY, coreWidth, false, random);
        }
        BlockPos center = new BlockPos(islandCenter.x, shardY, islandCenter.z);

        if (ModFeatureParser.isEnabled(DEFeatures.chaosCrystal)) {
            world.setBlockState(center, DEFeatures.chaosCrystal.getDefaultState());
            TileChaosCrystal tileChaosShard = (TileChaosCrystal) world.getTileEntity(center);
            tileChaosShard.setLockPos();
        }

    }

    /**
     * @author sddsd2332
     * @reason 生成核心外部黑曜石
     */
    @Overwrite(remap = false)
    public static void genCoreSlice(World world, int xi, int yi, int zi, int ringRadius, int yc, int coreRadious, boolean fillIn, Random rand) {
        if (DEConfig.chaosIslandVoidMode) return;
        for (int x = xi - coreRadious; x <= xi + coreRadious; x++) {
            for (int z = zi - coreRadious; z <= zi + coreRadious; z++) {
                double dist = Utils.getDistanceAtoB(x, yi, z, xi, yc, zi);
                double oRad = coreRadious - (Math.abs(yc - yi) * Math.abs(yc - yi)) / 10;
                if (dist > oRad - 3D && rand.nextDouble() * 3D < dist - (oRad - 3D)) {
                    continue;
                }
                if (fillIn && (int) (Utils.getDistanceAtoB(x, z, xi, zi)) <= ringRadius) {
                    if ((int) dist < 9)
                        world.setBlockState(new BlockPos(x, yi, z), DEFeatures.infusedObsidian.getDefaultState());
                    else world.setBlockState(new BlockPos(x, yi, z), Blocks.OBSIDIAN.getDefaultState());
                } else if (!fillIn && (int) (Utils.getDistanceAtoB(x, z, xi, zi)) >= ringRadius) {
                    world.setBlockState(new BlockPos(x, yi, z), Blocks.OBSIDIAN.getDefaultState());
                } else if (!fillIn && (int) Utils.getDistanceAtoB(x, z, xi, zi) <= ringRadius) {
                    Block b = world.getBlockState(new BlockPos(x, yi, z)).getBlock();
                    if (b == Blocks.AIR || b == Blocks.END_STONE || b == Blocks.OBSIDIAN)
                        world.setBlockState(new BlockPos(x, yi, z), DEFeatures.chaosShardAtmos.getDefaultState());
                }

            }
        }
    }
}
