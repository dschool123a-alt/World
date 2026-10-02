package nl.worldmorph.sim;
import java.util.*;
public final class TradeRouteManager {
 public record Route(UUID id,UUID origin,UUID destination,String good,int quantity,int intervalTicks,boolean active){}
 private final Map<UUID,Route> routes=new LinkedHashMap<>();
 public Route create(UUID a,UUID b,String good,int quantity,int interval){if(a.equals(b)||quantity<1||interval<20)return null;Route r=new Route(UUID.randomUUID(),a,b,good,quantity,interval,true);routes.put(r.id(),r);return r;}
 public Route setActive(UUID id,boolean active){Route r=routes.get(id);if(r==null)return null;Route n=new Route(r.id(),r.origin(),r.destination(),r.good(),r.quantity(),r.intervalTicks(),active);routes.put(id,n);return n;}
 public List<Route> active(){return routes.values().stream().filter(Route::active).toList();}
}