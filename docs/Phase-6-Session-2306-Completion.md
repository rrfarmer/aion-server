# Phase 6 Session 2306 Completion - CM_FIND_GROUP Instance Group Info Update

## Scope

Wired live C# `CM_FIND_GROUP` instance-group actions `15` and `17`.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_FIND_GROUP.java`

Java behavior used:

- Action `15` calls `showInstanceGroupMembersInfo(player, playerOrTeamId)` and sends `new SM_FIND_GROUP(16, List.of(instanceGroup))` directly to the active player only when the requested instance group exists.
- Missing action `15` rows emit no packets.
- Action `17` calls `updateInstanceGroup(player, message)`, updates the active player's instance-group message, refreshes last update, and calls `showInstanceGroups(player, true)` only when the active player's row exists.
- Missing action `17` rows emit no packets.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`

`HandleFindGroupAsync` now permits action `15` and action `17` through the existing planner-backed direct-send boundary.

Added `ProcessPacketAsync_ActionFifteenAndSeventeenHandleInstanceGroupInfoAndUpdates`, which:

- seeds an instance-group row,
- runs live action `15` and verifies direct `SmFindGroup` action `16` member-info output,
- runs missing action `15` and verifies no packets,
- runs live action `17` and verifies direct `SmFindGroup` action `10` update-list output plus stored message mutation,
- removes the row, repeats action `17`, and verifies no packets,
- verifies no registry direct sends or world broadcasts were used.

## Validation Decision

- Changed surface: live find-group boundary dispatch for active-player instance-group direct/no-op branches.
- Specific behavior/contract: live C# action `15`/`17` emits Java-equivalent direct `SmFindGroup` packets for existing rows and no-ops for missing rows.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_ActionFifteenAndSeventeenHandleInstanceGroupInfoAndUpdates" --no-restore
```

Result: passed 1, failed 0, skipped 0. The command built the affected project and dependencies. Pre-existing nullable/analyzer warnings remain.

- Adjacent C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CreateDisabledFindGroupBoundaryPlan_ActionFifteen|FullyQualifiedName~CreateDisabledFindGroupBoundaryPlan_ActionSeventeen" --no-restore
```

Result: passed 3, failed 0, skipped 0.

- Focused Java/Maven command:

```powershell
mvn --% -pl game-server -am -DskipTests=false -Dmaven.test.skip=false -Dtest=FindGroupMutationPostTraceCaptureTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Result: build success. `FindGroupMutationPostTraceCaptureTest` ran 32 tests, with 31 passed and 1 skipped.

- Repository hygiene:

```powershell
git diff --check
```

Result: passed with line-ending normalization warnings only.

- Broad-validation trigger: live find-group dispatch expanded to action `15`/`17`.
- Broad .NET decision: skipped full project/solution validation after focused live boundary plus adjacent planner tests covered the changed branch. No shared packet primitive, crypto, scheduler, persistence, or broad model change was made.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `15` | `Aion.GameServer.Network.Aion.GameServerConnection` / `FindGroupRecruitmentPlanService.ShowInstanceGroupMembersInfo` / `SmFindGroup.ShowInstanceGroupMemberInfo` | Client Packet Handler / Service / Server Packet | Partial | Focused Boundary Tested | Partial Parity | Live C# action `15` sends action `16` member info when the requested row exists and no-ops when missing. Verified parity is not claimed; encrypted bytes and multi-member team data remain open. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `17` | `Aion.GameServer.Network.Aion.GameServerConnection` / `FindGroupRecruitmentPlanService.UpdateInstanceGroup` / `SmFindGroup.ShowInstanceGroups` | Client Packet Handler / Service / Server Packet | Partial | Focused Boundary Tested | Partial Parity | Live C# action `17` updates the active player's row and sends action `10` only when the row exists. Verified parity is not claimed; encrypted bytes and multi-row ordering remain open. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.showInstanceGroupMembersInfo/updateInstanceGroup` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.ShowInstanceGroupMembersInfo/UpdateInstanceGroup` | Service | Partial | Boundary Tested / Java Fixture Tested | Partial Parity | Existing-row and missing-row behavior are covered through the live boundary for representative singleton rows. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `ProcessPacketAsync_ActionFifteenAndSeventeenHandleInstanceGroupInfoAndUpdates` | Boundary Runtime | Java source review plus targeted Java find-group fixture run | Live C# action `15` sends action `16` for existing rows and no-ops for missing rows; action `17` updates and sends action `10` for existing rows and no-ops for missing rows. | Focused C# boundary execution and Java source/fixture validation. | Does not compare encrypted socket bytes, multi-member rows, or multi-row Java ordering. |

## Remaining Gaps

- No verified parity claim for `CM_FIND_GROUP`.
- No encrypted socket or real-client frame comparison.
- Action `1` and `3` current-team id behavior needs live boundary coverage.
- Remaining actions `11` and `12` still need concrete live parity evidence or explicit scope decisions.
- Action `11` direct packet targets a non-active player and needs registry direct-send wiring.
- Action `12` invite behavior needs live invite runtime dispatch review.

## Commit

Commit message:

```text
[Phase 6][UOW-2306] Wire find-group instance info
```

## Next Recommended UOW

Move to action `11`, because it is the next concrete `CM_FIND_GROUP` branch. It sends `SM_FIND_GROUP` action `11` to a non-active online player resolved by `playerOrTeamId`, so the live boundary should use registry direct sends instead of the current active-player-only `SendPacketAsync` path.
