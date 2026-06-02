# Phase 6 Session 2303 Handoff - CM_FIND_GROUP Updates

## Startup Instructions

For the next session, read these documents first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2303-Completion.md`
- `docs/Phase-6-Session-2303-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive; current working state lives in the latest completion and handoff documents.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

UOW-2303 wired live C# `CM_FIND_GROUP` update actions `3` and `7`.

Concrete evidence:

- `GameServerConnection.HandleFindGroupAsync` now accepts actions `0`, `1`, `2`, `3`, `4`, `5`, `6`, and `7`.
- Action `3` updates an existing recruitment row and emits no direct or broadcast packets.
- Action `7` updates an existing application row and emits no direct or broadcast packets.
- Missing action `3`/`7` rows emit no packets and do not create rows.

Still not proven:

- Verified parity.
- Encrypted socket or real-client frame comparison.
- Action `1` and `3` current-team id behavior through the live boundary.
- Remaining `CM_FIND_GROUP` branches.

## Validation From Last Session

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_ActionThreeAndSevenUpdateRowsWithoutPacketSideEffects" --no-restore
```

Result: passed 1, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

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

Broad-validation trigger: live find-group dispatch expanded to action `3`/`7`.

Broad .NET decision: full project/solution validation was skipped after the focused live boundary test covered the changed branch. No shared packet primitive, crypto, scheduler, persistence, or broad model change was made.

## Next Sequential UOW

Port and validate `CM_FIND_GROUP` instance-group register/remove actions:

- action `8`: register instance group
- action `9`: remove instance group

Java behavior to inspect:

- `CM_FIND_GROUP.readImpl/runImpl` action `8` and action `9`
- `FindGroupService.registerInstanceGroup(Player, int, String, int)`
- `FindGroupService.removeInstanceGroup(Player)`
- `SM_FIND_GROUP` constructors for action `14` and action `10`

Expected Java behavior:

- Action `8` registers or replaces the active player's instance-group row and sends a direct `SM_FIND_GROUP` register packet to the active player.
- Action `9` removes the active player's instance-group row and sends a direct `SM_FIND_GROUP` show-list packet to the active player with remaining same-race rows.
- Missing action `9` rows still send the refreshed same-race show-list packet based on the current Java/C# planner behavior; verify this against Java before wiring.

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupRecruitmentPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`

## Focused Validation Recipe For Next UOW

Specific behavior to prove: live C# `CM_FIND_GROUP` action `8` and action `9` mutate instance-group rows and emit the Java-equivalent direct `SmFindGroup` packets to the active player only.

Expected C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_ActionEightAndNineMutateInstanceGroupsWithDirectPackets|FullyQualifiedName~CreateDisabledFindGroupBoundaryPlan_ActionEight|FullyQualifiedName~CreateDisabledFindGroupBoundaryPlan_ActionNine" --no-restore
```

If substring matching catches action `17`, narrow to the new live test name first and document that narrower command.

Expected Java/Maven command:

```powershell
mvn --% -pl game-server -am -DskipTests=false -Dmaven.test.skip=false -Dtest=FindGroupMutationPostTraceCaptureTest -Dsurefire.failIfNoSpecifiedTests=false test
```

If a narrower Java instance-group fixture is found during discovery, use that instead and document the command.

Broad-validation trigger: live find-group dispatch will expand beyond actions `0` through `7`; document the trigger before validation. Start with focused boundary tests and run broader .NET validation only if focused evidence exposes wider risk.

## Safe Candidates

- Add live dispatch for action `8` and action `9` under the existing guarded boundary path.
- Add focused tests for register, remove, same-race show-list refresh, and missing removal behavior after Java verification.
- Add current-team id live coverage for action `1` and `3` if team runtime seeding is easy and does not broaden the unit.
- Keep verified parity unclaimed until Java/C# runtime or golden evidence covers the full branch behavior.
