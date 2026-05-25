# Phase 6PF Completion Handoff - PvP AP Reward/Loss Planner

Date: May 25, 2026
Unit of Work: UOW-910
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-910] Add PvP AP reward planner`)

## Status

Phase 6 is still in progress. This unit adds a compact PvP AP reward/loss planner using the Java `PvpService`, `StatFunctions`, `Rates`, and `AbyssRankEnum` behavior as the source of truth.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/PvpApRewardService.cs`
- `dotnetConversion/src/Aion.GameServer/Program.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PvpApRewardServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6PF-Completion.md`

## What Changed

- Added `PvpApRewardService`.
- Ported source-derived PvP AP gain and loss formulas.
- Added the `AbyssRankEnum` PvP AP points gained/lost table for ranks 1-18 inside the planner.
- Applied configured `GameServerOptions.Rates.ApPvpGainRates` and `ApPvpLossRates`.
- Modeled AP boost as an explicit input pending live `PlayerGameStats.getStat(StatEnum.AP_BOOST)`.
- Planned member AP gain through `AbyssPointsService.AddApFromObject`.
- Planned victim AP loss through `AbyssPointsService.AddAp`.
- Registered the service in DI.

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~PvpApRewardServiceTests --no-restore
```

Result: passed, 13 tests.

```powershell
dotnet test dotnetConversion\AionServer.slnx --no-restore
```

Result: passed. Commons 57, Chat 29, Login 121, GameServer 1510.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.PvpService` | `Aion.GameServer.Services.PvpApRewardService` | Service / Reward Planner | Partial | Regression Tested in C# | Partial Parity | Models AP member gain and victim loss planner slices. Full damage-list/team extraction, kill counters, bounty/headhunting, quest/event hooks, PvP map handling, death broadcasts, XP/DP fanout, and live controller invocation remain missing. |
| `com.aionemu.gameserver.utils.stats.StatFunctions.calculatePvpApGained` | `PvpApRewardService.CalculatePvpApGained` | Utility / Reward Calculation | Partial | Regression Tested in C# | Partial Parity | Level penalties, under-level bonus, soldier rank penalty, and Java positive `Math.round` behavior are covered against source-derived cases. |
| `com.aionemu.gameserver.utils.stats.StatFunctions.calculatePvPApLost` | `PvpApRewardService.CalculatePvpApLost` / `ApplyVictimApLoss` | Utility / Loss Calculation | Partial | Regression Tested in C# | Partial Parity | Death-loss level penalties and AP-relevant damage scaling are covered. C# defensively guards non-positive total damage pending live integration. |
| `com.aionemu.gameserver.model.gameobjects.player.Rates.AP_PVP` | `GameServerRateOptions.ApPvpGainRates` / `PvpApRewardService.ApplyPvpGainRate` | Rate Calculation / Configuration Consumption | Partial | Regression Tested in C# | Partial Parity | Configured membership rates and AP boost input are applied with Java long-to-int fallback behavior. Live AP boost stat lookup remains missing. |
| `com.aionemu.gameserver.model.gameobjects.player.Rates.AP_PVP_LOST` | `GameServerRateOptions.ApPvpLossRates` / `PvpApRewardService.ApplyPvpLossRate` | Rate Calculation / Configuration Consumption | Partial | Regression Tested in C# | Partial Parity | Configured membership rates are applied to victim AP loss with Java long-to-int fallback behavior. |
| `com.aionemu.gameserver.utils.stats.AbyssRankEnum` | `PvpApRewardService` internal AP points table | Enum / Source Table Projection | Partial | Regression Tested in C# | Needs Verification | Points gained/lost for ranks 1-18 were copied for PvP AP formulas only. Required AP/GP, quotas, GP loss, names, and ranking behavior were not revalidated here. |
| `com.aionemu.gameserver.services.abyss.AbyssPointsService` | `Aion.GameServer.Services.AbyssPointsService` | Service | Partial | Regression Tested in C# | Partial Parity | PvP AP planner uses `AddApFromObject` for member gains and `AddAp` for victim losses. Full persistence, Legion contribution fanout, ranking cache, large-AP logging, and live siege callback execution remain incomplete. |
| `com.aionemu.gameserver.services.SiegeService.onAbyssPointsAdded` | `Aion.GameServer.Services.AbyssPointsSiegeCallback` | Service Callback / Intent DTO | Partial | Regression Tested in C# | Needs Verification | Regression validates player-source PvP AP gains create callback intent. Live `SiegeService` execution remains unported. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` | Server Packet | Partial | Regression Tested in C# | Needs Verification | Tests validate AP gain message id `1320000` and AP loss/use message id `1300965` are planned. Byte-level Java comparison remains unavailable. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ABYSS_RANK` | `Aion.GameServer.Network.Aion.ServerPackets.SmAbyssRank` | Server Packet | Partial | Regression Tested in C# | Needs Verification | Tests validate rank packet intent after PvP AP gain/loss mutation. Ranking-position lookup and byte-level Java comparison remain unavailable. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ApplyMemberApReward_CalculatesConfiguredRateAndAddsApThroughPlanner` | Regression | Java `PvpService.rewardPlayerTeam`, `StatFunctions.calculatePvpApGained`, `Rates.AP_PVP`, and `AbyssPointsService.addAp(Player, VisibleObject, int)` source review | Validates base AP `523`, AP-win multiplier, group damage/member `Math.round` share `262`, configured gain rate/AP boost producing `491`, AP mutation `1000 -> 1491`, AP gain packet intent, rank packet intent, and player-source siege callback intent. | Deterministic C# regression grounded in Java source. | No Java runtime artifact; live damage list/team extraction and kill counter are not wired. |
| `ApplyVictimApLoss_CalculatesConfiguredRateAndRemovesDamageShare` | Regression | Java `PvpService.doReward`, `StatFunctions.calculatePvPApLost`, and `Rates.AP_PVP_LOST` source review | Validates base loss `101`, configured loss rate to `151`, AP-relevant damage fraction to actual loss `90`, AP mutation `1000 -> 910`, AP-use packet intent, and rank packet intent. | Deterministic C# regression grounded in Java source. | No Java runtime artifact; live `apRelevantDamage` accumulation is not wired. |
| `CalculatePvpApGained_MatchesJavaLevelAndRankPenalties` | Regression | Java `StatFunctions.calculatePvpApGained` and `AbyssRankEnum` source review | Validates neutral level, high-level penalty, under-level bonus, and soldier rank penalty. | Deterministic C# formula regression. | No Java runtime artifact; only selected cases. |
| `CalculatePvpApLost_MatchesJavaLevelPenalties` | Regression | Java `StatFunctions.calculatePvPApLost` and `AbyssRankEnum` source review | Validates neutral, +3, +4, and +5 winner-level penalty cases. | Deterministic C# formula regression. | No Java runtime artifact; only selected cases. |
| `CalculateMemberApGain_UsesJavaMinimumAndRateFallbacks` | Regression | Java `PvpService.rewardPlayerTeam` and `Rates.get` source review | Validates daily-cap and zero-reward minimum AP `1`, empty-rate fallback to `1`, and AP boost truncation. | Deterministic C# regression. | AP boost is input-projected. |
| `ApplyVictimApLoss_SkipsMissingAndNonRelevantDamage` | Guard Regression | Java caller assumptions plus C# defensive planner boundary | Validates missing victim/winner and non-positive damage inputs do not mutate AP. | Deterministic C# guard regression. | Java does not explicitly guard every projected input. |
| `ApplyMemberApReward_SkipsMissingInputsAndNoEligibleMembers` | Guard Regression | Java `PvpService.rewardPlayerTeam` empty-player branch source review | Validates missing member/victim and no eligible members do not mutate AP. | Deterministic C# guard regression. | Live team membership filtering remains separate. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- `PvpApRewardService` is a planner slice; full `PvpService.doReward` integration, damage-list/team extraction, kill counters, bounty/headhunting, quest/event hooks, PvP map handling, XP/DP fanout, and death broadcasts remain missing.
- `StatEnum.AP_BOOST` and live `PlayerGameStats` stat lookup remain represented by an integer input.
- C# defensively guards non-positive `totalDamage`; Java relies on valid `DamageList` state, so this is an intentional planner-boundary difference pending live integration.
- Remaining AP callers in Quest, Trade, item purification, and Dredgion/basic PvP instances still need convergence through `AbyssPointsService`.
- Packet bytes, persistence, ranking cache, Legion contribution fanout, and live siege callback execution remain incomplete.

## Summary Metrics

- Total Java artifacts discovered: 10
- Total artifacts ported: 1 PvP AP reward/loss planner slice plus 1 DI registration
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 10
- Total blocked artifacts: 7 blocked/not-started categories, including Java runtime artifact generation, live PvP service integration, live AP boost stat lookup, damage-list/team extraction, quest/event/bounty side effects, remaining AP callers, and byte-level packet comparison
- Estimated overall migration completion: Phase 6 remains about 67% complete

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | PvP AP reward/loss planner | `PvpService`, `StatFunctions`, `Rates.AP_PVP`, `Rates.AP_PVP_LOST`, `AbyssRankEnum` | `PvpApRewardService.cs`, tests | Planner / Integration Prep | No with other AP service writes | Medium | Touches AP reward planning and shared AP planner behavior. Completed in UOW-910. |
| B | Quest AP reward planner | `QuestService`, `Rates.AP_QUEST` | likely new service/tests after analysis | Java Analysis / Planner | Yes read-only first | Low read-only | Config is available, but reward branch and template shape need a small design pass. |
| C | Dredgion/basic PvP instance AP planner | instance handlers, `Rates.AP_DREDGION` | read-only initially | Java Analysis | Yes read-only | Medium | Handler/runtime surfaces may be broader. |
| D | Live PvP adapter analysis | `PvpService`, C# combat/death surfaces | read-only initially | Integration Analysis | Yes read-only | Medium | Needs damage-list/team/death lifecycle support map before writes. |
| E | Trade/AP-purification analysis | `TradeService`, `ItemPurificationService` | read-only initially | Java Analysis | Yes read-only | Medium | Broader inventory/dialog surfaces likely required. |

## Next Recommended Unit of Work

Recommended sequential task:
- Add a compact Quest AP reward planner around Java `QuestService.giveReward`, `Rates.AP_QUEST`, and the newly bound `GameServerOptions.Rates.ApQuestRates`, unless a narrow live PvP adapter surface is discovered first.

Suggested safe parallel batch for the next session:

| Agent | Task | Allowed Files | Forbidden Files | Expected Result |
|---|---|---|---|---|
| Agent A | Analyze Java Quest AP reward behavior | read-only `QuestService.java`, `Rates.java`, quest reward templates | all writes | Behavior report and edge cases. |
| Agent B | Analyze live PvP adapter feasibility | read-only C# combat/death/team files and Java `PvpService.java` | all writes | Candidate integration map or blocker list. |
| Orchestrator | Implement one compact AP planner/integration slice | Exact production/test files chosen after discovery | Shared docs until final docs update | Code, tests, docs, commit. |

## Do Not Parallelize

- Multiple AP caller wiring tasks touching `AbyssPointsService`, `WorldNpcSoloDpRewardService`, `PvpApRewardService`, `PvpDpRewardService`, `QuestRewardService`, or shared AP tests.
- AP caller implementation with config edits unless one owner controls both.
- Legion contribution fanout with AP service edits unless one owner controls both.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest decompose docs, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting subagents.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, continue Quest AP, Dredgion AP, NPC team AP, or live PvP adapter convergence.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
