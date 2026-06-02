# Phase 6 Session 2314 Completion - Find Group Joined-Team Dispatch

## Scope

Wired concrete runtime parity for Java `FindGroupService.onJoinedTeam` packet side effects after accepted group/alliance invite responses.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/model/team/group/PlayerGroupService.java`
- `game-server/src/com/aionemu/gameserver/model/team/group/events/PlayerGroupInvite.java`
- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/PlayerAllianceInvite.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

Java behavior used:

- `PlayerGroupInvite.acceptRequest` and `PlayerAllianceInvite.acceptRequest` mutate team membership after `CM_QUESTION_RESPONSE`.
- Java group/alliance entered events call `FindGroupService.onJoinedTeam` before entered-team packet fanout.
- `FindGroupService.onJoinedTeam` removes posted applications, removes solo recruitments with `unknown3=16`, re-adds the leader's recruitment under the new team when applicable, and removes full-team recruitments when applicable.
- Removed applications/recruitments broadcast `SM_FIND_GROUP` to same-race world players. Re-added recruitments send the posted system message and refreshed recruitment list to the player.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupJoinedTeamLifecycleRecorder.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupRecruitmentPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionGroupInviteTests.cs`

Implemented:

- Accepted group invite responses now dispatch `FindGroupJoinedTeamPlan` side effects before `SendGroupEnteredPlanAsync`.
- Accepted alliance invite responses now dispatch `FindGroupJoinedTeamPlan` side effects before alliance entered-packet fanout.
- Dispatch preserves Java cleanup order: application removal broadcast, solo recruitment removal broadcast, then team recruitment add direct sends or full-team recruitment removal broadcast.
- Live tests now prove posted application cleanup and solo recruitment move-to-team side effects are emitted through the connection registry.

## Validation Decision

- Changed surface: live C# question-response dispatch for accepted group/alliance invites.
- Specific behavior/contract: Java `FindGroupService.onJoinedTeam` packet side effects run after accepted invite membership mutation and before entered-team fanout.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleQuestionResponseAsync_GroupInviteAcceptUsesInjectedFindGroupRecorder|FullyQualifiedName~HandleQuestionResponseAsync_AllianceInviteAcceptUsesInjectedFindGroupRecorder" --no-restore
```

Result: passed 2, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain. This filtered command supplied the compile signal for the affected C# project and tests.

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

- Broad-validation trigger: none. The change is a focused live question-response branch and uses existing packet intent types.
- Broad .NET decision: skipped full project/solution validation. Focused live response tests cover the changed dispatch and supplied the compile signal.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.onJoinedTeam` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.OnJoinedTeam` plus `GameServerConnection.DispatchFindGroupJoinedTeamPlansAsync` | Service / Live Dispatch | Partial | Focused Boundary Tested | Partial Parity | C# now mutates shared find-group state and dispatches application/recruitment cleanup packets after accepted group/alliance invite responses. Instance-group removal has no Java packet side effect; logout/disband callers remain separate. |
| `com.aionemu.gameserver.model.team.group.events.PlayerGroupInvite.acceptRequest` | `Aion.GameServer.Services.PlayerGroupInviteRequestService.HandleResponse` and `GameServerConnection.HandleGroupInviteQuestionResponseAsync` | Request Handler | Partial | Focused Boundary Tested | Partial Parity | Accepted response now dispatches Java find-group cleanup side effects before group entered-packet fanout. Broader `PlayerRestrictions` parity remains partial. |
| `com.aionemu.gameserver.model.team.alliance.events.PlayerAllianceInvite.acceptRequest` | `Aion.GameServer.Services.PlayerAllianceInviteRequestService.HandleResponse` and `GameServerConnection.HandleAllianceInviteQuestionResponseAsync` | Request Handler | Partial | Focused Boundary Tested | Partial Parity | Accepted response now dispatches Java find-group cleanup side effects before alliance entered-packet fanout. Broader alliance restriction/group-merge parity remains partial. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `HandleQuestionResponseAsync_GroupInviteAcceptUsesInjectedFindGroupRecorder` | Boundary Runtime | Java source review of `PlayerGroupInvite.acceptRequest`, `PlayerGroupService.addPlayerToGroup`, and `FindGroupService.onJoinedTeam` | Accepted group invite removes the invited player's application, broadcasts application/recruitment removals, re-adds the inviter's solo recruitment under the new group id, and sends refreshed find-group packets before group entered fanout. | Focused C# boundary execution plus Java source review and targeted Java Maven fixture. | Does not prove full `PlayerRestrictions.canInviteToGroup` parity. |
| `HandleQuestionResponseAsync_AllianceInviteAcceptUsesInjectedFindGroupRecorder` | Boundary Runtime | Java source review of `PlayerAllianceInvite.acceptRequest`, `PlayerAllianceService.addPlayerToAlliance`, and `FindGroupService.onJoinedTeam` | Accepted alliance invite removes the invited player's application, broadcasts application/recruitment removals, re-adds the requester's solo recruitment under the new alliance id, and sends refreshed find-group packets before alliance entered fanout. | Focused C# boundary execution plus Java source review and targeted Java Maven fixture. | Does not prove full `PlayerRestrictions.canInviteToAlliance` parity. |

## Remaining Gaps

- Full `FindGroupService.onJoinedTeam` parity remains partial outside accepted group/alliance invite responses.
- Full group/alliance restriction parity remains partial.
- Real-client or encrypted socket bytes for the joined-team cleanup packets remain unverified.
- Full `PortalDialogAI`, `AutoGroupType`, and auto-instance creation remain partial.

## Commit

Commit message:

```text
[Phase 6][UOW-2314] Dispatch find-group joined-team cleanup
```

## Next Recommended UOW

Move to another concrete gameplay parity gap rather than more evidence plumbing. The next safe candidate is portal dialog fall-through around `PortalDialogAI.onDialogSelect` only if discovery finds a production mismatch; otherwise inspect the next live Find Group branch with packet side effects not yet wired through `GameServerConnection`.

