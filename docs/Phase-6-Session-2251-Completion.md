# Phase 6 Session 2251 Completion - Explicit Root Post Capture Validator Summary

## Scope

Added a non-live post-capture validator summary for explicit-root Java `CM_FIND_GROUP` action `2`/`6` mutation-post artifacts.

Java source reviewed:

- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureTest.java`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureArtifactValidator.java`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureArtifactWriter.java`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureInMemoryArtifactBridge.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureHooks.java`

C# source reviewed:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostJavaTraceArtifactDirectoryReportService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostJavaTraceArtifactValidatorService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostJavaArtifactRootValidationCommandReportService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostExplicitRootJavaCaptureDryRunCommandReportService.cs`

## Changes

Added `FindGroupMutationPostExplicitRootJavaPostCaptureValidatorSummaryService`.

The summary consumes a supplied explicit artifact root through the existing C# directory reader and artifact validator. It reports:

- Missing explicit root.
- Repository-root rejection.
- Missing artifact directory.
- Missing action `2` or action `6` files.
- Invalid artifact JSON/schema/action mapping.
- Shape-valid action `2`/`6` files.
- Trace row count and validation issue count per expected artifact.
- Runtime comparison and verified parity as blocked until accepted live C# boundary rows and comparison evidence exist.

Updated `FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService` and `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md` so the Java runtime trace artifact evidence chain names the post-capture validator summary.

`docs/PHASE-6-PROGRESS.md` was intentionally not touched. Current working context remains in latest completion/handoff docs.

## Validation Decision

Changed surface:

- Non-live C# service and unit tests.
- Adjacent runtime evidence checklist provider text.
- Design/session documentation.

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostExplicitRootJavaPostCaptureValidatorSummaryServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactDirectoryReportServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactValidatorServiceTests|FullyQualifiedName~FindGroupMutationPostJavaArtifactRootValidationCommandReportServiceTests|FullyQualifiedName~FindGroupMutationPostExplicitRootJavaCaptureDryRunCommandReportServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests" --no-restore
```

Result: passed 30, failed 0, skipped 0. Existing nullable/analyzer warnings remain from unrelated files.

Focused Java/Maven validation: not run. No Java source or fixture changed in this UOW, and UOW-2250 already executed the targeted explicit-root Java fixture command. This UOW only adds C# summary metadata over existing C# readers and validator behavior.

Broad-validation trigger: none. Full `.NET` build/suite was skipped because the filtered C# command built the affected project and dependencies and covered the new summary plus directly adjacent directory/validator/command-report/checklist services.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.findgroup.FindGroupMutationPostTraceCaptureArtifactValidator` | `Aion.GameServer.Services.FindGroupMutationPostExplicitRootJavaPostCaptureValidatorSummaryService` | Post Capture Artifact Validation Metadata | Partial | Unit Tested | Partial Parity | Summarizes missing, invalid, and shape-valid explicit-root Java artifacts using the C# directory reader/validator. Does not execute Java capture, C# live boundary capture, or runtime comparison. |
| `com.aionemu.gameserver.services.findgroup.FindGroupMutationPostTraceCaptureInMemoryArtifactBridge` | `Aion.GameServer.Services.FindGroupMutationPostExplicitRootJavaPostCaptureValidatorSummaryService` | Explicit Root Artifact Handoff Metadata | Partial | Unit Tested | Partial Parity | Consumes the explicit artifact root expected by the Java bridge and rejects repository-root validation. Shape-valid files remain insufficient without accepted live C# boundary rows and runtime comparison evidence. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `Create_MissingExplicitRootBlocksBeforeFilesystemValidation` | Unit | Java bridge requires nonblank artifact-root property | Empty root blocks post-capture validation. | Guardrail metadata. | Does not execute Java. |
| `Create_RepositoryRootIsRejectedAsExplicitCaptureRoot` | Unit | Java artifact writer default root and explicit-root handoff path | Repository artifact root is rejected as an isolated explicit capture root. | Guardrail metadata. | Does not inspect live Java output. |
| `Create_MissingDirectoryNamesExpectedRowsAndCaptureCommand` | Unit | Java action `2`/`6` artifact file targets | Missing directory reports expected rows and the explicit-root capture command. | C# file-target metadata. | No generated files. |
| `Create_PartialArtifactsRemainBlockedOnMissingExpectedFile` | Unit | Java validator requires both action files | Single generated action file remains blocked on missing counterpart. | Shape validation for one file only. | No live C# rows. |
| `Create_InvalidArtifactReportsValidationIssuesAndBlocksComparison` | Unit | Java action mapping for application row | Wrong action `6` posted-message id reports validation issues and blocks comparison. | C# validator schema derived from Java mapping. | No runtime comparison. |
| `Create_ShapeValidArtifactsRemainRuntimeComparisonBlocked` | Unit | Java fixture artifact schema and action mappings | Shape-valid action `2`/`6` files are summarized but still cannot start runtime comparison. | Shape-valid artifact evidence only. | Missing accepted live C# boundary rows and comparison. |

Adjacent updated test:

- `FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests.Create_JavaRuntimeArtifactRowNamesJavaCaptureAndCSharpReader`

## Summary Metrics

- Java artifacts reviewed: 5
- C# non-live services added: 1
- Verified parity rows added: 0
- Partial parity rows added: 2
- Blocked by missing evidence: accepted live C# boundary rows, executor observation, registry observation, row identity, value projection, materialization, emission, runtime/socket comparison, executable implementation, and live dispatch.
- Estimated Phase 6 completion: unchanged; this UOW improves post-capture validation visibility but does not add live C# runtime parity evidence.

## Next Recommended UOW

Add a non-live C# live-boundary row intake preflight that states exactly what accepted C# action `2`/`6` boundary rows must contain before the explicit-root Java artifacts can feed runtime comparison.

Safe candidates:

- Capture live boundary/runtime trace evidence for shared singleton caller interleavings.
- Tighten precise blocker wording for executor observations versus registry observations.
- Add a narrow C# documentation-hygiene report that lists current focused validation commands by artifact type.
