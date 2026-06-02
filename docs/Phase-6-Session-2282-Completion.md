# Phase 6 Session 2282 Completion - Materialization Blocker Projected Row Evidence

## Scope

Surfaced projected-value row contract evidence inside the projected-value materialization blocker report for `CM_FIND_GROUP` mutation-post action `2` and action `6`.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

C# source reviewed:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedValueMaterializationBlockerReportService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostValueReaderProjectedValueRowContractService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedValueMaterializationBlockerReportServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostValueReaderProjectedValueRowContractServiceTests.cs`

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated. Current working context remains in the latest completion/handoff documents.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedValueMaterializationBlockerReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedValueMaterializationBlockerReportServiceTests.cs`

The projected-value materialization blocker report now includes `projectedValueRows` evidence in every blocker row. This preserves projected-value row contract evidence, including value-reader function preflight rows, typed-reader gate rows, runtime-row-value intake rows, and accepted-boundary-row handoff status.

Materialization blocker rows still keep all materialization and result-emission flags false. Matched, missing-row, field-mismatch, and ignored-context outputs remain blocked until real projected values, row identity decisions, context attachment, and result emission evidence exist.

No live dispatch, packet sends, reader invocation, value reads, comparison execution, materialization, result emission, Java capture, C# capture, or verified parity claim was added.

## Validation Decision

Changed surface:

- C# non-live projected-value materialization blocker metadata plus unit tests.

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedValueMaterializationBlockerReportServiceTests|FullyQualifiedName~FindGroupMutationPostValueReaderProjectedValueRowContractServiceTests" --no-restore
```

Result: passed 10, failed 0, skipped 0. Existing nullable/analyzer warnings remain from unrelated files.

Focused Java/Maven validation: not run. No Java source or fixture changed in this UOW; Java action `2`/`6` source was reviewed directly as source-of-truth context for the non-live materialization blocker metadata.

Broad-validation trigger: none. Full `.NET` project tests, solution tests, and full solution builds were skipped because the filtered C# command built the affected project and dependencies and covered the changed materialization blocker report plus the directly adjacent projected-value row contract.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP.runImpl` | `Aion.GameServer.Services.FindGroupMutationPostProjectedValueMaterializationBlockerReportService` | Projected Value Materialization Blocker Metadata | Partial | Unit Tested | Partial Parity | Materialization blocker rows now preserve projected-value row evidence, including function preflight and accepted-boundary-row handoff metadata. Metadata only; no values were materialized. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment/addApplication` | `Aion.GameServer.Services.FindGroupMutationPostProjectedValueMaterializationBlockerReportService` | Projected Value Materialization Blocker Metadata | Partial | Unit Tested | Partial Parity | Materialization remains blocked until runtime-backed Java artifacts, accepted C# boundary rows, runtime row values, concrete reader invocation, row identity decisions, comparison, and result emission evidence exist. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `Create_DefaultReportBlocksBeforeProjectedValueRowsAreReady` | Unit | Java action `2`/`6` mutation-post mapping | Default materialization blockers expose projected-value row evidence that includes function preflight and blocked accepted-boundary-row handoff evidence. | Non-live materialization blocker metadata. | No runtime Java artifacts, accepted live C# rows, or materialized values. |
| `Create_ReadyProjectedRowsStillBlockWhenMaterializationPreflightNotReady` | Unit | `addRecruitment` and `addApplication` mutation-post actions | Ready projected-value row evidence carries accepted-boundary-row handoff status into materialization blockers while materialization preflight remains blocked. | Non-live materialization blocker metadata. | No reader invocation, value projection, comparator execution, or result emission. |
| `Create_RuntimeMissingReportMapsMatchedAndFieldMismatchToUnreadProjectedValues` | Unit | Java refreshed `SM_FIND_GROUP` action `0`/`4` outputs | Runtime-missing materialization blockers preserve projected-value row evidence but still block matched and mismatch output on unread values. | Non-live materialization blocker metadata. | No concrete differing field or matched value set. |

## Summary Metrics

- Java artifacts reviewed: 2
- C# non-live services updated: 1
- C# test classes updated: 1
- Verified parity rows added: 0
- Partial parity rows updated: 2
- Blocked by missing evidence: runtime-backed Java artifacts, accepted live C# boundary rows from production `ProcessPacketAsync`, runtime Java/C# row values, concrete reader invocation, row identity decisions, value comparison, result materialization/emission evidence, runtime comparison execution, and live dispatch.
- Estimated Phase 6 completion: unchanged; this UOW improves materialization blocker evidence traceability but adds no runtime evidence.

## Commit

Commit message:

```text
[Phase 6][UOW-2282] Surface projected row evidence in materialization blockers
```

## Next Recommended UOW

Surface projected-value materialization blocker evidence inside the projected-value result emission blocker report. Keep the unit metadata-only: the result emission blocker should continue consuming `FindGroupMutationPostProjectedValueMaterializationBlockerReportService`, but its evidence can preserve the materialization blocker rows that now include projected-value row and accepted-boundary-row handoff data.

Safe candidates:

- Inspect `FindGroupMutationPostProjectedValueResultEmissionBlockerReportService` and its tests.
- Update the result emission blocker report to include materialization blocker row evidence without emitting results.
- Capture live boundary/runtime trace evidence only after metadata gates remain visible and a focused command handoff names the exact command.
