# Phase 6 Session 2509 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2509: Add guarded Vortex defender batch registration runtime adapter

## Commits Made

- `[Phase 6][UOW-2509] Add guarded Vortex defender batch registration adapter`

## Summary

UOW-2509 added `VortexDefenderInvitationBatchRuntimeAdapterService`, an opt-in adapter that accepts defender `Player` instances, existing defender snapshots, and a defender-alliance snapshot, then applies the guarded single-defender registration runtime adapter plus the non-sent question-window intent adapter to each supplied defender.

The adapter preserves the Java ordering modeled from `Invasion.updateAlliance -> Invasion.updateDefenders`: candidate defender, already-defender guard, alliance fullness guard, pending request storage, and question-window intent only after storage succeeds.

This remains partial parity. The adapter composes runtime metadata and response-registry storage only. It does not scan production Vortex location players, send packets, execute live callbacks, mutate groups, mutate alliances, mutate defender participants, despawn NPCs, spawn NPCs, start portals, or schedule stops.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/VortexDefenderAllianceUpdatePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`
- `docs/Phase-6-Session-2509-Completion.md`
- `docs/Phase-6-Session-2509-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.vortex.Invasion.updateAlliance`
- `com.aionemu.gameserver.services.vortex.Invasion.updateDefenders`
- `com.aionemu.gameserver.model.gameobjects.player.ResponseRequester.putRequest`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW`

## C# Artifacts Touched

- `Aion.GameServer.Services.VortexDefenderInvitationBatchRuntimeAdapterService`
- `Aion.GameServer.Services.VortexDefenderInvitationBatchRuntimeReport`
- `Aion.GameServer.Services.VortexDefenderInvitationBatchRuntimePlayerReport`
- `Aion.GameServer.Services.VortexDefenderUpdateDefendersRegistrationRuntimeAdapterService`
- `Aion.GameServer.Services.VortexDefenderQuestionWindowIntentAdapterService`
- `Aion.GameServer.Network.Aion.QuestionResponseRegistry`

## Validation Completed

Validation target: the adapter applies update-defenders registration across multiple defender players, creates non-sent question-window intent only for stored requests, preserves occupied-slot/already-defender/full-alliance outcomes, and keeps packet dispatch plus gameplay mutation disabled.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~QuestionResponseRegistryTests" --no-restore
```

- Passed: 123 tests.

Java/Maven validation was skipped because no Java source or fixtures changed and no narrow Java batch fixture exists for this seam. Broad-validation trigger was `none` because this adapter is opt-in and does not send packets or wire live lifecycle dispatch; full project/solution validation was skipped.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.vortex.Invasion.updateAlliance` | `Aion.GameServer.Services.VortexDefenderInvitationBatchRuntimeAdapterService` | Runtime Vortex batch registration adapter | Partial | Unit Tested | Partial Parity | C# accepts defender `Player` candidates and applies guarded update-defenders registration plus non-sent question-window intent per candidate. Production lifecycle wiring, live location filtering, packet sending, and gameplay mutations remain disabled. |
| `com.aionemu.gameserver.services.vortex.Invasion.updateDefenders` | `Aion.GameServer.Services.VortexDefenderInvitationBatchRuntimeAdapterService` / `Aion.GameServer.Services.VortexDefenderUpdateDefendersRegistrationRuntimeAdapterService` | Runtime Vortex defender registration adapter | Partial | Unit Tested | Partial Parity | C# preserves existing-defender, full-alliance, and occupied-request-slot guards while storing pending requests only through the response registry. Live callback execution and defender/alliance mutations remain disabled. |
| `com.aionemu.gameserver.model.gameobjects.player.ResponseRequester.putRequest` | `Aion.GameServer.Network.Aion.QuestionResponseRegistry.PutRequest` | Question response registry | Partial | Unit Tested | Partial Parity | C# preserves no-replace registration semantics for occupied request ids and leaves existing requests intact. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW` | `Aion.GameServer.Network.Aion.ServerPackets.SmQuestionWindow` / `Aion.GameServer.Services.VortexDefenderQuestionWindowIntentAdapterService` | Server packet intent | Partial | Unit Tested | Partial Parity | C# creates non-sent Vortex question-window intent only after request storage succeeds. Live packet dispatch remains disabled. |

## Known Gaps

- Live Vortex `start`, `spawn`, `despawn`, and `stop` behavior remains unimplemented.
- The new batch adapter does not yet scan production `VortexLocation.getPlayers()` or filter live players by defender race.
- Defender invitation registration and question-window creation are available through opt-in adapters and are not wired into production Vortex lifecycle.
- `SM_QUESTION_WINDOW` dispatch remains disabled.
- Vortex response-handler callbacks, acceptance flow, add-player transition, and alliance mutation remain metadata-only.
- Invader participant and alliance mutation remain metadata-only.
- Start and stop prepared requests still depend on supplied candidate collections rather than production world/location/alliance/Kisk/spawn containers.

## Remaining Risks

- Future live defender invitation wiring must preserve Java ordering: location-player scan, defender-race filter, existing defender guard, alliance fullness guard, handler/request payload creation, request storage, then question-window packet only when storage succeeds.
- Future live acceptance handling must preserve Java branch priority: group removal before alliance removal, then add defender only if the defender alliance is still open.
- Enabling live packet dispatch or gameplay mutation is a broad-validation trigger.

## Next Recommended UOW

[Phase 6] UOW-2510: Add guarded Vortex update-alliance runtime adapter

The next concrete task is to add an opt-in update-alliance runtime adapter that accepts a location summary or defender race plus live location players, uses `VortexDefenderAllianceUpdatePlanService` to filter defender-race candidates, passes defender `Player` candidates into `VortexDefenderInvitationBatchRuntimeAdapterService`, and returns both alliance update planning metadata and batch runtime registration metadata. Keep packet sending disabled.

Suggested files to inspect:

- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`
- `dotnetConversion/src/Aion.GameServer/Services/VortexDefenderAllianceUpdatePlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexDefenderUpdateDefendersPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexStartInvasionSideEffectPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

Suggested validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~QuestionResponseRegistryTests" --no-restore
```

Validation target: C# guarded update-alliance runtime adapter filters live player candidates to defender-race players, invokes guarded batch registration for those defenders only, preserves already-defender/full-alliance/occupied-slot outcomes, and keeps packet dispatch, callback execution, group, alliance, defender-map, scheduler, spawn, despawn, and portal side effects disabled.

Java/Maven is not expected unless Java source or fixtures change or a narrow Java Vortex update-alliance fixture is added. Broad-validation trigger should remain `none` if the adapter is opt-in and does not send packets; if it wires live packet sending or production lifecycle dispatch, packet dispatch/live side effects become a broad-validation trigger.

Safe alternative candidates:

- Add a narrow Java fixture/golden for `Invasion.updateAlliance` if a suitable Java harness seam exists.
- Add a non-live observer integration surface that exposes registration plus packet intent metadata from a start-invasion side-effect report.
- Continue production Vortex lifecycle adapter discovery for real world/location/alliance/Kisk/spawn inputs.

## Context Needed By Next Session

- Java `Invasion.updateAlliance` loops `getVortexLocation().getPlayers().values()` and calls `updateDefenders` for defender-race players.
- Java `Invasion.updateDefenders` creates a `RequestResponseHandler`, calls `defender.getResponseRequester().putRequest(904306, responseHandler)`, then sends `SM_QUESTION_WINDOW(904306, 0, 0)` only when registration succeeds.
- C# `VortexDefenderInvitationBatchRuntimeAdapterService` now applies registration plus packet-intent adapters per supplied defender `Player`.
- C# `VortexDefenderUpdateDefendersRegistrationRuntimeAdapterService` snapshots live player request slots, composes invitation planning, and delegates actual pending-request storage.
- C# `VortexDefenderQuestionWindowIntentAdapterService` creates non-sent `SmQuestionWindow` intent only after registration succeeds.
- C# pending-request payload planning creates handler payload for `RequestNotStored` because Java creates the handler before `putRequest` returns false.
- C# response routing for Vortex question id `904306` remains live in `GameServerConnection.HandleQuestionResponseAsync` for pending-request removal and metadata reporting.
