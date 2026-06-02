# Phase 6 Session 2125 Completion - FindGroup Action 17 Missing Update Evidence

Date: 2026-06-02
Unit of Work: UOW-2125
Status: Completed

## Scope

- Exposed instance-group mutation status on the disabled `CM_FIND_GROUP` boundary intent plan.
- Added focused disabled-boundary evidence for action `17` missing instance-group update.
- Covered Java's no-op branch where `FindGroupService.updateInstanceGroup` sends no packets when the responder has no registered instance group.
- Kept `GameServerConnection.ProcessPacketAsync` live `CmFindGroup` dispatch deferred.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - Java `readImpl` action `17` reads `playerOrTeamId`, `instanceMaskId`, and `message`.
  - Java `runImpl` action `17` calls `FindGroupService.updateInstanceGroup(player, message)`.
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
  - `updateInstanceGroup` looks up `instanceGroups.get(player.getObjectId())`.
  - If missing, Java returns without mutation and without packet side effects.
  - If present, Java updates message/timestamp and calls `showInstanceGroups(player, true)`.

## What Changed

- Added `InstanceGroupStatus` to `FindGroupConnectionBoundarySideEffectIntentPlan`.
- Preserved `FindGroupInstanceGroupPlanStatus` from the client action planner through the disabled boundary intent surface.
- Added `ExecuteOptInAsync_ComposesParsedActionSeventeenMissingInstanceGroupWithoutSideEffects`.
- Added an existing action `17` boundary assertion that the status is `Updated`.
- Updated `Phase-6-CmFindGroup-Live-Dispatch-Design.md` to record the new action `17` status/no-side-effect evidence.

## Validation

- Changed surface:
  - Production evidence/result surface plus focused tests and documentation.
- Focused C#:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests|FullyQualifiedName~FindGroupSideEffectDispatchExecutorServiceTests|FullyQualifiedName~FindGroupClientActionPlanServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests" --no-restore`
  - Final result: passed, 65 tests.
  - Existing nullable/xUnit warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven:
  - Not run.
  - Rationale: no Java source changed. This UOW reviewed Java `CM_FIND_GROUP.runImpl` action `17` and `FindGroupService.updateInstanceGroup`; no focused Java test target was identified for this disabled C# boundary status/no-side-effect evidence.
- Broad .NET suite/build:
  - Intentionally skipped.
  - Broad-validation trigger: none.
  - Rationale: this UOW did not enable live `CmFindGroup` dispatch, live packet sends from the connection boundary, packet primitives, crypto, persistence schema, scheduling, or broad world-state behavior. Filtered tests built the affected project and covered the scoped boundary evidence.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP.runImpl` action `17` | `Aion.GameServer.Services.FindGroupConnectionBoundarySideEffectIntentPlan.InstanceGroupStatus`; `FindGroupConnectionBoundarySideEffectCompositionEvidenceService.ExecuteOptInAsync` | Client Action Boundary | Partial | Unit Tested | Partial Parity | Focused evidence preserves `Updated` and `Missing` action `17` mutation statuses at the disabled boundary. Live `GameServerConnection.ProcessPacketAsync` dispatch remains deferred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.updateInstanceGroup` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.UpdateInstanceGroup`; disabled boundary execution plan | Service Method | Partial | Unit Tested | Partial Parity | Focused evidence covers Java's missing-instance-group no-op branch: no direct packets, no broadcasts, and no executor order entries. Live socket behavior and Java runtime trace remain unverified. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.ExecuteOptInAsync_ComposesParsedActionSeventeenMissingInstanceGroupWithoutSideEffects` | Unit | Java `CM_FIND_GROUP.runImpl`; `FindGroupService.updateInstanceGroup` | Disabled boundary action `17` missing instance group records `Missing` and no packet side effects | Focused C# unit test plus reviewed Java source | Does not prove live `ProcessPacketAsync` execution, real socket behavior, Java runtime trace, or packet-byte parity |
| `FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.ExecuteOptInAsync_ComposesParsedActionSeventeenUpdateInstanceGroupAsUpdatedShowList` | Unit | Java `CM_FIND_GROUP.runImpl`; `FindGroupService.updateInstanceGroup`; `FindGroupService.showInstanceGroups` | Existing instance group update records `Updated` and emits action `10` show-list direct packet | Focused C# unit test plus reviewed Java source | Does not prove live `ProcessPacketAsync` execution, real socket order, Java runtime trace, or packet-byte parity |

## Summary Metrics

- Total Java artifacts reviewed in this UOW: 2 classes, 2 methods/branches.
- Total artifacts ported or represented in this UOW: 3 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 2 table rows.
- Total blocked artifacts: 0 new blocked artifacts; live `CM_FIND_GROUP` remains blocked.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- Action `17` boundary status/no-op behavior is now visible through the disabled boundary plan, but live socket behavior remains unverified.
- Java runtime/socket trace and packet-byte comparison were not produced in this UOW.
- Broad .NET suite/build was not run because no broad-validation trigger applied.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionBoundarySideEffectCompositionEvidenceService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2125-Completion.md`
- `docs/Phase-6-Session-2125-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: add packet-byte evidence for action `12` declined `SM_MESSAGE`, or add live-readiness failure-result tests for missing world recipients or skipped invite recipients before any `ProcessPacketAsync` wiring.

Safe alternative candidates:

- Add focused Java/Maven parity fixture coverage for one executable FindGroup branch if a suitable Java test target can be identified.
- Add disabled-boundary status evidence for action `9` missing remove-instance-group no-side-effect/update behavior if not already explicit.
- Review multi-step mutation ordering under concurrent singleton callers before live dispatch.
