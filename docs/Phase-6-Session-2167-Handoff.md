# Phase 6 Session 2167 Handoff - FindGroup Mutation Direct Trace Scaffold

Date: 2026-06-02
Unit of Work: UOW-2167
Status: Completed

## Startup Instructions

Future Phase 6 sessions should read:

1. `docs/csharp-port.md`
2. `docs/orchestration-rules.md`
3. `docs/parity-verification.md`
4. latest `docs/Phase-6-Session-*-Completion.md`
5. latest `docs/Phase-6-Session-*-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is historical archive material only.

Focused validation is the default. Do not run the broad .NET suite, an unfiltered project-wide test run, or a full solution build unless a documented broad-validation trigger applies. Filtered `dotnet test` commands already build the affected project and dependencies.

## Current State

- Phase 6 remains in progress.
- Java remains the source of truth.
- `PHASE-6-PROGRESS.md` remained untouched.
- Completion/handoff docs are the active progress/parity record.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Direct-packet, world-broadcast, action `12` invite, runtime/socket comparison, direct show-list trace schema, and mutation-post trace scaffold contracts exist as non-live readiness artifacts.

## UOW-2167 Summary

This UOW added a non-live live-boundary trace scaffold for mutating direct-packet actions `2` and `6`.

The scaffold requires future trace evidence to prove:

- action `2`/`6` boundary acceptance,
- shared singleton mutation planning,
- recruitment/application state mutation before direct sends,
- posted system message ids `1400392`/`1400393`,
- refreshed show-list action `0`/`4` intent materialization,
- direct-packet executor invocation from the boundary,
- registry send ordering with posted message before refreshed show-list,
- one ordered boundary trace for action `2` and one for action `6`.

The scaffold is non-live and does not populate Java or C# trace captures.

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`

## C# Artifacts Touched

- `Aion.GameServer.Services.FindGroupDirectPacketMutationPostLiveBoundaryTraceScaffoldService`
- `Aion.GameServer.Services.FindGroupDirectPacketMutationPostLiveBoundaryTraceScaffold`
- `Aion.GameServer.Services.FindGroupDirectPacketBoundaryTraceReadinessService`
- `Aion.GameServer.Services.FindGroupRecruitmentPlanService`
- `Aion.GameServer.Tests.FindGroupDirectPacketMutationPostLiveBoundaryTraceScaffoldServiceTests`
- `Aion.GameServer.Tests.FindGroupDirectPacketBoundaryTraceReadinessServiceTests`
- `Aion.GameServer.Tests.FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests`

## Validation In UOW-2167

Validation decision:

- Changed surface: focused production readiness/scaffold service, focused tests, readiness-report text, and non-live design documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupDirectPacketMutationPostLiveBoundaryTraceScaffoldServiceTests|FullyQualifiedName~FindGroupDirectPacketBoundaryTraceReadinessServiceTests|FullyQualifiedName~FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed, and this non-live scaffold used reviewed Java `CM_FIND_GROUP.runImpl` actions `2`/`6` plus `FindGroupService.addRecruitment/addApplication` behavior as the oracle. No narrow executable Java fixture was identified for this readiness artifact.
- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally.
- Why this scope is sufficient: the filtered tests cover the new scaffold plus adjacent readiness and existing action `2`/`6` opt-in composition evidence, and the filtered command built the affected project/dependencies.

Result:

- Passed: 30
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupDirectPacketMutationPostLiveBoundaryTraceScaffoldService` | Runtime Comparison Readiness / Trace Scaffold | Blocked | Unit Tested | Partial Parity | Action `2`/`6` mutation-post trace milestones are represented, but no Java/C# trace capture or live `ProcessPacketAsync` execution has occurred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupDirectPacketMutationPostLiveBoundaryTraceScaffoldService`; `Aion.GameServer.Services.FindGroupDirectPacketBoundaryTraceReadinessService`; `Aion.GameServer.Services.FindGroupRecruitmentPlanService` | Direct Packet Readiness / Service Planner | Partial | Unit Tested | Partial Parity | Java add-recruitment/add-application mutation and direct-send ordering are represented in non-live scaffold and existing opt-in composition tests. No live registry send, socket comparison, or runtime trace has executed. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- Trace scaffold is non-live; no Java/C# trace capture, live registry send, encrypted socket capture, or real-client runtime comparison has executed.
- Show-list traces, mutating direct-packet traces, world-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add a non-live trace export population helper for action `0`/`4` disabled boundary plans using the existing show-list trace schema.

Safe candidates:

- Add a trace schema/export DTO for action `2`/`6` mutation-post boundary traces.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.

## Files Changed In UOW-2167

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupDirectPacketMutationPostLiveBoundaryTraceScaffoldService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupDirectPacketMutationPostLiveBoundaryTraceScaffoldServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupDirectPacketBoundaryTraceReadinessService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupDirectPacketBoundaryTraceReadinessServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2167-Completion.md`
- `docs/Phase-6-Session-2167-Handoff.md`

## Commit

Recommended commit message:

```text
[Phase 6][UOW-2167] Add find group mutation direct trace scaffold
```
