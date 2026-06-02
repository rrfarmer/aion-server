# Phase 6 Session 2302 Completion - CM_FIND_GROUP Removals

## Scope

Wired live C# `CM_FIND_GROUP` removal actions `1` and `5`.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

Java behavior used:

- Action `1` reads `playerOrTeamId`, `serverId`, `unk1`, `unk2`, and `unk3`, but `runImpl` removes the active player's current-team recruitment or falls back to the active player's object id.
- Removed recruitment rows broadcast `new SM_FIND_GROUP(playerOrTeamId, serverId, unk1, unk2, unk3)` to players whose race matches the removed recruitment.
- Action `5` reads `playerOrTeamId`, but `runImpl` ignores it and removes the active player's application.
- Removed application rows broadcast `new SM_FIND_GROUP(player.getObjectId())` to players whose race matches the removed applicant.
- Missing rows produce no packet side effects.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`

`HandleFindGroupAsync` now permits action `1` and action `5`, accepts planner-composed world broadcast intents, requires a connection registry when broadcasts are present, and broadcasts removal packets with Java-style race filtering.

Added `ProcessPacketAsync_ActionOneAndFiveBroadcastRemovalPacketsToSameRacePlayers`, which:

- seeds a recruitment and application,
- runs live action `1` through `ProcessPacketAsync`,
- verifies the action `1` `SmFindGroup` removal packet fields and same-race world recipients,
- verifies the recruitment row is removed,
- repeats action `1` and verifies the missing row emits no extra broadcast,
- runs live action `5` through `ProcessPacketAsync`,
- verifies the action `5` `SmFindGroup` removal packet id and same-race world recipients,
- verifies the application row is removed,
- repeats action `5` and verifies the missing row emits no extra broadcast.

## Validation Decision

- Changed surface: live find-group boundary dispatch with world broadcast side effects.
- Specific behavior/contract: live C# action `1`/`5` removes existing rows, emits Java-shaped `SmFindGroup` removal packets only to same-race players, and emits no packets for missing rows.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_ActionOneAndFiveBroadcastRemovalPacketsToSameRacePlayers|FullyQualifiedName~CreateDisabledFindGroupBoundaryPlan_ActionOne|FullyQualifiedName~CreateDisabledFindGroupBoundaryPlan_ActionFive" --no-restore
```

Result: passed 5, failed 0, skipped 0. The command built the affected project and dependencies. Pre-existing nullable/analyzer warnings remain.

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

- Broad-validation trigger: live side-effect dispatch expanded to world broadcast removals.
- Broad .NET decision: skipped full project/solution validation after focused live boundary and adjacent planner tests covered the changed dispatch branch. No shared packet primitive, crypto, scheduler, persistence, or broad model change was made.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `1` | `Aion.GameServer.Network.Aion.GameServerConnection` / `SmFindGroup.RemoveRecruitment` | Client Packet Handler / Server Packet | Partial | Focused Boundary Tested | Partial Parity | Live C# action `1` removes the active player's recruitment and broadcasts a removal packet to same-race online players. Verified parity is not claimed; encrypted socket bytes, real-client fanout, and team-id cases remain open. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `5` | `Aion.GameServer.Network.Aion.GameServerConnection` / `SmFindGroup.RemoveApplication` | Client Packet Handler / Server Packet | Partial | Focused Boundary Tested | Partial Parity | Live C# action `5` removes the active player's application and broadcasts a removal packet to same-race online players. Verified parity is not claimed; encrypted socket bytes and real-client fanout remain open. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.removeRecruitment/removeApplication` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.RemoveRecruitment/RemoveApplication` | Service | Partial | Boundary Tested / Java Fixture Tested | Partial Parity | Planner already modeled Java removal rows; this UOW wires live action `1`/`5` dispatch through the connection registry. Missing rows are no-op. Current-team recruitment fallback is represented in service code but not covered by the live boundary test. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `ProcessPacketAsync_ActionOneAndFiveBroadcastRemovalPacketsToSameRacePlayers` | Boundary Runtime | Java source review plus targeted Java find-group fixture run | Live C# action `1` and `5` remove rows, broadcast Java-shaped removal packets to same-race players, and no-op on missing rows. | Focused C# boundary execution and Java source/fixture validation. | Does not compare encrypted socket bytes, real-client socket fanout, or current-team id removal. |

## Remaining Gaps

- No verified parity claim for `CM_FIND_GROUP`.
- No encrypted socket or real-client frame comparison.
- Action `1` current-team id removal needs live boundary coverage.
- Remaining actions `3`, `7`, `8`, `9`, `10`, `11`, `12`, `13`, `15`, and `17` still need concrete live parity evidence or explicit scope decisions.

## Commit

Commit message:

```text
[Phase 6][UOW-2302] Wire find-group removals
```

## Next Recommended UOW

Move to another concrete `CM_FIND_GROUP` branch. A safe next target is update actions `3` and `7`, because Java updates existing recruitment/application rows without direct packet side effects and the C# planner appears to already model missing-row no-op behavior.
