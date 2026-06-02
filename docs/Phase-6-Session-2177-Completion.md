# Phase 6 Session 2177 Completion - FindGroup Mutation CSharp Trace Emitter Design

Date: 2026-06-02
Unit of Work: UOW-2177
Status: Completed

## Scope

This unit added a non-live C# trace-emitter design companion for future `CM_FIND_GROUP` action `2` and `6` mutation-post runtime rows.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

This UOW does not enable live `CM_FIND_GROUP` dispatch, does not implement a live trace emitter, does not capture C# runtime rows, and does not compare Java/C# runtime output.

## Changes

- Added `FindGroupMutationPostCSharpTraceEmitterDesignReportService`.
- Added non-live design rows for:
  - artifact shape validation boundary,
  - `GameServerConnection.ProcessPacketAsync` live boundary acceptance,
  - singleton mutation projection,
  - direct packet intent materialization,
  - boundary executor invocation,
  - registry send observation,
  - runtime trace row serialization.
- The report reuses the mutation-post trace schema name `cm-find-group-direct-mutation-post-boundary`.
- All live-relevant rows remain blocked until live boundary capture and live emitter implementation exist.
- Added focused tests for hook coverage, schema field reuse, boundary/executor blockers, mutation/direct-packet ordering requirements, registry-send observation, and serialization blockers.
- Updated live-dispatch design notes to include the C# trace-emitter design.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: focused non-live C# trace-emitter design report service, focused tests, and non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostCSharpTraceEmitterDesignReportServiceTests|FullyQualifiedName~FindGroupDirectPacketMutationPostBoundaryTraceSchemaServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactDirectoryReportServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactFileReportServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactValidatorServiceTests|FullyQualifiedName~FindGroupMutationPostJavaInstrumentationDesignReportServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed, no Java instrumentation/serializer exists, and this design-only C# report depends on already-reviewed Java source and schema contracts.
- Broad-validation trigger: none. No live connection dispatch, live side effects, packet primitive, common runtime base, persistence, shared infrastructure, or broad behavior surface changed.
- Broad .NET decision: skipped intentionally.
- Why this scope is sufficient: the filtered tests cover the new emitter design plus adjacent mutation-post schema, Java artifact reader/file target, validator, and Java instrumentation design dependencies, and the filtered command builds the affected project/dependencies.

Result:

- Passed: 31
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupMutationPostCSharpTraceEmitterDesignReportService` | C# Trace Emitter Design Target | Blocked | Unit Tested | Partial Parity | Action `2`/`6` future C# trace-emitter hook sites are represented, but live boundary capture, live emitter implementation, C# runtime rows, generated Java artifacts, and runtime comparison are missing. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupMutationPostCSharpTraceEmitterDesignReportService`; `Aion.GameServer.Services.FindGroupDirectPacketMutationPostBoundaryTraceSchemaService` | C# Trace Emitter Design / Direct Packet Readiness | Partial | Unit Tested | Partial Parity | The design requires singleton mutation projection, posted-message-before-refreshed-list ordering, and registry send observation matching Java action `2`/`6` behavior. It is design-only and does not prove runtime parity. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupMutationPostCSharpTraceEmitterDesignReportServiceTests.Create_ListsNonLiveEmitterHookSites` | Unit | Java `CM_FIND_GROUP.runImpl`; mutation-post schema source review | Non-live status, hook-site coverage, schema reuse, and blockers. | Focused design-report assertion. | No live emitter. |
| `FindGroupMutationPostCSharpTraceEmitterDesignReportServiceTests.Create_ArtifactBoundaryReusesMutationPostSchemaFields` | Unit | Mutation-post schema target | C# rows must satisfy the same trace fields as future Java artifacts. | Focused design-report assertion. | No runtime rows. |
| `FindGroupMutationPostCSharpTraceEmitterDesignReportServiceTests.Create_BoundaryAndExecutorRowsRemainBlockedUntilLiveCaptureExists` | Unit | Java `AionClientPacket.run`/`CM_FIND_GROUP.runImpl` source review | Live boundary and executor invocation remain blocked until `ProcessPacketAsync` capture exists. | Focused design-report assertion. | Live boundary not wired. |
| `FindGroupMutationPostCSharpTraceEmitterDesignReportServiceTests.Create_MutationAndDirectPacketRowsPreserveJavaActionTwoAndSixOrdering` | Unit | Java `FindGroupService.addRecruitment/addApplication` source review | Singleton mutation and direct-packet rows require mutation-before-posted-message-before-refreshed-list ordering. | Focused design-report assertion. | No registry send observation. |
| `FindGroupMutationPostCSharpTraceEmitterDesignReportServiceTests.Create_RegistryAndSerializationRowsKeepRuntimeComparisonBlocked` | Unit | Java `PacketSendUtility.sendPacket` source review | Registry send observation and runtime row serialization remain blocked until live evidence exists. | Focused design-report assertion. | No Java/C# comparison. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2
- Total artifacts ported in this UOW: 0
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 2
- Total blocked artifacts: 1 live `CM_FIND_GROUP` boundary and 1 Java/C# trace generation/comparison gap
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- The C# trace-emitter design is non-live metadata only; no runtime rows, registry-send observations, encrypted socket capture, or real-client comparison has executed.
- Java instrumentation, Java serializer, and generated Java artifacts remain missing.
- Shape-valid Java artifacts and a C# emitter design would still not prove parity without live C# trace rows and deterministic comparison.
- World-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add comparison-readiness rows that aggregate the mutation-post schema, Java artifact reader, C# trace-emitter design, and runtime comparison blockers for action `2`/`6`.

Safe candidates:

- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.
- Add a live-boundary trace contract extension that names the exact registry observation evidence needed for action `2`/`6`.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostCSharpTraceEmitterDesignReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostCSharpTraceEmitterDesignReportServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2177-Completion.md`
- `docs/Phase-6-Session-2177-Handoff.md`
