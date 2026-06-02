# Phase 6 Session 2127 Completion - FindGroup Action 15 Missing Member Info Evidence

Date: 2026-06-02
Unit of Work: UOW-2127
Status: Completed

## Scope

- Exposed instance-group member-info status on the disabled `CM_FIND_GROUP` boundary intent plan.
- Added focused disabled-boundary evidence for action `15` missing instance-group member-info target.
- Covered Java's no-op branch where `FindGroupService.showInstanceGroupMembersInfo` sends no packet when the target instance group is missing.
- Kept `GameServerConnection.ProcessPacketAsync` live `CmFindGroup` dispatch deferred.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - Java `readImpl` action `15` reads `playerOrTeamId` and `instanceMaskId`.
  - Java `runImpl` action `15` calls `FindGroupService.showInstanceGroupMembersInfo(player, playerOrTeamId)`.
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
  - `showInstanceGroupMembersInfo` looks up `instanceGroups.get(playerObjectId)`.
  - If missing, Java returns without packet side effects.
  - If present, Java sends `new SM_FIND_GROUP(16, List.of(instanceGroup))`.

## What Changed

- Added `InstanceGroupMemberInfoStatus` to `FindGroupConnectionBoundarySideEffectIntentPlan`.
- Preserved `FindGroupInstanceGroupPlanStatus` from the member-info planner through the disabled boundary intent surface.
- Added `ExecuteOptInAsync_ComposesParsedActionFifteenMissingInstanceGroupWithoutSideEffects`.
- Added an existing action `15` boundary assertion that the status is `Shown`.
- Updated `Phase-6-CmFindGroup-Live-Dispatch-Design.md` to record the new action `15` status/no-side-effect evidence.

## Validation

- Changed surface:
  - Production evidence/result surface plus focused tests and documentation.
- Focused C#:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests|FullyQualifiedName~FindGroupSideEffectDispatchExecutorServiceTests|FullyQualifiedName~FindGroupClientActionPlanServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests" --no-restore`
  - Final result: passed, 67 tests.
  - Existing nullable/xUnit warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven:
  - Not run.
  - Rationale: no Java source changed. This UOW reviewed Java `CM_FIND_GROUP.runImpl` action `15` and `FindGroupService.showInstanceGroupMembersInfo`; no focused Java test target was identified for this disabled C# boundary status/no-side-effect evidence.
- Broad .NET suite/build:
  - Intentionally skipped.
  - Broad-validation trigger: none.
  - Rationale: this UOW did not enable live `CmFindGroup` dispatch, live packet sends from the connection boundary, packet primitives, crypto, persistence schema, scheduling, or broad world-state behavior. Filtered tests built the affected project and covered the scoped boundary evidence.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP.runImpl` action `15` | `Aion.GameServer.Services.FindGroupConnectionBoundarySideEffectIntentPlan.InstanceGroupMemberInfoStatus`; `FindGroupConnectionBoundarySideEffectCompositionEvidenceService.ExecuteOptInAsync` | Client Action Boundary | Partial | Unit Tested | Partial Parity | Focused evidence preserves `Shown` and `Missing` action `15` member-info statuses at the disabled boundary. Live `GameServerConnection.ProcessPacketAsync` dispatch remains deferred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.showInstanceGroupMembersInfo` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.ShowInstanceGroupMembersInfo`; disabled boundary execution plan | Service Method | Partial | Unit Tested | Partial Parity | Focused evidence covers Java's missing-target no-op branch: no direct packets, no broadcasts, and no executor order entries. Live socket behavior and Java runtime trace remain unverified. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.ExecuteOptInAsync_ComposesParsedActionFifteenMissingInstanceGroupWithoutSideEffects` | Unit | Java `CM_FIND_GROUP.runImpl`; `FindGroupService.showInstanceGroupMembersInfo` | Disabled boundary action `15` missing target records `Missing` and no packet side effects | Focused C# unit test plus reviewed Java source | Does not prove live `ProcessPacketAsync` execution, real socket behavior, Java runtime trace, or packet-byte parity |
| `FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.ExecuteOptInAsync_ComposesParsedActionFifteenPlanAndExecutorResult` | Unit | Java `CM_FIND_GROUP.runImpl`; `FindGroupService.showInstanceGroupMembersInfo` | Existing target records `Shown` and emits action `16` member-info direct packet | Focused C# unit test plus reviewed Java source | Does not prove live `ProcessPacketAsync` execution, real socket order, Java runtime trace, or packet-byte parity |

## Summary Metrics

- Total Java artifacts reviewed in this UOW: 2 classes, 2 methods/branches.
- Total artifacts ported or represented in this UOW: 3 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 2 table rows.
- Total blocked artifacts: 0 new blocked artifacts; live `CM_FIND_GROUP` remains blocked.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- Action `15` boundary status/no-op behavior is now visible through the disabled boundary plan, but live socket behavior remains unverified.
- Java runtime/socket trace and packet-byte comparison were not produced in this UOW.
- Broad .NET suite/build was not run because no broad-validation trigger applied.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionBoundarySideEffectCompositionEvidenceService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2127-Completion.md`
- `docs/Phase-6-Session-2127-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: add packet-byte evidence for action `12` declined `SM_MESSAGE`, or add live-readiness failure-result tests for missing world recipients or skipped invite recipients before any `ProcessPacketAsync` wiring.

Safe alternative candidates:

- Add focused Java/Maven parity fixture coverage for one executable FindGroup branch if a suitable Java test target can be identified.
- Add boundary status evidence for parsed-only action `20`/`25` no-run-impl statuses if the current docs need stronger per-action assertions.
- Review multi-step mutation ordering under concurrent singleton callers before live dispatch.
