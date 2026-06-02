# Phase 6 Session 2299 Handoff - CM_FIND_GROUP Row Value Comparison

## Startup Instructions

For the next session, read these documents first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2299-Completion.md`
- `docs/Phase-6-Session-2299-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive; current working state lives in the latest completion and handoff documents.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

UOW-2299 added concrete row-value comparison for `CM_FIND_GROUP` mutation-post action `2` and action `6`.

Concrete evidence now available:

- Java artifact rows from `FindGroupMutationPostTraceCaptureTest` retain the values needed for comparison.
- Accepted C# boundary rows can be compared against Java artifact rows for:
  - `action`
  - `mutationKind`
  - `activePlayerObjectId`
  - `mutatedEntryObjectId`
  - `postedSystemMessageId`
  - `refreshedListAction`
  - `visibleEntryObjectIdsAfterMutation`
  - `worldBroadcastCount`
  - `inviteDispatchCount`
- The comparison executor emits `Matched`, `MissingJavaRow`, `MissingCSharpRow`, and `FieldMismatch` rows.
- A negative test proves a changed visible-entry list is reported as a field mismatch.

Still not proven:

- Verified parity.
- Encrypted socket or real-client frame order.
- Comparison using rows captured directly from `ProcessPacketAsync` in the same test.
- Live wiring/comparison for most other `CM_FIND_GROUP` actions.

## Validation From Last Session

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowValueComparisonExecutorServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactValidatorServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactDirectoryReportServiceTests|FullyQualifiedName~FindGroupMutationPostComparisonInputEnvelopeServiceTests" --no-restore
```

Result: passed 23, failed 0, skipped 0.

Focused Java/Maven validation:

```powershell
mvn --% -pl game-server -am -DskipTests=false -Dmaven.test.skip=false -Dtest=FindGroupMutationPostTraceCaptureTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Result: build success. `FindGroupMutationPostTraceCaptureTest` ran 32 tests, with 31 passed and 1 skipped.

Repository hygiene:

```powershell
git diff --check
```

Result: passed with line-ending normalization warning only.

## Next Sequential UOW

Feed live `ProcessPacketAsync` rows into the new comparison executor.

Suggested scope:

- Extend or add a focused `GameServerConnectionFindGroupBoundaryTests` test.
- Reuse the existing live materialization helper that produces accepted action `2` and `6` rows from `ProcessPacketAsync`.
- Build matching Java artifact rows for the same object ids and visible-entry lists.
- Call `FindGroupMutationPostProjectedRowValueComparisonExecutorService.Compare`.
- Assert all scoped fields match and `CanClaimVerifiedParity` remains false.

Java artifacts to keep in view:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureTest.java`

C# artifacts likely involved:

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowValueComparisonExecutorService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupDirectPacketMutationPostBoundaryTraceSchemaService.cs`

## Focused Validation Recipe For Next UOW

Specific behavior to prove: live `ProcessPacketAsync` action `2`/`6` C# boundary rows can feed the concrete Java/C# row-value comparison executor and match Java fixture values for the scoped field set.

Expected C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionFindGroupBoundaryTests|FullyQualifiedName~FindGroupMutationPostProjectedRowValueComparisonExecutorServiceTests" --no-restore
```

Narrow to a specific boundary test plus `FindGroupMutationPostProjectedRowValueComparisonExecutorServiceTests` if the full boundary class is slow.

Expected Java/Maven command:

```powershell
mvn --% -pl game-server -am -DskipTests=false -Dmaven.test.skip=false -Dtest=FindGroupMutationPostTraceCaptureTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Broad-validation trigger: none unless the next UOW changes shared packet primitives, live dispatch outside action `2`/`6`, persistence, scheduler, crypto, connection infrastructure, or packet serialization.

## Safe Candidates

- Add live-boundary-to-comparison coverage for action `2` and action `6`.
- Keep comparison output conservative; do not mark verified parity.
- After live-boundary comparison is in place, move to the next actual `CM_FIND_GROUP` behavior such as action `0`/`4` show-list live comparison or another Java-backed side-effect branch.
