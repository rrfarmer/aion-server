# Phase 6 Session 2301 Completion - CM_FIND_GROUP Show Lists

## Scope

Wired live C# `CM_FIND_GROUP` show-list actions `0` and `4` and added focused boundary evidence for Java-style race filtering.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureTest.java`

Java behavior used:

- Action `0` calls `FindGroupService.showRecruitments(player)`.
- `showRecruitments` filters `recruitments.values()` by `recruitment.getRace() == player.getRace()` and sends `new SM_FIND_GROUP(0, recruitments)` directly to the player.
- Action `4` calls `FindGroupService.showApplications(player)`.
- `showApplications` filters `applications.values()` by applicant race and sends `new SM_FIND_GROUP(4, applications)` directly to the player.
- Both show-list branches use direct sends only.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`

`HandleFindGroupAsync` now permits action `0` and action `4` through the existing guarded live direct-send path, alongside action `2` and action `6`.

Added `ProcessPacketAsync_ActionZeroAndFourSendRaceFilteredShowLists`, which:

- seeds mixed-race recruitment and application rows,
- runs action `0` through live `ProcessPacketAsync` for an Elyos viewer,
- verifies a direct `SmFindGroup` action `0` packet contains only the Elyos recruitment,
- runs action `4` through live `ProcessPacketAsync` for an Asmodian viewer,
- verifies a direct `SmFindGroup` action `4` packet contains only the Asmodian application.

## Validation Decision

- Changed surface: live find-group boundary dispatch for actions `0` and `4`.
- Specific behavior: live C# `CM_FIND_GROUP` action `0`/`4` emits Java-equivalent direct `SmFindGroup` show-list packets with race-filtered entries.
- Broad-validation trigger: live handler dispatch expanded beyond action `2`/`6`.
- Broad .NET decision: skipped full project/solution validation after focused evidence; the full `GameServerConnectionFindGroupBoundaryTests` class covered the changed live find-group boundary plus adjacent action branches.

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionFindGroupBoundaryTests" --no-restore
```

Result: passed 31, failed 0, skipped 0.

Targeted C# precheck:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_ActionZeroAndFourSendRaceFilteredShowLists|FullyQualifiedName~ProcessPacketAsync_ActionTwoAndSixRowsFeedJavaCSharpValueComparison|FullyQualifiedName~ProcessPacketAsync_ActionTwoAndSixCanMaterializeAcceptedMutationPostBoundaryRows" --no-restore
```

Result: passed 3, failed 0, skipped 0 after tightening the reflection helper.

Focused Java/Maven validation:

```powershell
mvn --% -pl game-server -am -DskipTests=false -Dmaven.test.skip=false -Dtest=FindGroupMutationPostTraceCaptureTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Result: build success. `FindGroupMutationPostTraceCaptureTest` ran 32 tests, with 31 passed and 1 skipped.

Repository hygiene:

```powershell
git diff --check
```

Result: passed with line-ending normalization warnings only.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `0` | `Aion.GameServer.Network.Aion.GameServerConnection` / `SmFindGroup.ShowRecruitments` | Client Packet Handler / Server Packet | Partial | Focused Boundary Tested | Partial Parity | Live C# action `0` now emits direct `SmFindGroup` action `0` with race-filtered recruitment entries. Verified parity is not claimed; encrypted socket bytes and Java runtime artifact comparison remain open. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `4` | `Aion.GameServer.Network.Aion.GameServerConnection` / `SmFindGroup.ShowApplications` | Client Packet Handler / Server Packet | Partial | Focused Boundary Tested | Partial Parity | Live C# action `4` now emits direct `SmFindGroup` action `4` with race-filtered application entries. Verified parity is not claimed; encrypted socket bytes and Java runtime artifact comparison remain open. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.showRecruitments/showApplications` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.ShowRecruitments/ShowApplications` | Service | Partial | Boundary Tested / Java Fixture Tested | Partial Parity | C# live boundary now reaches the planner-backed show-list service and verifies race filtering for representative mixed-race rows. Concurrent map ordering remains not asserted beyond singleton visible fixtures. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `ProcessPacketAsync_ActionZeroAndFourSendRaceFilteredShowLists` | Boundary Runtime | Java source review plus targeted Java trace fixture run | Live C# action `0` and `4` direct `SmFindGroup` packets include only same-race visible entries. | Focused C# boundary execution and Java source/fixture validation. | Does not compare encrypted socket bytes or multi-entry ordering. |

## Summary Metrics

- Java artifacts reviewed: 3
- C# artifacts updated: 2
- Focused C# tests passed: 31
- Focused Java tests run: 32 total, 31 passed, 1 skipped
- Verified parity rows added: 0
- Partial parity rows updated: 3
- Estimated Phase 6 completion: unchanged; this UOW adds live show-list behavior for two find-group actions.

## Remaining Gaps

- No verified parity claim for `CM_FIND_GROUP`.
- No encrypted socket or real-client frame comparison.
- Multi-entry Java `ConcurrentHashMap` ordering remains deliberately not asserted.
- Remaining actions `1`, `3`, `5`, `7`, `8`, `9`, `10`, `11`, `12`, `13`, `15`, and `17` still need concrete live parity evidence or explicit scope decisions.

## Commit

Commit message:

```text
[Phase 6][UOW-2301] Wire find-group show lists
```

## Next Recommended UOW

Move to another concrete `CM_FIND_GROUP` branch. A safe next target is removal actions `1` and `5`, because Java removes recruitment/application rows and broadcasts `SM_FIND_GROUP` removal packets to same-race players.
