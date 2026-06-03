# Phase 6 Session 2508 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2508: Add guarded Vortex defender question-window packet intent adapter

## Commits Made

- `[Phase 6][UOW-2508] Add Vortex defender question-window intent adapter`

## Summary

UOW-2508 added `VortexDefenderQuestionWindowIntentAdapterService`, an opt-in adapter that accepts `VortexDefenderUpdateDefendersRegistrationRuntimeReport` and creates a non-sent `SmQuestionWindow` intent only when defender request registration succeeded.

The unit also added a packet serialization assertion for `SmQuestionWindow.VortexDefenderInvitation`, pinning the Vortex `SM_QUESTION_WINDOW(904306, 0, 0)` payload shape against the reviewed Java write order.

This remains partial parity. The adapter composes only packet intent metadata. It does not send packets, execute live callbacks, mutate groups, mutate alliances, mutate defender participants, despawn NPCs, spawn NPCs, start portals, or schedule stops.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/VortexDefenderUpdateDefendersPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `docs/Phase-6-Session-2508-Completion.md`
- `docs/Phase-6-Session-2508-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.vortex.Invasion.updateDefenders`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW`

## C# Artifacts Touched

- `Aion.GameServer.Services.VortexDefenderQuestionWindowIntentAdapterService`
- `Aion.GameServer.Services.VortexDefenderQuestionWindowIntent`
- `Aion.GameServer.Network.Aion.ServerPackets.SmQuestionWindow`

## Validation Completed

Validation target: C# Vortex question-window packet-intent adapter creates intent only for successful registration, preserves Java question id `904306` and args `0,0`, and keeps live packet sending disabled.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~GamePacketTests" --no-restore
```

- Passed: 391 tests.
- Existing nullable/analyzer warnings were emitted from unrelated files.

Java/Maven validation was skipped because no Java source or fixtures changed and the narrow source-of-truth behavior was reviewed directly from `SM_QUESTION_WINDOW.writeImpl`. Broad-validation trigger was `none` because this adapter is opt-in and does not send packets; full project/solution validation was skipped.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.vortex.Invasion.updateDefenders` | `Aion.GameServer.Services.VortexDefenderQuestionWindowIntentAdapterService` | Runtime Vortex packet-intent adapter | Partial | Unit Tested | Partial Parity | C# now composes non-sent question-window intent only after registration success. Production start/update-defenders wiring and live packet dispatch remain disabled. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW` | `Aion.GameServer.Network.Aion.ServerPackets.SmQuestionWindow` | Server packet | Partial | Unit Tested | Partial Parity | C# Vortex row writes code `904306`, three empty string slots, unknown dword `0`, range flag `0`, sender `0`, and range/cooldown `0` per reviewed Java write order. Not yet Java golden-file verified. |

## Known Gaps

- Live Vortex `start`, `spawn`, `despawn`, and `stop` behavior remains unimplemented.
- Defender invitation registration and question-window creation are available through opt-in adapters and are not wired into production Vortex lifecycle.
- `SM_QUESTION_WINDOW` dispatch remains disabled.
- Vortex response-handler callbacks, acceptance flow, add-player transition, and alliance mutation remain metadata-only.
- Invader participant and alliance mutation remain metadata-only.
- Start and stop prepared requests still depend on supplied candidate collections rather than production world/location/alliance/Kisk/spawn containers.

## Remaining Risks

- Future live defender invitation wiring must preserve Java ordering: existing defender guard, alliance fullness guard, handler/request payload creation, request storage, then question-window packet only when storage succeeds.
- Future live acceptance handling must preserve Java branch priority: group removal before alliance removal, then add defender only if the defender alliance is still open.
- Enabling live packet dispatch or gameplay mutation is a broad-validation trigger.

## Next Recommended UOW

[Phase 6] UOW-2509: Add guarded Vortex defender batch registration runtime adapter

The next concrete task is to add an opt-in batch runtime adapter that accepts defender `Player` instances, existing defender snapshots, an optional defender-alliance snapshot, and applies the guarded update-defenders registration plus question-window intent adapters to each eligible defender. Keep packet sending disabled and return per-defender metadata.

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

Validation target: C# guarded batch runtime adapter applies update-defenders registration to multiple defender players, creates non-sent question-window intent only for successful registrations, preserves Java guard outcomes for already-defender/full-alliance/occupied-slot cases, and keeps packet dispatch, callback execution, group, alliance, defender-map, scheduler, spawn, despawn, and portal side effects disabled.

Java/Maven is not expected unless Java source or fixtures change or a narrow Java Vortex batch fixture is added. Broad-validation trigger should remain `none` if the adapter is opt-in and does not send packets; if it wires live packet sending or production lifecycle dispatch, packet dispatch/live side effects become a broad-validation trigger.

Safe alternative candidates:

- Add a narrow Java fixture/golden for `SM_QUESTION_WINDOW(904306, 0, 0)` if a suitable Java packet harness seam exists.
- Continue production Vortex lifecycle adapter discovery for real world/location/alliance/Kisk/spawn inputs.
- Add a non-live observer integration surface that exposes registration plus packet intent metadata from a start-invasion side-effect report.

## Context Needed By Next Session

- Java `Invasion.updateAlliance` loops location players and calls `updateDefenders` for defender-race players.
- Java `Invasion.updateDefenders` creates a `RequestResponseHandler`, calls `defender.getResponseRequester().putRequest(904306, responseHandler)`, then sends `SM_QUESTION_WINDOW(904306, 0, 0)` only when registration succeeds.
- C# `VortexDefenderUpdateDefendersRegistrationRuntimeAdapterService` snapshots live player request slots, composes invitation planning, and delegates actual pending-request storage.
- C# `VortexDefenderQuestionWindowIntentAdapterService` creates non-sent `SmQuestionWindow` intent only after registration succeeds.
- C# pending-request payload planning creates handler payload for `RequestNotStored` because Java creates the handler before `putRequest` returns false.
- C# response routing for Vortex question id `904306` remains live in `GameServerConnection.HandleQuestionResponseAsync` for pending-request removal and metadata reporting.
