# Phase 6 Session 2300 Handoff - CM_FIND_GROUP Live Row Comparison

## Startup Instructions

For the next session, read these documents first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2300-Completion.md`
- `docs/Phase-6-Session-2300-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive; current working state lives in the latest completion and handoff documents.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

UOW-2300 fed live C# `ProcessPacketAsync` rows into the row-value comparison executor for `CM_FIND_GROUP` mutation-post actions `2` and `6`.

Concrete evidence now available:

- Live C# action `2` and action `6` rows are materialized from observed `ProcessPacketAsync` sends.
- Those live rows feed `FindGroupMutationPostProjectedRowValueComparisonExecutorService`.
- The scoped fields all match Java-shaped fixture rows:
  - `action`
  - `mutationKind`
  - `activePlayerObjectId`
  - `mutatedEntryObjectId`
  - `postedSystemMessageId`
  - `refreshedListAction`
  - `visibleEntryObjectIdsAfterMutation`
  - `worldBroadcastCount`
  - `inviteDispatchCount`
- Verified parity remains unclaimed.

Still not proven:

- Encrypted socket or real-client frame order.
- Full `CM_FIND_GROUP` parity.
- Show-list action `0`/`4` row-value comparison.
- Remaining find-group branches and invite/result flows.

## Validation From Last Session

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_ActionTwoAndSixRowsFeedJavaCSharpValueComparison|FullyQualifiedName~FindGroupMutationPostProjectedRowValueComparisonExecutorServiceTests" --no-restore
```

Result: passed 4, failed 0, skipped 0.

Focused Java/Maven validation:

```powershell
mvn --% -pl game-server -am -DskipTests=false -Dmaven.test.skip=false -Dtest=FindGroupMutationPostTraceCaptureTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Result: build success. `FindGroupMutationPostTraceCaptureTest` ran 32 tests, with 31 passed and 1 skipped.

## Next Sequential UOW

Move to concrete Java/C# comparison for `CM_FIND_GROUP` show-list actions `0` and `4`.

Suggested scope:

- Review Java `FindGroupService.showRecruitments(Player)` and `showApplications(Player)`.
- Inspect C# `FindGroupRecruitmentPlanService.ShowRecruitments/ShowApplications` and existing boundary tests for actions `0` and `4`.
- Add focused comparison evidence for:
  - action id `0` for recruitment list,
  - action id `4` for application list,
  - race-filtered visible entry ordering,
  - direct send shape `SmFindGroup`,
  - no world broadcasts/invite dispatches.
- Keep parity status conservative.

Java artifacts:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- Existing Java find-group tests, if a narrow show-list fixture exists.

C# artifacts likely involved:

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupRecruitmentPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmFindGroup.cs`

## Focused Validation Recipe For Next UOW

Specific behavior to prove: C# `CM_FIND_GROUP` action `0` and action `4` boundary behavior emits Java-equivalent `SmFindGroup` show-list actions with race-filtered visible entries in Java order.

Expected C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionFindGroupBoundaryTests" --no-restore
```

Narrow to newly added show-list comparison test names if the full boundary class is slow.

Expected Java/Maven command:

```powershell
mvn --% -pl game-server -am -DskipTests=false -Dmaven.test.skip=false -Dtest=FindGroupMutationPostTraceCaptureTest -Dsurefire.failIfNoSpecifiedTests=false test
```

If a narrower Java show-list fixture is found during discovery, use that instead and document the command.

Broad-validation trigger: none unless the next UOW changes shared packet primitives, live dispatch outside find-group action `0`/`4`, persistence, scheduler, crypto, connection infrastructure, or packet serialization.

## Safe Candidates

- Add a focused action `0`/`4` show-list comparison test.
- Inspect whether `SmFindGroup` exposes enough list payload state for row-value comparison; if not, add a test-visible safe reader rather than changing wire serialization.
- After action `0`/`4`, continue to another actual `CM_FIND_GROUP` branch with Java source review and focused boundary evidence.
