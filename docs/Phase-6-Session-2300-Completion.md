# Phase 6 Session 2300 Completion - CM_FIND_GROUP Live Row Comparison

## Scope

Fed live C# `ProcessPacketAsync` mutation-post rows into the concrete Java/C# row-value comparison executor for `CM_FIND_GROUP` actions `2` and `6`.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureTest.java`

Java behavior used:

- Action `2` stores a recruitment, sends `SM_SYSTEM_MESSAGE` id `1400392`, then sends refreshed `SM_FIND_GROUP` action `0`.
- Action `6` stores an application, sends `SM_SYSTEM_MESSAGE` id `1400393`, then sends refreshed `SM_FIND_GROUP` action `4`.
- Both branches use direct sends only, with zero world broadcasts and zero invite dispatches.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

Updated:

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`

Added `ProcessPacketAsync_ActionTwoAndSixRowsFeedJavaCSharpValueComparison`, which:

- runs action `2` and action `6` through live `GameServerConnection.ProcessPacketAsync`,
- materializes accepted C# boundary rows from observed packet order,
- creates Java-shaped artifact rows for the same fixture identities and Java-derived field values,
- feeds both sides into `FindGroupMutationPostProjectedRowValueComparisonExecutorService`,
- asserts all 18 scoped field comparisons match,
- keeps `CanClaimVerifiedParity=false`.

## Validation Decision

- Changed surface: focused C# boundary test only.
- Specific behavior: live `ProcessPacketAsync` action `2`/`6` rows can feed the Java/C# row-value comparison executor and match Java fixture values for the scoped field set.
- Broad-validation trigger: none.
- Broad .NET decision: skipped; filtered `dotnet test` built the affected project and covered the edited boundary test plus the adjacent comparison executor tests.

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

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `2` | `Aion.GameServer.Network.Aion.GameServerConnection` / `FindGroupMutationPostProjectedRowValueComparisonExecutorService` | Client Packet Handler / Runtime Comparison | Partial | Focused Runtime Row Compared | Partial Parity | Live C# `ProcessPacketAsync` action `2` row now feeds the comparison executor and matches Java-shaped fixture values for the scoped fields. Verified parity is not claimed; encrypted socket/real-client frame comparison remains open. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `6` | `Aion.GameServer.Network.Aion.GameServerConnection` / `FindGroupMutationPostProjectedRowValueComparisonExecutorService` | Client Packet Handler / Runtime Comparison | Partial | Focused Runtime Row Compared | Partial Parity | Live C# `ProcessPacketAsync` action `6` row now feeds the comparison executor and matches Java-shaped fixture values for the scoped fields. Verified parity is not claimed; encrypted socket/real-client frame comparison remains open. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment/addApplication` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService` / `FindGroupDirectPacketMutationPostBoundaryTraceSchemaService` | Service / Boundary Trace | Partial | Boundary Tested / Java Fixture Tested | Partial Parity | State mutation, direct packet ids/actions, visible entry ids, and zero side-effect counts are compared for the live C# boundary rows. Other find-group actions remain outside this comparison. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `ProcessPacketAsync_ActionTwoAndSixRowsFeedJavaCSharpValueComparison` | Boundary Runtime Comparison | Java source review plus `FindGroupMutationPostTraceCaptureTest` | Live C# action `2`/`6` rows from `ProcessPacketAsync` can be compared with Java-shaped artifact rows and match on the scoped fields. | Focused C# live-boundary comparison plus targeted Java Maven fixture run. | Does not compare encrypted socket frames or all `CM_FIND_GROUP` actions. |

## Summary Metrics

- Java artifacts reviewed: 3
- C# artifacts updated: 1
- Focused C# tests passed: 4
- Focused Java tests run: 32 total, 31 passed, 1 skipped
- Verified parity rows added: 0
- Partial parity rows updated: 3
- Estimated Phase 6 completion: unchanged; this UOW strengthens action `2`/`6` evidence but does not complete `CM_FIND_GROUP`.

## Remaining Gaps

- No verified parity claim for `CM_FIND_GROUP`.
- No encrypted socket or real-client frame order comparison.
- Actions `0` and `4` show-list behavior have disabled-plan coverage but no concrete Java/C# row-value comparison.
- Other `CM_FIND_GROUP` branches remain outside the current comparison.

## Commit

Commit message:

```text
[Phase 6][UOW-2300] Compare live find-group boundary rows
```

## Next Recommended UOW

Move to another actual `CM_FIND_GROUP` behavior. The safest next step is concrete Java/C# comparison for show-list actions `0` and `4`, starting with Java source review of `FindGroupService.showRecruitments/showApplications` and focused C# boundary tests for `SmFindGroup` action ids and visible entry ordering.
