# Phase 6PH Completion Handoff - Dredgion Instance AP Reward Planner

Date: May 25, 2026
Unit of Work: UOW-912
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-912] Add instance AP reward planner`)

## Status

Phase 6 is still in progress. This unit adds a compact Dredgion/basic PvP instance AP reward planner using Java `DredgionInstance`, `BasicPvpInstance`, `PvpInstanceScore`, `PvpInstancePlayerReward`, and `Rates.AP_DREDGION` as the source of truth.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/PvpInstanceApRewardService.cs`
- `dotnetConversion/src/Aion.GameServer/Program.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PvpInstanceApRewardServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6PH-Completion.md`

## What Changed

- Added `PvpInstanceApRewardService`.
- Added AP reward result and breakdown records for PvP instance AP rewards.
- Added Java winner/loser/draw base AP and bonus AP math for Dredgion/basic PvP reward branches.
- Added support for Basic PvP winner boss-bonus AP as an input.
- Applied configured `GameServerOptions.Rates.ApDredgionRates`.
- Registered the service in DI.

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~PvpInstanceApRewardServiceTests --no-restore
```

Result: passed, 7 tests.

```powershell
dotnet test dotnetConversion\AionServer.slnx --no-restore
```

Result: passed. Commons 57, Chat 29, Login 121, GameServer 1521.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `instance.dredgion.DredgionInstance` | `Aion.GameServer.Services.PvpInstanceApRewardService` | Instance Handler / Reward Planner | Partial | Regression Tested in C# | Partial Parity | Models Dredgion winner/loser/draw AP base/bonus math and AP_DREDGION rate application at the planner boundary. Full lifecycle, score updates, room capture, quest reward callback, item rewards, score packets, revive/leave timers, NPC deletion, random start position, and live handler loading remain missing. |
| `instance.pvp.BasicPvpInstance` | `Aion.GameServer.Services.PvpInstanceApRewardService` | Instance Handler / Reward Planner | Partial | Regression Tested in C# | Partial Parity | Models shared Basic PvP AP distribution and generic winner/loser/draw reward math, including winner boss-bonus input used by subclasses. Full progression state, score packets, GP rewards, item rewards, boss-kill conditions, point updates, revive/leave timers, and live handler integration remain missing. |
| `com.aionemu.gameserver.model.instance.instancescore.PvpInstanceScore` | `PvpInstanceApRewardService.CalculateFactionApReward` | Score / Reward Input Projection | Partial Input Projection | Regression Tested in C# | Needs Verification | Winner/loser/draw AP rewards and score points are accepted as inputs. Score storage, race score calculation, progression types, kill counters, and score writer serialization are not ported here. |
| `com.aionemu.gameserver.model.instance.playerreward.PvpInstancePlayerReward` | `PvpInstanceApRewardBreakdown` / `PvpInstanceApRewardResult` | Reward DTO Projection | Partial | Regression Tested in C# | Needs Verification | Base AP, bonus AP, total AP, applied AP, and AP plan result are modeled. Reward item IDs/counts, GP, kills, points, and packet serialization fields remain outside this slice. |
| `com.aionemu.gameserver.model.gameobjects.player.Rates.AP_DREDGION` | `PvpInstanceApRewardService.ApplyDredgionApRate` / `GameServerRateOptions.ApDredgionRates` | Rate Calculation / Configuration Consumption | Partial | Regression Tested in C# | Partial Parity | Configured membership rates, membership clamping, empty-rate fallback, and Java long-to-int fallback behavior are covered. |
| `com.aionemu.gameserver.services.abyss.AbyssPointsService` | `Aion.GameServer.Services.AbyssPointsService.AddAp` | Service | Partial | Regression Tested in C# | Partial Parity | Instance AP reward mutates AP through the existing add-AP planner. Persistence, full Legion contribution fanout, ranking cache, large-AP logging, and live caller side effects remain incomplete. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` | Server Packet | Partial | Regression Tested in C# | Needs Verification | Regression validates AP gain message id `1320000` is planned for instance AP rewards, including zero-reward planner behavior. Packet bytes and live ordering were not Java-runtime compared. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ABYSS_RANK` | `Aion.GameServer.Network.Aion.ServerPackets.SmAbyssRank` | Server Packet | Partial | Regression Tested in C# | Needs Verification | Regression validates rank packet intent after nonzero instance AP reward mutation. Ranking-position lookup and byte-level Java comparison remain unavailable. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ApplyApReward_AppliesConfiguredDredgionRateAndAddsApThroughPlanner` | Regression | Java `DredgionInstance.distributeRewards`, `BasicPvpInstance.distributeRewards`, `Rates.AP_DREDGION`, and `AbyssPointsService.addAp(Player, int)` source review | Validates base AP `4500`, bonus AP `500`, total AP `5000`, configured rate `1.5` producing applied AP `7500`, AP mutation `1000 -> 8500`, and AP gain/rank packet intent. | Deterministic C# regression grounded in Java source. | No Java runtime artifact; live instance handler integration remains missing. |
| `CalculateFactionApReward_MatchesJavaDredgionAndBasicPvpRewardBranches` | Regression | Java `DredgionInstance.doReward` and Basic PvP subclass `setAndDistributeRewards` source review | Validates winner bonus `2 * score / 6`, loser/draw bonus `score / 6`, draw base override, and winner boss-bonus addition. | Deterministic C# formula regression grounded in Java source. | Subclass-specific reward constants and item/GP rewards are not fully modeled. |
| `ApplyDredgionApRate_MatchesJavaMembershipFallbacksAndOverflowBehavior` | Regression | Java `Rates.AP_DREDGION`, `Rates.get`, and `Rates.calcResult(int)` source review | Validates membership clamping, empty-rate fallback to `1`, and overflow fallback to original AP. | Deterministic C# formula regression. | No Java runtime artifact. |
| `ApplyApReward_HandlesMissingPlayerAndZeroRewardLikePlannerBoundary` | Guard Regression | Java reward distribution plus C# planner boundary | Validates missing player does not mutate AP and zero AP still routes through add-AP planner, producing AP gain message intent with no rank packet. | Deterministic C# guard regression grounded in existing `AbyssPointsService` planner behavior. | Java runtime packet behavior for zero AP was not captured. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- `PvpInstanceApRewardService` is a planner slice; live Dredgion/Basic PvP instance handler loading, score lifecycle, score packets, quest callbacks, GP rewards, item rewards, revive/leave timers, NPC deletion, and map-specific subclass behavior remain incomplete.
- Basic PvP subclass constants and boss-kill bonuses are represented as inputs rather than live handler state.
- Remaining AP callers in Trade, item purification, NPC team/group distribution, Aturam/EternalBastion/Stonespear/PvP arena, and admin paths still need convergence through `AbyssPointsService` as appropriate.
- Packet bytes, persistence, ranking cache, Legion contribution fanout, and live siege callback execution remain incomplete.

## Summary Metrics

- Total Java artifacts discovered: 8
- Total artifacts ported: 1 Dredgion/basic PvP instance AP reward planner slice plus 1 DI registration
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 8
- Total blocked artifacts: 7 blocked/not-started categories, including Java runtime artifact generation, live instance handler integration, score/packet serialization lifecycle, item/GP reward side effects, remaining AP callers, persistence/fanout side effects, and byte-level packet comparison
- Estimated overall migration completion: Phase 6 remains about 67% complete

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Dredgion/basic PvP AP planner | `DredgionInstance`, `BasicPvpInstance`, `PvpInstanceScore`, `PvpInstancePlayerReward`, `Rates.AP_DREDGION` | `PvpInstanceApRewardService.cs`, tests, DI | Service Port | No with other AP writes | Medium | Completed in UOW-912. Shared AP planner and DI ownership required one writer. |
| B | NPC team/group AP distribution | `PlayerTeamDistributionService`, `NpcController`, `StatFunctions.calculatePvEApGained` | read-only initially | Java Analysis | Yes read-only | Medium | Needs team distribution surface map. |
| C | Remaining instance AP callers | `AturamSkyFortressInstance`, `EternalBastionInstance`, `StonespearReachInstance`, `PvPArenaInstance` | read-only initially | Java Analysis | Yes read-only | Medium | Determine which use simple AP plans versus broad instance reward models. |
| D | Trade/AP-purification analysis | `TradeService`, `ItemPurificationService` | read-only initially | Java Analysis | Yes read-only | Medium | Broader inventory/dialog surfaces likely required. |
| E | Live adapter analysis | existing AP planners and C# caller surfaces | read-only initially | Integration Analysis | Yes read-only | Medium | Map narrow live integration points without expanding runtime scope. |

## Next Recommended Unit of Work

Recommended sequential task:
- Analyze NPC team/group AP distribution or remaining instance AP callers and implement the smallest isolated AP planner.

Suggested safe parallel batch for the next session:

| Agent | Task | Allowed Files | Forbidden Files | Expected Result |
|---|---|---|---|---|
| Agent A | Analyze NPC team AP distribution | read-only `PlayerTeamDistributionService.java`, `NpcController.java`, C# team files | all writes | Candidate planner map or blocker list. |
| Agent B | Analyze remaining instance AP callers | read-only `AturamSkyFortressInstance.java`, `EternalBastionInstance.java`, `StonespearReachInstance.java`, `PvPArenaInstance.java` | all writes | Behavior report and dependency split. |
| Orchestrator | Implement one compact AP planner/integration slice | Exact production/test files chosen after discovery | Shared docs until final docs update | Code, tests, docs, commit. |

## Do Not Parallelize

- Multiple AP caller wiring tasks touching `AbyssPointsService`, `WorldNpcSoloDpRewardService`, `PvpApRewardService`, `QuestRewardService`, `PvpInstanceApRewardService`, or shared AP tests.
- AP caller implementation with config edits unless one owner controls both.
- Legion contribution fanout with AP service edits unless one owner controls both.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest decompose docs, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting subagents.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, continue NPC team AP, remaining instance AP, Trade/AP-purification, or live adapter convergence.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
