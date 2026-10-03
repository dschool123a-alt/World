package nl.worldmorph.data;
import java.util.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
public final class EconomyPersistentState extends SavedData {
 public record PriceData(String item,int price){}
 private static final Codec<PriceData> PRICE_CODEC=RecordCodecBuilder.create(i->i.group(Codec.STRING.fieldOf("item").forGetter(PriceData::item),Codec.INT.fieldOf("price").forGetter(PriceData::price)).apply(i,PriceData::new));
 private static final Codec<EconomyPersistentState> CODEC=RecordCodecBuilder.create(i->i.group(PRICE_CODEC.listOf().fieldOf("prices").forGetter(s->new ArrayList<>(s.prices.values()))).apply(i,EconomyPersistentState::new));
 public static final SavedDataType<EconomyPersistentState> TYPE=new SavedDataType<>(Identifier.fromNamespaceAndPath("worldmorph","economy"),EconomyPersistentState::new,CODEC,null);
 private final Map<String,PriceData> prices=new LinkedHashMap<>();
 public EconomyPersistentState(){}
 private EconomyPersistentState(List<PriceData> list){for(PriceData p:list)if(p.price()>0)prices.put(p.item(),p);}
 public List<PriceData> all(){return List.copyOf(prices.values());}
 public void replaceAll(Collection<PriceData> list){prices.clear();for(PriceData p:list)if(p.price()>0)prices.put(p.item(),p);setDirty();}
}