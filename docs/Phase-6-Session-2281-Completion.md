# Phase 6 Session 2281 Completion - Projected Value Row Preflight Evidence

## Scope

Surfaced value-reader function execution preflight row evidence inside the projected-value row contract for `CM_FIND_GROUP` mutation-post action `2` and action `6`.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

C# source reviewed:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostValueReaderProjectedValueRowContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostValueReaderFunctionExecutionPreflightService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostValueReaderProjectedValueRowContractServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostValueReaderFunctionExecutionPreflightServiceTests.cs`

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated. Current working context remains in the latest completion/handoff documents.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostValueReaderProjectedValueRowContractService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostValueReaderProjectedValueRowContractServiceTests.cs`

The projected-value row contract now includes `functionPreflightRows` evidence in every projected value row. This preserves value-reader function execution preflight evidence, including typed-reader gate rows, runtime-row-value intake rows, value-projection handoff rows, and accepted-boundary-row handoff status.

Projected rows still carry `<not-read>` Java/C# values and remain blocked until reader invocation, value reads, comparison, and result emission evidence exist.

No live dispatch, packet sends, reader invocation, value reads, comparison execution, materialization, result emission, Java capture, C# capture, or verified parity claim was added.

## Validation Decision

Changed surface:

- C# non-live projected-value row contract metadata plus unit tests.

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostValueReaderProjectedValueRowContractServiceTests|FullyQualifiedName~FindGroupMutationPostValueReaderFunctionExecutionPreflightServiceTests" --no-restore
```

Result: passed 10, failed 0, skipped 0. Existing nullable/analyzer warnings remain from unrelated files.

Focused Java/Maven validation: not run. No Java source or fixture changed in this UOW; Java action `2`/`6` source was reviewed directly as source-of-truth context for the non-live projected-value row metadata.

Broad-validation trigger: none. Full `.NET` project tests, solution tests, and full solution builds were skipped because the filtered C# command built the affected project and dependencies and covered the changed projected-value row contract plus the directly adjacent value-reader function execution preflight.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP.runImpl` | `Aion.GameServer.Services.FindGroupMutationPostValueReaderProjectedValueRowContractService` | Projected Value Row Metadata | Partial | Unit Tested | Partial Parity | Projected-value rows now preserve value-reader function preflight evidence, including typed-reader gate and accepted-boundary-row handoff metadata. Metadata only; values remain `<not-read>`. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment/addApplication` | `Aion.GameServer.Services.FindGroupMutationPostValueReaderProjectedValueRowContractService` | Projected Value Row Metadata | Partial | Unit Tested | Partial Parity | Projected-value row contract preserves Java-shaped action/mutation reader requirements but remains blocked until runtime-backed Java artifacts, accepted C# boundary rows, runtime row values, concrete reader invocation, comparison, and result emission evidence exist. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `Create_DefaultContractBlocksBeforeFunctionExecutionPreflight` | Unit | Java action `2`/`6` mutation-post mapping | Default projected-value rows expose function preflight rows that include typed-reader gate, runtime intake, and blocked accepted-boundary-row handoff evidence. | Non-live projected-value row metadata. | No runtime Java artifacts, accepted live C# rows, or value reads. |
| `Create_ReadyFunctionPreflightStillBlocksWhenExecutorPlanNotReady` | Unit | `addRecruitment` and `addApplication` mutation-post actions | Ready function preflight evidence carries runtime intake and accepted-boundary-row handoff status into projected-value rows while executor plan readiness remains blocked. | Non-live projected-value row metadata. | No reader invocation, value projection, comparator execution, or result emission. |

## Summary Metrics

- Java artifacts reviewed: 2
- C# non-live services updated: 1
- C# test classes updated: 1
- Verified parity rows added: 0
- Partial parity rows updated: 2
- Blocked by missing evidence: runtime-backed Java artifacts, accepted live C# boundary rows from production `ProcessPacketAsync`, runtime Java/C# row values, concrete reader invocation, row identity decisions, value comparison, result materialization/emission evidence, runtime comparison execution, and live dispatch.
- Estimated Phase 6 completion: unchanged; this UOW improves projected-value row evidence traceability but adds no runtime evidence.

## Commit

Commit message:

```text
[Phase 6][UOW-2281] Surface preflight evidence in projected value rows
```

## Next Recommended UOW

Surface projected-value row contract evidence inside the projected-value materialization blocker report. Keep the unit metadata-only: the materialization blocker should continue consuming `FindGroupMutationPostValueReaderProjectedValueRowContractService`, but its evidence can preserve the projected rows that now include value-reader function preflight and accepted-boundary-row handoff data.

Safe candidates:

- Inspect `FindGroupMutationPostProjectedValueMaterializationBlockerReportService` and its tests.
- Update the materialization blocker report to include projected-value row evidence without materializing values.
- Capture live boundary/runtime trace evidence only after metadata gates remain visible and a focused command handoff names the exact command.
