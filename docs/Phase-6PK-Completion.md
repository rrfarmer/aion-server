# Phase 6PK Completion Handoff - Aturam Fixed AP Reward Planner

Date: May 25, 2026
Unit of Work: UOW-915
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-915] Add Aturam fixed AP reward planner`)

## Status

Phase 6 is still in progress. This unit adds the fixed Aturam AP reward planner using Java `AturamSkyFortressInstance.onDie` case `217382` and `AbyssPointsService.addAp(player, 540)` as the source of truth.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/AturamSkyFortressApRewardService.cs`
- `dotnetConversion/src/Aion.GameServer/Program.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AturamSkyFortressApRewardServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6PK-Completion.md`

## What Changed

- Added `AturamSkyFortressApRewardService`.
- Added fixed reward NPC id `217382` and fixed AP reward `540`.
- Added result/status records for applied, non-reward NPC, missing most-damage player, and AP-boundary skip.
- Applied AP through plain `AbyssPointsService.AddAp`, matching Java's no-source-object call.
- Registered the service in DI.

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~AturamSkyFortressApRewardServiceTests --no-restore
```

Result: passed, 3 tests.

```powershell
dotnet test dotnetConversion\AionServer.slnx --no-restore
```

Result: passed. Commons 57, Chat 29, Login 121, GameServer 1536.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `instance.AturamSkyFortressInstance` | `Aion.GameServer.Services.AturamSkyFortressApRewardService` | Instance Handler / Reward Planner | Partial | Regression Tested in C# | Partial Parity | Models the AP reward slice for NPC `217382`: most-damage player input and fixed AP `540`. Door state, NPC deletion, instance message, and live handler integration remain missing. |
| `com.aionemu.gameserver.model.gameobjects.Npc.getAggroList().getMostPlayerDamage` | `AturamSkyFortressApRewardService.ApplyGeneratorApReward(Player? mostDamagePlayer, ...)` input projection | Aggro / Input Projection | Partial Input Projection | Regression Tested in C# | Needs Verification | Recipient selection is caller-projected. Aggro ranking/tie behavior is not ported. |
| `com.aionemu.gameserver.services.abyss.AbyssPointsService` | `Aion.GameServer.Services.AbyssPointsService.AddAp` | Service | Partial | Regression Tested in C# | Partial Parity | Fixed AP reward mutates AP through the existing AP planner. Persistence, Legion contribution fanout, and ranking cache remain incomplete. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_MSG_IDStation_3FDoor_322` | Not ported in this unit | Server Packet / Instance Message | Not Started | No Tests | Unknown | Java broadcasts this after the AP branch; out of scope here. |
| `com.aionemu.gameserver.world.WorldMapInstance.setDoorState` | Not ported in this unit | Instance Door State | Not Started | No Tests | Unknown | Door `307` and `230` state changes remain live-instance work. |
| `instance.EternalBastionInstance` | Not ported in this unit | Instance Handler / AP Caller | Not Started | No Tests | Unknown | Final AP distribution was discovered but not implemented. |
| `instance.StonespearReachInstance` | Not ported in this unit | Instance Handler / AP Caller | Not Started | No Tests | Unknown | Final AP distribution plus GP/items was discovered but not implemented. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` | Server Packet | Partial | Regression Tested in C# | Needs Verification | AP gain message id `1320000` is planned. Byte-level Java comparison remains unavailable. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ABYSS_RANK` | `Aion.GameServer.Network.Aion.ServerPackets.SmAbyssRank` | Server Packet | Partial | Regression Tested in C# | Needs Verification | Rank packet intent is planned after AP mutation. Byte-level Java comparison remains unavailable. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ApplyGeneratorApReward_AddsFixedJavaRewardToMostDamagePlayer` | Regression | Java `AturamSkyFortressInstance.onDie` case `217382` and `AbyssPointsService.addAp(Player, int)` source review | Validates NPC `217382` applies fixed AP `540`, mutates AP, plans AP gain/rank packets, and has no siege callback. | Deterministic C# regression grounded in Java source. | Live aggro-list recipient selection remains missing. |
| `ApplyGeneratorApReward_SkipsMissingMostDamagePlayer` | Guard Regression | Java null check around most-damage player source review | Validates null most-damage player skips AP mutation. | Deterministic C# guard regression. | Live NPC aggro-list behavior is not ported. |
| `ApplyGeneratorApReward_SkipsNonRewardNpc` | Guard Regression | Java switch case source review and C# planner boundary | Validates non-`217382` NPC ids do not mutate AP. | Deterministic C# guard regression. | Java caller reaches this only through the instance switch. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- `AturamSkyFortressApRewardService` is an AP-only planner slice; live handler integration, aggro-list recipient selection, door state changes, NPC deletion, and instance message broadcast remain missing.
- Most-damage-player tie behavior, offline/dead-player behavior, and aggro-list cleanup were not verified.
- Eternal Bastion and Stonespear final AP callers remain unported, along with their GP/item reward side effects.
- Packet bytes, persistence, ranking cache, Legion contribution fanout, and live siege callback execution remain incomplete.

## Summary Metrics

- Total Java artifacts discovered: 9
- Total artifacts ported: 1 Aturam fixed AP reward planner slice plus 1 DI registration
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 9
- Total blocked artifacts: 7 blocked/not-started categories, including Java runtime artifact generation, live Aturam handler integration, aggro-list selection, door/message side effects, remaining final-AP instance callers, persistence/fanout side effects, and byte-level packet comparison
- Estimated overall migration completion: Phase 6 remains about 68% complete

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Aturam fixed AP grant | `AturamSkyFortressInstance` | `AturamSkyFortressApRewardService.cs`, tests, DI | Service Port | No with AP writes | Low | Completed in UOW-915. |
| B | Eternal Bastion final AP table | `EternalBastionInstance` | read-only initially | Java Analysis / Small Planner | Yes read-only | Low-Medium | Rank-to-final-AP table appears compact; item side effects should stay out of AP slice. |
| C | Stonespear final AP/GP table | `StonespearReachInstance` | read-only initially | Java Analysis / Small Planner | Yes read-only | Medium | Includes AP, GP, and item branches; split AP carefully. |
| D | Trade/AP-purification analysis | `TradeService`, `ItemPurificationService` | read-only initially | Java Analysis | Yes read-only | Medium | Broader inventory/dialog surfaces likely required. |

## Next Recommended Unit of Work

Recommended sequential task:
- Port Eternal Bastion final AP rank table and final AP application as the next compact instance AP planner.

Suggested safe parallel batch for the next session:

| Agent | Task | Allowed Files | Forbidden Files | Expected Result |
|---|---|---|---|---|
| Agent A | Analyze Eternal Bastion final AP table | read-only `EternalBastionInstance.java` | all writes | Rank/points/AP table and out-of-scope side effects. |
| Agent B | Analyze Stonespear final AP/GP table | read-only `StonespearReachInstance.java` | all writes | AP/GP/item split and candidate planner map. |
| Orchestrator | Implement one compact AP planner/integration slice | Exact production/test files chosen after discovery | Shared docs until final docs update | Code, tests, docs, commit. |

## Do Not Parallelize

- Multiple AP caller wiring tasks touching `AbyssPointsService`, `AturamSkyFortressApRewardService`, other AP planner services, or shared AP tests.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest decompose docs, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting subagents.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, continue Eternal Bastion/Stonespear final AP, Trade/AP-purification, admin AP paths, or live adapter convergence.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
