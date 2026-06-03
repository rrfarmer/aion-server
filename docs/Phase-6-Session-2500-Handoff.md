# Phase 6 Session 2500 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2500: Capture Vortex defender request-slot metadata

## Commits Made

- `[Phase 6][UOW-2500] Capture Vortex defender request slots`

## Summary

UOW-2500 added a read-only adapter from C# `Player.ResponseRequester` to Vortex defender invitation metadata. The start collector can now derive Java `ResponseRequester.putRequest(904306, handler)` availability from supplied defender player objects and feed that into the existing non-live defender invitation batch plan.

This remains partial parity. The path records request-slot intent only; it does not store live Vortex response handlers, send `SM_QUESTION_WINDOW`, execute acceptance callbacks, mutate groups, mutate alliances, mutate defender participants, despawn NPCs, spawn NPCs, start portals, or schedule stops.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/QuestionResponseRegistry.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmQuestionWindow.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexDefenderInvitationPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexStartInvasionRuntimeSnapshotCollectorService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestionResponseRegistryTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`
- `docs/Phase-6-Session-2500-Completion.md`
- `docs/Phase-6-Session-2500-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.vortex.Invasion.updateDefenders`
- `com.aionemu.gameserver.model.gameobjects.player.ResponseRequester`
- `com.aionemu.gameserver.model.gameobjects.player.RequestResponseHandler`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW`

## C# Artifacts Touched

- `Aion.GameServer.Model.GameObjects.QuestionResponseRegistry`
- `Aion.GameServer.Network.Aion.ServerPackets.SmQuestionWindow`
- `Aion.GameServer.Services.VortexDefenderInvitationRequestSlotSnapshotService`
- `Aion.GameServer.Services.VortexDefenderInvitationRequestSlotSnapshot`
- `Aion.GameServer.Services.VortexDefenderInvitationPlanService`
- `Aion.GameServer.Services.VortexStartInvasionRuntimeSnapshotCollectorService`

## Validation Completed

Validation target: C# Vortex defender invitation adapter metadata captures Java request-slot availability and question-window intent while keeping live request storage, packet dispatch, acceptance, group, alliance, defender-map, scheduler, spawn, despawn, and portal side effects disabled.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~QuestionResponseRegistryTests" --no-restore
```

- Passed: 106 tests.
- Existing nullable/analyzer warnings were emitted from unrelated files.

```powershell
git diff --check
git diff --cached --check
```

- `git diff --check` passed. Git reported expected CRLF conversion warnings for edited C# source and test files.
- `git diff --cached --check` passed.

Java/Maven validation was skipped because no Java source or fixtures changed and no narrow Java Vortex defender-request fixture exists. Java source was reviewed directly. Broad-validation trigger was `none` because this UOW only adds read-only metadata/adapters and tests.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.gameobjects.player.ResponseRequester` | `Aion.GameServer.Model.GameObjects.QuestionResponseRegistry` | Runtime request registry | Partial | Unit Tested | Partial Parity | C# exposes read-only same-question availability metadata matching Java `putIfAbsent`; live Vortex request storage remains disabled. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW` | `Aion.GameServer.Network.Aion.ServerPackets.SmQuestionWindow` | Packet constant | Partial | Unit Tested | Partial Parity | C# names Vortex defender invitation question id `904306`; packet send remains disabled in Vortex path. |
| `com.aionemu.gameserver.services.vortex.Invasion.updateDefenders` | `Aion.GameServer.Services.VortexDefenderInvitationRequestSlotSnapshotService` | Runtime metadata adapter | Partial | Unit Tested | Partial Parity | C# snapshots defender request-slot availability from `Player.ResponseRequester` and feeds batch invitation metadata. Acceptance callback and live packet dispatch remain disabled. |

## Known Gaps

- Live Vortex `start`, `spawn`, `despawn`, and `stop` behavior remains unimplemented.
- Defender request storage, packet dispatch, request-handler callbacks, acceptance flow, add-player transition, and alliance mutation remain metadata-only.
- Invader participant and alliance mutation remain metadata-only.
- Start and stop prepared requests still depend on supplied candidate collections rather than production world/location/alliance/Kisk/spawn containers.
- Full production XML coverage for Vortex spawns has not been compared against Java output.

## Remaining Risks

- Future live defender invitation work must preserve Java ordering: existing defender guard, alliance fullness guard, request handler creation, request storage, then question-window packet only when storage succeeds.
- Future live request storage must also model the `RequestResponseHandler` requester/responder callback relationship.
- Current metadata captures request-slot availability but does not create a live pending Vortex-specific request payload.
- Live request storage or packet dispatch would be a broad-validation trigger.

## Next Recommended UOW

[Phase 6] UOW-2501: Compose Vortex defender acceptance request payload metadata

The next smallest safe task is to inspect Java `RequestResponseHandler.handle` and `Invasion.updateDefenders.acceptRequest` against current C# `QuestionResponseRequest`/pending request models, then add an inert Vortex defender invitation request payload that records requester, responder, question id, and acceptance metadata without registering it live or dispatching packets.

Suggested files to inspect:

- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/RequestResponseHandler.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/ResponseRequester.java`
- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/QuestionResponseRegistry.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexDefenderInvitationPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexDefenderInvitationAcceptancePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

Suggested validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~QuestionResponseRegistryTests" --no-restore
```

Validation target: C# Vortex defender invitation metadata captures Java request payload and accept/deny dispatch shape while keeping live request storage, packet dispatch, group, alliance, defender-map, scheduler, spawn, despawn, and portal side effects disabled.

Java/Maven is not expected unless Java source or fixtures change or a narrow Java Vortex defender-request fixture is added. Broad-validation trigger should be `none` if the UOW only adds non-live metadata/adapters and tests; it becomes present if live packet dispatch, request storage, scheduler dispatch, alliance mutation, group mutation, participant mutation, portal spawn, NPC despawn, or NPC spawn is enabled.

Safe alternative candidates:

- Add a narrow Java fixture/golden for defender invitation branch metadata if a suitable Java seam is available.
- Continue production Vortex lifecycle adapter discovery for real world/location/alliance/Kisk/spawn inputs.
- Compose acceptance-batch metadata if the pending-request payload seam is not clear.

## Context Needed By Next Session

- Java `ResponseRequester.putRequest` rejects null handlers and returns false when the same message id is already registered.
- Java `RequestResponseHandler.handle` maps response code `0` to deny and nonzero to accept.
- Java Vortex defender question id is `904306`; C# now names it as `SmQuestionWindow.VortexDefenderInvitation`.
- Existing C# plans remain metadata-only and must not enable live request storage or packet dispatch without a separate broad validation decision.
