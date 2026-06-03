# Phase 6 Session 2491 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2491: Compose Vortex invader updateInvaders/addPlayer metadata

## Commits Made

- `[Phase 6][UOW-2491] Compose Vortex invader update metadata`

## Summary

UOW-2491 added a non-live invader update composition planner for Java `Invasion.updateInvaders`. The C# planner exposes the Java entry-point guard and `addPlayer(player, true)` transition metadata while preserving disabled live invader participant, group, and alliance mutation.

This remains partial parity. Live participant maps, team mutation, alliance creation/add, warning emission, and production player/location adapters remain disabled.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/VortexInvaderUpdateInvadersPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`
- `docs/Phase-6-Session-2491-Completion.md`
- `docs/Phase-6-Session-2491-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.vortex.Invasion.updateInvaders`
- `com.aionemu.gameserver.services.vortex.Invasion.addPlayer(Player, boolean=true)`
- `com.aionemu.gameserver.model.team.alliance.PlayerAlliance`
- `com.aionemu.gameserver.model.team.TeamType.ALLIANCE_OFFENCE`

## C# Artifacts Touched

- `Aion.GameServer.Services.VortexInvaderUpdateInvadersPlanService`
- `Aion.GameServer.Services.VortexInvaderUpdateInvadersPlan`
- `Aion.GameServer.Services.VortexInvaderUpdateInvadersPlanStatus`

## Validation Completed

Validation target: C# invader update metadata composes Java-shaped `updateInvaders` and `addPlayer(player, true)` branches while preserving already-invader guard, alliance/group transition metadata, participant-put intent, and no-live-mutation flags.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests" --no-restore
```

- Passed: 86 tests.
- Existing nullable/analyzer warnings were emitted from unrelated files.

```powershell
git diff --check
git diff --cached --check
```

- `git diff --check` passed. Git reported an expected CRLF conversion warning for the edited test file.
- `git diff --cached --check` passed after staging. Git reported expected CRLF conversion warnings for edited/new files.

Java/Maven validation was skipped because no Java source or fixtures changed and no narrow Java Vortex invader-update fixture exists. Java source was reviewed directly. Broad-validation trigger was `none` because this UOW only adds non-live composition metadata and tests.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.vortex.Invasion.updateInvaders` | `Aion.GameServer.Services.VortexInvaderUpdateInvadersPlanService` | Runtime planner | Partial | Unit Tested | Partial Parity | C# composes already-invader guard and addPlayer invocation as metadata. Live invader participant mutation remains disabled. |
| `com.aionemu.gameserver.services.vortex.Invasion.addPlayer(Player, boolean=true)` | `Aion.GameServer.Services.VortexInvaderUpdateInvadersPlanService` / `VortexInvaderUpdateAddPlayerPlanService` | Runtime planner | Partial | Unit Tested | Partial Parity | C# composes invader add-player transition metadata, including offence-alliance creation and missing-alliance warning branches. Live team/alliance mutation remains disabled. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAlliance` | `Aion.GameServer.Services.VortexInvaderAllianceSnapshot` | Runtime metadata | Partial | Unit Tested | Partial Parity | C# records only existence/disbanded snapshots needed by invader update branches. Full alliance runtime behavior remains elsewhere. |
| `com.aionemu.gameserver.model.team.TeamType.ALLIANCE_OFFENCE` | `Aion.GameServer.Model.GameObjects.PlayerAllianceTeamType.AllianceOffence` | Runtime metadata | Partial | Unit Tested | Partial Parity | C# records offence-alliance creation intent only. Live alliance creation remains disabled. |

## Known Gaps

- Live Vortex `start`, `spawn`, `despawn`, and `stop` behavior remains unimplemented.
- Production Vortex creature/zone lifecycle adapter remains absent.
- Live Vortex zone-player and invader-Kisk membership maps remain disabled.
- Defender request storage, packet dispatch, request-handler callbacks, add-player transition, and alliance mutation remain metadata-only.
- Invader participant and alliance mutation remain metadata-only.
- Full production XML coverage for Vortex spawns has not been compared against Java output.

## Remaining Risks

- Future live invader update work must preserve Java's concurrent-map already-invader guard before `addPlayer`.
- Java participant map behavior is represented only by supplied snapshots.
- Java synchronization is represented only by C# runtime locks and guarded metadata so far.
- Current composition planners still need production adapters before they can observe real `Player`, alliance, and location state.

## Next Recommended UOW

[Phase 6] UOW-2492: Compose Vortex kickPlayer removal metadata

The next smallest safe task is to compose Java `Invasion.kickPlayer` into a non-live removal planner that covers participant removal, alliance-member removal/message intent, disbanded-alliance nulling intent, invader direct-portal teleport/message intent, passed-player removal, and `syncPassed(true)` metadata. Existing leave scheduling metadata already decides when `kickPlayer` should be scheduled; this UOW should expose what the kick does without enabling live packets, teleport, alliance mutation, passed-player mutation, or sync writes.

Safe alternative candidates:

- Add a production Vortex lifecycle adapter preview if real player/location snapshots become the next target.
- Compose Vortex `startInvasion` metadata if lifecycle start/stop orchestration becomes higher priority.

## Suggested Files To Inspect

- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`
- `dotnetConversion/src/Aion.GameServer/Services/VortexZoneLeaveKickSchedulePlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexRemovalRiftEntryUpdatePreviewService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexRemovalRiftEntryUpdateReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

## Suggested Validation

For Vortex kickPlayer removal metadata:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests" --no-restore
```

Validation target: C# kickPlayer removal metadata mirrors Java participant removal, alliance-member removal/message intent, disbanded-alliance nulling, invader direct-portal teleport/message intent, passed-player removal, `syncPassed(true)` intent, and no-live-mutation flags.

Java/Maven is not expected unless Java source or fixtures change or a narrow Java Vortex kick/removal fixture is added. Broad-validation trigger should be `none` if the UOW only adds non-live composition metadata and tests; it becomes present if live packet dispatch, teleport, alliance mutation, participant mutation, passed-player mutation, sync writes, scheduler dispatch, or zone-player/Kisk map mutation is enabled.
