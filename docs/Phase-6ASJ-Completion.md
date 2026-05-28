# Phase 6ASJ Completion - NPC Target Change Packet Plan

Date: 2026-05-28
Unit of Work: UOW-1668
Status: Complete after focused packet-plan tests

## Scope

This unit added a non-live NPC target-change packet-plan boundary for Java `NpcController.onTargetChanged`.

The C# code now models the Java branches that lead to `SM_LOOKATOBJECT` packet creation, the talk-NPC delayed think branch, and the dead-NPC no-packet branch. It does not integrate with live NPC controllers, AI, scheduler, heading calculation, or broadcast utilities.

## Completed Work

- Added `NpcTargetChangePacketPlanService`.
- Added `NpcTargetChangePacketPlanInput`.
- Added `NpcTargetChangePacketPlan`.
- Added `NpcTargetChangePacketPlanStatus`.
- Modeled pre-dead-branch side-effect intents: clear attacked count and renew last target-change time.
- Modeled dead NPC no-packet branch.
- Modeled talk-info target-clear scheduled think intent with 750 ms delay.
- Modeled `SmLookAtObject` packet creation for target and no-talk-info target-clear branches.
- Added focused packet-plan tests.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, file ownership, parity table, tests, risks, metrics, and next-unit guidance.

## Java Parity Notes

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/controllers/NpcController.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LOOKATOBJECT.java`
  - `game-server/src/com/aionemu/gameserver/model/gameobjects/Npc.java`
  - `game-server/src/com/aionemu/gameserver/utils/PositionUtil.java`
  - `game-server/src/com/aionemu/gameserver/utils/ThreadPoolManager.java`
  - `game-server/src/com/aionemu/gameserver/utils/PacketSendUtility.java`
- Java always calls `clearAttackedCount()` and `getGameStats().renewLastChangeTargetTime()` before checking `isDead()`.
- Java schedules `AI.think()` after 750 ms when target clears for an NPC with talk info and target is still null.
- Java updates heading toward a non-self new target before broadcasting `SM_LOOKATOBJECT`.
- C# records these as non-live intents and packet plans.

## Validation

Ran:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~NpcTargetChangePacketPlanServiceTests|FullyQualifiedName~SmLookAtObjectPacketTests|FullyQualifiedName~GamePacketTests"
```

Result: passed 247 tests.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| NPC target-change packet-plan boundary | NPC target service/tests | Low | Yes | Closes the non-live packet factory boundary after `SM_LOOKATOBJECT` packet body port. |
| Run gated DB integration | DB integration harness | Medium | No | Deferred because no `AION_GAMESERVER_DB_*` env vars were present. |
| Zone handler source audit | read-only Java/C# docs | Low | No | Useful before live callback work, but outside NPC target-change scope. |
| Isolated packet audit | packet class/tests | Low | No | Still viable after NPC target boundary is documented. |

## File Ownership Map

| Agent | Scope | Allowed Files | Forbidden Files | Expected Output |
| --- | --- | --- | --- | --- |
| Orchestrator | NPC target-change packet-plan service, tests, docs, commit | `NpcTargetChangePacketPlanService.cs`, `NpcTargetChangePacketPlanServiceTests.cs`, progress/handoff docs | Java source writes, live NPC controller/broadcast integration, unrelated services/tests | Implemented and documented UOW-1668. |
| Sub-agents | None | None | All files | Not spawned because selected work touched a small service/test unit plus shared docs. |

No sub-agent was spawned for UOW-1668 because the selected boundary was small and shared docs remained Orchestrator-owned.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `CreatePlan_CreatesLookAtObjectPacketAndUsesHeadingTowardNonSelfTargetLikeJava` | Added | Non-self target selects heading-toward-target and creates `SmLookAtObject`. | Java non-null non-self target branch. |
| `CreatePlan_CreatesZeroTargetPacketWhenTargetClearsAndNpcHasNoTalkInfoLikeJava` | Added | Target clear without talk info creates zero-target packet using current heading. | Java null target/no talk-info else branch. |
| `CreatePlan_SchedulesThinkAndDoesNotBroadcastWhenTalkNpcTargetClearsLikeJava` | Added | Talk NPC target clear records 750 ms think schedule and no packet. | Java scheduled think branch. |
| `CreatePlan_DoesNotBroadcastWhenNpcIsDeadButKeepsPreDeadSideEffectsLikeJava` | Added | Dead NPC keeps clear/renew intents and produces no packet. | Java side effects before `if (!isDead())`. |
| `CreatePlan_BlocksInvalidNpcOwnerBeforeJavaSideEffects` | Added | Invalid NPC id blocks all side-effect intents. | Non-live C# safety boundary for Java live owner requirement. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.controllers.NpcController.onTargetChanged` | `Aion.GameServer.Services.NpcTargetChangePacketPlanService` | Controller Boundary | Partial | Unit Tested | Partial Parity | C# models clear-attacked-count and renew-last-target-change-time intents, dead branch, talk-info target-clear scheduled-think intent, heading selection, and `SmLookAtObject` packet creation. It does not integrate with live NPC controller state, AI, thread pool, PositionUtil, or PacketSendUtility. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LOOKATOBJECT` | `Aion.GameServer.Network.Aion.ServerPackets.SmLookAtObject` | Server Packet | Complete | Regression Tested | Partial Parity | Reused from UOW-1667; this unit verifies packet creation through the NPC target-change planner. No Java runtime golden/encrypted frame comparison. |
| `com.aionemu.gameserver.model.gameobjects.Npc` | `NpcTargetChangePacketPlanInput` | Model Boundary | Partial | Unit Tested | Partial Parity | C# uses explicit snapshot inputs for NPC id, target id, dead flag, talk-info flag, and headings. Live NPC state, object template lookup, clearAttackedCount mutation, GameStats timestamp mutation, and equality semantics remain unported. |
| `com.aionemu.gameserver.utils.PositionUtil.getHeadingTowards` | `NpcTargetChangePacketPlanInput.HeadingTowardTarget` | Utility Boundary | Not Started | Manual Only | Needs Verification | C# consumes a precomputed heading snapshot instead of calculating Java heading. Precision, rounding, coordinate handling, and byte conversion remain unverified. |
| `com.aionemu.gameserver.utils.ThreadPoolManager.schedule` | `NpcTargetChangePacketPlan.ShouldScheduleThink` | Scheduler Boundary | Partial | Unit Tested | Partial Parity | C# records a 750 ms scheduled-think intent for talk NPC target clear. It does not schedule a task, check target still null after delay, or call AI think. Threading/cancellation behavior remains unported. |
| `com.aionemu.gameserver.utils.PacketSendUtility.broadcastPacket` | future broadcast integration | Utility Boundary | Not Started | No Tests | Needs Verification | Live packet broadcast remains disabled. Recipient selection, ordering, source inclusion, visibility, and threading remain unverified. |

## Remaining Risks

- Live `NpcController.onTargetChanged` integration remains unported; no actual NPC state mutation, heading update, scheduler, AI think, or broadcast occurs.
- `PositionUtil.getHeadingTowards` is not ported in this unit; heading is supplied by snapshot.
- Threading/scheduler behavior for the 750 ms delayed AI think remains unverified.
- Broadcast recipient selection, packet ordering, source inclusion, visibility, and threading remain unverified.
- No Java runtime golden frame or encrypted frame comparison was produced for this workflow.
- Gated charge-all DB integration execution still needs a real MySQL environment.
- `docs/commit-conventions.md` is still missing.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped rows.
- Total artifacts ported: 1 non-live NPC target-change packet-plan service, 3 supporting DTO/enum artifacts, and 5 focused regressions.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 2 grouped rows explicitly marked Needs Verification; remaining rows are Partial Parity with documented non-live gaps.
- Total blocked artifacts: live NPC controller integration, Java heading calculation, live scheduler/AI think, broadcast utility integration, Java runtime workflow comparison, DB-backed charge-all integration run.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Next best unit:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| Run gated DB integration | disposable MySQL schema | Set `AION_GAMESERVER_DB_INTEGRATION=1` plus DB env vars if a DB is available. |
| `PositionUtil.getHeadingTowards` helper | position/heading utility tests | Add deterministic Java-derived heading helper to remove a snapshot gap in NPC target-change planning. |
| Another isolated packet parity unit | packet class/tests | Continue small packet-body parity while live dispatch remains incomplete. |

## Next Work Options

## Recommended Sequential Task

- Task: run the gated DB integration suite if a disposable DB is available; otherwise add a non-live `PositionUtil.getHeadingTowards` parity helper/test.
- Why: NPC target-change planning now depends on externally supplied heading; the Java heading calculation is the next small dependency gap.
- Files: likely no file changes for DB execution; otherwise a focused position/heading helper and tests plus docs.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
| --- | --- | --- | --- | --- |
| A | PositionUtil source audit | read-only Java `PositionUtil` and C# position helpers | Low | Convert to writes only after ownership is reserved. |
| B | Isolated packet audit | packet Java/C# tests read-only unless selected | Low | Avoid live dispatch work. |
| C | Zone handler source audit | read-only Java zone handler files | Low | Useful before live zone callbacks. |
| D | DB integration setup check | env/read-only status | Medium | Only if a disposable DB is known to be available. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
| --- | --- | --- | --- |
| Orchestrator | Pick DB run, heading helper, or next packet unit | exact selected files | Java writes, live NPC broadcast, unrelated shared files |
| Read-only Agent | Audit PositionUtil or next packet source | read-only inspection | all writes |

## Do Not Parallelize

- Java source files: read-only only.
- Live target dispatch, live NPC target broadcast, live scheduler mutation, live nearby dispatch, live weather mutation, live actor mutation, and live generated-zone writes: still high risk and intentionally disabled.
- Shared targeting helpers, packet helper/test fixtures, and DB integration setup: one owner only.

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1668] Add NPC target change packet plan
```

Files changed in this unit:

- `dotnetConversion/src/Aion.GameServer/Services/NpcTargetChangePacketPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NpcTargetChangePacketPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ASJ-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
