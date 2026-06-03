# Phase 6 Session 2507 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2507: Wire Vortex defender invitation registration into a guarded update-defenders adapter

## Commits Made

- `[Phase 6][UOW-2507] Add guarded Vortex update-defenders registration adapter`

## Summary

UOW-2507 added `VortexDefenderUpdateDefendersRegistrationRuntimeAdapterService`, an opt-in adapter that snapshots a live defender's Vortex request slot, composes the update-defenders invitation plan, delegates to the Vortex invitation registration runtime adapter, and returns metadata for observer/reporting surfaces.

The unit also refined pending-request payload planning: C# now creates the pending Vortex handler payload whenever Java would create `RequestResponseHandler`, including occupied-slot paths where `putRequest` later rejects storage.

This remains partial parity. The adapter stores only the pending response request and metadata. It does not send packets, execute live callbacks, mutate groups, mutate alliances, mutate defender participants, despawn NPCs, spawn NPCs, start portals, or schedule stops.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/VortexDefenderInvitationPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexDefenderUpdateDefendersPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`
- `docs/Phase-6-Session-2507-Completion.md`
- `docs/Phase-6-Session-2507-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.vortex.Invasion.updateDefenders`
- `com.aionemu.gameserver.model.gameobjects.player.ResponseRequester.putRequest`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW`

## C# Artifacts Touched

- `Aion.GameServer.Services.VortexDefenderUpdateDefendersRegistrationRuntimeAdapterService`
- `Aion.GameServer.Services.VortexDefenderUpdateDefendersRegistrationRuntimeReport`
- `Aion.GameServer.Services.VortexDefenderInvitationRequestPayloadPlanService`
- `Aion.GameServer.Services.VortexDefenderInvitationRegistrationRuntimeAdapterService`
- `Aion.GameServer.Model.GameObjects.QuestionResponseRegistry`

## Validation Completed

Validation target: C# guarded update-defenders adapter uses Java guard ordering, calls the registration runtime adapter for eligible defenders, records question-window intent only after request storage, and keeps packet dispatch, callback execution, group, alliance, defender-map, scheduler, spawn, despawn, and portal side effects disabled.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~QuestionResponseRegistryTests" --no-restore
```

- Passed: 119 tests.
- Existing nullable/analyzer warnings were emitted from unrelated files.

Java/Maven validation was skipped because no Java source or fixtures changed and the narrow source-of-truth behavior was reviewed directly. Broad-validation trigger was `none` because this adapter is opt-in and is not wired into live start/update-defenders dispatch or packet sending; full project/solution validation was skipped.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.vortex.Invasion.updateDefenders` | `Aion.GameServer.Services.VortexDefenderUpdateDefendersRegistrationRuntimeAdapterService` | Runtime Vortex update-defenders adapter | Partial | Unit Tested | Partial Parity | C# now snapshots live request-slot state, composes invitation planning, creates handler payload before storage rejection, and delegates pending-request storage. Live packet dispatch, callback execution, defender map mutation, alliance mutation, and production start/update-defenders wiring remain disabled. |
| `com.aionemu.gameserver.model.gameobjects.player.ResponseRequester.putRequest` | `Aion.GameServer.Model.GameObjects.QuestionResponseRegistry.PutRequest` | Runtime request registry | Partial | Unit Tested | Partial Parity | C# adapter uses put-if-absent-equivalent storage and preserves existing requests when question id `904306` is occupied. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW` | `Aion.GameServer.Network.Aion.ServerPackets.SmQuestionWindow` | Packet intent | Partial | Unit Tested | Partial Parity | C# records Java question id `904306` and args `0,0` only after successful storage. Actual packet sending remains disabled. |

## Known Gaps

- Live Vortex `start`, `spawn`, `despawn`, and `stop` behavior remains unimplemented.
- Defender invitation registration is available through opt-in adapters and is not wired into production Vortex lifecycle.
- `SM_QUESTION_WINDOW` dispatch remains disabled.
- Vortex response-handler callbacks, acceptance flow, add-player transition, and alliance mutation remain metadata-only.
- Invader participant and alliance mutation remain metadata-only.
- Start and stop prepared requests still depend on supplied candidate collections rather than production world/location/alliance/Kisk/spawn containers.

## Remaining Risks

- Future live defender invitation wiring must preserve Java ordering: existing defender guard, alliance fullness guard, handler/request payload creation, request storage, then question-window packet only when storage succeeds.
- Future live acceptance handling must preserve Java branch priority: group removal before alliance removal, then add defender only if the defender alliance is still open.
- Enabling packet dispatch or gameplay mutation is a broad-validation trigger.

## Next Recommended UOW

[Phase 6] UOW-2508: Add guarded Vortex defender question-window packet intent adapter

The next concrete task is to add a small, opt-in packet-intent adapter that accepts `VortexDefenderUpdateDefendersRegistrationRuntimeReport` and composes a non-sent `SmQuestionWindow` intent only when registration storage succeeded. Keep the actual send disabled unless the unit explicitly documents and validates a broad live packet-dispatch trigger.

Suggested files to inspect:

- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_QUESTION_WINDOW.java`
- `dotnetConversion/src/Aion.GameServer/Services/VortexDefenderInvitationPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexDefenderUpdateDefendersPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmQuestionWindow.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`

Suggested validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~GamePacketTests" --no-restore
```

Validation target: C# Vortex question-window packet-intent adapter creates intent only for successful registration, preserves Java question id `904306` and args `0,0`, and keeps live packet sending disabled.

Java/Maven is not expected unless Java source or fixtures change or a narrow Java packet fixture is added. Broad-validation trigger should remain `none` if the adapter is opt-in and does not send packets; if it wires live packet sending, packet dispatch becomes a broad-validation trigger.

Safe alternative candidates:

- Add a narrow Java fixture/golden for `SM_QUESTION_WINDOW(904306, 0, 0)` if a suitable Java packet harness seam exists.
- Continue production Vortex lifecycle adapter discovery for real world/location/alliance/Kisk/spawn inputs.
- Add a guarded batch runtime adapter that applies update-defenders registration to multiple defender `Player` instances without packet dispatch.

## Context Needed By Next Session

- Java `Invasion.updateDefenders` creates a `RequestResponseHandler`, calls `defender.getResponseRequester().putRequest(904306, responseHandler)`, then sends `SM_QUESTION_WINDOW(904306, 0, 0)` only when registration succeeds.
- C# `VortexDefenderUpdateDefendersRegistrationRuntimeAdapterService` now snapshots the live player request slot, composes invitation planning, and delegates actual pending-request storage.
- C# pending-request payload planning now creates handler payload for `RequestNotStored` because Java creates the handler before `putRequest` returns false.
- C# response routing for Vortex question id `904306` remains live in `GameServerConnection.HandleQuestionResponseAsync` for pending-request removal and metadata reporting.
- Existing C# Vortex response route removes from `Player.ResponseRequester` and returns metadata only; it does not mutate gameplay state.
