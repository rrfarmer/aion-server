# Phase 6 Session 2349 Completion - Registered Team Disband Checker Lookup

## Scope

Wired Java `EmptyInstanceCheckerTask.isRegisteredTeamDisbanded()` behavior into the C# empty-instance checker scheduling path.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- `game-server/src/com/aionemu/gameserver/world/WorldMapInstance.java`
- `game-server/src/com/aionemu/gameserver/model/team/GeneralTeam.java`

Java behavior used:

- `WorldMapInstance.registerTeam(...)` stores a registered team reference and registers the team id.
- `EmptyInstanceCheckerTask.isRegisteredTeamDisbanded()` returns true only when a registered team exists and `registeredTeam.isDisbanded()`.
- `GeneralTeam.isDisbanded()` is `size() == 0`.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

- Added `InstanceRegisteredTeamDisbandService`.
- Mapped Java retained-team disband semantics to C# runtime state:
  - no `RegisteredTeamId` -> not disbanded;
  - active group/alliance runtime members for the registered id -> not disbanded;
  - retained registered id with no group/alliance runtime members -> disbanded.
- Updated the portal empty-instance scheduler callback to pass the registered-team disband resolver into `InstanceEmptyInstanceCheckerService.Schedule(...)`.
- Added focused resolver tests for no registered team, active group, active alliance, and retained-id-after-runtime-removal cases.

Known limitations:

- C# represents disbanded teams by removed group/alliance runtime rows, not a retained `GeneralTeam` object with zero members.
- The resolver checks shared group and alliance runtimes; league/auto-group-specific edge cases remain unreviewed.
- Other instance creation call sites still need scheduler callback review.

## Validation Decision

- Changed surface: checker callback wiring plus registered-team runtime lookup.
- Specific behavior/contract: Java `registeredTeam != null && registeredTeam.isDisbanded()` maps to a C# instance retaining `RegisteredTeamId` while shared group/alliance runtimes have no members for that id.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~InstanceRegisteredTeamDisbandServiceTests|FullyQualifiedName~InstanceEmptyInstanceCheckerServiceTests|FullyQualifiedName~GameServerConnectionInstanceCooldownTests.QueueAllocatedInstancePortalTransferAsync_AllocatesRegistersSpawnsAndTransfersLikeJavaInstanceService" --no-restore
```

Result: passed 6, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Documentation hygiene:

```powershell
git diff --check
```

Result: passed. Git reported line-ending normalization warnings only.

- Focused Java/Maven command: skipped. No targeted Java unit fixture exists for `EmptyInstanceCheckerTask.isRegisteredTeamDisbanded()`; Java source review was used as source-of-truth evidence.
- Broad-validation trigger: none. The change is isolated to a small resolver and existing scheduler callback.
- Broad .NET decision: skipped full project/solution validation because the filtered command compiled the affected project and covered the resolver/checker/portal callback contracts.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.instance.InstanceService.EmptyInstanceCheckerTask.isRegisteredTeamDisbanded()` | `Aion.GameServer.Services.InstanceRegisteredTeamDisbandService.IsRegisteredTeamDisbanded(...)` | Service Helper | Complete | Unit Tested | Partial Parity | Java source reviewed and active/missing runtime cases tested. C# maps disbanded teams to removed group/alliance rows rather than retained empty `GeneralTeam` objects. |
| `com.aionemu.gameserver.model.team.GeneralTeam.isDisbanded()` | `Aion.GameServer.Services.PlayerGroupRuntime.GetMemberObjectIds(...)` / `PlayerAllianceRuntime.GetMemberObjectIds(...)` | Runtime State | Partial | Unit Tested | Partial Parity | Member-count behavior is used for the registered-team resolver. Full team lifecycle parity remains broader than this UOW. |
| `com.aionemu.gameserver.services.instance.InstanceService.getNextAvailableInstance(..., autoDestroy)` | `Aion.GameServer.Network.Aion.GameServerConnection.QueueAllocatedInstancePortalTransferAsync(...)` | Runtime Caller | Partial | Unit Tested | Partial Parity | Portal checker scheduling now includes registered-team disband lookup. Other creation call sites remain. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `InstanceRegisteredTeamDisbandServiceTests.IsRegisteredTeamDisbanded_MatchesJavaRegisteredTeamIsDisbandedCheck` | Unit | Java source review | No registered team is false; active group/alliance ids are false; retained registered id with no runtime members is true. | Focused C# test plus Java source review. | Does not execute a live scheduled destroy task. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 3
- Total artifacts with verified parity: 0
- Total artifacts needing verification or remaining partial: 3
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains in progress; overall completion unchanged conservatively.

## Remaining Gaps

- Other runtime instance creation paths need real scheduler callback review.
- Live forced-exit packet send and teleport dispatch.
- Dynamic handler/auto-group destroy call sites.
- Instance-scoped walker spawn plan cache parity.

## Commit

Commit message:

```text
[Phase 6][UOW-2349] Add registered team disband checker lookup
```
