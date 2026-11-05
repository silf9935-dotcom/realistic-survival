package ru.realisticsurvival.world;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import ru.realisticsurvival.core.config.SurvivalConfig;

public final class NatureGenerator {
    private static final Direction[] HORIZONTAL = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
    private NatureGenerator() {}

    public static void register() {
        ServerChunkEvents.CHUNK_GENERATE.register((level, chunk) -> {
            if (!level.dimension().equals(Level.OVERWORLD)
                    || !SurvivalConfig.getBoolean("world.nature.enabled", true)) return;
            decorate(level, chunk);
        });
    }

    private static void decorate(ServerLevel level, LevelChunk chunk) {
        ChunkPos cp = chunk.getPos();
        RandomSource random = RandomSource.create(level.getSeed() ^ (cp.toLong() * 0x9E3779B97F4A7C15L) ^ 0x5EEDL);
        int x = cp.getMinBlockX(), z = cp.getMinBlockZ();
        if (random.nextInt(100) < SurvivalConfig.getInt("world.nature.stump_chance", 20, 0, 100)) {
            BlockPos p = surface(level, x + 4 + random.nextInt(8), z + 4 + random.nextInt(8));
            if (p != null) stump(level, p, random);
        }
        if (random.nextInt(100) < SurvivalConfig.getInt("world.nature.dead_tree_chance", 8, 0, 100)) {
            BlockPos p = surface(level, x + 4 + random.nextInt(8), z + 4 + random.nextInt(8));
            if (p != null) deadTree(level, p, random);
        }
        if (random.nextInt(100) < SurvivalConfig.getInt("world.nature.boulder_chance", 14, 0, 100)) {
            BlockPos p = surface(level, x + 4 + random.nextInt(8), z + 4 + random.nextInt(8));
            if (p != null) boulder(level, p, random);
        }
    }

    private static boolean forest(Holder<Biome> b) { return b.is(BiomeTags.IS_FOREST); }
    private static boolean taiga(Holder<Biome> b) { return b.is(BiomeTags.IS_TAIGA); }
    private static boolean hills(Holder<Biome> b) { return b.is(BiomeTags.IS_HILL) || b.is(BiomeTags.IS_MOUNTAIN); }

    private static void stump(ServerLevel level, BlockPos p, RandomSource r) {
        Holder<Biome> biome = level.getBiome(p);
        if (!forest(biome) && !taiga(biome)) return;
        Block log = taiga(biome) ? Blocks.SPRUCE_LOG : (r.nextBoolean() ? Blocks.OAK_LOG : Blocks.BIRCH_LOG);
        level.setBlock(p, log.defaultBlockState(), 2);
        if (r.nextInt(100) < 45 && replace(level.getBlockState(p.above())))
            level.setBlock(p.above(), log.defaultBlockState(), 2);
    }

    private static void deadTree(ServerLevel level, BlockPos p, RandomSource r) {
        Holder<Biome> biome = level.getBiome(p);
        if (!forest(biome) && !taiga(biome)) return;
        Block log = taiga(biome) ? Blocks.SPRUCE_LOG : Blocks.OAK_LOG;
        int height = 4 + r.nextInt(4), built = 0;
        for (int i = 0; i < height && replace(level.getBlockState(p.above(i))); i++) {
            level.setBlock(p.above(i), log.defaultBlockState(), 2); built++;
        }
        if (built < 3) return;
        for (int i = 2; i < built; i++) {
            if (r.nextInt(100) >= 35) continue;
            Direction d = HORIZONTAL[r.nextInt(4)];
            BlockPos branch = p.above(i).relative(d);
            if (replace(level.getBlockState(branch)))
                level.setBlock(branch, log.defaultBlockState().setValue(BlockStateProperties.AXIS, d.getAxis()), 2);
        }
        if (level.getBlockState(p.below()).is(Blocks.GRASS_BLOCK) && r.nextInt(100) < 60)
            level.setBlock(p.below(), Blocks.COARSE_DIRT.defaultBlockState(), 2);
    }

    private static void boulder(ServerLevel level, BlockPos p, RandomSource r) {
        Holder<Biome> biome = level.getBiome(p);
        Block[] palette;
        int radius;
        if (hills(biome)) {
            palette = new Block[]{Blocks.STONE, Blocks.ANDESITE, Blocks.COBBLESTONE, Blocks.DIORITE};
            radius = 1 + r.nextInt(3);
        } else if (taiga(biome) || forest(biome)) {
            palette = new Block[]{Blocks.MOSSY_COBBLESTONE, Blocks.COBBLESTONE, Blocks.ANDESITE};
            radius = 1 + r.nextInt(2);
        } else return;
        for (int dx=-radius; dx<=radius; dx++) for (int dz=-radius; dz<=radius; dz++)
            for (int dy=-1; dy<=radius; dy++) {
                if (dx*dx + dy*dy*1.96 + dz*dz > radius*radius + 0.5 || r.nextInt(100) >= 88) continue;
                BlockPos target=p.offset(dx,dy,dz); BlockState old=level.getBlockState(target);
                if ((dy<0 ? ground(old) : replace(old)))
                    level.setBlock(target, palette[r.nextInt(palette.length)].defaultBlockState(), 2);
            }
    }

    static BlockPos surface(ServerLevel level, int x, int z) {
        BlockPos p = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z), z);
        if (!ground(level.getBlockState(p.below())) || !replace(level.getBlockState(p))
                || !level.getFluidState(p).isEmpty() || !level.getWorldBorder().isWithinBounds(p)) return null;
        return p;
    }
    static boolean ground(BlockState s) {
        return s.is(Blocks.GRASS_BLOCK)||s.is(Blocks.DIRT)||s.is(Blocks.PODZOL)||s.is(Blocks.COARSE_DIRT)
                ||s.is(Blocks.STONE)||s.is(Blocks.SAND)||s.is(Blocks.RED_SAND)||s.is(Blocks.GRAVEL);
    }
    static boolean replace(BlockState s) { return s.isAir() || s.canBeReplaced(); }
}