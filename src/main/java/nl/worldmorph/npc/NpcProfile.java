package nl.worldmorph.npc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class NpcProfile {
    private final UUID id;
    private String name;
    private int age;
    private String job;
    private int money;
    private int ambition;
    private int loyalty;
    private boolean alive = true;
    private final List<Memory> memories = new ArrayList<>();

    public NpcProfile(UUID id, String name) {
        this.id = id;
        this.name = name;
        this.age = 20;
        this.job = "unassigned";
        this.loyalty = 50;
    }

    public UUID id() { return id; }
    public String name() { return name; }
    public int age() { return age; }
    public String job() { return job; }
    public int money() { return money; }
    public int ambition() { return ambition; }
    public int loyalty() { return loyalty; }
    public boolean alive() { return alive; }
    public List<Memory> memories() { return List.copyOf(memories); }

    public void addMemory(Memory memory) {
        memories.add(memory);
    }

    public void changeLoyalty(int amount) {
        loyalty = Math.max(0, Math.min(100, loyalty + amount));
    }

    public record Memory(String type, UUID target, long tick, int importance) {}
}
