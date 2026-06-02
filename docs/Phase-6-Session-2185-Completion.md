# Phase 6 Session 2185 Completion - FindGroup Mutation Comparison Execution Result Contract

Date: 2026-06-02
Unit of Work: UOW-2185
Status: Completed

## Scope

This unit added a non-live comparison execution result contract for future projected `CM_FIND_GROUP` action `2` and `6` mutation-post trace rows.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

This UOW does not wire live `CmFindGroup` dispatch, does not capture Java artifacts, does not emit C# runtime trace rows, does not observe live registry sends, and does not execute row comparison.

## Changes

- Added `FindGroupMutationPostComparisonExecutionResultContractService`.
- The contract maps existing comparison projection fields into explicit future mismatch categories:
  - compatibility gate mismatch,
  - row identity mismatch,
  - mutation state mismatch,
  - direct packet mismatch,
  - registry observation mismatch,
  - side-effect guard mismatch,
  - runtime-only ignored fields.
- Preserved Java action-specific expectations:
  - action `2`: recruitment mutation, system message `1400392`, refreshed list action `0`,
  - action `6`: application mutation, system message `1400393`, refreshed list action `4`.
- Kept `traceSource` and `serverEpochSeconds` ignored for equality while documenting how they may appear as runtime context in a mismatch report.
- Kept the contract blocked by default until generated Java rows, live C# rows, registry observation, and preflight readiness exist.
- Added focused tests for default blocked state, action coverage, mismatch-category mapping, runtime-only ignored fields, Java/C# value reporting rules, and preflight-block status.
- Updated live-dispatch design notes to include the comparison result contract and blockers.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: focused non-live comparison result contract service, focused tests, and non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostComparisonExecutionResultContractServiceTests|FullyQualifiedName~FindGroupMutationPostComparisonKeyProjectionMetadataServiceTests|FullyQualifiedName~FindGroupMutationPostTraceRowReadinessAggregateServiceTests|FullyQualifiedName~FindGroupMutationPostArtifactComparisonPreflightServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed, no Java fixture/instrumentation/serializer exists, and no generated Java artifacts exist for row comparison.
- Broad-validation trigger: none. No live connection dispatch, live side effects, packet primitive, common runtime base, persistence, shared infrastructure, Java source, or broad behavior surface changed.
- Broad .NET decision: skipped intentionally. The filtered `dotnet test` command built the affected project and dependencies.
- Why this scope is sufficient: the filtered tests cover the new contract and its immediate projection metadata, trace-row readiness, and artifact preflight dependencies without spending time on unrelated suites.

Result:

- Passed: 24
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupMutationPostComparisonExecutionResultContractService` | Comparison Result Contract | Blocked | Unit Tested | Partial Parity | The contract defines future mismatch reporting for action `2`/`6` mutation-post rows, but Java capture, C# live rows, registry observation, and comparison execution are missing. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupMutationPostComparisonExecutionResultContractService`; `Aion.GameServer.Services.FindGroupMutationPostComparisonKeyProjectionMetadataService`; `Aion.GameServer.Services.FindGroupMutationPostTraceRowReadinessAggregateService`; `Aion.GameServer.Services.FindGroupMutationPostArtifactComparisonPreflightService` | Mutation-Post Comparison Result Contract | Partial | Unit Tested | Partial Parity | The contract preserves Java mutation-before-posted-message-before-refreshed-list fields and action-specific packet ids, but it only defines result reporting and proves no runtime parity. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupMutationPostComparisonExecutionResultContractServiceTests.Create_DefaultContractBlocksOnMissingTraceRowsAndIsNonLive` | Unit | Java source review and current readiness blockers | Default contract remains non-live and blocked until trace rows and preflight readiness exist. | Focused contract assertion. | No Java/C# rows or comparison execution. |
| `FindGroupMutationPostComparisonExecutionResultContractServiceTests.Create_CoversActionTwoAndSixWithJavaPacketExpectations` | Unit | Java `CM_FIND_GROUP.runImpl`; `FindGroupService.addRecruitment/addApplication` | Contract covers action `2` and `6` with Java method, system message id, and refreshed list action expectations. | Focused Java-derived action assertion. | No runtime rows. |
| `FindGroupMutationPostComparisonExecutionResultContractServiceTests.Create_MapsProjectionRolesToDifferenceKinds` | Unit | Existing Java-derived projection metadata | Projection roles map to explicit mismatch categories. | Focused mapping assertion. | No actual mismatch report emitted. |
| `FindGroupMutationPostComparisonExecutionResultContractServiceTests.Create_IgnoresRuntimeOnlySourceAndClockFieldsForEquality` | Unit | Existing projection metadata | `traceSource` and raw `serverEpochSeconds` remain ignored for equality. | Focused runtime-only field assertion. | No same-clock fixture. |
| `FindGroupMutationPostComparisonExecutionResultContractServiceTests.Create_RequiredFieldRulesNameJavaAndCSharpValuesWithoutExecutingComparison` | Unit | Java action-specific packet and state fields | Required mismatch rules name `javaValue`, `csharpValue`, field name, action, mutation kind, and Java source evidence. | Focused result-rule assertion. | No comparison execution. |
| `FindGroupMutationPostComparisonExecutionResultContractServiceTests.Create_ReadinessWithoutPreflightStillBlocksPreflightReadinessSeparately` | Unit | Readiness dependency chain | Contract reports missing preflight readiness separately after trace-row blockers are synthetically cleared. | Focused status-priority assertion. | Synthetic readiness only. |

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
- The comparison result contract is non-live metadata and cannot prove Java/C# runtime parity.
- World-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add a guarded comparison input envelope for action `2`/`6` mutation-post rows that can hold Java row references, C# row references, projection metadata, readiness status, and the result contract without executing comparison until prerequisites are satisfied.

Safe candidates:

- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Implement the targeted Java/Maven fixture only when ready to edit Java source and capture artifacts.
- Add a live direct-packet boundary test or trace for action `0`/`4` before mutation actions if a lower-risk live observation path is preferred.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostComparisonExecutionResultContractService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostComparisonExecutionResultContractServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2185-Completion.md`
- `docs/Phase-6-Session-2185-Handoff.md`
