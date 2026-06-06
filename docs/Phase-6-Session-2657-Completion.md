# Phase 6 Session 2657 Completion

## UOW

[Phase 6] UOW-2657: Schedule live player periodic general and item saves after enter-world.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: players entering the C# world now register Java-style periodic general and inventory save tasks instead of only persisting on explicit live mutations or logout.
- Java source/runtime path: PlayerEnterWorldService.enterWorld schedules TaskId.PLAYER_UPDATE and TaskId.INVENTORY_UPDATE using PeriodicSaveConfig.PLAYER_GENERAL and PLAYER_ITEMS; GeneralUpdateTask.run and ItemUpdateTask.run persist live player and item state while the player remains in World.
- C# runtime artifact wired: GameServerOptions.PeriodicSave loads player general/items cadence keys, PlayerEnterWorldService schedules/cancels fixed-rate tasks, and IPlayerEnterWorldRepository exposes periodic general/items persistence methods implemented by MySqlPlayerEnterWorldRepository.
- Client-visible/state/persistence effect: after enter-world, configured intervals drive live periodic database persistence for modeled player common/cooldown/settings fields and dirty/deleted inventory rows without logging the player out; leave-world cancels the scheduled callbacks.
- Why this is runtime progress: it wires real scheduler behavior and runtime persistence through the existing database shape from live enter-world state.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/configs/main/PeriodicSaveConfig.java`
  - `PLAYER_GENERAL` maps to `gameserver.periodicsave.player.general`, default `900`.
  - `PLAYER_ITEMS` maps to `gameserver.periodicsave.player.items`, default `900`.
- `game-server/config/main/periodicsave.properties`
  - `gameserver.periodicsave.player.general = 900`
  - `gameserver.periodicsave.player.items = 900`
- `game-server/src/com/aionemu/gameserver/services/player/PlayerEnterWorldService.java`
  - `enterWorld` schedules `GeneralUpdateTask` and `ItemUpdateTask` at fixed rate.
  - `GeneralUpdateTask.run` looks up the player from `World`, then persists abyss rank, skills, quests, player common data, and houses.
  - `ItemUpdateTask.run` looks up the player from `World`, then calls `InventoryDAO.store(player)` and `ItemStoneListDAO.save(player)`.
- `game-server/src/com/aionemu/gameserver/services/player/PlayerLeaveWorldService.java`
  - `leaveWorld` calls `player.getController().delete()`.
- `game-server/src/com/aionemu/gameserver/controllers/CreatureController.java`
  - `onDelete` calls `cancelAllTasks`, cancelling registered scheduler futures.

## C# Changes

- Extended `GameServerPeriodicSaveOptions` with:
  - `PlayerGeneralSeconds`, default `900`.
  - `PlayerItemsSeconds`, default `900`.
- Loaded both Java config keys in `GameServerOptions.LoadFromJavaConfig`, preserving existing override precedence.
- Added `PlayerEnterWorldService` live scheduling:
  - schedules fixed-rate general and item save callbacks after successful enter-world,
  - skips scheduling if no `ThreadPoolManager` is available,
  - looks up the player in live `World` before saving,
  - cancels both tasks on leave-world before logout persistence.
- Added `IPlayerEnterWorldRepository.SavePeriodicPlayerGeneralAsync`.
- Added `IPlayerEnterWorldRepository.SavePeriodicPlayerItemsAsync`.
- Implemented MySQL periodic persistence:
  - general save writes currently modeled player common/cooldown/settings fields without changing `online` or `last_online`,
  - item save flushes tracked dirty/deleted inventory rows and marks them persisted.

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `LoadFromJavaConfig_ReadsCoreAndNetworkDefaults` | Unit/config | `PeriodicSaveConfig` defaults | Java checkout defaults resolve to `900/900/10` seconds. | Reads repository Java config through C# loader. | Does not validate live scheduling. |
| `LoadFromJavaConfig_AppliesMyGsOverridesLast` | Unit/config | Java config override order | `mygs.properties` overrides player general/items/pets periodic keys. | Uses Java-style temp config layout. | Synthetic temp config only. |
| `EnterWorld_SchedulesPeriodicGeneralAndItemSavesFromJavaConfigCadence` | Unit/live service | `PlayerEnterWorldService.enterWorld` scheduling | Successful enter-world registers two fixed-rate tasks using configured general/items intervals. | Observes actual `ThreadPoolManager.ScheduleAtFixedRateTask` calls. | Does not wait for 4/6-second callbacks. |
| `EnterWorld_PeriodicSaveCallbacksPersistLivePlayerStateWithoutLogout` | Unit/live service | `GeneralUpdateTask.run`, `ItemUpdateTask.run` | Scheduled callbacks call periodic repository methods while the player remains online and no logout save occurs. | Uses live world lookup, real scheduler, and shortened 1-second cadence. | Repository is in-memory capture. |
| `LeaveWorld_CancelsPeriodicSaveCallbacksBeforeTheyPersistAgain` | Unit/live service | `PlayerLeaveWorldService.leaveWorld` plus `CreatureController.onDelete` task cancellation | Leave-world cancels scheduled periodic callbacks before their initial delay elapses. | Uses real scheduler task cancellation and logout flow. | Timing-based, bounded to 1.2 seconds. |

## Validation Decision

```text
- Changed surface: live enter-world scheduler behavior, periodic persistence repository contract, Java config loading, and leave-world scheduler cancellation.
- Specific behavior/contract: Java enter-world registers PLAYER_UPDATE and INVENTORY_UPDATE fixed-rate tasks from PeriodicSaveConfig; callbacks persist live player/item state only while the player remains in World; leave-world cancels registered tasks.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~GameServerOptionsTests" --no-restore
- Focused Java/Maven command: not run; no narrow Java unit fixture exists for PlayerEnterWorldService periodic scheduling in this checkout, and Java source was reviewed directly.
- Broad-validation trigger: live scheduler and persistence surfaces changed.
- Broad .NET decision: skipped after focused validation because the filtered suite built affected projects and directly exercised config loading, enter-world scheduling, scheduled callbacks, and leave-world cancellation.
- Why this scope is sufficient: the edited behavior is isolated to PlayerEnterWorldService scheduling and IPlayerEnterWorldRepository periodic methods; focused tests prove the live scheduling/callback/cancel contract and compile the changed interface implementations.
```

Results:

- Focused C# validation passed: 69/69 `PlayerEnterWorldServiceTests` and `GameServerOptionsTests`.
- Existing nullable/analyzer warnings were emitted in unrelated surfaces; no failures remained.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.configs.main.PeriodicSaveConfig.PLAYER_GENERAL` | `Aion.GameServer.Configuration.GameServerPeriodicSaveOptions.PlayerGeneralSeconds` | Config/runtime data | Implemented | Unit Tested | Partial Parity | Java key/default are loaded and consumed by live enter-world scheduling. |
| `com.aionemu.gameserver.configs.main.PeriodicSaveConfig.PLAYER_ITEMS` | `Aion.GameServer.Configuration.GameServerPeriodicSaveOptions.PlayerItemsSeconds` | Config/runtime data | Implemented | Unit Tested | Partial Parity | Java key/default are loaded and consumed by live enter-world scheduling. |
| `com.aionemu.gameserver.services.player.PlayerEnterWorldService.enterWorld` | `Aion.GameServer.Services.PlayerEnterWorldService.EnterWorldAsync` | Runtime service | Partial | Unit Tested | Partial Parity | C# now schedules player general/items periodic saves after successful enter-world. Broader Java enter-world send/login side effects remain separately partial. |
| `com.aionemu.gameserver.services.player.GeneralUpdateTask` | `Aion.GameServer.Data.IPlayerEnterWorldRepository.SavePeriodicPlayerGeneralAsync` / `MySqlPlayerEnterWorldRepository.SavePeriodicPlayerGeneralAsync` | Scheduler callback/persistence | Partial | Unit Tested | Partial Parity | Persists modeled common/cooldown/settings fields without logging out. Java abyss rank, skill-list, quest-list, and house-save parity remains incomplete. |
| `com.aionemu.gameserver.services.player.ItemUpdateTask` | `Aion.GameServer.Data.IPlayerEnterWorldRepository.SavePeriodicPlayerItemsAsync` / `MySqlPlayerEnterWorldRepository.SavePeriodicPlayerItemsAsync` | Scheduler callback/persistence | Partial | Unit Tested | Partial Parity | Flushes tracked dirty/deleted inventory rows. Java `ItemStoneListDAO.save(player)` parity remains incomplete for periodic saves. |
| `com.aionemu.gameserver.controllers.CreatureController.onDelete` task cancellation | `Aion.GameServer.Services.PlayerEnterWorldService.LeaveWorldAsync` periodic task cancellation | Runtime lifecycle | Partial | Unit Tested | Partial Parity | C# cancels the two per-player periodic save tasks before logout persistence; there is not yet a general creature-controller task map. |

## Known Gaps

- Periodic general save does not yet persist abyss rank, full skill list, full quest list, or house saves equivalent to Java `GeneralUpdateTask`.
- Periodic item save does not yet persist item stones equivalent to Java `ItemStoneListDAO.save(player)`.
- C# uses service-local task dictionaries rather than a general Java-style `CreatureController` task map.
- Real client validation was not run.
- Real MySQL validation for the new periodic methods was not run.
- No targeted Java/Maven fixture exists for the reviewed scheduling path.

## Next Runtime UOW Candidates

1. Add live periodic item-stone persistence for player item saves by wiring the existing item-stone SQL helpers into `SavePeriodicPlayerItemsAsync`, matching Java `ItemUpdateTask -> ItemStoneListDAO.save(player)`.
2. Add live periodic abyss-rank persistence to `SavePeriodicPlayerGeneralAsync`, matching Java `GeneralUpdateTask -> AbyssRankDAO.storeAbyssRank(player)`, if the existing repository helpers can be reused safely.
3. Bind `gameserver.periodicsave.legion.items` into live `PeriodicSaveService` legion warehouse scheduling only if the C# legion warehouse runtime repository path is present or can be safely wired.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 6
- Total artifacts ported or extended in this UOW: 6
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 6
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%
