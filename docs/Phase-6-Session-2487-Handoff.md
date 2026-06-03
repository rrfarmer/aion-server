# Phase 6 Session 2487 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2487: Add Vortex onLeaveZone kick-schedule metadata

## Commits Made

- `[Phase 6][UOW-2487] Add Vortex leave-zone kick metadata`

## Summary

UOW-2487 added non-live metadata for Java `VortexLocation.onLeaveZone` player scheduling behavior. The C# planner records the full-location-leave gate, zone-player removal intent, active-vortex gate, invader passed-player gate, invader battlefield-left message `904305`, 10-second kick schedule intent, scheduled online/outside-active guard, invader/defender kick selection, and no-live-side-effect flags.

This remains partial parity. Java Vortex zone-player storage, live packet dispatch, live scheduler dispatch, and live participant kick side effects remain unimplemented.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/VortexZoneLeaveKickSchedulePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`
- `docs/Phase-6-Session-2487-Completion.md`
- `docs/Phase-6-Session-2487-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.model.vortex.VortexLocation.onLeaveZone`
- `com.aionemu.gameserver.utils.PacketSendUtility.sendPacket`
- `com.aionemu.commons.utils.concurrent.ThreadPoolManager.schedule`
- `com.aionemu.gameserver.services.vortex.Invasion.kickPlayer`

## C# Artifacts Touched

- `Aion.GameServer.Services.VortexZoneLeaveKickSchedulePlanService`
- `Aion.GameServer.Services.VortexZoneLeaveKickSchedulePlan`
- `Aion.GameServer.Services.VortexZoneLeaveKickSchedulePlanStatus`
- `Aion.GameServer.Services.VortexZonePlayerSnapshot`

## Validation Completed

Validation target: C# Vortex leave-zone metadata records Java-shaped gating for active invasion, invader passed-player membership, defender leave scheduling, battlefield-left message intent, 10-second kick delay, and no live scheduler/packet/participant mutation.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests" --no-restore
```

- Passed: 71 tests.
- Existing nullable/analyzer warnings were emitted.

```powershell
git diff --check
git diff --cached --check
```

- Passed. Git reported expected CRLF conversion warnings for edited/new files.

Java/Maven validation was skipped because no Java source or fixtures changed and no narrow Java Vortex lifecycle/static-data fixture exists. Java source was reviewed directly. Broad-validation trigger was `none` because this UOW only adds non-live leave-zone kick-schedule metadata and tests.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.vortex.VortexLocation.onLeaveZone` | `Aion.GameServer.Services.VortexZoneLeaveKickSchedulePlanService` | Runtime planner | Partial | Unit Tested | Partial Parity | C# models player leave-zone removal and kick-schedule branches as metadata. It does not mutate live zone, scheduler, packet, or participant state. |
| `com.aionemu.gameserver.utils.PacketSendUtility.sendPacket` | `Aion.GameServer.Services.VortexZoneLeaveKickSchedulePlan` | Side-effect intent metadata | Partial | Unit Tested | Partial Parity | C# records invader battlefield-left message id `904305`; live packet dispatch is disabled. |
| `com.aionemu.commons.utils.concurrent.ThreadPoolManager.schedule` | `Aion.GameServer.Services.VortexZoneLeaveKickSchedulePlan` | Side-effect intent metadata | Partial | Unit Tested | Partial Parity | C# records Java's 10-second kick schedule and online/outside-active guard; live scheduling is disabled. |
| `com.aionemu.gameserver.services.vortex.Invasion.kickPlayer` | `Aion.GameServer.Services.VortexZoneLeaveKickSchedulePlan` | Side-effect intent metadata | Partial | Unit Tested | Partial Parity | C# records invader/defender kick selection only. Existing runtime kick helpers remain separate and live scheduler dispatch is not enabled. |

## Known Gaps

- Live Vortex `start`, `spawn`, `despawn`, and `stop` behavior remains unimplemented.
- Vortex zone-player and invader-kisk membership sourcing remains metadata-only or absent.
- Defender request storage, packet dispatch, invitation acceptance, add-player transition, and alliance mutation remain metadata-only.
- Invader participant and alliance mutation remain metadata-only.
- Full production XML coverage for Vortex spawns has not been compared against Java output.

## Remaining Risks

- Future live leave-zone work must re-check online and outside-active-vortex state at schedule execution time, not at schedule creation time.
- Java scheduler and packet behavior are represented only as metadata in this slice.
- Java synchronization is represented only by C# runtime locks and guarded metadata so far.
- Kisk zone membership remains a gap in `VortexLocation.onEnterZone/onLeaveZone` coverage.

## Next Recommended UOW

[Phase 6] UOW-2488: Add Vortex invader-kisk zone membership metadata

The next smallest safe task is to model the remaining Java `VortexLocation.onEnterZone/onLeaveZone` non-player branch for Kisks. Java records a Kisk in `kisks` only when the entering creature is a `Kisk` whose race equals the invader race, and removes Kisk ids after the creature is no longer inside any Vortex zone. Add a non-live plan that records object id, race branch, inside-location gate for leave, add/remove membership intent, and no-live-map mutation.

Safe alternative candidates:

- Compose the existing zone-entry and leave-zone player planners into a single non-live `VortexLocation` lifecycle plan if production zone-player sourcing becomes available.
- Compose defender invitation/acceptance/addPlayer planners into one non-live updateDefenders pipeline if defender-side live work becomes the next target.

## Suggested Files To Inspect

- `game-server/src/com/aionemu/gameserver/model/vortex/VortexLocation.java`
- `dotnetConversion/src/Aion.GameServer/Services/VortexZoneLeaveKickSchedulePlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexInvaderPassedPortalUpdatePlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexDefenderZoneEntryUpdatePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

## Suggested Validation

For Vortex invader-kisk zone membership metadata:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests" --no-restore
```

Validation target: C# Vortex Kisk zone metadata records Java-shaped gating for invader-race Kisk enter, non-invader Kisk skip, leave removal only after fully outside location, and no live Kisk-map mutation.

Java/Maven is not expected unless Java source or fixtures change or a narrow Java Vortex lifecycle fixture is added. Broad-validation trigger should be `none` if the UOW only adds non-live Kisk membership metadata and tests; it becomes present if live Kisk map mutation or death/despawn side effects are enabled.
