package ru.realisticsurvival.npc;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import ru.realisticsurvival.core.config.SurvivalConfig;

public final class CampGenerator {
    private static final String TAG="rs_territory";
    private CampGenerator() {}
    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(level -> {
            if(!level.dimension().equals(Level.OVERWORLD)||level.getDifficulty()==Difficulty.PEACEFUL
                    ||!SurvivalConfig.getBoolean("world.npc.auto_camps_enabled",true)
                    ||!SurvivalConfig.getBoolean("world.npc.territories_enabled",true)) return;
            int interval=SurvivalConfig.getInt("world.npc.auto_camp_interval_seconds",60,10,3600);
            if(level.getGameTime()%(interval*20L)!=0) return;
            int chance=SurvivalConfig.getInt("world.npc.auto_camp_chance",25,0,100);
            for(ServerPlayer player:level.players()) if(!player.isSpectator()&&level.random.nextInt(100)<chance)
                tryCreate(level,player);
        });
    }
    private static void tryCreate(ServerLevel level,ServerPlayer player) {
        int spacing=SurvivalConfig.getInt("world.npc.auto_camp_spacing",160,64,512);
        BlockPos origin=player.blockPosition();
        for(int i=0;i<8;i++) {
            double a=level.random.nextDouble()*Math.PI*2;
            int d=40+level.random.nextInt(41);
            BlockPos probe=new BlockPos(origin.getX()+(int)Math.round(Math.cos(a)*d),origin.getY(),
                    origin.getZ()+(int)Math.round(Math.sin(a)*d));
            if(!level.hasChunkAt(probe)) continue;
            BlockPos pos=level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,probe);
            if(!level.getBlockState(pos).isAir()||!level.getBlockState(pos.above()).isAir()
                    ||!level.getBlockState(pos.below()).isSolid()||!level.getBlockState(pos.below()).getFluidState().isEmpty()
                    ||!level.getWorldBorder().isWithinBounds(pos)) continue;
            if(!level.getEntitiesOfClass(ArmorStand.class,new AABB(pos).inflate(spacing),
                    e->e.getTags().contains(TAG)).isEmpty()) continue;
            ArmorStand marker=EntityType.ARMOR_STAND.spawn(level,pos,EntitySpawnReason.EVENT);
            if(marker==null) continue;
            boolean bandits=level.random.nextInt(100)<60;
            marker.addTag(TAG); marker.addTag(bandits?"rs_territory_start_bandits":"rs_territory_start_military");
            marker.addTag("rs_auto_camp"); marker.setInvisible(true); marker.setNoGravity(true); marker.setInvulnerable(true);
            if(bandits) {
                BlockPos fire=pos.east(2);
                if(level.getBlockState(fire).isAir()&&level.getBlockState(fire.below()).isSolid())
                    level.setBlock(fire,Blocks.CAMPFIRE.defaultBlockState(),3);
            }
            return;
        }
    }
}