# Phase 6 Session 2204 Completion - FindGroup Mutation C# Trace Row Fixture Report

Date: 2026-06-02
Unit of Work: UOW-2204
Status: Completed

## Scope

This unit added a focused C# mutation-post trace-row fixture report for `CM_FIND_GROUP` actions `2` and `6`.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

C# source reviewed:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupDirectPacketMutationPostBoundaryTraceSchemaService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostComparisonInputEnvelopeService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostCSharpLiveTraceRowFixturePlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostJavaTraceArtifactDirectoryReportService.cs`

This UOW does not wire live C# `CmFindGroup` dispatch, does not execute registry sends, does not add live runtime rows, and does not compare Java/C# rows.

## Changes

- Added `FindGroupMutationPostCSharpTraceRowFixtureReportService`.
- The report classifies action `2`/`6` C# mutation-post rows as missing, shape-valid non-live disabled projections, live boundary evidence, invalid, or unsupported.
- Rows count as live only when boundary, executor, and registry observations are all present.
- The report feeds `FindGroupMutationPostComparisonInputEnvelopeService` so disabled C# rows can be visible as shape rows while still blocked as non-live comparison evidence.
- Added focused tests that build action `2`/`6` rows from existing disabled `CmFindGroup` composition/projection paths and consume checked-in Java repository artifacts as shape-valid Java input.
- Updated live-dispatch design notes to record the new fixture report.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: C# service/test-only trace-row fixture report plus non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostCSharpTraceRowFixtureReportServiceTests|FullyQualifiedName~FindGroupDirectPacketMutationPostBoundaryTraceExportProjectionTests|FullyQualifiedName~FindGroupMutationPostComparisonInputEnvelopeServiceTests" --no-restore
```

- Focused Java/Maven command: not run; no Java source or Java fixture file changed in this UOW, and the Java behavior source was the already-reviewed action `2`/`6` `CM_FIND_GROUP.runImpl` plus `FindGroupService.addRecruitment/addApplication` ordering.
- Broad-validation trigger: none. No live C# dispatch, shared runtime primitive, packet primitive, persistence, common world state, or broad connection side effect changed.
- Broad .NET decision: skipped intentionally. The filtered C# test command built the affected project and dependencies, and no broad trigger applied.
- Why this scope is sufficient: the added service only classifies supplied trace-row fixture values and reuses the existing disabled projection/envelope services; focused tests cover the new service plus both adjacent dependencies.

Result:

- First focused C# attempt failed 4 assertions because tests assumed default Java artifact discovery from the test binary working directory. Tests were corrected to pass the repository artifact root explicitly where checked-in artifacts are required.
- Final focused C# command: passed 14, failed 0, skipped 0. Existing nullable/analyzer warnings were emitted from unrelated C# files.
- `git diff --check`: passed. Git emitted line-ending normalization warnings for touched documentation files on Windows.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `2`/`6` | `Aion.GameServer.Services.FindGroupMutationPostCSharpTraceRowFixtureReportService`; `Aion.GameServer.Services.FindGroupDirectPacketMutationPostBoundaryTraceSchemaService` | Client Packet Boundary / Trace Fixture | Partial | Unit Tested | Partial Parity | C# disabled projection rows preserve Java action `2`/`6` mutation-post schema shape and direct-packet IDs, but they are non-live because production `CmFindGroup` dispatch, executor invocation, and registry observation remain missing. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment` | `Aion.GameServer.Services.FindGroupMutationPostCSharpTraceRowFixtureReportService`; `Aion.GameServer.Services.FindGroupRecruitmentPlanService` | Service Mutation / Trace Fixture | Partial | Unit Tested | Partial Parity | Action `2` disabled C# row is shape-valid for recruitment, posted system message `1400392`, refreshed action `0`, zero broadcasts, and zero invites. No live send ordering or runtime comparison exists. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addApplication` | `Aion.GameServer.Services.FindGroupMutationPostCSharpTraceRowFixtureReportService`; `Aion.GameServer.Services.FindGroupRecruitmentPlanService` | Service Mutation / Trace Fixture | Partial | Unit Tested | Partial Parity | Action `6` disabled C# row is shape-valid for application, posted system message `1400393`, refreshed action `4`, zero broadcasts, and zero invites. No live send ordering or runtime comparison exists. |
| Repository artifacts under `parity-artifacts/find-group/mutation-post/java` | `Aion.GameServer.Services.FindGroupMutationPostComparisonInputEnvelopeService`; `Aion.GameServer.Services.FindGroupMutationPostCSharpTraceRowFixtureReportService` | Golden Fixture Artifact / Envelope Input | Partial | Unit Tested | Partial Parity | Checked-in Java artifacts can feed the C# row fixture report through the envelope as shape-valid Java rows. Disabled C# rows still block on missing live C# evidence and readiness. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupMutationPostCSharpTraceRowFixtureReportServiceTests.Create_DefaultReportBlocksOnMissingCSharpRows` | C# unit | Existing artifact reader/envelope contracts | Default report has no C# rows and does not infer Java artifacts from the test binary working directory. | Focused C# test. | No live rows. |
| `Create_DisabledProjectionRowsAreShapeValidButDoNotSatisfyLiveGate` | C# unit | Java action `2`/`6` mutation-post mappings and disabled C# projection service | Disabled rows are shape-valid but remain non-live because executor and registry observations are false. | Focused C# test using checked-in Java artifacts and disabled C# projections. | No registry observation or live boundary capture. |
| `Create_DisabledProjectionRowsPreserveJavaActionTwoAndSixShape` | C# unit | Java `FindGroupService.addRecruitment/addApplication` system-message and refreshed-list mappings | Action `2`/`6` row references preserve mutation kind, posted system message ids, and refreshed list actions. | Focused C# test. | No runtime comparison. |
| `Create_LiveMarkedRowsSatisfyOnlyCSharpRowsGateAndStillNeedReadiness` | C# unit | Existing comparison envelope readiness rules | Even rows marked with boundary/executor/registry evidence only satisfy the C# row gate; readiness and comparison remain blocked. | Focused C# test. | Rows are synthetic, not real live boundary evidence. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2 Java source artifacts
- Total artifacts ported in this UOW: 1 C# fixture-report service
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 4 Java/C# trace-preparation artifacts
- Total blocked artifacts: live C# mutation-post boundary rows, registry observation, projected-row comparison, and live `CM_FIND_GROUP` dispatch
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- The new C# fixture report is non-live by default and cannot prove Java/C# parity.
- Synthetic live-marked rows in tests are gate-shape checks only; they are not runtime evidence.
- Live C# trace-row emitter, registry send observation, projected-row comparison, world-broadcast fanout, action `12` invite dispatch, and runtime/socket comparison remain missing.

## Next Recommended Unit of Work

Next sequential task:

- Add a focused artifact-backed preflight/readiness update that passes repository Java artifacts and disabled C# fixture rows together, proving the Java and C# shape inputs are both present while readiness remains blocked on live C# boundary/registry evidence.

Safe candidates:

- Add a guarded live-boundary fixture skeleton for action `2`/`6` that records missing executor/registry observations without sending packets.
- Add a deterministic Java artifact timestamp override if future fixture churn around `serverEpochSeconds` becomes noisy.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostCSharpTraceRowFixtureReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostCSharpTraceRowFixtureReportServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2204-Completion.md`
- `docs/Phase-6-Session-2204-Handoff.md`
