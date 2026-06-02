# Phase 6 Session 2207 Handoff - FindGroup Mutation Guarded Fixture Readiness Wiring

Date: 2026-06-02
Unit of Work: UOW-2207
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

Before running an expensive broad command, name the broad-validation trigger in the active notes. If no trigger applies, choose a filtered test or hygiene command and record the remaining risk.

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
- `FindGroupMutationPostCSharpTraceRowFixtureReportService` classifies disabled action `2`/`6` C# projection rows as shape-valid but non-live.
- `FindGroupMutationPostArtifactComparisonPreflightService` exposes `HasCSharpTraceRowShapeInputs`.
- `FindGroupMutationPostGuardedLiveBoundaryFixtureSkeletonService` records the explicit trace guard, production dispatch guard, action `2`/`6` scenario requirements, and missing executor/registry observations for a future guarded boundary fixture.
- `FindGroupMutationPostTraceRowReadinessAggregateService` now consumes the guarded boundary fixture skeleton and disabled C# shape-input signal, while keeping readiness blocked on live boundary rows, executor observation, registry observation, and comparison.
- Shape-valid Java artifacts, shape-valid disabled C# rows, guarded fixture skeleton metadata, and readiness aggregate metadata are not verified parity.

## UOW-2207 Summary

This UOW wired mutation-post action `2`/`6` readiness aggregation to the guarded boundary fixture skeleton.

Key behavior:

- adds a `GuardedLiveBoundaryFixtureSkeleton` readiness row,
- exposes `HasGuardedLiveBoundaryFixtureSkeleton`,
- exposes `HasCSharpTraceRowShapeInputs`,
- exposes `NeedsGuardedBoundaryFixture`,
- treats disabled C# shape rows as useful preflight inputs but not live rows,
- keeps `NeedsCSharpLiveRows` and `NeedsRegistryObservation` true until guarded live execution and registry observations exist,
- keeps production `CmFindGroup` dispatch disabled.

Important notes:

- The aggregate is a report only.
- It does not execute side effects, registry sends, or comparison.
- It does not wire production `CmFindGroup` dispatch.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`

## C# Artifacts Reviewed

- `Aion.GameServer.Services.FindGroupMutationPostTraceRowReadinessAggregateService`
- `Aion.GameServer.Services.FindGroupMutationPostGuardedLiveBoundaryFixtureSkeletonService`
- `Aion.GameServer.Services.FindGroupMutationPostArtifactComparisonPreflightService`
- `Aion.GameServer.Services.FindGroupMutationPostComparisonExecutionResultContractService`
- `Aion.GameServer.Services.FindGroupMutationPostComparisonInputEnvelopeService`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostTraceRowReadinessAggregateService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostTraceRowReadinessAggregateServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostComparisonExecutionResultContractServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostComparisonInputEnvelopeServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/orchestration-rules.md`
- `docs/Phase-6-Session-2207-Completion.md`
- `docs/Phase-6-Session-2207-Handoff.md`

## Validation In UOW-2207

Validation decision:

- Changed surface: C# service/test-only readiness aggregate wiring plus non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostTraceRowReadinessAggregateServiceTests|FullyQualifiedName~FindGroupMutationPostGuardedLiveBoundaryFixtureSkeletonServiceTests|FullyQualifiedName~FindGroupMutationPostArtifactComparisonPreflightServiceTests|FullyQualifiedName~FindGroupMutationPostComparisonExecutionResultContractServiceTests|FullyQualifiedName~FindGroupMutationPostComparisonInputEnvelopeServiceTests" --no-restore
```

- Focused Java/Maven command: not run; no Java source or Java fixture file changed in this UOW.
- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally because filtered tests built the affected C# project/dependencies and no broad trigger applied.
- Why this scope is sufficient: the edited service is a non-live readiness aggregate, and focused tests cover the aggregate plus adjacent guarded-fixture, preflight, result-contract, and input-envelope surfaces.

Result:

- Focused C# command: passed 33, failed 0, skipped 0.
- `git diff --check`: passed. Git emitted line-ending normalization warnings for touched documentation and C# files on Windows.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `2`/`6` | `Aion.GameServer.Services.FindGroupMutationPostTraceRowReadinessAggregateService`; `Aion.GameServer.Services.FindGroupMutationPostGuardedLiveBoundaryFixtureSkeletonService` | Client Packet Boundary / Readiness Aggregate | Partial | Unit Tested | Partial Parity | Aggregate now distinguishes guarded fixture skeleton and disabled C# shape inputs from live boundary rows. Live boundary execution, executor observation, registry observation, and comparison remain missing. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment` | `Aion.GameServer.Services.FindGroupMutationPostTraceRowReadinessAggregateService` | Service Mutation / Readiness Aggregate | Partial | Unit Tested | Partial Parity | Action `2` shape inputs can be recognized, but readiness still blocks until guarded live rows and observations exist. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addApplication` | `Aion.GameServer.Services.FindGroupMutationPostTraceRowReadinessAggregateService` | Service Mutation / Readiness Aggregate | Partial | Unit Tested | Partial Parity | Action `6` shape inputs can be recognized, but readiness still blocks until guarded live rows and observations exist. |
| Repository artifacts under `parity-artifacts/find-group/mutation-post/java` | `Aion.GameServer.Services.FindGroupMutationPostArtifactComparisonPreflightService`; `Aion.GameServer.Services.FindGroupMutationPostTraceRowReadinessAggregateService` | Golden Fixture Artifact / Readiness Handoff | Partial | Unit Tested | Partial Parity | Repository Java artifacts plus disabled C# shape inputs can reach readiness metadata, but live runtime comparison is still blocked. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- Shape-valid Java fixture artifacts, disabled C# projection rows, guarded fixture skeleton metadata, and readiness aggregate metadata are not Java/C# runtime comparison evidence.
- Live C# mutation-post rows, executor observation, registry observation, projected-row comparison, comparison execution, world-broadcast fanout, action `12` invite dispatch, and runtime/socket comparison are still missing.

## Next Recommended Unit of Work

Next sequential task:

- Add a guarded fixture result contract for action `2`/`6` that can later hold real boundary rows without sending packets by default.

Safe candidates:

- Add a deterministic Java artifact timestamp override if future fixture churn around `serverEpochSeconds` becomes noisy.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add more precise readiness blocker wording for executor versus registry observations if the guarded fixture contract needs separate status rows.

## Commit

Commit message:

```text
[Phase 6][UOW-2207] Wire find group mutation guarded fixture readiness
```
