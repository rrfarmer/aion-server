# Phase 6 Session 2179 Completion - FindGroup Mutation Registry Observation Trace Contract

Date: 2026-06-02
Unit of Work: UOW-2179
Status: Completed

## Scope

This unit added a non-live live-boundary registry-observation trace contract for future `CM_FIND_GROUP` action `2` and `6` mutation-post runtime traces.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

This UOW does not enable live dispatch, does not observe registry sends, does not generate Java artifacts, does not capture live C# runtime rows, and does not execute a runtime comparison.

## Changes

- Added `FindGroupMutationPostRegistryObservationTraceContractService`.
- The contract records the exact evidence required before live direct-packet dispatch can be considered for mutation-post action `2` and `6`:
  - boundary executor invoked from the live `CmFindGroup` boundary,
  - accepted boundary trace for the action under test,
  - direct registry send #1 to the active player with the Java posted system message,
  - direct registry send #2 to the active player with the refreshed `SM_FIND_GROUP` list,
  - registry-send ordering observed as posted system message before refreshed list,
  - zero world broadcasts,
  - zero invite dispatches,
  - schema-v1 runtime trace fields.
- Preserved Java action-specific packet evidence:
  - action `2`: `SmSystemMessage` id `1400392`, then `SmFindGroup` action `0`.
  - action `6`: `SmSystemMessage` id `1400393`, then `SmFindGroup` action `4`.
- Added focused tests for blocked/non-live state, action coverage, registry send evidence, boundary/ordering/side-effect evidence, and runtime schema metadata.
- Updated live-dispatch design notes to include the registry-observation trace contract and its live blockers.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: focused non-live registry-observation trace contract service, focused tests, and non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostRegistryObservationTraceContractServiceTests|FullyQualifiedName~FindGroupDirectPacketMutationPostLiveBoundaryTraceScaffoldServiceTests|FullyQualifiedName~FindGroupDirectPacketMutationPostBoundaryTraceSchemaServiceTests|FullyQualifiedName~FindGroupMutationPostCSharpTraceEmitterDesignReportServiceTests|FullyQualifiedName~FindGroupMutationPostRuntimeComparisonReadinessReportServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed, no Java instrumentation/serializer exists, no generated Java artifacts exist, and no narrow Java fixture exists for this non-live contract metadata UOW.
- Broad-validation trigger: none. No live connection dispatch, live side effects, packet primitive, common runtime base, persistence, shared infrastructure, or broad behavior surface changed.
- Broad .NET decision: skipped intentionally.
- Why this scope is sufficient: the filtered tests cover the new registry-observation contract and the immediate mutation-post scaffold, trace schema, C# emitter design, and runtime-readiness dependencies; the filtered command builds the affected project/dependencies.

Result:

- Passed: 24
- Failed: 0
- Skipped: 0

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupMutationPostRegistryObservationTraceContractService` | Live Boundary Registry Observation Contract | Blocked | Unit Tested | Partial Parity | Action `2`/`6` registry observation evidence is named, but no live boundary invocation or connection-registry sends have been observed. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupMutationPostRegistryObservationTraceContractService`; `Aion.GameServer.Services.FindGroupDirectPacketMutationPostBoundaryTraceSchemaService`; `Aion.GameServer.Services.FindGroupMutationPostRuntimeComparisonReadinessReportService` | Registry Observation / Direct Packet Readiness | Partial | Unit Tested | Partial Parity | The contract preserves Java mutation-before-posted-message-before-refreshed-list ordering and action-specific message/list ids. It does not prove runtime parity. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupMutationPostRegistryObservationTraceContractServiceTests.Create_KeepsRegistryObservationContractBlockedAndNonLive` | Unit | Java `CM_FIND_GROUP.runImpl`; live-boundary blockers | Contract remains blocked and non-live. | Focused contract assertion. | No live trace row. |
| `FindGroupMutationPostRegistryObservationTraceContractServiceTests.Create_CoversActionTwoAndSixOnly` | Unit | Java action `2` recruitment and action `6` application branches | Contract scopes mutation-post registry observation to action `2` and `6`. | Focused action coverage assertion. | No runtime dispatch. |
| `FindGroupMutationPostRegistryObservationTraceContractServiceTests.Create_RequiresPostedMessageThenRefreshedListRegistryObservationPerAction` | Unit | Java `FindGroupService.addRecruitment`; `FindGroupService.addApplication` | Posted system message and refreshed list sends are required per action with Java ids/actions. | Focused registry evidence assertion. | No connection-registry observation. |
| `FindGroupMutationPostRegistryObservationTraceContractServiceTests.Create_RequiresExecutorRegistryOrderingAndNoUnexpectedSideEffects` | Unit | Java direct `PacketSendUtility.sendPacket` calls | Boundary executor, send ordering, zero broadcast, and zero invite evidence remain required. | Focused blocker assertion. | No live side-effect counters. |
| `FindGroupMutationPostRegistryObservationTraceContractServiceTests.Create_RuntimeTraceFieldsAreOnlyNonLiveSchemaMetadata` | Unit | Mutation-post trace schema | Runtime fields are recorded as non-live schema metadata only. | Focused metadata assertion. | No generated Java/C# runtime rows. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2
- Total artifacts ported in this UOW: 0
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 2
- Total blocked artifacts: 1 live `CM_FIND_GROUP` boundary and 1 registry-observation/runtime comparison gap
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Registry send observation, generated Java artifacts, Java instrumentation, Java serializer, C# live runtime rows, encrypted socket capture, and deterministic comparison are still missing.
- The registry-observation trace contract is non-live metadata and cannot prove Java/C# parity.
- World-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add comparison key-projection metadata for mutation-post action `2`/`6` rows after actual Java/C# trace row shapes exist.

Safe candidates:

- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.
- Add a live direct-packet boundary test or trace for action `0`/`4` before mutation actions if a lower-risk live observation path is preferred.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostRegistryObservationTraceContractService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostRegistryObservationTraceContractServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2179-Completion.md`
- `docs/Phase-6-Session-2179-Handoff.md`
