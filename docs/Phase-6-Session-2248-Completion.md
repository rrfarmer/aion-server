# Phase 6 Session 2248 Completion - Java Artifact Root Validation Command Report

## Scope

Added a non-live command-oriented report for checking generated Java `CM_FIND_GROUP` action `2`/`6` mutation-post artifacts at a supplied artifact root. The report wraps the existing C# directory/validator services and records the exact deterministic Java capture command plus the focused C# validator command to run after artifact generation.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureHooks.java`

C# source reviewed:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostJavaTraceArtifactDirectoryReportService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostJavaTraceArtifactValidatorService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostJavaTraceArtifactFileReportService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractService.cs`

## Changes

Added `FindGroupMutationPostJavaArtifactRootValidationCommandReportService`.

The report records:

- Artifact root.
- Expected action `2` and action `6` artifact paths.
- File status from `FindGroupMutationPostJavaTraceArtifactDirectoryReportService`.
- Deterministic timestamp property `aion.findGroupMutationPost.serverEpochSeconds`.
- Deterministic timestamp value `1700000000`.
- Exact Java capture command with explicit artifact root.
- Focused C# validator command.
- Whether generated Java artifacts exist, whether all expected files exist, and whether every file is shape-valid.
- A conservative execution decision that keeps runtime comparison blocked even when files are shape-valid.

Updated `FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService` and `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md` so the Java runtime trace artifact evidence chain includes the new command report.

`docs/PHASE-6-PROGRESS.md` was intentionally not touched.

## Validation Decision

Changed surface:

- Non-live C# service and unit tests.
- Adjacent runtime evidence checklist provider text.
- Design/session documentation.

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostJavaArtifactRootValidationCommandReportServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactDirectoryReportServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactValidatorServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests" --no-restore
```

Result: passed 21, failed 0, skipped 0. Existing nullable/analyzer warnings remain from unrelated files.

Focused Java/Maven validation: not run. No Java source or fixture changed in this UOW; the new artifact-root report is C# non-live metadata over existing Java capture commands and existing C# artifact readers.

Broad-validation trigger: none. Full `.NET` build/suite was skipped because the filtered C# command built the affected project and dependencies and covered the new report plus directly adjacent directory/validator/checklist services.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.findgroup.FindGroupMutationPostTraceCaptureHooks` | `Aion.GameServer.Services.FindGroupMutationPostJavaArtifactRootValidationCommandReportService` | Java Capture Artifact Command Metadata | Partial | Unit Tested | Partial Parity | Records deterministic capture command and artifact-root validation state for action `2`/`6` artifacts. Does not generate Java artifacts, execute C# boundary capture, or compare runtime rows. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment/addApplication` | `Aion.GameServer.Services.FindGroupMutationPostJavaArtifactRootValidationCommandReportService` | Service Mutation Artifact Readiness Metadata | Partial | Unit Tested | Partial Parity | Reuses C# artifact directory/validator results for generated Java files and keeps runtime comparison blocked after shape validation. Live C# rows, registry observation, value projection, materialization, emission, and runtime comparison remain missing. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `Create_MissingDirectoryNamesCaptureAndValidatorCommands` | Unit | Java capture command metadata and C# file report | Missing artifact root reports expected action `2`/`6` files and names deterministic Java/C# commands. | Non-live metadata only. | Does not run Java capture. |
| `Create_ShapeValidArtifactsRemainRuntimeComparisonBlocked` | Unit | Existing C# artifact validator schema derived from Java action `2`/`6` behavior | Shape-valid generated files remain blocked for runtime comparison. | Shape validation only. | No live C# rows or runtime comparison. |
| `Create_InvalidArtifactKeepsValidatorTargetAndBlocksComparison` | Unit | Existing C# artifact validator schema | Invalid generated file keeps validator target and blocks comparison. | Shape validation only. | No runtime evidence. |

Adjacent updated test:

- `FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests.Create_MetadataRowsPointToExistingNonLiveProviders`

## Summary Metrics

- Java artifacts reviewed: 3
- C# non-live services added: 1
- Verified parity rows added: 0
- Partial parity rows added: 2
- Blocked by missing evidence: generated runtime-backed Java artifact files, live C# boundary rows, executor observation, registry observation, row identity, value projection, materialization, emission, runtime/socket comparison, executable implementation, and live dispatch.
- Estimated Phase 6 completion: unchanged; this UOW improves evidence workflow clarity but adds no runtime parity evidence.

## Next Recommended UOW

Add a C# non-live report that records the deterministic timestamp property across all value-reader capture command providers and verifies the Java capture runbook, live-capture preflight, execution blocker summary, and artifact-root validation command report all agree on the same property/value.

Safe candidates:

- Capture live boundary/runtime trace evidence for shared singleton caller interleavings.
- Tighten precise blocker wording for executor observations versus registry observations.
- Add a command example for intentional explicit-root artifact capture using a temporary root and the deterministic timestamp property.
