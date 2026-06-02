# Phase 6 Session 2180 Completion - FindGroup Mutation Comparison Key Projection Metadata

Date: 2026-06-02
Unit of Work: UOW-2180
Status: Completed

## Scope

This unit added non-live comparison key-projection metadata for future `CM_FIND_GROUP` action `2` and `6` mutation-post Java/C# trace rows.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

This UOW does not enable live dispatch, does not generate Java artifacts, does not capture live C# runtime rows, and does not execute a runtime comparison.

## Changes

- Added `FindGroupMutationPostComparisonKeyProjectionMetadataService`.
- The metadata separates:
  - schema compatibility gates: `schemaVersion`, `traceName`,
  - row identity keys: `action`, `mutationKind`, `activePlayerObjectId`, `mutatedEntryObjectId`,
  - equality projection fields for mutation state, direct packet shape, registry observation, and side-effect guards,
  - runtime-only ignored fields: `traceSource`, raw `serverEpochSeconds`.
- Preserved Java action-specific comparison facts:
  - action `2`: recruitment mutation, `SmSystemMessage` id `1400392`, refreshed `SmFindGroup` action `0`.
  - action `6`: application mutation, `SmSystemMessage` id `1400393`, refreshed `SmFindGroup` action `4`.
- Kept projection blocked until generated Java trace rows, live C# trace rows, and registry observation exist.
- Added focused tests for blocked/non-live state, action coverage, compatibility gates, row identity, direct packet keys, mutation ordering, registry observation, side-effect guards, and ignored runtime fields.
- Updated live-dispatch design notes to include the comparison key-projection metadata and blockers.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: focused non-live comparison key-projection metadata service, focused tests, and non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostComparisonKeyProjectionMetadataServiceTests|FullyQualifiedName~FindGroupDirectPacketMutationPostBoundaryTraceSchemaServiceTests|FullyQualifiedName~FindGroupMutationPostRegistryObservationTraceContractServiceTests|FullyQualifiedName~FindGroupMutationPostRuntimeComparisonReadinessReportServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed, no Java instrumentation/serializer exists, no generated Java artifacts exist, and no narrow Java fixture exists for this non-live projection metadata UOW.
- Broad-validation trigger: none. No live connection dispatch, live side effects, packet primitive, common runtime base, persistence, shared infrastructure, or broad behavior surface changed.
- Broad .NET decision: skipped intentionally.
- Why this scope is sufficient: the filtered tests cover the new key-projection metadata and the immediate mutation-post schema, registry-observation contract, and runtime-readiness dependencies; the filtered command builds the affected project/dependencies.

Result:

- Passed: 21
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupMutationPostComparisonKeyProjectionMetadataService` | Runtime Comparison Key Projection Metadata | Blocked | Unit Tested | Partial Parity | Action `2`/`6` comparison keys are named, but generated Java rows, live C# rows, and runtime comparison are missing. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupMutationPostComparisonKeyProjectionMetadataService`; `Aion.GameServer.Services.FindGroupDirectPacketMutationPostBoundaryTraceSchemaService`; `Aion.GameServer.Services.FindGroupMutationPostRegistryObservationTraceContractService` | Mutation-Post Comparison Projection | Partial | Unit Tested | Partial Parity | The projection preserves Java mutation-before-posted-message-before-refreshed-list facts and action-specific message/list ids, while ignoring source/runtime-only fields that cannot prove equality. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupMutationPostComparisonKeyProjectionMetadataServiceTests.Create_KeepsProjectionBlockedAndNonLive` | Unit | Java `CM_FIND_GROUP.runImpl`; runtime comparison blockers | Projection metadata remains blocked and non-live. | Focused metadata assertion. | No generated/live rows. |
| `FindGroupMutationPostComparisonKeyProjectionMetadataServiceTests.Create_CoversActionTwoAndSixOnlyWithStableOrdering` | Unit | Java action `2` recruitment and action `6` application branches | Projection covers only action `2` and `6` with stable row order. | Focused action coverage assertion. | No runtime dispatch. |
| `FindGroupMutationPostComparisonKeyProjectionMetadataServiceTests.Create_DefinesCompatibilityGatesAndRowIdentity` | Unit | Mutation-post schema and active player source review | Compatibility gates and row identity keys are separated. | Focused projection assertion. | Future fixtures must use matching player identities. |
| `FindGroupMutationPostComparisonKeyProjectionMetadataServiceTests.Create_PreservesJavaActionSpecificDirectPacketKeys` | Unit | Java `FindGroupService.addRecruitment`; `FindGroupService.addApplication` | Action-specific posted message ids and refreshed list actions are projected. | Focused Java-derived key assertion. | No generated artifacts. |
| `FindGroupMutationPostComparisonKeyProjectionMetadataServiceTests.Create_RequiresMutationOrderingRegistryObservationAndSideEffectGuards` | Unit | Java map mutation before `PacketSendUtility.sendPacket`; no broadcast/invite for actions `2`/`6` | Mutation ordering, registry ordering, zero broadcast, and zero invite fields are required. | Focused key assertion. | No live registry observation. |
| `FindGroupMutationPostComparisonKeyProjectionMetadataServiceTests.Create_ExcludesSourceAndRawClockFromEqualityProjection` | Unit | Trace source/runtime clock metadata | `traceSource` and raw `serverEpochSeconds` are ignored for cross-runtime equality. | Focused projection assertion. | Future same-clock fixtures could add stricter clock validation separately. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2
- Total artifacts ported in this UOW: 0
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 2
- Total blocked artifacts: 1 live `CM_FIND_GROUP` boundary and 1 mutation-post trace comparison gap
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Generated Java artifacts, Java instrumentation, Java serializer, C# live runtime rows, registry-send observation, encrypted socket capture, and deterministic comparison are still missing.
- The comparison key-projection metadata is non-live and cannot prove Java/C# parity.
- World-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add Java artifact comparison preflight metadata tying expected action `2`/`6` Java files, C# live rows, key projection, and blocked comparison execution into one guarded contract.

Safe candidates:

- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.
- Add a live direct-packet boundary test or trace for action `0`/`4` before mutation actions if a lower-risk live observation path is preferred.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostComparisonKeyProjectionMetadataService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostComparisonKeyProjectionMetadataServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2180-Completion.md`
- `docs/Phase-6-Session-2180-Handoff.md`
