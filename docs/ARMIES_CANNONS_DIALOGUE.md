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
- `/worldmorph cannon craft` consumes 3 iron blocks, 8 iron ingots, 4 iron bars, 16 stone bricks and 4 gunpowder to make one cannon kit.
- `/worldmorph cannon build` places the cannon structure and consumes the crafted kit.
- `/worldmorph cannon fire` fires a vanilla Minecraft explosion in the cannon's facing direction.
- `/worldmorph cannon status` reports the tracked cannon.

Cannon crafting currently uses an in-game command and inventory costs, not a crafting-table recipe or custom inventory item. Reload/ammunition, persistent ownership and a proper crafting-table recipe remain follow-up work.

## NPC speech
- `/worldmorph npc spawn Alex`
- `/worldmorph npc say Alex Welcome to our village!`
- `/worldmorph npc ask Alex`

Speech currently uses vanilla visible custom-name text, not a custom client-rendered bubble panel.
