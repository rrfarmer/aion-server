# Phase 6 Session 2299 Completion - CM_FIND_GROUP Row Value Comparison

## Scope

Added the first concrete Java/C# field-value comparison for `CM_FIND_GROUP` mutation-post actions `2` and `6`.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureTest.java`

Java behavior used:

- Action `2` posts a recruitment, sends system message `1400392`, refreshes `SM_FIND_GROUP` action `0`, and has zero world broadcasts/invite dispatches.
- Action `6` posts an application, sends system message `1400393`, refreshes `SM_FIND_GROUP` action `4`, and has zero world broadcasts/invite dispatches.
- The Java trace fixture records concrete row values including active player id, mutated entry id, visible entry ids, and side-effect counts.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostJavaTraceArtifactValidatorService.cs`

Added:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowValueComparisonExecutorService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowValueComparisonExecutorServiceTests.cs`

The Java artifact validator now retains the concrete trace values needed for comparison instead of keeping only action/mutation/message/action mapping metadata.

The new executor pairs shape-valid Java rows with accepted C# boundary rows and compares these scoped fields:

- `action`
- `mutationKind`
- `activePlayerObjectId`
- `mutatedEntryObjectId`
- `postedSystemMessageId`
- `refreshedListAction`
- `visibleEntryObjectIdsAfterMutation`
- `worldBroadcastCount`
- `inviteDispatchCount`

The executor emits matched, missing-row, and field-mismatch result rows. It always keeps `CanClaimVerifiedParity=false`; this UOW creates scoped comparison evidence only.

## Validation Decision

- Changed surface: C# Java-artifact value retention plus a focused row-value comparison executor/test.
- Specific behavior: shape-valid Java action `2`/`6` artifact rows can be paired with accepted C# action `2`/`6` boundary rows and compared on concrete field values.
- Broad-validation trigger: none.
- Broad .NET decision: skipped; filtered `dotnet test` built the affected project and covered the edited comparison/Java-artifact surface.

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

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `2` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowValueComparisonExecutorService` | Client Packet Handler / Comparison Executor | Partial | Focused Runtime Row Compared | Partial Parity | Java fixture row and accepted C# boundary row compare equal for the scoped action `2` fields listed above. Verified parity is not claimed; encrypted socket/real-client frame comparison remains open. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `6` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowValueComparisonExecutorService` | Client Packet Handler / Comparison Executor | Partial | Focused Runtime Row Compared | Partial Parity | Java fixture row and accepted C# boundary row compare equal for the scoped action `6` fields listed above. Verified parity is not claimed; encrypted socket/real-client frame comparison remains open. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment/addApplication` | `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactValidatorService` | Service Trace Artifact Reader | Partial | Unit Tested / Java Fixture Tested | Partial Parity | The C# artifact reader now retains Java fixture values needed for direct comparison. Remaining gaps include socket serialization and additional `CM_FIND_GROUP` actions. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `Compare_ShapeValidJavaArtifactsAndAcceptedCSharpRowsMatchesConcreteFieldsWithoutClaimingParity` | Runtime Row Comparison | Java source review plus `FindGroupMutationPostTraceCaptureTest` fixture values | Java action `2`/`6` artifact rows and accepted C# boundary rows match on the first concrete comparison field set. | Focused C# comparison result plus targeted Java Maven fixture run. | Does not compare encrypted socket frames or all `CM_FIND_GROUP` actions. |
| `Compare_VisibleEntryMismatchEmitsFieldMismatch` | Regression | Java `showRecruitments/showApplications` visible-entry filtering behavior | A changed C# visible-entry list is reported as a `MutationStateMismatch`. | Negative comparison test tied to Java visible-entry fixture values. | Does not exercise live client frame serialization. |
| `Compare_MissingAcceptedCSharpActionBlocksComparison` | Unit | Java action `2`/`6` fixture requirement | Missing accepted C# action `6` row blocks comparison instead of producing optimistic matches. | Conservative missing-row gate. | Does not produce new C# live rows itself. |

## Summary Metrics

- Java artifacts reviewed: 3
- C# artifacts updated/added: 3
- Focused C# tests passed: 23
- Focused Java tests run: 32 total, 31 passed, 1 skipped
- Verified parity rows added: 0
- Partial parity rows updated: 3
- Estimated Phase 6 completion: unchanged; this UOW adds direct row-value comparison evidence for two find-group mutation-post actions.

## Remaining Gaps

- No verified parity claim for `CM_FIND_GROUP`.
- No encrypted socket or real-client frame order comparison.
- Most other `CM_FIND_GROUP` actions remain outside live parity comparison.
- The current comparison uses accepted C# boundary rows supplied to the executor; the next UOW should feed rows captured from `ProcessPacketAsync` directly into this executor.

## Commit

Commit message:

```text
[Phase 6][UOW-2299] Compare find-group mutation rows
```

## Next Recommended UOW

Feed the live `ProcessPacketAsync` action `2`/`6` rows from `GameServerConnectionFindGroupBoundaryTests` into `FindGroupMutationPostProjectedRowValueComparisonExecutorService`, using Java fixture values with matching row identities, so the comparison executor consumes actual live boundary materialization rather than only accepted row objects constructed in the executor test.
