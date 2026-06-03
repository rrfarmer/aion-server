# Phase 6 Session 2501 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2501: Compose Vortex defender acceptance request payload metadata

## Commits Made

- `[Phase 6][UOW-2501] Compose Vortex defender request payload`

## Summary

UOW-2501 added inert pending-request payload metadata for Vortex defender invitations and a response-dispatch planner that mirrors Java `RequestResponseHandler.handle`: response code `0` denies, while any nonzero response accepts and feeds the existing defender acceptance plan.

This remains partial parity. The path records request payload and response dispatch shape only; it does not register live response requests, send `SM_QUESTION_WINDOW`, remove live requests, execute callbacks, mutate groups, mutate alliances, mutate defender participants, despawn NPCs, spawn NPCs, start portals, or schedule stops.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/QuestionResponseRegistry.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexDefenderInvitationPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexDefenderInvitationAcceptancePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`
- `docs/Phase-6-Session-2501-Completion.md`
- `docs/Phase-6-Session-2501-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.vortex.Invasion.updateDefenders`
- `com.aionemu.gameserver.model.gameobjects.player.RequestResponseHandler`
- `com.aionemu.gameserver.model.gameobjects.player.ResponseRequester`

## C# Artifacts Touched

- `Aion.GameServer.Model.GameObjects.QuestionResponseRequestKind`
- `Aion.GameServer.Services.VortexDefenderInvitationRequestPayloadPlanService`
- `Aion.GameServer.Services.PendingVortexDefenderInvitationRequest`
- `Aion.GameServer.Services.VortexDefenderInvitationResponseDispatchPlanService`
- `Aion.GameServer.Services.VortexDefenderInvitationAcceptancePlan`

## Validation Completed

Validation target: Vortex defender invitation metadata captures Java request payload and accept/deny dispatch shape while keeping live request storage, packet dispatch, group, alliance, defender-map, scheduler, spawn, despawn, and portal side effects disabled.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~QuestionResponseRegistryTests" --no-restore
```

- Passed: 108 tests.

Java/Maven validation was skipped because no Java source or fixtures changed and no narrow Java Vortex defender-request fixture exists. Java source was reviewed directly. Broad-validation trigger was `none` because this UOW only adds non-live metadata/adapters and tests.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.gameobjects.player.RequestResponseHandler` | `Aion.GameServer.Services.VortexDefenderInvitationResponseDispatchPlanService` | Runtime response metadata | Partial | Unit Tested | Partial Parity | C# maps response code `0` to deny and nonzero to accept; live request removal and callback invocation remain disabled. |
| `com.aionemu.gameserver.services.vortex.Invasion.updateDefenders` | `Aion.GameServer.Services.VortexDefenderInvitationRequestPayloadPlanService` | Runtime request metadata adapter | Partial | Unit Tested | Partial Parity | C# creates Vortex defender pending request payload metadata for question-window intent; live request storage and packet dispatch remain disabled. |
| `com.aionemu.gameserver.model.gameobjects.player.ResponseRequester` | `Aion.GameServer.Model.GameObjects.QuestionResponseRequestKind.VortexDefenderInvitation` and `Aion.GameServer.Model.GameObjects.QuestionResponseRequest` | Runtime request metadata | Partial | Unit Tested | Partial Parity | C# now carries a Vortex-specific request kind and payload; Java `putRequest` storage remains disabled in Vortex path. |

## Known Gaps

- Live Vortex `start`, `spawn`, `despawn`, and `stop` behavior remains unimplemented.
- Defender request storage, packet dispatch, request-handler callbacks, acceptance flow, add-player transition, and alliance mutation remain metadata-only.
- Invader participant and alliance mutation remain metadata-only.
- Start and stop prepared requests still depend on supplied candidate collections rather than production world/location/alliance/Kisk/spawn containers.
- Full production XML coverage for Vortex spawns has not been compared against Java output.

## Remaining Risks

- Future live defender invitation work must preserve Java ordering: existing defender guard, alliance fullness guard, request handler creation, request storage, then question-window packet only when storage succeeds.
- Future live response dispatch must remove or consume the pending request consistently with Java `ResponseRequester`.
- Future live acceptance handling must preserve Java branch priority: group removal before alliance removal, then add defender only if the defender alliance is still open.
- Live request storage, response removal, packet dispatch, or mutation would be a broad-validation trigger.

## Next Recommended UOW

[Phase 6] UOW-2502: Compose Vortex defender invitation registration report metadata

The next smallest safe task is to inspect Java request storage and packet send ordering against the current payload, slot, and question-window plans, then add a non-live registration/report plan that combines payload plan, request-slot availability, question-window metadata, and simulated `putRequest` result without live storage or dispatch.

Suggested files to inspect:

- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/ResponseRequester.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_QUESTION_WINDOW.java`
- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/QuestionResponseRegistry.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexDefenderInvitationPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexDefenderInvitationAcceptancePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

Suggested validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~QuestionResponseRegistryTests" --no-restore
```

Validation target: C# Vortex defender invitation registration metadata captures Java `putRequest` success/failure and packet-send gating while keeping live request storage, packet dispatch, group, alliance, defender-map, scheduler, spawn, despawn, and portal side effects disabled.

Java/Maven is not expected unless Java source or fixtures change or a narrow Java Vortex defender-request fixture is added. Broad-validation trigger should be `none` if the UOW only adds non-live metadata/adapters and tests; it becomes present if live packet dispatch, request storage, scheduler dispatch, alliance mutation, group mutation, participant mutation, portal spawn, NPC despawn, or NPC spawn is enabled.

Safe alternative candidates:

- Add a narrow Java fixture/golden for defender invitation branch metadata if a suitable Java seam is available.
- Continue production Vortex lifecycle adapter discovery for real world/location/alliance/Kisk/spawn inputs.
- Compose acceptance-batch metadata if the registration-report seam is not clear.

## Context Needed By Next Session

- Java `Invasion.updateDefenders` creates the request handler before attempting `ResponseRequester.putRequest(904306, responseHandler)`.
- Java sends `SM_QUESTION_WINDOW(904306, 0, 0)` only when `putRequest` returns true.
- Java `RequestResponseHandler.handle` maps response code `0` to deny and nonzero to accept.
- Existing C# request payload and response dispatch plans remain metadata-only and must not enable live request storage, response removal, or packet dispatch without a separate broad validation decision.
