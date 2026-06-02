# Phase 6 Session 2212 Handoff - FindGroup Mutation Dry-Run Paired Row Readiness

Date: 2026-06-02
Unit of Work: UOW-2212
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
- `FindGroupMutationPostProjectedRowComparisonDryRunContractService` carries shape-valid Java artifact row references, accepted guarded C# row references, and paired readiness rows for action `2`/`6` future executor inputs.
- Paired readiness rows require matching action, mutation kind, and required identity across Java and C# references before marking an input pair ready.
- Paired readiness is non-live metadata only. It does not compare values, does not execute side effects, and is not verified parity evidence.

## UOW-2212 Summary

This UOW added paired Java/C# row-readiness metadata to the projected-row comparison dry-run contract.

Key behavior:

- adds `PairedRowReadiness`,
- reports action `2` recruitment and action `6` application readiness rows,
- marks Java-only and C#-only references as incomplete,
- marks paired references as future-input ready only when both accepted sides exist for the same action/mutation/identity,
- keeps `ShouldCompareRows` controlled by the execution blocker report,
- keeps production `CmFindGroup` dispatch disabled.

Important notes:

- The dry-run contract is still an input-shape/readiness report only.
- It does not execute side effects, registry sends, or row comparison.
- It does not wire production `CmFindGroup` dispatch.
- Shape-valid Java rows and synthetic accepted C# rows in tests are not runtime evidence.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`

## C# Artifacts Reviewed

- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonDryRunContractService`
- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonResultSkeletonService`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonDryRunContractService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonDryRunContractServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonResultSkeletonServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2212-Completion.md`
- `docs/Phase-6-Session-2212-Handoff.md`

## Validation In UOW-2212

Validation decision:

- Changed surface: C# service/test-only dry-run comparison input-shape aggregation plus non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonDryRunContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonResultSkeletonServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactDirectoryReportServiceTests|FullyQualifiedName~FindGroupMutationPostGuardedFixtureResultContractServiceTests|FullyQualifiedName~FindGroupMutationPostComparisonExecutionResultContractServiceTests" --no-restore
```

- Focused Java/Maven command: not run; no Java source or Java fixture file changed in this UOW, and the Java source review was sufficient for the non-live action/mutation identity mapping.
- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally because filtered tests built the affected C# project/dependencies and no broad trigger applied.
- Why this scope is sufficient: the edited service is a non-live dry-run contract; the focused filter covers the changed dry-run contract, downstream result skeleton construction, Java artifact row source, guarded C# row source, and comparison result contract adjacency.

Result:

- Focused C# command: passed 30, failed 0, skipped 0. Existing nullable/analyzer warnings were emitted from unrelated C# files.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `2`/`6` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonDryRunContractService` | Client Packet Boundary / Dry-Run Comparison Contract | Partial | Unit Tested | Partial Parity | Dry-run contract now reports whether Java/C# row references are paired by action, mutation kind, and required identity. No live C# boundary dispatch or row comparison exists. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonDryRunContractService` | Service Mutation / Dry-Run Readiness Metadata | Partial | Unit Tested | Partial Parity | Action `2` readiness requires Java `Recruitment` artifact shape plus accepted C# live-boundary evidence before a future executor input is considered paired. Values are not compared. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addApplication` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonDryRunContractService` | Service Mutation / Dry-Run Readiness Metadata | Partial | Unit Tested | Partial Parity | Action `6` readiness requires Java `Application` artifact shape plus accepted C# live-boundary evidence before a future executor input is considered paired. Values are not compared. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- Paired readiness, shape-valid Java fixture artifacts, disabled C# projection rows, guarded fixture skeleton metadata, readiness aggregate metadata, guarded fixture result contract metadata, envelope handoff metadata, and dry-run row references are not Java/C# runtime comparison evidence.
- Live C# mutation-post rows, executor observation from the guarded boundary, registry observation, projected-row comparison, comparison execution, world-broadcast fanout, action `12` invite dispatch, and runtime/socket comparison are still missing.

## Next Recommended Unit of Work

Next sequential task:

- Add a non-live projected-row comparison executor skeleton that consumes paired-readiness rows and emits only blocked planned result rows when values cannot be compared.

Safe candidates:

- Add a deterministic Java artifact timestamp override if future fixture churn around `serverEpochSeconds` becomes noisy.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add more precise readiness blocker wording for executor versus registry observations if the result contract needs separate status rows.

## Commit

Commit message:

```text
[Phase 6][UOW-2212] Add find group mutation dry-run paired row readiness
```
