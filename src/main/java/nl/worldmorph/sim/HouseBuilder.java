package nl.worldmorph.sim;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
public final class HouseBuilder {
 public boolean buildStarterHouse(ServerLevel level,BlockPos origin){
  BlockPos base=origin.offset(2,0,2);
  for(int x=0;x<5;x++)for(int z=0;z<5;z++)for(int y=0;y<4;y++){
   BlockPos p=base.offset(x,y,z);if(!level.getBlockState(p).isAir()&&!level.getBlockState(p).canBeReplaced())return false;
  }
  BlockState floor=Blocks.OAK_PLANKS.defaultBlockState(),wall=Blocks.OAK_PLANKS.defaultBlockState(),log=Blocks.OAK_LOG.defaultBlockState(),glass=Blocks.GLASS_PANE.defaultBlockState(),roof=Blocks.SPRUCE_PLANKS.defaultBlockState();
  for(int x=0;x<5;x++)for(int z=0;z<5;z++)level.setBlock(base.offset(x,0,z),floor,3);
  for(int y=1;y<=3;y++)for(int x=0;x<5;x++)for(int z=0;z<5;z++){
   boolean edge=x==0||x==4||z==0||z==4;if(!edge)continue;
   BlockPos p=base.offset(x,y,z);boolean corner=(x==0||x==4)&&(z==0||z==4);
   if(y==1&&x==2&&z==0)continue;
   if(y==2&&((x==0||x==4)&&z==2||(z==0||z==4)&&x==2)){level.setBlock(p,glass,3);continue;}
   level.setBlock(p,corner?log:wall,3);
  }
  for(int x=0;x<5;x++)for(int z=0;z<5;z++)level.setBlock(base.offset(x,4,z),roof,3);
  return true;
 }
}