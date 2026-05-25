# Phase 6PG Completion Handoff - Quest AP Reward Planner

Date: May 25, 2026
Unit of Work: UOW-911
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-911] Add Quest AP reward planner`)

## Status

Phase 6 is still in progress. This unit adds the compact Quest AP reward branch from Java `QuestService.giveReward`, including ordinary quest rate application and the `QuestCategory.NON_COUNT` rate bypass.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/QuestRewardService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestRewardServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6PG-Completion.md`

## What Changed

- Injected `GameServerOptions` into `QuestRewardService`.
- Added `QuestRewardService.ApplyApReward`.
- Added `QuestRewardService.ApplyQuestApRate` for Java `Rates.AP_QUEST`.
- Added `QuestApRewardResult` and `QuestApRewardStatus`.
- Applied configured `GameServerOptions.Rates.ApQuestRates` for ordinary quest AP rewards.
- Added an explicit `isNonCountQuest` bypass for Java relic-exchange style `QuestCategory.NON_COUNT` rewards.

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~QuestRewardServiceTests --no-restore
```

Result: passed, 8 tests.

```powershell
dotnet test dotnetConversion\AionServer.slnx --no-restore
```

Result: passed. Commons 57, Chat 29, Login 121, GameServer 1514.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.QuestService.giveReward` | `Aion.GameServer.Services.QuestRewardService.ApplyApReward` | Service / Reward Planner | Partial | Regression Tested in C# | Partial Parity | Models quest AP reward zero-skip, quest-rate application for ordinary quests, `NON_COUNT` rate bypass, and AP mutation through `AbyssPointsService`. Full quest completion flow, reward template selection, kinah/XP/title/item/GP/cube/warehouse side effects, and live `QuestEnv` integration remain missing. |
| `com.aionemu.gameserver.model.gameobjects.player.Rates.AP_QUEST` | `QuestRewardService.ApplyQuestApRate` / `GameServerRateOptions.ApQuestRates` | Rate Calculation / Configuration Consumption | Partial | Regression Tested in C# | Partial Parity | Configured membership rates, membership clamping, empty-rate fallback, and Java long-to-int fallback behavior are covered. |
| `com.aionemu.gameserver.model.templates.quest.QuestCategory.NON_COUNT` | `QuestRewardService.ApplyApReward(bool isNonCountQuest)` | Quest Category / Input Projection | Partial Input Projection | Regression Tested in C# | Needs Verification | C# accepts the Java quest category decision as an input and bypasses quest AP rates when true. Quest template lookup remains unwired. |
| `com.aionemu.gameserver.services.abyss.AbyssPointsService` | `Aion.GameServer.Services.AbyssPointsService.AddAp` | Service | Partial | Regression Tested in C# | Partial Parity | Quest AP reward now mutates AP through the existing add-AP planner. Persistence, full Legion contribution fanout, ranking cache, large-AP logging, and live caller side effects remain incomplete. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` | Server Packet | Partial | Regression Tested in C# | Needs Verification | Regression validates AP gain message id `1320000` is planned for quest AP reward. Packet bytes and live ordering were not Java-runtime compared. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ABYSS_RANK` | `Aion.GameServer.Network.Aion.ServerPackets.SmAbyssRank` | Server Packet | Partial | Regression Tested in C# | Needs Verification | Regression validates rank packet intent after quest AP reward mutation. Ranking-position lookup and byte-level Java comparison remain unavailable. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ApplyApReward_AppliesConfiguredQuestRateAndAddsApThroughPlanner` | Regression | Java `QuestService.giveReward`, `Rates.AP_QUEST`, and `AbyssPointsService.addAp(Player, int)` source review | Validates configured `ApQuestRates = [1.0, 1.75]` for membership `1` turns reward AP `200` into applied AP `350`, mutates AP `900 -> 1250`, and plans AP gain/rank packets. | Deterministic C# regression grounded in Java source. | No Java runtime artifact; live `QuestEnv`/reward-template integration remains missing. |
| `ApplyApReward_SkipsQuestRateForJavaNonCountCategory` | Regression | Java `QuestService.giveReward` `QuestCategory.NON_COUNT` branch source review | Validates non-count quest AP reward `200` bypasses configured rate `3.0` and applies exactly `200`. | Deterministic C# regression grounded in Java source. | Quest category is input-projected; template lookup remains unwired. |
| `ApplyApReward_SkipsMissingPlayerAndZeroApReward` | Guard Regression | Java quest reward branch and C# planner boundary | Validates missing player and zero AP rewards do not mutate AP. | Deterministic C# guard regression. | Java caller normally supplies a player through `QuestEnv`. |
| `ApplyQuestApRate_MatchesJavaMembershipFallbacksAndOverflowBehavior` | Regression | Java `Rates.AP_QUEST`, `Rates.get`, and `Rates.calcResult(int)` source review | Validates membership clamping, empty-rate fallback to `1`, and overflow fallback to original AP. | Deterministic C# formula regression. | No Java runtime artifact. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- `QuestRewardService.ApplyApReward` is a planner slice; live quest completion, `QuestEnv`, selected reward index, template lookup, reward package selection, and non-AP reward side effects remain incomplete.
- `QuestCategory.NON_COUNT` is represented by a boolean input until quest template/category lookup has a C# home.
- Remaining AP callers in Dredgion/basic PvP instances, Trade, item purification, and NPC team/group distribution still need convergence through `AbyssPointsService`.
- Packet bytes, persistence, ranking cache, Legion contribution fanout, and live siege callback execution remain incomplete.

## Summary Metrics

- Total Java artifacts discovered: 6
- Total artifacts ported: 1 Quest AP reward planner slice
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 6
- Total blocked artifacts: 6 blocked/not-started categories, including Java runtime artifact generation, live quest completion integration, quest template/category lookup, remaining AP callers, persistence/fanout side effects, and byte-level packet comparison
- Estimated overall migration completion: Phase 6 remains about 67% complete

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Quest AP reward planner | `QuestService.giveReward`, `Rates.AP_QUEST`, `QuestCategory.NON_COUNT` | `QuestRewardService.cs`, tests | Planner / Integration Prep | No with quest reward writes | Low | Compact AP branch. Completed in UOW-911. |
| B | Dredgion/basic PvP instance AP analysis | instance handlers, `Rates.AP_DREDGION` | read-only initially | Java Analysis | Yes read-only | Medium | Handler/runtime surfaces may be broader. |
| C | NPC team/group AP distribution | `PlayerTeamDistributionService`, `NpcController`, `StatFunctions.calculatePvEApGained` | read-only initially | Java Analysis | Yes read-only | Medium | Needs team distribution surface map. |
| D | Live PvP adapter analysis | `PvpService`, C# combat/death surfaces | read-only initially | Integration Analysis | Yes read-only | Medium | Needs damage-list/team/death lifecycle support map before writes. |
| E | Trade/AP-purification analysis | `TradeService`, `ItemPurificationService` | read-only initially | Java Analysis | Yes read-only | Medium | Broader inventory/dialog surfaces likely required. |

## Next Recommended Unit of Work

Recommended sequential task:
- Analyze Dredgion/basic PvP instance AP rewards and add a compact planner only if the handler surface can stay isolated.

Suggested safe parallel batch for the next session:

| Agent | Task | Allowed Files | Forbidden Files | Expected Result |
|---|---|---|---|---|
| Agent A | Analyze Java Dredgion/basic PvP instance AP behavior | read-only instance handlers and `Rates.java` | all writes | Behavior report and config dependencies. |
| Agent B | Analyze NPC team AP distribution | read-only `PlayerTeamDistributionService.java`, `NpcController.java`, C# team files | all writes | Candidate planner map or blocker list. |
| Orchestrator | Implement one compact AP planner/integration slice | Exact production/test files chosen after discovery | Shared docs until final docs update | Code, tests, docs, commit. |

## Do Not Parallelize

- Multiple AP caller wiring tasks touching `AbyssPointsService`, `WorldNpcSoloDpRewardService`, `PvpApRewardService`, `QuestRewardService`, or shared AP tests.
- AP caller implementation with config edits unless one owner controls both.
- Legion contribution fanout with AP service edits unless one owner controls both.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest decompose docs, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting subagents.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, continue Dredgion AP, NPC team AP, Trade/AP-purification, or live adapter convergence.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
