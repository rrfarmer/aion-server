# Phase 6 Session 2628 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2628: Send restored pet list during enter-world. See
[Phase-6-Session-2628-Completion.md](Phase-6-Session-2628-Completion.md).

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
- `72c3a94` - `[Phase 6][UOW-2626] Execute pet spawn live from CM_PET`
- `f50f024` - `[Phase 6][UOW-2627] Restore owned pets during enter-world`
- Current commit - `[Phase 6][UOW-2628] Send restored pet list during enter-world`

## Session Summary

- Java review confirmed `PetService.onPlayerLogin` sends `SM_PET LOAD_PETS` from restored `PetList` before mailbox/macro/recipe login packets.
- `PlayerOwnedPet` now carries the persisted fields needed to build Java `SM_PET.writePetData` snapshots.
- `MySqlPlayerEnterWorldRepository.LoadPlayerPetsAsync` now preserves birthday, expiration, feed, refeed, and doping fields when runtime pet templates indicate the relevant functions.
- `GameServerConnection` now sends a real `SmPet.LoadPets` packet during live enter-world for non-empty restored pet lists.
- Focused connection validation drives real `CM_ENTER_WORLD` parsing and asserts the emitted packet payload and ordering.

## Files Changed In UOW-2628

- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/PlayerOwnedPet.cs`
- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `docs/Phase-6-Session-2628-Completion.md`
- `docs/Phase-6-Session-2628-Handoff.md`

## Validation

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~PlayerPetRowProjectionTests|FullyQualifiedName~GamePacketTests" --no-restore
```

Result: passed, 410/410.

Java/Maven: not run. No narrow Java fixture was discovered for `PetService.onPlayerLogin` or `SM_PET` load-pets; Java behavior was verified by source review.

Broad .NET: skipped after focused coverage. Broad trigger existed because the live enter-world packet sequence changed, but focused validation built affected projects and covered packet serialization, real `CM_ENTER_WORLD` connection dispatch, enter-world restore, and row projection.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.toypet.PetService#onPlayerLogin` | `GameServerConnection.SendOwnedPetListAsync` | Runtime packet send | Partial | Unit Tested | Partial Parity | Sends `SM_PET LOAD_PETS` for restored pets during live enter-world; wider pet login behavior remains partial. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET#writePetData` | `SmPet.WritePetData` plus `PlayerOwnedPet` mapping | Packet/runtime mapping | Partial | Unit Tested | Partial Parity | Known fields and packet-writable static functions are emitted; full pet common-data runtime is not complete. |
| `com.aionemu.gameserver.dao.PlayerPetsDAO#getPlayerPets` | `MySqlPlayerEnterWorldRepository.LoadPlayerPetsAsync` / `PlayerPetRowProjection` | Repository | Partial | Unit Tested | Partial Parity | Projection now keeps fields used by login packet snapshots when static pet templates are available. |

## Known Gaps

- Pet expirable registration from Java `ExpireTimerTask` is not wired.
- Java last-used-pet tracking is not represented.
- `CM_PET` DISMISS, SURRENDER, auto-sell, auto-loot, adopt, and food/doping interaction flows remain deferred.
- Full live `PetCommonData` mood, gift, feed, and refeed scheduling behavior remains partial.
- Live MySQL and real-client validation were not run.

## Next Recommended Runtime UOW

**UOW-2629 candidate: execute `CM_PET` DISMISS live for the active summoned pet.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: spawned pets can enter active/world state, but client dismiss requests do not yet remove the active pet from live player/world state with Java-equivalent packets.
- Java source method or runtime path: CM_PET.runImpl DISMISS path delegates to PetSpawnService.dismissPet; Java sends SM_PET dismiss/despawn behavior and clears active pet state.
- C# runtime artifact to wire or fix: GameServerConnection CM_PET action handling, Player active pet fields, World pet removal, and SmPet dismiss packet emission.
- Client-visible/state effect expected: a player with an active spawned pet can dismiss it; the server clears player pet summon state, removes the world pet, and sends/broadcasts Java-equivalent pet removal packets.
- Why this is not preview-only/test-only/documentation-only if feasible: it mutates live player/world pet state and sends real server packets from a live client handler.
```

Suggested focused validation:

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~GamePacketTests" --no-restore
```

Java/Maven: run only if a narrow Java `CM_PET` dismiss or `SM_PET` dismiss fixture is discovered.

Broad-validation trigger: live pet world-state lifecycle changes. Start focused on spawn/dismiss packet/state tests and broaden only if shared world-object removal behavior is touched.

## Safe Runtime Candidates

- Wire `CM_PET` DISMISS if it removes active player/world pet state and sends Java-equivalent dismiss/despawn packets.
- Wire `CM_PET` SURRENDER only if it deletes persisted owned-pet state, mutates `Player.OwnedPets`, and sends Java-equivalent surrender packets.
- Restore pet expirable registration if it uses the existing expirable task service to mutate live runtime state.
- Execute a narrow Java/C# packet golden comparison for `SM_PET LOAD_PETS` only if it directly unblocks one of the live pet lifecycle handlers above.

## Context Needed By Next Session

- Java pet merchant functions load into C# runtime `StaticData.PetTemplates`, and active player pet state can feed action 17 sell-to-shop as of UOW-2625.
- `CM_PET` SPAWN executes live for owned pets and creates active/world pet state plus `SM_PET` spawn as of UOW-2626.
- `Player.OwnedPets` is restored from `player_pets` during live enter-world as of UOW-2627.
- `SM_PET LOAD_PETS` is sent from live enter-world for restored pets as of UOW-2628.
- `PlayerOwnedPet` now stores the fields needed by load-pets packet snapshots, but full pet common-data runtime behavior remains partial.
- Avoid preview/metadata/evidence-only work. Each next UOW must mutate live state, send real packets, persist/restore live state, load runtime-used Java data, or execute a live handler path.
