# Phase 6 Session 2511 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2511: Add guarded Vortex start-invasion runtime defender update report

## Commits Made

- `[Phase 6][UOW-2511] Add guarded Vortex start defender runtime report`

## Summary

UOW-2511 added an opt-in start-invasion coordinator path, `StartInvasionWithRuntimeDefenderUpdate`, that invokes `VortexDefenderAllianceUpdateRuntimeAdapterService` only after the Java-shaped start guard succeeds. The resulting `VortexDefenderAllianceUpdateRuntimeReport` is carried on `VortexStartInvasionSideEffectPlan` with runtime defender counts.

This models the reviewed Java ordering through the defender update step: `DimensionalVortex.start` guard, `Invasion.startInvasion`, then `updateAlliance`. Duplicate starts skip runtime defender update work and do not store defender requests.

This remains partial parity. The path can store pending defender invitation requests through the guarded response registry adapter, but it does not send packets, execute live callbacks, mutate groups, mutate alliances, mutate defender participants, despawn NPCs, spawn NPCs, start portals, or schedule stops.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/VortexStartInvasionCoordinatorService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexStartInvasionSideEffectPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`
- `docs/Phase-6-Session-2511-Completion.md`
- `docs/Phase-6-Session-2511-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.vortex.DimensionalVortex.start`
- `com.aionemu.gameserver.services.vortex.Invasion.startInvasion`
- `com.aionemu.gameserver.services.vortex.Invasion.updateAlliance`
- `com.aionemu.gameserver.services.vortex.Invasion.updateDefenders`
- `com.aionemu.gameserver.model.vortex.VortexLocation.getPlayers`
- `com.aionemu.gameserver.model.gameobjects.player.ResponseRequester.putRequest`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW`

## C# Artifacts Touched

- `Aion.GameServer.Services.VortexStartInvasionCoordinatorService`
- `Aion.GameServer.Services.VortexStartInvasionSideEffectPlan`
- `Aion.GameServer.Services.VortexDefenderAllianceUpdateRuntimeAdapterService`
- `Aion.GameServer.Services.VortexDefenderInvitationBatchRuntimeAdapterService`
- `Aion.GameServer.Services.VortexDefenderUpdateDefendersRegistrationRuntimeAdapterService`
- `Aion.GameServer.Network.Aion.QuestionResponseRegistry`

## Validation Completed

Validation target: C# guarded start-invasion runtime report follows Java `startInvasion` ordering through the update-alliance step, exposes defender update runtime metadata from supplied location players, preserves exact defender-race filtering and registration guard outcomes, skips update work when the Java start guard fails, and keeps packet dispatch, callback execution, group, alliance, defender-map, scheduler, spawn, despawn, and portal side effects disabled.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~QuestionResponseRegistryTests" --no-restore
```

- Passed: 127 tests.
- Existing nullable/analyzer warnings were emitted from unrelated files and prior test lines.

Java/Maven validation was skipped because no Java source or fixtures changed and no narrow Java start-invasion/update-alliance fixture exists for this seam. Broad-validation trigger was `none` because this adapter is opt-in and does not send packets or execute live lifecycle side effects; full project/solution validation was skipped.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.vortex.DimensionalVortex.start` | `Aion.GameServer.Services.VortexInvasionRuntime.StartInvasionWithResult` / `VortexStartInvasionCoordinatorService.StartInvasionWithRuntimeDefenderUpdate` | Runtime start guard | Partial | Unit Tested | Partial Parity | C# duplicate-start guard prevents opt-in runtime defender update invocation. Java atomic/finished lifecycle and production service map wiring remain partial. |
| `com.aionemu.gameserver.services.vortex.Invasion.startInvasion` | `Aion.GameServer.Services.VortexStartInvasionCoordinatorService.StartInvasionWithRuntimeDefenderUpdate` / `VortexStartInvasionSideEffectPlan` | Runtime start-invasion report | Partial | Unit Tested | Partial Parity | C# carries runtime defender update metadata after the start guard and preserves non-live start step ordering. Live active-vortex, despawn, spawn, rift-generator, scheduler, and packet dispatch execution remain disabled. |
| `com.aionemu.gameserver.services.vortex.Invasion.updateAlliance` | `Aion.GameServer.Services.VortexDefenderAllianceUpdateRuntimeAdapterService` carried by `VortexStartInvasionSideEffectPlan` | Runtime defender update metadata | Partial | Unit Tested | Partial Parity | C# start report exposes runtime update-alliance selected/skipped ids and registration counts from supplied players. Production `VortexLocation.getPlayers()` lookup remains unimplemented. |
| `com.aionemu.gameserver.services.vortex.Invasion.updateDefenders` | `Aion.GameServer.Services.VortexDefenderInvitationBatchRuntimeAdapterService` / `VortexDefenderUpdateDefendersRegistrationRuntimeAdapterService` | Runtime defender registration adapter | Partial | Unit Tested | Partial Parity | C# opt-in start path can store pending request metadata for selected defenders while packet sending, callback execution, and defender/alliance mutation remain disabled. |

## Known Gaps

- Live Vortex `start`, `spawn`, `despawn`, and `stop` behavior remains unimplemented.
- Start runtime defender update accepts supplied location players and does not yet read production `VortexLocation.getPlayers().values()` directly.
- `SM_QUESTION_WINDOW` dispatch remains disabled.
- Vortex response-handler callbacks, acceptance flow, add-player transition, and alliance mutation remain metadata-only.
- Invader participant and alliance mutation remain metadata-only.
- Start and stop prepared requests still depend on supplied candidate collections rather than production world/location/alliance/Kisk/spawn containers.

## Remaining Risks

- Future live defender invitation wiring must preserve Java ordering: start guard, location-player scan, defender-race filter, existing defender guard, alliance fullness guard, handler/request payload creation, request storage, then question-window packet only when storage succeeds.
- Future production location wiring must account for Java's `Map<Integer, Player>` player container semantics.
- Future live acceptance handling must preserve Java branch priority: group removal before alliance removal, then add defender only if the defender alliance is still open.
- Enabling live packet dispatch or gameplay mutation is a broad-validation trigger.

## Next Recommended UOW

[Phase 6] UOW-2512: Add guarded Vortex defender invitation acceptance transition runtime report

The next concrete task is to extend the existing defender invitation response-consumption path with an opt-in runtime report that carries accepted-response metadata through the Java `acceptRequest` branch into `VortexDefenderUpdateDefendersPlanService.CreateAcceptancePlan` and `VortexDefenderAddPlayerTransitionPlanService`, without mutating live group/alliance/defender state. Preserve denial/missing/non-vortex behavior as no-op metadata.

Suggested files to inspect:

- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`
- `dotnetConversion/src/Aion.GameServer/Services/VortexDefenderInvitationAcceptancePlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexDefenderUpdateDefendersPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexDefenderAddPlayerTransitionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

Suggested validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~QuestionResponseRegistryTests" --no-restore
```

Validation target: C# guarded defender invitation acceptance runtime report consumes accepted Vortex question responses, composes Java-equivalent group/alliance removal intent and add-defender transition metadata, preserves full-alliance/denied/missing-request outcomes, and keeps live group, alliance, defender-map, packet, scheduler, spawn, despawn, and portal side effects disabled.

Java/Maven is not expected unless Java source or fixtures change or a narrow Java Vortex response-handler fixture is added. Broad-validation trigger should remain `none` if the adapter is opt-in and does not send packets or execute live gameplay side effects; if it mutates live group/alliance/defender state, gameplay mutation becomes a broad-validation trigger.

Safe alternative candidates:

- Add a narrow Java fixture/golden for `RequestResponseHandler.acceptRequest` branch ordering if a suitable Java harness seam exists.
- Continue production Vortex lifecycle adapter discovery for real world/location/alliance/Kisk/spawn inputs.
- Add a non-live observer that ties start report defender invitations to later response acceptance reports.

## Context Needed By Next Session

- Java `Invasion.startInvasion` calls `setActiveVortex`, `despawn`, `spawn(INVASION)`, `initRiftGenerator`, then `updateAlliance`.
- Java `Invasion.updateAlliance` loops `getVortexLocation().getPlayers().values()` and calls `updateDefenders` for players whose race exactly equals the location defender race.
- Java `Invasion.updateDefenders` creates a `RequestResponseHandler`, calls `defender.getResponseRequester().putRequest(904306, responseHandler)`, then sends `SM_QUESTION_WINDOW(904306, 0, 0)` only when registration succeeds.
- Java `RequestResponseHandler.acceptRequest` removes the responder from group first, otherwise alliance, then calls `addPlayer(responder, false)` only if the defender alliance is still missing/open.
- C# `StartInvasionWithRuntimeDefenderUpdate` now runs runtime defender update only after start succeeds and carries the result through `VortexStartInvasionSideEffectPlan`.
- C# `VortexDefenderInvitationResponseRuntimeAdapterService` consumes pending request responses and returns dispatch metadata but does not yet expose the full acceptance/add-player transition as a runtime report.
- C# `VortexDefenderUpdateDefendersPlanService.CreateAcceptancePlan` already composes acceptance and add-player transition metadata for the next unit.
