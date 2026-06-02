# Phase 6 Session 2188 Completion - FindGroup Mutation Projected Row Comparison Dry-Run Contract

Date: 2026-06-02
Unit of Work: UOW-2188
Status: Completed

## Scope

This unit added a non-live dry-run contract for a future projected-row comparison executor for `CM_FIND_GROUP` action `2` and `6` mutation-post trace rows.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

This UOW does not wire live `CmFindGroup` dispatch, does not capture Java artifacts, does not emit C# runtime trace rows, does not observe live registry sends, and does not execute row comparison.

## Changes

- Added `FindGroupMutationPostProjectedRowComparisonDryRunContractService`.
- The dry-run contract consumes:
  - comparison execution blocker report,
  - comparison execution result contract.
- The contract defines:
  - required equality input fields,
  - ignored runtime context fields,
  - planned output kinds: matched, missing Java row, missing C# row, field mismatch, ignored runtime context,
  - Java action-specific expectations for action `2` and `6`.
- Preserved Java expectations:
  - action `2`: recruitment mutation, system message `1400392`, refreshed list action `0`,
  - action `6`: application mutation, system message `1400393`, refreshed list action `4`.
- Kept the default contract blocked by the execution blocker report.
- Added focused tests for default blocked state, action coverage, output kinds, required field mismatch output shape, ignored runtime context, and synthetic ready blocker-report behavior.
- Updated live-dispatch design notes to include the dry-run contract and remaining blockers.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: focused non-live projected-row comparison dry-run contract service, focused tests, and non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonDryRunContractServiceTests|FullyQualifiedName~FindGroupMutationPostComparisonExecutionBlockerReportServiceTests|FullyQualifiedName~FindGroupMutationPostComparisonExecutionResultContractServiceTests|FullyQualifiedName~FindGroupMutationPostComparisonInputEnvelopeServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed, no Java fixture/instrumentation/serializer exists, and no generated Java artifacts exist for row comparison.
- Broad-validation trigger: none. No live connection dispatch, live side effects, packet primitive, common runtime base, persistence, shared infrastructure, Java source, or broad behavior surface changed.
- Broad .NET decision: skipped intentionally. The filtered `dotnet test` command built the affected project and dependencies.
- Why this scope is sufficient: the filtered tests cover the new dry-run contract and its immediate blocker-report, result-contract, and envelope dependencies without spending time on unrelated suites.

Result:

- Passed: 22
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonDryRunContractService` | Projected Row Comparison Dry-Run Contract | Blocked | Unit Tested | Partial Parity | The contract defines future equality inputs and output shape for action `2`/`6`, but Java capture, C# live rows, registry observation, and comparison execution are missing. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonDryRunContractService`; `Aion.GameServer.Services.FindGroupMutationPostComparisonExecutionBlockerReportService`; `Aion.GameServer.Services.FindGroupMutationPostComparisonExecutionResultContractService` | Mutation-Post Projected Row Comparison Dry-Run Contract | Partial | Unit Tested | Partial Parity | The dry-run contract preserves Java mutation-before-posted-message-before-refreshed-list fields and action-specific packet ids, but it only names planned executor shape and proves no runtime parity. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupMutationPostProjectedRowComparisonDryRunContractServiceTests.Create_DefaultDryRunBlocksAndDoesNotCompareRows` | Unit | Java source review and current comparison blockers | Default dry-run contract is blocked, non-live, and does not compare rows. | Focused dry-run assertion. | No Java/C# rows or comparison execution. |
| `FindGroupMutationPostProjectedRowComparisonDryRunContractServiceTests.Create_CoversActionTwoAndSixWithJavaPacketExpectations` | Unit | Java `CM_FIND_GROUP.runImpl`; `FindGroupService.addRecruitment/addApplication` | Contract covers action `2` and `6` with Java method, system message id, and refreshed list action expectations. | Focused Java-derived action assertion. | No runtime rows. |
| `FindGroupMutationPostProjectedRowComparisonDryRunContractServiceTests.Create_DefinesPlannedOutputKindsWithoutProducingResults` | Unit | Result contract/output planning | Planned output kinds are named without producing comparison results. | Focused output-shape assertion. | No comparator executed. |
| `FindGroupMutationPostProjectedRowComparisonDryRunContractServiceTests.Create_MapsRequiredFieldsToFieldMismatchOutputShape` | Unit | Java-derived projection fields | Required equality fields map to field mismatch output shape with Java/C# values. | Focused field-output assertion. | No mismatch emitted. |
| `FindGroupMutationPostProjectedRowComparisonDryRunContractServiceTests.Create_KeepsRuntimeOnlyFieldsAsContextNotEqualityInputs` | Unit | Projection metadata | `traceSource` and `serverEpochSeconds` remain ignored runtime context, not equality inputs. | Focused runtime-context assertion. | No same-clock fixture. |
| `FindGroupMutationPostProjectedRowComparisonDryRunContractServiceTests.Create_ReadyBlockerReportAllowsFutureExecutorButStillDryRunOnly` | Unit | Execution blocker report | Synthetic ready blocker report allows a future executor while contract remains non-live. | Focused ready-state assertion. | Synthetic ready state only. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2
- Total artifacts ported in this UOW: 0
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 2
- Total blocked artifacts: 1 live `CM_FIND_GROUP` boundary and 1 mutation-post trace-row capture/comparison chain
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Java fixture, Java instrumentation, Java serializer, generated Java artifacts, C# live fixture, live emitter, registry observation, and deterministic comparison are missing.
- The projected-row comparison dry-run contract is non-live metadata and cannot prove Java/C# runtime parity.
- World-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add a projected-row comparison result skeleton for action `2`/`6` that can represent matched/missing/mismatched rows using the dry-run output contract, while still refusing to instantiate real comparison results until live Java/C# rows exist.

Safe candidates:

- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Implement the targeted Java/Maven fixture only when ready to edit Java source and capture artifacts.
- Add a live direct-packet boundary test or trace for action `0`/`4` before mutation actions if a lower-risk live observation path is preferred.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonDryRunContractService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonDryRunContractServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2188-Completion.md`
- `docs/Phase-6-Session-2188-Handoff.md`
