# Phase 6 Session 2295 Completion - Row-Pairing Post-Capture Evidence

## Scope

Surfaced explicit-root Java post-capture dry-run command consistency evidence inside the Java/C# row-pairing readiness report for `CM_FIND_GROUP` mutation-post action `2` and action `6`.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

C# source reviewed:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostJavaCSharpRowPairingReadinessReportService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostExplicitRootJavaPostCaptureValidatorSummaryService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostJavaCSharpRowPairingReadinessReportServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostExplicitRootJavaPostCaptureValidatorSummaryServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostValueProjectionHandoffGateServiceTests.cs`

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated. Current working context remains in the latest completion/handoff documents.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostJavaCSharpRowPairingReadinessReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostJavaCSharpRowPairingReadinessReportServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostValueProjectionHandoffGateServiceTests.cs`

The Java/C# row-pairing readiness report now includes `JavaPostCaptureDryRunCommandConsistencyEvidence`, sourced from `FindGroupMutationPostExplicitRootJavaPostCaptureValidatorSummary.DryRunCommandConsistencyEvidence`. Row-pairing readiness now preserves the Java capture command gate and post-capture blocker chain when Java artifacts are missing and when Java/C# action pairs can feed value projection but runtime comparison remains blocked.

The report remains non-live: it pairs shape-valid Java artifacts with accepted C# boundary-row handoff metadata only, does not project values, does not execute sends, does not run runtime comparison, and does not claim verified parity.

No Java source, fixture, packet send, live dispatch, reader invocation, value read, materialization, result emission, runtime comparison, capture execution, or verified parity claim changed.

## Validation Decision

Changed surface:

- C# non-live Java/C# row-pairing readiness metadata plus unit tests.

Specific behavior/contract:

- The Java/C# row-pairing readiness report preserves post-capture dry-run command consistency evidence while continuing to block runtime comparison and verified parity until accepted live C# boundary rows and runtime row values exist.

Focused C# validation, first attempt:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostJavaCSharpRowPairingReadinessReportServiceTests|FullyQualifiedName~FindGroupMutationPostExplicitRootJavaPostCaptureValidatorSummaryServiceTests" --no-restore
```

Result: failed during test project compilation. The new row-pairing report constructor field exposed one direct test fixture constructor in `FindGroupMutationPostValueProjectionHandoffGateServiceTests.cs` that needed the new metadata argument. Product code did not fail.

Focused C# validation, final:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostJavaCSharpRowPairingReadinessReportServiceTests|FullyQualifiedName~FindGroupMutationPostExplicitRootJavaPostCaptureValidatorSummaryServiceTests|FullyQualifiedName~FindGroupMutationPostValueProjectionHandoffGateServiceTests" --no-restore
```

Result: passed 15, failed 0, skipped 0. Existing nullable/analyzer warnings remain from unrelated files.

Repository hygiene:

```powershell
git diff --check
```

Result: passed. Git reported line-ending normalization warnings only for edited files.

Focused Java/Maven validation: not run. No Java source or fixture changed in this UOW; Java action `2`/`6` source was reviewed directly as source-of-truth context for the non-live row-pairing metadata.

Broad-validation trigger: none. Full `.NET` project tests, solution tests, and full solution builds were skipped because the filtered C# command built the affected project and dependencies and covered the changed row-pairing readiness report, the directly adjacent post-capture validator summary, and the adjacent value-projection handoff test fixture touched for constructor compatibility.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP.runImpl` | `Aion.GameServer.Services.FindGroupMutationPostJavaCSharpRowPairingReadinessReportService` | Java/C# Row-Pairing Readiness Metadata | Partial | Unit Tested | Partial Parity | Row-pairing readiness now preserves post-capture dry-run command consistency evidence, including command-provider consistency rows and command-decision evidence that carries nested blocker summary, acceptance matrix, live-capture preflight, runtime-comparison handoff, executor consistency audit, executor bridge, result-emission blocker, materialization, projected-value row, and accepted-boundary-row handoff metadata where present. Metadata only; no value projection, comparison, or live dispatch was run or enabled. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment/addApplication` | `Aion.GameServer.Services.FindGroupMutationPostJavaCSharpRowPairingReadinessReportService` | Java/C# Row-Pairing Readiness Metadata | Partial | Unit Tested | Partial Parity | Java action `2`/`6` mutation-post behavior remains represented as action/mutation pairing readiness metadata only. Verified parity remains blocked until runtime-backed Java artifacts, accepted C# boundary rows, runtime row values, concrete reader invocation, row identity decisions, comparison, materialization, result emission, runtime comparison, and live dispatch evidence exist. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `Create_DefaultReportBlocksOnMissingJavaArtifactsFirst` | Unit | Java action `2`/`6` mutation-post mapping and post-capture evidence metadata | Default row-pairing readiness blocks on missing Java artifacts and still preserves post-capture dry-run command consistency evidence. | Non-live row-pairing metadata plus source review. | No Java capture, accepted live C# rows, executor execution, comparison, or emitted results. |
| `Create_ShapeValidJavaAndAcceptedCSharpRowsCanFeedValueProjectionButNotParity` | Unit | Java action `2`/`6` mutation-post shape fixtures and accepted C# handoff metadata | Shape-valid Java artifacts plus accepted C# rows can feed value projection metadata, preserve dry-run evidence, and still block runtime comparison and verified parity. | Non-live row-pairing metadata plus source review. | No runtime row values, concrete value reads, comparison, materialization, result emission, or live dispatch evidence. |

## Summary Metrics

- Java artifacts reviewed: 2
- C# non-live services updated: 1
- C# test classes updated: 2
- Verified parity rows added: 0
- Partial parity rows updated: 2
- Blocked by missing evidence: runtime-backed Java artifacts, accepted live C# boundary rows from production `ProcessPacketAsync`, runtime Java/C# row values, concrete reader invocation, row identity decisions, value comparison, result materialization/emission evidence, runtime comparison execution, and live dispatch.
- Estimated Phase 6 completion: unchanged; this UOW improves row-pairing evidence traceability but adds no runtime evidence.

## Commit

Commit message:

```text
[Phase 6][UOW-2295] Surface post-capture evidence in row pairing
```

## Next Recommended UOW

Surface row-pairing readiness evidence inside the runtime evidence checklist. Keep the unit metadata-only: the runtime evidence checklist should continue listing existing providers, but its Java artifact row can preserve row-pairing readiness evidence including post-capture dry-run command consistency evidence.

Safe candidates:

- Inspect `FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService` and its tests.
- Add checklist row evidence sourced from `FindGroupMutationPostJavaCSharpRowPairingReadinessReport.JavaPostCaptureDryRunCommandConsistencyEvidence` or an adjacent row-pairing summary field.
- Keep validation focused on runtime evidence checklist tests plus row-pairing readiness tests.
