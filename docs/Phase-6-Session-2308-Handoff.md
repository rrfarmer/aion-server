# Phase 6 Session 2308 Handoff - CM_FIND_GROUP Instance Application Results

## Startup Instructions

For the next session, read these documents first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2308-Completion.md`
- `docs/Phase-6-Session-2308-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive; current working state lives in the latest completion and handoff documents.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

UOW-2308 wired live C# `CM_FIND_GROUP` action `12`.

Concrete evidence:

- `GameServerConnection.HandleFindGroupAsync` now accepts actions `0` through `13`, plus actions `15` and `17`.
- Action `12` decline sends a direct `SmMessage` whisper to the applicant.
- Action `12` accept with `minMembers <= 6` dispatches a party invite request message/question pair and mutates the applicant response requester.
- Action `12` accept with `minMembers > 6` dispatches an alliance invite request message/question pair and mutates the applicant response requester/pending-alliance request.
- Missing applicants and missing instance-group rows remain no-op branches.
- Adjacent action `12` planner/adapter tests still pass.

Still not proven:

- Verified parity.
- Encrypted socket or real-client frame comparison.
- Full `PlayerGroupService`/`PlayerAllianceService` invite response lifecycle parity.
- Action `1` and `3` current-team id behavior through the live boundary.
- Target-NPC action `10` instance-mask lookup behavior.

## Validation From Last Session

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_ActionTwelveHandlesInstanceApplicationResults" --no-restore
```

Result: passed 1, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Adjacent C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ActionTwelve" --no-restore
```

Result: passed 18, failed 0, skipped 0. An earlier parallel invocation hit a compiler file-lock on `Aion.GameServer.dll`; the command passed when rerun serially.

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

Broad-validation trigger: live find-group dispatch expanded to action `12` invite request side effects.

Broad .NET decision: full project/solution validation was skipped after focused live boundary plus adjacent action `12` tests covered the changed branch. No shared packet primitive, crypto, scheduler, persistence, or broad model change was made.

## Next Sequential UOW

Add live coverage for `CM_FIND_GROUP` action `1` and action `3` current-team behavior:

- action `1`: remove recruitment
- action `3`: update recruitment

Java behavior to inspect:

- `CM_FIND_GROUP.readImpl/runImpl` action `1` and action `3`
- `FindGroupService.removeRecruitment(Player, ...)`
- `FindGroupService.updateRecruitment(Player, ...)`
- `Player.getCurrentTeam()`
- `TemporaryPlayerTeam.getObjectId()`

Expected Java behavior:

- Java resolves the recruitment id from the current team when the player is in a team.
- Java falls back to the solo player id when there is no current team.
- Action `1` removes that recruitment id and broadcasts remove packet to same-race online players when a row exists.
- Missing action `1` rows emit no packet side effects.
- Action `3` updates the row message/group type when a row exists and emits no packets.
- Missing action `3` rows emit no packet side effects.

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupRecruitmentPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`

## Focused Validation Recipe For Next UOW

Specific behavior to prove: live C# action `1` and action `3` use the active player's current-team id instead of solo player id when team state is present, and preserve Java no-op behavior for missing rows.

Expected C# command after adding live coverage:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_ActionOneAndThreeUseCurrentTeamId" --no-restore
```

Adjacent C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CreateDisabledFindGroupBoundaryPlan_ActionOne|FullyQualifiedName~CreateDisabledFindGroupBoundaryPlan_ActionThree" --no-restore
```

If the adjacent filter catches unrelated classes or is slow, run the new live test first and document residual risk.

Expected Java/Maven command:

```powershell
mvn --% -pl game-server -am -DskipTests=false -Dmaven.test.skip=false -Dtest=FindGroupMutationPostTraceCaptureTest -Dsurefire.failIfNoSpecifiedTests=false test
```

If a narrower Java current-team fixture is found during discovery, use that instead and document the command.

Broad-validation trigger: none expected if this only adds focused live boundary coverage without changing shared invite services, packet primitives, persistence, crypto, scheduler, or broad dispatch rules.

## Safe Candidates

- Add current-team live coverage for action `1` and `3` if team seeding can be done with existing `Player.CurrentTeamId`/runtime helpers.
- Add target-NPC action `10` mask lookup coverage if world/NPC setup is easy and does not broaden the unit.
- Continue into smaller find-group cleanup only when it changes live parity, not report/readiness scaffolding.
