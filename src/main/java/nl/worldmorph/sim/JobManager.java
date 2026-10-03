package nl.worldmorph.sim;
import java.util.*; import nl.worldmorph.npc.NpcProfile;
public final class JobManager {
 private final Map<UUID,JobType> assignments=new HashMap<>();
 public boolean assign(NpcProfile npc,JobType job){if(!npc.alive())return false;npc.setJob(job.name());assignments.put(npc.id(),job);npc.addMemory("JOB_ASSIGNED_"+job.name(),null,0,3);return true;}
 public JobType get(UUID npc){return assignments.getOrDefault(npc,JobType.UNEMPLOYED);}
 public Map<UUID,JobType> assignments(){return Map.copyOf(assignments);}
}