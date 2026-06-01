# Phase 6 Session 2065 Completion - Find Group Logout Cleanup Planning

Date: 2026-06-01
Unit of Work: UOW-2065
Status: Completed

## Scope

- Inspected Java `FindGroupService.onLogout(Player)`.
- Added disabled C# cleanup planning for find-group logout map removals.
- Preserved Java's key behavior: remove only entries keyed by `player.getObjectId()`, not current-team ids.

## What Changed

- Added `FindGroupRecruitmentPlanService.OnLogout(Player)`.
- Added `FindGroupLogoutCleanupPlan`.
- Added focused tests proving:
  - Solo recruitment, application, and instance-group entries keyed by player object id are removed.
  - Logout cleanup produces no direct packet intents and does not enable live side effects.
  - Team recruitment keyed by team object id is not removed by player logout, matching Java's direct `recruitments.remove(player.getObjectId())`.

## Validation

- Focused C#:
  - `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupRecruitmentPlanServiceTests" --no-restore`
  - Result: passed, 28 tests.
- Focused Java/Maven:
  - `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_FIND_GROUP_ReadPayloadGoldenTest,SM_FIND_GROUP_GoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
  - Result: passed, 25 tests.
- Java `onLogout` has no dedicated Java unit in this repo; parity evidence for logout cleanup is Java source review plus focused C# unit tests.
- Broad .NET suite was intentionally skipped under the focused validation policy:
  - This unit changed disabled find-group planning and tests only.
  - No shared packet primitives, serialization helpers, crypto, persistence, world state, connection dispatch, live side effects, or common runtime infrastructure were changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.findgroup.FindGroupService.onLogout(Player)` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.OnLogout(Player)` | Service Method | Partial | Unit Tested | Partial Parity | Java source reviewed. C# removes recruitment, application, and instance-group entries keyed by player object id and sends no packets. Live singleton/service wiring and concurrency are not implemented. |
| Java `FindGroupService` maps | C# `FindGroupLogoutCleanupPlan` | Plan DTO | Partial | Unit Tested | Partial Parity | Plan records removed states and no direct packet intents. It is disabled evidence only, not live logout dispatch. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupRecruitmentPlanServiceTests.OnLogout_RemovesOnlyPlayerObjectIdEntriesWithoutPackets` | Unit | Java `FindGroupService.onLogout` source review | Removes player-keyed recruitment/application/instance-group state and plans no packets | C# unit assertions against planner state and post-cleanup show lists | Does not prove live logout hook wiring or concurrency |
| `FindGroupRecruitmentPlanServiceTests.OnLogout_DoesNotRemoveTeamRecruitmentKeyedByTeamObjectId` | Unit | Java direct `recruitments.remove(player.getObjectId())` source review | Team-keyed recruitment remains when player logs out | C# unit assertion for remaining team recruitment | Does not prove whether this Java behavior is intended gameplay behavior |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 1.
- Total artifacts ported or represented in this UOW: 2 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts; logout cleanup has source-reviewed unit evidence.
- Total artifacts needing verification or partial parity: 2.
- Total blocked artifacts: 0.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live logout hook wiring is not implemented.
- Live `FindGroupService` singleton runtime and concurrent map behavior remain unverified.
- Java source sends no packets during logout cleanup; C# planner follows that but real-client behavior is not exercised.
- Team-keyed recruitment survival on player logout is preserved from Java but not proven as desired gameplay behavior.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupRecruitmentPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupRecruitmentPlanServiceTests.cs`
- `docs/Phase-6-Session-2065-Completion.md`
- `docs/Phase-6-Session-2065-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: inspect live `CM_FIND_GROUP` handler composition prerequisites and identify the smallest safe runtime dependency boundary, without enabling live dispatch prematurely.

Safe alternative candidates:

- Inspect Java call sites for prepare-window actions `18`-`24` beyond packet serialization.
- Inspect group/alliance ban services separately from `CM_FIND_GROUP` only if a Java caller is identified.
- Add a disabled end-to-end action composition test that includes caller-supplied runtime facts for action `10`/`13`/`26`.
