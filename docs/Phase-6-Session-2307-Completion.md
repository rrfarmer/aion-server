# Phase 6 Session 2307 Completion - CM_FIND_GROUP Instance Applications

## Scope

Wired live C# `CM_FIND_GROUP` action `11`.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_FIND_GROUP.java`

Java behavior used:

- Action `11` reads `playerOrTeamId` and `instanceMaskId`, then calls `sendInstanceApplication(player, playerOrTeamId)`.
- `sendInstanceApplication` resolves the target with `World.getInstance().getPlayer(playerOrTeamId)`.
- If the target exists, Java sends `new SM_FIND_GROUP(applicant)` directly to that target player.
- If the target is missing/offline, Java emits no packet side effects.
- `SM_FIND_GROUP(Player instanceApplicant)` is action `11` and carries the applicant snapshot.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`

`HandleFindGroupAsync` now permits action `11` through the live planner-backed boundary.

The direct-send guard now allows non-active direct packet intents only for action `11` and only when a connection registry is available. Active-player direct sends still use the active socket; action `11` recruiter sends use `IGameClientConnectionRegistry.SendPacketToPlayerAsync`.

Added `ProcessPacketAsync_ActionElevenSendsInstanceApplicationToTarget`, which:

- creates an active applicant and an online recruiter,
- sends live action `11` from the applicant to the recruiter,
- verifies the recruiter receives exactly one registry direct send,
- verifies the packet is `SmFindGroup` action `11` with the applicant snapshot,
- verifies the applicant socket receives no packet,
- sends action `11` to a missing target and verifies no direct sends, broadcasts, or active-socket packets.

## Validation Decision

- Changed surface: live find-group boundary dispatch now supports a non-active direct packet send through the connection registry.
- Specific behavior/contract: live C# action `11` sends Java-equivalent `SmFindGroup` action `11` to the online recruiter and no-ops for a missing recruiter.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_ActionElevenSendsInstanceApplicationToTarget" --no-restore
```

Result: passed 1, failed 0, skipped 0.

- Adjacent C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CreateDisabledFindGroupBoundaryPlan_ActionEleven" --no-restore
```

Result: passed 2, failed 0, skipped 0.

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

- Broad-validation trigger: live find-group dispatch expanded to a non-active direct registry send.
- Broad .NET decision: skipped full project/solution validation after focused live boundary plus adjacent planner tests covered the changed branch. No shared packet primitive, crypto, scheduler, persistence, or broad model change was made.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `11` | `Aion.GameServer.Network.Aion.GameServerConnection` / `FindGroupRecruitmentPlanService.SendInstanceApplication` / `SmFindGroup.SendInstanceGroupApplicationAsWhisperChatMessage` | Client Packet Handler / Service / Server Packet | Partial | Focused Boundary Tested | Partial Parity | Live C# action `11` sends a registry direct packet to an online non-active target and no-ops when the target is missing. Verified parity is not claimed; encrypted bytes and real client behavior remain open. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.sendInstanceApplication` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.SendInstanceApplication` | Service | Partial | Boundary Tested / Java Fixture Tested | Partial Parity | Online target and missing target behavior are covered through the live boundary and adjacent planner tests. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `ProcessPacketAsync_ActionElevenSendsInstanceApplicationToTarget` | Boundary Runtime | Java source review plus targeted Java find-group fixture run | Live C# action `11` sends action `11` to the online recruiter through the registry and no-ops for a missing target. | Focused C# boundary execution and Java source/fixture validation. | Does not compare encrypted socket bytes or real-client frame handling. |

## Remaining Gaps

- No verified parity claim for `CM_FIND_GROUP`.
- No encrypted socket or real-client frame comparison.
- Action `1` and `3` current-team id behavior needs live boundary coverage.
- Action `12` remains the next concrete unconnected `CM_FIND_GROUP` branch.
- Action `12` invite behavior needs live invite runtime dispatch review.
- Target-NPC action `10` mask lookup coverage remains open if needed.

## Commit

Commit message:

```text
[Phase 6][UOW-2307] Wire find-group instance applications
```

## Next Recommended UOW

Move to action `12`, because it is the next sequential `CM_FIND_GROUP` branch. It accepts or declines an instance-group applicant and needs careful live invite dispatch review before wiring.
