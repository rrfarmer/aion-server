# Phase 6 Session 2647 Completion

## UOW

[Phase 6] UOW-2647: Execute pet spawn refeed scheduling live.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: spawning a restored food pet with a future refeed timestamp now schedules the remaining refeed delay instead of leaving the persisted not-hungry state without a live reset.
- Java source/runtime path: PetSpawnService.summonPet calls petCommonData.getRefeedDelay(); when positive it calls petCommonData.scheduleRefeed(delay), otherwise it sets feedProgress.hungryLevel = HUNGRY for food pets.
- C# runtime artifact wired: GameServerConnection.HandlePetSpawnAsync now applies spawn-time refeed state through ApplyPetSpawnRefeedState and SchedulePetRefeed.
- Client-visible/state effect: a restored spawned pet with future RefeedTimeMillis becomes Hungry after the remaining delay, changing future CM_PET food handling from not-hungry back to feedable.
- Why this is runtime progress: this wires a live CM_PET SPAWN path to ThreadPoolManager scheduling and mutates live player pet state through the scheduled callback.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/toypet/PetSpawnService.java`
  - `summonPet` spawns the pet, then checks `petCommonData.getRefeedDelay()`.
  - Positive delay calls `petCommonData.scheduleRefeed(petCommonData.getRefeedDelay())`.
  - No delay with food progress sets hungry level to `PetHungryLevel.HUNGRY`.
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/PetCommonData.java`
  - `getRefeedDelay` clears expired refeed timestamps and returns zero.
  - `scheduleRefeed` schedules the delayed hungry reset.
- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_PET.java`
  - `SPAWN` dispatches to `PetSpawnService.summonPet`.

## C# Changes

- Added `GameServerConnection.ApplyPetSpawnRefeedState` in the live pet spawn handler.
- Spawned pets with positive `RefeedTimeMillis - now` now call the existing live `SchedulePetRefeed`.
- Food pets with no remaining refeed delay are normalized to `RefeedTimeMillis = 0` and `HungryLevel = Hungry`, matching the Java spawn fallback branch.
- Added a focused live `CM_PET SPAWN` scheduler test using a real `ThreadPoolManager`.

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmPetSpawnSchedulesRestoredRefeedDelayAndCallbackMutatesPetState` | Unit/live connection | `PetSpawnService.summonPet` positive `getRefeedDelay` branch | Live `CM_PET SPAWN` schedules a restored future refeed delay, then the real scheduler callback clears refeed time and sets the pet Hungry. | Runs the actual connection packet path and `ThreadPoolManager` callback against live `Player.OwnedPets`. | Does not runtime-compare Java timing; no real client or MySQL validation. |

## Validation Decision

```text
- Changed surface: live CM_PET SPAWN scheduler state mutation.
- Specific behavior/contract: spawning a restored food pet with future RefeedTimeMillis should schedule the remaining delay and later reset RefeedTimeMillis to 0 and HungryLevel to Hungry.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter FullyQualifiedName~ProcessPacketAsync_CmPetSpawnSchedulesRestoredRefeedDelayAndCallbackMutatesPetState --no-restore
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter FullyQualifiedName~GameServerConnectionBuyItemTests --no-restore
- Focused Java/Maven command: not run; no narrow Java fixture exists for PetSpawnService.summonPet scheduler timing in this checkout, and Java source was reviewed directly.
- Broad-validation trigger: scheduler/live pet state behavior changed.
- Broad .NET decision: skipped after focused validation because the filtered connection tests built affected projects and directly exercised the changed live spawn path plus adjacent pet runtime paths.
- Why this scope is sufficient: the edited behavior is isolated to GameServerConnection pet spawn scheduling and is covered by a live packet-path test with a real scheduler callback.
```

Results:

- New focused spawn scheduler test: passed, 1/1.
- `GameServerConnectionBuyItemTests`: passed, 83/83.
- Existing nullable/analyzer warnings were emitted in unrelated surfaces during the first compile; no failures remained.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.toypet.PetSpawnService.summonPet` refeed branch | `Aion.GameServer.Network.Aion.GameServerConnection.HandlePetSpawnAsync` / `ApplyPetSpawnRefeedState` | Runtime handler | Partial | Unit Tested | Partial Parity | Live spawn now schedules positive restored refeed delays and normalizes no-delay food pets to hungry. Mood reset/autoloot/autosell spawn fanout remains outside this UOW. |
| `com.aionemu.gameserver.model.gameobjects.player.PetCommonData.getRefeedDelay` | `Aion.GameServer.Model.GameObjects.PlayerOwnedPet.RefeedDelaySeconds` and `GameServerConnection.ApplyPetSpawnRefeedState` | Runtime timing | Partial | Unit Tested | Partial Parity | Spawn-time C# computes remaining delay and clears no-delay food pet refeed state. Exact Java mutation semantics are only covered for the spawn path touched here. |
| `com.aionemu.gameserver.model.gameobjects.player.PetCommonData.scheduleRefeed` | `Aion.GameServer.Network.Aion.GameServerConnection.SchedulePetRefeed` | Runtime scheduler | Partial | Unit Tested | Partial Parity | Spawn path now invokes the live scheduler for restored future refeed delay. Cancellation on pet delete remains a known gap. |

## Known Gaps

- Java `PetController.onDelete` calls `commonData.cancelRefeedTask`; C# dismiss/surrender currently clears active pet state but does not cancel `_petRefeedTasks`.
- Java `PetSpawnService.summonPet` sends autoloot/autosell special-function packets on spawn when those persisted flags are enabled; not addressed here.
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

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 3
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%
