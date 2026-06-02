# Phase 6 Session 2304 Handoff - CM_FIND_GROUP Instance Group Register Remove

## Startup Instructions

For the next session, read these documents first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2304-Completion.md`
- `docs/Phase-6-Session-2304-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive; current working state lives in the latest completion and handoff documents.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

UOW-2304 wired live C# `CM_FIND_GROUP` instance-group actions `8` and `9`.

Concrete evidence:

- `GameServerConnection.HandleFindGroupAsync` now accepts actions `0` through `9`.
- Action `8` registers an instance-group row and sends direct `SmFindGroup` action `14`.
- Action `9` removes the active player's instance-group row when present and sends direct `SmFindGroup` action `10` with remaining same-race rows.
- Missing action `9` rows still send the refreshed action `10` list, matching Java `removeInstanceGroup -> showInstanceGroups(player, true)`.
- Adjacent disabled planner tests for action `8` and action `9` still pass.

Still not proven:

- Verified parity.
- Encrypted socket or real-client frame comparison.
- Multi-row Java ordering.
- Action `1` and `3` current-team id behavior through the live boundary.
- Remaining `CM_FIND_GROUP` branches.

## Validation From Last Session

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_ActionEightAndNineMutateInstanceGroupsWithDirectPackets" --no-restore
```

Result: passed 1, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Adjacent C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CreateDisabledFindGroupBoundaryPlan_ActionEight|FullyQualifiedName~CreateDisabledFindGroupBoundaryPlan_ActionNine" --no-restore
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

Broad-validation trigger: live find-group dispatch expanded to action `8`/`9`.

Broad .NET decision: full project/solution validation was skipped after focused live boundary plus adjacent planner tests covered the changed branch. No shared packet primitive, crypto, scheduler, persistence, or broad model change was made.

## Next Sequential UOW

Port and validate `CM_FIND_GROUP` instance-group show-list actions:

- action `10`: show instance groups
- action `13`: update instance groups

Java behavior to inspect:

- `CM_FIND_GROUP.readImpl/runImpl` action `10` and action `13`
- `FindGroupService.showInstanceGroups(Player, boolean)`
- `SM_FIND_GROUP` constructors/write branches for action `26` and action `10`
- `GroupConfig.FORM_INSTANCE_GROUP_ANYWHERE` and `DataManager.AUTO_GROUP.getRecruitableInstanceMaskIds`

Expected Java behavior:

- Action `10` calls `showInstanceGroups(player, false)`.
- When `FORM_INSTANCE_GROUP_ANYWHERE` is enabled, action `10` sends action `26` mask ids before action `10`.
- Action `10` always sends action `10` same-race instance-group list.
- Action `13` calls `showInstanceGroups(player, true)` and sends only action `10`.

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupRecruitmentPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`

## Focused Validation Recipe For Next UOW

Specific behavior to prove: live C# `CM_FIND_GROUP` action `10` and action `13` emit Java-equivalent direct `SmFindGroup` show-list packets, including optional action `26` before action `10` only for action `10` when anywhere registration is enabled.

Expected C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_ActionTenAndThirteenSendInstanceGroupShowLists|FullyQualifiedName~CreateDisabledFindGroupBoundaryPlan_ActionTen|FullyQualifiedName~CreateDisabledFindGroupBoundaryPlan_ActionThirteen" --no-restore
```

If the adjacent filter is too broad or collides with unrelated names, narrow to the new live test first and document the residual risk.

Expected Java/Maven command:

```powershell
mvn --% -pl game-server -am -DskipTests=false -Dmaven.test.skip=false -Dtest=FindGroupMutationPostTraceCaptureTest -Dsurefire.failIfNoSpecifiedTests=false test
```

If a narrower Java instance-group show-list fixture is found during discovery, use that instead and document the command.

Broad-validation trigger: live find-group dispatch will expand beyond actions `0` through `9`; document the trigger before validation. Start with focused boundary tests and run broader .NET validation only if focused evidence exposes wider risk.

## Safe Candidates

- Add live dispatch for action `10` and action `13` under the existing guarded direct-send path.
- Add focused tests for race filtering, action `10` optional action `26`, and action `13` update-only action `10`.
- Add current-team id live coverage for action `1` and `3` if team runtime seeding is easy and does not broaden the unit.
- Keep verified parity unclaimed until Java/C# runtime or golden evidence covers the full branch behavior.
