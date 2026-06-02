# Phase 6 Session 2209 Completion - FindGroup Mutation Guarded Result Handoff Wiring

Date: 2026-06-02
Unit of Work: UOW-2209
Status: Completed

## Scope

This unit wired the guarded fixture result contract into the action `2`/`6` mutation-post comparison input handoff.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

C# source reviewed:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostComparisonInputEnvelopeService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostGuardedFixtureResultContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostComparisonExecutionBlockerReportService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonDryRunContractService.cs`

This UOW does not wire live C# `CmFindGroup` dispatch, does not execute registry sends, does not add live runtime rows, and does not compare Java/C# rows.

## Changes

- Added `GuardedFixtureResultContract` as an explicit comparison input envelope gate.
- Added `HasGuardedFixtureResultContract` to the comparison input envelope.
- Changed C# row references to use `FindGroupMutationPostGuardedFixtureResultContractService` classification for liveness.
- Kept disabled sample projections rejected as non-live.
- Added coverage that rows with live-looking boundary/executor/registry flags but bad packet shape do not enter the live comparison handoff.
- Updated execution blocker reporting so a blocked guarded fixture result contract is named separately from generic missing live C# rows.
- Updated live-dispatch design notes.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: C# service/test-only comparison envelope and blocker handoff wiring plus non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostComparisonInputEnvelopeServiceTests|FullyQualifiedName~FindGroupMutationPostGuardedFixtureResultContractServiceTests|FullyQualifiedName~FindGroupMutationPostComparisonExecutionBlockerReportServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonDryRunContractServiceTests|FullyQualifiedName~FindGroupMutationPostComparisonExecutionResultContractServiceTests" --no-restore
```

- Focused Java/Maven command: not run; no Java source or Java fixture file changed in this UOW.
- Broad-validation trigger: none. This unit does not modify `GameServerConnection.ProcessPacketAsync`, live dispatch, shared runtime primitives, packet primitives, persistence, common world state, or live side effects.
- Broad .NET decision: skipped intentionally. The filtered C# test command built the affected project and dependencies, and no broad trigger applied.
- Why this scope is sufficient: the edited services are non-live handoff reports; focused tests cover the envelope, guarded fixture contract, blocker report, and downstream dry-run/result-contract surfaces.

Result:

- Focused C# command: passed 28, failed 0, skipped 0. Existing nullable/analyzer warnings were emitted from unrelated C# files.
- `git diff --check`: passed. Git emitted line-ending normalization warnings for touched documentation and C# files on Windows.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `2`/`6` | `Aion.GameServer.Services.FindGroupMutationPostComparisonInputEnvelopeService`; `Aion.GameServer.Services.FindGroupMutationPostGuardedFixtureResultContractService` | Client Packet Boundary / Comparison Handoff | Partial | Unit Tested | Partial Parity | Envelope now uses guarded fixture result classification before treating C# rows as live. No live boundary rows, registry observation, or comparison exists. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment` | `Aion.GameServer.Services.FindGroupMutationPostComparisonInputEnvelopeService` | Service Mutation / Comparison Handoff | Partial | Unit Tested | Partial Parity | Action `2` rows must pass guarded result contract shape and observation gates before entering comparison handoff. Evidence is non-live handoff only. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addApplication` | `Aion.GameServer.Services.FindGroupMutationPostComparisonInputEnvelopeService` | Service Mutation / Comparison Handoff | Partial | Unit Tested | Partial Parity | Action `6` rows must pass guarded result contract shape and observation gates before entering comparison handoff. Evidence is non-live handoff only. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupMutationPostComparisonInputEnvelopeServiceTests.Create_DisabledCSharpProjectionRowsDoNotCountAsLiveRows` | C# unit | Java action `2`/`6` runtime sends require live boundary execution | Disabled C# projections remain non-live and guarded fixture contract gate is blocked. | Focused C# test. | No live boundary execution. |
| `Create_LiveRowsWithoutReadinessStillBlockReadiness` | C# unit | Java rows and C# rows are only comparison inputs after readiness gates | Guarded fixture result gate can be satisfied while readiness still blocks comparison. | Focused C# test. | Synthetic rows are not runtime evidence. |
| `Create_BadShapeRowsDoNotEnterLiveHandoffThroughBooleanFlagsAlone` | C# unit | Java posted-message/refreshed-list packet ids must match action mapping | Bad-shape rows with live-looking flags are rejected by guarded contract classification. | Focused C# test. | No live packet send observation. |
| `FindGroupMutationPostComparisonExecutionBlockerReportServiceTests.Create_MapsEnvelopeGateBlockersToExecutionReasons` | C# unit | Comparison must not run until all Java/C# handoff gates are satisfied | Blocker report names missing guarded fixture result contract separately. | Focused C# test. | No comparison execution. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2 Java source artifacts
- Total artifacts ported in this UOW: 1 C# comparison handoff wiring update
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 3 Java/C# trace-preparation artifacts
- Total blocked artifacts: live C# mutation-post boundary rows, executor observation, registry observation, projected-row comparison, and live `CM_FIND_GROUP` dispatch
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- The envelope and blocker reports are metadata/classification only and cannot prove Java/C# runtime parity.
- Synthetic live-shaped rows in tests prove handoff gating, not runtime behavior.
- Live C# trace-row emitter, registry send observation, projected-row comparison, world-broadcast fanout, action `12` invite dispatch, and runtime/socket comparison remain missing.

## Next Recommended Unit of Work

Next sequential task:

- Add a non-live accepted-row reference projection from `FindGroupMutationPostGuardedFixtureResultContractService` into the projected-row comparison dry-run contract, so the future executor input shape names guarded accepted C# rows explicitly without executing comparison.

Safe candidates:

- Add a deterministic Java artifact timestamp override if future fixture churn around `serverEpochSeconds` becomes noisy.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add more precise readiness blocker wording for executor versus registry observations if the result contract needs separate status rows.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostComparisonInputEnvelopeService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostComparisonExecutionBlockerReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostComparisonInputEnvelopeServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostComparisonExecutionBlockerReportServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2209-Completion.md`
- `docs/Phase-6-Session-2209-Handoff.md`
