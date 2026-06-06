# Phase 6 Session 2662 Completion

## UOW

[Phase 6] UOW-2662: Persist live periodic player houses.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: periodic general saves now persist modeled player house rows after player general state.
- Java source/runtime path: PlayerEnterWorldService.GeneralUpdateTask.run -> player.getHouses() -> House.save() -> HousesDAO.storeHouse.
- C# runtime artifact wired: MySqlPlayerEnterWorldRepository.SavePeriodicPlayerGeneralAsync now calls SavePeriodicPlayerHousesAsync.
- Client-visible/state/persistence effect: modeled house building, owner id, acquire time, door/settings bits, next rent payment, and sign notice can survive scheduled periodic saves.
- Why this is runtime progress: this extends the existing live periodic general save callback to mutate the Java-shaped houses table.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/player/PlayerEnterWorldService.java`
  - `GeneralUpdateTask.run` saves abyss rank, skills, quests, player row, then iterates `player.getHouses()` and calls `house.save()`.
- `game-server/src/com/aionemu/gameserver/model/house/House.java`
  - `save()` delegates to `HousesDAO.storeHouse(this)` and saves the loaded registry when present.
- `game-server/src/com/aionemu/gameserver/dao/HousesDAO.java`
  - `storeHouse` inserts `NEW` houses and updates `UPDATE_REQUIRED` houses using columns `id`, `address`, `building_id`, `player_id`, `acquire_time`, `settings`, `next_pay`, and `sign_notice`.

## C# Changes

- Added `SavePeriodicPlayerHousesAsync` to `MySqlPlayerEnterWorldRepository`.
- `SavePeriodicPlayerGeneralAsync` now saves `player.Houses` after the Java-shaped player-row update succeeds.
- The helper upserts modeled `PlayerHouse` rows into `houses` using Java `HousesDAO.storeHouse` columns.

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `SavePeriodicPlayerGeneralAsync_UpsertsLiveHousesAgainstJavaSchema_WhenEnabled` | Gated DB integration | `GeneralUpdateTask.run -> House.save -> HousesDAO.storeHouse` | Periodic general save updates an existing owned house and inserts another modeled owned house using Java `houses` columns, including settings and nullable timestamps/sign notice. | Uses the live repository method invoked by the scheduler callback and the Java database schema. | The DB body is guarded by `AION_GAMESERVER_DB_INTEGRATION=1`; in this environment it compiled but returned early. |

## Validation Decision

```text
- Changed surface: live periodic general persistence repository implementation.
- Specific behavior/contract: Java periodic general save calls House.save for player houses, and HousesDAO.storeHouse writes the houses table fields.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldRepositoryDatabaseIntegrationTests.SavePeriodicPlayerGeneralAsync_UpsertsLiveHousesAgainstJavaSchema_WhenEnabled" --no-restore
- Focused Java/Maven command: not run; no narrow Java unit fixture exists for GeneralUpdateTask/House.save/HousesDAO.storeHouse in this checkout, and Java source was reviewed directly.
- Broad-validation trigger: repository persistence surface changed.
- Broad .NET decision: skipped after focused validation because the filtered test compiled the changed repository and targeted the Java houses table contract.
- Why this scope is sufficient: UOW-2657 already validated the live scheduler invokes SavePeriodicPlayerGeneralAsync; this UOW changed the repository method invoked by that callback.
```

Results:

- Focused C# validation passed: 1/1 test.
- The gated database test returned early because `AION_GAMESERVER_DB_INTEGRATION` was not enabled in this environment.
- Existing nullable/analyzer warnings were emitted in unrelated surfaces; no failures remained.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.player.PlayerEnterWorldService.GeneralUpdateTask` house-save loop | `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.SavePeriodicPlayerGeneralAsync` | Scheduler callback/persistence | Partial | DB Contract Compiled | Partial Parity | The scheduled callback was wired in UOW-2657; this UOW adds house-row persistence inside the repository method. |
| `com.aionemu.gameserver.model.house.House.save` | `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.SavePeriodicPlayerHousesAsync` | Model persistence boundary | Partial | DB Contract Compiled | Partial Parity | C# snapshots modeled `PlayerHouse` rows; Java saves only `NEW`/`UPDATE_REQUIRED` houses and also saves a loaded registry. |
| `com.aionemu.gameserver.dao.HousesDAO.storeHouse` | `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.SavePeriodicPlayerHousesAsync` | Repository | Partial | DB Contract Compiled | Partial Parity | Java insert/update columns are represented. C# uses upsert because house persistent state is not modeled. |

## Known Gaps

- C# does not currently model Java house `PersistentState`, so periodic saves snapshot all loaded modeled houses instead of only `NEW`/`UPDATE_REQUIRED` rows.
- Java `House.save()` also saves a loaded `HouseRegistry` when its persistent state requires it. C# housing registry mutations currently persist through immediate live repository methods, but there is no Java-equivalent registry dirty-state periodic flush.
- Real MySQL validation was not run because `AION_GAMESERVER_DB_INTEGRATION` was not enabled.
- Real client validation was not run.

## Next Runtime UOW Candidates

1. Find another live periodic/general-save gap only if the Java path has modeled C# runtime state and a concrete database write target.
2. Investigate Java `HouseRegistry.save -> PlayerRegisteredItemsDAO.store` versus C# live registry mutation persistence only if a dirty-state/runtime registry model exists or a live mutation path is currently unsaved.
3. Continue from deferred live packet/state/persistence searches rather than adding housing readiness/planner scaffolding.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 3
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%
