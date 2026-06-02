# Phase 6 Session 2302 Handoff - CM_FIND_GROUP Removals

## Startup Instructions

For the next session, read these documents first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2302-Completion.md`
- `docs/Phase-6-Session-2302-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive; current working state lives in the latest completion and handoff documents.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

UOW-2302 wired live C# `CM_FIND_GROUP` removal actions `1` and `5`.

Concrete evidence:

- `GameServerConnection.HandleFindGroupAsync` now accepts actions `0`, `1`, `2`, `4`, `5`, and `6`.
- Action `1` removes an existing recruitment row and broadcasts `SmFindGroup` action `1` to same-race players through `IGameClientConnectionRegistry.BroadcastToWorldAsync`.
- Action `5` removes an existing application row and broadcasts `SmFindGroup` action `5` to same-race players.
- Missing action `1`/`5` rows emit no extra broadcast.
- Existing disabled planner tests for action `1` and action `5` still pass.

Still not proven:

- Verified parity.
- Encrypted socket or real-client frame comparison.
- Action `1` current-team id removal through the live boundary.
- Remaining `CM_FIND_GROUP` branches.

## Validation From Last Session

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_ActionOneAndFiveBroadcastRemovalPacketsToSameRacePlayers|FullyQualifiedName~CreateDisabledFindGroupBoundaryPlan_ActionOne|FullyQualifiedName~CreateDisabledFindGroupBoundaryPlan_ActionFive" --no-restore
```

Result: passed 5, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

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

Broad-validation trigger: live side-effect dispatch expanded to action `1`/`5` world broadcasts.

Broad .NET decision: full project/solution validation was skipped after focused live boundary plus adjacent planner tests covered the changed branch. No shared packet primitive, crypto, scheduler, persistence, or broad model change was made.

## Next Sequential UOW

Port and validate `CM_FIND_GROUP` update actions:

- action `3`: update recruitment
- action `7`: update application

Java behavior to inspect:

- `CM_FIND_GROUP.readImpl/runImpl` action `3` and action `7`
- `FindGroupService.updateRecruitment(Player, String, int)`
- `FindGroupService.updateApplication(Player, String, int)`

Expected Java behavior:

- Action `3` resolves the active player's current-team recruitment id or active player id, updates the existing recruitment message/group type, and emits no packet side effects when the row exists or is missing.
- Action `7` updates the active player's existing application message/group type/class/level and emits no packet side effects when the row exists or is missing.

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupRecruitmentPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`

## Focused Validation Recipe For Next UOW

Specific behavior to prove: live C# `CM_FIND_GROUP` action `3` and action `7` update existing find-group rows, preserve Java missing-row no-op behavior, and emit no direct or broadcast packets.

Expected C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_ActionThreeAndSevenUpdateRowsWithoutPacketSideEffects|FullyQualifiedName~CreateDisabledFindGroupBoundaryPlan_ActionThree|FullyQualifiedName~CreateDisabledFindGroupBoundaryPlan_ActionSeven" --no-restore
```

If the named live test does not exist yet, add it and run the same filter. Keep the filter focused on the new live update test plus adjacent disabled action `3`/`7` planner tests.

Expected Java/Maven command:

```powershell
mvn --% -pl game-server -am -DskipTests=false -Dmaven.test.skip=false -Dtest=FindGroupMutationPostTraceCaptureTest -Dsurefire.failIfNoSpecifiedTests=false test
```

If a narrower Java update fixture is found during discovery, use that instead and document the command.

Broad-validation trigger: live find-group dispatch will expand beyond actions `0`/`1`/`2`/`4`/`5`/`6`; document the trigger before validation. Start with focused boundary tests and run broader .NET validation only if focused evidence exposes wider risk.

## Safe Candidates

- Add live dispatch for action `3` and action `7` under the existing guarded boundary path.
- Add focused tests for existing-row update and missing-row no-op.
- Add action `1` current-team id live boundary coverage if team runtime state is easy to seed without broad setup.
- Keep verified parity unclaimed until Java/C# runtime or golden evidence covers the full branch behavior.
