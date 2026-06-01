# Phase 6 Session 2063 Completion - Find Group Prepare Window Planning

Date: 2026-06-01
Unit of Work: UOW-2063
Status: Completed

## Scope

- Inspected Java `SM_FIND_GROUP` prepare-window payload actions `18`, `22`, `23`, and `24`.
- Added disabled C# planner boundaries for prepare-window packet intents.
- Kept live server-wide group runtime, send ordering, and requester lifecycle out of scope.

## What Changed

- Added `FindGroupPrepareWindowPlanKind`.
- Added `FindGroupPrepareWindowPlan`.
- Added disabled planner methods:
  - `ShowEnterButtonInPrepareForEntryWindow(...)` for action `18`.
  - `ShowPrepareForEntryWindow(...)` for action `22`.
  - `DestroyPrepareForEntryWindow(...)` for action `23`.
  - `UpdatePrepareForEntryWindow(...)` for action `24`.
- Added focused tests proving each planner method creates one direct packet intent, targets the player, keeps live dispatch disabled, and serializes through the existing Java-golden-backed `SmFindGroup` packet path.

## Validation

- Focused C#:
  - `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupRecruitmentPlanServiceTests|FullyQualifiedName~SmFindGroupTests" --no-restore`
  - Result: passed, 40 tests.
- Focused Java/Maven:
  - `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=SM_FIND_GROUP_GoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
  - Result: passed, 13 tests.
- Broad .NET suite was intentionally skipped under the focused validation policy:
  - This unit changed disabled find-group planning and focused tests only.
  - No shared packet primitives, serialization helpers, crypto, persistence, world state, connection dispatch, live side effects, or common runtime infrastructure were changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_FIND_GROUP` | `Aion.GameServer.Network.Aion.ServerPackets.SmFindGroup` | Packet | Partial | Golden File Tested | Partial Parity | Actions `18`, `22`, `23`, and `24` already had Java-golden byte tests; this unit used that packet path from disabled planner tests. Other `SM_FIND_GROUP` actions and live send context remain broader find-group work. |
| `com.aionemu.gameserver.model.gameobjects.findGroup.ServerWideGroup` | `FindGroupInstanceGroupWindowSnapshot`, `FindGroupInstanceGroupPrepareWindowSnapshot`, `FindGroupInstanceGroupPrepareMemberSnapshot` | DTO / Snapshot | Partial | Unit Tested | Partial Parity | Only fields needed by prepare-window packet actions are represented. Java dynamic `getMembers()` behavior through current team remains caller-supplied and not live-verified. |
| Java `PacketSendUtility.sendPacket(player, new SM_FIND_GROUP(...))` prepare-window call sites | `Aion.GameServer.Services.FindGroupPrepareWindowPlan` | Plan DTO | Partial | Unit Tested | Partial Parity | Disabled intent only. Tests prove packet selection and recipient, not live send ordering, concurrency, requester lifecycle, or real-client behavior. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupRecruitmentPlanServiceTests.PrepareWindowPlansRouteToJavaEquivalentPacketsWithoutLiveDispatch` | Unit | Java `SM_FIND_GROUP` action methods and existing Java golden packet tests | Planner routes actions `18`, `22`, `23`, and `24` to the expected `SmFindGroup` packet constructors and disables live dispatch | Compares planned packets to same C# packet constructors covered by `SM_FIND_GROUP_GoldenTest` bytes | Does not prove live service invocation, send ordering, or real-client UI behavior |
| `SmFindGroupTests` prepare-window tests | Golden File | Java `SM_FIND_GROUP_GoldenTest` | Packet payload shape for actions `18`, `22`, `23`, and `24` | Exact payload bytes | Does not prove runtime trigger conditions |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2.
- Total artifacts ported or represented in this UOW: 3 C# planner/snapshot surfaces.
- Total artifacts with verified parity: 0 broad artifacts; packet byte slices retain golden evidence.
- Total artifacts needing verification or partial parity: 3.
- Total blocked artifacts: 0.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred.
- Live prepare-window trigger conditions, requester lifecycle, send ordering, server-wide group membership sourcing, and real-client UI behavior remain unverified.
- `ServerWideGroup.getMembers()` can defer to the recruiter's current team; disabled C# snapshots require caller-supplied members.
- No actual `FindGroupService` singleton runtime, `PacketSendUtility.sendPacket`, or service concurrency parity has been proven for prepare-window actions.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupRecruitmentPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupRecruitmentPlanServiceTests.cs`
- `docs/Phase-6-Session-2063-Completion.md`
- `docs/Phase-6-Session-2063-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: inspect action `25` ban behavior and confirm whether Java intentionally leaves it unhandled in `CM_FIND_GROUP.runImpl`.

Safe alternative candidates:

- Inspect logout cleanup parity for recruitment/application/instance-group maps.
- Continue refining disabled live-handler composition only after runtime dependency sourcing is explicitly planned.
- Return to action `18`-`24` live trigger discovery only with Java call-site evidence beyond packet serialization.
