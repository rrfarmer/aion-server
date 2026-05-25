# Phase 6PI Completion Handoff - NPC Team AP Reward Planner

Date: May 25, 2026
Unit of Work: UOW-913
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-913] Add NPC team AP reward planner`)

## Status

Phase 6 is still in progress. This unit adds a compact NPC team-member AP reward planner using Java `PlayerTeamDistributionService.doReward`, `PlayerTeamRewardStats`, `StatFunctions.calculatePvEApGained`, `Rates.AP_PVE`, and `AbyssPointsService.addAp` as the source of truth.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/WorldNpcTeamApRewardService.cs`
- `dotnetConversion/src/Aion.GameServer/Program.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldNpcTeamApRewardServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6PI-Completion.md`

## What Changed

- Added `WorldNpcTeamApRewardService`.
- Added AP reward result/status records for NPC team-member AP rewards.
- Added caller-supplied input projections for Java's eligible filtered player count, AI AP reward decision, mentor AP suppression, and instance AP multiplier.
- Reused `WorldNpcSoloDpRewardService.CalculatePveApGained` to preserve PvE AP rating/membership/AP-boost behavior.
- Added Java-style float narrowing before integer group-share division.
- Added guards for missing member/NPC, no eligible player count, dead member, denied AP reward, mentor suppression, and below-minimum AP reward.
- Added siege callback intent for non-peace siege NPC sources.
- Registered the service in DI.

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~WorldNpcTeamApRewardServiceTests --no-restore
```

Result: passed, 6 tests.

```powershell
dotnet test dotnetConversion\AionServer.slnx --no-restore
```

Result: passed. Commons 57, Chat 29, Login 121, GameServer 1527.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.team.common.service.PlayerTeamDistributionService` | `Aion.GameServer.Services.WorldNpcTeamApRewardService` | Service / Reward Planner | Partial | Regression Tested in C# | Partial Parity | Models the AP reward slice for one already-filtered team member. Full team traversal, league/alliance expansion, range filtering, quest kill hook, XP/DP, loot, and live `NpcController` invocation remain missing. |
| `com.aionemu.gameserver.model.team.common.service.PlayerTeamDistributionService.PlayerTeamRewardStats` | `WorldNpcTeamApRewardService.ApplyMemberApRewardFromNpcStats` input projection | Internal Stats / Input Projection | Partial Input Projection | Regression Tested in C# | Needs Verification | Eligible count, mentor suppression, and living/dead member decisions are supplied by the caller. Future live callers must preserve Java denominator behavior where dead filtered non-mentor players can still count in `players.size()`. |
| `com.aionemu.gameserver.model.team.TemporaryPlayerTeam` | Not ported in this unit | Team Model | Not Started | No Tests | Unknown | Java team iteration dependency discovered; live C# team traversal remains future work. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAlliance` | Not ported in this unit | Team Model | Not Started | No Tests | Unknown | Java league/alliance expansion branch is not ported. |
| `com.aionemu.gameserver.configs.main.DropConfig.DISABLE_RANGE_CHECK_MAPS` | Not ported in this unit | Configuration / Range Filter | Not Started | No Tests | Unknown | Java disables range checks for configured maps; C# planner accepts already-filtered members. |
| `com.aionemu.gameserver.configs.main.GroupConfig.GROUP_MAX_DISTANCE` | Not ported in this unit | Configuration / Range Filter | Not Started | No Tests | Unknown | Java range filtering remains a live integration dependency. |
| `com.aionemu.gameserver.questEngine.QuestEngine.onKill` | Not ported in this unit | Quest Hook | Not Started | No Tests | Unknown | Java invokes quest kill handling for eligible filtered members before rewards; this planner excludes quest side effects. |
| `com.aionemu.gameserver.world.WorldMapInstance.getInstanceHandler().getApMultiplier` | `WorldNpcTeamApRewardService.ApplyMemberApRewardFromNpcStats(instanceApMultiplier)` | Instance Handler / Input Projection | Partial Input Projection | Regression Tested in C# | Needs Verification | Instance AP multiplier is supplied by the caller. Live handler lookup remains unwired. |
| `com.aionemu.gameserver.utils.stats.StatFunctions.calculatePvEApGained` | `WorldNpcSoloDpRewardService.CalculatePveApGained` reused by `WorldNpcTeamApRewardService` | Utility / Reward Calculation | Partial | Regression Tested in C# | Partial Parity | Existing PvE AP formula is reused; Java runtime comparison remains unavailable. |
| `com.aionemu.gameserver.model.gameobjects.player.Rates.AP_PVE` | `GameServerRateOptions.ApPveRates` / `WorldNpcTeamApRewardService` | Rate Calculation / Configuration Consumption | Partial | Regression Tested in C# | Partial Parity | Configured membership rates are consumed through `CalculatePveApGained`; live `StatEnum.AP_BOOST` remains an input. |
| `com.aionemu.gameserver.services.abyss.AbyssPointsService` | `Aion.GameServer.Services.AbyssPointsService.AddApFromObject` | Service | Partial | Regression Tested in C# | Partial Parity | Team AP planner mutates AP through the existing AP planner. Persistence, Legion contribution fanout, ranking cache, and live caller side effects remain incomplete. |
| `com.aionemu.gameserver.services.SiegeService.onAbyssPointsAdded` | `Aion.GameServer.Services.AbyssPointsSiegeCallback` | Service Callback / Intent DTO | Partial | Regression Tested in C# | Needs Verification | Non-peace siege NPC source creates callback intent; live `SiegeService` execution remains unported. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` | Server Packet | Partial | Regression Tested in C# | Needs Verification | Tests validate AP gain message id `1320000` is planned. Byte-level Java comparison remains unavailable. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ABYSS_RANK` | `Aion.GameServer.Network.Aion.ServerPackets.SmAbyssRank` | Server Packet | Partial | Regression Tested in C# | Needs Verification | Tests validate rank packet intent. Ranking-position lookup and byte-level Java comparison remain unavailable. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ApplyMemberApRewardFromNpcStats_CalculatesJavaTeamShareAndAddsAp` | Regression | Java `PlayerTeamDistributionService.doReward`, `StatFunctions.calculatePvEApGained`, `Rates.AP_PVE`, and `AbyssPointsService.addAp(Player, VisibleObject, int)` source review | Validates PvE AP, damage and instance multiplier scaling, group-share AP, AP mutation, AP gain packet intent, rank packet intent, and no ordinary siege callback. | Deterministic C# regression grounded in Java source. | No Java runtime artifact; live team filtering remains missing. |
| `ApplyMemberApRewardFromNpcStats_UsesConfiguredApPveRatesWhenNoOverrideIsSupplied` | Regression | Java `Rates.AP_PVE` and config source review | Validates injected `ApPveRates` are used for team AP without an explicit override. | Deterministic C# regression grounded in Java source and prior config binding. | AP boost remains an input projection. |
| `CalculateTeamMemberApReward_MatchesJavaFloatNarrowingAndGroupDivision` | Regression | Java `(int) rewardAp / players.size()` source review | Validates Java-style float narrowing, integer group-share division, NaN cast behavior, and no-eligible guard. | Deterministic C# formula regression. | No Java runtime artifact. |
| `ApplyMemberApRewardFromNpcStats_SkipsAiDeniedMentorSuppressedAndBelowMinimum` | Guard Regression | Java `AIQuestion.REWARD_AP`, mentor group AP condition, and `ap >= 1` branch source review | Validates denied AI reward, mentor AP suppression, and below-minimum AP do not mutate AP. | Deterministic C# guard regression. | Mentor count/config evaluation is caller-projected. |
| `ApplyMemberApRewardFromNpcStats_SkipsMissingDeadAndNoEligibleTargets` | Guard Regression | Java dead-member skip and C# planner boundary | Validates missing member, missing NPC, no eligible players, and dead member do not mutate AP. | Deterministic C# guard regression. | Future live caller must preserve Java filtered-player denominator behavior. |
| `ApplyMemberApRewardFromNpcStats_CreatesSiegeCallbackForNonPeaceSiegeNpc` | Regression | Java `AbyssPointsService.addAp(Player, VisibleObject, int)` and `SiegeService.onAbyssPointsAdded` source review | Validates non-peace siege NPC source creates siege callback intent. | Deterministic C# regression through existing AP planner. | Live `SiegeService` execution remains unported. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- `WorldNpcTeamApRewardService` is a planner slice; live team filtering, league/alliance expansion, map/range checks, quest kill hooks, XP/DP rewards, loot/kinah distribution, and `NpcController` reward invocation remain missing.
- Java denominator behavior is subtle: `filteredStats.players.size()` can include dead non-mentor filtered members while the loop skips the dead member. The C# planner preserves this only if future callers pass the Java-equivalent eligible count.
- `AIQuestion.REWARD_AP`, `CustomConfig.MENTOR_GROUP_AP`, mentor counting, and instance AP multiplier are input projections until their live C# homes are ready.
- `StatEnum.AP_BOOST` and live `PlayerGameStats` stat lookup remain represented by an integer input.
- Remaining AP callers in Trade, item purification, remaining instance handlers, admin paths, and live adapters still need convergence through `AbyssPointsService` as appropriate.
- Packet bytes, persistence, ranking cache, Legion contribution fanout, and live siege callback execution remain incomplete.

## Summary Metrics

- Total Java artifacts discovered: 14
- Total artifacts ported: 1 NPC team-member AP reward planner slice plus 1 DI registration
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 14
- Total blocked artifacts: 8 blocked/not-started categories, including Java runtime artifact generation, live team filtering, league/alliance traversal, quest kill hooks, XP/DP/loot side effects, remaining AP callers, persistence/fanout side effects, and byte-level packet comparison
- Estimated overall migration completion: Phase 6 remains about 67% complete

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | NPC team AP planner | `PlayerTeamDistributionService`, `PlayerTeamRewardStats`, `StatFunctions.calculatePvEApGained`, `Rates.AP_PVE` | `WorldNpcTeamApRewardService.cs`, tests, DI | Service Port | No with other AP writes | Medium | Completed in UOW-913. Shared AP planner and DI ownership required one writer. |
| B | Remaining instance AP callers | `AturamSkyFortressInstance`, `EternalBastionInstance`, `StonespearReachInstance`, `PvPArenaInstance` | read-only initially | Java Analysis | Yes read-only | Medium | Determine which use simple AP plans versus broad instance reward models. |
| C | Trade/AP-purification analysis | `TradeService`, `ItemPurificationService` | read-only initially | Java Analysis | Yes read-only | Medium | Broader inventory/dialog surfaces likely required. |
| D | Live adapter analysis | existing AP planners and C# caller surfaces | read-only initially | Integration Analysis | Yes read-only | Medium | Map narrow live integration points without expanding runtime scope. |

## Next Recommended Unit of Work

Recommended sequential task:
- Analyze remaining instance AP callers and implement the smallest isolated planner if their Java AP reward behavior can be projected cleanly.

Suggested safe parallel batch for the next session:

| Agent | Task | Allowed Files | Forbidden Files | Expected Result |
|---|---|---|---|---|
| Agent A | Analyze remaining instance AP callers | read-only `AturamSkyFortressInstance.java`, `EternalBastionInstance.java`, `StonespearReachInstance.java`, `PvPArenaInstance.java` | all writes | Behavior report and dependency split. |
| Agent B | Analyze Trade/AP-purification AP paths | read-only `TradeService.java`, `ItemPurificationService.java`, related handlers | all writes | Blocker/dependency map and candidate slice. |
| Orchestrator | Implement one compact AP planner/integration slice | Exact production/test files chosen after discovery | Shared docs until final docs update | Code, tests, docs, commit. |

## Do Not Parallelize

- Multiple AP caller wiring tasks touching `AbyssPointsService`, `WorldNpcSoloDpRewardService`, `WorldNpcTeamApRewardService`, `PvpApRewardService`, `QuestRewardService`, `PvpInstanceApRewardService`, or shared AP tests.
- AP caller implementation with config edits unless one owner controls both.
- Legion contribution fanout with AP service edits unless one owner controls both.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest decompose docs, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting subagents.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, continue remaining instance AP, Trade/AP-purification, admin AP paths, or live adapter convergence.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
