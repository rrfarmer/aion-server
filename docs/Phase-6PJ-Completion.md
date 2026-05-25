# Phase 6PJ Completion Handoff - PvP Arena AP Reward Planner

Date: May 25, 2026
Unit of Work: UOW-914
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-914] Add PvP Arena AP reward planner`)

## Status

Phase 6 is still in progress. This unit adds a compact PvP Arena AP reward planner using Java `PvPArenaInstance.calculateRewards`, `calculateIndividualReward`, `reward`, arena subclass config hooks, and `RatesConfig.PVP_ARENA_*_REWARD_RATES` as the source of truth.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Configuration/GameServerOptions.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PvpArenaApRewardService.cs`
- `dotnetConversion/src/Aion.GameServer/Program.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PvpArenaApRewardServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6PJ-Completion.md`

## What Changed

- Added `PvpArenaApRewardService`.
- Added arena AP reward item/result/status records.
- Added Java config bindings for `gameserver.rates.pvparena.discipline`, `chaos`, `harmony`, and `glory`.
- Added Java-style AP formula support for 70% rank pool, 30% score pool, rank reward rate, score rate, and configured membership rates.
- Added Java `Math.round(float)` behavior for Harmony-style group AP splits.
- Added positive-only arena AP application through `AbyssPointsService.AddAp`.
- Registered the service in DI.

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~PvpArenaApRewardServiceTests --no-restore
```

Result: passed, 6 tests.

```powershell
dotnet test dotnetConversion\AionServer.slnx --no-restore
```

Result: passed. Commons 57, Chat 29, Login 121, GameServer 1533.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `instance.pvparenas.PvPArenaInstance` | `Aion.GameServer.Services.PvpArenaApRewardService` | Instance Handler / Reward Planner | Partial | Regression Tested in C# | Partial Parity | Models AP formula/application only. Arena lifecycle, score storage, GP/items, revive, score packets, and live handler integration remain missing. |
| `instance.pvparenas.ArenaOfChaosInstance` | `PvpArenaKind.Chaos` / `GameServerRateOptions.PvpArenaChaosRewardRates` | Instance Handler / Config Projection | Partial | Regression Tested in C# | Needs Verification | Chaos reward-rate selection is modeled; reward tables are caller-projected. |
| `instance.pvparenas.ArenaOfDisciplineInstance` | `PvpArenaKind.Discipline` / `GameServerRateOptions.PvpArenaDisciplineRewardRates` | Instance Handler / Config Projection | Partial | Regression Tested in C# | Needs Verification | Discipline reward-rate selection is modeled; reward tables are caller-projected. |
| `instance.pvparenas.ArenaOfHarmonyInstance` | `PvpArenaKind.Harmony` / `PvpArenaApRewardService.CalculateIndividualApReward` | Instance Handler / Group Reward Projection | Partial | Regression Tested in C# | Partial Parity | Harmony rate selection and AP group split are modeled. Live associated-player reward assignment remains missing. |
| `instance.pvparenas.ArenaOfGloryInstance` | `PvpArenaKind.Glory` / `GameServerRateOptions.PvpArenaGloryRewardRates` | Instance Handler / Config Projection | Partial | Regression Tested in C# | Needs Verification | Glory reward-rate selection is modeled; reward tables and score writer remain missing. |
| `com.aionemu.gameserver.configs.main.RatesConfig.PVP_ARENA_*_REWARD_RATES` | `GameServerRateOptions.PvpArena*RewardRates` | Configuration / Rate Arrays | Partial | Regression Tested in C# | Partial Parity | Four Java config keys are bound with defaults `[1.0, 2.0]`. Runtime config-file comparison remains unavailable. |
| `com.aionemu.gameserver.model.gameobjects.player.Rates.get` | `PvpArenaApRewardService.SelectConfiguredRewardRate` | Rate Selection Utility | Partial | Regression Tested in C# | Partial Parity | Membership clamping and empty-rate fallback are covered. |
| `com.aionemu.gameserver.model.templates.rewards.ArenaRewardItem` | `Aion.GameServer.Services.PvpArenaApRewardItem` | DTO Projection | Partial | Regression Tested in C# | Needs Verification | AP base/ranking/score counts are modeled; item IDs and non-AP rewards remain outside this slice. |
| `com.aionemu.gameserver.model.instance.playerreward.PvPArenaPlayerReward` | `PvpArenaApRewardResult` / caller projection | Reward DTO Projection | Partial | Regression Tested in C# | Needs Verification | AP result is modeled; reward storage, rank, score points, GP/items, and serialization remain missing. |
| `com.aionemu.gameserver.model.instance.playerreward.HarmonyGroupReward` | `PvpArenaApRewardService.CalculateIndividualApReward` | Group Reward Projection | Partial | Regression Tested in C# | Partial Parity | AP component of Java group split is modeled with `Math.round`; associated-player iteration remains missing. |
| `com.aionemu.gameserver.services.abyss.AbyssPointsService` | `Aion.GameServer.Services.AbyssPointsService.AddAp` | Service | Partial | Regression Tested in C# | Partial Parity | Arena AP mutates AP only for positive totals. Persistence, Legion contribution fanout, and ranking cache remain incomplete. |
| `com.aionemu.gameserver.services.abyss.GloryPointsService` | Not ported in this unit | Service | Not Started | No Tests | Unknown | Java arena GP reward side effects remain outside AP-only scope. |
| `com.aionemu.gameserver.services.item.ItemService` | Not ported in this unit | Service | Not Started | No Tests | Unknown | Java arena item reward side effects remain outside AP-only scope. |
| `com.aionemu.gameserver.services.player.PlayerReviveService` | Not ported in this unit | Service | Not Started | No Tests | Unknown | Java arena revive-before-reward behavior remains outside this slice. |
| `instance.AturamSkyFortressInstance` | Not ported in this unit | Instance Handler / AP Caller | Not Started | No Tests | Unknown | Discovery found fixed AP grant `540`; not implemented here. |
| `instance.EternalBastionInstance` | Not ported in this unit | Instance Handler / AP Caller | Not Started | No Tests | Unknown | Discovery found final AP distribution; not implemented here. |
| `instance.StonespearReachInstance` | Not ported in this unit | Instance Handler / AP Caller | Not Started | No Tests | Unknown | Discovery found final AP distribution plus GP/items; not implemented here. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` | Server Packet | Partial | Regression Tested in C# | Needs Verification | AP gain message id `1320000` is planned. Byte-level Java comparison remains unavailable. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ABYSS_RANK` | `Aion.GameServer.Network.Aion.ServerPackets.SmAbyssRank` | Server Packet | Partial | Regression Tested in C# | Needs Verification | Rank packet intent is planned after AP mutation. Byte-level Java comparison remains unavailable. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CalculateApReward_AppliesJavaRankScorePoolsAndConfiguredRate` | Regression | Java `PvPArenaInstance.calculateRewards`, `ArenaOfChaosInstance.getConfigRate`, and arena rate config source review | Validates rank pool, score pool, score rate, configured rate, rank AP, score AP, base AP, and total AP. | Deterministic C# regression grounded in Java source. | Base reward table and rank reward rate are caller-projected. |
| `ApplyApReward_AddsPositiveArenaApThroughPlanner` | Regression | Java `PvPArenaInstance.reward` and `AbyssPointsService.addAp(Player, int)` source review | Validates positive AP total mutates AP and emits AP/rank packet intents. | Deterministic C# regression through existing AP planner. | Live reward storage and packet ordering remain missing. |
| `ApplyApReward_SkipsMissingPlayerAndZeroTotalReward` | Guard Regression | Java positive-total branch and C# planner boundary | Validates missing player and zero AP do not mutate AP or create packets. | Deterministic C# guard regression. | Java caller iterates live instance players. |
| `CalculateIndividualApReward_MatchesJavaHarmonyGroupMathRound` | Regression | Java `PvPArenaInstance.calculateIndividualReward` source review | Validates Java `Math.round(float)` for split AP reward components. | Deterministic C# regression. | Live Harmony group assignment remains missing. |
| `SelectConfiguredRewardRate_MatchesJavaMembershipFallbacks` | Regression | Java `Rates.get` and arena config source review | Validates membership clamping, empty-rate fallback, and override-rate selection. | Deterministic C# regression. | No Java runtime artifact. |
| `CalculateApReward_GuardsInvalidScoreProjectionInputs` | Guard Regression | Java valid-score assumptions plus C# planner boundary | Validates zero player count or zero total points returns base-only AP without division. | Deterministic C# guard regression. | Intentional planner-boundary guard pending live score integration. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- `PvpArenaApRewardService` is an AP-only planner slice; live arena lifecycle, score storage, rank calculation, score packets, morale effects, round timers, revive, GP/item rewards, reward-item selection, and live handler integration remain missing.
- Arena base rewards, rank reward rates, score points, total points, and interrupted-rank substitution are caller-projected inputs.
- C# defensively guards zero player count and zero total points; Java expects valid `PvPArenaScore`.
- Aturam, Eternal Bastion, and Stonespear AP callers were discovered but not implemented in this unit.
- Packet bytes, persistence, ranking cache, Legion contribution fanout, and live siege callback execution remain incomplete.

## Summary Metrics

- Total Java artifacts discovered: 22
- Total artifacts ported: 1 PvP Arena AP reward planner slice, 4 arena reward-rate config bindings, and 1 DI registration
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 22
- Total blocked artifacts: 9 blocked/not-started categories, including Java runtime artifact generation, live arena score/lifecycle integration, subclass reward tables, GP/item/revive side effects, simple remaining instance AP callers, Trade/AP-purification, persistence/fanout side effects, live packet ordering, and byte-level packet comparison
- Estimated overall migration completion: Phase 6 remains about 68% complete

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | PvP Arena AP planner | `PvPArenaInstance`, arena subclasses, arena reward rates | `PvpArenaApRewardService.cs`, config, tests, DI | Service Port | No with other AP/config writes | Medium | Completed in UOW-914. Shared config and DI ownership required one writer. |
| B | Aturam fixed AP grant | `AturamSkyFortressInstance` | read-only initially | Java Analysis / Small Planner | Yes read-only | Low | Fixed AP grant to most-damaging player; requires aggro/source projection if ported. |
| C | Eternal/Stonespear final AP grants | `EternalBastionInstance`, `StonespearReachInstance` | read-only initially | Java Analysis / Small Planner | Yes read-only | Medium | Final AP tables plus GP/item side effects need split. |
| D | Trade/AP-purification analysis | `TradeService`, `ItemPurificationService` | read-only initially | Java Analysis | Yes read-only | Medium | Broader inventory/dialog surfaces likely required. |

## Next Recommended Unit of Work

Recommended sequential task:
- Port the smallest fixed/final AP instance caller next, likely Aturam fixed AP grant or Eternal/Stonespear final AP planner, while documenting GP/item side effects as out of scope.

Suggested safe parallel batch for the next session:

| Agent | Task | Allowed Files | Forbidden Files | Expected Result |
|---|---|---|---|---|
| Agent A | Analyze Aturam fixed AP grant | read-only `AturamSkyFortressInstance.java` and C# NPC/aggro surfaces | all writes | Candidate planner map or blocker list. |
| Agent B | Analyze Eternal/Stonespear final AP tables | read-only `EternalBastionInstance.java`, `StonespearReachInstance.java` | all writes | AP table/report and dependency split. |
| Orchestrator | Implement one compact AP planner/integration slice | Exact production/test files chosen after discovery | Shared docs until final docs update | Code, tests, docs, commit. |

## Do Not Parallelize

- Multiple AP caller wiring tasks touching `AbyssPointsService`, `PvpArenaApRewardService`, `PvpInstanceApRewardService`, or shared AP tests.
- AP caller implementation with config edits unless one owner controls both.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest decompose docs, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting subagents.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, continue smaller remaining instance AP, Trade/AP-purification, admin AP paths, or live adapter convergence.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
