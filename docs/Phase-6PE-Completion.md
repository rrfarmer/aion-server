# Phase 6PE Completion Handoff - NPC AP Rate Options Wiring

Date: May 25, 2026
Unit of Work: UOW-909
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-909] Wire NPC AP rates from options`)

## Status

Phase 6 is still in progress. This unit wires the AP PvE rate configuration bound in UOW-908 into the NPC solo AP reward calculation path added in UOW-907.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/WorldNpcSoloDpRewardService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldNpcSoloDpRewardServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6PE-Completion.md`

## What Changed

- Injected `GameServerOptions` into `WorldNpcSoloDpRewardService`.
- Stored `GameServerRateOptions` with Java-default fallback for tests or manual construction.
- Updated `ApplySoloApRewardFromNpcStats` so `GameServerOptions.Rates.ApPveRates` is used when no explicit AP PvE rate override is supplied.
- Preserved explicit `apPveRates` overrides for deterministic test projections and future call sites.
- Added a regression proving configured AP PvE rates affect calculated NPC AP and the final `AbyssPointsService` AP mutation.

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~WorldNpcSoloDpRewardServiceTests --no-restore
```

Result: passed, 22 tests.

```powershell
dotnet test dotnetConversion\AionServer.slnx --no-restore
```

Result: passed. Commons 57, Chat 29, Login 121, GameServer 1497.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.NpcController` | `Aion.GameServer.Services.WorldNpcSoloDpRewardService.ApplySoloApRewardFromNpcStats` | Controller Reward Slice / Service | Partial | Regression Tested in C# | Partial Parity | NPC solo AP calculation now consumes configured AP PvE rates through injected `GameServerOptions` when no override is supplied. Full controller invocation, AI ask dispatch, group/alliance distribution, XP/drop/tap-list side effects, and persistence remain missing. |
| `com.aionemu.gameserver.utils.stats.StatFunctions.calculatePvEApGained` | `WorldNpcSoloDpRewardService.CalculatePveApGained` / `ApplySoloApRewardFromNpcStats` | Utility / Reward Calculation | Partial | Regression Tested in C# | Partial Parity | Rating table, over-level fallback, special name handling, membership-rate selection, AP boost input, and truncation remain covered. Live `PlayerGameStats.getStat(StatEnum.AP_BOOST)` is still an input projection. |
| `com.aionemu.gameserver.model.gameobjects.player.Rates.AP_PVE` | `GameServerRateOptions.ApPveRates` / `WorldNpcSoloDpRewardService` | Rate Calculation / Configuration Consumption | Partial | Regression Tested in C# | Partial Parity | Java `Rates.AP_PVE.calcResult` membership-rate behavior is now represented by configured AP PvE rate consumption at the NPC solo reward boundary. No Java runtime comparison or team distribution path yet. |
| `com.aionemu.gameserver.configs.main.RatesConfig` | `Aion.GameServer.Configuration.GameServerOptions` / `GameServerRateOptions` | Configuration | Partial | Regression Tested in C# | Partial Parity | AP PvE key binding now has a consumer in NPC solo AP reward planning. PvP, Quest, and Dredgion AP rate arrays still need consumer wiring. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` | Server Packet | Partial | Regression Tested in C# | Needs Verification | Regression validates the AP gain planner mutation amount and existing packet intent path indirectly. Packet bytes and Java runtime ordering were not compared. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ABYSS_RANK` | `Aion.GameServer.Network.Aion.ServerPackets.SmAbyssRank` | Server Packet | Partial | Regression Tested in C# | Needs Verification | Regression validates the existing rank packet intent path remains reachable after configured-rate AP mutation. Ranking-position lookup and byte-level Java comparison remain unavailable. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ApplySoloApRewardFromNpcStats_UsesConfiguredApPveRatesWhenNoOverrideIsSupplied` | Regression | Java `NpcController.doReward`, `StatFunctions.calculatePvEApGained`, `Rates.AP_PVE`, and `RatesConfig.AP_PVE_RATES` source review | Validates injected `GameServerOptions.Rates.ApPveRates = [1.0, 1.25]` is used for membership `1`, producing calculated AP `75`, reward AP `75`, and player AP mutation `600 -> 675` without an explicit rate override. | Deterministic C# regression grounded in Java source and prior config binding. | No Java runtime artifact; AP boost remains an input; live NPC controller and group/team reward paths remain unwired. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- NPC solo AP now consumes configured AP PvE rates at the service boundary, but the live `NpcController`/combat reward invocation is still not wired.
- `StatEnum.AP_BOOST` and live `PlayerGameStats` stat lookup remain represented by an integer input.
- Group/alliance NPC AP distribution through `PlayerTeamDistributionService` is not ported.
- Remaining AP callers in PvP, Quest, Trade, item purification, and Dredgion/basic PvP instances still need convergence through `AbyssPointsService`.
- Packet bytes, persistence, ranking cache, Legion contribution fanout, and live siege callback execution remain incomplete.

## Summary Metrics

- Total Java artifacts discovered: 6
- Total artifacts ported: 1 AP PvE config-consumption slice at the NPC solo reward service boundary
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 6
- Total blocked artifacts: 6 blocked/not-started categories, including Java runtime artifact generation, live AP boost stat lookup, live NPC controller integration, team reward distribution, remaining AP caller consumption, and byte-level packet comparison
- Estimated overall migration completion: Phase 6 remains about 66% complete

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | NPC AP options wiring | `NpcController`, `StatFunctions.calculatePvEApGained`, `Rates.AP_PVE`, `RatesConfig` | `WorldNpcSoloDpRewardService.cs`, tests | Integration Fix | No with other AP reward writes | Medium | Touches NPC reward files and consumes shared AP rate options. Completed in UOW-909. |
| B | PvP AP reward/loss analysis | `PvpService`, `StatFunctions.calculatePvpApGained`, `StatFunctions.calculatePvPApLost`, `Rates.AP_PVP`, `Rates.AP_PVP_LOST` | read-only initially | Java Analysis | Yes | Low read-only | Best next AP caller candidate because config arrays are now available. |
| C | Quest AP reward analysis | `QuestService`, `Rates.AP_QUEST` | read-only initially | Java Analysis | Yes | Low read-only | Needs reward/config dependency map before production changes. |
| D | Dredgion/basic PvP instance AP analysis | `DredgionInstance`, `BasicPvpInstance`, `Rates.AP_DREDGION` | read-only initially | Java Analysis | Yes | Medium | Runtime handlers/instance reward surfaces may be broader. |
| E | Trade/AP-purification analysis | `TradeService`, `ItemPurificationService` | read-only initially | Java Analysis | Yes | Medium | Broader inventory/dialog surfaces likely required. |

## Next Recommended Unit of Work

Recommended sequential task:
- Start a compact PvP AP reward/loss planner around Java `PvpService`, `StatFunctions.calculatePvpApGained`, `StatFunctions.calculatePvPApLost`, and `Rates.AP_PVP` / `AP_PVP_LOST`, using the newly bound AP rate arrays where the C# surface can stay isolated.

Suggested safe parallel batch for the next session:

| Agent | Task | Allowed Files | Forbidden Files | Expected Result |
|---|---|---|---|---|
| Agent A | Analyze Java PvP AP reward/loss behavior | read-only `game-server/src/com/aionemu/gameserver/services/PvpService.java`, `StatFunctions.java`, `Rates.java` | all writes | Behavior report and edge cases. |
| Agent B | Analyze Java Quest AP reward behavior | read-only `QuestService.java`, `Rates.java`, quest reward templates | all writes | Behavior report and config dependencies. |
| Orchestrator | Implement one AP caller/config consumption slice | Exact production/test files chosen after discovery | Shared docs until final docs update | Code, tests, docs, commit. |

## Do Not Parallelize

- Multiple AP caller wiring tasks touching `AbyssPointsService`, `WorldNpcSoloDpRewardService`, `PvpDpRewardService`, `QuestRewardService`, or shared AP tests.
- AP caller implementation with config edits unless one owner controls both.
- Legion contribution fanout with AP service edits unless one owner controls both.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest decompose docs, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting subagents.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, use the newly bound AP rate arrays to continue PvP AP, Quest AP, Dredgion AP, or NPC team AP convergence.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
