# Phase 6 Session 2061 Completion - Find Group Action 26 Mask Planning

Date: 2026-06-01
Unit of Work: UOW-2061
Status: Completed

## Scope

- Inspected Java `FindGroupService.showInstanceGroups(Player, boolean)` and `AutoGroupData` as the source of truth for action `26` mask-list behavior.
- Extended the disabled find-group planner to model the optional `SM_FIND_GROUP` action `26` packet that precedes action `10` in Java.
- Kept live config, target NPC, and `DataManager.AUTO_GROUP` lookup as caller-supplied facts.

## What Changed

- Added `FindGroupRecruitmentPlanService.ShowInstanceGroupsForClient(...)`.
- Added `FindGroupInstanceGroupClientShowPlan`.
- Updated `FindGroupClientActionPlanService` action `10` and action `13` composition:
  - Action `10` can plan action `26` when `formInstanceGroupAnywhere` is true.
  - Action `13` is treated as an update and does not plan action `26`, matching Java's `!isUpdate` guard.
  - Target-NPC mask ids are preferred when supplied; otherwise the all-recruitable mask list is used.
- Added focused tests proving non-update action `10`, update action `13`, and fallback mask-list behavior.

## Validation

- Focused C#:
  - `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupClientActionPlanServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests|FullyQualifiedName~SmFindGroupTests" --no-restore`
  - Result: passed, 44 tests.
- Focused Java/Maven:
  - `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=SM_FIND_GROUP_GoldenTest,CM_FIND_GROUP_ReadPayloadGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
  - Result: passed, 25 tests.
- Broad .NET suite was intentionally skipped under the UOW-2059 focused validation policy:
  - This unit changed disabled find-group planning and tests only.
  - No shared packet primitives, serialization helpers, crypto, persistence, world state, connection dispatch, live side effects, or common runtime infrastructure were changed.

## Known Gaps

- Live `GroupConfig.FORM_INSTANCE_GROUP_ANYWHERE`, target NPC, and `DataManager.AUTO_GROUP` sourcing remain unimplemented in C#.
- The Java overload `showInstanceGroups(Player, Npc portalNpc)` remains unmodeled as a separate disabled plan boundary.
- Live `CM_FIND_GROUP` dispatch remains deferred.
- Real send order and real-client behavior for action `26` followed by action `10` are not proven beyond disabled intent ordering.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupRecruitmentPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupClientActionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupClientActionPlanServiceTests.cs`
- `docs/Phase-6-Session-2061-Completion.md`
- `docs/Phase-6-Session-2061-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: inspect the Java overload `FindGroupService.showInstanceGroups(Player, Npc portalNpc)` as a disabled action `26` portal-specific plan boundary.

Safe alternative candidates:

- Inspect prepare-window actions `18`/`22`/`23`/`24` as disabled packet-plan boundaries.
- Inspect action `25` ban behavior and confirm whether Java intentionally leaves it unhandled in `CM_FIND_GROUP.runImpl`.
- Continue toward live `CM_FIND_GROUP` handler composition only after runtime dependency sourcing is explicitly planned.
