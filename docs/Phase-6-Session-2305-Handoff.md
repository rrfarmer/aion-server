# Phase 6 Session 2305 Handoff - CM_FIND_GROUP Instance Group Show Lists

## Startup Instructions

For the next session, read these documents first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2305-Completion.md`
- `docs/Phase-6-Session-2305-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive; current working state lives in the latest completion and handoff documents.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

UOW-2305 wired live C# `CM_FIND_GROUP` instance-group show-list actions `10` and `13`.

Concrete evidence:

- `GameServerConnection.HandleFindGroupAsync` now accepts actions `0` through `10`, plus action `13`.
- Action `10` sends direct `SmFindGroup` action `26` before action `10` when anywhere registration is enabled.
- Action `10` sends direct `SmFindGroup` action `10` same-race instance-group list.
- Action `13` sends direct `SmFindGroup` action `10` only.
- Adjacent disabled planner tests for action `10` and action `13` still pass.

Still not proven:

- Verified parity.
- Encrypted socket or real-client frame comparison.
- Target-NPC auto-group mask lookup through live boundary.
- Multi-row Java ordering.
- Action `1` and `3` current-team id behavior through the live boundary.
- Remaining `CM_FIND_GROUP` branches.

## Validation From Last Session

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_ActionTenAndThirteenSendInstanceGroupShowLists" --no-restore
```

Result: passed 1, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Adjacent C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CreateDisabledFindGroupBoundaryPlan_ActionTen|FullyQualifiedName~CreateDisabledFindGroupBoundaryPlan_ActionThirteen" --no-restore
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

Broad-validation trigger: live find-group dispatch expanded to action `10`/`13`.

Broad .NET decision: full project/solution validation was skipped after focused live boundary plus adjacent planner tests covered the changed branch. No shared packet primitive, crypto, scheduler, persistence, or broad model change was made.

## Next Sequential UOW

Port and validate `CM_FIND_GROUP` active-player direct/no-op branches:

- action `15`: show instance-group member info
- action `17`: update instance group

Java behavior to inspect:

- `CM_FIND_GROUP.readImpl/runImpl` action `15` and action `17`
- `FindGroupService.showInstanceGroupMembersInfo(Player, int)`
- `FindGroupService.updateInstanceGroup(Player, String)`
- `SM_FIND_GROUP` action `16` and action `10` write branches

Expected Java behavior:

- Action `15` sends `new SM_FIND_GROUP(16, List.of(instanceGroup))` directly to the active player only when the requested instance group exists.
- Missing action `15` rows emit no packets.
- Action `17` updates the active player's instance-group message and sends `showInstanceGroups(player, true)` action `10` only when the active player's row exists.
- Missing action `17` rows emit no packets.

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupRecruitmentPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`

## Focused Validation Recipe For Next UOW

Specific behavior to prove: live C# `CM_FIND_GROUP` action `15` and action `17` emit Java-equivalent direct `SmFindGroup` packets for existing rows and no-op for missing rows.

Expected C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_ActionFifteenAndSeventeenHandleInstanceGroupInfoAndUpdates|FullyQualifiedName~CreateDisabledFindGroupBoundaryPlan_ActionFifteen|FullyQualifiedName~CreateDisabledFindGroupBoundaryPlan_ActionSeventeen" --no-restore
```

If the adjacent filter is too broad, narrow to the new live test first and document the residual risk.

Expected Java/Maven command:

```powershell
mvn --% -pl game-server -am -DskipTests=false -Dmaven.test.skip=false -Dtest=FindGroupMutationPostTraceCaptureTest -Dsurefire.failIfNoSpecifiedTests=false test
```

If a narrower Java instance-group fixture is found during discovery, use that instead and document the command.

Broad-validation trigger: live find-group dispatch will expand to action `15`/`17`; document the trigger before validation. Start with focused boundary tests and run broader .NET validation only if focused evidence exposes wider risk.

## Safe Candidates

- Add live dispatch for action `15` and action `17` under the existing guarded direct-send path.
- Add focused tests for existing and missing rows.
- Add target-NPC action `10` mask lookup coverage if world/NPC setup is easy and does not broaden the unit.
- Add current-team id live coverage for action `1` and `3` if team runtime seeding is easy and does not broaden the unit.
- Leave action `11` for a later unit because it needs non-active recipient direct-send wiring through the registry.
- Leave action `12` for a later unit because it needs invite runtime dispatch review.
