# Phase 6PL Completion Handoff - Eternal Bastion Final AP Reward Planner

Date: May 25, 2026
Unit of Work: UOW-916
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-916] Add Eternal Bastion final AP reward planner`)

## Status

Phase 6 is still in progress. This unit adds the Eternal Bastion final AP reward planner using Java `EternalBastionInstance.getFinalRank`, `endInstance`, `distributeRewards`, and `AbyssPointsService.addAp` as the source of truth.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/EternalBastionApRewardService.cs`
- `dotnetConversion/src/Aion.GameServer/Program.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/EternalBastionApRewardServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6PL-Completion.md`

## What Changed

- Added `EternalBastionApRewardService`.
- Added final-rank calculation from Java points thresholds.
- Added final AP table for ranks 1-5.
- Preserved Java's `distributeRewards` behavior by routing present players through `AbyssPointsService.AddAp` even when final AP is zero.
- Registered the service in DI.
- Used read-only sub-agents for Stonespear and Trade/AP-purification analysis; both were closed after completion and made no writes.

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~EternalBastionApRewardServiceTests --no-restore
```

Result: passed, 9 tests.

```powershell
dotnet test dotnetConversion\AionServer.slnx --no-restore
```

Result: passed. Commons 57, Chat 29, Login 121, GameServer 1545.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `instance.EternalBastionInstance` | `Aion.GameServer.Services.EternalBastionApRewardService` | Instance Handler / Reward Planner | Partial | Regression Tested in C# | Partial Parity | Models final-rank AP calculation and per-player AP application. Lifecycle, score packets, NPC cleanup, item rewards, chest/exit spawning, and live handler integration remain missing. |
| `instance.EternalBastionInstance.getFinalRank` | `EternalBastionApRewardService.CalculateFinalRank` | Utility / Rank Calculation | Partial | Regression Tested in C# | Partial Parity | Java source thresholds are covered; older comment thresholds are not used. |
| `instance.EternalBastionInstance.endInstance` | `EternalBastionApRewardService.GetFinalAp` | Reward Table Projection | Partial | Regression Tested in C# | Partial Parity | AP table for ranks 1-5 is modeled. Item rewards and final chest spawning remain missing. |
| `instance.EternalBastionInstance.distributeRewards` | `EternalBastionApRewardService.ApplyFinalApReward` | Reward Application | Partial | Regression Tested in C# | Partial Parity | Java always calls AP add with final AP, including zero AP. Item rewards remain missing. |
| `com.aionemu.gameserver.services.abyss.AbyssPointsService` | `Aion.GameServer.Services.AbyssPointsService.AddAp` | Service | Partial | Regression Tested in C# | Partial Parity | Final AP mutates through existing AP planner. Persistence, Legion contribution fanout, and ranking cache remain incomplete. |
| `com.aionemu.gameserver.services.item.ItemService` | Not ported in this unit | Service | Not Started | No Tests | Unknown | Eternal Bastion item rewards are out of AP-only scope. |
| `instance.StonespearReachInstance` | Not ported in this unit | Instance Handler / AP/GP Caller | Not Started | Manual Analysis | Needs Verification | Explorer found AP only when `points > 67000 && bossKilled`, with AP `points / 10`; GP/items dominate full reward behavior. |
| `com.aionemu.gameserver.services.abyss.GloryPointsService` | Not ported in this unit | Service | Not Started | Manual Analysis | Unknown | Needed for Stonespear GP and broader arena/instance reward parity. |
| `com.aionemu.gameserver.services.TradeService` | Not ported in this unit | Service / AP Spend-Reward Caller | Not Started | Manual Analysis | Needs Verification | Explorer mapped AP formulas, but live C# wiring is blocked by missing trade/goods data and inventory mutation surfaces. |
| `com.aionemu.gameserver.services.item.ItemPurificationService` | Not ported in this unit | Service / AP Spend Caller | Not Started | Manual Analysis | Needs Verification | Explorer mapped AP precheck/spend behavior; C# lacks purification data and item upgrade mutation surfaces. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` | Server Packet | Partial | Regression Tested in C# | Needs Verification | AP gain message id `1320000` is planned for positive and zero final AP routes. Byte-level Java comparison unavailable. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ABYSS_RANK` | `Aion.GameServer.Network.Aion.ServerPackets.SmAbyssRank` | Server Packet | Partial | Regression Tested in C# | Needs Verification | Rank packet intent is planned after AP mutation; zero AP can also plan rank packet through existing C# rank recalculation. Byte-level Java comparison unavailable. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CalculateFinalRankAndAp_MatchesJavaThresholds` | Regression | Java `EternalBastionInstance.getFinalRank` and `endInstance` source review | Validates rank/AP mapping at Java thresholds. | Deterministic C# regression grounded in Java source. | No Java runtime artifact; item rewards not covered. |
| `ApplyFinalApReward_AddsRankBasedFinalApThroughPlanner` | Regression | Java `distributeRewards` and `AbyssPointsService.addAp` source review | Validates S-rank AP `35000`, AP mutation, AP gain packet intent, and rank packet intent. | Deterministic C# regression through existing AP planner. | Live instance player iteration and item rewards missing. |
| `ApplyFinalApReward_RoutesNoRankApLikeJavaDistributeRewards` | Regression | Java `distributeRewards` source review | Validates rank `8` final AP `0` still routes through AP planner and leaves AP unchanged. | Deterministic C# route regression. | Java runtime zero-AP packet behavior not captured. |
| `ApplyFinalApReward_SkipsMissingPlayer` | Guard Regression | C# planner boundary around Java live player iteration | Validates missing player returns rank/AP calculation without mutation. | Deterministic C# guard regression. | Java live caller normally supplies non-null players. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- `EternalBastionApRewardService` is an AP-only planner slice; live finalization, score packets, item rewards, chest/exit spawning, NPC deletion, and handler integration remain missing.
- Zero-AP packet behavior is only validated against current C# AP planner, not Java runtime output.
- Stonespear full reward behavior needs GP/item-aware modeling; Trade/Purification live wiring is blocked by missing data and inventory surfaces.
- Packet bytes, persistence, ranking cache, Legion contribution fanout, and live siege callback execution remain incomplete.

## Summary Metrics

- Total Java artifacts discovered: 12
- Total artifacts ported: 1 Eternal Bastion final AP reward planner slice plus 1 DI registration
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 12
- Total blocked artifacts: 8 blocked/not-started categories, including Java runtime artifact generation, live Eternal Bastion integration, item reward side effects, Stonespear GP/item/AP reward modeling, Trade/Purification data surfaces, persistence/fanout side effects, live packet ordering, and byte-level packet comparison
- Estimated overall migration completion: Phase 6 remains about 68% complete

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Eternal Bastion final AP | `EternalBastionInstance` | `EternalBastionApRewardService.cs`, tests, DI | Service Port | No with AP writes | Low-Medium | Completed in UOW-916. |
| B | Stonespear AP/GP reward | `StonespearReachInstance`, `GloryPointsService`, `LegionDominionScore` | read-only initially or isolated AP planner | Java Analysis / Small Planner | Yes read-only | Medium | AP-only slice is safe only for `points > 67000 && bossKilled`; full reward needs GP/items. |
| C | Trade/AP-purification formulas | `TradeService`, `TradeList`, `ItemPurificationService` | pure planner/test files only | Java Analysis / Planner | Maybe | Medium | Live wiring blocked; pure formula planners may be safe. |

## Next Recommended Unit of Work

Recommended sequential task:
- Either add a narrow Stonespear AP-only planner for `points > 67000 && bossKilled`, or add pure Trade AP formula planner tests without live packet wiring.

Suggested safe parallel batch for the next session:

| Agent | Task | Allowed Files | Forbidden Files | Expected Result |
|---|---|---|---|---|
| Agent A | Design Stonespear AP-only planner | read-only Java/C# or one new service/test pair if assigned | shared AP services/docs | AP branch test plan. |
| Agent B | Design pure Trade AP formula planner | read-only Java/C# or one new service/test pair if assigned | packet handlers/inventory/docs | Formula test plan. |
| Orchestrator | Implement one compact AP planner | Exact selected production/test files | Shared docs until final pass | Code, tests, docs, commit. |

## Do Not Parallelize

- Multiple AP caller wiring tasks touching `AbyssPointsService`, existing AP planner services, or shared AP tests.
- Stonespear GP planner with AP planner until GP service ownership is explicit.
- Trade/Purification live packet wiring with formula planner work.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest decompose docs, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting subagents.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, continue Stonespear AP branch, pure Trade AP formulas, admin AP paths, or live adapter convergence.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
