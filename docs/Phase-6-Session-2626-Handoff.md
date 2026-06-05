# Phase 6 Session 2626 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2626: Execute pet spawn live from `CM_PET`. See
[Phase-6-Session-2626-Completion.md](Phase-6-Session-2626-Completion.md).

## Commits Made

- `0671411` - `[Phase 6][UOW-2606] Execute NPC shop kinah buys live`
- `7d70054` - `[Phase 6][UOW-2607] Send NPC shop kinah denial live`
- `e00f057` - `[Phase 6][UOW-2608] Send NPC shop invalid-goods denial live`
- `8c0e2ee` - `[Phase 6][UOW-2609] Send NPC shop full-inventory denial live`
- `e8e1689` - `[Phase 6][UOW-2610] Send NPC shop limited-item denial live`
- `4aae7d1` - `[Phase 6][UOW-2611] Send NPC shop abyss-point denial live`
- `1daf029` - `[Phase 6][UOW-2612] Persist NPC shop kinah buys live`
- `c015455` - `[Phase 6][UOW-2613] Consume NPC shop required items live`
- `40ea371` - `[Phase 6][UOW-2614] Spend NPC shop abyss points live`
- `b848bbb` - `[Phase 6][UOW-2615] Mutate NPC shop limited counters live`
- `867b01a` - `[Phase 6][UOW-2616] Schedule NPC shop limited resets live`
- `3507068` - `[Phase 6][UOW-2617] Execute NPC shop abyss kinah buys live`
- `36b6746` - `[Phase 6][UOW-2618] Execute NPC shop abyss buys live`
- `a56bdbb` - `[Phase 6][UOW-2619] Execute NPC shop reward buys live`
- `816199d` - `[Phase 6][UOW-2620] Execute normal NPC sell-to-shop live`
- `46397e7` - `[Phase 6][UOW-2621] Execute abyss AP sell-to-shop live`
- `0eeb30d` - `[Phase 6][UOW-2622] Execute NPC shop repurchase live`
- `6c31b53` - `[Phase 6][UOW-2623] Execute partial AP sell-to-shop live`
- `e9a5945` - `[Phase 6][UOW-2624] Execute pet merchant sell-to-shop live`
- `b6f1206` - `[Phase 6][UOW-2625] Load pet merchant templates into active pet sell`
- Current commit - `[Phase 6][UOW-2626] Execute pet spawn live from CM_PET`

## Session Summary

- Java review confirmed `CM_PET` SPAWN routes through `PetSpawnService.summonPet` and `VisibleObjectSpawner.spawnPet`.
- C# now dispatches live `CmPet` packets from `GameServerConnection`.
- C# player runtime state now has an owned-pet list and template-id lookup equivalent to the Java `PetList.getPet(templateId)` guard.
- `CM_PET` SPAWN now mutates active pet fields, registers a production `WorldPet`, and sends `SM_PET` spawn.
- Focused coverage proves the spawned merchant pet can immediately execute live `CM_BUY_ITEM` action 17 sell-to-shop.

## Files Changed In UOW-2626

- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/Player.cs`
- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/PlayerOwnedPet.cs`
- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/WorldNpc.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `docs/Phase-6-Session-2626-Completion.md`
- `docs/Phase-6-Session-2626-Handoff.md`

## Validation

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmPetTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~StaticDataPetTemplateTests" --no-restore
```

Result: passed, 78/78.

Java/Maven: not run. No narrow Java fixture was discovered for `CM_PET` spawn or pet merchant sell chaining; Java behavior was verified by source review of `CM_PET`, `PetSpawnService`, `VisibleObjectSpawner`, `PetList`, and `PlayerPetsDAO`.

Broad .NET: skipped after focused coverage. Broad trigger existed because live packet dispatch and player/world state changed, but the focused command directly covered the parser, live handler execution, spawn packet contract, world registration, static pet template use, and downstream action 17 mutation/persistence packet fanout.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PET#runImpl` | `Aion.GameServer.Network.Aion.GameServerConnection.HandlePetAsync` | Live packet handler path | Partial | Unit Tested | Partial Parity | SPAWN is live; other actions remain deferred. |
| `com.aionemu.gameserver.spawnengine.VisibleObjectSpawner#spawnPet` | `GameServerConnection.HandlePetSpawnAsync` / `WorldPet` | Runtime world/player state | Partial | Unit Tested | Partial Parity | Active pet state, world object registration, and SM_PET spawn are implemented for owned pets. |
| `com.aionemu.gameserver.model.gameobjects.player.PetList#getPet` | `Player.OwnedPets` / `Player.GetOwnedPet` | Runtime player state | Partial | Unit Tested | Partial Parity | Spawn lookup exists; production DB hydration remains a gap. |

## Known Gaps

- `Player.OwnedPets` still needs to be populated from persisted `player_pets` data during production enter-world/load.
- Full Java pet common data fields are not represented beyond object id, template id, name, and decoration.
- Full `Pet` object behavior, known-list wiring, movement, update scheduling, and world visibility are partial.
- `CM_PET` DISMISS, SURRENDER, auto-sell, auto-loot, and adopt flows remain deferred.
- Spawn-side auto-sell/auto-loot packet restoration is not implemented.
- Live MySQL and real-client validation were not run.

## Next Recommended Runtime UOW

**UOW-2627 candidate: hydrate `Player.OwnedPets` from persisted `player_pets` rows during live enter-world.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: CM_PET SPAWN now consumes Player.OwnedPets, but production enter-world does not yet restore owned pets from the database, so real players cannot spawn persisted pets without manual/test setup.
- Java source method or runtime path: PlayerPetsDAO.getPlayerPets, PetList.loadPets, PetCommonData, and the player enter-world/load path that attaches PetList to Player.
- C# runtime artifact to wire or fix: PlayerEnterWorldRepository/PlayerEnterWorldService or the nearest existing player-load repository should project `player_pets` rows into Player.OwnedPets using the existing database shape.
- Client-visible/state/persistence effect expected: after enter-world, a persisted pet can be spawned by live CM_PET SPAWN, registered as a world pet, and used as a merchant seller for live action 17.
- Why this is not preview-only/test-only/documentation-only if feasible: it restores runtime state from the existing database shape and feeds a live client packet handler and downstream live sell mutation path.
```

Suggested focused validation:

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~PlayerPetRowProjectionTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmPetTests" --no-restore
```

Java/Maven: not expected unless a narrow Java pet restore/load fixture is discovered.

Broad-validation trigger: live enter-world/player pet runtime state changes. Start focused on DB row projection, enter-world player hydration, CM_PET spawn, and action 17 sell; broaden only if focused evidence exposes wider risk.

## Safe Runtime Candidates

- Hydrate owned pets from `player_pets` into `Player.OwnedPets` during live player load.
- Wire `CM_PET` DISMISS only if it removes active player/world pet state and sends the corresponding real packet/effect.
- Restore spawn-side auto-sell/auto-loot state and packets only if the live Java-equivalent client-visible effect is implemented.

## Context Needed By Next Session

- `CM_BUY_ITEM` NPC action 13 normal kinah buys execute live and persist as of UOW-2612.
- `CM_BUY_ITEM` NPC action 13 normal required-item buys execute live and persist as of UOW-2613.
- `CM_BUY_ITEM` NPC action 13 normal AP-cost buys execute live and persist as of UOW-2614.
- Successful `CM_BUY_ITEM` NPC action 13 normal limited-item buys mutate live limited counters as of UOW-2615.
- Limited-item counters reset from live scheduler callbacks as of UOW-2616.
- `CM_BUY_ITEM` NPC action 13 `ABYSS_KINAH` buys execute live and persist as of UOW-2617.
- `CM_BUY_ITEM` NPC action 13 `ABYSS` buys execute live without Kinah as of UOW-2618.
- `CM_BUY_ITEM` NPC action 13 `REWARD` buys execute live without Kinah as of UOW-2619.
- `CM_BUY_ITEM` NPC action 1 normal sell-to-shop executes live for covered whole-item sells as of UOW-2620.
- `CM_BUY_ITEM` NPC action 1 `ABYSS` AP sell-to-shop executes live for exact-count deletes as of UOW-2621.
- `CM_BUY_ITEM` NPC action 2 repurchase executes live for the covered success path as of UOW-2622.
- `CM_BUY_ITEM` NPC action 1 `ABYSS` AP sell-to-shop partial-stack decreases execute live as of UOW-2623.
- `CM_BUY_ITEM` pet action 17 merchant sell-to-shop executes live for `IWorldPetObject` targets as of UOW-2624.
- Java pet merchant functions load into C# runtime `StaticData.PetTemplates`, and active player pet state can feed action 17 sell-to-shop as of UOW-2625.
- `CM_PET` SPAWN now executes live for owned pets and creates active/world pet state plus `SM_PET` spawn as of UOW-2626.
- Buy/sell/repurchase planners are not themselves runtime progress unless the live handler consumes them.
- Avoid preview/metadata/evidence-only work. Each next UOW must mutate live state, send real packets, persist/restore live state, load runtime-used Java data, or execute a live handler path.
