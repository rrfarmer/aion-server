# Phase 6 Session 2171 Handoff - FindGroup Mutation Runtime Fixture Contract

Date: 2026-06-02
Unit of Work: UOW-2171
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

Use this validation decision template in future completion/handoff docs:

```text
Validation decision:
- Changed surface:
- Focused C# command:
- Focused Java/Maven command:
- Broad-validation trigger:
- Broad .NET decision:
- Why this scope is sufficient:
```

## Current State

- Phase 6 remains in progress.
- Java remains the source of truth.
- `PHASE-6-PROGRESS.md` remained untouched.
- Completion/handoff docs are the active progress/parity record.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Direct-packet, world-broadcast, action `12` invite, runtime/socket comparison, direct show-list trace schema/projection, mutation-post scaffold/schema/projection, and mutation-post runtime fixture contract rows exist as non-live readiness artifacts.

## UOW-2171 Summary

This UOW extended `FindGroupRuntimeComparisonPreflightContractService` with a blocked fixture row for action `2`/`6` mutation-post runtime comparisons.

Fixture row:

- Name: `mutation-post-actions-2-6`
- Actions: `2`, `6`
- Trace name: `cm-find-group-direct-mutation-post-boundary`
- Java source: `CM_FIND_GROUP.runImpl` actions `2`/`6`; `FindGroupService.addRecruitment/addApplication`
- C# projection source: `FindGroupDirectPacketMutationPostBoundaryTraceSchemaService.CreateExportFromDisabledPlan`
- Status: `BlockedPendingJavaAndLiveCSharpTrace`

The row names the future comparison target but does not generate Java artifacts, does not capture live C# traces, and does not make runtime comparison ready.

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`

## C# Artifacts Touched

- `Aion.GameServer.Services.FindGroupRuntimeComparisonPreflightContractService`
- `Aion.GameServer.Services.FindGroupRuntimeComparisonPreflightContract`
- `Aion.GameServer.Services.FindGroupRuntimeComparisonFixtureContractRow`
- `Aion.GameServer.Services.FindGroupRuntimeComparisonFixtureContractStatus`
- `Aion.GameServer.Tests.FindGroupRuntimeComparisonPreflightContractServiceTests`

## Validation In UOW-2171

Validation decision:

- Changed surface: focused production runtime preflight contract service, focused tests, and non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupRuntimeComparisonPreflightContractServiceTests|FullyQualifiedName~FindGroupDirectPacketMutationPostBoundaryTraceExportProjectionTests|FullyQualifiedName~FindGroupDirectPacketMutationPostBoundaryTraceSchemaServiceTests|FullyQualifiedName~FindGroupConcurrentMutationOrderingReadinessServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed, and this contract row uses reviewed Java `CM_FIND_GROUP.runImpl` actions `2`/`6` plus `FindGroupService.addRecruitment/addApplication` behavior as the oracle. No narrow executable Java fixture exists yet; this UOW documents that future fixture target.
- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally.
- Why this scope is sufficient: the filtered tests cover the edited runtime preflight contract plus adjacent mutation-post schema/projection and singleton-readiness surfaces, and the filtered command builds the affected project/dependencies.

Result:

- Passed: 15
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupRuntimeComparisonPreflightContractService` | Runtime Comparison Fixture Contract | Blocked | Unit Tested | Partial Parity | Action `2`/`6` mutation-post runtime fixture row is represented and tied to the trace schema/projection, but no Java/C# runtime trace capture or live `ProcessPacketAsync` execution has occurred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupRuntimeComparisonPreflightContractService`; `Aion.GameServer.Services.FindGroupDirectPacketMutationPostBoundaryTraceSchemaService` | Runtime Comparison Fixture Contract / Direct Packet Readiness | Partial | Unit Tested | Partial Parity | Java mutation, posted system message ordering, refreshed show-list ordering, and post-mutation visible ids are named as future fixture requirements. No Java artifact, live registry send, socket comparison, or runtime trace has executed. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- The mutation-post fixture row is non-live contract metadata; no Java/C# trace capture, live registry send, encrypted socket capture, or real-client runtime comparison has executed.
- The fixture row does not prove `GameServerConnection.ProcessPacketAsync` invokes the direct-packet executor from the triggering packet boundary.
- World-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add a Java trace artifact schema or validator target for the action `2`/`6` mutation-post fixture row, without enabling live C# dispatch.

Safe candidates:

- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.
- Add a non-live trace export projection for another direct-packet subgroup only if a stable schema already exists.

## Files Changed In UOW-2171

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupRuntimeComparisonPreflightContractService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupRuntimeComparisonPreflightContractServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2171-Completion.md`
- `docs/Phase-6-Session-2171-Handoff.md`

## Commit

Recommended commit message:

```text
[Phase 6][UOW-2171] Add find group mutation runtime fixture contract
```
