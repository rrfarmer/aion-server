# Phase 6 Session 2204 Handoff - FindGroup Mutation C# Trace Row Fixture Report

Date: 2026-06-02
Unit of Work: UOW-2204
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
- `PHASE-6-PROGRESS.md` remained untouched and should not be reopened for normal startup.
- Completion/handoff docs are the active progress/parity record.
- Broad .NET validation is not routine. Use focused test selection from `docs/orchestration-rules.md`.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Checked-in Java action `2`/`6` mutation-post artifacts exist under `parity-artifacts/find-group/mutation-post/java` and are shape-valid to the C# artifact reader.
- `FindGroupMutationPostCSharpTraceRowFixtureReportService` now classifies disabled action `2`/`6` C# projection rows as shape-valid but non-live, and feeds them into the comparison envelope without satisfying the live C# row gate.
- Shape-valid Java artifacts and shape-valid disabled C# rows are not verified parity. Live C# boundary rows, registry observation, projected-row comparison, and live dispatch remain missing.

## UOW-2204 Summary

This UOW added a non-live C# trace-row fixture report for mutation-post action `2`/`6` rows.

Key behavior:

- missing C# rows produce `BlockedMissingCSharpRows`,
- disabled action `2`/`6` projection rows produce `BlockedNonLiveRowsOnly`,
- rows count as live only when `BoundaryAccepted`, `ExecutorInvokedFromBoundary`, and `RegistrySendsObservedInOrder` are true,
- repository Java artifacts can be supplied explicitly so the report/envelope can prove Java rows are shape-valid while still blocking on live C# readiness.

Important notes:

- The report is an evidence classifier only.
- It does not execute side effects, registry sends, or comparison.
- It does not wire production `CmFindGroup` dispatch.
- Synthetic live-marked rows in tests prove gate behavior only, not runtime parity.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`

## C# Artifacts Reviewed

- `Aion.GameServer.Services.FindGroupDirectPacketMutationPostBoundaryTraceSchemaService`
- `Aion.GameServer.Services.FindGroupMutationPostComparisonInputEnvelopeService`
- `Aion.GameServer.Services.FindGroupMutationPostCSharpLiveTraceRowFixturePlanService`
- `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactDirectoryReportService`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostCSharpTraceRowFixtureReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostCSharpTraceRowFixtureReportServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2204-Completion.md`
- `docs/Phase-6-Session-2204-Handoff.md`

## Validation In UOW-2204

Validation decision:

- Changed surface: C# service/test-only trace-row fixture report plus non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostCSharpTraceRowFixtureReportServiceTests|FullyQualifiedName~FindGroupDirectPacketMutationPostBoundaryTraceExportProjectionTests|FullyQualifiedName~FindGroupMutationPostComparisonInputEnvelopeServiceTests" --no-restore
```

- Focused Java/Maven command: not run; no Java source or Java fixture file changed in this UOW, and the Java behavior source was the already-reviewed action `2`/`6` `CM_FIND_GROUP.runImpl` plus `FindGroupService.addRecruitment/addApplication` ordering.
- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally because filtered tests built the affected C# project/dependencies and no broad trigger applied.
- Why this scope is sufficient: the added service only classifies supplied trace-row fixture values and reuses the existing disabled projection/envelope services; focused tests cover the new service plus both adjacent dependencies.

Result:

- First focused C# attempt failed 4 assertions because tests assumed default Java artifact discovery from the test binary working directory. Tests were corrected to pass the repository artifact root explicitly where checked-in artifacts are required.
- Final focused C# command: passed 14, failed 0, skipped 0. Existing nullable/analyzer warnings were emitted from unrelated C# files.
- `git diff --check`: passed. Git emitted line-ending normalization warnings for touched documentation files on Windows.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `2`/`6` | `Aion.GameServer.Services.FindGroupMutationPostCSharpTraceRowFixtureReportService`; `Aion.GameServer.Services.FindGroupDirectPacketMutationPostBoundaryTraceSchemaService` | Client Packet Boundary / Trace Fixture | Partial | Unit Tested | Partial Parity | C# disabled projection rows preserve Java action `2`/`6` mutation-post schema shape and direct-packet IDs, but they are non-live because production `CmFindGroup` dispatch, executor invocation, and registry observation remain missing. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment` | `Aion.GameServer.Services.FindGroupMutationPostCSharpTraceRowFixtureReportService`; `Aion.GameServer.Services.FindGroupRecruitmentPlanService` | Service Mutation / Trace Fixture | Partial | Unit Tested | Partial Parity | Action `2` disabled C# row is shape-valid for recruitment, posted system message `1400392`, refreshed action `0`, zero broadcasts, and zero invites. No live send ordering or runtime comparison exists. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addApplication` | `Aion.GameServer.Services.FindGroupMutationPostCSharpTraceRowFixtureReportService`; `Aion.GameServer.Services.FindGroupRecruitmentPlanService` | Service Mutation / Trace Fixture | Partial | Unit Tested | Partial Parity | Action `6` disabled C# row is shape-valid for application, posted system message `1400393`, refreshed action `4`, zero broadcasts, and zero invites. No live send ordering or runtime comparison exists. |
| Repository artifacts under `parity-artifacts/find-group/mutation-post/java` | `Aion.GameServer.Services.FindGroupMutationPostComparisonInputEnvelopeService`; `Aion.GameServer.Services.FindGroupMutationPostCSharpTraceRowFixtureReportService` | Golden Fixture Artifact / Envelope Input | Partial | Unit Tested | Partial Parity | Checked-in Java artifacts can feed the C# row fixture report through the envelope as shape-valid Java rows. Disabled C# rows still block on missing live C# evidence and readiness. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- Shape-valid Java fixture artifacts and disabled C# projection rows are not Java/C# runtime comparison evidence.
- Live C# mutation-post rows, registry observation, projected-row comparison, comparison execution, world-broadcast fanout, action `12` invite dispatch, and runtime/socket comparison are still missing.

## Next Recommended Unit of Work

Next sequential task:

- Add a focused artifact-backed preflight/readiness update that passes repository Java artifacts and disabled C# fixture rows together, proving the Java and C# shape inputs are both present while readiness remains blocked on live C# boundary/registry evidence.

Safe candidates:

- Add a guarded live-boundary fixture skeleton for action `2`/`6` that records missing executor/registry observations without sending packets.
- Add a deterministic Java artifact timestamp override if future fixture churn around `serverEpochSeconds` becomes noisy.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.

## Commit

Commit message:

```text
[Phase 6][UOW-2204] Add find group mutation C# trace row fixture report
```
