# Phase 6PB Completion Handoff - NPC Solo AP Reward Planner

Date: May 25, 2026
Unit of Work: UOW-906
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-906] Add NPC solo AP reward planner`)

## Status

Phase 6 is still in progress. This unit advances AP caller convergence by adding the solo-player NPC AP reward planning/mutation slice beside the existing NPC solo DP reward service.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/WorldNpcSoloDpRewardService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldNpcSoloDpRewardServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6PB-Completion.md`

## What Changed

- Added `WorldNpcSoloDpRewardService.ApplySoloApReward`.
- Added `WorldNpcSoloDpRewardService.CalculateSoloApReward`.
- Added `WorldNpcSoloApRewardResult` and `WorldNpcSoloApRewardStatus`.
- Preserved Java breadcrumbs for `NpcController.doReward`, `AIQuestion.REWARD_AP`, `StatFunctions.calculatePvEApGained`, and `AbyssPointsService.addAp(player, npc, rewardAp)`.
- Exercised Java float scaling/truncation and the `rewardAp >= 1` gate.
- Routed applied NPC AP reward through `AbyssPointsService.AddApFromObject`, including ordinary NPC no-callback and non-peace siege NPC callback intent cases.

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~WorldNpcSoloDpRewardServiceTests --no-restore
```

Result: passed, 12 tests.

```powershell
dotnet test dotnetConversion\AionServer.slnx --no-restore
```

Result: passed. Commons 57, Chat 29, Login 121, GameServer 1487.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.NpcController` | `Aion.GameServer.Services.WorldNpcSoloDpRewardService.ApplySoloApReward` / `CalculateSoloApReward` | Controller Reward Slice / Service | Partial | Regression Tested in C# | Partial Parity | C# now models the solo-player NPC AP calculation and AP mutation plan. Full controller integration, group/alliance distribution, XP/drop/tap-list side effects, and actual AI/controller invocation remain missing. |
| `com.aionemu.gameserver.ai2.AIQuestion.REWARD_AP` | `WorldNpcSoloDpRewardService.ApplySoloApReward(bool shouldRewardAp, ...)` | AI Gate / Input Projection | Partial Input Projection | Regression Tested in C# | Needs Verification | The Java AI question result is accepted as a boolean input; the AI subsystem and ask dispatch are not wired. |
| `com.aionemu.gameserver.utils.stats.StatFunctions.calculatePvEApGained` | `WorldNpcSoloDpRewardService.ApplySoloApReward(int calculatedAp, ...)` | Utility Dependency / Input Projection | Not Started / Input Projection | Regression Tested in C# at consumer boundary | Needs Verification | PvE AP stat calculation is not ported here; `calculatedAp` remains caller-supplied. |
| `com.aionemu.gameserver.services.abyss.AbyssPointsService` | `Aion.GameServer.Services.AbyssPointsService.AddApFromObject` | Service | Partial | Unit + Regression Tested in C# | Partial Parity | NPC solo AP reward uses the object-source AP planner and validates AP gain/rank packet intent plus siege callback intent. Legion fanout, ranking cache, large-AP logging, persistence, and callback execution remain incomplete. |
| `com.aionemu.gameserver.services.SiegeService.onAbyssPointsAdded` | `Aion.GameServer.Services.AbyssPointsSiegeCallback` | Service Callback / Intent DTO | Partial | Regression Tested in C# | Needs Verification | Callback intent is created for non-peace siege NPC sources and omitted for ordinary NPCs; live `SiegeService` execution remains missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` | Server Packet | Partial | Regression Tested in C# | Needs Verification | Regression validates AP gain message id `1320000` through the AP planner; byte-level Java comparison remains unavailable. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ABYSS_RANK` | `Aion.GameServer.Network.Aion.ServerPackets.SmAbyssRank` | Server Packet | Partial | Regression Tested in C# | Needs Verification | Regression validates rank packet intent after NPC AP reward; ranking-position lookup and byte comparison remain unavailable. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ApplySoloApReward_CalculatesScalesAndAddsApThroughAbyssPlanner` | Regression | Java `NpcController.doReward`, `StatFunctions.calculatePvEApGained`, and `AbyssPointsService.addAp(Player, VisibleObject, int)` source review | Validates damage percent and AP multiplier scaling, AP mutation, AP gain message id `1320000`, rank packet intent, and no siege callback for ordinary NPCs. | Deterministic C# regression grounded in Java source. | Uses projected `calculatedAp`; no Java runtime artifact; no live controller integration. |
| `ApplySoloApReward_SkipsAiDeniedAndBelowJavaMinimumReward` | Regression | Java `AIQuestion.REWARD_AP` gate and `if (rewardAp >= 1)` branch source review | Validates AI-denied reward skips mutation, scaled reward `< 1` skips mutation, and `NaN` damage percent skips mutation through Java-style float handling. | Deterministic C# regression grounded in Java source. | AI subsystem not wired; Java runtime comparison unavailable. |
| `ApplySoloApReward_CreatesSiegeCallbackForNonPeaceSiegeNpc` | Regression | Java `AbyssPointsService.addAp(Player, VisibleObject, int)` siege callback branch source review | Validates non-peace siege NPC source creates an `AbyssPointsSiegeCallback` with player object id, source object id, and reward AP. | Deterministic C# callback-intent regression grounded in Java source. | Live `SiegeService.onAbyssPointsAdded` remains unported/unexecuted. |
| `ApplySoloApReward_SkipsMissingDeadTargets` | Regression | Existing Java reward guard behavior and C# DP reward guard parity | Validates null player, null NPC, and dead player outcomes do not mutate AP or create AP plans. | Deterministic C# guard regression. | Full Java controller/tap-list context not modeled. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- `StatFunctions.calculatePvEApGained` is not ported here; `calculatedAp` is a projected input.
- `AIQuestion.REWARD_AP` is represented as a boolean input; AI ask dispatch and NPC controller invocation remain unwired.
- Group/alliance reward distribution, PvP/Quest/Trade/AP-purification callers, persistence, packet delivery, ranking cache, full Legion contribution fanout, and live siege callback execution remain incomplete.
- Packet bytes and runtime ordering were not compared against Java.

## Summary Metrics

- Total Java artifacts discovered: 7
- Total artifacts ported: 1 production reward-planning slice plus 1 result DTO/status enum set
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 7
- Total blocked artifacts: 6 blocked/not-started categories, including Java runtime artifact generation, PvE AP stat calculation, AI ask integration, live NPC controller integration, live siege callback execution, and byte-level packet comparison
- Estimated overall migration completion: Phase 6 remains about 66% complete

## Next Recommended Unit of Work

Continue AP caller convergence by porting `StatFunctions.calculatePvEApGained` or wiring another compact AP caller through `AbyssPointsService` where the surrounding C# surface already exists.

Good candidates are quest AP reward planning once AP quest rates/config have a C# home, or a focused PvP AP reward/loss slice if the victim/member reward surfaces can stay isolated.

If AP caller work becomes too broad, continue isolated Legion domain groundwork or choose another independent non-AP Phase 6 slice.

If Java 25/Maven tooling becomes available, return to selectable-decompose artifact capture using the projection guide.

## Safe Parallel Work Candidates

| Candidate | Files | Parallel Safe? | Notes |
|---|---|---|---|
| PvE AP stat calculation analysis | Java `StatFunctions`, C# reward services/tests | Maybe | Safe as read-only; write work should own `WorldNpcSoloDpRewardService` and its tests. |
| Quest AP reward analysis | Java `QuestService`, C# quest reward service/options | Yes if read-only | Likely needs rate/config modeling before production changes. |
| PvP AP reward/loss analysis | Java `PvpService`, C# `PvpDpRewardService`, AP service tests | Yes if read-only | Write work should stay out of NPC reward files unless coordinated. |
| Legion domain analysis | Java Legion classes, C# model search | Yes if read-only | Prepare future Legion contribution fanout without touching AP reward code. |
| Java observer/runtime capture | Java diagnostic patch plus artifact files | No | Still tooling-blocked locally. |

## Do Not Parallelize

- Multiple AP caller wiring tasks touching `AbyssPointsService`, `WorldNpcSoloDpRewardService`, or shared AP tests.
- PvE AP formula work with NPC controller integration unless one owner controls both.
- Legion contribution fanout with AP service edits unless one owner controls both.
- Java observer implementation with live-server artifact capture unless one owner controls both.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest decompose docs, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting subagents.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, continue AP caller convergence with `StatFunctions.calculatePvEApGained`, Quest AP, PvP AP, Trade AP, or another compact AP path.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
