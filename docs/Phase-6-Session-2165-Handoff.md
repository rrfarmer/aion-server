# Phase 6 Session 2165 Handoff - FindGroup Direct Show-List Trace Schema

Date: 2026-06-02
Unit of Work: UOW-2165
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

Each completion/handoff must record the changed surface, focused C# command or hygiene command, Java/Maven command or skip rationale, broad-validation trigger or `none`, broad .NET skip/run decision, and why the selected scope was sufficient.

## Current State

- Phase 6 remains in progress.
- Java remains the source of truth.
- `PHASE-6-PROGRESS.md` remained untouched.
- Completion/handoff docs are the active progress/parity record.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Direct-packet, world-broadcast, action `12` invite, and runtime/socket comparison contracts exist as non-live readiness artifacts.
- Direct show-list actions `0`/`4` now have both a non-live boundary trace scaffold and schema version `1` for future trace exports.

## UOW-2165 Summary

This UOW added a trace schema/export DTO for future direct show-list boundary traces:

- Schema version: `1`.
- Trace name: `cm-find-group-direct-show-list-boundary`.
- Supported actions:
  - `0`: recruitments, Java `showRecruitments(player)`, `SM_FIND_GROUP` action `0`.
  - `4`: applications, Java `showApplications(player)`, `SM_FIND_GROUP` action `4`.
- Stable fields include parsed action, boundary acceptance, active player facts, visible entry ids, direct packet recipient/type/action, boundary executor status, registry-send observation, and zero broadcast/invite counts.

The schema is non-live and does not populate Java or C# trace captures.

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`

## C# Artifacts Touched

- `Aion.GameServer.Services.FindGroupDirectPacketShowListBoundaryTraceSchemaService`
- `Aion.GameServer.Services.FindGroupDirectPacketShowListBoundaryTraceSchema`
- `Aion.GameServer.Services.FindGroupDirectPacketShowListBoundaryTraceExport`
- `Aion.GameServer.Services.FindGroupDirectPacketBoundaryTraceReadinessService`
- `Aion.GameServer.Tests.FindGroupDirectPacketShowListBoundaryTraceSchemaServiceTests`
- `Aion.GameServer.Tests.FindGroupDirectPacketBoundaryTraceReadinessServiceTests`

## Validation In UOW-2165

Validation decision:

- Changed surface: focused production readiness/schema service, focused tests, readiness-report text, and non-live design documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupDirectPacketShowListBoundaryTraceSchemaServiceTests|FullyQualifiedName~FindGroupDirectPacketShowListLiveBoundaryTraceScaffoldServiceTests|FullyQualifiedName~FindGroupDirectPacketBoundaryTraceReadinessServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed, and this non-live schema used reviewed Java `CM_FIND_GROUP.runImpl` actions `0`/`4` plus `FindGroupService.showRecruitments/showApplications` behavior as the oracle. No narrow executable Java fixture was identified for this readiness artifact.
- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally.
- Why this scope is sufficient: the filtered tests cover the new schema plus adjacent show-list scaffold and direct-packet readiness surfaces, and the filtered command built the affected project/dependencies.

Result:

- Passed: 10
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupDirectPacketShowListBoundaryTraceSchemaService` | Trace Schema / Runtime Comparison Readiness | Blocked | Unit Tested | Partial Parity | Action `0`/`4` trace export fields are represented, but no Java/C# trace capture or live `ProcessPacketAsync` execution has occurred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupDirectPacketShowListBoundaryTraceSchemaService`; `Aion.GameServer.Services.FindGroupDirectPacketBoundaryTraceReadinessService` | Trace Schema / Direct Packet Readiness | Partial | Unit Tested | Partial Parity | Java show-list filtering and direct-send comparison fields are represented. No live registry send, socket comparison, or runtime trace has executed. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- Trace schema is non-live; no Java/C# trace capture, live registry send, encrypted socket capture, or real-client runtime comparison has executed.
- Mutating direct-packet actions, world-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add focused direct-packet live-boundary trace scaffolding for mutating direct-packet actions `2` and `6` without live dispatch.

Safe candidates:

- Add a non-live trace export population helper for action `0`/`4` disabled boundary plans using the schema.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.

## Files Changed In UOW-2165

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupDirectPacketShowListBoundaryTraceSchemaService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupDirectPacketShowListBoundaryTraceSchemaServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupDirectPacketBoundaryTraceReadinessService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupDirectPacketBoundaryTraceReadinessServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2165-Completion.md`
- `docs/Phase-6-Session-2165-Handoff.md`

## Commit

Recommended commit message:

```text
[Phase 6][UOW-2165] Add find group show-list trace schema
```
