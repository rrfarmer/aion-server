# Phase 6ASF Completion - Target Select Input Resolution Plan

Date: 2026-05-28
Unit of Work: UOW-1664
Status: Complete after focused input-resolution tests

## Scope

This unit added a non-live `CM_TARGET_SELECT` input-resolution planner.

The C# code now models Java target-selection resolution outcomes before `player.setTarget(newTarget)`: clear target, self target, known visible target, invisible known target audit/clear, team-member fallback, unknown target clear, and assist-key target-of-target messages. This unit does not integrate the planner into `GameServerConnection.HandleTargetSelect`, mutate live targets, send system-message packets, or dispatch target-change packets.

## Completed Work

- Added `TargetSelectResolutionPlanService`.
- Added `TargetSelectResolutionInput`.
- Added `TargetSelectResolutionPlan`.
- Added `TargetSelectResolutionStatus`.
- Added `TargetSelectSystemMessage`.
- Added focused tests for direct selection, team fallback, invisible-target audit metadata, and assist-key early returns.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, file ownership, parity table, tests, risks, metrics, and next-unit guidance.

## Java Parity Notes

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_TARGET_SELECT.java`
  - `game-server/src/com/aionemu/gameserver/model/gameobjects/VisibleObject.java`
  - `game-server/src/com/aionemu/gameserver/model/knownlist/KnownList.java`
  - `game-server/src/com/aionemu/gameserver/model/team/PlayerTeam.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
  - `game-server/src/com/aionemu/gameserver/utils/audit/AuditLogger.java`
- Java `CM_TARGET_SELECT.runImpl` resolves `newTarget` and then calls `player.setTarget(newTarget)` unless an assist-key failure sends a system message and returns early.
- C# models that resolution as metadata and target snapshots. It does not claim live KnownList, team member, object-reference, system packet, or audit logger parity.

## Validation

Ran:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~TargetSelectResolutionPlanServiceTests|FullyQualifiedName~PlayerTargetChangePacketPlanServiceTests|FullyQualifiedName~SmTargetPacketsTests|FullyQualifiedName~GamePacketTests"
```

Result: passed 259 tests.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| `CM_TARGET_SELECT` input-resolution plan | target-select planner/tests | Low | Yes | Closes the next documented targeting gap without live dispatch. |
| Run gated DB integration | DB integration harness | Medium | No | Deferred because no `AION_GAMESERVER_DB_*` env vars were present. |
| Zone handler source audit | read-only Java/C# docs | Low | No | Useful before live callback work, but outside targeting input scope. |
| Isolated packet audit | packet class/tests | Low | No | Still viable after targeting input planning is documented. |

## File Ownership Map

| Agent | Scope | Allowed Files | Forbidden Files | Expected Output |
| --- | --- | --- | --- | --- |
| Orchestrator | Target-select input planner, tests, docs, commit | `TargetSelectResolutionPlanService.cs`, `TargetSelectResolutionPlanServiceTests.cs`, progress/handoff docs | Java source writes, live connection-handler integration, live target dispatch, unrelated services/tests | Implemented and documented UOW-1664. |
| Sub-agents | None | None | All files | Not spawned because selected work touched one planner/test pair plus shared docs. |

No sub-agent was spawned for UOW-1664 because the selected planner was small and shared docs remained Orchestrator-owned.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `CreatePlan_ClearsTargetWhenClientRequestsZeroLikeJava` | Added | Target id zero resolves to null and proceeds to set target. | Java `CM_TARGET_SELECT.runImpl` `targetObjectId == 0` branch. |
| `CreatePlan_SelectsSelfBeforeKnownListLookupLikeJava` | Added | Requesting player id resolves to self target. | Java self-target branch. |
| `CreatePlan_SelectsKnownVisibleObjectLikeJava` | Added | Known visible object resolves as target. | Java KnownList lookup plus sees check. |
| `CreatePlan_AuditsAndClearsInvisibleKnownObjectLikeJava` | Added | Invisible known object returns audit metadata and clears target. | Java invisible known target audit branch. |
| `CreatePlan_SelectsTeamMemberFallbackWhenKnownListMissesLikeJava` | Added | KnownList miss can resolve team member target. | Java team fallback branch. |
| `CreatePlan_ReturnsAssistMessageWhenSelectingTargetOfTargetWithoutCurrentTargetLikeJava` | Added | Assist with no current target returns the this-is-assist-key message. | Java assist-key early return. |
| `CreatePlan_ReturnsAssistNoUserWhenCurrentTargetHasNoTargetLikeJava` | Added | Current target without a target returns no-user. | Java assist-key early return. |
| `CreatePlan_SelectsVisibleTargetOfTargetLikeJava` | Added | Visible target-of-target resolves and proceeds to set target. | Java assist-key success branch. |
| `CreatePlan_ReturnsAssistTooFarWhenTargetOfTargetIsUnknownAndUnseenLikeJava` | Added | Unknown/unseen target-of-target returns too-far. | Java assist-key too-far branch. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_TARGET_SELECT` | `Aion.GameServer.Services.TargetSelectResolutionPlanService` | Client Packet / Input Boundary | Partial | Unit Tested | Partial Parity | C# models direct selection and assist-key resolution outcomes as a non-live plan. It does not read live `KnownList`, live team membership, actual `VisibleObject` references, or send system-message packets. |
| `com.aionemu.gameserver.model.gameobjects.VisibleObject.setTarget` | `TargetSelectResolutionPlan.ShouldCallSetTarget`; `PlayerTargetChangePacketPlanService` | Model Boundary | Partial | Unit Tested | Partial Parity | C# plan records whether Java would proceed to `player.setTarget`. Actual target mutation and controller callback remain outside this unit. Object-reference equality remains approximated by ids in the packet-plan boundary. |
| `com.aionemu.gameserver.model.gameobjects.KnownList.getObject/sees/knows` | `TargetSelectResolutionInput.KnownTargetObjectId`, visibility flags | Visibility Boundary | Partial | Unit Tested | Partial Parity | C# uses explicit test/input flags instead of live KnownList object lookup. Invisible known target audit and assist too-far/no-user branches are modeled, but visibility threading/spatial state is unported. |
| `com.aionemu.gameserver.model.team.PlayerTeam.hasMember/getMember` | `TargetSelectResolutionInput.TeamMemberObjectId` | Team Boundary | Partial | Unit Tested | Partial Parity | Team-member fallback is modeled as an input snapshot. Live team membership lookup, member object nullability, cross-server/offline cases, and team synchronization are unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `TargetSelectSystemMessage` | Server Packet / Message Boundary | Partial | Unit Tested | Partial Parity | C# returns message intents for assist-key failures: this-is-assist-key, no-user, and too-far. It does not instantiate or serialize `SM_SYSTEM_MESSAGE` packets. |
| `com.aionemu.gameserver.utils.audit.AuditLogger` | `TargetSelectResolutionPlan.AuditMessage` | Utility Boundary | Partial | Unit Tested | Partial Parity | Invisible known target audit is surfaced as metadata only. No live audit logging sink, player identity formatting, or persistence is invoked. |
| `com.aionemu.gameserver.controllers.PlayerController.onTargetChanged` | `PlayerTargetChangePacketPlanService` | Controller Boundary | Partial | Regression Tested | Partial Parity | This unit composes with the prior non-live packet-plan boundary but does not invoke it from `GameServerConnection.HandleTargetSelect`. Live owner send and sighted broadcast remain disabled. |

## Remaining Risks

- `GameServerConnection.HandleTargetSelect` still uses direct target id assignment and does not call `TargetSelectResolutionPlanService` or `PlayerTargetChangePacketPlanService`.
- Live KnownList lookup, sees/knows visibility, team membership fallback, object reference storage, and system-message packet sending remain unported.
- Assist-key known-but-not-visible no-user branch is represented by the service but lacks a dedicated focused test.
- Invisible-target audit is metadata only and does not invoke `AuditLogger`.
- No Java runtime comparison, live packet dispatch, or encrypted frame comparison was produced for target-select workflows.
- Gated charge-all DB integration execution still needs a real MySQL environment.
- `docs/commit-conventions.md` is still missing.

## Summary Metrics

- Total Java artifacts discovered: 7 grouped rows.
- Total artifacts ported: 1 non-live target-select input planner, 4 supporting DTO/enum artifacts, and 9 focused regressions.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 0 grouped rows explicitly marked Needs Verification; all rows are Partial Parity with documented non-live gaps.
- Total blocked artifacts: live target-select integration, KnownList visibility/object references, team lookup, system-message packet sending, audit logger invocation, target-change dispatch, Java runtime workflow comparison, DB-backed charge-all integration run.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Next best unit:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| Run gated DB integration | disposable MySQL schema | Set `AION_GAMESERVER_DB_INTEGRATION=1` plus DB env vars if a DB is available. |
| Compose target-select + target-change plans | non-live targeting adapter/tests | Connect resolution output to packet-plan output without mutating live connection state. |
| Add missing assist visibility test | target-select planner tests | Cover known-but-not-visible target-of-target -> no-user branch before adapter work. |

## Next Work Options

## Recommended Sequential Task

- Task: run the gated DB integration suite if a disposable DB is available; otherwise add the missing assist known-but-not-visible no-user branch test, then compose target-select resolution with target-change packet planning in a non-live adapter.
- Why: target-select resolution is now staged, but it is not yet connected to the packet-plan boundary from UOW-1663.
- Files: likely no file changes for DB execution; otherwise `TargetSelectResolutionPlanServiceTests.cs`, a focused adapter service/test pair, and docs.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
| --- | --- | --- | --- | --- |
| A | Adapter design | read-only target-select and target-change services/tests | Low | Convert to writes only after ownership is reserved. |
| B | Isolated packet audit | packet Java/C# tests read-only unless selected | Low | Avoid live dispatch work. |
| C | Zone handler source audit | read-only Java zone handler files | Low | Useful before live zone callbacks. |
| D | DB integration setup check | env/read-only status | Medium | Only if a disposable DB is known to be available. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
| --- | --- | --- | --- |
| Orchestrator | Pick DB run, adapter unit, missing branch test, or next packet unit | exact selected files | Java writes, live connection-handler integration, unrelated shared files |
| Read-only Agent | Audit adapter inputs or next packet source | read-only inspection | all writes |

## Do Not Parallelize

- Java source files: read-only only.
- Live target dispatch, live nearby dispatch, live weather mutation, live actor mutation, and live generated-zone writes: still high risk and intentionally disabled.
- Shared targeting helpers, packet helper/test fixtures, and DB integration setup: one owner only.

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1664] Add target select resolution plan
```

Files changed in this unit:

- `dotnetConversion/src/Aion.GameServer/Services/TargetSelectResolutionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/TargetSelectResolutionPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ASF-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
