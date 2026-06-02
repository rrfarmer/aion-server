# Phase 6 Session 2181 Completion - FindGroup Mutation Artifact Comparison Preflight

Date: 2026-06-02
Unit of Work: UOW-2181
Status: Completed

## Scope

This unit added a guarded non-live comparison preflight for future `CM_FIND_GROUP` action `2` and `6` mutation-post Java/C# trace artifacts.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

This UOW does not enable live dispatch, does not generate Java artifacts, does not capture live C# runtime rows, and does not compare runtime rows.

## Changes

- Added `FindGroupMutationPostArtifactComparisonPreflightService`.
- The preflight ties together:
  - expected action `2`/`6` Java artifact file targets,
  - generated Java artifact reader status,
  - live C# trace-row availability,
  - comparison key-projection metadata,
  - live registry-observation evidence,
  - comparison execution and projected-row match result.
- Default status remains blocked on missing generated Java artifacts.
- Shape-valid Java artifacts clear only the Java artifact blocker; live C# rows, registry observation, and comparison execution remain required.
- Even when Java artifacts, live C# rows, and registry observation are supplied, preflight remains blocked until comparison execution occurs and projected rows match.
- Added focused tests for default missing-artifact status, non-live metadata rows, shape-valid artifact progression, missing live C# rows, missing registry observation, unexecuted comparison, and mismatched/matched comparison outcomes.
- Updated live-dispatch design notes to include the artifact comparison preflight and blockers.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: focused non-live artifact comparison preflight service, focused tests, and non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostArtifactComparisonPreflightServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactDirectoryReportServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactFileReportServiceTests|FullyQualifiedName~FindGroupMutationPostComparisonKeyProjectionMetadataServiceTests|FullyQualifiedName~FindGroupMutationPostRegistryObservationTraceContractServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed, no Java instrumentation/serializer exists, no generated Java artifacts exist, and no narrow Java fixture exists for this non-live preflight metadata UOW.
- Broad-validation trigger: none. No live connection dispatch, live side effects, packet primitive, common runtime base, persistence, shared infrastructure, or broad behavior surface changed.
- Broad .NET decision: skipped intentionally.
- Why this scope is sufficient: the filtered tests cover the new preflight and the immediate Java artifact target/reader, key projection, and registry-observation dependencies; the filtered command builds the affected project/dependencies.

Result:

- Passed: 26
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupMutationPostArtifactComparisonPreflightService` | Runtime Comparison Preflight | Blocked | Unit Tested | Partial Parity | Action `2`/`6` comparison gates are aggregated, but generated Java rows, live C# rows, registry observation, and comparison execution are missing. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupMutationPostArtifactComparisonPreflightService`; `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactDirectoryReportService`; `Aion.GameServer.Services.FindGroupMutationPostComparisonKeyProjectionMetadataService` | Mutation-Post Artifact Comparison Preflight | Partial | Unit Tested | Partial Parity | The preflight preserves Java mutation-before-posted-message-before-refreshed-list requirements and action-specific ids, but it performs no row comparison and proves no runtime parity. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupMutationPostArtifactComparisonPreflightServiceTests.Create_DefaultPreflightBlocksOnMissingJavaArtifacts` | Unit | Java action `2`/`6` artifact targets | Default preflight blocks on missing generated Java artifacts and remains non-live. | Focused preflight assertion. | No generated/live rows. |
| `FindGroupMutationPostArtifactComparisonPreflightServiceTests.Create_RecordsArtifactTargetsKeyProjectionAndRegistryContractRows` | Unit | Mutation-post artifact/key/registry contracts | Non-live metadata rows are included without clearing runtime blockers. | Focused aggregate assertion. | Metadata only. |
| `FindGroupMutationPostArtifactComparisonPreflightServiceTests.Create_ShapeValidJavaArtifactsMoveBlockerToLiveCSharpRows` | Unit | Java artifact reader shape contract | Shape-valid Java artifacts clear only the Java artifact blocker. | Synthetic shape-valid artifact assertion. | Synthetic artifacts only. |
| `FindGroupMutationPostArtifactComparisonPreflightServiceTests.Create_WithJavaArtifactsAndLiveRowsStillRequiresRegistryObservation` | Unit | Java posted-message-before-refreshed-list ordering | Live C# rows without registry observation are still blocked. | Focused preflight assertion. | No real live rows. |
| `FindGroupMutationPostArtifactComparisonPreflightServiceTests.Create_WithPrerequisitesStillBlocksUntilComparisonExecutes` | Unit | Runtime comparison requirement | All prerequisites still require comparison execution. | Focused preflight assertion. | No comparer implementation. |
| `FindGroupMutationPostArtifactComparisonPreflightServiceTests.Create_ComparisonExecutionMustMatchProjectedRowsBeforeReady` | Unit | Projected-row comparison requirement | Executed comparison must match projected rows before readiness. | Synthetic matched/mismatched result assertion. | No actual row comparison executed. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2
- Total artifacts ported in this UOW: 0
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 2
- Total blocked artifacts: 1 live `CM_FIND_GROUP` boundary and 1 mutation-post artifact comparison gap
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Generated Java artifacts, Java instrumentation, Java serializer, C# live runtime rows, registry-send observation, encrypted socket capture, and deterministic comparison are still missing.
- The artifact comparison preflight is non-live and cannot prove Java/C# parity.
- World-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add a generated Java artifact capture runbook or fixture-plan metadata for action `2`/`6` mutation-post traces, naming the exact Java hook, serializer shape, artifact paths, and focused Maven command once a narrow Java fixture exists.

Safe candidates:

- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.
- Add a live direct-packet boundary test or trace for action `0`/`4` before mutation actions if a lower-risk live observation path is preferred.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostArtifactComparisonPreflightService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostArtifactComparisonPreflightServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2181-Completion.md`
- `docs/Phase-6-Session-2181-Handoff.md`
