# Phase 6 Session 2309 Completion - CM_FIND_GROUP Current-Team Recruitment Ids

## Scope

Added live C# boundary coverage for `CM_FIND_GROUP` action `1` and action `3` current-team recruitment ids.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- `game-server/src/com/aionemu/gameserver/model/team/TemporaryPlayerTeam.java`

Java behavior used:

- Action `1` calls `FindGroupService.removeRecruitment(player, serverId, unk1, unk2, unk3)`.
- Action `3` calls `FindGroupService.updateRecruitment(player, message, groupType)`.
- Both Java branches resolve the recruitment id from `player.getCurrentTeamId()`, falling back to `player.getObjectId()` when the current team id is `0`.
- Action `1` broadcasts the remove packet to same-race online players when the row exists.
- Action `3` updates message/group type/last update with no packet side effects when the row exists.
- Missing rows emit no packet side effects.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

Updated:

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`

Added `ProcessPacketAsync_ActionOneAndThreeUseCurrentTeamId`, which:

- seeds a team recruitment row under a non-solo team id,
- sets the active player `TeamMembership`, `CurrentTeamId`, and team member snapshot,
- sends live action `3` and verifies the team row, not the solo player row, is updated with no packet side effects,
- sends live action `1` and verifies the remove broadcast uses the team id and same-race fanout,
- sends a missing action `3` update after removal and verifies no new packets.

No product code changed in this UOW.

## Validation Decision

- Changed surface: test-only live boundary parity evidence for current-team recruitment ids.
- Specific behavior/contract: live C# action `1` and action `3` use `Player.CurrentTeamId` like Java `player.getCurrentTeamId()` when current-team state is present.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_ActionOneAndThreeUseCurrentTeamId" --no-restore
```

Result: passed 1, failed 0, skipped 0. The command built the affected project and dependencies. Pre-existing nullable/analyzer warnings remain.

- Adjacent C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CreateDisabledFindGroupBoundaryPlan_ActionOne|FullyQualifiedName~CreateDisabledFindGroupBoundaryPlan_ActionThree" --no-restore
```

Result: passed 2, failed 0, skipped 0. An earlier parallel invocation hit a compiler file-lock on `Aion.GameServer.dll`; the same command passed when rerun serially.

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

- Broad-validation trigger: none. This was a test-only parity-evidence unit.
- Broad .NET decision: skipped full project/solution validation. The focused filtered test supplied the compile signal and directly covered the Java-derived behavior.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `1` | `Aion.GameServer.Network.Aion.GameServerConnection` / `FindGroupRecruitmentPlanService.RemoveRecruitment` | Client Packet Handler / Service | Partial | Focused Boundary Tested | Partial Parity | Live C# coverage now proves representative current-team id removal and same-race broadcast behavior. Verified parity is not claimed; encrypted bytes remain open. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `3` | `Aion.GameServer.Network.Aion.GameServerConnection` / `FindGroupRecruitmentPlanService.UpdateRecruitment` | Client Packet Handler / Service | Partial | Focused Boundary Tested | Partial Parity | Live C# coverage now proves representative current-team id update and missing-row no-op behavior. Verified parity is not claimed; encrypted bytes remain open. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.removeRecruitment/updateRecruitment` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.RemoveRecruitment/UpdateRecruitment` | Service | Partial | Boundary Tested / Java Fixture Tested | Partial Parity | Java current-team id fallback behavior has representative live C# boundary evidence. Full Java `TemporaryPlayerTeam` object parity remains outside this UOW. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `ProcessPacketAsync_ActionOneAndThreeUseCurrentTeamId` | Boundary Runtime | Java source review plus targeted Java find-group fixture run | Live C# action `3` updates a team recruitment by team id; action `1` removes and broadcasts that team id; missing action `3` no-ops. | Focused C# boundary execution and Java source/fixture validation. | Does not compare encrypted socket bytes or full `TemporaryPlayerTeam` model behavior. |

## Remaining Gaps

- No verified parity claim for `CM_FIND_GROUP`.
- No encrypted socket or real-client frame comparison.
- Target-NPC action `10` instance-mask lookup coverage remains open.
- Full `TemporaryPlayerTeam`, group, alliance, and invite response lifecycle parity remains outside this find-group unit.

## Commit

Commit message:

```text
[Phase 6][UOW-2309] Cover find-group current-team ids
```

## Next Recommended UOW

Add live coverage for `CM_FIND_GROUP` action `10` target-NPC instance-mask lookup. Java `showInstanceGroups(player, false)` checks whether `player.getTarget()` is an `Npc`, resolves recruitable instance mask ids from `DataManager.AUTO_GROUP`, and prepends action `26` mask rows before action `10` when the global anywhere option is disabled and a target NPC supplies masks.
