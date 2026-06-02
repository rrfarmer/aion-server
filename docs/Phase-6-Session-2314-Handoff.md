# Phase 6 Session 2314 Handoff - Find Group Joined-Team Dispatch

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2314-Completion.md`
- `docs/Phase-6-Session-2314-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive. Current parity/progress state is in the latest completion and handoff documents.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

Last completed UOW: UOW-2314, live Find Group joined-team cleanup dispatch after accepted group/alliance invite responses.

Completed in UOW-2314:

- `GameServerConnection.HandleGroupInviteQuestionResponseAsync` dispatches `FindGroupJoinedTeamPlan` side effects before group entered-packet fanout.
- `GameServerConnection.HandleAllianceInviteQuestionResponseAsync` dispatches `FindGroupJoinedTeamPlan` side effects before alliance entered-packet fanout.
- Live tests prove application removal broadcasts, solo recruitment removal broadcasts, and team recruitment re-add direct packets on accepted invite responses.

Still not proven:

- Full `FindGroupService.onJoinedTeam` parity outside group/alliance invite acceptance.
- Full `PlayerRestrictions.canInviteToGroup` / `canInviteToAlliance` parity.
- Real-client or encrypted socket bytes for the cleanup packet sequence.
- Full `PortalDialogAI`, `AutoGroupType`, and auto-instance behavior.

## Commits Made

- `925c9ce89 [Phase 6][UOW-2311] Wire portal find-group recruit masks`
- `cf5dcccd9 [Phase 6][UOW-2312] Wire portal recruit option dialog`
- `19ea69bac [Phase 6][UOW-2313] Wire portal instance party match`
- `[Phase 6][UOW-2314] Dispatch find-group joined-team cleanup`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupJoinedTeamLifecycleRecorder.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupRecruitmentPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionGroupInviteTests.cs`
- `docs/Phase-6-Session-2314-Completion.md`
- `docs/Phase-6-Session-2314-Handoff.md`

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.onJoinedTeam` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.OnJoinedTeam` plus `GameServerConnection.DispatchFindGroupJoinedTeamPlansAsync` | Service / Live Dispatch | Partial | Focused Boundary Tested | Partial Parity | C# now mutates shared find-group state and dispatches application/recruitment cleanup packets after accepted group/alliance invite responses. |
| `com.aionemu.gameserver.model.team.group.events.PlayerGroupInvite.acceptRequest` | `Aion.GameServer.Services.PlayerGroupInviteRequestService.HandleResponse` and `GameServerConnection.HandleGroupInviteQuestionResponseAsync` | Request Handler | Partial | Focused Boundary Tested | Partial Parity | Accepted response dispatches Java find-group cleanup side effects before group entered-packet fanout. |
| `com.aionemu.gameserver.model.team.alliance.events.PlayerAllianceInvite.acceptRequest` | `Aion.GameServer.Services.PlayerAllianceInviteRequestService.HandleResponse` and `GameServerConnection.HandleAllianceInviteQuestionResponseAsync` | Request Handler | Partial | Focused Boundary Tested | Partial Parity | Accepted response dispatches Java find-group cleanup side effects before alliance entered-packet fanout. |

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleQuestionResponseAsync_GroupInviteAcceptUsesInjectedFindGroupRecorder|FullyQualifiedName~HandleQuestionResponseAsync_AllianceInviteAcceptUsesInjectedFindGroupRecorder" --no-restore
```

Result: passed 2, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

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

Broad-validation trigger: none.

Broad .NET decision: skipped full project/solution validation. Focused boundary tests covered the changed live response dispatch and supplied the compile signal.

## Next Sequential UOW

Recommended next concrete runtime scope: inspect the next Find Group or portal branch for production behavior still missing from live dispatch.

Start discovery with:

- Java `game-server/data/handlers/ai/portals/PortalDialogAI.java`
- Java `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- C# `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- C# `dotnetConversion/src/Aion.GameServer/Services/FindGroupClientActionPlanService.cs`
- C# `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionClientActionCompositionPlanService.cs`
- Relevant focused boundary tests in `GameServerConnectionFindGroupBoundaryTests.cs`

Do not choose a test-only/evidence-only UOW unless a specific production parity claim is blocked by missing evidence.

## Focused Validation Recipe For Next UOW

Specific behavior to prove: whichever Java-derived live branch is selected during discovery.

Recommended C# command shape:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~<exact edited test name>" --no-restore
```

If the branch touches portal dialog packet shape, add only the directly adjacent packet test:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~<exact boundary test>|FullyQualifiedName~<exact packet test>" --no-restore
```

Expected Java/Maven command:

```powershell
mvn --% -pl game-server -am -DskipTests=false -Dmaven.test.skip=false -Dtest=FindGroupMutationPostTraceCaptureTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Use a narrower Java fixture if discovery finds one for the selected branch. If the next UOW is portal-only and no Java fixture exists, document Java source review and skip Maven only if no targeted Java test applies.

Broad-validation trigger: none expected unless shared packet primitives, broad connection dispatch, persistence, or common runtime state changes.

## Safe Candidates

- Inspect portal dialog fall-through only if a production mismatch is found, not just for coverage.
- Inspect remaining live `CM_FIND_GROUP` branch dispatch for concrete packet side effects not yet wired.
- Inspect group/alliance invite restrictions only if scoped to one Java restriction branch and a focused runtime test.

Avoid:

- More readiness/evidence propagation layers.
- Full .NET project tests or solution builds without a documented broad-validation trigger.
- Updating `docs/PHASE-6-PROGRESS.md`.

