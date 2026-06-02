# Phase 6 Session 2174 Completion - FindGroup Mutation Java Instrumentation Design

Date: 2026-06-02
Unit of Work: UOW-2174
Status: Completed

## Scope

This unit added a non-live Java instrumentation design/runbook target for future `CM_FIND_GROUP` mutation-post trace emission for actions `2` and `6`.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

Java behavior:

- Action `2` parses `playerOrTeamId`, `message`, and `groupType`, calls `FindGroupService.addRecruitment(player, message, groupType)`, mutates the recruitment map, sends posted system message id `1400392`, then refreshes recruitments with `SM_FIND_GROUP` action `0`.
- Action `6` parses `playerOrTeamId`, `message`, `groupType`, `classId`, and `level`, calls `FindGroupService.addApplication(player, message, groupType, classId, level)`, mutates the application map, sends posted system message id `1400393`, then refreshes applications with `SM_FIND_GROUP` action `4`.

This UOW does not modify Java source, does not add Java instrumentation, does not create a Java trace serializer, does not generate Java trace artifacts, does not capture live C# runtime traces, does not enable live `CM_FIND_GROUP` dispatch, and does not claim runtime/socket parity.

## Changes

- Added `FindGroupMutationPostJavaInstrumentationDesignReportService`.
- Added ordered design point records for:
  - `CM_FIND_GROUP.readImpl` payload capture,
  - `CM_FIND_GROUP.runImpl` active-player boundary capture,
  - `FindGroupService.addRecruitment` mutation after `recruitments.put`,
  - recruitment posted system message send observation,
  - recruitment refreshed action `0` list send observation,
  - `FindGroupService.addApplication` mutation after `applications.put`,
  - application posted system message send observation,
  - application refreshed action `4` list send observation,
  - future artifact row serialization and validation.
- Added caveats to prevent instrumentation from changing Java `ConcurrentHashMap` timing, `PacketSendUtility` ordering, list materialization timing, packet-path latency, or timestamp parity semantics.
- Added focused tests for hook coverage, action `2`/`6` ordering, schema/validator reuse, and caveats.
- Updated live-dispatch design notes to record the instrumentation design while keeping Java instrumentation and runtime comparison blocked.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: focused non-live Java instrumentation design report service, focused tests, and non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostJavaInstrumentationDesignReportServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactValidatorServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactSchemaReportServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed, no Java instrumentation exists, and no narrow executable Java fixture or generated Java trace artifact exists for this design-only unit.
- Broad-validation trigger: none. No live connection dispatch, live side effects, packet primitive, common runtime base, persistence, shared infrastructure, or broad behavior surface changed.
- Broad .NET decision: skipped intentionally. The filtered `dotnet test` command builds the affected project and dependencies while exercising the new report and adjacent schema/validator surfaces.
- Why this scope is sufficient: the new artifact is non-live metadata derived from reviewed Java source and existing mutation-post trace schema/validator contracts; focused tests verify the required hook locations, ordering constraints, blockers, and caveats.

Result:

- Passed: 16
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupMutationPostJavaInstrumentationDesignReportService` | Java Instrumentation Design Target | Blocked | Unit Tested | Partial Parity | Action `2`/`6` payload and runImpl hook locations are documented for future Java trace emission, but no Java instrumentation, generated Java artifact, live C# trace capture, or live `ProcessPacketAsync` execution has occurred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupMutationPostJavaInstrumentationDesignReportService`; `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactValidatorService`; `Aion.GameServer.Services.FindGroupDirectPacketMutationPostBoundaryTraceSchemaService` | Java Instrumentation Design / Direct Packet Readiness | Partial | Unit Tested | Partial Parity | The report documents mutation-after-map-put, posted-message, and refreshed-list observation points for recruitment/application flows while preserving Java send ordering. It is design-only and does not prove runtime parity. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupMutationPostJavaInstrumentationDesignReportServiceTests.Create_ListsNonLiveJavaInstrumentationDesignPoints` | Unit | Java `CM_FIND_GROUP.runImpl`; `FindGroupService.addRecruitment/addApplication` source review | Non-live status, action coverage, ordering flags, validator reuse, and blocked runtime comparison. | Focused design-report assertion. | No Java hooks implemented. |
| `FindGroupMutationPostJavaInstrumentationDesignReportServiceTests.Create_DocumentsClientPacketPayloadAndRunImplHookSources` | Unit | Java `CM_FIND_GROUP.readImpl/runImpl` source review | Payload and active-player capture points for actions `2` and `6`. | Focused design-report assertion. | No packet runtime capture. |
| `FindGroupMutationPostJavaInstrumentationDesignReportServiceTests.Create_RecordsRecruitmentMutationBeforePostedMessageBeforeRefreshedList` | Unit | Java `FindGroupService.addRecruitment/showRecruitments` source review | Action `2` mutation-after-put, posted message id `1400392`, refreshed action `0`, and race-filtered `toList()` snapshot point. | Focused design-report assertion. | No generated Java trace row. |
| `FindGroupMutationPostJavaInstrumentationDesignReportServiceTests.Create_RecordsApplicationMutationBeforePostedMessageBeforeRefreshedList` | Unit | Java `FindGroupService.addApplication/showApplications` source review | Action `6` mutation-after-put, posted message id `1400393`, refreshed action `4`, and race-filtered `toList()` snapshot point. | Focused design-report assertion. | No generated Java trace row. |
| `FindGroupMutationPostJavaInstrumentationDesignReportServiceTests.Create_TraceSerializerRowReusesSchemaAndValidator` | Unit | Existing mutation-post schema/validator contracts | Future serializer row uses the mutation-post trace name, field order, and validator target. | Focused design-report assertion. | No Java serializer exists. |
| `FindGroupMutationPostJavaInstrumentationDesignReportServiceTests.Create_CaveatsProtectJavaTimingAndNonParityFields` | Unit | Java `ConcurrentHashMap`, `PacketSendUtility`, and list materialization source review | Instrumentation caveats prevent synchronization, blocking IO, send-order changes, early materialization, and timestamp parity claims. | Focused design-report assertion. | Caveats do not enforce Java runtime behavior. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2
- Total artifacts ported in this UOW: 0
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 2
- Total blocked artifacts: 1 live `CM_FIND_GROUP` boundary and 1 Java trace instrumentation/serializer/artifact generation/comparison gap
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- The Java instrumentation design is non-live contract metadata; no Java hooks, serializer, artifact generation, C# live trace row, encrypted socket capture, or real-client runtime comparison has executed.
- Future Java instrumentation could still perturb map timing, packet-path latency, send ordering, or snapshot materialization if it ignores the design caveats.
- World-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add a generated-artifact file/directory report service for action `2`/`6` Java mutation-post trace artifacts once artifact paths and naming are chosen, keeping it blocked until real Java artifacts exist.

Safe candidates:

- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.
- Add a C# live trace-emitter design companion for action `2`/`6` mutation-post rows, still blocked until live boundary capture exists.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostJavaInstrumentationDesignReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostJavaInstrumentationDesignReportServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2174-Completion.md`
- `docs/Phase-6-Session-2174-Handoff.md`
