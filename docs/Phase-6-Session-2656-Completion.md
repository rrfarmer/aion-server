# Phase 6 Session 2656 Completion

## UOW

[Phase 6] UOW-2656: Bind Java pet periodic-save cadence into live C# pet mood scheduling.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: active pet mood update scheduling now reads the Java `gameserver.periodicsave.player.pets` cadence instead of always using a hardcoded default.
- Java source/runtime path: PeriodicSaveConfig.PLAYER_PETS loads `gameserver.periodicsave.player.pets`; PetSpawnService.summonPet passes PLAYER_PETS as both the initial delay and period for TaskId.PET_UPDATE.
- C# runtime artifact wired: GameServerOptions loads a PeriodicSave.PlayerPetsSeconds value, and GameServerConnection initializes its live PetMoodUpdateInterval from that option before CM_PET SPAWN scheduling.
- Client-visible/state/persistence effect: deployments changing `gameserver.periodicsave.player.pets` now change the live fixed-rate delay/period for scheduled SM_PET mood packets and the callback cadence that drives mood persistence checks.
- Why this is runtime progress: it loads Java static configuration into runtime C# structures used by live scheduler, packet, active pet state, and persistence behavior.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/configs/main/PeriodicSaveConfig.java`
  - `PLAYER_PETS` is bound to `gameserver.periodicsave.player.pets` with Java default `10`.
- `game-server/config/main/periodicsave.properties`
  - `gameserver.periodicsave.player.pets = 10`.
- `game-server/src/com/aionemu/gameserver/services/toypet/PetSpawnService.java`
  - `summonPet` calls `ThreadPoolManager.scheduleAtFixedRate(..., PLAYER_PETS * 1000, PLAYER_PETS * 1000)`.

## C# Changes

- Added `GameServerPeriodicSaveOptions` with `PlayerPetsSeconds`, defaulting to Java's 10-second pet cadence.
- Bound `gameserver.periodicsave.player.pets` through `GameServerOptions.LoadFromJavaConfig`, including existing Java-style `mygs.properties` and environment override behavior.
- Initialized `GameServerConnection.PetMoodUpdateInterval` from `GameServerOptions.PeriodicSave.PlayerPetsSeconds`, so live pet spawn scheduling uses the loaded Java key.

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `LoadFromJavaConfig_ReadsCoreAndNetworkDefaults` | Unit/config | `PeriodicSaveConfig.PLAYER_PETS` default | Java checkout default resolves to `10` seconds. | Reads real repository Java config through the C# loader. | Does not validate live scheduling. |
| `LoadFromJavaConfig_AppliesMyGsOverridesLast` | Unit/config | Java config override order | `mygs.properties` can override `gameserver.periodicsave.player.pets`. | Uses the existing Java-style config directory layout. | Synthetic temp config only. |
| `ProcessPacketAsync_CmPetSpawnUsesConfiguredPeriodicSavePetCadenceForMoodUpdate` | Unit/live connection | `PetSpawnService.summonPet` fixed-rate scheduling with `PLAYER_PETS` | Live CM_PET SPAWN schedules a fixed-rate pet mood task using the configured delay and period. | Runs actual connection packet dispatch and observes `ThreadPoolManager.ScheduleAtFixedRateTask`. | Does not wait for a 3-second callback; existing UOW-2655 tests cover callback packets/persistence with shortened test cadence. |

## Validation Decision

```text
- Changed surface: Java config loading and live CM_PET SPAWN pet mood update scheduling cadence.
- Specific behavior/contract: PeriodicSaveConfig.PLAYER_PETS should feed PetSpawnService.summonPet fixed-rate initial delay and period.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerOptionsTests|FullyQualifiedName~GameServerConnectionBuyItemTests" --no-restore
- Focused Java/Maven command: not run; no narrow Java unit fixture exists for PeriodicSaveConfig/PetSpawnService scheduling in this checkout, and Java source plus properties were reviewed directly.
- Broad-validation trigger: runtime scheduler cadence changed, but focused tests build affected projects and exercise config loading plus live connection scheduling.
- Broad .NET decision: skipped after focused validation because the changed path is isolated to options loading and the active pet scheduler interval.
- Why this scope is sufficient: the focused suite proves the Java key is loaded, overrideable, and consumed by live spawn scheduling; prior UOW-2655 tests still cover the callback packet/persistence behavior.
```

Results:

- Focused C# validation passed: 103/103 `GameServerOptionsTests` and `GameServerConnectionBuyItemTests`.
- Existing nullable/analyzer warnings were emitted in unrelated surfaces; no new analyzer warning remained from this UOW.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.configs.main.PeriodicSaveConfig.PLAYER_PETS` | `GameServerPeriodicSaveOptions.PlayerPetsSeconds` | Config/runtime data | Implemented | Unit Tested | Partial Parity | The Java key and default are loaded through the C# config loader. Other `PeriodicSaveConfig` keys remain separate runtime gaps. |
| `com.aionemu.gameserver.services.toypet.PetSpawnService.summonPet` | `GameServerConnection.SchedulePetMoodUpdate` | Runtime scheduler behavior | Partial | Unit Tested | Partial Parity | The fixed-rate pet mood task now uses configured seconds for delay and period. Broader Java `TaskId` controller-map semantics are still represented by connection-local task dictionaries. |

## Known Gaps

- Java `player.isSpawned()` remains approximated with C# `Player.IsOnline` in the scheduled pet update callback.
- General player, player item, and legion item periodic-save config keys are not yet fully wired into equivalent live C# periodic save flows.
- Real client validation was not run.
- Real MySQL validation was not run.
- No narrow Java/Maven fixture exists for the reviewed scheduling path in this checkout.

## Next Runtime UOW Candidates

1. Bind `gameserver.periodicsave.player.general` and `gameserver.periodicsave.player.items` into the live C# player-enter-world periodic save scheduling path, if the corresponding C# runtime scheduler is present or can be safely ported.
2. Bind `gameserver.periodicsave.legion.items` into live legion warehouse periodic persistence scheduling, replacing any remaining hardcoded cadence.
3. Replace the pet update callback's `Player.IsOnline` approximation with a direct spawned-state runtime check if the C# player/world lifecycle exposes or can safely add that state.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 3
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 2
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%
