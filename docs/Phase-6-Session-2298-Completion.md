# Phase 6 Session 2298 Completion - CM_FIND_GROUP Accepted Boundary Rows

## Scope

Materialized accepted C# boundary rows for `CM_FIND_GROUP` mutation-post actions `2` and `6` from the live `ProcessPacketAsync` path.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

Java behavior used:

- Action `2` calls `FindGroupService.addRecruitment`, records/stores the recruitment, sends system message `1400392`, then sends refreshed `SM_FIND_GROUP` action `0`.
- Action `6` calls `FindGroupService.addApplication`, records/stores the application, sends system message `1400393`, then sends refreshed `SM_FIND_GROUP` action `4`.
- Both branches have zero world broadcasts and zero invite dispatches.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated. Current working context remains in the latest completion/handoff documents.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupDirectPacketMutationPostBoundaryTraceSchemaService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupDirectPacketMutationPostBoundaryTraceExportProjectionTests.cs`

Added `CreateExportFromLiveBoundaryObservation`, which turns a boundary-accepted projected C# mutation-post row into an accepted live row only when observed `ProcessPacketAsync` sends are exactly `SmSystemMessage` followed by `SmFindGroup`, with the Java-mapped posted system message id.

Added a live boundary test that:

- runs action `2` and action `6` through `GameServerConnection.ProcessPacketAsync`,
- observes the actual sent packet order,
- materializes accepted C# boundary rows,
- feeds those rows through `FindGroupMutationPostGuardedFixtureResultContractService`,
- then proves the existing intake and handoff services can feed Java artifact pairing while still blocking runtime comparison and verified parity.

The existing schema field `executorInvokedFromBoundary` remains for contract compatibility. In this UOW it is satisfied only after observing live boundary sends from `ProcessPacketAsync`; no test-only opt-in executor call is used to accept the row.

## Validation Decision

- Changed surface: C# boundary-row materialization plus focused boundary/projection tests.
- Specific behavior: action `2`/`6` live `ProcessPacketAsync` sends can produce accepted C# rows for Java artifact pairing, and bad observed packet order is rejected.
- Broad-validation trigger: none.
- Broad .NET decision: skipped; the focused C# command built the affected project and covered the edited schema service plus directly adjacent boundary/intake/handoff contracts.

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionFindGroupBoundaryTests|FullyQualifiedName~FindGroupDirectPacketMutationPostBoundaryTraceExportProjectionTests|FullyQualifiedName~FindGroupMutationPostGuardedFixtureResultContractServiceTests|FullyQualifiedName~FindGroupMutationPostCSharpLiveBoundaryRowIntakePreflightServiceTests|FullyQualifiedName~FindGroupMutationPostCSharpAcceptedBoundaryRowHandoffReportServiceTests" --no-restore
```

Result: passed 46, failed 0, skipped 0. Existing nullable/analyzer warnings remain from unrelated files.

Focused Java/Maven validation:

```powershell
mvn --% -pl game-server -am -DskipTests=false -Dmaven.test.skip=false -Dtest=FindGroupMutationPostTraceCaptureTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Result: build success. `FindGroupMutationPostTraceCaptureTest` ran 32 tests, with 31 passed and 1 skipped.

Repository hygiene:

```powershell
git diff --check
```

Result: passed. Git reported line-ending normalization warnings only for edited files.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `2` | `Aion.GameServer.Network.Aion.GameServerConnection` / `FindGroupDirectPacketMutationPostBoundaryTraceSchemaService` | Client Packet Handler / Boundary Row Materializer | Partial | Focused Boundary Tested | Partial Parity | Live `ProcessPacketAsync` action `2` sends can now materialize an accepted C# boundary row with posted message id `1400392`, refreshed action `0`, zero broadcasts, and zero invites. Runtime Java/C# value comparison is still not run. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `6` | `Aion.GameServer.Network.Aion.GameServerConnection` / `FindGroupDirectPacketMutationPostBoundaryTraceSchemaService` | Client Packet Handler / Boundary Row Materializer | Partial | Focused Boundary Tested | Partial Parity | Live `ProcessPacketAsync` action `6` sends can now materialize an accepted C# boundary row with posted message id `1400393`, refreshed action `4`, zero broadcasts, and zero invites. Runtime Java/C# value comparison is still not run. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment/addApplication` | `FindGroupMutationPostCSharpAcceptedBoundaryRowHandoffReportService` | Handoff Contract | Partial | Unit/Boundary Tested | Partial Parity | Accepted C# rows can feed Java artifact pairing. Verified parity remains blocked until Java artifacts and accepted C# rows are compared field-by-field. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `ProcessPacketAsync_ActionTwoAndSixCanMaterializeAcceptedMutationPostBoundaryRows` | Boundary/Contract | Java source review plus `FindGroupMutationPostTraceCaptureTest` | Live C# action `2`/`6` packet sends can become accepted boundary rows and satisfy guarded/intake/handoff gates. | C# live boundary evidence plus targeted Java fixture run. | No Java/C# value comparison or encrypted socket capture. |
| `CreateExportFromLiveBoundaryObservation_RequiresPostedMessageBeforeFindGroup` | Unit | `FindGroupService.addRecruitment/addApplication` send order | Reversed observed packet order cannot produce an accepted C# boundary row. | Java send-order source review. | Does not execute socket frame serialization. |

## Summary Metrics

- Java artifacts reviewed: 2
- C# artifacts updated: 3
- Focused C# tests passed: 46
- Focused Java tests run: 32 total, 31 passed, 1 skipped
- Verified parity rows added: 0
- Partial parity rows updated: 3
- Estimated Phase 6 completion: unchanged; this UOW adds accepted C# row evidence but not runtime comparison.

## Remaining Gaps

- No Java/C# projected row value comparison has been executed.
- No encrypted socket or real-client comparison has verified frame order.
- Most `CM_FIND_GROUP` actions remain unwired at the live boundary.
- No verified parity claim is made.

## Commit

Commit message:

```text
[Phase 6][UOW-2298] Materialize find-group boundary rows
```

## Next Recommended UOW

Pair the accepted C# action `2`/`6` boundary rows with Java artifacts from `FindGroupMutationPostTraceCaptureTest` and run the first concrete field-value comparison.

Safe candidates:

- Inspect `FindGroupMutationPostJavaCSharpRowPairingReadinessReportService`.
- Inspect `FindGroupMutationPostProjectedRowComparisonValueContractService`.
- Inspect `FindGroupMutationPostProjectedRowComparisonDryRunContractService`.
- Feed the accepted C# rows from the live boundary materializer into the existing pairing/comparison path.
- Compare concrete fields first: action, mutation kind, active player id, mutated entry id, posted system message id, refreshed list action, visible entry ids, world broadcast count, and invite dispatch count.
