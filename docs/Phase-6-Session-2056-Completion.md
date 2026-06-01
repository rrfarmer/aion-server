# Phase 6 Session 2056 Completion - Find Group Instance-Group Planner

Date: 2026-06-01
Unit of Work: UOW-2056
Status: Completed

## Scope

- Performed Work Discovery after UOW-2055 and inspected Java/C# find-group instance-group service, model, packet, parser, progress, and handoff surfaces.
- Scoped this unit to disabled C# planner evidence for Java instance-group registration/update/remove/show/member-info behavior.
- Kept live `CM_FIND_GROUP` dispatch and world/socket side effects deferred.

## What Changed

- Extended `FindGroupRecruitmentPlanService` with instance-group state keyed by recruiter object id.
- Modeled Java `registerInstanceGroup` as state put plus direct `SM_FIND_GROUP` action `14` packet intent.
- Modeled Java `showInstanceGroups` as race-filtered action `10` show-list packet planning.
- Modeled Java `updateInstanceGroup` as existing-entry message/timestamp mutation plus action `10` show-list planning.
- Modeled Java `removeInstanceGroup` as remove-by-recruiter followed by action `10` show-list planning even when missing.
- Modeled Java `showInstanceGroupMembersInfo` as existing-entry action `16` direct packet intent.
- Preserved the Java `ServerWideGroup.getMinLevel` / `getMaxLevel` comparator quirk in registration snapshots.

## Validation

- Focused C# `FindGroupRecruitmentPlanServiceTests` passed with 18 tests.
- Focused C# find-group planner/packet/parser tests passed with 32 tests.
- Focused Java `SM_FIND_GROUP_GoldenTest` and `CM_FIND_GROUP_ReadPayloadGoldenTest` passed with 25 test methods.
- Full scoped Maven reactor passed with 1 commons test and 126 game-server tests.
- Broad C# validation passed with 5195 tests.

## Known Gaps

- This is disabled planner evidence only.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- No Java `FindGroupService` singleton runtime test, actual packet send, online recipient filtering, encrypted socket frame, real-client behavior, service concurrency, or `TemporaryPlayerTeam` dynamic member parity is proven.
- The planner uses deterministic timestamps and caller-supplied current-member snapshots; Java uses system time and dynamically reads recruiter team members when present.
- `GroupConfig.FORM_INSTANCE_GROUP_ANYWHERE`, `DataManager.AUTO_GROUP` instance-mask list packets, portal NPC routing, applicant-response, prepare-window actions, ban action, logout cleanup, and live handler composition remain outside this unit.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupRecruitmentPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupRecruitmentPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-2056-Completion.md`
- `docs/Phase-6-Session-2056-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: inspect instance-group applicant response behavior (`sendInstanceApplication` and `sendInstanceApplicationResult`) as a disabled planner slice, including accept-to-group/alliance intent and denial whisper intent without live invitations.

Safe alternative candidates:

- Inspect CM_FIND_GROUP action `0`-`17` composition with the planner while still disabled.
- Inspect `GroupConfig.FORM_INSTANCE_GROUP_ANYWHERE` action `26` mask-list planning.
- Return to alliance/group recipient filtering only with objective packet/fanout evidence.
