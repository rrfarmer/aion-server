# Phase 6 Session 2228 Completion - Value Reader Preflight Metadata Integration

Date: 2026-06-02
Unit of Work: UOW-2228
Status: Completed

## Scope

This unit integrated the non-live value-reader preflight contract into the value-reader readiness summary and runtime evidence metadata for `CM_FIND_GROUP` action `2` and action `6` mutation-post comparison.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureSerializer.java`

C# source reviewed:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderPreflightContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderReadinessSummaryService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonLiveInputHandoffContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService.cs`

This UOW does not wire live C# `CmFindGroup` dispatch, does not execute registry sends, does not parse Java trace JSON values, does not read C# trace-export values, does not compare rows, and does not emit real result rows.

## Changes

- Added `PreflightContract` as the second value-reader readiness summary stage.
- Added `HasPreflightContract` to `FindGroupMutationPostProjectedRowComparisonValueReaderReadinessSummary`.
- Default readiness creation now builds the preflight from the already-created design contract, avoiding recursive live-input handoff construction.
- Preflight stage evidence records status, field count, reader kind count, schema-v1 type-map availability, and disabled Java/C# read flags.
- Live-input handoff now describes the value-reader summary as linking design, typed-reader preflight, skeleton, and blocked-result report.
- Runtime evidence checklist now maps `ValueReaderReadinessSummary` to both:
  - `FindGroupMutationPostProjectedRowComparisonValueReaderReadinessSummaryService`
  - `FindGroupMutationPostProjectedRowComparisonValueReaderPreflightContractService`
- Updated focused tests and live-dispatch design notes.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: non-live C# service/test metadata plus design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderReadinessSummaryServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderPreflightContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonLiveInputHandoffContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests" --no-restore
```

- Focused Java/Maven command: not run; no Java source or Java fixture changed, and the unit only wires existing C# non-live metadata based on reviewed Java action/schema sources.
- Broad-validation trigger: none. This unit does not modify packet primitives, live dispatch, shared runtime state, persistence, scheduling, connection side effects, or broad model state.
- Broad .NET decision: skipped intentionally. The filtered C# test command built the affected project and dependencies, and no broad trigger applied.
- Why this scope is sufficient: the edited services are non-live metadata/readiness/reporting surfaces; the focused filter covers the new preflight stage, the preflight source contract, the handoff row that consumes the summary, and the checklist mapping that names providers.

Result:

- First focused C# run: failed 1 test because the runtime evidence checklist note changed from `reads no values` to `read no values`. The service note was aligned to the expected wording while still naming preflight metadata.
- Final focused C# command: passed 21, failed 0, skipped 0. Existing nullable/analyzer warnings were emitted from unrelated C# files.
- `git diff --check`: passed with usual Windows line-ending warnings.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `2`/`6` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderReadinessSummaryService` | Client Packet Boundary / Value Reader Readiness | Partial | Unit Tested | Partial Parity | Readiness summary now includes typed-reader preflight metadata. No live boundary dispatch, runtime row value reads, value comparison, result emission, or socket comparison exists. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonLiveInputHandoffContractService` | Service Mutation / Live Input Handoff | Partial | Unit Tested | Partial Parity | Action `2` value-reader metadata is represented as non-live handoff evidence only; no Java/C# values are read or compared. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addApplication` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService` | Service Mutation / Runtime Evidence Checklist | Partial | Unit Tested | Partial Parity | Action `6` checklist now names the preflight provider, but runtime evidence and comparison remain missing. |
| `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureSerializer` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderPreflightContractService` | Java Trace Serializer / Value Reader Preflight | Partial | Unit Tested | Partial Parity | Java serializer schema fields inform typed-reader preflight metadata, but no JSON values are parsed in this UOW. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupMutationPostProjectedRowComparisonValueReaderReadinessSummaryServiceTests.Create_DefaultSummaryBlocksBeforeDesignReadiness` | C# unit | Reviewed Java action `2`/`6` mutation-post schema sources | Default value-reader summary includes a preflight contract and keeps reads/comparison disabled. | Focused C# test. | No runtime values or live rows. |
| `Create_DefaultSummaryListsEachValueReaderStage` | C# unit | Java schema-v1 field/token shape reviewed through serializer | Stages are design, preflight, skeleton, and blocked report; preflight stage records reader kinds and disabled read flags. | Focused C# test. | No typed reader implementation. |
| `Create_JavaOnlyRowsBlockAtMissingAcceptedRows` | C# unit | Java/C# row pairing remains required before value reads | Preflight does not bypass missing accepted C# rows. | Focused C# test. | No live C# rows. |
| `Create_PairedRowsStillBlockAtReaderImplementation` | C# unit | Reader implementation remains intentionally deferred | Even paired rows remain blocked by deferred typed readers. | Focused C# test. | No value comparison. |
| `FindGroupMutationPostProjectedRowComparisonLiveInputHandoffContractServiceTests.Create_DefaultContractListsExpectedRuntimeRequirements` | C# unit | Java action `2`/`6` mutation-post source review | Handoff row exposes four-stage value-reader summary including typed-reader preflight. | Focused C# test. | Non-live metadata only. |
| `FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests.Create_JavaRuntimeArtifactRowNamesJavaCaptureAndCSharpReader` | C# unit | Java serializer/schema source review | Checklist maps value-reader readiness to both readiness summary and preflight providers. | Focused C# test. | Runtime value-reader evidence missing. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3 Java source/test artifacts
- Total artifacts ported in this UOW: 1 C# non-live readiness metadata integration
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 4 Java/C# trace-preparation artifacts
- Total blocked artifacts: live Java runtime trace artifacts, live C# boundary rows, boundary executor invocation, registry send observation, value reader implementation, value projection, row identity matching, result emission, runtime/socket comparison, comparator implementation, and live `CM_FIND_GROUP` dispatch
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- The value-reader preflight is now visible in readiness/checklist metadata, but it still cannot prove parity.
- No Java JSON values or C# trace-export values are read or compared.
- Future implementation must validate missing fields, field types, collection ordering, ignored runtime context, live C# trace-row emission, registry observation, and real projected-row comparison before claiming parity.

## Next Recommended Unit of Work

Next sequential task:

- Add a non-live projected-row comparison value-reader mismatch-context preflight contract that names which ignored runtime fields can be attached only after a real missing-row or field-mismatch result exists.

Safe candidates:

- Add a deterministic Java artifact timestamp override if future fixture churn around `serverEpochSeconds` becomes noisy.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add more precise readiness blocker wording for executor versus registry observations if the result contract needs separate status rows.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderReadinessSummaryService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonLiveInputHandoffContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonValueReaderReadinessSummaryServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonLiveInputHandoffContractServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2228-Completion.md`
- `docs/Phase-6-Session-2228-Handoff.md`
