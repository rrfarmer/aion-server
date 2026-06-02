# Phase 6 Session 2308 Completion - CM_FIND_GROUP Instance Application Results

## Scope

Wired live C# `CM_FIND_GROUP` action `12`.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- `game-server/src/com/aionemu/gameserver/services/player/PlayerGroupService.java`
- `game-server/src/com/aionemu/gameserver/services/player/PlayerAllianceService.java`

Java behavior used:

- Action `12` reads `playerOrTeamId` as the applicant id and `instanceApplicationReply`.
- `FindGroupService.sendInstanceApplicationResult` resolves the applicant with `World.getInstance().getPlayer(applicantId)`.
- Missing applicants emit no packet side effects.
- Accept reply `1` checks the responder's instance-group row.
- Missing responder instance-group rows emit no invite side effects.
- Accepted rows dispatch `PlayerGroupService.inviteToGroup` when `minMembers <= 6`.
- Accepted rows dispatch `PlayerAllianceService.inviteToAlliance` when `minMembers > 6`.
- Decline reply `0` sends an `SM_MESSAGE` whisper to the applicant with localized message id `1400217`.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`

`HandleFindGroupAsync` now permits action `12` through the live planner-backed boundary.

The live boundary now:

- allows action `12` non-active direct packet intents so decline whispers can reach the applicant,
- executes the already-composed group invite request plan by sending the inviter message and applicant question window,
- executes the already-composed alliance invite request plan by sending requester messages, rejection messages when present, and the question window,
- still returns without side effects for missing applicants or missing instance-group rows.

Added `ProcessPacketAsync_ActionTwelveHandlesInstanceApplicationResults`, which:

- accepts an applicant into a group-sized instance group and verifies party invite message/question dispatch plus request mutation,
- accepts an applicant into an alliance-sized instance group and verifies alliance invite message/question dispatch plus pending-alliance mutation,
- declines an applicant and verifies the applicant receives an `SmMessage` whisper,
- sends action `12` to a missing applicant and verifies no packets or broadcasts.

## Validation Decision

- Changed surface: live find-group boundary dispatch for action `12` group/alliance invite request side effects and decline direct sends.
- Specific behavior/contract: live C# action `12` follows Java's missing-applicant, accept-group, accept-alliance, and decline branches for representative online players.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_ActionTwelveHandlesInstanceApplicationResults" --no-restore
```

Result: passed 1, failed 0, skipped 0. The command built the affected project and dependencies. Pre-existing nullable/analyzer warnings remain.

- Adjacent C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ActionTwelve" --no-restore
```

Result: passed 18, failed 0, skipped 0. An earlier parallel invocation hit a compiler file-lock on `Aion.GameServer.dll`; the same command passed when rerun serially.

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

- Broad-validation trigger: live find-group dispatch expanded to action `12` invite request side effects.
- Broad .NET decision: skipped full project/solution validation after focused live boundary plus adjacent action `12` tests covered the changed branch. No shared packet primitive, crypto, scheduler, persistence, or broad model change was made.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `12` | `Aion.GameServer.Network.Aion.GameServerConnection` / `FindGroupRecruitmentPlanService.SendInstanceApplicationResult` | Client Packet Handler / Service | Partial | Focused Boundary Tested | Partial Parity | Live C# action `12` handles representative missing applicant, accept group, accept alliance, and decline branches. Verified parity is not claimed; encrypted bytes and full Java invite-service edge cases remain open. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.sendInstanceApplicationResult` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.SendInstanceApplicationResult` / `FindGroupInstanceApplicationInviteDispatchPlanService` | Service / Boundary Adapter | Partial | Boundary Tested / Java Fixture Tested | Partial Parity | Java source reviewed; C# now dispatches live group/alliance request side effects for the action `12` boundary. |
| `com.aionemu.gameserver.services.player.PlayerGroupService.inviteToGroup` | `Aion.GameServer.Services.PlayerGroupInviteRequestService.SendInvite` | Service | Partial | Adjacent Boundary Tested | Partial Parity | Used by action `12` for `minMembers <= 6`. Full invite response lifecycle parity remains outside this UOW. |
| `com.aionemu.gameserver.services.player.PlayerAllianceService.inviteToAlliance` | `Aion.GameServer.Services.PlayerAllianceInviteRequestService.SendInvite` | Service | Partial | Adjacent Boundary Tested | Partial Parity | Used by action `12` for `minMembers > 6`. Full alliance invite restriction and response lifecycle parity remains outside this UOW. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `ProcessPacketAsync_ActionTwelveHandlesInstanceApplicationResults` | Boundary Runtime | Java source review plus targeted Java find-group fixture run | Live C# action `12` dispatches group invite, alliance invite, decline whisper, and missing-applicant no-op behavior for representative players. | Focused C# boundary execution and Java source/fixture validation. | Does not compare encrypted socket bytes, all invite-service guard failures, or invite response lifecycle behavior. |

## Remaining Gaps

- No verified parity claim for `CM_FIND_GROUP`.
- No encrypted socket or real-client frame comparison.
- Action `1` and `3` current-team id behavior needs live boundary coverage.
- Target-NPC action `10` instance mask lookup coverage remains open if needed.
- Full group/alliance invite response lifecycle parity remains outside this find-group action `12` unit.

## Commit

Commit message:

```text
[Phase 6][UOW-2308] Wire find-group application results
```

## Next Recommended UOW

Add live coverage for `CM_FIND_GROUP` action `1` and action `3` current-team behavior. Java resolves the recruitment id from `player.getCurrentTeam()` when present, falling back to the solo player id. The C# service currently uses `Player.CurrentTeamId`, so the next unit should seed representative current-team state and prove remove/update behavior through the live boundary.
