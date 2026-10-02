package nl.worldmorph.data;
import java.util.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
public final class HomePersistentState extends SavedData {
 public record HomeData(String npc,String hostPlayer,String settlement,BlockPos position,String type,long movedInTick){}
 private static final Codec<HomeData> HOME_CODEC=RecordCodecBuilder.create(i->i.group(Codec.STRING.fieldOf("npc").forGetter(HomeData::npc),Codec.STRING.fieldOf("hostPlayer").forGetter(HomeData::hostPlayer),Codec.STRING.fieldOf("settlement").forGetter(HomeData::settlement),BlockPos.CODEC.fieldOf("position").forGetter(HomeData::position),Codec.STRING.fieldOf("type").forGetter(HomeData::type),Codec.LONG.fieldOf("movedInTick").forGetter(HomeData::movedInTick)).apply(i,HomeData::new));
 private static final Codec<HomePersistentState> CODEC=RecordCodecBuilder.create(i->i.group(HOME_CODEC.listOf().fieldOf("homes").forGetter(s->new ArrayList<>(s.homes.values()))).apply(i,HomePersistentState::new));
 public static final SavedDataType<HomePersistentState> TYPE=new SavedDataType<>(Identifier.fromNamespaceAndPath("worldmorph","homes"),HomePersistentState::new,CODEC,null);
 private final Map<UUID,HomeData> homes=new LinkedHashMap<>();
 public HomePersistentState(){}
 private HomePersistentState(List<HomeData> list){for(HomeData h:list)try{homes.put(UUID.fromString(h.npc()),h);}catch(IllegalArgumentException ignored){}}
 public List<HomeData> all(){return List.copyOf(homes.values());}
 public void replaceAll(Collection<HomeData> list){homes.clear();for(HomeData h:list)try{homes.put(UUID.fromString(h.npc()),h);}catch(IllegalArgumentException ignored){}setDirty();}
}