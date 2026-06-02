# Phase 6 Session 2304 Completion - CM_FIND_GROUP Instance Group Register Remove

## Scope

Wired live C# `CM_FIND_GROUP` instance-group actions `8` and `9`.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_FIND_GROUP.java`

Java behavior used:

- Action `8` registers/replaces the active player's `ServerWideGroup` and sends `new SM_FIND_GROUP(14, List.of(instanceGroup))` directly to the active player.
- Action `9` removes `instanceGroups.remove(player.getObjectId())`, then always calls `showInstanceGroups(player, true)`.
- `showInstanceGroups(player, true)` sends only `new SM_FIND_GROUP(10, sameRaceInstanceGroups)` directly to the active player.
- Missing action `9` rows still send the refreshed action `10` same-race list.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`

`HandleFindGroupAsync` now permits action `8` and action `9` through the existing planner-backed direct-send boundary.

Added `ProcessPacketAsync_ActionEightAndNineMutateInstanceGroupsWithDirectPackets`, which:

- seeds same-race and other-race instance-group rows,
- runs live action `8` through `ProcessPacketAsync`,
- verifies a direct `SmFindGroup` action `14` registration packet for the active player's row,
- verifies the row was stored,
- runs live action `9` and verifies a direct `SmFindGroup` action `10` refresh containing the remaining same-race row,
- repeats action `9` after the row is missing and verifies Java-style refreshed same-race action `10` output,
- verifies no registry direct sends or world broadcasts were used.

## Validation Decision

- Changed surface: live find-group boundary dispatch for instance-group direct packet branches.
- Specific behavior/contract: live C# action `8`/`9` mutates instance-group rows and emits Java-equivalent direct `SmFindGroup` packets to the active player only.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_ActionEightAndNineMutateInstanceGroupsWithDirectPackets" --no-restore
```

Result: passed 1, failed 0, skipped 0. The command built the affected project and dependencies. Pre-existing nullable/analyzer warnings remain.

- Adjacent C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CreateDisabledFindGroupBoundaryPlan_ActionEight|FullyQualifiedName~CreateDisabledFindGroupBoundaryPlan_ActionNine" --no-restore
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

- Broad-validation trigger: live find-group dispatch expanded to action `8`/`9`.
- Broad .NET decision: skipped full project/solution validation after focused live boundary plus adjacent planner tests covered the changed branch. No shared packet primitive, crypto, scheduler, persistence, or broad model change was made.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `8` | `Aion.GameServer.Network.Aion.GameServerConnection` / `FindGroupRecruitmentPlanService.RegisterInstanceGroup` / `SmFindGroup.RegisterInstanceGroup` | Client Packet Handler / Service / Server Packet | Partial | Focused Boundary Tested | Partial Parity | Live C# action `8` registers the active player's instance group and sends action `14` directly. Verified parity is not claimed; encrypted socket bytes and real-client behavior remain open. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `9` | `Aion.GameServer.Network.Aion.GameServerConnection` / `FindGroupRecruitmentPlanService.RemoveInstanceGroup` / `SmFindGroup.ShowInstanceGroups` | Client Packet Handler / Service / Server Packet | Partial | Focused Boundary Tested | Partial Parity | Live C# action `9` removes the active player's row when present and always sends an action `10` same-race refresh. Verified parity is not claimed; encrypted socket bytes remain open. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.registerInstanceGroup/removeInstanceGroup` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.RegisterInstanceGroup/RemoveInstanceGroup` | Service | Partial | Boundary Tested / Java Fixture Tested | Partial Parity | Existing-row register/remove and missing-row action `9` refresh behavior are covered through the live boundary. Ordering beyond singleton visible fixtures remains unproven. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `ProcessPacketAsync_ActionEightAndNineMutateInstanceGroupsWithDirectPackets` | Boundary Runtime | Java source review plus targeted Java find-group fixture run | Live C# action `8` and `9` mutate instance-group rows and send Java-shaped direct `SmFindGroup` action `14`/`10` packets. | Focused C# boundary execution and Java source/fixture validation. | Does not compare encrypted socket bytes or multi-row Java ordering. |

## Remaining Gaps

- No verified parity claim for `CM_FIND_GROUP`.
- No encrypted socket or real-client frame comparison.
- Action `1` and `3` current-team id behavior needs live boundary coverage.
- Remaining actions `10`, `11`, `12`, `13`, `15`, and `17` still need concrete live parity evidence or explicit scope decisions.

## Commit

Commit message:

```text
[Phase 6][UOW-2304] Wire find-group instance groups
```

## Next Recommended UOW

Move to action `10` and action `13`, the instance-group show-list branches. Action `10` may emit action `26` before action `10` when `FORM_INSTANCE_GROUP_ANYWHERE` is enabled; action `13` is update-only and should emit action `10` only.
