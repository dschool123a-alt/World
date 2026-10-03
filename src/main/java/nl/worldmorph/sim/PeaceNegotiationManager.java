package nl.worldmorph.sim;
import java.util.*;
public final class PeaceNegotiationManager {
 public enum Term{STATUS_QUO,TRIBUTE,TERRITORY_TRANSFER,PRISONER_RELEASE,NON_AGGRESSION,TRADE_ACCESS}
 public enum Status{PENDING,ACCEPTED,REJECTED,COUNTERED}
 public record Proposal(UUID id,UUID proposer,UUID recipient,UUID warKey,Set<Term> terms,int tribute,long createdTick,Status status){public Proposal{terms=Set.copyOf(terms);}}
 private final Map<UUID,Proposal> proposals=new LinkedHashMap<>();
 public Proposal propose(UUID proposer,UUID recipient,Set<Term> terms,int tribute,long tick){if(proposer.equals(recipient)||tribute<0)return null;Proposal p=new Proposal(UUID.randomUUID(),proposer,recipient,null,terms,tribute,tick,Status.PENDING);proposals.put(p.id(),p);return p;}
 public Proposal respond(UUID id,UUID recipient,Status status){Proposal p=proposals.get(id);if(p==null||p.status()!=Status.PENDING||!p.recipient().equals(recipient)||status==Status.PENDING)return null;Proposal n=new Proposal(p.id(),p.proposer(),p.recipient(),p.warKey(),p.terms(),p.tribute(),p.createdTick(),status);proposals.put(id,n);return n;}
 public Collection<Proposal> all(){return List.copyOf(proposals.values());}
}