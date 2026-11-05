package ru.realisticsurvival.world;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import ru.realisticsurvival.core.config.SurvivalConfig;
import ru.realisticsurvival.item.MedicalItems;

public final class AbandonedSites {
    private static final Direction[] FACING={Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST};
    private AbandonedSites() {}

    public static void register() {
        ServerChunkEvents.CHUNK_LOAD.register((level,chunk,generated)->{
            if(!generated || !level.dimension().equals(Level.OVERWORLD)
                    || !SurvivalConfig.getBoolean("world.sites.enabled",true)) return;
            generate(level,chunk);
        });
    }

    private static void generate(ServerLevel level, LevelChunk chunk) {
        ChunkPos cp=chunk.getPos();
        RandomSource r=RandomSource.create(level.getSeed() ^ (cp.toLong()*0xC2B2AE3D27D4EB4FL) ^ 0xA11CEL);
        if(r.nextInt(100)>=SurvivalConfig.getInt("world.sites.chance",2,0,100)) return;
        int x=cp.getMinBlockX()+8,z=cp.getMinBlockZ()+8;
        BlockPos c=NatureGenerator.surface(level,x,z);
        if(c==null) return;
        for(int[] o:new int[][]{{-5,-5},{5,-5},{-5,5},{5,5}}) {
            BlockPos p=NatureGenerator.surface(level,x+o[0],z+o[1]);
            if(p==null || Math.abs(p.getY()-c.getY())>2) return;
        }
        switch(r.nextInt(4)) {
            case 0 -> camp(level,c,r);
            case 1 -> hut(level,c,r);
            case 2 -> post(level,c,r);
            default -> cache(level,c,r);
        }
    }

    private static void camp(ServerLevel l,BlockPos c,RandomSource r) {
        put(l,c,Blocks.CAMPFIRE.defaultBlockState().setValue(BlockStateProperties.LIT,false));
        put(l,c.east(2),Blocks.OAK_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS,Direction.Axis.Z));
        put(l,c.west(2),Blocks.OAK_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS,Direction.Axis.Z));
        BlockPos tent=c.north(4);
        for(int dx=-1;dx<=1;dx++) {
            for(int dz=-1;dz<=1;dz++) support(l,tent.offset(dx,0,dz));
            BlockState wool=(r.nextBoolean()?Blocks.GREEN_WOOL:Blocks.BROWN_WOOL).defaultBlockState();
            put(l,tent.offset(dx,1,-1),wool); put(l,tent.offset(dx,2,0),wool); put(l,tent.offset(dx,1,1),wool);
        }
        chest(l,tent,Direction.SOUTH,r,false);
        if(r.nextBoolean()) put(l,tent.east(),Blocks.COBWEB.defaultBlockState());
    }

    private static void hut(ServerLevel l,BlockPos c,RandomSource r) {
        for(int dx=-2;dx<=2;dx++) for(int dz=-2;dz<=2;dz++) {
            BlockPos floor=c.offset(dx,-1,dz);
            if(r.nextInt(100)<60 && NatureGenerator.ground(l.getBlockState(floor)))
                l.setBlock(floor,Blocks.OAK_PLANKS.defaultBlockState(),2);
            if(Math.abs(dx)!=2 && Math.abs(dz)!=2) continue;
            if(dx==0 && dz==-2) continue;
            boolean corner=Math.abs(dx)==2&&Math.abs(dz)==2;
            int h=corner?3:1+r.nextInt(3);
            for(int y=0;y<h;y++) if(r.nextInt(100)>=15)
                put(l,c.offset(dx,y,dz),(corner?Blocks.OAK_LOG: r.nextBoolean()?Blocks.MOSSY_COBBLESTONE:Blocks.OAK_PLANKS).defaultBlockState());
        }
        chest(l,c.offset(-1,0,1),FACING[r.nextInt(4)],r,false);
        if(r.nextBoolean()) put(l,c.offset(1,0,1),Blocks.COBWEB.defaultBlockState());
    }

    private static void post(ServerLevel l,BlockPos c,RandomSource r) {
        for(int dx=-3;dx<=3;dx++) for(int dz=-3;dz<=3;dz++) {
            if(Math.max(Math.abs(dx),Math.abs(dz))!=3 || dx==0 || dz==0) continue;
            for(int y=0;y<1+r.nextInt(2);y++) put(l,c.offset(dx,y,dz),Blocks.POLISHED_ANDESITE.defaultBlockState());
        }
        put(l,c,Blocks.CAMPFIRE.defaultBlockState().setValue(BlockStateProperties.LIT,false));
        put(l,c.offset(1,0,1),Blocks.BARREL.defaultBlockState());
        chest(l,c.offset(-1,0,-1),Direction.SOUTH,r,true);
        put(l,c.offset(-2,0,2),Blocks.OAK_FENCE.defaultBlockState());
        put(l,c.offset(-2,1,2),Blocks.OAK_FENCE.defaultBlockState());
    }

    private static void cache(ServerLevel l,BlockPos c,RandomSource r) {
        put(l,c.east(),Blocks.BARREL.defaultBlockState()); put(l,c.west(),Blocks.BARREL.defaultBlockState());
        chest(l,c,FACING[r.nextInt(4)],r,r.nextInt(100)<40);
        for(int dx=-2;dx<=2;dx++) for(int dz=-2;dz<=2;dz++)
            if(Math.max(Math.abs(dx),Math.abs(dz))==2 && NatureGenerator.ground(l.getBlockState(c.offset(dx,-1,dz)))
                    && r.nextInt(100)<60) l.setBlock(c.offset(dx,-1,dz),Blocks.COARSE_DIRT.defaultBlockState(),2);
    }

    private static void put(ServerLevel l,BlockPos p,BlockState s) {
        if(NatureGenerator.replace(l.getBlockState(p))) l.setBlock(p,s,2);
    }
    private static void support(ServerLevel l,BlockPos p) {
        BlockPos b=p.below();
        for(int i=0;i<3;i++,b=b.below()) if(NatureGenerator.replace(l.getBlockState(b)))
            l.setBlock(b,Blocks.DIRT.defaultBlockState(),2);
    }
    private static void chest(ServerLevel l,BlockPos p,Direction face,RandomSource r,boolean military) {
        l.setBlock(p,Blocks.CHEST.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,face),2);
        if(!(l.getBlockEntity(p) instanceof ChestBlockEntity chest)) return;
        for(int i=0,n=3+r.nextInt(4);i<n;i++) chest.setItem(r.nextInt(27),loot(r,military));
        chest.setChanged();
    }
    private static ItemStack loot(RandomSource r,boolean military) {
        int n=r.nextInt(100);
        if(military) {
            if(n<25)return new ItemStack(Items.IRON_INGOT,2+r.nextInt(4));
            if(n<45)return new ItemStack(MedicalItems.BANDAGE,1+r.nextInt(3));
            if(n<60)return new ItemStack(Items.COOKED_BEEF,2+r.nextInt(3));
            if(n<75)return new ItemStack(Items.ARROW,8+r.nextInt(9));
            if(n<90)return new ItemStack(Items.COAL,2+r.nextInt(5));
            if(n<98)return new ItemStack(Items.EMERALD,1+r.nextInt(2));
            return new ItemStack(Items.GOLDEN_APPLE);
        }
        if(n<20)return new ItemStack(Items.BREAD,1+r.nextInt(3));
        if(n<35)return new ItemStack(Items.APPLE,1+r.nextInt(3));
        if(n<45)return new ItemStack(MedicalItems.BANDAGE,1+r.nextInt(2));
        if(n<60)return new ItemStack(Items.TORCH,2+r.nextInt(5));
        if(n<75)return new ItemStack(Items.STRING,1+r.nextInt(4));
        if(n<90)return new ItemStack(Items.COAL,2+r.nextInt(4));
        return new ItemStack(Items.LEATHER,1+r.nextInt(3));
    }
}