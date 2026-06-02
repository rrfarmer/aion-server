# Phase 6 Session 2301 Handoff - CM_FIND_GROUP Show Lists

## Startup Instructions

For the next session, read these documents first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2301-Completion.md`
- `docs/Phase-6-Session-2301-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive; current working state lives in the latest completion and handoff documents.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

UOW-2301 wired live C# `CM_FIND_GROUP` show-list actions `0` and `4`.

Concrete evidence:

- `GameServerConnection.HandleFindGroupAsync` now accepts actions `0`, `2`, `4`, and `6` through the guarded direct-send path.
- Action `0` live boundary test sends `SmFindGroup` action `0` and includes only same-race recruitment rows.
- Action `4` live boundary test sends `SmFindGroup` action `4` and includes only same-race application rows.
- Existing action `2`/`6` live mutation-post comparisons still pass in the boundary class.

Still not proven:

- Verified parity.
- Encrypted socket or real-client frame comparison.
- Multi-entry Java/C# ordering when more than one same-race entry is visible.
- Remaining `CM_FIND_GROUP` branches.

## Validation From Last Session

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionFindGroupBoundaryTests" --no-restore
```

Result: passed 31, failed 0, skipped 0.

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

Port and validate `CM_FIND_GROUP` removal actions:

- action `1`: remove recruitment
- action `5`: remove application

Java behavior to inspect:

- `CM_FIND_GROUP.readImpl/runImpl` action `1` and action `5`
- `FindGroupService.removeRecruitment(Player, byte, byte, byte, byte)`
- `FindGroupService.removeApplication(Player)`

Expected Java behavior:

- Action `1` resolves current team id, falls back to player id, removes the recruitment, and broadcasts `new SM_FIND_GROUP(playerOrTeamId, serverId, unk1, unk2, unk3)` to same-race players only when a row existed.
- Action `5` removes the active player's application and broadcasts `new SM_FIND_GROUP(player.getObjectId())` to same-race players only when a row existed.

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupRecruitmentPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`

## Focused Validation Recipe For Next UOW

Specific behavior to prove: live C# `CM_FIND_GROUP` action `1` and action `5` remove existing find-group rows and broadcast Java-equivalent removal packets to same-race players, while missing rows emit no packets.

Expected C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionFindGroupBoundaryTests" --no-restore
```

Narrow to new action `1`/`5` test names first if the full boundary class is slow.

Expected Java/Maven command:

```powershell
mvn --% -pl game-server -am -DskipTests=false -Dmaven.test.skip=false -Dtest=FindGroupMutationPostTraceCaptureTest -Dsurefire.failIfNoSpecifiedTests=false test
```

If a narrower Java removal fixture is found during discovery, use that instead and document the command.

Broad-validation trigger: live find-group dispatch will expand beyond actions `0`/`2`/`4`/`6`; document this trigger before validation. Start with focused boundary tests and run broader .NET validation only if focused evidence exposes wider risk.

## Safe Candidates

- Add live dispatch for action `1` and action `5` under the existing guarded boundary path.
- Add focused tests for existing-row removal and missing-row no-op.
- Keep verified parity unclaimed until Java/C# runtime or golden evidence covers the full branch behavior.
