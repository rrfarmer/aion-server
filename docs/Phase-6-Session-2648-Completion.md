# Phase 6 Session 2648 Completion

## UOW

[Phase 6] UOW-2648: Cancel active pet refeed on dismiss live.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: active pet dismiss/delete now cancels any pending refeed reset task instead of allowing a stale callback to mutate owned-pet state after the pet is inactive.
- Java source/runtime path: PetController.onDelete calls PetCommonData.cancelRefeedTask() before persisting feed state and clearing the player's active pet.
- C# runtime artifact wired: GameServerConnection.ClearActivePetAsync now calls CancelPetRefeedTask, sharing the same task-map cancellation used by SchedulePetRefeed replacement.
- Client-visible/state effect: CM_PET DISMISS and active-pet surrender no longer allow a delayed refeed callback to reset RefeedTimeMillis/HungryLevel after the pet has been dismissed.
- Why this is runtime progress: this changes live CM_PET DISMISS/SURRENDER scheduler cancellation and prevents future live player pet state mutation.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/controllers/PetController.java`
  - `onDelete` gets `PetCommonData`, then calls `commonData.cancelRefeedTask()` before feed persistence, pet update cancellation, mood persistence, and clearing the player's active pet.
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/PetCommonData.java`
  - `cancelRefeedTask()` cancels the stored `Future<?>`.
- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_PET.java`
  - `DISMISS` deletes the active pet controller; `SURRENDER` deletes the active pet when the surrendered pet is active.

## C# Changes

- Added `GameServerConnection.CancelPetRefeedTask`.
- Reused `CancelPetRefeedTask` from `SchedulePetRefeed` replacement scheduling.
- Called `CancelPetRefeedTask` from `ClearActivePetAsync` before world removal and active pet field clearing.
- Added focused live dismiss coverage that schedules a refeed through spawn, dismisses the pet, waits past the original delay, and asserts the owned pet was not mutated by a stale callback.

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmPetDismissCancelsPendingRefeedCallback` | Unit/live connection | `PetController.onDelete` and `PetCommonData.cancelRefeedTask` source review | Live `CM_PET DISMISS` cancels a pending spawned refeed task so the delayed callback does not reset the inactive owned pet. | Runs actual `CM_PET SPAWN` and `CM_PET DISMISS` packet paths with a real `ThreadPoolManager`, then waits past the scheduled delay. | Does not validate DB feed persistence on dismiss; no real client or MySQL validation. |

## Validation Decision

```text
- Changed surface: live CM_PET DISMISS/SURRENDER scheduler cancellation.
- Specific behavior/contract: active pet delete should cancel pending refeed reset tasks so stale callbacks cannot mutate inactive owned-pet state.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter FullyQualifiedName~ProcessPacketAsync_CmPetDismissCancelsPendingRefeedCallback --no-restore
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CmPetDismiss|FullyQualifiedName~ProcessPacketAsync_CmPetSurrender|FullyQualifiedName~SchedulePetRefeed" --no-restore
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter FullyQualifiedName~GameServerConnectionBuyItemTests --no-restore
- Focused Java/Maven command: not run; no narrow Java fixture exists for PetController.onDelete refeed cancellation in this checkout, and Java source was reviewed directly.
- Broad-validation trigger: scheduler/live pet state behavior changed.
- Broad .NET decision: skipped after focused validation because the filtered connection tests built affected projects and directly exercised dismiss, surrender, scheduler replacement, and adjacent pet paths.
- Why this scope is sufficient: the edited behavior is isolated to GameServerConnection active pet cleanup and is covered by a live packet-path stale-callback regression plus adjacent pet lifecycle tests.
```

Results:

- New focused dismiss cancellation test: passed, 1/1.
- Targeted dismiss/surrender/scheduler filter: passed, 6/6.
- `GameServerConnectionBuyItemTests`: passed, 84/84.
- Existing nullable/analyzer warnings were emitted in unrelated surfaces during the first compile; no failures remained.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PetController.onDelete` refeed cancellation | `Aion.GameServer.Network.Aion.GameServerConnection.ClearActivePetAsync` / `CancelPetRefeedTask` | Runtime lifecycle | Partial | Unit Tested | Partial Parity | Active pet clear now cancels pending refeed tasks. Feed-status DB persistence, mood persistence, and pet-update task cancellation remain incomplete. |
| `com.aionemu.gameserver.model.gameobjects.player.PetCommonData.cancelRefeedTask` | `Aion.GameServer.Network.Aion.GameServerConnection.CancelPetRefeedTask` | Runtime scheduler cancellation | Partial | Unit Tested | Partial Parity | C# cancels and removes the stored `ScheduledTask` handle from the connection task map. Java stores the `Future<?>` on `PetCommonData`. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PET` dismiss/surrender active delete path | `Aion.GameServer.Network.Aion.GameServerConnection.HandlePetDismissAsync` / `HandlePetSurrenderAsync` | Runtime handler | Partial | Unit Tested | Partial Parity | Dismiss and active surrender flow through `ClearActivePetAsync`, so both cancel pending refeed callbacks. Surrender-specific stale-callback test not separate; adjacent surrender tests passed. |

## Known Gaps

- Java `PetController.onDelete` persists feed status on delete; C# active pet dismiss currently does not persist feed status as a delete-time operation.
- Java `PetController.onDelete` persists mood/despawn data and cancels `TaskId.PET_UPDATE`; C# pet update/mood lifecycle remains incomplete.
- Java `PetSpawnService.summonPet` sends autoloot/autosell special-function packets on spawn when persisted flags are enabled; not yet wired.
- Real client validation was not run.
- Real MySQL validation was not run.
- No Java/C# runtime timing comparison was run.

## Next Recommended Runtime UOW

**UOW-2649 candidate: send persisted pet auto-loot/auto-sell special-function packets on live pet spawn.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: spawning a pet with persisted auto-loot or auto-sell enabled should immediately send the Java-equivalent special-function activation packets.
- Java source method or runtime path: PetSpawnService.summonPet checks petCommonData.isLooting() and isSelling(), then sends new SM_PET(PetSpecialFunction.AUTOLOOT/AUTOSELL, true).
- C# runtime artifact to wire or fix: GameServerConnection.HandlePetSpawnAsync after the spawn packet, using PlayerOwnedPet.IsLooting and IsSelling plus existing SmPet.SpecialFunction packet support.
- Client-visible/packet effect expected: CM_PET SPAWN sends real SM_PET special-function activation packets for persisted enabled pet flags so the client reflects auto-loot/auto-sell state immediately after spawn.
- Why this is not preview-only/test-only/documentation-only: it sends real server packets from the live CM_PET SPAWN handler based on persisted runtime pet state.
```

Suggested focused validation:

```text
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CmPetSpawn|FullyQualifiedName~ProcessPacketAsync_CmPetAutoLoot|FullyQualifiedName~ProcessPacketAsync_CmPetAutoSell" --no-restore
```

Start with a single new spawn special-function test first, then run the filter above. Java/Maven is not expected unless a narrow Java packet fixture is discovered. Broad-validation trigger: live packet fanout behavior changes; run focused connection packet tests first.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 3
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%
