# Phase 6 Session 2123 Completion - FindGroup Action 10 Mask-List Ordering Evidence

Date: 2026-06-02
Unit of Work: UOW-2123
Status: Completed

## Scope

- Added focused disabled-boundary execution-order assertions for `CM_FIND_GROUP` action `10` when form-instance-group-anywhere is enabled.
- Covered Java's action `26` instance-mask-list packet before action `10` instance-group show-list packet.
- Kept `GameServerConnection.ProcessPacketAsync` live `CmFindGroup` dispatch deferred.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - Java `runImpl` action `10` calls `FindGroupService.showInstanceGroups(player, false)`.
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
  - `showInstanceGroups(player, false)` filters same-race instance groups.
  - When `GroupConfig.FORM_INSTANCE_GROUP_ANYWHERE` is enabled, Java sends `new SM_FIND_GROUP(instanceMaskIds)` first.
  - Java then sends `new SM_FIND_GROUP(10, instanceGroups)`.
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_FIND_GROUP.java`
  - The `List<Integer>` constructor sets action `26`.
  - Action `10` writes instance-group show-list rows.

## What Changed

- Extended `FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.ExecuteOptInAsync_ComposesParsedActionTenInstanceGroupShowWithEnableRegisterPacket` to assert execution sequence:
  - sequence `1`: `SmFindGroup` action `26` instance-mask-list direct packet.
  - sequence `2`: `SmFindGroup` action `10` instance-group show-list direct packet.
- Updated `Phase-6-CmFindGroup-Live-Dispatch-Design.md` to record disabled-boundary action `10` multi-direct ordering evidence.

## Validation

- Changed surface:
  - Test-only ordering evidence plus design documentation.
- Focused C#:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests|FullyQualifiedName~FindGroupSideEffectDispatchExecutorServiceTests" --no-restore`
  - Final result: passed, 26 tests.
  - Existing nullable/xUnit warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven:
  - Not run.
  - Rationale: no Java source changed. This UOW reviewed Java `CM_FIND_GROUP.runImpl`, `FindGroupService.showInstanceGroups`, and `SM_FIND_GROUP`; no focused Java test target was identified for this disabled C# boundary ordering evidence.
- Broad .NET suite/build:
  - Intentionally skipped.
  - Broad-validation trigger: none.
  - Rationale: this UOW did not enable live `CmFindGroup` dispatch, live packet sends from the connection boundary, packet primitives, crypto, persistence schema, scheduling, or broad world-state behavior. Filtered tests built the affected project and covered the scoped ordering evidence.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP.runImpl` action `10` | `Aion.GameServer.Services.FindGroupConnectionBoundarySideEffectCompositionEvidenceService.ExecuteOptInAsync`; `FindGroupSideEffectDispatchExecutorService` | Client Action Boundary | Partial | Unit Tested | Partial Parity | Focused evidence covers disabled-boundary direct-packet execution order for action `26` mask list before action `10` instance-group show list. Live `GameServerConnection.ProcessPacketAsync` dispatch remains deferred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.showInstanceGroups` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.ShowInstanceGroupsForClient`; disabled boundary execution plan | Service Method | Partial | Unit Tested | Partial Parity | Focused evidence records `SM_FIND_GROUP(instanceMaskIds)` before `SM_FIND_GROUP(10, instanceGroups)` when form-anywhere is enabled and the request is not an update. Live socket order and Java runtime trace remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_FIND_GROUP` actions `26` and `10` | `Aion.GameServer.Network.Aion.ServerPackets.SmFindGroup.EnableRegisterForInstances`; `SmFindGroup.ShowInstanceGroups` | Server Packet | Partial | Unit Tested | Partial Parity | Packet objects are staged in Java order at the disabled boundary; this UOW did not add Java-generated packet-byte evidence. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.ExecuteOptInAsync_ComposesParsedActionTenInstanceGroupShowWithEnableRegisterPacket` | Unit | Java `CM_FIND_GROUP.runImpl`; `FindGroupService.showInstanceGroups`; `SM_FIND_GROUP` | Disabled boundary execution order sends action `26` mask-list packet before action `10` instance-group show-list packet | Focused C# unit test plus reviewed Java source | Does not prove live `ProcessPacketAsync` execution, real socket order, Java runtime trace, or packet-byte parity |

## Summary Metrics

- Total Java artifacts reviewed in this UOW: 3 classes, 3 methods/branches.
- Total artifacts ported or represented in this UOW: 3 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 3 table rows.
- Total blocked artifacts: 0 new blocked artifacts; live `CM_FIND_GROUP` remains blocked.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- Disabled-boundary action `10` direct-packet order is now covered, but live socket ordering relative to the triggering client packet remains unverified.
- Java runtime/socket trace and packet-byte comparison were not produced in this UOW.
- Broad .NET suite/build was not run because no broad-validation trigger applied.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2123-Completion.md`
- `docs/Phase-6-Session-2123-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: add packet-byte evidence for action `12` declined `SM_MESSAGE`, or add live-readiness failure-result tests for missing world recipients or skipped invite recipients before any `ProcessPacketAsync` wiring.

Safe alternative candidates:

- Add focused Java/Maven parity fixture coverage for one executable FindGroup branch if a suitable Java test target can be identified.
- Add disabled-boundary evidence for action `13` update behavior not emitting action `26`.
- Review multi-step mutation ordering under concurrent singleton callers before live dispatch.
