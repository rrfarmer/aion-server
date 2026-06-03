# Phase 6 Session 2521 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2520: Wire Vortex acceptance dependencies at production startup via GameClientSocketServer

## Session Summary (UOWs 2514-2520)

This session built up the complete Vortex defender acceptance pipeline, from a non-live composite observer all the way to production startup wiring:

- **UOW-2514:** Added `VortexDefenderAcceptanceRuntimeObserverService` — combines `HandleResponseWithAcceptanceTransition` and `VortexDefenderAcceptanceParticipantRuntimeReportService.CreateReport` into a single non-live observer. 2 new tests.
- **UOW-2515:** Wired the observer into `GameServerConnection.HandleVortexDefenderInvitationQuestionResponse`. Added `_vortexDefenderAcceptanceObserver` field; kept backward-compat `_vortexDefenderInvitationResponseObserver`. 2 new connection integration tests.
- **UOW-2516:** Added `VortexInvasionRuntime.FindDefenderLocationId(playerObjectId)` — read-only lookup matching Java `VortexService.removeDefenderPlayer` iteration. Wired into connection handler; `locationId: 0` fallback replaced by runtime lookup. 2 new tests.
- **UOW-2517:** Added `VortexDefenderAcceptanceInputResolverService.Resolve(snapshot, playerLookup)` — derives `existingDefenders` from runtime snapshot + player lookup, and approximates `defenderAlliance` from defender count (Java `defAlliance` proxy). 2 new tests.
- **UOW-2518:** Wired the resolver into the connection handler. Added `_worldPlayerLookup` field; resolver now fires when `_vortexInvasionRuntime != null && _worldPlayerLookup != null && locationId != 0`. 1 new integration test.
- **UOW-2519:** Added `_defenderAcceptanceVortexLocationService` field to `GameServerConnection`; world-position fallback via `GetLocationByWorld(responder.Position.WorldId)` when runtime lookup returns null. Full location chain: runtime → world-position → 0. 1 new unit test.
- **UOW-2520:** Wired `vortexInvasionRuntime`, `worldPlayerLookup`, and `defenderAcceptanceVortexLocationService` into each `GameServerConnection` created by `GameClientSocketServer.HandleConnectionAsync`. Added `_vortexInvasionRuntime` field to `GameClientSocketServer`. No new tests (compile validation).

## Key Observation

`VortexInvasionRuntime` is already registered as a DI singleton in `Program.cs` at line 165. When `GameClientSocketServer` is resolved from DI, `VortexInvasionRuntime` is automatically injected. The full Vortex acceptance pipeline is therefore **production-ready at the wiring level**. All live mutation guards (`ShouldMutateLive*`) remain false throughout.

## Commits Made

- `[Phase 6][UOW-2514] Add guarded Vortex defender acceptance runtime observer`
- `[Phase 6][UOW-2515] Wire Vortex defender acceptance observer into game connection`
- `[Phase 6][UOW-2516] Add defender location lookup and wire into game connection`
- `[Phase 6][UOW-2517] Add defender acceptance input resolver for production wiring`
- `[Phase 6][UOW-2518] Wire defender acceptance input resolver into game connection`
- `[Phase 6][UOW-2519] Add world-position fallback for defender acceptance location resolution`
- `[Phase 6][UOW-2520] Wire Vortex acceptance dependencies at production startup`

## Files Changed (Session Total)

- `dotnetConversion/src/Aion.GameServer/Services/VortexDefenderInvitationAcceptancePlanService.cs` — new observer service and report
- `dotnetConversion/src/Aion.GameServer/Services/VortexDefenderAddPlayerTransitionPlanService.cs` — new input resolver service and record
- `dotnetConversion/src/Aion.GameServer/Services/VortexInvasionRuntime.cs` — `FindDefenderLocationId`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs` — observer + resolver + location wiring
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientSocketServer.cs` — production startup wiring
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs` — 9 new tests
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionVortexQuestionResponseTests.cs` — 4 new tests + helper updates
- `docs/Phase-6-Session-2514-Completion.md` through `docs/Phase-6-Session-2520-Handoff.md` — 14 new docs

## Test Summary

- Tests before session: 132
- Tests after session: 144
- New tests: 13 (across VortexLocationServiceTests and GameServerConnectionVortexQuestionResponseTests)

## Java Artifacts Covered (Session)

| Java Artifact | C# Artifact | Parity Status |
| --- | --- | --- |
| `Invasion.updateDefenders.RequestResponseHandler.acceptRequest` + `addPlayer(player, false)` + `defenders.put(...)` | `VortexDefenderAcceptanceRuntimeObserverService` | Partial Parity — non-live observer; live mutation disabled |
| `VortexService.removeDefenderPlayer` (iteration pattern) | `VortexInvasionRuntime.FindDefenderLocationId` | Partial Parity — read-only lookup matches Java pattern |
| `Invasion.defenders` (read path) | `VortexDefenderAcceptanceInputResolverService` | Partial Parity — derives snapshots via runtime + world lookup |
| `Invasion.defAlliance` (state approximation) | `VortexDefenderAcceptanceInputs.DefenderAlliance` | Partial Parity — count-based approximation; disbandment not modeled |
| `VortexService.getLocationByWorld` (fallback) | Connection world-position fallback | Partial Parity — approximates Java's implicit callback context |
| `CM_QUESTION_RESPONSE.runImpl` (Vortex defender) | `GameServerConnection.HandleVortexDefenderInvitationQuestionResponse` | Partial Parity — full observer now wired; live mutation disabled |

## Known Gaps

- `VortexInvasionRuntime.AddDefender` is NOT called on acceptance; live defender-map mutation remains disabled. This is the major remaining gap.
- `defAlliance` disbandment state is not modeled; `Disbanded` snapshot can only be supplied externally.
- Alliance state is a count-based approximation; Java's live `defAlliance` reference tracking is missing.
- `PendingVortexDefenderInvitationRequest` lacks `LocationId`; location resolution relies on runtime lookup and world-position fallback chain.

## Remaining Risks

- Enabling live `AddDefender` on acceptance will be a broad-validation trigger and requires integration tests.
- The `worldPlayerLookup` lambda uses `World.TryGetObject` under concurrent access; thread safety should be confirmed.
- `defenderAlliance` approximation may diverge from Java when alliance is disbanded and re-created in the same session.

## Next Recommended UOW

[Phase 6] UOW-2521: Add `LocationId` to `PendingVortexDefenderInvitationRequest` for principled location context

The current location resolution chain (runtime lookup → world-position fallback → 0) is an approximation. A more principled approach is to embed `LocationId` in the pending request when the invitation is created. This requires:

1. Add `int LocationId` as a positional parameter to `PendingVortexDefenderInvitationRequest`
2. Update `VortexDefenderInvitationRequestPayloadPlanService.CreatePlan` to accept `locationId`
3. Update `VortexDefenderInvitationRegistrationRuntimeAdapterService.Register` and the batch adapter to thread `locationId` through
4. Update `VortexDefenderAllianceUpdateRuntimeAdapterService.UpdateAlliance` to pass `location.Id`
5. Update `GameServerConnection.HandleVortexDefenderInvitationQuestionResponse` to read `locationId` from the stored payload request instead of the runtime lookup chain
6. Update all 10+ test construction sites for `PendingVortexDefenderInvitationRequest`

Suggested files to inspect:

- `dotnetConversion/src/Aion.GameServer/Services/VortexDefenderInvitationPlanService.cs` (`PendingVortexDefenderInvitationRequest`, payload plan service)
- `dotnetConversion/src/Aion.GameServer/Services/VortexDefenderAllianceUpdatePlanService.cs` (batch adapter)
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs` (construction sites)
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionVortexQuestionResponseTests.cs` (construction sites)

Suggested validation:

```powershell
dotnet test "dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj" --filter "FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~GameServerConnectionVortexQuestionResponseTests|FullyQualifiedName~QuestionResponseRegistryTests" --no-restore
```

Broad-validation trigger: none (adding a field to a non-live record and threading it through service layers is non-live).

Safe alternative candidates:

- Move to a different Phase 6 subsystem (NPC dialog sub-restriction checks, storage expansion NPC for warehouse, soul healing, craft skill updates).
- Enable live `VortexInvasionRuntime.AddDefender` on acceptance (broad-validation trigger).
- Add a narrow Java fixture for the `Invasion.updateDefenders` → `acceptRequest` → `addPlayer` → `defenders.put` chain.

## Context Needed By Next Session

- The Vortex defender acceptance pipeline (UOW-2514 through 2520) is fully wired at the non-live metadata level. All `ShouldMutateLive*` guards remain false.
- `VortexInvasionRuntime` is already registered in DI (`Program.cs` line 165) and will be auto-injected into `GameClientSocketServer` by .NET DI.
- `GameServerConnection.HandleVortexDefenderInvitationQuestionResponse` now:
  1. Resolves `locationId` via `FindDefenderLocationId` → `GetLocationByWorld(responder.Position.WorldId)` → 0
  2. Resolves `existingDefenders` and `defenderAlliance` from `VortexDefenderAcceptanceInputResolverService` when runtime + lookup are available
  3. Calls `VortexDefenderAcceptanceRuntimeObserverService.Observe(locationId, responder, questionId, responseCode, existingDefenders, defenderAlliance)`
  4. Fires `_vortexDefenderAcceptanceObserver` (full report) and `_vortexDefenderInvitationResponseObserver` (consumption report, backward compat)
- `VortexDefenderAllianceMaxSize = 24` is the proxy for "alliance full" in the resolver.
- `PendingVortexDefenderInvitationRequest` lacks `LocationId`; this is the principled next fix.
