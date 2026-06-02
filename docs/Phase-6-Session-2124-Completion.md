# Phase 6 Session 2124 Completion - FindGroup Action 13 No-Mask Update Evidence

Date: 2026-06-02
Unit of Work: UOW-2124
Status: Completed

## Scope

- Added focused disabled-boundary negative evidence for `CM_FIND_GROUP` action `13`.
- Covered Java's update path where `FindGroupService.showInstanceGroups(player, true)` does not emit the optional action `26` instance-mask-list packet.
- Kept `GameServerConnection.ProcessPacketAsync` live `CmFindGroup` dispatch deferred.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - Java `runImpl` action `13` calls `FindGroupService.showInstanceGroups(player, true)`.
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
  - `showInstanceGroups(player, isUpdate)` sends action `26` only when `!isUpdate && GroupConfig.FORM_INSTANCE_GROUP_ANYWHERE`.
  - Java action `13` passes `isUpdate = true`, so the action `26` branch is skipped.
  - Java still sends `new SM_FIND_GROUP(10, instanceGroups)`.

## What Changed

- Extended `FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.ExecuteOptInAsync_ComposesParsedActionThirteenInstanceGroupUpdateWithoutEnableRegisterPacket` to assert:
  - no direct packet intent contains the Java action `26` `instanceMaskIds` source.
  - exactly one execution-order step is recorded.
  - the only direct packet is the action `10` instance-group show-list packet.

## Validation

- Changed surface:
  - Test-only negative ordering evidence plus documentation.
- Focused C#:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests|FullyQualifiedName~FindGroupSideEffectDispatchExecutorServiceTests" --no-restore`
  - Final result: passed, 26 tests.
  - Existing nullable/xUnit warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven:
  - Not run.
  - Rationale: no Java source changed. This UOW reviewed Java `CM_FIND_GROUP.runImpl` action `13` and `FindGroupService.showInstanceGroups`; no focused Java test target was identified for this disabled C# boundary negative evidence.
- Broad .NET suite/build:
  - Intentionally skipped.
  - Broad-validation trigger: none.
  - Rationale: this UOW did not enable live `CmFindGroup` dispatch, live packet sends from the connection boundary, packet primitives, crypto, persistence schema, scheduling, or broad world-state behavior. Filtered tests built the affected project and covered the scoped negative evidence.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP.runImpl` action `13` | `Aion.GameServer.Services.FindGroupConnectionBoundarySideEffectCompositionEvidenceService.ExecuteOptInAsync`; `FindGroupSideEffectDispatchExecutorService` | Client Action Boundary | Partial | Unit Tested | Partial Parity | Focused evidence covers disabled-boundary update behavior: action `13` records only the action `10` instance-group show-list direct packet and no action `26` mask-list packet. Live `GameServerConnection.ProcessPacketAsync` dispatch remains deferred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.showInstanceGroups` update branch | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.ShowInstanceGroupsForClient`; disabled boundary execution plan | Service Method | Partial | Unit Tested | Partial Parity | Focused evidence records that `isUpdate = true` suppresses the form-anywhere action `26` packet. Live socket order and Java runtime trace remain unverified. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.ExecuteOptInAsync_ComposesParsedActionThirteenInstanceGroupUpdateWithoutEnableRegisterPacket` | Unit | Java `CM_FIND_GROUP.runImpl`; `FindGroupService.showInstanceGroups` | Disabled boundary action `13` update emits only action `10` show-list and no action `26` mask-list packet | Focused C# unit test plus reviewed Java source | Does not prove live `ProcessPacketAsync` execution, real socket order, Java runtime trace, or packet-byte parity |

## Summary Metrics

- Total Java artifacts reviewed in this UOW: 2 classes, 2 methods/branches.
- Total artifacts ported or represented in this UOW: 2 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 2 table rows.
- Total blocked artifacts: 0 new blocked artifacts; live `CM_FIND_GROUP` remains blocked.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- Disabled-boundary action `13` no-mask behavior is now covered, but live socket behavior remains unverified.
- Java runtime/socket trace and packet-byte comparison were not produced in this UOW.
- Broad .NET suite/build was not run because no broad-validation trigger applied.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2124-Completion.md`
- `docs/Phase-6-Session-2124-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: add packet-byte evidence for action `12` declined `SM_MESSAGE`, or add live-readiness failure-result tests for missing world recipients or skipped invite recipients before any `ProcessPacketAsync` wiring.

Safe alternative candidates:

- Add focused Java/Maven parity fixture coverage for one executable FindGroup branch if a suitable Java test target can be identified.
- Add disabled-boundary no-side-effect evidence for action `17` missing instance-group update, if not already explicit.
- Review multi-step mutation ordering under concurrent singleton callers before live dispatch.
