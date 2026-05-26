# Phase 6ZQ Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1181
Status: Phase 6 continues; trade-list/trade-in live sends remain disabled.

## Session Summary

UOW-1181 added the companion no-legion fallback assertion to the guarded `PlayerEnterWorldRepositoryDatabaseIntegrationTests` harness. Together with UOW-1180, the harness now has opt-in DB checks for both joined legion level hydration and missing-legion defaults.

Files changed:

- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldRepositoryDatabaseIntegrationTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ZQ-Completion.md`

No production repository behavior, Java generator artifacts, packet sends, or live trade-list behavior changed.

## What Changed

- Added `LoadPlayerAsync_DefaultsLegionFactsWhenNoLegionMemberAgainstJavaSchema_WhenEnabled`.
- The test initializes the real Java schema from `game-server/sql/aion_gs.sql`, seeds only `players`, omits `legion_members`, and loads the player through `MySqlPlayerEnterWorldRepository`.
- The proof asserts:
  - `LegionId == 0`
  - `LegionLevel == 0`
  - `LegionName == string.Empty`
- Java breadcrumbs point to the `SM_TRADELIST` / `DialogService` no-legion expression: `player.getLegion() == null ? 0 : player.getLegion().getLegionLevel()`.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PlayerEnterWorldRepositoryDatabaseIntegrationTests" --nologo` passed 6 tests.
- The local run covered the disabled DB guard path only because `AION_GAMESERVER_DB_INTEGRATION` was not enabled. Do not treat this as live DB parity evidence.

## Migration Parity Table - UOW-1181

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.DialogService` | `Aion.GameServer.Services.NpcDialogTradeRuntimeFactAdapterService` / `QuestDialogNpcTargetBranchInputAssemblyPlanService` | Service / Runtime Fact Consumer | Partial | Manual Only | Needs Verification | Java uses `player.getLegion() == null ? 0 : player.getLegion().getLegionLevel()`. Opt-in C# DB test now covers the no-legion fallback shape when enabled, but local validation only compiled and exercised the disabled guard branch. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_TRADELIST` | `Aion.GameServer.Services.SmTradeListPacketPlanService` / `Aion.GameServer.Network.Aion.ServerPackets.SmTradeList` | Packet / Runtime Fact Consumer | Partial | Manual Only | Needs Verification | Java filters trade tabs using the no-legion level-zero fact. This unit added upstream repository fallback coverage only; packet bytes and live sends remain unverified. |
| `com.aionemu.gameserver.dao.LegionMemberDAO` | `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.LoadPlayerAsync` / `Aion.GameServer.Tests.PlayerEnterWorldRepositoryDatabaseIntegrationTests` | DAO / Repository Dependency | Partial | Manual Only | Needs Verification | No `legion_members` row should leave C# joined columns `NULL`; `ReadInt` maps them to `0` and `ReadString` maps the legion name to empty string. Needs DB-enabled execution. |
| `com.aionemu.gameserver.model.team.legion.Legion` | `Aion.GameServer.Model.GameObjects.Player.LegionLevel` | Model / Runtime Fact | Partial | Manual Only | Needs Verification | Java absence of `Legion` is represented by `null`; C# runtime fact is represented by zero/empty cached fields. This intentional representation difference needs broader legion-service parity review. |
| `game-server/sql/aion_gs.sql` `players` and absent `legion_members` join | `Aion.Commons.Database.DatabaseFactory` + integration test schema loader | Schema / Integration Fixture | Partial | Manual Only | Needs Verification | Existing harness loads the real Java schema, but local run skipped DB execution because integration env was disabled. |

Tests added or updated:

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `PlayerEnterWorldRepositoryDatabaseIntegrationTests.LoadPlayerAsync_DefaultsLegionFactsWhenNoLegionMemberAgainstJavaSchema_WhenEnabled` | Opt-in Integration / Guarded DB Test | Java `DialogService`; Java `SM_TRADELIST`; Java `aion_gs.sql` left-join shape | When DB integration is enabled, verifies C# `LoadPlayerAsync` defaults missing legion facts to id `0`, level `0`, and empty name. | Compiles and is included in the focused test filter; local run covered only the disabled guard branch. | Not executed against MySQL locally; no Java runtime comparison was performed. |

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact/schema rows in this unit
- Total artifacts ported: 0 runtime artifacts; 1 opt-in C# integration test added
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 3 blocked/partial categories: opt-in DB execution, broader legion-service representation parity, and trade-list Java packet artifacts
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Both legion-level DB assertions remain dormant locally unless `AION_GAMESERVER_DB_INTEGRATION=1` and a compatible MySQL/MariaDB instance is available.
- Local validation did not exercise SQL `LEFT JOIN` null materialization, so null-to-zero/string-empty behavior is verified by code inspection and compile coverage only.
- Java represents no legion as `null` while C# currently represents it as zero/empty cached facts; this remains an intentional difference pending broader legion-service parity.
- Trade-list packet parity remains blocked on Java vector artifacts; this unit only strengthens an upstream repository fact.

## Next Recommended Unit of Work

Primary next unit:

- Run the guarded `PlayerEnterWorldRepositoryDatabaseIntegrationTests` against a real Java-schema database and update parity status, or switch to a broader `PricesService` parity audit while DB and Java packet artifacts remain unavailable.

Suggested scope for `PricesService` audit:

- Compare Java `PricesService` caller expectations with C# trade fact adapters and packet-plan services.
- Keep the first pass docs/test-only unless a narrow deterministic formula gap is found.
- Do not enable live `SM_TRADELIST` sends.

## Next Work Options

| Option | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Run opt-in DB integration locally | No code required unless failures surface | Medium | Requires MySQL/MariaDB test database matching harness env vars. |
| B | Broader `PricesService` parity audit | New docs-only audit plus progress/handoff docs | Low | Useful while Java packet artifacts remain blocked. |
| C | Java generator skeleton feasibility rerun | Java test-only files under `game-server/test` | High | Only safe with Java 25 JDK and Maven available. |
| D | Trade-vector `wireFrameHex` design note | New docs-only note | Medium | Do not implement encrypted comparison until deterministic Java/C# crypt setup exists. |

Do not start live `SM_TRADELIST` or `SM_TRADE_IN_LIST` send wiring until Java vector artifacts exist and the guarded C# verifier executes against generated Java output.
