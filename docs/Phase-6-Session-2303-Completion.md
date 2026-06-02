# Phase 6 Session 2303 Completion - CM_FIND_GROUP Updates

## Scope

Wired live C# `CM_FIND_GROUP` update actions `3` and `7`.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

Java behavior used:

- Action `3` reads `playerOrTeamId`, `serverId`, `unk1`, `unk2`, `unk3`, `message`, and `groupType`; `runImpl` ignores the id/server/unknown fields and updates the active player's current-team recruitment or active-player recruitment.
- Action `7` reads `playerOrTeamId`, `message`, `groupType`, `classId`, and `level`; `runImpl` ignores `playerOrTeamId` and updates the active player's application.
- Existing rows are updated in place and refresh `lastUpdate`.
- Missing rows emit no packet side effects.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`

`HandleFindGroupAsync` now permits action `3` and action `7` through the existing planner-backed live boundary. These branches mutate find-group state through `FindGroupRecruitmentPlanService` and then return without packet dispatch because the Java branches have no direct or broadcast packet side effects.

Added `ProcessPacketAsync_ActionThreeAndSevenUpdateRowsWithoutPacketSideEffects`, which:

- seeds a recruitment and application,
- runs live action `3` through `ProcessPacketAsync`,
- verifies recruitment message/group type and current-time `lastUpdate` changed,
- removes the recruitment and repeats action `3` to verify missing-row no-op behavior,
- runs live action `7` through `ProcessPacketAsync`,
- verifies application message/group type/class/level and current-time `lastUpdate` changed,
- removes the application and repeats action `7` to verify missing-row no-op behavior,
- verifies no direct packets, world broadcasts, or connection-send packets were emitted.

## Validation Decision

- Changed surface: live find-group boundary dispatch for no-packet state update branches.
- Specific behavior/contract: live C# action `3`/`7` updates existing rows, preserves Java missing-row no-op behavior, and emits no packets.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_ActionThreeAndSevenUpdateRowsWithoutPacketSideEffects" --no-restore
```

Result: passed 1, failed 0, skipped 0. The command built the affected project and dependencies. Pre-existing nullable/analyzer warnings remain.

An earlier focused command exposed a test variable placement error and an over-broad substring filter that matched `ActionSeventeen`; both were corrected before the final validation command above.

- Focused Java/Maven command:

```powershell
mvn --% -pl game-server -am -DskipTests=false -Dmaven.test.skip=false -Dtest=FindGroupMutationPostTraceCaptureTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Result: build success. `FindGroupMutationPostTraceCaptureTest` ran 32 tests, with 31 passed and 1 skipped.

- Repository hygiene:

```powershell
git diff --check
```

Result: passed with line-ending normalization warnings only.

- Broad-validation trigger: live find-group dispatch expanded to action `3`/`7`.
- Broad .NET decision: skipped full project/solution validation after the focused live boundary test covered the changed branch. No shared packet primitive, crypto, scheduler, persistence, or broad model change was made.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `3` | `Aion.GameServer.Network.Aion.GameServerConnection` / `FindGroupRecruitmentPlanService.UpdateRecruitment` | Client Packet Handler / Service | Partial | Focused Boundary Tested | Partial Parity | Live C# action `3` updates the active player's recruitment and emits no packets. Verified parity is not claimed; current-team id update through the live boundary remains open. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `7` | `Aion.GameServer.Network.Aion.GameServerConnection` / `FindGroupRecruitmentPlanService.UpdateApplication` | Client Packet Handler / Service | Partial | Focused Boundary Tested | Partial Parity | Live C# action `7` updates the active player's application and emits no packets. Verified parity is not claimed; encrypted socket/client behavior remains open. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.updateRecruitment/updateApplication` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.UpdateRecruitment/UpdateApplication` | Service | Partial | Boundary Tested / Java Fixture Tested | Partial Parity | Existing-row update and missing-row no-op behavior are covered through the live boundary for solo recruitment/application rows. Current-team recruitment update remains untested. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `ProcessPacketAsync_ActionThreeAndSevenUpdateRowsWithoutPacketSideEffects` | Boundary Runtime | Java source review plus targeted Java find-group fixture run | Live C# action `3` and `7` update rows, no-op on missing rows, and emit no packets. | Focused C# boundary execution and Java source/fixture validation. | Does not compare encrypted socket bytes or current-team recruitment update. |

## Remaining Gaps

- No verified parity claim for `CM_FIND_GROUP`.
- No encrypted socket or real-client frame comparison.
- Action `1` and `3` current-team id behavior needs live boundary coverage.
- Remaining actions `8`, `9`, `10`, `11`, `12`, `13`, `15`, and `17` still need concrete live parity evidence or explicit scope decisions.

## Commit

Commit message:

```text
[Phase 6][UOW-2303] Wire find-group updates
```

## Next Recommended UOW

Move to instance-group actions `8` and `9`, because Java registers/removes instance-group rows and sends direct `SM_FIND_GROUP` packets. The C# planner has disabled coverage for these branches, but the live `GameServerConnection` gate still does not include them.
