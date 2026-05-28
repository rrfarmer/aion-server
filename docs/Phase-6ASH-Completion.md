# Phase 6ASH Completion - Target Select Handler Plan Boundary

Date: 2026-05-28
Unit of Work: UOW-1666
Status: Complete after focused handler-plan tests

## Scope

This unit added a non-live handler-plan layer for the current C# `GameServerConnection.HandleTargetSelect` boundary.

The C# code now has a read-only adapter that takes active-player state plus target-select context and feeds it through the staged target-select resolution and execution services. It intentionally does not replace the live connection handler, mutate `Player.TargetObjectId`, send system-message packets, write audit logs, send owner packets, or broadcast target updates.

## Completed Work

- Added `TargetSelectHandlerPlanService`.
- Added `TargetSelectHandlerInput`.
- Added `TargetSelectHandlerPlan`.
- Added `TargetSelectHandlerPlanStatus`.
- Added focused tests for known-target selection, target clearing, assist early return, and team-member fallback.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, file ownership, parity table, tests, risks, metrics, and next-unit guidance.

## Java Parity Notes

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_TARGET_SELECT.java`
  - `game-server/src/com/aionemu/gameserver/model/gameobjects/VisibleObject.java`
  - `game-server/src/com/aionemu/gameserver/controllers/PlayerController.java`
  - `game-server/src/com/aionemu/gameserver/model/gameobjects/KnownList.java`
  - `game-server/src/com/aionemu/gameserver/model/team/PlayerTeam.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
- Existing C# source inspected:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- Java resolves the target from packet input and live `Player` state, then either returns early with an assist-key system message or calls `player.setTarget(newTarget)`.
- Current C# live handler still uses a direct target-id assignment for non-assist packets and logs assist selection as unported.
- New C# handler plan documents the Java-shaped path but stays non-live.

## Validation

Ran:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~TargetSelectHandlerPlanServiceTests|FullyQualifiedName~TargetSelectExecutionPlanServiceTests|FullyQualifiedName~TargetSelectResolutionPlanServiceTests|FullyQualifiedName~PlayerTargetChangePacketPlanServiceTests|FullyQualifiedName~SmTargetPacketsTests|FullyQualifiedName~GamePacketTests"
```

Result: passed 268 tests.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| `HandleTargetSelect` handler-plan layer | targeting handler service/tests | Low | Yes | Bridges C# active-player state into staged targeting plans without changing live handler behavior. |
| Run gated DB integration | DB integration harness | Medium | No | Deferred because no `AION_GAMESERVER_DB_*` env vars were present. |
| Zone handler source audit | read-only Java/C# docs | Low | No | Useful before live callback work, but outside targeting handler scope. |
| Isolated packet audit | packet class/tests | Low | No | Still viable after targeting handler plan is documented. |

## File Ownership Map

| Agent | Scope | Allowed Files | Forbidden Files | Expected Output |
| --- | --- | --- | --- | --- |
| Orchestrator | Target-select handler-plan service, tests, docs, commit | `TargetSelectHandlerPlanService.cs`, `TargetSelectHandlerPlanServiceTests.cs`, progress/handoff docs | Java source writes, live `GameServerConnection` mutation, live target dispatch, unrelated services/tests | Implemented and documented UOW-1666. |
| Sub-agents | None | None | All files | Not spawned because selected work touched a small service/test unit plus shared docs. |

No sub-agent was spawned for UOW-1666 because the selected handler plan was small and shared docs remained Orchestrator-owned.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `CreatePlan_UsesPlayerStateAndKnownTargetInputWithoutMutatingPlayer` | Added | Known visible target produces target-change packet intent and leaves C# player state unchanged. | Java known target branch and current C# handler boundary. |
| `CreatePlan_ModelsCurrentTargetClearWithoutLiveMutation` | Added | Clear request plans target clearing from current player target without mutating C# player. | Java clear-target branch. |
| `CreatePlan_ModelsAssistEarlyReturnWithoutPacketPlanOrMutation` | Added | Assist no-user branch preserves message intent and skips target-change packet plan. | Java assist known-but-not-visible branch. |
| `CreatePlan_ModelsTeamMemberFallbackFromPacketContext` | Added | Handler input can stage team-member fallback into target-change packet intent. | Java team-member fallback branch. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_TARGET_SELECT` | `Aion.GameServer.Services.TargetSelectHandlerPlanService` | Client Packet / Handler Boundary | Partial | Unit Tested | Partial Parity | C# handler plan feeds active-player target state and packet context through staged resolution/execution services. It does not replace the live `GameServerConnection.HandleTargetSelect` method, send packets, or mutate player target state. |
| `Aion.GameServer.Network.Aion.GameServerConnection.HandleTargetSelect` | `TargetSelectHandlerPlanService` | C# Handler Boundary | Partial | Unit Tested | Partial Parity | Existing live C# handler still directly assigns `Player.TargetObjectId` for non-assist target packets and logs assist selection as unported. New plan documents the Java-shaped path but is not yet called by the connection. |
| `com.aionemu.gameserver.model.gameobjects.VisibleObject.setTarget` | `TargetSelectExecutionPlanService`; `PlayerTargetChangePacketPlanService` | Model Boundary | Partial | Regression Tested | Partial Parity | Handler plan composes into the existing id-based changed-target approximation. Live Java object-reference equality, assignment, and controller callback remain unported. |
| `com.aionemu.gameserver.controllers.PlayerController.onTargetChanged` | `PlayerTargetChangePacketPlanService` through handler plan | Controller Boundary | Partial | Regression Tested | Partial Parity | Handler plan can produce non-live selected/update packet plans. It does not send owner packets or broadcast to sighted players. |
| `com.aionemu.gameserver.model.gameobjects.KnownList` | `TargetSelectHandlerInput` visibility/object-id snapshots | Visibility Boundary | Partial | Unit Tested | Partial Parity | KnownList state is provided as explicit input flags and ids. Live spatial visibility, knows/sees semantics, object references, concurrency, and collection behavior remain unverified. |
| `com.aionemu.gameserver.model.team.PlayerTeam` | `TargetSelectHandlerInput.TeamMemberObjectId` | Team Boundary | Partial | Unit Tested | Partial Parity | Team fallback is staged through a single snapshot id. Live team membership, member object nullability, offline members, ordering, and synchronization remain unported. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `TargetSelectHandlerPlan.SystemMessage` | Server Packet / Message Boundary | Partial | Regression Tested | Partial Parity | Handler plan surfaces assist messages as intents only. It does not create or serialize `SM_SYSTEM_MESSAGE`. |

## Remaining Risks

- `GameServerConnection.HandleTargetSelect` still does not call `TargetSelectHandlerPlanService`; live behavior remains direct id assignment for non-assist packets.
- Live player target mutation, `VisibleObject` object-reference storage, KnownList lookup, team lookup, system-message packet sending, audit logger invocation, owner packet send, and sighted-player broadcast remain unported.
- Handler plan accepts explicit snapshot inputs for KnownList/team/target-of-target state instead of deriving them from live world state.
- C# changed-target guard still uses target ids rather than Java object-reference equality.
- No Java runtime workflow comparison, live dispatch verification, or encrypted frame comparison was produced.
- Gated charge-all DB integration execution still needs a real MySQL environment.
- `docs/commit-conventions.md` is still missing.

## Summary Metrics

- Total Java artifacts discovered: 7 grouped rows.
- Total artifacts ported: 1 non-live handler-plan service, 3 supporting DTO/enum artifacts, and 4 focused regressions.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 0 grouped rows explicitly marked Needs Verification; all rows are Partial Parity with documented non-live gaps.
- Total blocked artifacts: live target-select connection integration, KnownList object references, team lookup, system-message packet send, audit logger invocation, target-change dispatch, Java runtime workflow comparison, DB-backed charge-all integration run.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Next best unit:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| Run gated DB integration | disposable MySQL schema | Set `AION_GAMESERVER_DB_INTEGRATION=1` plus DB env vars if a DB is available. |
| Opt-in/live-disabled `HandleTargetSelect` integration seam | connection handler/tests | Make live handler optionally consume the staged plan only if no live mutation/dispatch behavior is enabled by default. |
| Another isolated packet parity unit | packet class/tests | Continue packet-body parity while live targeting state remains incomplete. |

## Next Work Options

## Recommended Sequential Task

- Task: run the gated DB integration suite if a disposable DB is available; otherwise decide whether a live-disabled `HandleTargetSelect` integration seam is safe, or pivot to another isolated packet parity unit.
- Why: targeting is now staged through a handler plan, but actual connection integration remains the next risky boundary.
- Files: likely no file changes for DB execution; otherwise exclusive ownership of `GameServerConnection.cs` and focused tests, or exact packet files for a packet unit.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
| --- | --- | --- | --- | --- |
| A | Connection seam analysis | read-only connection handler and targeting services/tests | Low | Convert to writes only after exclusive ownership is reserved. |
| B | Isolated packet audit | packet Java/C# tests read-only unless selected | Low | Avoid live dispatch work. |
| C | Zone handler source audit | read-only Java zone handler files | Low | Useful before live zone callbacks. |
| D | DB integration setup check | env/read-only status | Medium | Only if a disposable DB is known to be available. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
| --- | --- | --- | --- |
| Orchestrator | Pick DB run, connection seam, or next packet unit | exact selected files | Java writes, live connection mutation unless explicitly selected, unrelated shared files |
| Read-only Agent | Audit connection seam or next packet source | read-only inspection | all writes |

## Do Not Parallelize

- Java source files: read-only only.
- `GameServerConnection.cs` if selected for any write: one owner only.
- Live target dispatch, live nearby dispatch, live weather mutation, live actor mutation, and live generated-zone writes: still high risk and intentionally disabled.
- Shared targeting helpers, packet helper/test fixtures, and DB integration setup: one owner only.

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1666] Add target select handler plan boundary
```

Files changed in this unit:

- `dotnetConversion/src/Aion.GameServer/Services/TargetSelectHandlerPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/TargetSelectHandlerPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ASH-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
