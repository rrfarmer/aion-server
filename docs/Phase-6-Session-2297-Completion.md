# Phase 6 Session 2297 Completion - CM_FIND_GROUP Mutation-Post Live Boundary

## Scope

Wired concrete C# live boundary behavior for `CM_FIND_GROUP` mutation-post actions `2` and `6`.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

Java behavior confirmed:

- Action `2` reads `playerOrTeamId`, `message`, `groupType`, calls `FindGroupService.addRecruitment`, sends system message `1400392`, then sends refreshed `SM_FIND_GROUP` action `0`.
- Action `6` reads `playerOrTeamId`, `message`, `groupType`, `classId`, `level`, calls `FindGroupService.addApplication`, sends system message `1400393`, then sends refreshed `SM_FIND_GROUP` action `4`.
- Both branches are direct sends to the triggering player, not world broadcasts or invite dispatches.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated. Current working context remains in the latest completion/handoff documents.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupDirectPacketBoundaryTraceReadinessService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupDirectPacketBoundaryTraceReadinessServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchReadinessReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchReadinessReportServiceTests.cs`
- `docs/parity-verification.md`

`GameServerConnection.ProcessPacketAsync` now dispatches parsed `CmFindGroup` action `2` and action `6` through the reviewed find-group boundary plan, then sends the direct packet intents to the active player in Java order.

The handler is deliberately narrow. It returns without side effects when there is no active player, the action is not `2` or `6`, the composition services are unavailable, a world broadcast or invite plan appears, or a direct packet intent targets a player other than the active player.

Readiness wording was updated only where the old broad statement was now false: action `2`/`6` mutation-post direct sends are wired, while the remaining `CM_FIND_GROUP` actions, runtime trace rows, registry observations, encrypted socket evidence, and full Java/C# value comparison remain unverified.

## Validation

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionFindGroupBoundaryTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~FindGroupDirectPacketBoundaryTraceReadinessServiceTests" --no-restore
```

Result: passed 36, failed 0, skipped 0. Existing nullable/analyzer warnings remain from unrelated files.

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

Broad-validation trigger: none. Full `.NET` project tests, solution tests, and solution builds were skipped because the focused commands covered the edited connection boundary, adjacent readiness wording, and targeted Java source-of-truth fixture.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `CM_FIND_GROUP.runImpl` action `2` | `GameServerConnection.HandleFindGroupAsync` / `FindGroupRecruitmentPlanService.AddRecruitment` | Client Packet Handler | Partial | Focused Boundary Tested | Partial Parity | C# `ProcessPacketAsync` now mutates the shared find-group store and sends `SmSystemMessage` id `1400392` before `SmFindGroup` action `0` for action `2`. Java source and targeted Java fixture reviewed/run. No encrypted socket or runtime row-value comparison yet. |
| `CM_FIND_GROUP.runImpl` action `6` | `GameServerConnection.HandleFindGroupAsync` / `FindGroupRecruitmentPlanService.AddApplication` | Client Packet Handler | Partial | Focused Boundary Tested | Partial Parity | C# `ProcessPacketAsync` now mutates the shared find-group store and sends `SmSystemMessage` id `1400393` before `SmFindGroup` action `4` for action `6`. Java source and targeted Java fixture reviewed/run. No encrypted socket or runtime row-value comparison yet. |

## Remaining Gaps

- `CM_FIND_GROUP` actions other than `2` and `6` remain unwired at the live connection boundary.
- No encrypted socket or real-client comparison has verified frame order.
- No Java/C# projected row value comparison has been executed.
- No verified parity claim is made.

## Commit

Commit message:

```text
[Phase 6][UOW-2297] Wire find-group mutation posts
```

## Next Recommended UOW

Capture or materialize accepted C# action `2`/`6` boundary rows from the now-wired `ProcessPacketAsync` path, then pair them with the Java mutation-post artifacts. Keep the next unit concrete: produce rows or compare values, not another evidence propagation layer.

Safe candidates:

- Add a guarded C# boundary-row capture for action `2` and action `6` using the live `ProcessPacketAsync` tests.
- Feed captured C# rows into the existing accepted-boundary-row handoff.
- Compare the captured rows with the Java artifacts from `FindGroupMutationPostTraceCaptureTest`.
- Defer action `0`/`4` show-list live wiring until mutation-post row capture/comparison is settled.
