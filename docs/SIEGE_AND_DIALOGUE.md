# Siege cannons and NPC speech bubbles (prototype)

These features target the server-side Fabric build for Minecraft 26.3.

## Siege cannon

Run as an operator in-game:

- `/worldmorph cannon build` — constructs a small block-based cannon facing the direction you look.
- `/worldmorph cannon status` — shows the cannon position and shots fired.
- `/worldmorph cannon fire` — fires a TNT-style Minecraft explosion from the muzzle.

The cannon registry is currently held in memory and is reset when the server restarts. One cannon is tracked per player. This is a first playable prototype, not a custom rendered entity or a full artillery trajectory system yet. Explosions use Minecraft's normal block and entity rules.

## NPC speech bubbles

- `/worldmorph npc spawn Alex` — spawns a named villager with a WorldMorph profile (operator command).
- `/worldmorph npc say Alex Welcome to our village!` — displays the message as the villager's visible name for five seconds.
- `/worldmorph npc ask Alex` — displays the existing help-request bubble.

The bubble currently uses the vanilla visible custom-name label, so it works without a client-side mod. A fully styled speech balloon with a background panel would require client rendering support.
