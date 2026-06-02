# Phase 6 Session 2298 Handoff - Accepted CM_FIND_GROUP Boundary Rows

## Startup Instructions

For the next session, read these documents first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2298-Completion.md`
- `docs/Phase-6-Session-2298-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive; current working state lives in the latest completion and handoff documents.

Java remains the source of truth. Do not claim verified parity without objective Java/C# evidence.

Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

UOW-2298 materialized accepted C# boundary rows for `CM_FIND_GROUP` mutation-post action `2` and action `6` from the live `ProcessPacketAsync` path.

Current concrete evidence:

- C# live boundary sends action `2`: `SmSystemMessage` id `1400392`, then `SmFindGroup` action `0`.
- C# live boundary sends action `6`: `SmSystemMessage` id `1400393`, then `SmFindGroup` action `4`.
- `CreateExportFromLiveBoundaryObservation` accepts rows only when observed live sends are `SmSystemMessage` before `SmFindGroup`.
- The accepted rows satisfy guarded fixture, live-row intake, and accepted-boundary-row handoff gates.

Still not proven:

- Java/C# field-value comparison.
- Encrypted socket or real-client frame order.
- Verified parity.
- Live wiring for most other `CM_FIND_GROUP` actions.

## Validation From Last Session

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionFindGroupBoundaryTests|FullyQualifiedName~FindGroupDirectPacketMutationPostBoundaryTraceExportProjectionTests|FullyQualifiedName~FindGroupMutationPostGuardedFixtureResultContractServiceTests|FullyQualifiedName~FindGroupMutationPostCSharpLiveBoundaryRowIntakePreflightServiceTests|FullyQualifiedName~FindGroupMutationPostCSharpAcceptedBoundaryRowHandoffReportServiceTests" --no-restore
```

Result: passed 46, failed 0, skipped 0.

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

## Next Sequential UOW

Pair accepted C# action `2`/`6` boundary rows with Java artifacts from `FindGroupMutationPostTraceCaptureTest` and run the first concrete field-value comparison.

Suggested scope:

- Inspect `FindGroupMutationPostJavaCSharpRowPairingReadinessReportService`.
- Inspect `FindGroupMutationPostProjectedRowComparisonValueContractService`.
- Inspect `FindGroupMutationPostProjectedRowComparisonDryRunContractService`.
- Inspect `FindGroupMutationPostComparisonInputEnvelopeService`.
- Reuse the live boundary-row materialization path from `GameServerConnectionFindGroupBoundaryTests`.
- Compare concrete row fields before adding any further readiness metadata.

Concrete fields to compare first:

- `action`
- `mutationKind`
- `activePlayerObjectId`
- `mutatedEntryObjectId`
- `postedSystemMessageId`
- `refreshedListAction`
- `visibleEntryObjectIdsAfterMutation`
- `worldBroadcastCount`
- `inviteDispatchCount`

## Focused Validation Recipe For Next UOW

Specific behavior to prove: accepted C# action `2`/`6` boundary rows can be paired with Java action `2`/`6` artifacts and compared on concrete field values without claiming verified parity.

Expected C# command shape:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostJavaCSharpRowPairingReadinessReportServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonDryRunContractServiceTests|FullyQualifiedName~GameServerConnectionFindGroupBoundaryTests" --no-restore
```

Narrow this further if only one comparison class is edited.

Expected Java/Maven command:

```powershell
mvn --% -pl game-server -am -DskipTests=false -Dmaven.test.skip=false -Dtest=FindGroupMutationPostTraceCaptureTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Broad-validation trigger: none unless the next UOW changes shared packet primitives, live dispatch outside action `2`/`6`, persistence, scheduler, crypto, or connection infrastructure beyond the current find-group boundary.

## Safe Candidates

- Add a focused comparison service/test that consumes Java artifact rows plus accepted C# rows.
- Keep comparison output conservative: matched/mismatched field rows, not verified parity.
- Wire comparison only after both action `2` and action `6` rows are present.
- Leave action `0`/`4` show-list live wiring for a later UOW.
