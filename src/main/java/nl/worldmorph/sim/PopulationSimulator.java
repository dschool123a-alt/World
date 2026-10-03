package nl.worldmorph.sim;

import java.util.*;
import nl.worldmorph.data.WorldMorphState;
import nl.worldmorph.npc.NpcManager;
import nl.worldmorph.npc.NpcProfile;

/**
 * Connects the existing NPC/family/settlement systems into a slow population loop.
 * The simulation creates founders, pairs compatible adults, creates children and
 * periodically grows settlements when their population has outgrown housing.
 */
public final class PopulationSimulator {
    private static final long FAMILY_CHECK = 1200;
    private static final long CHILD_CHECK = 2400;
    private static final long EXPANSION_CHECK = 6000;
    private int nameCounter;

    public void tick(WorldMorphState state, NpcManager npcs, FamilyManager families, HousingManager housing) {
        long tick = state.getSimulationTick();
        if (tick % FAMILY_CHECK == 0) ensureFounders(state, npcs, families);
        if (tick % CHILD_CHECK == 0) createChildren(state, npcs, families);
        if (tick % EXPANSION_CHECK == 0) expandSettlements(state, npcs);
    }

    private void ensureFounders(WorldMorphState state, NpcManager npcs, FamilyManager families) {
        for (WorldMorphState.SettlementData s : state.settlements().values()) {
            List<NpcProfile> citizens = citizens(npcs, s.id());
            if (!citizens.isEmpty()) continue;
            int founders = 2 + Math.floorMod(s.id().getLeastSignificantBits(), 2);
            for (int i=0;i<founders;i++) {
                NpcProfile p=npcs.createForSettlement("Citizen_"+(++nameCounter),s.id(),s.kingdomId(),state.getSimulationTick());
                p.setAge(20+i);
                families.found(p,s.id());
            }
            state.updateSettlement(s.withPopulation(Math.max(s.population(),founders)));
            state.history("FOUNDERS_ARRIVED",s.name()+" started with "+founders+" citizens.");
        }
    }

    private void createChildren(WorldMorphState state, NpcManager npcs, FamilyManager families) {
        for (WorldMorphState.SettlementData s : state.settlements().values()) {
            List<NpcProfile> adults=citizens(npcs,s.id()).stream().filter(p->p.alive()&&p.age()>=18&&p.age()<=45).toList();
            for(int i=0;i+1<adults.size();i+=2) {
                NpcProfile a=adults.get(i), b=adults.get(i+1);
                if(a.familyId()==null || !a.familyId().equals(b.familyId())) continue;
                if(a.relationships().getOrDefault(b.id(),0)<20) continue;
                FamilyManager.Family f=families.get(a.familyId()).orElse(null);
                if(f==null || f.members().size()>=8) continue;
                NpcProfile child=npcs.createForSettlement("Child_"+(++nameCounter),s.id(),s.kingdomId(),state.getSimulationTick());
                child.setAge(0);
                families.add(f.id(),child);
                state.updateSettlement(s.withPopulation(s.population()+1));
                state.history("CHILD_BORN",child.name()+" was born in "+s.name()+".");
            }
        }
    }

    private void expandSettlements(WorldMorphState state, NpcManager npcs) {
        for(WorldMorphState.SettlementData s:state.settlements().values()){
            int actual=citizens(npcs,s.id()).size();
            int target=Math.max(s.population(),actual);
            if(target> s.population()) state.updateSettlement(s.withPopulation(target));
            if(target>=10 && target>s.population()+2) state.history("SETTLEMENT_EXPANDING",s.name()+" is expanding as its population grows.");
        }
    }

    private List<NpcProfile> citizens(NpcManager npcs, UUID settlement) {
        return npcs.profiles().values().stream().filter(p->settlement.equals(p.settlementId())).toList();
    }
}