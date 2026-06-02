# Phase 6 Session 2169 Handoff - FindGroup Mutation Trace Schema

Date: 2026-06-02
Unit of Work: UOW-2169
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
- Direct-packet, world-broadcast, action `12` invite, runtime/socket comparison, direct show-list trace schema/projection, mutation-post scaffold, and mutation-post trace schema contracts exist as non-live readiness artifacts.

## UOW-2169 Summary

This UOW added a trace schema/export DTO for future action `2`/`6` mutation-post boundary traces:

- Schema version: `1`.
- Trace name: `cm-find-group-direct-mutation-post-boundary`.
- Supported actions:
  - `2`: recruitment mutation, Java `addRecruitment(player, message, groupType)`, posted system message id `1400392`, refreshed `SM_FIND_GROUP` action `0`.
  - `6`: application mutation, Java `addApplication(player, message, groupType, classId, level)`, posted system message id `1400393`, refreshed `SM_FIND_GROUP` action `4`.
- Stable fields include parsed action, boundary acceptance, active player facts, mutation kind, mutated entry id, mutation-before-direct-send flag, posted system message recipient/type/id, refreshed list recipient/type/action, visible entry ids after mutation, boundary executor status, registry-send ordering observation, and zero broadcast/invite counts.

The schema is non-live and does not populate Java or C# trace captures.

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`

## C# Artifacts Touched

- `Aion.GameServer.Services.FindGroupDirectPacketMutationPostBoundaryTraceSchemaService`
- `Aion.GameServer.Services.FindGroupDirectPacketMutationPostBoundaryTraceSchema`
- `Aion.GameServer.Services.FindGroupDirectPacketMutationPostBoundaryTraceExport`
- `Aion.GameServer.Services.FindGroupDirectPacketBoundaryTraceReadinessService`
- `Aion.GameServer.Tests.FindGroupDirectPacketMutationPostBoundaryTraceSchemaServiceTests`
- `Aion.GameServer.Tests.FindGroupDirectPacketMutationPostLiveBoundaryTraceScaffoldServiceTests`
- `Aion.GameServer.Tests.FindGroupDirectPacketBoundaryTraceReadinessServiceTests`

## Validation In UOW-2169

Validation decision:

- Changed surface: focused production readiness/schema service, focused tests, readiness-report text, and non-live design documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupDirectPacketMutationPostBoundaryTraceSchemaServiceTests|FullyQualifiedName~FindGroupDirectPacketMutationPostLiveBoundaryTraceScaffoldServiceTests|FullyQualifiedName~FindGroupDirectPacketBoundaryTraceReadinessServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed, and this non-live schema used reviewed Java `CM_FIND_GROUP.runImpl` actions `2`/`6` plus `FindGroupService.addRecruitment/addApplication` behavior as the oracle. No narrow executable Java fixture was identified for this readiness artifact.
- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally.
- Why this scope is sufficient: the filtered tests cover the new schema plus adjacent mutation-post scaffold and direct-packet readiness surfaces, and the filtered command built the affected project/dependencies.

Result:

- Passed: 11
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupDirectPacketMutationPostBoundaryTraceSchemaService` | Trace Schema / Runtime Comparison Readiness | Blocked | Unit Tested | Partial Parity | Action `2`/`6` trace export fields are represented, but no Java/C# trace capture or live `ProcessPacketAsync` execution has occurred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupDirectPacketMutationPostBoundaryTraceSchemaService`; `Aion.GameServer.Services.FindGroupDirectPacketBoundaryTraceReadinessService` | Trace Schema / Direct Packet Readiness | Partial | Unit Tested | Partial Parity | Java mutation, posted system message, and refreshed show-list ordering fields are represented. No live registry send, socket comparison, or runtime trace has executed. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- Trace schema is non-live; no Java/C# trace capture, live registry send, encrypted socket capture, or real-client runtime comparison has executed.
- Mutation-post trace projection, world-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add a non-live export projection helper for action `2`/`6` mutation-post disabled boundary plans using the new schema.

Safe candidates:

- Add live boundary or runtime trace evidence for shared singleton caller interleavings.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.
- Add a runtime comparison fixture contract row for action `2`/`6` only after the projection helper exists.

## Files Changed In UOW-2169

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupDirectPacketMutationPostBoundaryTraceSchemaService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupDirectPacketMutationPostBoundaryTraceSchemaServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupDirectPacketBoundaryTraceReadinessService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupDirectPacketBoundaryTraceReadinessServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2169-Completion.md`
- `docs/Phase-6-Session-2169-Handoff.md`

## Commit

Recommended commit message:

```text
[Phase 6][UOW-2169] Add find group mutation trace schema
```
