package nl.worldmorph.data;
import java.util.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class NpcPersistentState extends SavedData {
 public record MemoryData(String type,String target,long tick,int importance){} public record RelationshipData(String otherId,int score){}
 public record NpcData(String id,String name,int age,int money,int ambition,int loyalty,boolean alive,String job,String familyId,String settlementId,String kingdomId,String personality,List<MemoryData> memories,List<RelationshipData> relationships){}
 private static final Codec<RelationshipData> RELATIONSHIP_CODEC=RecordCodecBuilder.create(i->i.group(Codec.STRING.fieldOf("otherId").forGetter(RelationshipData::otherId),Codec.INT.fieldOf("score").forGetter(RelationshipData::score)).apply(i,RelationshipData::new));
 private static final Codec<MemoryData> MEMORY_CODEC=RecordCodecBuilder.create(i->i.group(Codec.STRING.fieldOf("type").forGetter(MemoryData::type),Codec.STRING.fieldOf("target").forGetter(MemoryData::target),Codec.LONG.fieldOf("tick").forGetter(MemoryData::tick),Codec.INT.fieldOf("importance").forGetter(MemoryData::importance)).apply(i,MemoryData::new));
 public static final Codec<NpcData> NPC_CODEC=RecordCodecBuilder.create(i->i.group(Codec.STRING.fieldOf("id").forGetter(NpcData::id),Codec.STRING.fieldOf("name").forGetter(NpcData::name),Codec.INT.fieldOf("age").forGetter(NpcData::age),Codec.INT.fieldOf("money").forGetter(NpcData::money),Codec.INT.fieldOf("ambition").forGetter(NpcData::ambition),Codec.INT.fieldOf("loyalty").forGetter(NpcData::loyalty),Codec.BOOL.fieldOf("alive").forGetter(NpcData::alive),Codec.STRING.fieldOf("job").forGetter(NpcData::job),Codec.STRING.fieldOf("familyId").forGetter(NpcData::familyId),Codec.STRING.fieldOf("settlementId").forGetter(NpcData::settlementId),Codec.STRING.fieldOf("kingdomId").forGetter(NpcData::kingdomId),Codec.STRING.fieldOf("personality").forGetter(NpcData::personality),MEMORY_CODEC.listOf().fieldOf("memories").forGetter(NpcData::memories),RELATIONSHIP_CODEC.listOf().optionalFieldOf("relationships",List.of()).forGetter(NpcData::relationships)).apply(i,NpcData::new));
 private static final Codec<NpcPersistentState> CODEC=RecordCodecBuilder.create(i->i.group(NPC_CODEC.listOf().fieldOf("npcs").forGetter(s->new ArrayList<>(s.npcs.values()))).apply(i,NpcPersistentState::new));
 public static final SavedDataType<NpcPersistentState> TYPE=new SavedDataType<>(Identifier.fromNamespaceAndPath("worldmorph","npcs"),NpcPersistentState::new,CODEC,null);
 private final Map<UUID,NpcData> npcs=new LinkedHashMap<>();
 public NpcPersistentState(){}
 private NpcPersistentState(List<NpcData> list){for(NpcData n:list)try{npcs.put(UUID.fromString(n.id()),n);}catch(IllegalArgumentException ignored){}}
 public List<NpcData> all(){return List.copyOf(npcs.values());}
 public void replaceAll(Collection<NpcData> data){npcs.clear();for(NpcData n:data)try{npcs.put(UUID.fromString(n.id()),n);}catch(IllegalArgumentException ignored){}setDirty();}
}