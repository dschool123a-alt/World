# Armies, siege cannons and NPC dialogue

## Raise an army
An operator can create NPC profiles and enlist living profiles in the kingdom they lead:
1. `/worldmorph kingdom create "North Kingdom"`
2. `/worldmorph npc spawn Alex` (or create a profile with `/worldmorph npc create Alex`)
3. `/worldmorph army recruit Alex`
4. `/worldmorph army list`
5. `/worldmorph army train`

The current army prototype tracks a kingdom roster and training level. Soldiers are NPC profiles assigned the SOLDIER job; formations, pathfinding, combat AI and persistent army saves are future work.

## Siege cannon
- `/worldmorph cannon build` builds the prototype cannon structure.
- `/worldmorph cannon fire` fires a vanilla Minecraft explosion in the cannon's facing direction.
- `/worldmorph cannon status` reports the tracked cannon.

Cannons currently are not crafting-table craftable yet. A proper craftable cannon kit item, recipe, inventory costs, reload/ammunition and persistent ownership are planned follow-up work; the current build command is a prototype.

## NPC speech
- `/worldmorph npc spawn Alex`
- `/worldmorph npc say Alex Welcome to our village!`
- `/worldmorph npc ask Alex`

Speech currently uses vanilla visible custom-name text, not a custom client-rendered bubble panel.
