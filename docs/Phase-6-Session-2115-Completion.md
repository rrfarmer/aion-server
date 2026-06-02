# Phase 6 Session 2115 Completion - FindGroup Show Snapshot Evidence

Date: 2026-06-02
Unit of Work: UOW-2115
Status: Completed

## Scope

- Added focused evidence that FindGroup show-list plans materialize recruitment, application, and instance-group snapshots.
- Covered returned show plans staying stable after later update and logout mutations.
- Kept `GameServerConnection.ProcessPacketAsync` live `CmFindGroup` dispatch deferred.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
  - `showRecruitments` enumerates `recruitments.values().stream().filter(...).toList()`.
  - `showApplications` enumerates `applications.values().stream().filter(...).toList()`.
  - `showInstanceGroups` enumerates `instanceGroups.values().stream().filter(...).toList()`.
  - The backing maps are `ConcurrentHashMap` instances.

## What Changed

- Added `FindGroupRecruitmentPlanServiceTests.ShowPlans_ReturnMaterializedSnapshotsLikeJavaStreamToList`.
- Updated `Phase-6-CmFindGroup-Live-Dispatch-Design.md` to record materialized show-list snapshot evidence and narrow the remaining blocker language.

## Validation

- Changed surface:
  - Test-only service evidence plus design documentation.
- Focused C#:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupRecruitmentPlanServiceTests" --no-restore`
  - Final result: passed, 32 tests.
  - Existing nullable/xUnit warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven:
  - Not run.
  - Rationale: no Java source changed. This UOW reviewed Java `FindGroupService` show-list source and added C# test evidence for materialized snapshot behavior; no focused Java test target was identified.
- Broad .NET suite/build:
  - Intentionally skipped.
  - Broad-validation trigger: none.
  - Rationale: this UOW did not enable live `CmFindGroup` dispatch, live packet sends from the connection boundary, packet primitives, crypto, persistence schema, scheduling, or broad world-state behavior. Filtered tests built the affected project and covered the scoped planner behavior.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.findgroup.FindGroupService.showRecruitments` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.ShowRecruitments` | Service Method | Partial | Unit Tested | Partial Parity | Focused evidence covers race-filtering and materialized snapshot stability after later mutations. Java runtime comparison, weakly consistent concurrent iteration edge cases, and live dispatch remain unverified. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.showApplications` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.ShowApplications` | Service Method | Partial | Unit Tested | Partial Parity | Focused evidence covers race-filtering and materialized snapshot stability after later mutations. Java runtime comparison, weakly consistent concurrent iteration edge cases, and live dispatch remain unverified. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.showInstanceGroups(Player, boolean)` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.ShowInstanceGroups`; `ShowInstanceGroupsForClient` | Service Method | Partial | Unit Tested | Partial Parity | Focused evidence covers materialized action 10 snapshot stability after later mutations. Optional action 26 has separate evidence; Java runtime comparison and live dispatch remain unverified. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupRecruitmentPlanServiceTests.ShowPlans_ReturnMaterializedSnapshotsLikeJavaStreamToList` | Unit | Java `FindGroupService` `values().stream().filter(...).toList()` show paths | Returned recruitment/application/instance-group show plans retain original snapshot data after later updates and logout removal | Focused C# unit test plus reviewed Java source | Does not prove Java weakly consistent concurrent iteration behavior under simultaneous mutation |

## Summary Metrics

- Total Java artifacts reviewed in this UOW: 1.
- Total artifacts ported or represented in this UOW: 3 C# methods.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 3 table rows.
- Total blocked artifacts: 0 new blocked artifacts; live `CM_FIND_GROUP` remains blocked.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- Show-list snapshot materialization has focused evidence, but concurrent iteration under simultaneous mutations remains only partially characterized.
- Java runtime traces, real-client behavior, socket-level order, singleton mutation ordering, cross-caller lifecycle cleanup, and live dispatch remain unverified.
- Broad .NET suite/build was not run because no broad-validation trigger applied.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupRecruitmentPlanServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2115-Completion.md`
- `docs/Phase-6-Session-2115-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: add focused evidence for FindGroup multi-step mutation ordering under singleton lifecycle callers, starting with `onLogout` ordering relative to `ResponseRequester.denyAll()` or joined-team lifecycle cleanup.

Safe alternative candidates:

- Add a focused Java/Maven parity fixture for one executable FindGroup branch if a suitable Java test target can be identified.
- Add a non-live execution-result surface for action `12` live-readiness failure reporting before any `ProcessPacketAsync` wiring.
- Add packet-byte evidence for action `12` declined `SM_MESSAGE` if a Java golden target can be created.
