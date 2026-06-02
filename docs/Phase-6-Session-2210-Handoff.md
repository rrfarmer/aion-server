# Phase 6 Session 2210 Handoff - FindGroup Mutation Dry-Run Accepted Row Projection

Date: 2026-06-02
Unit of Work: UOW-2210
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
- `FindGroupMutationPostGuardedFixtureResultContractService` classifies future C# candidate rows for the guarded fixture handoff and accepts only action `2`/`6` C# rows with boundary acceptance, executor observation, registry observation, expected packet shape, and zero broadcast/invite counts.
- `FindGroupMutationPostComparisonInputEnvelopeService` uses the guarded fixture result contract to decide whether C# rows are live handoff rows.
- `FindGroupMutationPostComparisonExecutionBlockerReportService` names the guarded fixture result contract as its own blocker reason.
- `FindGroupMutationPostProjectedRowComparisonDryRunContractService` now carries accepted guarded C# row references as future executor inputs.
- Shape-valid Java artifacts, shape-valid disabled C# rows, guarded fixture skeleton metadata, readiness aggregate metadata, guarded fixture result contract metadata, envelope handoff metadata, and dry-run accepted-row references are not verified parity.

## UOW-2210 Summary

This UOW added accepted guarded C# row references to the projected-row comparison dry-run contract.

Key behavior:

- adds `AcceptedCSharpRows`,
- adds `HasGuardedFixtureResultContract`,
- carries accepted guarded row action, mutation kind, guarded status, row identity, evidence, and planned input source,
- keeps default dry-run blocked with no accepted rows,
- allows synthetic accepted rows to be named as future executor inputs only when supplied through the guarded fixture result contract,
- keeps production `CmFindGroup` dispatch disabled.

Important notes:

- The dry-run contract is an input-shape report only.
- It does not execute side effects, registry sends, or comparison.
- It does not wire production `CmFindGroup` dispatch.
- Synthetic accepted rows in tests are not runtime evidence.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`

## C# Artifacts Reviewed

- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonDryRunContractService`
- `Aion.GameServer.Services.FindGroupMutationPostGuardedFixtureResultContractService`
- `Aion.GameServer.Services.FindGroupMutationPostComparisonExecutionResultContractService`
- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonResultSkeletonService`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonDryRunContractService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonDryRunContractServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonResultSkeletonServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2210-Completion.md`
- `docs/Phase-6-Session-2210-Handoff.md`

## Validation In UOW-2210

Validation decision:

- Changed surface: C# service/test-only dry-run comparison input-shape wiring plus non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonDryRunContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonResultSkeletonServiceTests|FullyQualifiedName~FindGroupMutationPostGuardedFixtureResultContractServiceTests|FullyQualifiedName~FindGroupMutationPostComparisonInputEnvelopeServiceTests|FullyQualifiedName~FindGroupMutationPostComparisonExecutionBlockerReportServiceTests|FullyQualifiedName~FindGroupMutationPostComparisonExecutionResultContractServiceTests" --no-restore
```

- Focused Java/Maven command: not run; no Java source or Java fixture file changed in this UOW.
- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally because filtered tests built the affected C# project/dependencies and no broad trigger applied.
- Why this scope is sufficient: the edited service is a non-live dry-run contract and the focused filter covers its guarded-row source, comparison envelope/blocker surfaces, result contract, and downstream result skeleton.

Result:

- Focused C# command: passed 35, failed 0, skipped 0. Existing nullable/analyzer warnings were emitted from unrelated C# files.
- `git diff --check`: passed. Git emitted line-ending normalization warnings for touched documentation and C# files on Windows.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `2`/`6` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonDryRunContractService`; `Aion.GameServer.Services.FindGroupMutationPostGuardedFixtureResultContractService` | Client Packet Boundary / Dry-Run Comparison Contract | Partial | Unit Tested | Partial Parity | Dry-run contract now names accepted guarded C# row references as future executor inputs. No live boundary rows, registry observation, or comparison exists. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonDryRunContractService` | Service Mutation / Dry-Run Comparison Contract | Partial | Unit Tested | Partial Parity | Action `2` accepted-row references carry Java-derived row identity and packet expectations, but evidence is synthetic/non-live only. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addApplication` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonDryRunContractService` | Service Mutation / Dry-Run Comparison Contract | Partial | Unit Tested | Partial Parity | Action `6` accepted-row references carry Java-derived row identity and packet expectations, but evidence is synthetic/non-live only. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- Shape-valid Java fixture artifacts, disabled C# projection rows, guarded fixture skeleton metadata, readiness aggregate metadata, guarded fixture result contract metadata, envelope handoff metadata, and dry-run accepted-row references are not Java/C# runtime comparison evidence.
- Live C# mutation-post rows, executor observation from the guarded boundary, registry observation, projected-row comparison, comparison execution, world-broadcast fanout, action `12` invite dispatch, and runtime/socket comparison are still missing.

## Next Recommended Unit of Work

Next sequential task:

- Add a non-live Java accepted-row reference projection alongside the accepted C# row references so the dry-run contract can name both future executor inputs without executing comparison.

Safe candidates:

- Add a deterministic Java artifact timestamp override if future fixture churn around `serverEpochSeconds` becomes noisy.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add more precise readiness blocker wording for executor versus registry observations if the result contract needs separate status rows.

## Commit

Commit message:

```text
[Phase 6][UOW-2210] Add find group mutation dry-run accepted row references
```
