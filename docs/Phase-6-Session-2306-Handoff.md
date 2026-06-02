# Phase 6 Session 2306 Handoff - CM_FIND_GROUP Instance Group Info Update

## Startup Instructions

For the next session, read these documents first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2306-Completion.md`
- `docs/Phase-6-Session-2306-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive; current working state lives in the latest completion and handoff documents.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

UOW-2306 wired live C# `CM_FIND_GROUP` action `15` and action `17`.

Concrete evidence:

- `GameServerConnection.HandleFindGroupAsync` now accepts actions `0` through `10`, plus actions `13`, `15`, and `17`.
- Action `15` sends direct `SmFindGroup` action `16` member info when the requested instance-group row exists.
- Missing action `15` rows emit no packets.
- Action `17` updates the active player's instance-group message and sends direct `SmFindGroup` action `10` when the row exists.
- Missing action `17` rows emit no packets.
- Adjacent disabled planner tests for action `15` and action `17` still pass.

Still not proven:

- Verified parity.
- Encrypted socket or real-client frame comparison.
- Multi-member action `15` data.
- Multi-row Java ordering.
- Action `1` and `3` current-team id behavior through the live boundary.
- Remaining `CM_FIND_GROUP` branches.

## Validation From Last Session

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_ActionFifteenAndSeventeenHandleInstanceGroupInfoAndUpdates" --no-restore
```

Result: passed 1, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Adjacent C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CreateDisabledFindGroupBoundaryPlan_ActionFifteen|FullyQualifiedName~CreateDisabledFindGroupBoundaryPlan_ActionSeventeen" --no-restore
```

Result: passed 3, failed 0, skipped 0.

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

Broad-validation trigger: live find-group dispatch expanded to action `15`/`17`.

Broad .NET decision: full project/solution validation was skipped after focused live boundary plus adjacent planner tests covered the changed branch. No shared packet primitive, crypto, scheduler, persistence, or broad model change was made.

## Next Sequential UOW

Port and validate `CM_FIND_GROUP` action `11`:

- action `11`: send instance-group application to a recruiter

Java behavior to inspect:

- `CM_FIND_GROUP.readImpl/runImpl` action `11`
- `FindGroupService.sendInstanceApplication(Player applicant, int playerOrTeamId)`
- `World.getInstance().getPlayer(playerOrTeamId)`
- `SM_FIND_GROUP(Player instanceApplicant)` action `11`

Expected Java behavior:

- Action `11` resolves `playerOrTeamId` through `World.getInstance().getPlayer`.
- If the target player is online, Java sends `new SM_FIND_GROUP(applicant)` directly to that target player, not the active applicant.
- If the target player is missing/offline, Java emits no packet side effects.

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupRecruitmentPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`

## Focused Validation Recipe For Next UOW

Specific behavior to prove: live C# `CM_FIND_GROUP` action `11` resolves the requested online target, sends Java-equivalent `SmFindGroup` action `11` to that non-active target through the registry, and no-ops when the target is missing.

Expected C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_ActionElevenSendsInstanceApplicationToTarget|FullyQualifiedName~CreateDisabledFindGroupBoundaryPlan_ActionEleven" --no-restore
```

If the adjacent filter is too broad, narrow to the new live test first and document the residual risk.

Expected Java/Maven command:

```powershell
mvn --% -pl game-server -am -DskipTests=false -Dmaven.test.skip=false -Dtest=FindGroupMutationPostTraceCaptureTest -Dsurefire.failIfNoSpecifiedTests=false test
```

If a narrower Java instance-application fixture is found during discovery, use that instead and document the command.

Broad-validation trigger: live find-group dispatch will expand to non-active direct sends through the connection registry. Document the trigger before validation. Start with focused boundary tests and run broader .NET validation only if focused evidence exposes wider risk.

## Safe Candidates

- Add live dispatch for action `11` by allowing non-active direct packet intents only for action `11` and sending them through `IGameClientConnectionRegistry.SendPacketToPlayerAsync`.
- Add focused tests for online target and missing target.
- Leave action `12` for a later unit because it needs invite runtime dispatch review.
- Add target-NPC action `10` mask lookup coverage if world/NPC setup is easy and does not broaden the unit.
- Add current-team id live coverage for action `1` and `3` if team runtime seeding is easy and does not broaden the unit.
