# Phase 6ASG Completion - Target Select Execution Plan Adapter

Date: 2026-05-28
Unit of Work: UOW-1665
Status: Complete after focused adapter tests

## Scope

This unit added a non-live adapter that composes `CM_TARGET_SELECT` resolution with target-change packet planning.

The C# code now bridges the staged resolution plan to the staged target-change packet plan when Java would continue to `player.setTarget(newTarget)`. Assist-key early returns remain message intents and do not create target-change packets. This unit does not integrate the adapter into `GameServerConnection.HandleTargetSelect`, mutate live player target state, send system-message packets, write audit logs, or dispatch packets.

## Completed Work

- Added `TargetSelectExecutionPlanService`.
- Added `TargetSelectExecutionPlan`.
- Added `TargetSelectExecutionPlanStatus`.
- Added the missing assist known-but-not-visible no-user branch test.
- Added focused adapter tests for target selection, target clearing, unchanged target, and assist early return.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, file ownership, parity table, tests, risks, metrics, and next-unit guidance.

## Java Parity Notes

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_TARGET_SELECT.java`
  - `game-server/src/com/aionemu/gameserver/model/gameobjects/VisibleObject.java`
  - `game-server/src/com/aionemu/gameserver/controllers/PlayerController.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_TARGET_SELECTED.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_TARGET_UPDATE.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
  - `game-server/src/com/aionemu/gameserver/utils/audit/AuditLogger.java`
- Java `CM_TARGET_SELECT.runImpl` either returns early after an assist-key system message or calls `player.setTarget(newTarget)`.
- Java `VisibleObject.setTarget` invokes `PlayerController.onTargetChanged` only when the target reference changes.
- Java `PlayerController.onTargetChanged` sends `SM_TARGET_SELECTED` to the owner and broadcasts `SM_TARGET_UPDATE` to sighted players.
- C# composes these behaviors as non-live plans and remains intentionally short of live connection integration.

## Validation

Ran:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~TargetSelectExecutionPlanServiceTests|FullyQualifiedName~TargetSelectResolutionPlanServiceTests|FullyQualifiedName~PlayerTargetChangePacketPlanServiceTests|FullyQualifiedName~SmTargetPacketsTests|FullyQualifiedName~GamePacketTests"
```

Result: passed 264 tests.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| Target-select execution adapter | targeting adapter/tests | Low | Yes | Composes already-staged resolution and packet-plan boundaries without live dispatch. |
| Run gated DB integration | DB integration harness | Medium | No | Deferred because no `AION_GAMESERVER_DB_*` env vars were present. |
| Zone handler source audit | read-only Java/C# docs | Low | No | Useful before live callback work, but outside targeting adapter scope. |
| Isolated packet audit | packet class/tests | Low | No | Still viable after targeting adapter is documented. |

## File Ownership Map

| Agent | Scope | Allowed Files | Forbidden Files | Expected Output |
| --- | --- | --- | --- | --- |
| Orchestrator | Target-select execution adapter, tests, docs, commit | `TargetSelectExecutionPlanService.cs`, `TargetSelectExecutionPlanServiceTests.cs`, `TargetSelectResolutionPlanServiceTests.cs`, progress/handoff docs | Java source writes, live connection-handler integration, live target dispatch, unrelated services/tests | Implemented and documented UOW-1665. |
| Sub-agents | None | None | All files | Not spawned because selected work touched a small adapter/test unit plus shared docs. |

No sub-agent was spawned for UOW-1665 because the selected adapter and tests were tightly coupled and shared docs remained Orchestrator-owned.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `CreatePlan_ReturnsAssistNoUserWhenTargetOfTargetIsKnownButNotVisibleLikeJava` | Added | Known but unseen target-of-target returns no-user and does not call set target. | Java `CM_TARGET_SELECT.runImpl` assist visibility branch. |
| `CreatePlan_ComposesResolutionAndTargetChangePacketsWhenJavaWouldSetNewTarget` | Added | Resolved known target composes owner selected-target and sighted-player target-update packets. | Java target-select -> setTarget -> onTargetChanged chain. |
| `CreatePlan_ComposesClearTargetPacketsWhenJavaWouldSetNullTarget` | Added | Clearing current target composes zero-target selected/update packet payloads. | Java clear-target branch plus target-change packets. |
| `CreatePlan_DoesNotCreatePacketsWhenResolvedTargetMatchesCurrentTarget` | Added | Unchanged target id returns no packet plan in the C# approximation. | Java `VisibleObject.setTarget` changed-reference guard, approximated by id. |
| `CreatePlan_PreservesAssistSystemMessageAndSkipsTargetChangeWhenJavaReturnsEarly` | Added | Assist no-user branch preserves message intent and skips target-change packet creation. | Java assist-key early return. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_TARGET_SELECT` | `Aion.GameServer.Services.TargetSelectExecutionPlanService` | Client Packet / Execution Boundary | Partial | Unit Tested | Partial Parity | C# composes resolution and target-change packet planning when Java would call `player.setTarget(newTarget)`, and preserves assist-key early returns. It does not run from `GameServerConnection.HandleTargetSelect`, mutate the live player, send `SM_SYSTEM_MESSAGE`, or invoke live audit/dispatch. |
| `com.aionemu.gameserver.model.gameobjects.VisibleObject.setTarget` | `TargetSelectExecutionPlan.TargetChangePacketPlan`; `PlayerTargetChangePacketPlanService` | Model Boundary | Partial | Regression Tested | Partial Parity | Adapter reaches the existing packet-plan guard, which approximates Java reference equality with object ids. Actual object reference assignment and controller callback are still not live. |
| `com.aionemu.gameserver.controllers.PlayerController.onTargetChanged` | `PlayerTargetChangePacketPlanService` through `TargetSelectExecutionPlanService` | Controller Boundary | Partial | Regression Tested | Partial Parity | Adapter creates the same non-live owner and sighted-player packet-plan objects used by the previous unit. It still does not call `PacketSendUtility.sendPacket` or `broadcastToSightedPlayers`. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_TARGET_SELECTED` | `Aion.GameServer.Network.Aion.ServerPackets.SmTargetSelected` | Server Packet | Complete | Regression Tested | Partial Parity | Adapter tests verify selected-target packet payloads for direct target selection and target clearing. No Java runtime golden/encrypted frame comparison. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_TARGET_UPDATE` | `Aion.GameServer.Network.Aion.ServerPackets.SmTargetUpdate` | Server Packet | Complete | Regression Tested | Partial Parity | Adapter tests verify target-update packet payloads for direct target selection and target clearing. Live sighted-player recipient selection and ordering remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `TargetSelectExecutionPlan.SystemMessage` | Server Packet / Message Boundary | Partial | Unit Tested | Partial Parity | Assist known-but-not-visible no-user branch is now covered as a message intent. The C# port still does not instantiate or serialize the real system-message packet for this workflow. |
| `com.aionemu.gameserver.utils.audit.AuditLogger` | `TargetSelectResolutionPlan.AuditMessage` | Utility Boundary | Partial | Regression Tested | Partial Parity | Existing invisible known target metadata remains non-live. No audit sink, formatting, persistence, or threading behavior is invoked. |

## Remaining Risks

- `GameServerConnection.HandleTargetSelect` still uses direct target id assignment and does not call the new execution adapter.
- Live player target mutation, `VisibleObject` object-reference storage, KnownList lookup, team lookup, system-message packet sending, audit logger invocation, owner packet send, and sighted-player broadcast remain unported.
- C# changed-target guard still uses target ids rather than Java object-reference equality.
- No Java runtime workflow comparison, live dispatch verification, or encrypted frame comparison was produced.
- Gated charge-all DB integration execution still needs a real MySQL environment.
- `docs/commit-conventions.md` is still missing.

## Summary Metrics

- Total Java artifacts discovered: 7 grouped rows.
- Total artifacts ported: 1 non-live execution adapter plus 5 focused regressions.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 0 grouped rows explicitly marked Needs Verification; all rows are Partial Parity with documented non-live gaps.
- Total blocked artifacts: live target-select connection integration, KnownList object references, team lookup, system-message packet send, audit logger invocation, target-change dispatch, Java runtime workflow comparison, DB-backed charge-all integration run.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Next best unit:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| Run gated DB integration | disposable MySQL schema | Set `AION_GAMESERVER_DB_INTEGRATION=1` plus DB env vars if a DB is available. |
| Non-live `HandleTargetSelect` adapter plan | connection-handler planning service/tests | Feed active-player state and target-select inputs into the staged targeting services without live mutation/dispatch. |
| Another isolated packet parity unit | packet class/tests | Continue small packet-body parity while live target integration remains intentionally deferred. |

## Next Work Options

## Recommended Sequential Task

- Task: run the gated DB integration suite if a disposable DB is available; otherwise add a non-live `GameServerConnection.HandleTargetSelect` adapter/handler plan.
- Why: target-select resolution and execution are now staged, but the connection handler still directly assigns target ids and has not been bridged to the staged services.
- Files: likely no file changes for DB execution; otherwise a focused handler-plan service/test pair plus docs.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
| --- | --- | --- | --- | --- |
| A | Handler adapter design | read-only connection handler and targeting services/tests | Low | Convert to writes only after ownership is reserved. |
| B | Isolated packet audit | packet Java/C# tests read-only unless selected | Low | Avoid live dispatch work. |
| C | Zone handler source audit | read-only Java zone handler files | Low | Useful before live zone callbacks. |
| D | DB integration setup check | env/read-only status | Medium | Only if a disposable DB is known to be available. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
| --- | --- | --- | --- |
| Orchestrator | Pick DB run, handler adapter unit, or next packet unit | exact selected files | Java writes, live connection-handler mutation, unrelated shared files |
| Read-only Agent | Audit connection handler or next packet source | read-only inspection | all writes |

## Do Not Parallelize

- Java source files: read-only only.
- Live target dispatch, live connection-handler mutation, live nearby dispatch, live weather mutation, live actor mutation, and live generated-zone writes: still high risk and intentionally disabled.
- Shared targeting helpers, packet helper/test fixtures, and DB integration setup: one owner only.

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1665] Add target select execution plan adapter
```

Files changed in this unit:

- `dotnetConversion/src/Aion.GameServer/Services/TargetSelectExecutionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/TargetSelectExecutionPlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/TargetSelectResolutionPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ASG-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
