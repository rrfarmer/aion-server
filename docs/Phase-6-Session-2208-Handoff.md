# Phase 6 Session 2208 Handoff - FindGroup Mutation Guarded Fixture Result Contract

Date: 2026-06-02
Unit of Work: UOW-2208
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
- `FindGroupMutationPostTraceRowReadinessAggregateService` consumes the guarded boundary fixture skeleton and disabled C# shape-input signal, while keeping readiness blocked on live boundary rows, executor observation, registry observation, and comparison.
- `FindGroupMutationPostGuardedFixtureResultContractService` now classifies future C# candidate rows for the guarded fixture handoff. It accepts only action `2`/`6` C# rows with boundary acceptance, executor observation, registry observation, expected packet shape, and zero broadcast/invite counts.
- Shape-valid Java artifacts, shape-valid disabled C# rows, guarded fixture skeleton metadata, readiness aggregate metadata, and the guarded fixture result contract are not verified parity.

## UOW-2208 Summary

This UOW added the guarded fixture result contract for mutation-post action `2`/`6` rows.

Key behavior:

- keeps production `CmFindGroup` dispatch disabled,
- sends no packets by default,
- requires explicit trace guard metadata,
- requires one accepted future C# live boundary row for action `2`,
- requires one accepted future C# live boundary row for action `6`,
- rejects disabled shape rows as non-live,
- rejects unsupported action, non-C# source, missing boundary acceptance, missing executor observation, missing registry observation, unexpected packet shape, and unexpected world-broadcast/invite side effects,
- marks comparison handoff ready only when both action rows are accepted.

Important notes:

- The contract is a report/classifier only.
- It does not execute side effects, registry sends, or comparison.
- It does not wire production `CmFindGroup` dispatch.
- Synthetic live-shaped rows in tests are not runtime evidence.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`

## C# Artifacts Reviewed

- `Aion.GameServer.Services.FindGroupDirectPacketMutationPostBoundaryTraceSchemaService`
- `Aion.GameServer.Services.FindGroupMutationPostGuardedLiveBoundaryFixtureSkeletonService`
- `Aion.GameServer.Services.FindGroupMutationPostComparisonInputEnvelopeService`
- `Aion.GameServer.Services.FindGroupMutationPostComparisonExecutionResultContractService`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostGuardedFixtureResultContractService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostGuardedFixtureResultContractServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2208-Completion.md`
- `docs/Phase-6-Session-2208-Handoff.md`

## Validation In UOW-2208

Validation decision:

- Changed surface: C# service/test-only guarded fixture result contract plus non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostGuardedFixtureResultContractServiceTests|FullyQualifiedName~FindGroupMutationPostGuardedLiveBoundaryFixtureSkeletonServiceTests|FullyQualifiedName~FindGroupDirectPacketMutationPostBoundaryTraceSchemaServiceTests" --no-restore
```

- Focused Java/Maven command: not run; no Java source or Java fixture file changed in this UOW.
- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally because filtered tests built the affected C# project/dependencies and no broad trigger applied.
- Why this scope is sufficient: the added service is a non-live result contract that depends on the existing guarded skeleton and mutation-post trace schema; focused tests cover the contract plus both adjacent surfaces.

Result:

- Focused C# command: passed 15, failed 0, skipped 0. Existing nullable/analyzer warnings were emitted from unrelated C# files.
- `git diff --check`: passed. Git emitted line-ending normalization warnings for touched documentation and C# files on Windows.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `2`/`6` | `Aion.GameServer.Services.FindGroupMutationPostGuardedFixtureResultContractService`; `Aion.GameServer.Services.FindGroupDirectPacketMutationPostBoundaryTraceSchemaService` | Client Packet Boundary / Fixture Result Contract | Partial | Unit Tested | Partial Parity | Contract defines accepted future C# live rows for actions `2`/`6` but does not run the boundary or send packets. Live rows, registry observation, and comparison remain missing. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment` | `Aion.GameServer.Services.FindGroupMutationPostGuardedFixtureResultContractService` | Service Mutation / Fixture Result Contract | Partial | Unit Tested | Partial Parity | Action `2` accepted rows must match Java recruitment mutation-post packet shape: posted system message `1400392`, refreshed action `0`, boundary/executor/registry observed, zero broadcast/invite counts. Evidence is contract-level only. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addApplication` | `Aion.GameServer.Services.FindGroupMutationPostGuardedFixtureResultContractService` | Service Mutation / Fixture Result Contract | Partial | Unit Tested | Partial Parity | Action `6` accepted rows must match Java application mutation-post packet shape: posted system message `1400393`, refreshed action `4`, boundary/executor/registry observed, zero broadcast/invite counts. Evidence is contract-level only. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- Shape-valid Java fixture artifacts, disabled C# projection rows, guarded fixture skeleton metadata, readiness aggregate metadata, and guarded fixture result contract metadata are not Java/C# runtime comparison evidence.
- Live C# mutation-post rows, executor observation from the guarded boundary, registry observation, projected-row comparison, comparison execution, world-broadcast fanout, action `12` invite dispatch, and runtime/socket comparison are still missing.

## Next Recommended Unit of Work

Next sequential task:

- Wire the guarded fixture result contract into the mutation-post readiness/comparison handoff so future accepted live rows can flow into the existing input envelope without treating disabled projections as live.

Safe candidates:

- Add a deterministic Java artifact timestamp override if future fixture churn around `serverEpochSeconds` becomes noisy.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add more precise readiness blocker wording for executor versus registry observations if the result contract needs separate status rows.

## Commit

Commit message:

```text
[Phase 6][UOW-2208] Add find group mutation guarded fixture result contract
```
