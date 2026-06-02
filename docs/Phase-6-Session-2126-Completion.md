# Phase 6 Session 2126 Completion - FindGroup Action 9 Missing Remove Evidence

Date: 2026-06-02
Unit of Work: UOW-2126
Status: Completed

## Scope

- Added focused disabled-boundary evidence for action `9` missing instance-group removal.
- Covered Java's behavior where `FindGroupService.removeInstanceGroup` always calls `showInstanceGroups(player, true)` after removal, even when no entry existed.
- Kept `GameServerConnection.ProcessPacketAsync` live `CmFindGroup` dispatch deferred.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - Java `readImpl` action `9` reads `playerOrTeamId` and `instanceMaskId`.
  - Java `runImpl` action `9` calls `FindGroupService.removeInstanceGroup(player)`.
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
  - `removeInstanceGroup` calls `instanceGroups.remove(player.getObjectId())`.
  - It then calls `showInstanceGroups(player, true)` unconditionally.
  - The update show path sends action `10` and does not emit action `26`.

## What Changed

- Added an existing action `9` boundary assertion that the surfaced instance-group mutation status is `Removed`.
- Added `ExecuteOptInAsync_ComposesParsedActionNineMissingInstanceGroupAsUpdatedShowList`.
- The missing action `9` test asserts:
  - status `Missing`,
  - no world broadcast intents,
  - one action `10` direct packet to the requester,
  - one executor order entry,
  - remaining same-race instance-group state is preserved.
- Updated `Phase-6-CmFindGroup-Live-Dispatch-Design.md` to record action `9` missing-remove evidence.

## Validation

- Changed surface:
  - Test-only boundary status evidence plus documentation.
- Focused C#:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests|FullyQualifiedName~FindGroupSideEffectDispatchExecutorServiceTests|FullyQualifiedName~FindGroupClientActionPlanServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests" --no-restore`
  - Final result: passed, 66 tests.
  - Existing nullable/xUnit warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven:
  - Not run.
  - Rationale: no Java source changed. This UOW reviewed Java `CM_FIND_GROUP.runImpl` action `9` and `FindGroupService.removeInstanceGroup`; no focused Java test target was identified for this disabled C# boundary status evidence.
- Broad .NET suite/build:
  - Intentionally skipped.
  - Broad-validation trigger: none.
  - Rationale: this UOW did not enable live `CmFindGroup` dispatch, live packet sends from the connection boundary, packet primitives, crypto, persistence schema, scheduling, or broad world-state behavior. Filtered tests built the affected project and covered the scoped boundary evidence.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP.runImpl` action `9` | `Aion.GameServer.Services.FindGroupConnectionBoundarySideEffectIntentPlan.InstanceGroupStatus`; `FindGroupConnectionBoundarySideEffectCompositionEvidenceService.ExecuteOptInAsync` | Client Action Boundary | Partial | Unit Tested | Partial Parity | Focused evidence preserves `Removed` and `Missing` action `9` mutation statuses at the disabled boundary. Live `GameServerConnection.ProcessPacketAsync` dispatch remains deferred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.removeInstanceGroup` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.RemoveInstanceGroup`; disabled boundary execution plan | Service Method | Partial | Unit Tested | Partial Parity | Focused evidence covers Java's unconditional refreshed show-list behavior after remove, including when no instance group existed. Live socket behavior and Java runtime trace remain unverified. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.ExecuteOptInAsync_ComposesParsedActionNineMissingInstanceGroupAsUpdatedShowList` | Unit | Java `CM_FIND_GROUP.runImpl`; `FindGroupService.removeInstanceGroup`; `FindGroupService.showInstanceGroups` | Disabled boundary action `9` missing removal records `Missing` and still sends the action `10` refreshed list | Focused C# unit test plus reviewed Java source | Does not prove live `ProcessPacketAsync` execution, real socket behavior, Java runtime trace, or packet-byte parity |
| `FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.ExecuteOptInAsync_ComposesParsedActionNineRemoveInstanceGroupAsUpdatedShowList` | Unit | Java `CM_FIND_GROUP.runImpl`; `FindGroupService.removeInstanceGroup`; `FindGroupService.showInstanceGroups` | Existing instance-group removal records `Removed` and sends action `10` refreshed list | Focused C# unit test plus reviewed Java source | Does not prove live `ProcessPacketAsync` execution, real socket order, Java runtime trace, or packet-byte parity |

## Summary Metrics

- Total Java artifacts reviewed in this UOW: 2 classes, 2 methods/branches.
- Total artifacts ported or represented in this UOW: 2 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 2 table rows.
- Total blocked artifacts: 0 new blocked artifacts; live `CM_FIND_GROUP` remains blocked.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- Action `9` boundary status/refresh behavior is now visible through the disabled boundary plan, but live socket behavior remains unverified.
- Java runtime/socket trace and packet-byte comparison were not produced in this UOW.
- Broad .NET suite/build was not run because no broad-validation trigger applied.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2126-Completion.md`
- `docs/Phase-6-Session-2126-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: add packet-byte evidence for action `12` declined `SM_MESSAGE`, or add live-readiness failure-result tests for missing world recipients or skipped invite recipients before any `ProcessPacketAsync` wiring.

Safe alternative candidates:

- Add focused Java/Maven parity fixture coverage for one executable FindGroup branch if a suitable Java test target can be identified.
- Add disabled-boundary status evidence for action `15` missing member-info no-side-effect behavior if not already explicit.
- Review multi-step mutation ordering under concurrent singleton callers before live dispatch.
