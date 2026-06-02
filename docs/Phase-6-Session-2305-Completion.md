# Phase 6 Session 2305 Completion - CM_FIND_GROUP Instance Group Show Lists

## Scope

Wired live C# `CM_FIND_GROUP` instance-group show-list actions `10` and `13`.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_FIND_GROUP.java`

Java behavior used:

- Action `10` calls `FindGroupService.showInstanceGroups(player, false)`.
- When `GroupConfig.FORM_INSTANCE_GROUP_ANYWHERE` is enabled, action `10` sends `new SM_FIND_GROUP(instanceMaskIds)` action `26` before the action `10` show-list packet.
- Action `10` always sends `new SM_FIND_GROUP(10, sameRaceInstanceGroups)` directly to the active player.
- Action `13` calls `FindGroupService.showInstanceGroups(player, true)` and sends only the action `10` show-list packet.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`

`HandleFindGroupAsync` now permits action `10` and action `13` through the existing planner-backed direct-send boundary.

Added `ProcessPacketAsync_ActionTenAndThirteenSendInstanceGroupShowLists`, which:

- enables C# `FormInstanceGroupAnywhere`,
- seeds recruitable auto-group masks,
- seeds same-race and other-race instance-group rows,
- runs live action `10` through `ProcessPacketAsync`,
- verifies direct send order: `SmFindGroup` action `26` mask list, then `SmFindGroup` action `10` same-race instance-group list,
- runs live action `13` through `ProcessPacketAsync`,
- verifies action `13` sends only `SmFindGroup` action `10` and excludes other-race rows,
- verifies no registry direct sends or world broadcasts were used.

## Validation Decision

- Changed surface: live find-group boundary dispatch for instance-group show-list direct packet branches.
- Specific behavior/contract: live C# action `10`/`13` emits Java-equivalent direct `SmFindGroup` show-list packets, including optional action `26` before action `10` only for non-update action `10`.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_ActionTenAndThirteenSendInstanceGroupShowLists" --no-restore
```

Result: passed 1, failed 0, skipped 0. The command built the affected project and dependencies. Pre-existing nullable/analyzer warnings remain.

- Adjacent C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CreateDisabledFindGroupBoundaryPlan_ActionTen|FullyQualifiedName~CreateDisabledFindGroupBoundaryPlan_ActionThirteen" --no-restore
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

- Broad-validation trigger: live find-group dispatch expanded to action `10`/`13`.
- Broad .NET decision: skipped full project/solution validation after focused live boundary plus adjacent planner tests covered the changed branch. No shared packet primitive, crypto, scheduler, persistence, or broad model change was made.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `10` | `Aion.GameServer.Network.Aion.GameServerConnection` / `FindGroupRecruitmentPlanService.ShowInstanceGroupsForClient` / `SmFindGroup` action `26` and `10` | Client Packet Handler / Service / Server Packet | Partial | Focused Boundary Tested | Partial Parity | Live C# action `10` sends optional action `26` before action `10` when anywhere registration is enabled. Verified parity is not claimed; target-NPC mask selection and encrypted bytes remain open. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `13` | `Aion.GameServer.Network.Aion.GameServerConnection` / `FindGroupRecruitmentPlanService.ShowInstanceGroupsForClient` / `SmFindGroup.ShowInstanceGroups` | Client Packet Handler / Service / Server Packet | Partial | Focused Boundary Tested | Partial Parity | Live C# action `13` sends only action `10` same-race instance-group list. Verified parity is not claimed; encrypted bytes and multi-row ordering remain open. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.showInstanceGroups` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.ShowInstanceGroupsForClient` | Service | Partial | Boundary Tested / Java Fixture Tested | Partial Parity | Action `10`/`13` live boundary covers anywhere mask list and same-race filtering for representative rows. Java `DataManager.AUTO_GROUP` target-NPC lookup is modeled but not live-tested here. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `ProcessPacketAsync_ActionTenAndThirteenSendInstanceGroupShowLists` | Boundary Runtime | Java source review plus targeted Java find-group fixture run | Live C# action `10` sends action `26` then `10`, and action `13` sends only action `10`, with same-race filtering. | Focused C# boundary execution and Java source/fixture validation. | Does not compare encrypted socket bytes, target-NPC mask lookup, or multi-row Java ordering. |

## Remaining Gaps

- No verified parity claim for `CM_FIND_GROUP`.
- No encrypted socket or real-client frame comparison.
- Action `1` and `3` current-team id behavior needs live boundary coverage.
- Remaining actions `11`, `12`, `15`, and `17` still need concrete live parity evidence or explicit scope decisions.
- Action `11` direct packet targets a non-active player and will need registry direct-send wiring rather than the current active-player-only direct-send guard.
- Action `12` invite behavior needs live invite runtime dispatch review.

## Commit

Commit message:

```text
[Phase 6][UOW-2305] Wire find-group instance show lists
```

## Next Recommended UOW

Move to action `15` and action `17`, because both stay close to the current active-player direct-send/no-op boundary: action `15` sends member info when the requested instance group exists, and action `17` updates the active player's instance group and sends the action `10` update list when the row exists.
