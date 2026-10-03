# WorldMorph

WorldMorph is an early-stage Minecraft **26.3 Fabric** mod for a persistent, evolving civilization simulation. The server owns the simulation state.

## Requirements

- Minecraft 26.3
- Fabric Loader 0.19.5+
- Fabric API 0.161.0+26.3
- Java 25
- Gradle 9.6 / Fabric Loom 1.17

## Commands

Commands are registered under `/worldmorph`.

| Command | Purpose |
|---|---|
| `/worldmorph test` | Check that the mod loaded |
| `/worldmorph debug` | Show simulation tick, kingdoms, settlements, NPC profiles and history |
| `/worldmorph kingdom create <name>` | Found a kingdom; the player becomes its ruler |
| `/worldmorph kingdom list` | List kingdoms and treasury/stability |
| `/worldmorph settlement create <name>` | Found a settlement for the player's kingdom when possible |
| `/worldmorph settlement list` | Show population and the five settlement need scores |
| `/worldmorph settlement info <name>` | Inspect a settlement and its coordinates/needs |
| `/worldmorph status` | Show world population, treasury, stability and simulation totals |
| `/worldmorph event list` | Show the latest world events from this server session |
| `/worldmorph economy list` | List currently known market prices |
| `/worldmorph npc spawn <name>` | Spawn a named villager linked to an NPC profile |
| `/worldmorph npc create <name>` | Create an NPC profile without spawning a villager |
| `/worldmorph npc list` | List NPC profiles |
| `/worldmorph npc ask <name>` | Start a help request and show a temporary name-tag bubble for a spawned NPC |
| `/worldmorph npc help <name>` | Accept a current help request |
| `/worldmorph npc refuse <name>` | Refuse a current help request |
| `/worldmorph npc memory <name>` | Inspect an NPC's recorded memories |
| `/worldmorph job set <npc> <job>` | Assign a job to an NPC profile (operator permission required) |
| `/worldmorph law enact <law>` | Enact a law for the player's kingdom (operator permission required) |
| `/worldmorph tax set <rate>` | Set the player's kingdom tax rate from 0 to 75 (operator permission required) |
| `/worldmorph family found <npc>` | Create a family for an NPC profile (operator permission required) |
| `/worldmorph economy price <item>` | Check the current base market price |
| `/worldmorph reputation <group>` | Check reputation with a social group |
| `/worldmorph history` | Show the latest world-history entries |

For job and law names, use the enum values shown by the command's error message, such as `FARMER` or `FOOD_RESERVE`.

## Current implementation

- World-level saved data for kingdoms, settlements and world history
- Separate saved NPC profiles, memories, family/settlement/kingdom links, personality and relationship scores
- NPC age/lifecycle updates, persistent home assignments and civilian job routines
- Kingdom treasury updates, need-driven population changes, persistent market prices and citizen mood
- NPC help/refuse interaction state and temporary speech bubbles for spawned villagers
- Daily settlement events: harvests, shortages, merchant caravans, immigration, outbreaks, storms, discoveries and festivals; events adjust settlement needs, market prices, population and kingdom stability
- Quick world/settlement diagnostics and market-price listing commands
- Building project planning, housing capacity, roads, resources, jobs and education data models
- Government, laws, group reputation, factions, elections, family/dynasty and citizen mood data models
- Diplomacy, treaties, trade ledger, quests, achievements and history/event models

## Build and verification

GitHub Actions compiles the project with Java 25 and Gradle 9.6.0. A successful compile does not mean every simulation subsystem is fully connected to in-game behavior yet; WorldMorph is still under active development.

Run locally:

    gradle build

The built JAR is in `build/libs/`.
