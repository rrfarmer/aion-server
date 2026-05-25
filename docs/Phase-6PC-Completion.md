# Phase 6PC Completion Handoff - NPC PvE AP Formula

Date: May 25, 2026
Unit of Work: UOW-907
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-907] Port NPC PvE AP formula`)

## Status

Phase 6 is still in progress. This unit closes the projected-input gap from UOW-906 by adding the Java PvE AP calculation used by NPC solo AP rewards.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/WorldNpcSoloDpRewardService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldNpcSoloDpRewardServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6PC-Completion.md`

## What Changed

- Added `WorldNpcSoloDpRewardService.CalculatePveApGained`.
- Added `WorldNpcSoloDpRewardService.GetApNpcRating`.
- Added `WorldNpcSoloDpRewardService.ApplySoloApRewardFromNpcStats`.
- Mirrored Java `StatFunctions.calculatePvEApGained` behavior for:
  - player more than 10 levels above the NPC returns `1`
  - AP NPC rating table: `JUNK=1`, `NORMAL=2`, `ELITE=4`, `HERO=35`, `LEGENDARY=2500`
  - exact case-sensitive `flame hoverstone` base-rate override
  - Java default AP PvE rates `[1.0, 2.0]`
  - membership-rate clamping to the last configured rate
  - empty-rate fallback to `1`
  - AP boost stat scaling by `current / 100f`
  - Java-style float product truncation and `Rates.calcResult(int)` overflow fallback

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~WorldNpcSoloDpRewardServiceTests --no-restore
```

Result: passed, 21 tests.

```powershell
dotnet test dotnetConversion\AionServer.slnx --no-restore
```

Result: passed. Commons 57, Chat 29, Login 121, GameServer 1496.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.utils.stats.StatFunctions.calculatePvEApGained` | `Aion.GameServer.Services.WorldNpcSoloDpRewardService.CalculatePveApGained` / `ApplySoloApRewardFromNpcStats` | Utility / Reward Calculation | Partial | Regression Tested in C# | Partial Parity | Rating table, over-level fallback, `flame hoverstone`, AP PvE membership rates, AP boost scaling, and truncation are covered. Live `Creature`/`Npc` integration, stat container lookup, config-bound rates, and Java runtime comparison remain missing. |
| `com.aionemu.gameserver.utils.stats.StatFunctions.getApNpcRating` | `Aion.GameServer.Services.WorldNpcSoloDpRewardService.GetApNpcRating` | Utility | Complete for known Java enum names | Regression Tested in C# | Partial Parity | Java values are ported. C# consumes string ratings from `NpcTemplateSummary`; Java consumes `NpcRating`, so unknown/case handling is an input-shape difference. |
| `com.aionemu.gameserver.model.gameobjects.player.Rates.AP_PVE` | `WorldNpcSoloDpRewardService.CalculatePveApGained` parameters `apPveRates` / `apBoostStat` | Rate Calculation / Input Projection | Partial | Regression Tested in C# | Partial Parity | Mirrors Java membership clamping, empty-rate fallback, AP boost percent scaling, and truncation. Rates are caller-supplied/defaulted; `GameServerOptions` does not yet bind `gameserver.rates.ap.pve`, and live `PlayerGameStats.getStat(AP_BOOST)` is not ported. |
| `com.aionemu.gameserver.configs.main.RatesConfig` | `Aion.GameServer.Configuration.GameServerRateOptions` / `CalculatePveApGained` default rate input | Configuration | Partial | Regression Tested at calculation boundary | Needs Verification | Java default AP PvE rates are represented in the calculation default, but config loading for `gameserver.rates.ap.pve` was not added here. Other AP rate arrays remain unmodeled. |
| `com.aionemu.gameserver.controllers.NpcController` | `WorldNpcSoloDpRewardService.ApplySoloApRewardFromNpcStats` | Controller Reward Slice / Service | Partial | Regression Tested in C# | Partial Parity | Solo NPC AP planner can now compute `calculatedAp` from NPC stats before reward scaling. Full controller invocation, AI ask dispatch, group/alliance distribution, XP/drop/tap-list side effects, and persistence remain missing. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CalculatePveApGained_MatchesJavaNpcRatingLevelAndSpecialNameRules` | Regression | Java `StatFunctions.calculatePvEApGained` and `getApNpcRating` source review | Validates AP rating values, over-level fallback, and exact lowercase `flame hoverstone` override. | Deterministic C# regression grounded in Java source. | No live Java runtime artifact; C# uses `NpcTemplateSummary` string values. |
| `CalculatePveApGained_AppliesJavaMembershipAndApBoostRates` | Regression | Java `Rates.AP_PVE` and `Rates.get` source review | Validates membership-rate clamping, empty-rate fallback, AP boost scaling, and float product truncation. | Deterministic C# regression grounded in Java source. | Config binding and live stat container lookup are missing. |
| `ApplySoloApRewardFromNpcStats_CalculatesPveApBeforeRewardScaling` | Regression | Java `NpcController.doReward`, `StatFunctions.calculatePvEApGained`, and `AbyssPointsService.addAp` source review | Validates calculated PvE AP feeds the solo AP reward planner, damage percent scales it, and AP mutates through the planner. | Deterministic C# regression grounded in Java source. | AI/controller live integration and Java runtime packet comparison remain unavailable. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- `GameServerOptions` still does not bind `gameserver.rates.ap.pve`; AP PvE rates are caller-supplied/defaulted in the calculation helper.
- `StatEnum.AP_BOOST` and live `PlayerGameStats` stat lookup are represented by an integer input, not a full stat container.
- C# uses string NPC ratings from `NpcTemplateSummary`; Java uses `NpcRating`, so unknown/case behavior is not a perfect type-level match.
- Remaining AP callers in PvP, Quest, Trade, item purification, Dredgion/basic PvP instances, and team distribution still need convergence through `AbyssPointsService`.
- Full NPC controller invocation, AI ask dispatch, group/alliance distribution, persistence, packet delivery, ranking cache, Legion contribution fanout, and live siege callback execution remain incomplete.

## Summary Metrics

- Total Java artifacts discovered: 5
- Total artifacts ported: 1 utility calculation slice plus 1 controller-facing overload
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 5
- Total blocked artifacts: 6 blocked/not-started categories, including Java runtime artifact generation, AP PvE config binding, live AP boost stat lookup, live NPC controller integration, team reward distribution, and byte-level packet comparison
- Estimated overall migration completion: Phase 6 remains about 66% complete

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | AP rate config binding | `RatesConfig`, `Rates` | `GameServerOptions.cs`, `GameServerOptionsTests.cs` | Configuration / Tests | Yes, if isolated | Medium | Separate from NPC reward service, but shared config file needs exclusive ownership. |
| B | PvP AP reward/loss analysis | `PvpService`, `StatFunctions.calculatePvPApLost`, `calculatePvpApGained` | read-only initially; likely `PvpDpRewardService.cs` and tests later | Java Analysis | Yes | Low read-only / Medium write | Read-only behavior mapping is independent; implementation needs isolated ownership. |
| C | Quest AP reward analysis | `QuestService`, `Rates.AP_QUEST` | read-only initially; likely `QuestRewardService.cs` and tests later | Java Analysis | Yes | Low read-only / Medium write | Needs AP quest rate/config model before production changes. |
| D | Trade/AP-purification analysis | `TradeService`, `ItemPurificationService` | read-only initially | Java Analysis | Yes | Medium | Broader inventory/dialog surfaces likely required. |
| E | Legion contribution fanout analysis | `AbyssPointsService`, Legion services | read-only initially | Java Analysis | Yes | Medium | Safe as analysis, but implementation touches shared AP/Legion surfaces. |

## Next Recommended Unit of Work

Recommended sequential task:
- Add config-bound AP rate options for `gameserver.rates.ap.pve`, `gameserver.rates.ap.quest`, `gameserver.rates.ap.pvp.gain`, `gameserver.rates.ap.pvp.loss`, and `gameserver.rates.ap.dredgion`, with option tests. This makes the AP caller slices less dependent on ad hoc rate inputs.

Suggested safe parallel batch for the next session:

| Agent | Task | Allowed Files | Forbidden Files | Expected Result |
|---|---|---|---|---|
| Agent A | Analyze Java PvP AP reward/loss behavior | read-only `game-server/src/com/aionemu/gameserver/services/PvpService.java`, `StatFunctions.java` | all writes | Behavior report and edge cases. |
| Agent B | Analyze Java Quest AP reward behavior | read-only `QuestService.java`, `Rates.java`, quest reward templates | all writes | Behavior report and config dependencies. |
| Orchestrator | Implement AP rate config binding | `GameServerOptions.cs`, `GameServerOptionsTests.cs`, docs | AP service/reward files unless needed after tests | Config implementation, tests, docs, commit. |

## Do Not Parallelize

- Multiple AP caller wiring tasks touching `AbyssPointsService`, `WorldNpcSoloDpRewardService`, `PvpDpRewardService`, or shared AP tests.
- AP rate config binding with another task editing `GameServerOptions.cs`.
- Legion contribution fanout with AP service edits unless one owner controls both.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest decompose docs, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting subagents.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, consider AP rate config binding first, then PvP AP or Quest AP caller convergence.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
