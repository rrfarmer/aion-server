# Phase 6PD Completion Handoff - AP Rate Config Binding

Date: May 25, 2026
Unit of Work: UOW-908
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-908] Bind AP rate config`)

## Status

Phase 6 is still in progress. This unit binds the Java AP rate configuration arrays that future AP caller slices need.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Configuration/GameServerOptions.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerOptionsTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6PD-Completion.md`

## What Changed

- Added `GameServerRateOptions.ApPvpGainRates`.
- Added `GameServerRateOptions.ApPvpLossRates`.
- Added `GameServerRateOptions.ApPveRates`.
- Added `GameServerRateOptions.ApQuestRates`.
- Added `GameServerRateOptions.ApDredgionRates`.
- Bound Java property keys from `RatesConfig` / `rates.properties`:
  - `gameserver.rates.ap.pvp.gain`
  - `gameserver.rates.ap.pvp.loss`
  - `gameserver.rates.ap.pve`
  - `gameserver.rates.ap.quest`
  - `gameserver.rates.ap.dredgion`
- Added default and `mygs.properties` override assertions.

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~GameServerOptionsTests --no-restore
```

Result: passed, 4 tests.

```powershell
dotnet test dotnetConversion\AionServer.slnx --no-restore
```

Result: passed. Commons 57, Chat 29, Login 121, GameServer 1496.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.configs.main.RatesConfig` | `Aion.GameServer.Configuration.GameServerRateOptions` / `GameServerOptions.LoadFromJavaConfig` | Configuration | Partial | Regression Tested in C# | Partial Parity | C# now binds Java AP rate keys for PvP gain/loss, PvE, quest, and dredgion with Java defaults and `mygs.properties` overrides. Other Java rate arrays remain partially modeled. |
| `com.aionemu.gameserver.model.gameobjects.player.Rates.AP_PVP` | `GameServerRateOptions.ApPvpGainRates` | Rate Configuration | Partial | Regression Tested in C# | Needs Verification | Config array is available to future PvP AP reward code. AP boost stat multiplier, member distribution, and caller integration remain incomplete. |
| `com.aionemu.gameserver.model.gameobjects.player.Rates.AP_PVP_LOST` | `GameServerRateOptions.ApPvpLossRates` | Rate Configuration | Partial | Regression Tested in C# | Needs Verification | Config array is available to future PvP AP loss code. Java death-loss calculation and caller integration remain incomplete. |
| `com.aionemu.gameserver.model.gameobjects.player.Rates.AP_PVE` | `GameServerRateOptions.ApPveRates` | Rate Configuration | Partial | Regression Tested in C# | Partial Parity | Config array is bound with Java defaults; UOW-907 calculation still accepts rates as caller input and has not yet been wired to `GameServerOptions`. |
| `com.aionemu.gameserver.model.gameobjects.player.Rates.AP_QUEST` | `GameServerRateOptions.ApQuestRates` | Rate Configuration | Partial | Regression Tested in C# | Needs Verification | Config array is available to future quest AP reward code. `QuestService.giveReward` AP branch remains unported/unwired. |
| `com.aionemu.gameserver.model.gameobjects.player.Rates.AP_DREDGION` | `GameServerRateOptions.ApDredgionRates` | Rate Configuration | Partial | Regression Tested in C# | Needs Verification | Config array is available to future Dredgion/basic PvP instance reward code. Instance reward handlers remain outside this slice. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `LoadFromJavaConfig_ReadsCoreAndNetworkDefaults` | Regression | Java `RatesConfig` annotations and `game-server/config/main/rates.properties` source review | Validates default AP PvP gain/loss, PvE, quest, and dredgion rate arrays match Java defaults. | Deterministic C# config regression grounded in Java config source. | Does not execute Java `Rates.calcResult`; only verifies binding. |
| `LoadFromJavaConfig_AppliesMyGsOverridesLast` | Regression | Java property loader override behavior and AP rate property keys | Validates `mygs.properties` overrides all five AP rate arrays with float parsing and ordered values. | Deterministic C# config override regression. | Environment override path is shared but not specifically retested for AP keys. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- AP rate arrays are configured but not yet consumed by all AP reward/spend callers.
- `WorldNpcSoloDpRewardService.CalculatePveApGained` still receives AP PvE rates as a parameter and is not yet wired to `GameServerOptions`.
- Live `PlayerGameStats.getStat(StatEnum.AP_BOOST)` remains represented by an input in existing AP calculations.
- Remaining AP callers in PvP, Quest, Trade, item purification, Dredgion/basic PvP instances, and team distribution still need convergence through `AbyssPointsService`.
- Packet bytes, persistence, ranking cache, Legion contribution fanout, and live siege callback execution remain incomplete.

## Summary Metrics

- Total Java artifacts discovered: 6
- Total artifacts ported: 1 AP rate configuration slice covering 5 Java AP rate arrays
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 6
- Total blocked artifacts: 6 blocked/not-started categories, including Java runtime artifact generation, AP caller consumption of config rates, live AP boost stat lookup, PvP/Quest/Dredgion caller integration, persistence/fanout side effects, and byte-level packet comparison
- Estimated overall migration completion: Phase 6 remains about 66% complete

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | NPC AP options wiring | `NpcController`, `StatFunctions`, `Rates.AP_PVE` | `WorldNpcSoloDpRewardService.cs`, tests | Integration Fix | No with PvP AP | Medium | Touches NPC reward files and should own them exclusively. |
| B | PvP AP reward/loss analysis | `PvpService`, `StatFunctions.calculatePvPApLost`, `calculatePvpApGained` | read-only initially | Java Analysis | Yes | Low read-only | Independent from config binding and useful before implementation. |
| C | Quest AP reward analysis | `QuestService`, `Rates.AP_QUEST` | read-only initially | Java Analysis | Yes | Low read-only | Needs reward/config dependency map before production changes. |
| D | Dredgion/basic PvP instance AP analysis | `DredgionInstance`, `BasicPvpInstance`, `Rates.AP_DREDGION` | read-only initially | Java Analysis | Yes | Medium | Runtime handlers/instance reward surfaces may be broader. |
| E | Trade/AP-purification analysis | `TradeService`, `ItemPurificationService` | read-only initially | Java Analysis | Yes | Medium | Broader inventory/dialog surfaces likely required. |

## Next Recommended Unit of Work

Recommended sequential task:
- Wire `GameServerOptions.Rates.ApPveRates` into the NPC solo AP calculation path or start a compact PvP AP reward/loss planner around `PvpService` / `PvpDpRewardService` using the newly bound AP rate arrays.

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
5. If still tooling-blocked, use the newly bound AP rate arrays to continue NPC AP, PvP AP, Quest AP, or Dredgion AP convergence.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
