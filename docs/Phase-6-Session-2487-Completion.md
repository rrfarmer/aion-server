# Phase 6 Session 2487 Completion

## UOW

[Phase 6] UOW-2487: Add Vortex onLeaveZone kick-schedule metadata

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/model/vortex/VortexLocation.java`

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Services/VortexZoneLeaveKickSchedulePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

## Implementation Notes

- Added non-live `VortexZoneLeaveKickSchedulePlanService`.
- Added `VortexZoneLeaveKickSchedulePlanStatus` and `VortexZoneLeaveKickSchedulePlan`.
- The planner mirrors Java `VortexLocation.onLeaveZone` player behavior by recording:
  - outer `!isInsideLocation(creature)` gate;
  - zone-player removal intent once a player fully leaves the Vortex location;
  - active-vortex gate from `isActive()`;
  - invader passed-player gate from `getVortexController().getPassedPlayers().containsKey(player.getObjectId())`;
  - invader battlefield-left message `904305`;
  - 10-second kick-schedule intent;
  - scheduled guard metadata for `player.isOnline() && !isInsideActiveVotrex(player)`;
  - invader vs defender `kickPlayer(player, true/false)` selection.
- Scope remains metadata-only. It does not mutate zone players, send packets, schedule tasks, or kick participants.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `ZoneLeaveKickSchedulePlan_BlocksWhileStillInsideAndRemovesOnlyWhenInactive` | Unit | `VortexLocation.onLeaveZone` source review | C# preserves Java's outer still-inside guard and inactive removal-only branch | Focused C# test validates no zone removal while still inside, zone removal intent after leaving, no schedule/message when inactive, and disabled live mutation | Does not exercise live Java zones |
| `ZoneLeaveKickSchedulePlan_InvaderRequiresPassedPlayerBeforeMessageAndKickSchedule` | Unit | `VortexLocation.onLeaveZone` source review | C# records invader passed-player gate, message `904305`, 10-second kick delay, and scheduled online/outside-active guards | Focused C# test validates missing-pass skip and passed invader schedule/message metadata with no live side effects | Does not send live packet or schedule task |
| `ZoneLeaveKickSchedulePlan_DefenderSchedulesKickWithoutBattlefieldLeftMessage` | Unit | `VortexLocation.onLeaveZone` source review | C# records defender schedule path without passed-player requirement or battlefield-left message | Focused C# test validates defender kick flag, 10-second delay, online/outside guard metadata, and disabled live side effects | Does not execute live defender kick |

## Validation Decision

- Changed surface: non-live Vortex leave-zone kick-schedule metadata and focused tests.
- Specific behavior/contract: C# metadata mirrors Java `VortexLocation.onLeaveZone` player gates for full location leave, active invasion, invader passed-player membership, defender scheduling, battlefield-left message intent, 10-second kick delay, and no live scheduler/packet/participant mutation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests" --no-restore
```

- Result: Passed, 71 tests. Existing nullable/analyzer warnings were emitted.
- Focused Java/Maven command: skipped; no Java source or fixtures changed and no narrow Java Vortex lifecycle/static-data fixture exists. Java source was reviewed directly.
- Broad-validation trigger: none. This UOW only adds non-live leave-zone schedule metadata and tests, without enabling live scheduler dispatch, packet sending, passed-player mutation, participant mutation, or teleport/kick side effects.
- Broad .NET decision: skipped; focused validation built the affected project and covered the edited Vortex leave-zone metadata paths.
- Why this scope is sufficient: the new code is an inert planner over supplied player, active-state, race, passed-player, and inside-location snapshots.

## Additional Hygiene

```powershell
git diff --check
git diff --cached --check
```

- `git diff --check` passed. Git reported an expected CRLF conversion warning for the edited test file.
- `git diff --cached --check` passed after staging. Git reported expected CRLF conversion warnings for edited/new files.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.vortex.VortexLocation.onLeaveZone` | `Aion.GameServer.Services.VortexZoneLeaveKickSchedulePlanService` | Runtime planner | Partial | Unit Tested | Partial Parity | C# models player leave-zone removal and kick-schedule branches as metadata. It does not mutate live zone, scheduler, packet, or participant state. |
| `com.aionemu.gameserver.utils.PacketSendUtility.sendPacket` | `Aion.GameServer.Services.VortexZoneLeaveKickSchedulePlan` | Side-effect intent metadata | Partial | Unit Tested | Partial Parity | C# records invader battlefield-left message id `904305`; live packet dispatch is disabled. |
| `com.aionemu.commons.utils.concurrent.ThreadPoolManager.schedule` | `Aion.GameServer.Services.VortexZoneLeaveKickSchedulePlan` | Side-effect intent metadata | Partial | Unit Tested | Partial Parity | C# records Java's 10-second kick schedule and online/outside-active guard; live scheduling is disabled. |
| `com.aionemu.gameserver.services.vortex.Invasion.kickPlayer` | `Aion.GameServer.Services.VortexZoneLeaveKickSchedulePlan` | Side-effect intent metadata | Partial | Unit Tested | Partial Parity | C# records invader/defender kick selection only. Existing runtime kick helpers remain separate and live scheduler dispatch is not enabled. |

## Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 1
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 41%

## Remaining Risks

- Live Vortex leave-zone scheduler dispatch, packet dispatch, and participant kick side effects remain disabled.
- Production zone-player sourcing for the Java `players` map gate remains absent.
- The scheduled online/outside-active check is metadata only and does not re-evaluate live zone membership.
- Kisk `onEnterZone`/`onLeaveZone` membership behavior is not yet modeled.
