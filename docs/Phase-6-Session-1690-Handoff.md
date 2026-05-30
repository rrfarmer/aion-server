# Phase 6 Session 1690 Handoff

Date: 2026-05-30
Previous Unit: UOW-1689 (`SummonCommandReleaseSchedulePlanService`)
Completed Unit: UOW-1690 (`SummonModeChangePlanService`)

## Current Phase

Phase 6 remains active. Java is still the source of truth. Continue in small Units of Work, update parity/progress/handoff docs after every unit, and commit each completed unit.

## Last Completed Unit of Work

UOW-1690 added the non-live `SummonModeChangePlanService` covering Java `SummonsService.restMode`, `guardMode`, `attackMode`, and `setUnkMode`, along with four new `SmSystemMessage` factories (`STR_SKILL_SUMMON_ATTACK_MODE`, `STR_SKILL_SUMMON_GUARD_MODE`, `STR_SKILL_SUMMON_REST_MODE`, `STR_SKILL_SUMMON_ALREADY_HAVE_A_FOLLOWER`).

## Commits Made

- UOW-1690 committed as `[Phase 6][UOW-1690] Add summon mode change planner`.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/src/Aion.GameServer/Services/SummonModeChangePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/SummonModeChangePlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1690-Completion.md`
- `docs/Phase-6-Session-1690-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.summons.SummonsService.restMode`
- `com.aionemu.gameserver.services.summons.SummonsService.guardMode`
- `com.aionemu.gameserver.services.summons.SummonsService.attackMode`
- `com.aionemu.gameserver.services.summons.SummonsService.setUnkMode`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_SKILL_SUMMON_ATTACK_MODE`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_SKILL_SUMMON_GUARD_MODE`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_SKILL_SUMMON_REST_MODE`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_SKILL_SUMMON_ALREADY_HAVE_A_FOLLOWER`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` (4 new factories)
- `Aion.GameServer.Services.SummonModeChangePlanService`
- `Aion.GameServer.Services.SummonModeChangePlan`
- `Aion.GameServer.Services.SummonModeChangeType`
- `Aion.GameServer.Services.SummonModeChangePlanStatus`
- `Aion.GameServer.Tests.SummonModeChangePlanServiceTests`
- `Aion.GameServer.Tests.GamePacketTests` (4 new assertions)

## Tests Run

Focused test run covering `SummonModeChangePlanServiceTests`, `SummonCommandReleaseSchedulePlanServiceTests`, and `GamePacketTests`.

Result:

- 257 tests passed.
- Build succeeded for the focused test slice.

## Full Suite Status

The full test suite (`dotnet test AionServer.slnx`) passes 3694/3695 tests. The single failure is a pre-existing flaky timing test in `GameServerConnectionInventoryExpansionUseItemTests`. The specific test that fails changes between runs (the first full-suite run showed `ProcessPacketAsync_CompositeStonesSendsConsumedPacketsInJavaOrderForMixedDeletes`; the second showed `HandleUseItemAsync_ExtractDeletesLastSourceWithUseDeleteAndCubeUpdate`). Both share the same root cause: `WaitUntilAsync` timeouts under load. This was documented as a known pre-existing issue in Session 1688. It is not a regression from UOW-1690.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.summons.SummonsService.restMode` | `Aion.GameServer.Services.SummonModeChangePlanService.CreatePlan(Rest, ...)` | Service Boundary | Partial | Unit Tested | Partial Parity | Non-live REST message + SM_SUMMON_UPDATE intent; live side effects deferred. |
| `com.aionemu.gameserver.services.summons.SummonsService.guardMode` | `Aion.GameServer.Services.SummonModeChangePlanService.CreatePlan(Guard, ...)` | Service Boundary | Partial | Unit Tested | Partial Parity | Non-live GUARD message + SM_SUMMON_UPDATE intent; live side effects deferred. |
| `com.aionemu.gameserver.services.summons.SummonsService.attackMode` | `Aion.GameServer.Services.SummonModeChangePlanService.CreatePlan(Attack, ...)` | Service Boundary | Partial | Unit Tested | Partial Parity | Non-live ATTACK message + SM_SUMMON_UPDATE intent; live side effects deferred. |
| `com.aionemu.gameserver.services.summons.SummonsService.setUnkMode` | `Aion.GameServer.Services.SummonModeChangePlanService.CreatePlan(Unk, ...)` | Service Boundary | Partial | Unit Tested | Partial Parity | Non-live UNK SM_SUMMON_UPDATE only intent; live setMode deferred. |
| `SM_SYSTEM_MESSAGE.STR_SKILL_SUMMON_ATTACK_MODE` | `SmSystemMessage.SkillSummonAttackMode` | System Message Factory | Complete | Regression Tested | Verified Parity | messageId 1200008, one string param. |
| `SM_SYSTEM_MESSAGE.STR_SKILL_SUMMON_GUARD_MODE` | `SmSystemMessage.SkillSummonGuardMode` | System Message Factory | Complete | Regression Tested | Verified Parity | messageId 1200009, one string param. |
| `SM_SYSTEM_MESSAGE.STR_SKILL_SUMMON_REST_MODE` | `SmSystemMessage.SkillSummonRestMode` | System Message Factory | Complete | Regression Tested | Verified Parity | messageId 1200010, one string param. |
| `SM_SYSTEM_MESSAGE.STR_SKILL_SUMMON_ALREADY_HAVE_A_FOLLOWER` | `SmSystemMessage.SkillSummonAlreadyHaveAFollower` | System Message Factory | Complete | Regression Tested | Verified Parity | messageId 1300072, no params. |

## Known Gaps

- Live mode dispatch (`cancelCurrentSkill`, `setMode`, `triggerRestoreTask`, `cancelRestoreTask`) remains absent.
- `SummonsService.doMode` dispatch routing, dead-guard, and `cancelReleaseTask` are not modeled.
- `SummonsService.createSummon` spawn + packet sequence is not ported.
- Full-suite flaky timing test failure in `GameServerConnectionInventoryExpansionUseItemTests` (pre-existing, not a regression).

## Remaining Risks

1. Runtime mode behavior may diverge until `setMode`, life-stats tasks, and `cancelCurrentSkill` are ported.
2. Summon packet ordering evidence is source-derived unit evidence, not Java runtime/golden evidence.
3. `doMode` dispatch routing for the new mode planners remains unwired.

## Next Recommended Unit of Work

Preferred next small unit:

1. Add a non-live `SummonDoModePlanService` for Java `SummonsService.doMode`: dead-guard, null-master guard, `cancelReleaseTask` intent for non-release COMMAND transitions, and mode-dispatch routing.
2. Alternatively, model Java `SummonsService.createSummon` non-live packet sequence: `STR_SKILL_SUMMON_ALREADY_HAVE_A_FOLLOWER` guard, `SM_SUMMON_PANEL`, `SM_EMOTION(CHANGE_SPEED)` broadcast, `SM_SUMMON_UPDATE` broadcast — still non-live.
3. Or continue any other adjacent pure planner boundary (broker live-readiness, bind-point Kinah deduction, or NPC dialog packet).

## Suggested Sub-Agent Plan

No sub-agent is needed for the next small `doMode` or `createSummon` unit.

| Agent | Scope | Allowed Files | Forbidden Files | Expected Output |
|---|---|---|---|---|
| Orchestrator | C# doMode/createSummon planner/tests/docs | `SummonModeChangePlanService.cs`, adjacent planners/tests, progress/handoff docs | Java source writes, live summon lifecycle files | Implement only if scope remains non-live and isolated |

## Files That Should Not Be Edited Concurrently

- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1690-Completion.md`
- `docs/Phase-6-Session-1690-Handoff.md`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/src/Aion.GameServer/Services/SummonModeChangePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/SummonModeChangePlanServiceTests.cs`

## Context Needed By The Next Session

- Read `docs/csharp-port.md`, `docs/PHASE-6-PROGRESS.md`, this handoff, and the Session 1690 completion doc.
- Keep Java as source of truth.
- Treat UOW-1690 as complete and committed.
- The next adjacent deterministic gap is the `SummonsService.doMode` dispatch planner or the `SummonsService.createSummon` non-live packet sequence.
- Do not enable live summon mode dispatch, spawn, or lifecycle mutation until each prerequisite boundary is separately tested.
- The full-suite flaky timing failure in `GameServerConnectionInventoryExpansionUseItemTests` is a known pre-existing issue — do not treat it as a regression.
