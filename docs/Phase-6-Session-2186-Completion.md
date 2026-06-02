# Phase 6 Session 2186 Completion - FindGroup Mutation Comparison Input Envelope

Date: 2026-06-02
Unit of Work: UOW-2186
Status: Completed

## Scope

This unit added a guarded comparison input envelope for future projected `CM_FIND_GROUP` action `2` and `6` mutation-post trace rows.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

This UOW does not wire live `CmFindGroup` dispatch, does not capture Java artifacts, does not emit C# runtime trace rows, does not observe live registry sends, and does not execute row comparison.

## Changes

- Added `FindGroupMutationPostComparisonInputEnvelopeService`.
- The envelope collects:
  - shape-valid Java row references,
  - live C# row references,
  - comparison projection metadata,
  - trace-row readiness aggregate,
  - comparison result contract.
- Preserved conservative status priority:
  - missing Java rows block first,
  - missing live C# rows block next,
  - missing readiness/result-contract gates block last.
- Required C# rows to have live boundary acceptance, executor invocation, and registry-send observation before they count as live evidence.
- Explicitly rejected disabled C# sample projections as comparison-ready C# rows.
- Added focused tests for default missing Java rows, shape-valid Java rows moving the blocker to C# rows, disabled C# projection rejection, live rows blocked by readiness, synthetic ready envelope, and projection/result-contract evidence.
- Updated live-dispatch design notes to include the comparison input envelope and remaining blockers.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: focused non-live comparison input envelope service, focused tests, and non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostComparisonInputEnvelopeServiceTests|FullyQualifiedName~FindGroupMutationPostComparisonExecutionResultContractServiceTests|FullyQualifiedName~FindGroupMutationPostTraceRowReadinessAggregateServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactDirectoryReportServiceTests|FullyQualifiedName~FindGroupMutationPostArtifactComparisonPreflightServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed, no Java fixture/instrumentation/serializer exists, and no generated Java artifacts exist for row comparison.
- Broad-validation trigger: none. No live connection dispatch, live side effects, packet primitive, common runtime base, persistence, shared infrastructure, Java source, or broad behavior surface changed.
- Broad .NET decision: skipped intentionally. The filtered `dotnet test` command built the affected project and dependencies.
- Why this scope is sufficient: the filtered tests cover the new envelope and its immediate Java artifact reader, readiness, preflight, and result-contract dependencies without spending time on unrelated suites.

Result:

- Passed: 29
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupMutationPostComparisonInputEnvelopeService` | Comparison Input Envelope | Blocked | Unit Tested | Partial Parity | The envelope defines future comparison inputs for action `2`/`6`, but Java capture, C# live rows, registry observation, and comparison execution are missing. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupMutationPostComparisonInputEnvelopeService`; `Aion.GameServer.Services.FindGroupMutationPostComparisonExecutionResultContractService`; `Aion.GameServer.Services.FindGroupMutationPostTraceRowReadinessAggregateService`; `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactDirectoryReportService` | Mutation-Post Comparison Input Envelope | Partial | Unit Tested | Partial Parity | The envelope preserves Java mutation-before-posted-message-before-refreshed-list row references and action-specific packet ids, but it only guards inputs and proves no runtime parity. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupMutationPostComparisonInputEnvelopeServiceTests.Create_DefaultEnvelopeBlocksOnMissingJavaRowsAndIsNonLive` | Unit | Java source review and current artifact blockers | Default envelope remains non-live and blocked on missing Java rows. | Focused envelope assertion. | No Java artifacts or live C# rows. |
| `FindGroupMutationPostComparisonInputEnvelopeServiceTests.Create_ShapeValidJavaRowsMoveBlockerToLiveCSharpRows` | Unit | Java action `2`/`6` artifact shape | Shape-valid Java rows satisfy Java gate and move blocker to C# rows. | Focused Java-row assertion. | Synthetic shape-valid rows only. |
| `FindGroupMutationPostComparisonInputEnvelopeServiceTests.Create_DisabledCSharpProjectionRowsDoNotCountAsLiveRows` | Unit | Deferred live boundary requirement | Disabled C# sample projections are not accepted as live rows. | Focused C# row guard assertion. | No live C# fixture. |
| `FindGroupMutationPostComparisonInputEnvelopeServiceTests.Create_LiveRowsWithoutReadinessStillBlockReadiness` | Unit | Readiness dependency chain | Live-looking rows still require readiness/result-contract gates. | Focused readiness gate assertion. | Synthetic rows only. |
| `FindGroupMutationPostComparisonInputEnvelopeServiceTests.Create_ReadyInputsProduceReadyEnvelopeWithoutExecutingComparison` | Unit | Comparison preflight shape | Synthetic ready inputs produce a ready envelope without executing comparison. | Focused status assertion. | Does not compare rows. |
| `FindGroupMutationPostComparisonInputEnvelopeServiceTests.Create_KeepsProjectionAndResultContractEvidenceInEnvelope` | Unit | Projection/result contract metadata | Envelope carries projection metadata and result-contract evidence. | Focused metadata assertion. | No mismatch report emitted. |

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
- The comparison input envelope is non-live metadata and cannot prove Java/C# runtime parity.
- World-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add a guarded comparison execution blocker report for action `2`/`6` mutation-post rows that consumes the input envelope and explains why comparison is not executed until all gates are ready.

Safe candidates:

- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Implement the targeted Java/Maven fixture only when ready to edit Java source and capture artifacts.
- Add a live direct-packet boundary test or trace for action `0`/`4` before mutation actions if a lower-risk live observation path is preferred.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostComparisonInputEnvelopeService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostComparisonInputEnvelopeServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2186-Completion.md`
- `docs/Phase-6-Session-2186-Handoff.md`
