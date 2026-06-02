# Phase 6 Session 2255 Handoff - Value Projection Handoff Gate

## Startup Instructions

For the next session, read these documents first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2255-Completion.md`
- `docs/Phase-6-Session-2255-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive; current working state lives in the latest completion and handoff documents.

Java remains the source of truth. Do not claim verified parity without objective Java/C# runtime evidence.

## Current State

UOW-2255 added a non-live value-projection handoff gate for `CM_FIND_GROUP` mutation-post action `2` and action `6`.

New C# artifact:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostValueProjectionHandoffGateService.cs`

New test artifact:

- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostValueProjectionHandoffGateServiceTests.cs`

Updated adjacent artifacts:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2255-Completion.md`

The new gate consumes the Java/C# row-pairing readiness report, value contract, and value-reader readiness summary. It records that future value projection may proceed only after row pairs, value-source mappings, typed readers, and runtime row values exist.

It does not execute `ProcessPacketAsync`, send packets, read Java JSON values, read C# trace-export values, materialize comparison results, execute runtime comparison, or prove verified parity.

## Validation From Last Session

Focused C# validation passed:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostValueProjectionHandoffGateServiceTests|FullyQualifiedName~FindGroupMutationPostJavaCSharpRowPairingReadinessReportServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderPreflightContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderReadinessSummaryServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests" --no-restore
```

Result: passed 30, failed 0, skipped 0. Existing nullable/analyzer warnings remain from unrelated files.

Focused hygiene validation passed:

```powershell
git diff --check
```

Result: passed. Git emitted line-ending normalization warnings for existing files, but no whitespace errors.

Java/Maven was not run because no Java source or fixture changed in UOW-2255. Java action `2`/`6` source was reviewed directly.

Broad `.NET` validation was skipped because there was no broad-validation trigger. The filtered C# test command built the affected project and dependencies.

## Focused Testing Guidance

Use the Phase 6 test targeting matrix in `docs/orchestration-rules.md` before running validation.

For this artifact family:

- Value-projection handoff gate changes: run the edited gate test class plus row-pairing readiness, value contract, value-reader preflight/readiness, and runtime evidence checklist tests.
- Java/C# row-pairing readiness changes: run the edited report test class plus explicit-root post-capture summary, C# live-boundary row intake preflight, and runtime evidence checklist tests.
- C# row-intake/report changes: run the edited test class plus directly adjacent guarded boundary/result/plan/checklist tests with a filtered `dotnet test`.
- Full `.NET` project tests, solution tests, or solution builds require a documented broad-validation trigger from `docs/orchestration-rules.md`.

Treat a passing filtered `dotnet test` as the compile/build signal for the affected project and dependencies. Do not follow it with a full solution build just to get a second compile signal.

## Next Recommended UOW

Add a non-live runtime-row-value evidence intake gate that consumes `FindGroupMutationPostValueProjectionHandoffGateService` and the existing runtime evidence checklist, then records the exact Java artifact rows and accepted C# trace rows required before typed value readers can read equality fields.

Suggested scope:

- Read the startup docs listed above.
- Inspect:
  - `FindGroupMutationPostValueProjectionHandoffGateService.cs`
  - `FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService.cs`
  - `FindGroupMutationPostProjectedRowComparisonValueReaderPreflightContractService.cs`
  - `FindGroupMutationPostProjectedRowComparisonValueReaderSkeletonService.cs`
  - `FindGroupMutationPostProjectedRowComparisonValueReaderBlockedResultReportService.cs`
  - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- Add a small non-live gate/report that names the missing runtime row values before typed reader execution.
- Run filtered C# tests for the new gate plus value-projection handoff, value-reader preflight/skeleton/blocked-result, and runtime evidence checklist tests.
- Skip Java/Maven unless Java source or fixture changes; document Java source review.
- Skip full `.NET` validation unless a broad trigger is documented.
- Update completion/handoff docs and commit.

Safe alternate candidates:

- Capture live boundary/runtime trace evidence for shared singleton caller interleavings.
- Tighten precise blocker wording for executor observations versus registry observations.
- Add a narrow C# documentation-hygiene report that lists current focused validation commands by artifact type.

## Parity Caution

Current status remains partial parity only. The project has explicit-root Java artifact validation, C# accepted-row intake gates, non-live Java/C# row-pairing readiness, and a value-projection handoff gate, but verified parity is still blocked by missing runtime-backed Java artifacts, missing actual accepted live C# boundary rows from production dispatch, missing runtime row values, typed value-reader implementation, value projection/materialization/emission evidence, runtime comparison execution, and live dispatch.
