# Phase 6 Session 2060 Completion - CM_FIND_GROUP Composition Planner

Date: 2026-06-01
Unit of Work: UOW-2060
Status: Completed

## Scope

- Inspected Java `CM_FIND_GROUP.readImpl` and `runImpl` as the source of truth.
- Added a disabled C# composition planner for client find-group actions.
- Proved action routing into the existing disabled find-group planner without enabling live dispatch or packet sends.

## What Changed

- Added `FindGroupClientActionPlanService`.
- Added `FindGroupClientAction`, including a `FromPacket(CmFindGroup)` bridge from the parsed C# packet.
- Added `FindGroupClientActionPlanKind` and `FindGroupClientActionPlan` to identify which Java-equivalent planner path was selected.
- Covered Java `runImpl` actions:
  - `0`: show recruitments.
  - `1`: remove recruitment.
  - `2`: add recruitment.
  - `3`: update recruitment.
  - `4`: show applications.
  - `5`: remove application.
  - `6`: add application.
  - `7`: update application.
  - `8`: register instance group.
  - `9`: remove instance group.
  - `10`: show instance groups.
  - `11`: send instance application through caller-supplied player resolver.
  - `12`: send instance application result through caller-supplied player resolver.
  - `13`: show instance groups as update route.
  - `15`: show instance group member info.
  - `17`: update instance group.
- Documented parsed-but-not-run actions `20` and `25` as non-dispatching plans because Java parses them but has no `runImpl` branch.

## Validation

- Focused C#:
  - `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupClientActionPlanServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests|FullyQualifiedName~GamePacketTests.CmFindGroup" --no-restore`
  - Result: passed, 29 tests.
- Focused Java/Maven:
  - `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_FIND_GROUP_ReadPayloadGoldenTest,SM_FIND_GROUP_GoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
  - Result: passed, 25 tests.
- Broad .NET suite was intentionally skipped under the UOW-2059 focused validation policy:
  - This unit added disabled composition and focused tests only.
  - No shared packet primitives, serialization helpers, crypto, persistence, world state, connection dispatch, live side effects, or common runtime infrastructure were changed.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- The composition planner requires caller-supplied runtime facts for player lookup, team/member snapshots, and timestamps.
- Action `13` currently routes to the same disabled `ShowInstanceGroups` packet plan as action `10`; Java-side optional action `26` mask-list behavior remains separate work.
- Prepare-window behavior, ban behavior, logout cleanup, live `FindGroupService` runtime state, real send/broadcast side effects, service concurrency, and real-client behavior remain unverified.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupClientActionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupClientActionPlanServiceTests.cs`
- `docs/Phase-6-Session-2060-Completion.md`
- `docs/Phase-6-Session-2060-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: inspect `GroupConfig.FORM_INSTANCE_GROUP_ANYWHERE` action `26` mask-list planning around Java `showInstanceGroups(player, isUpdate)`, especially action `13` update behavior.

Safe alternative candidates:

- Inspect prepare-window actions `18`/`22`/`23`/`24` as disabled packet-plan boundaries.
- Inspect action `25` ban behavior and confirm whether Java intentionally leaves it unhandled in `CM_FIND_GROUP.runImpl`.
- Continue toward live `CM_FIND_GROUP` handler composition only after runtime dependency sourcing is explicitly planned.
