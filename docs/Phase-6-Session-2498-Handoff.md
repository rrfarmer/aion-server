# Phase 6 Session 2498 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2498: Prepare Vortex startInvasion alliance update metadata

## Commits Made

- `[Phase 6][UOW-2498] Prepare Vortex start metadata`

## Summary

UOW-2498 added a non-live Vortex start snapshot collector and threaded prepared defender-alliance update metadata through the start coordinator side-effect report. The C# start path can now consume Java-shaped pre-start runtime/static inputs: existing spawned NPCs, static INVASION spawn rows, and current zone players filtered by exact defender race for `Invasion.updateAlliance`.

This remains partial parity. The path records intent only; it does not install defender request handlers, send question-window packets, mutate groups, mutate alliances, mutate defender participants, despawn NPCs, spawn NPCs, start portals, or schedule stops.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/VortexStartInvasionRuntimeSnapshotCollectorService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexStartInvasionCoordinatorService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexStartInvasionSideEffectPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`
- `docs/Phase-6-Session-2498-Completion.md`
- `docs/Phase-6-Session-2498-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.vortex.Invasion.startInvasion`
- `com.aionemu.gameserver.services.vortex.Invasion.updateAlliance`
- `com.aionemu.gameserver.services.VortexService.spawn`

## C# Artifacts Touched

- `Aion.GameServer.Services.VortexStartInvasionRuntimeSnapshotCollectorService`
- `Aion.GameServer.Services.VortexStartInvasionSnapshotRequest`
- `Aion.GameServer.Services.VortexStartInvasionCoordinatorService`
- `Aion.GameServer.Services.VortexStartInvasionSideEffectPlan`
- `Aion.GameServer.Services.VortexDefenderAllianceUpdatePlan`

## Validation Completed

Validation target: C# startInvasion metadata captures Java `startInvasion -> updateAlliance` runtime/static inputs while keeping live alliance, request, packet, spawn, despawn, scheduler, and portal side effects disabled.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests" --no-restore
```

- Passed: 98 tests.
- Existing nullable/analyzer warnings were emitted from unrelated files.

```powershell
git diff --check
git diff --cached --check
```

- `git diff --check` passed. Git reported expected CRLF conversion warnings for edited C# source and test files.
- `git diff --cached --check` passed.

Java/Maven validation was skipped because no Java source or fixtures changed and no narrow Java Vortex start/alliance fixture exists. Java source was reviewed directly. Broad-validation trigger was `none` because this UOW only adds non-live metadata/adapters and tests.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.vortex.Invasion.startInvasion` | `Aion.GameServer.Services.VortexStartInvasionRuntimeSnapshotCollectorService` | Runtime metadata adapter | Partial | Unit Tested | Partial Parity | C# prepares pre-start spawned NPCs, static INVASION spawns, and defender-alliance update metadata. Live despawn, spawn, portal, request, alliance, group, defender-map, and scheduler side effects remain disabled. |
| `com.aionemu.gameserver.services.vortex.Invasion.updateAlliance` | `Aion.GameServer.Services.VortexDefenderAllianceUpdatePlan` | Runtime metadata | Partial | Unit Tested | Partial Parity | C# preserves exact defender-race filtering from current zone players and carries the prepared plan through start coordination. Per-defender request-handler composition remains a later step. |
| `com.aionemu.gameserver.services.VortexService.spawn` | `Aion.GameServer.Services.VortexStartInvasionSnapshotRequest` | Runtime metadata | Partial | Unit Tested | Partial Parity | C# carries selected static INVASION spawn rows in the prepared request. Live spawn remains disabled. |

## Known Gaps

- Live Vortex `start`, `spawn`, `despawn`, and `stop` behavior remains unimplemented.
- Defender request storage, packet dispatch, request-handler callbacks, acceptance flow, add-player transition, and alliance mutation remain metadata-only.
- Invader participant and alliance mutation remain metadata-only.
- Start and stop prepared requests still depend on supplied candidate collections rather than production world/location/alliance/Kisk/spawn containers.
- Full production XML coverage for Vortex spawns has not been compared against Java output.

## Remaining Risks

- Future live start work must preserve Java ordering: active Vortex set, despawn, INVASION spawn, rift generator initialization, then defender alliance update.
- Future defender invitation work must preserve Java's double gate: skip existing defenders, require non-full alliance before storing request, and check alliance fullness again on acceptance before `addPlayer`.
- Current start metadata does not yet compose one invitation/updateDefenders plan per defender update candidate.
- Exact request-slot behavior still depends on future production response-requester adapters.

## Next Recommended UOW

[Phase 6] UOW-2499: Compose Vortex start defender invitation batch metadata

The next smallest safe task is to inspect Java `Invasion.updateDefenders` and existing C# `VortexDefenderUpdateDefendersPlanService`, then add a non-live batch plan that turns a prepared `VortexDefenderAllianceUpdatePlan` into per-defender invitation metadata. Keep live request storage, `SM_QUESTION_WINDOW` dispatch, acceptance callbacks, group/alliance mutation, and defender participant mutation disabled.

Suggested files to inspect:

- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`
- `dotnetConversion/src/Aion.GameServer/Services/VortexDefenderAllianceUpdatePlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexDefenderUpdateDefendersPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexDefenderInvitationPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexStartInvasionSideEffectPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

Suggested validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests" --no-restore
```

Validation target: C# Vortex start defender-alliance metadata composes Java `updateDefenders` invitation intent for each exact defender-race zone player while keeping live request, packet, acceptance, group, alliance, defender-map, scheduler, spawn, despawn, and portal side effects disabled.

Java/Maven is not expected unless Java source or fixtures change or a narrow Java Vortex defender-invitation fixture is added. Broad-validation trigger should be `none` if the UOW only adds non-live metadata/adapters and tests; it becomes present if live packet dispatch, request storage, scheduler dispatch, alliance mutation, group mutation, participant mutation, portal spawn, NPC despawn, or NPC spawn is enabled.

## Context Needed By Next Session

- Java `Invasion.updateAlliance` scans `getVortexLocation().getPlayers().values()` and calls `updateDefenders(player)` only when `player.getRace().equals(getVortexLocation().getDefendersRace())`.
- Java `updateDefenders` returns immediately for existing defenders; otherwise it stores request id `904306` only if defender alliance is missing or not full, then sends `SM_QUESTION_WINDOW(904306, 0, 0)` when storage succeeds.
- Java acceptance removes the responder from group/alliance, then rechecks alliance fullness before `addPlayer(responder, false)`.
- Existing C# plans remain metadata-only and must not enable live side effects without a separate broad validation decision.
