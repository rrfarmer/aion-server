# Phase 6 Session 2731 Completion

## UOW

[Phase 6] UOW-2731: Deny legion warehouse while legion is disbanding.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: OPEN_LEGION_WAREHOUSE now rejects players whose loaded legion is in the disbanding period.
- Java source/runtime path: LegionDAO.loadLegion reads legions.disband_time; Legion.isDisbanding returns disbandTime > 0; LegionService.canOpenWarehouse sends SM_SYSTEM_MESSAGE.STR_GUILD_WAREHOUSE_CANT_USE_WHILE_DISPERSE.
- C# runtime artifact wired: Player.LegionDisbandTime/IsLegionDisbanding, MySqlPlayerEnterWorldRepository.LoadPlayerAsync, GameServerConnection.HandleOpenLegionWarehouseDialogAsync, and SmSystemMessage.GuildWarehouseCantUseWhileDisbanding.
- Client-visible/state/persistence effect: live player login hydration includes the existing `legions.disband_time` column, and live dialog dispatch sends system-message id `1300333` instead of opening the warehouse.
- Why this is runtime progress: it loads existing Java-shaped database state into runtime C# player state and sends a real server packet from a live client packet handler.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/dao/LegionDAO.java`
  - `loadLegion` reads `disband_time` and calls `legion.setDisbandTime`.
- `game-server/src/com/aionemu/gameserver/model/team/legion/Legion.java`
  - `isDisbanding()` is represented by `disbandTime > 0`.
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
  - `canOpenWarehouse` checks `lm.getLegion().isDisbanding()` after target/config validation and before permission/in-use checks.
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
  - `STR_GUILD_WAREHOUSE_CANT_USE_WHILE_DISPERSE()` writes message id `1300333`.
- `game-server/sql/aion_gs.sql`
  - `legions.disband_time` exists as `int NOT NULL DEFAULT '0'`.

## C# Changes

- Added `Player.LegionDisbandTime` and `Player.IsLegionDisbanding`.
- Updated `MySqlPlayerEnterWorldRepository.LoadPlayerAsync` to project `l.disband_time AS legion_disband_time`.
- Added `SmSystemMessage.GuildWarehouseCantUseWhileDisbanding()` with Java message id `1300333`.
- Updated `GameServerConnection.HandleOpenLegionWarehouseDialogAsync` to send the disbanding denial after unsupported-target validation and before permission/in-use/open behavior.
- Updated opt-in DB integration assertions to cover loaded legion `disband_time` and the default no-legion value.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleDialogSelectAsync_OpenLegionWarehouseDisbandingLegionSendsJavaDenial` | Unit / live dialog dispatch | `LegionService.canOpenWarehouse` disbanding branch | A legion member with `LegionDisbandTime > 0` receives system-message id `1300333` from live `OPEN_LEGION_WAREHOUSE`. | Socket-backed connection fixture and decoded `SmSystemMessage` payload. | Does not exercise real DB hydration in this test. |
| `SmSystemMessage_WritesDialogTooFarMessages` addition | Unit / packet contract | `SM_SYSTEM_MESSAGE.STR_GUILD_WAREHOUSE_CANT_USE_WHILE_DISPERSE` | The C# helper writes Java message id `1300333`. | Packet decoder verifies id and empty parameters. | No Java golden bytes. |
| `LoadPlayerAsync_HydratesLegionLevelForTradeListFilteringAgainstJavaSchema_WhenEnabled` update | Opt-in DB integration | `LegionDAO.loadLegion`, `legions.disband_time` schema | When DB integration is enabled, the repository loads `disband_time` into `Player.LegionDisbandTime`. | Existing opt-in MySQL fixture; compiled in focused run but only executes with `AION_GAMESERVER_DB_INTEGRATION=1`. | Not executed in this session because DB integration is opt-in. |
| `LoadPlayerAsync_DefaultsLegionFactsWhenNoLegionMemberAgainstJavaSchema_WhenEnabled` update | Opt-in DB integration | Java no-legion handling | No legion defaults `LegionDisbandTime` to `0` and `IsLegionDisbanding` to false. | Existing opt-in MySQL fixture; compiled in focused run. | Not executed in this session because DB integration is opt-in. |

## Validation Decision

```text
- Changed surface: live dialog packet dispatch, player DB hydration projection, player runtime model, and system-message packet helper.
- Specific behavior/contract: disbanding legion warehouse open uses Java's `disband_time > 0` fact and sends system-message id 1300333 before permission/in-use/open.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionStorageExpansionDialogTests|FullyQualifiedName~GamePacketTests|FullyQualifiedName~PlayerEnterWorldRepositoryDatabaseIntegrationTests" --logger "console;verbosity=minimal"
- Result: passed; 329 tests passed.
- Focused Java/Maven command: not run; Java source and schema were reviewed unchanged, and no narrow Java fixture exists for this branch in the checkout.
- Broad-validation trigger: live dialog packet dispatch plus DB runtime projection. Broad .NET was skipped because the focused command compiled the affected project and exercised the live denial packet branch and packet helper; DB integration execution remains opt-in.
- Why this scope is sufficient: the command proves the client-visible live packet branch and compiles the repository projection against the test project; the opt-in DB assertions document remaining stronger evidence.
```

Repository hygiene:

```powershell
git diff --check
```

Result: passed with only Git CRLF working-copy warnings for touched files.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.dao.LegionDAO.loadLegion` | `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.LoadPlayerAsync` | Repository / runtime hydration | Partial | Opt-in Integration Covered | Partial Parity | Loads `legions.disband_time` through the player login projection; full Java Legion aggregate loading remains incomplete. |
| `com.aionemu.gameserver.model.team.legion.Legion.isDisbanding` | `Aion.GameServer.Model.GameObjects.Player.IsLegionDisbanding` | Runtime model | Partial | Unit Tested via live handler | Partial Parity | Represents `disband_time > 0`; does not model the full Java `Legion` object. |
| `com.aionemu.gameserver.services.LegionService.canOpenWarehouse` | `GameServerConnection.HandleOpenLegionWarehouseDialogAsync` | Live client packet handler branch | Partial | Unit Tested | Partial Parity | Membership, unsupported-action, disbanding, permission, in-use, and success branches are live; config-disabled remains missing. |
| `SM_SYSTEM_MESSAGE.STR_GUILD_WAREHOUSE_CANT_USE_WHILE_DISPERSE` | `SmSystemMessage.GuildWarehouseCantUseWhileDisbanding` | Server packet helper | Complete | Unit Tested | Partial Parity | Message id `1300333` verified by packet decoder and live dispatch; no Java golden bytes. |

## Known Gaps

- Java `LegionConfig.LEGION_WAREHOUSE` disabled still needs a C# config option and live open guard.
- C# does not model a full Java `Legion` aggregate; `LegionDisbandTime` is projected onto `Player`.
- Opt-in DB integration assertions were updated but not executed in this session.
- CM_LEGION leave/kick/member-removal is still deferred in `GameServerConnection`, so the Java member-removal lock-release branch cannot be wired as runtime progress yet.
- Exact Java golden bytes for `1300333` were not captured.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 5
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 43%

## Next Runtime UOW Candidates

1. Wire Java `gameserver.legion.warehouse` config into C# options and live `OPEN_LEGION_WAREHOUSE` denial, matching `LegionConfig.LEGION_WAREHOUSE == false` -> `STR_CANT_USE_GUILD_STORAGE`.
2. Discover a live C# legion leave/kick/member-removal path again later; currently CM_LEGION actions are deferred and do not pass the runtime gate.
3. Add Java-derived legion warehouse capacity checks to move/split if current C# can overfill location `3`.
