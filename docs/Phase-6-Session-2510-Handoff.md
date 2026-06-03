# Phase 6 Session 2510 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2510: Add guarded Vortex update-alliance runtime adapter

## Commits Made

- `[Phase 6][UOW-2510] Add guarded Vortex update-alliance runtime adapter`

## Summary

UOW-2510 added `VortexDefenderAllianceUpdateRuntimeAdapterService`, an opt-in adapter that accepts a Vortex location summary plus supplied live `Player` candidates, snapshots them for the existing update-alliance planner, and invokes guarded batch defender registration only for players whose race exactly matches the Java defender race check.

The adapter preserves the reviewed Java `Invasion.updateAlliance` shape: iterate location players, compare `player.getRace().equals(getVortexLocation().getDefendersRace())`, and call `updateDefenders` for selected defenders. The C# path then reuses the previously guarded batch registration and non-sent question-window intent adapters.

This remains partial parity. The adapter composes runtime metadata and response-registry storage only. It does not own production `VortexLocation.getPlayers()` lookup, send packets, execute live callbacks, mutate groups, mutate alliances, mutate defender participants, despawn NPCs, spawn NPCs, start portals, or schedule stops.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/VortexDefenderAllianceUpdatePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`
- `docs/Phase-6-Session-2510-Completion.md`
- `docs/Phase-6-Session-2510-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.vortex.Invasion.updateAlliance`
- `com.aionemu.gameserver.services.vortex.Invasion.updateDefenders`
- `com.aionemu.gameserver.model.vortex.VortexLocation.getPlayers`
- `com.aionemu.gameserver.model.gameobjects.player.Player.getRace`
- `com.aionemu.gameserver.model.gameobjects.player.ResponseRequester.putRequest`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW`

## C# Artifacts Touched

- `Aion.GameServer.Services.VortexDefenderAllianceUpdateRuntimeAdapterService`
- `Aion.GameServer.Services.VortexDefenderAllianceUpdateRuntimeReport`
- `Aion.GameServer.Services.VortexDefenderAllianceUpdatePlanService`
- `Aion.GameServer.Services.VortexDefenderInvitationBatchRuntimeAdapterService`
- `Aion.GameServer.Services.VortexDefenderUpdateDefendersRegistrationRuntimeAdapterService`
- `Aion.GameServer.Services.VortexDefenderQuestionWindowIntentAdapterService`
- `Aion.GameServer.Model.GameObjects.Player`
- `Aion.GameServer.Network.Aion.QuestionResponseRegistry`

## Validation Completed

Validation target: C# guarded update-alliance runtime adapter filters live player candidates to exact defender-race players, invokes guarded batch registration for those defenders only, preserves already-defender/full-alliance/occupied-slot outcomes, and keeps packet dispatch, callback execution, group, alliance, defender-map, scheduler, spawn, despawn, and portal side effects disabled.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~QuestionResponseRegistryTests" --no-restore
```

- Passed: 125 tests.
- Existing nullable/analyzer warnings were emitted from unrelated files and prior test lines.

Java/Maven validation was skipped because no Java source or fixtures changed and no narrow Java update-alliance fixture exists for this seam. Broad-validation trigger was `none` because this adapter is opt-in and does not send packets or wire live lifecycle dispatch; full project/solution validation was skipped.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.vortex.Invasion.updateAlliance` | `Aion.GameServer.Services.VortexDefenderAllianceUpdateRuntimeAdapterService` | Runtime Vortex update-alliance adapter | Partial | Unit Tested | Partial Parity | C# now filters supplied live `Player` candidates by exact defender race and invokes guarded batch registration for matching defenders. Production `VortexLocation.getPlayers()` container wiring, live lifecycle dispatch, packet sending, and gameplay mutations remain disabled. |
| `com.aionemu.gameserver.services.vortex.Invasion.updateDefenders` | `Aion.GameServer.Services.VortexDefenderInvitationBatchRuntimeAdapterService` / `Aion.GameServer.Services.VortexDefenderUpdateDefendersRegistrationRuntimeAdapterService` | Runtime Vortex defender registration adapter | Partial | Unit Tested | Partial Parity | C# update-alliance runtime path reuses guarded update-defenders registration and packet-intent adapters. Live callback execution and defender/alliance mutations remain disabled. |
| `com.aionemu.gameserver.model.vortex.VortexLocation.getPlayers` | `IReadOnlyList<Aion.GameServer.Model.GameObjects.Player>` input to `VortexDefenderAllianceUpdateRuntimeAdapterService` | Runtime location player source | Partial | Unit Tested | Partial Parity | C# accepts supplied location players rather than reading a production Vortex location container. Collection map semantics and production lifecycle integration remain unimplemented. |
| `com.aionemu.gameserver.model.gameobjects.player.Player.getRace` | `Aion.GameServer.Model.GameObjects.Player.Race` / `VortexZonePlayerSnapshot.Race` | Player state field | Partial | Unit Tested | Partial Parity | Exact ordinal string matching is covered for matching defender race and lower-case non-match. Broader Java race enum/object semantics remain outside this seam. |

## Known Gaps

- Live Vortex `start`, `spawn`, `despawn`, and `stop` behavior remains unimplemented.
- The update-alliance runtime adapter does not yet read production `VortexLocation.getPlayers().values()` or map semantics directly.
- Defender invitation registration and question-window creation are available through opt-in adapters and are not wired into production Vortex lifecycle.
- `SM_QUESTION_WINDOW` dispatch remains disabled.
- Vortex response-handler callbacks, acceptance flow, add-player transition, and alliance mutation remain metadata-only.
- Invader participant and alliance mutation remain metadata-only.
- Start and stop prepared requests still depend on supplied candidate collections rather than production world/location/alliance/Kisk/spawn containers.

## Remaining Risks

- Future live defender invitation wiring must preserve Java ordering: location-player scan, defender-race filter, existing defender guard, alliance fullness guard, handler/request payload creation, request storage, then question-window packet only when storage succeeds.
- Future production location wiring must account for Java's `Map<Integer, Player>` player container semantics.
- Future live acceptance handling must preserve Java branch priority: group removal before alliance removal, then add defender only if the defender alliance is still open.
- Enabling live packet dispatch or gameplay mutation is a broad-validation trigger.

## Next Recommended UOW

[Phase 6] UOW-2511: Add guarded Vortex start-invasion runtime defender update report

The next concrete task is to add an opt-in start-invasion runtime adapter/report that accepts the existing start result, supplied location players, existing defenders, and defender-alliance snapshot, invokes `VortexDefenderAllianceUpdateRuntimeAdapterService`, and carries the runtime defender update report into start-invasion side-effect metadata. Keep packet sending and live lifecycle mutation disabled.

Suggested files to inspect:

- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`
- `dotnetConversion/src/Aion.GameServer/Services/VortexStartInvasionSideEffectPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexStartInvasionPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexDefenderAllianceUpdatePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

Suggested validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~QuestionResponseRegistryTests" --no-restore
```

Validation target: C# guarded start-invasion runtime report follows Java `startInvasion` ordering through the update-alliance step, exposes defender update runtime metadata from supplied location players, preserves exact defender-race filtering and registration guard outcomes, and keeps packet dispatch, callback execution, group, alliance, defender-map, scheduler, spawn, despawn, and portal side effects disabled.

Java/Maven is not expected unless Java source or fixtures change or a narrow Java Vortex start-invasion/update-alliance fixture is added. Broad-validation trigger should remain `none` if the adapter is opt-in and does not send packets or execute live lifecycle side effects; if it wires live packet sending or production lifecycle dispatch, packet dispatch/live side effects become a broad-validation trigger.

Safe alternative candidates:

- Add a narrow Java fixture/golden for `Invasion.updateAlliance` or `Invasion.startInvasion` if a suitable Java harness seam exists.
- Continue production Vortex lifecycle adapter discovery for real world/location/alliance/Kisk/spawn inputs.
- Add focused runtime consumption tests for defender invitation response acceptance once a non-live start report can carry the full defender invitation metadata.

## Context Needed By Next Session

- Java `Invasion.startInvasion` calls `setActiveVortex`, `despawn`, `spawn(INVASION)`, `initRiftGenerator`, then `updateAlliance`.
- Java `Invasion.updateAlliance` loops `getVortexLocation().getPlayers().values()` and calls `updateDefenders` for players whose race exactly equals the location defender race.
- Java `Invasion.updateDefenders` creates a `RequestResponseHandler`, calls `defender.getResponseRequester().putRequest(904306, responseHandler)`, then sends `SM_QUESTION_WINDOW(904306, 0, 0)` only when registration succeeds.
- C# `VortexDefenderAllianceUpdateRuntimeAdapterService` now filters supplied live `Player` candidates through `VortexDefenderAllianceUpdatePlanService` and sends selected defenders to `VortexDefenderInvitationBatchRuntimeAdapterService`.
- C# `VortexDefenderInvitationBatchRuntimeAdapterService` applies registration plus packet-intent adapters per supplied defender `Player`.
- C# `VortexDefenderUpdateDefendersRegistrationRuntimeAdapterService` snapshots live player request slots, composes invitation planning, and delegates actual pending-request storage.
- C# `VortexDefenderQuestionWindowIntentAdapterService` creates non-sent `SmQuestionWindow` intent only after registration succeeds.
- C# response routing for Vortex question id `904306` remains live in `GameServerConnection.HandleQuestionResponseAsync` for pending-request removal and metadata reporting.
