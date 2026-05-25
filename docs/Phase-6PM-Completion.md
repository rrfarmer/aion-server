# Phase 6PM Completion Handoff - Stonespear AP Branch Planner

Date: May 25, 2026
Unit of Work: UOW-917
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-917] Add Stonespear AP branch planner`)

## Status

Phase 6 is still in progress. This unit adds the narrow Stonespear AP-only planner using Java `StonespearReachInstance.checkRank`, `reward`, and `AbyssPointsService.addAp` as the source of truth.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/StonespearReachApRewardService.cs`
- `dotnetConversion/src/Aion.GameServer/Program.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/StonespearReachApRewardServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6PM-Completion.md`

## What Changed

- Added `StonespearReachApRewardService`.
- Added strict Java `>` final-rank thresholds.
- Added AP branch `points / 10` only for S-rank with `bossKilled == true`.
- Added positive-only AP application through `AbyssPointsService.AddAp`, matching Java's `if (reward.getFinalAp() > 0)`.
- Registered the service in DI.

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~StonespearReachApRewardServiceTests --no-restore
```

Result: passed, 12 tests.

```powershell
dotnet test dotnetConversion\AionServer.slnx --no-restore
```

Result: passed. Commons 57, Chat 29, Login 121, GameServer 1557.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `instance.StonespearReachInstance` | `Aion.GameServer.Services.StonespearReachApRewardService` | Instance Handler / AP Reward Planner | Partial | Regression Tested in C# | Partial Parity | Models only the AP branch. GP, items, score packet fields, finalization, and live handler integration remain missing. |
| `instance.StonespearReachInstance.checkRank` | `StonespearReachApRewardService.CalculateFinalRank` / `CalculateFinalAp` | Rank/AP Calculation | Partial | Regression Tested in C# | Partial Parity | Strict `>` thresholds and AP branch are covered. GP/item table is not ported. |
| `instance.StonespearReachInstance.reward` | `StonespearReachApRewardService.ApplyFinalApReward` | Reward Application | Partial | Regression Tested in C# | Partial Parity | Positive-only AP application is modeled. GP/item application remains missing. |
| `com.aionemu.gameserver.services.abyss.AbyssPointsService` | `Aion.GameServer.Services.AbyssPointsService.AddAp` | Service | Partial | Regression Tested in C# | Partial Parity | AP mutates through existing AP planner. Persistence, Legion contribution fanout, and ranking cache remain incomplete. |
| `com.aionemu.gameserver.services.abyss.GloryPointsService` | Not ported in this unit | Service | Not Started | Manual Analysis | Unknown | Needed for full Stonespear GP parity. |
| `com.aionemu.gameserver.services.item.ItemService` | Not ported in this unit | Service | Not Started | Manual Analysis | Unknown | Needed for full Stonespear item reward parity. |
| `com.aionemu.gameserver.model.instance.instancescore.LegionDominionScore` | Not ported in this unit | Score / Reward State | Not Started | Manual Analysis | Unknown | Score AP/GP/item fields remain unported. |
| `com.aionemu.gameserver.network.aion.instanceinfo.LegionDominionScoreWriter` | Not ported in this unit | Packet Writer | Not Started | Manual Analysis | Unknown | Score packet serialization remains unported. |
| `com.aionemu.gameserver.services.instance.LegionDominionService.onFinishInstance` | Not ported in this unit | Service Callback | Not Started | Manual Analysis | Unknown | Final legion dominion accounting remains unported. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` | Server Packet | Partial | Regression Tested in C# | Needs Verification | AP gain message id `1320000` is planned. Byte-level Java comparison unavailable. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ABYSS_RANK` | `Aion.GameServer.Network.Aion.ServerPackets.SmAbyssRank` | Server Packet | Partial | Regression Tested in C# | Needs Verification | Rank packet intent is planned after AP mutation. Byte-level Java comparison unavailable. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CalculateFinalRankAndAp_MatchesJavaStrictThresholds` | Regression | Java `StonespearReachInstance.checkRank` source review | Validates strict rank boundaries and AP branch. | Deterministic C# regression grounded in Java source. | GP/items not covered. |
| `ApplyFinalApReward_AddsUnscaledBossKilledSRankApThroughPlanner` | Regression | Java `checkRank` and `reward` AP branch source review | Validates S-rank boss-killed AP `points / 10`, AP mutation, AP gain packet intent, and rank packet intent. | Deterministic C# regression through existing AP planner. | Live finalization missing. |
| `ApplyFinalApReward_SkipsSRankWhenBossWasNotKilled` | Regression | Java boss-killed branch source review | Validates S-rank without boss kill produces no AP and does not call AP planner. | Deterministic C# regression. | S-rank GP/items without boss kill remain missing. |
| `ApplyFinalApReward_SkipsMissingPlayer` | Guard Regression | C# planner boundary around Java live player iteration | Validates missing player returns rank/AP calculation without mutation. | Deterministic C# guard regression. | Java live caller normally supplies players. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- `StonespearReachApRewardService` is AP-only; GP, items, score state, packet writer fields, finalization, and live handler integration remain missing.
- No C# `GloryPointsService` equivalent exists, blocking full Stonespear reward parity.
- Trade/AP-purification live wiring remains blocked by missing static data and inventory mutation surfaces.
- Packet bytes, persistence, ranking cache, Legion contribution fanout, and live siege callback execution remain incomplete.

## Summary Metrics

- Total Java artifacts discovered: 11
- Total artifacts ported: 1 Stonespear AP-only reward planner slice plus 1 DI registration
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 11
- Total blocked artifacts: 8 blocked/not-started categories, including Java runtime artifact generation, live Stonespear integration, GP planner, item reward side effects, LegionDominion score/packet state, Trade/Purification data surfaces, persistence/fanout side effects, and byte-level packet comparison
- Estimated overall migration completion: Phase 6 remains about 68% complete

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Stonespear AP-only branch | `StonespearReachInstance` | `StonespearReachApRewardService.cs`, tests, DI | Service Port | No with AP writes | Low-Medium | Completed in UOW-917. |
| B | Trade AP formulas | `TradeService`, `TradeList` | pure planner/test files only | Planner/Test | Maybe | Medium | Live wiring blocked, but pure formulas are isolated. |
| C | Item purification AP formula/precheck | `ItemPurificationService` | pure planner/test files only | Planner/Test | Maybe | Medium | Static data and item mutation surfaces missing. |

## Next Recommended Unit of Work

Recommended sequential task:
- Add pure Trade AP formula planner/tests for shop buy AP cost, AP resale reward, and trade-in AP delta without live packet wiring.

Suggested safe parallel batch for the next session:

| Agent | Task | Allowed Files | Forbidden Files | Expected Result |
|---|---|---|---|---|
| Agent A | Design/port Trade buy AP formula | isolated new service/test files if assigned | packet handlers, inventory, docs | Formula planner and tests. |
| Agent B | Design/port Trade resale or trade-in AP formula | isolated new service/test files if assigned | packet handlers, inventory, docs | Formula planner and tests. |
| Orchestrator | Integrate one selected formula slice | Exact selected files | Shared docs until final pass | Code, tests, docs, commit. |

## Do Not Parallelize

- Live Trade/Purification packet wiring with formula work.
- Multiple agents editing one Trade AP planner file.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest decompose docs, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting subagents.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, continue pure Trade AP formulas, ItemPurification AP precheck/spend planning, admin AP paths, or live adapter convergence.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
