package nl.worldmorph.data;
import java.util.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
public final class NeedsPersistentState extends SavedData {
 public record NeedData(String settlement,int food,int housing,int safety,int jobs,int trade){}
 private static final Codec<NeedData> NEED_CODEC=RecordCodecBuilder.create(i->i.group(Codec.STRING.fieldOf("settlement").forGetter(NeedData::settlement),Codec.INT.fieldOf("food").forGetter(NeedData::food),Codec.INT.fieldOf("housing").forGetter(NeedData::housing),Codec.INT.fieldOf("safety").forGetter(NeedData::safety),Codec.INT.fieldOf("jobs").forGetter(NeedData::jobs),Codec.INT.fieldOf("trade").forGetter(NeedData::trade)).apply(i,NeedData::new));
 private static final Codec<NeedsPersistentState> CODEC=RecordCodecBuilder.create(i->i.group(NEED_CODEC.listOf().fieldOf("needs").forGetter(s->new ArrayList<>(s.data.values()))).apply(i,NeedsPersistentState::new));
 public static final SavedDataType<NeedsPersistentState> TYPE=new SavedDataType<>(Identifier.fromNamespaceAndPath("worldmorph","needs"),NeedsPersistentState::new,CODEC,null);
 private final Map<UUID,NeedData> data=new LinkedHashMap<>();
 public NeedsPersistentState(){}
 private NeedsPersistentState(List<NeedData> list){for(NeedData n:list)try{data.put(UUID.fromString(n.settlement()),n);}catch(IllegalArgumentException ignored){}}
 public List<NeedData> all(){return List.copyOf(data.values());}
 public void replaceAll(Collection<NeedData> values){data.clear();for(NeedData n:values)try{data.put(UUID.fromString(n.settlement()),n);}catch(IllegalArgumentException ignored){}setDirty();}
}