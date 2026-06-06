# Phase 6 Session 2655 Completion

## UOW

[Phase 6] UOW-2655: Execute live pet mood periodic update scheduling.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: active pet spawns now register a Java-style periodic `PET_UPDATE` task instead of only responding to immediate CM_PET mood packets.
- Java source/runtime path: PetSpawnService.summonPet registers TaskId.PET_UPDATE with ThreadPoolManager.scheduleAtFixedRate(new PetController.PetUpdateTask(player), PLAYER_PETS, PLAYER_PETS); PetController.onDelete cancels TaskId.PET_UPDATE; PetUpdateTask sends SM_PET subtype 4/subtype 3 mood updates and periodically calls PlayerPetsDAO.savePetMoodData.
- C# runtime artifact wired: GameServerConnection.HandlePetSpawnAsync now schedules a fixed-rate pet mood update task, ClearActivePetAsync cancels it, and the scheduled callback uses PlayerOwnedPet mood timing plus PlayerEnterWorldService.SavePlayerPetMoodDataAsync.
- Client-visible/state/persistence effect: live active pets now emit scheduled SM_PET MOOD subtype 4 packets, update last-sent mood state, and periodically persist mood fields through the existing player_pets schema while active; dismiss/surrender cancels the scheduled task.
- Why this is runtime progress: it wires live scheduler behavior, sends real server packets from scheduled runtime code, mutates active pet timing state, and persists runtime pet mood state.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/toypet/PetSpawnService.java`
  - `summonPet` schedules `TaskId.PET_UPDATE` at fixed rate using `PeriodicSaveConfig.PLAYER_PETS`.
- `game-server/src/com/aionemu/gameserver/controllers/PetController.java`
  - `onDelete` cancels `TaskId.PET_UPDATE`.
  - `PetUpdateTask.run` returns when the player is not spawned, cancels on unexpected failures, sends periodic mood packets, and persists mood data at its save cadence.
- `game-server/src/com/aionemu/gameserver/configs/main/PeriodicSaveConfig.java`
  - `gameserver.periodicsave.player.pets` defaults to `10` seconds.
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PET.java`
  - MOOD subtype 4 writes mood points and cooldown remaining values; subtype 3 writes condition reward and starts gift cooldown.

## C# Changes

- Added Java default pet update/save cadence constants to `GameServerConnection`.
- Added active pet mood update task tracking:
  - schedules fixed-rate updates on successful `CM_PET SPAWN`,
  - cancels and removes scheduler state from `ClearActivePetAsync`.
- Added scheduled callback behavior:
  - skips offline players,
  - cancels when the active pet is missing or replaced,
  - hydrates `PetCommonDataTiming` from active `PlayerOwnedPet`,
  - sends Java-shaped `SmPet.Mood` subtype 4 packets,
  - updates active pet last-sent/timing state,
  - persists mood fields at the Java save cadence through `SavePlayerPetMoodDataAsync`.
- Added internal cadence overrides for focused tests while production defaults remain Java-shaped.

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmPetSpawnSchedulesMoodUpdateAndSendsPeriodicMoodPacket` | Unit/live connection | `PetSpawnService.summonPet`, `PetController.PetUpdateTask.run`, `SM_PET.writeImpl` source review | Live pet spawn schedules the recurring update and emits a serialized subtype 4 mood packet from scheduled code. | Runs actual connection packet dispatch with a real `ThreadPoolManager` scheduled callback. | Uses shortened cadence for test speed. |
| `ProcessPacketAsync_CmPetSpawnMoodUpdatePersistsMoodDataAtSaveCadence` | Unit/live connection | `PetController.PetUpdateTask.run` and `PlayerPetsDAO.savePetMoodData` source review | Scheduled update persists active pet mood data after the Java save cadence elapses. | Runs actual connection dispatch and records repository save calls. | Uses in-memory repository; no MySQL round trip. |
| `ProcessPacketAsync_CmPetDismissCancelsPendingMoodUpdateCallback` | Unit/live connection | `PetController.onDelete -> cancelTask(TaskId.PET_UPDATE)` source review | Dismissing the active pet cancels the recurring mood update task so no extra mood packets are emitted after dismiss. | Runs actual connection dispatch, scheduler callback, and dismiss lifecycle. | Race-sensitive by nature; covered with a short post-dismiss delay. |

## Validation Decision

```text
- Changed surface: live CM_PET SPAWN/DISMISS pet lifecycle, ThreadPoolManager fixed-rate scheduling, active PlayerOwnedPet mood timing state, SM_PET mood packet emission, and pet mood persistence calls.
- Specific behavior/contract: Java PetSpawnService.summonPet registers TaskId.PET_UPDATE; PetController.PetUpdateTask sends periodic mood packets and persists mood data; PetController.onDelete cancels the task.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests" --no-restore
- Focused Java/Maven command: not run; no narrow Java unit fixture exists for PetSpawnService/PetController.PetUpdateTask in this checkout, and Java source was reviewed directly.
- Broad-validation trigger: live scheduler behavior, live connection dispatch, active pet runtime state, live packet sends, and persistence calls changed.
- Broad .NET decision: skipped after focused validation because the filtered live connection class built affected projects and directly exercised the scoped spawn/scheduler/dismiss behavior.
- Why this scope is sufficient: the edited runtime path is isolated to active pet spawn update scheduling and active pet clear cancellation; focused tests verify scheduled packet emission, save cadence persistence, and cancellation.
```

Results:

- Focused C# validation passed: 98/98 `GameServerConnectionBuyItemTests`.
- Existing nullable/analyzer warnings were emitted in unrelated surfaces; no failures remained.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.toypet.PetSpawnService.summonPet` | `Aion.GameServer.Network.Aion.GameServerConnection.HandlePetSpawnAsync` / `SchedulePetMoodUpdate` | Runtime service behavior | Partial | Unit Tested | Partial Parity | Active pet spawn now registers fixed-rate mood updates at the Java default cadence. Config binding for `gameserver.periodicsave.player.pets` is not surfaced yet; production uses the Java default. |
| `com.aionemu.gameserver.controllers.PetController.PetUpdateTask` | `Aion.GameServer.Network.Aion.GameServerConnection.ExecutePetMoodUpdateAsync` | Scheduler callback | Partial | Unit Tested | Partial Parity | C# sends subtype 4 mood updates, updates active timing state, and persists at the save cadence. It uses `Player.IsOnline` as the current closest C# equivalent of Java `player.isSpawned()`. |
| `com.aionemu.gameserver.controllers.PetController.onDelete` | `Aion.GameServer.Network.Aion.GameServerConnection.ClearActivePetAsync` | Runtime lifecycle | Partial | Unit Tested | Partial Parity | Active pet clear now cancels pending refeed and mood update tasks before persistence/despawn state updates. |
| `com.aionemu.gameserver.configs.main.PeriodicSaveConfig` | `Aion.GameServer.Network.Aion.GameServerConnection` constants | Config/runtime cadence | Partial | Unit Tested | Partial Parity | Java default `PLAYER_PETS = 10` seconds is represented; `.properties` override binding remains a gap. |

## Known Gaps

- `gameserver.periodicsave.player.pets` is not yet bound into `GameServerOptions`; production cadence uses the Java default of 10 seconds.
- Java `player.isSpawned()` is approximated with C# `Player.IsOnline` because the current player model does not expose a direct spawned flag.
- Real client validation was not run.
- Real MySQL validation was not run.
- Java task-map semantics are represented by connection-local task dictionaries, not a general `CreatureController` task map.

## Next Recommended Runtime UOW

**UOW-2656 candidate: bind Java `gameserver.periodicsave.player.pets` into runtime C# options and use it for active pet update scheduling.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: live active pet update cadence should come from the Java `.properties` key instead of a hardcoded Java default.
- Java source method or runtime path: PeriodicSaveConfig.PLAYER_PETS loads `gameserver.periodicsave.player.pets`; PetSpawnService.summonPet uses it for TaskId.PET_UPDATE scheduling.
- C# runtime artifact to wire or fix: GameServerOptions / configuration loading and GameServerConnection.SchedulePetMoodUpdate cadence selection.
- Client-visible/state effect expected: deployments changing `gameserver.periodicsave.player.pets` will change live scheduled SM_PET mood update and mood persistence cadence.
- Why this is not preview-only/test-only/documentation-only: it loads Java config into runtime C# behavior used by live scheduler and packet/persistence side effects.
```

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%
