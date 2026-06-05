# Phase 6 Session 2647 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2647: Execute pet spawn refeed scheduling live. See
[Phase-6-Session-2647-Completion.md](Phase-6-Session-2647-Completion.md).

## Commits Made

- `f50f024` - `[Phase 6][UOW-2627] Restore owned pets during enter-world`
- `f071686` - `[Phase 6][UOW-2628] Send restored pet list during enter-world`
- `f46d5d8` - `[Phase 6][UOW-2629] Execute active pet dismiss live`
- `0650121` - `[Phase 6][UOW-2630] Execute owned pet surrender live`
- `74b48ab` - `[Phase 6][UOW-2631] Execute active pet rename live`
- `1aaf094` - `[Phase 6][UOW-2632] Execute active pet feed cancel live`
- `7c25ce4` - `[Phase 6][UOW-2633] Send active pet not-hungry response live`
- `b9ec634` - `[Phase 6][UOW-2634] Start active pet feeding live`
- `a33935b` - `[Phase 6][UOW-2635] Execute pet auto-loot activation live`
- `32cd2d0` - `[Phase 6][UOW-2636] Execute pet auto-sell activation live`
- `0cf6fce` - `[Phase 6][UOW-2637] Execute pet doping slot switch live`
- `608dc7a` - `[Phase 6][UOW-2638] Execute pet doping add remove live`
- `1fc49f8` - `[Phase 6][UOW-2639] Persist pet doping bag mutations live`
- `57a404f` - `[Phase 6][UOW-2640] Execute single pet feeding check live`
- `e94c02d` - `[Phase 6][UOW-2641] Execute rejected pet feeding live`
- `426e709` - `[Phase 6][UOW-2642] Execute multi-count pet feeding live`
- `26a2ca1` - `[Phase 6][UOW-2643] Send pet-food inventory update type live`
- `fed5542` - `[Phase 6][UOW-2644] Execute rewarded pet feeding live`
- `b6f8269` - `[Phase 6][UOW-2645] Execute random loved pet reward selection live`
- `a305c40` - `[Phase 6][UOW-2646] Execute pet refeed replacement scheduler live`
- Current commit - `[Phase 6][UOW-2647] Execute pet spawn refeed scheduling live`

## Session Summary

- Live `CM_PET SPAWN` now applies Java `PetSpawnService.summonPet` refeed behavior.
- Spawned food pets with a future restored `RefeedTimeMillis` schedule the existing live `SchedulePetRefeed` callback.
- Food pets with no remaining refeed delay are normalized to `RefeedTimeMillis = 0` and `HungryLevel = Hungry`.
- Focused live connection coverage proves a restored spawned pet becomes hungry after the scheduled callback runs.

## Files Changed In UOW-2647

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `docs/Phase-6-Session-2647-Completion.md`
- `docs/Phase-6-Session-2647-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.toypet.PetSpawnService.summonPet`
- `com.aionemu.gameserver.model.gameobjects.player.PetCommonData.getRefeedDelay`
- `com.aionemu.gameserver.model.gameobjects.player.PetCommonData.scheduleRefeed`
- `com.aionemu.gameserver.network.aion.clientpackets.CM_PET`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.HandlePetSpawnAsync`
- `Aion.GameServer.Network.Aion.GameServerConnection.ApplyPetSpawnRefeedState`
- `Aion.GameServer.Network.Aion.GameServerConnection.SchedulePetRefeed`
- `Aion.GameServer.Tests.GameServerConnectionBuyItemTests`

## Validation

```text
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter FullyQualifiedName~ProcessPacketAsync_CmPetSpawnSchedulesRestoredRefeedDelayAndCallbackMutatesPetState --no-restore
```

Result: passed, 1/1.

```text
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter FullyQualifiedName~GameServerConnectionBuyItemTests --no-restore
```

Result: passed, 83/83.

Java/Maven: not run. No narrow Java scheduler fixture exists for `PetSpawnService.summonPet`; Java source was reviewed directly.

Broad .NET: skipped after focused validation. Broad trigger existed because scheduler/live pet state behavior changed; the focused commands built affected projects and directly exercised the changed live spawn path and adjacent pet paths.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.toypet.PetSpawnService.summonPet` refeed branch | `Aion.GameServer.Network.Aion.GameServerConnection.HandlePetSpawnAsync` / `ApplyPetSpawnRefeedState` | Runtime handler | Partial | Unit Tested | Partial Parity | Positive restored refeed delays now schedule live reset callbacks; no-delay food pets normalize to hungry. Other spawn fanout remains incomplete. |
| `com.aionemu.gameserver.model.gameobjects.player.PetCommonData.getRefeedDelay` | `Aion.GameServer.Network.Aion.GameServerConnection.ApplyPetSpawnRefeedState` | Runtime timing | Partial | Unit Tested | Partial Parity | Spawn path uses millisecond timestamp comparison and clears no-delay food pets. Broader packet read methods still rely on `PlayerOwnedPet.RefeedDelaySeconds`. |
| `com.aionemu.gameserver.model.gameobjects.player.PetCommonData.scheduleRefeed` | `Aion.GameServer.Network.Aion.GameServerConnection.SchedulePetRefeed` | Runtime scheduler | Partial | Unit Tested | Partial Parity | Existing live scheduler is now invoked from spawn; delete-time cancellation still needs wiring. |

## Known Gaps

- Active pet dismiss/delete does not yet cancel a pending C# refeed task, unlike Java `PetController.onDelete`.
- Spawn-time autoloot/autosell special-function packets remain incomplete.
- Mood despawn-time reset and periodic pet update scheduling remain incomplete.
- Real client validation was not run.
- Real MySQL validation was not run.
- No Java/C# runtime timing comparison was run.

## Next Recommended Runtime UOW

**UOW-2648 candidate: cancel active pet refeed task during live pet delete/dismiss.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: active pet delete/dismiss should cancel any pending refeed reset task so a despawned pet's stale callback cannot later mutate owned-pet state.
- Java source method or runtime path: PetController.onDelete calls commonData.cancelRefeedTask() before persisting feed state and clearing the player's active pet.
- C# runtime artifact to wire or fix: GameServerConnection.ClearActivePetAsync and the _petRefeedTasks/SchedulePetRefeed task map.
- Client-visible/state effect expected: dismissing or surrendering an active pet cancels pending refeed callbacks; the inactive/restored owned-pet state is not unexpectedly changed to hungry by a stale scheduled callback.
- Why this is not preview-only/test-only/documentation-only: it changes live CM_PET DISMISS/SURRENDER scheduler cancellation and prevents future live state mutation.
```

Suggested focused validation:

```text
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CmPetDismiss|FullyQualifiedName~ProcessPacketAsync_CmPetSurrender|FullyQualifiedName~SchedulePetRefeed" --no-restore
```

If the edited test is more specific, start with that single test first, then run `FullyQualifiedName~GameServerConnectionBuyItemTests` only if adjacent pet-path coverage is needed. Java/Maven is not expected unless a narrow Java pet delete scheduler fixture is discovered. Broad-validation trigger: scheduler/live pet state behavior changes; run focused connection tests first.

## Safe Runtime Candidates

- Cancel C# `_petRefeedTasks` from `ClearActivePetAsync`, covering dismiss and surrender of active pets.
- Wire Java spawn autoloot/autosell packet fanout if it can be proven from live `CM_PET SPAWN` and existing persisted flags.
- Review pet feed status persistence on dismiss against `PetController.onDelete`, but avoid DB evidence-only work unless paired with live persistence wiring.

## Context Needed By Next Session

- UOW-2640: accepted single-count feed consumes item, persists feed/inventory, sends subtype 2/subtype 5/end.
- UOW-2641: rejected food sends item unlock, subtype 5, END_FEEDING, and message `1400618`.
- UOW-2642: accepted multi-count food repeats until count reaches zero.
- UOW-2643: accepted pet feeding partial-stack inventory updates use Java `DEC_PET_FOOD = 0x5E`.
- UOW-2644: rewarded full feed grants inventory reward, persists refeed/feed state, sends subtype 6/7, and schedules refeed reset.
- UOW-2645: multiple valid loved rewards no longer collapse to the first valid reward in the live handler.
- UOW-2646: repeated refeed scheduling cancels stale callbacks and only the current scheduler callback mutates live pet state.
- UOW-2647: pet spawn schedules restored future refeed delays and resets no-delay food pets to hungry.
- Runtime feed evaluation uses `StaticData.PetFeedData`, loaded from Java `pet_feed.xml` and `item_groups.xml`.
- Avoid preview/metadata/evidence-only work. Each next UOW must mutate live state, send real packets, persist/restore live state, load runtime-used Java data, execute a live handler path, or directly unblock one of those behaviors.
