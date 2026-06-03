# Phase 6 Session 2499 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2499: Compose Vortex start defender invitation batch metadata

## Commits Made

- `[Phase 6][UOW-2499] Compose Vortex defender invitation batch`

## Summary

UOW-2499 added non-live batch composition for Java `Invasion.updateAlliance -> updateDefenders`. A prepared start request can now carry one defender invitation metadata plan per exact defender-race zone player, including Java-shaped branch counts for normal question-window intent, request-not-stored, already-defender skip, and full-alliance skip.

This remains partial parity. The path records intent only; it does not store live response requests, send `SM_QUESTION_WINDOW`, execute acceptance callbacks, mutate groups, mutate alliances, mutate defender participants, despawn NPCs, spawn NPCs, start portals, or schedule stops.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/VortexDefenderAllianceUpdatePlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexStartInvasionRuntimeSnapshotCollectorService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexStartInvasionCoordinatorService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexStartInvasionSideEffectPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`
- `docs/Phase-6-Session-2499-Completion.md`
- `docs/Phase-6-Session-2499-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.vortex.Invasion.startInvasion`
- `com.aionemu.gameserver.services.vortex.Invasion.updateAlliance`
- `com.aionemu.gameserver.services.vortex.Invasion.updateDefenders`

## C# Artifacts Touched

- `Aion.GameServer.Services.VortexDefenderInvitationBatchPlanService`
- `Aion.GameServer.Services.VortexDefenderInvitationBatchPlan`
- `Aion.GameServer.Services.VortexDefenderUpdateDefendersPlan`
- `Aion.GameServer.Services.VortexStartInvasionSnapshotRequest`
- `Aion.GameServer.Services.VortexStartInvasionRuntimeSnapshotCollectorService`
- `Aion.GameServer.Services.VortexStartInvasionSideEffectPlan`

## Validation Completed

Validation target: C# Vortex start defender-alliance metadata composes Java `updateDefenders` invitation intent for each exact defender-race zone player while keeping live request, packet, acceptance, group, alliance, defender-map, scheduler, spawn, despawn, and portal side effects disabled.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests" --no-restore
```

- Passed: 100 tests.
- Existing nullable/analyzer warnings were emitted from unrelated files.

```powershell
git diff --check
git diff --cached --check
```

- `git diff --check` passed. Git reported expected CRLF conversion warnings for edited C# source and test files.
- `git diff --cached --check` passed.

Java/Maven validation was skipped because no Java source or fixtures changed and no narrow Java Vortex defender-invitation fixture exists. Java source was reviewed directly. Broad-validation trigger was `none` because this UOW only adds non-live metadata/adapters and tests.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.vortex.Invasion.updateAlliance` | `Aion.GameServer.Services.VortexDefenderInvitationBatchPlanService` | Runtime metadata service | Partial | Unit Tested | Partial Parity | C# composes one invitation metadata plan per defender-race candidate from the prepared update-alliance scan. Live request and packet side effects remain disabled. |
| `com.aionemu.gameserver.services.vortex.Invasion.updateDefenders` | `Aion.GameServer.Services.VortexDefenderUpdateDefendersPlan` | Runtime metadata | Partial | Unit Tested | Partial Parity | C# batch composition preserves existing-defender skip, full-alliance skip, request-slot failure, and question-window intent branches. Acceptance callback remains metadata-only. |
| `com.aionemu.gameserver.services.vortex.Invasion.startInvasion` | `Aion.GameServer.Services.VortexStartInvasionSideEffectPlan` | Runtime coordinator metadata | Partial | Unit Tested | Partial Parity | C# start side-effect plan carries defender invitation batch counts in Java start ordering. Live despawn, spawn, portal, request, alliance, group, defender-map, and scheduler side effects remain disabled. |

## Known Gaps

- Live Vortex `start`, `spawn`, `despawn`, and `stop` behavior remains unimplemented.
- Defender request storage, packet dispatch, request-handler callbacks, acceptance flow, add-player transition, and alliance mutation remain metadata-only.
- Invader participant and alliance mutation remain metadata-only.
- Start and stop prepared requests still depend on supplied candidate collections rather than production world/location/alliance/Kisk/spawn containers.
- Full production XML coverage for Vortex spawns has not been compared against Java output.

## Remaining Risks

- Future live defender invitation work must preserve Java ordering: existing defender guard, alliance fullness guard, request handler creation, request storage, then question-window packet only when storage succeeds.
- Future acceptance work must preserve Java's second alliance-fullness gate before `addPlayer(responder, false)`.
- Current batch metadata assumes caller-supplied request-slot and alliance snapshots; production response-requester/alliance adapters are still absent.
- Live request storage or packet dispatch would be a broad-validation trigger.

## Next Recommended UOW

[Phase 6] UOW-2500: Discover Vortex defender invitation production adapter inputs

The next smallest safe task is to inspect Java `Player.getResponseRequester`, `RequestResponseHandler`, and `SM_QUESTION_WINDOW` usage against current C# player/request/packet surfaces, then identify or add the first inert production adapter inputs needed to supply defender request-slot availability to `VortexDefenderInvitationBatchPlanService`. Prefer an implementation slice that captures request-slot metadata without storing live requests or sending packets.

Suggested files to inspect:

- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/RequestResponseHandler.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/ResponseRequester.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_QUESTION_WINDOW.java`
- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/Player.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexDefenderInvitationPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexDefenderAllianceUpdatePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

Suggested validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests" --no-restore
```

Validation target: C# Vortex defender invitation adapter metadata captures Java request-slot availability and question-window intent while keeping live request storage, packet dispatch, acceptance, group, alliance, defender-map, scheduler, spawn, despawn, and portal side effects disabled.

Java/Maven is not expected unless Java source or fixtures change or a narrow Java Vortex defender-request fixture is added. Broad-validation trigger should be `none` if the UOW only adds non-live metadata/adapters and tests; it becomes present if live packet dispatch, request storage, scheduler dispatch, alliance mutation, group mutation, participant mutation, portal spawn, NPC despawn, or NPC spawn is enabled.

Safe alternative candidates:

- Add a narrow Java fixture/golden for defender invitation branch metadata if a suitable Java seam is available.
- Continue production Vortex lifecycle adapter discovery for real world/location/alliance/Kisk/spawn inputs.
- Compose acceptance-batch metadata only if request-slot adapter discovery does not expose a safe implementation seam.

## Context Needed By Next Session

- Java `updateDefenders` returns immediately for existing defenders.
- Java stores request id `904306` only if defender alliance is missing or not full.
- Java sends `SM_QUESTION_WINDOW(904306, 0, 0)` only when `putRequest(904306, responseHandler)` returns true.
- Existing C# plans remain metadata-only and must not enable live side effects without a separate broad validation decision.
