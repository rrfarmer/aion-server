# Phase 6 Session 2517 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2517: Add VortexDefenderAcceptanceInputResolverService for production wiring

## Commits Made

- `[Phase 6][UOW-2517] Add defender acceptance input resolver for production wiring`

## Summary

UOW-2517 added `VortexDefenderAcceptanceInputResolverService.Resolve(snapshot, playerLookup)`, which derives the `existingDefenders` list and an approximated `defenderAlliance` snapshot from a `VortexInvasionSnapshot` plus a live player lookup function. The alliance approximation mirrors Java's rule that `defAlliance` is absent for 0 or 1 defenders and created on the 2nd, using count as a proxy since C# runtime doesn't yet track the live alliance reference.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/VortexDefenderAddPlayerTransitionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`
- `docs/Phase-6-Session-2517-Completion.md`
- `docs/Phase-6-Session-2517-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.vortex.Invasion.defenders` (read path)
- `com.aionemu.gameserver.services.vortex.Invasion.defAlliance` (state approximation)
- `com.aionemu.gameserver.model.team.alliance.PlayerAlliance.isFull()` (max 24 members)

## C# Artifacts Touched

- `Aion.GameServer.Services.VortexDefenderAcceptanceInputResolverService`
- `Aion.GameServer.Services.VortexDefenderAcceptanceInputs`

## Validation Completed

Validation target: Resolver maps runtime snapshot defender ids to player snapshots with correct group/alliance flags; derives `Open`/`Missing`/`Full` alliance approximation; guards null-snapshot input.

```powershell
dotnet test "dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj" --filter "FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~GameServerConnectionVortexQuestionResponseTests|FullyQualifiedName~QuestionResponseRegistryTests" --no-restore
```

- Passed: 142 tests (140 prior + 2 new).

Java/Maven validation was skipped. Broad-validation trigger was `none`; full project/solution validation was skipped.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `Invasion.defenders` (read) | `VortexDefenderAcceptanceInputResolverService` | Input collector | Partial | Unit Tested | Partial Parity | Maps snapshot ids to player snapshots via world lookup. Absent-player fallback; no live mutation. |
| `Invasion.defAlliance` (state) | `VortexDefenderAcceptanceInputs.DefenderAlliance` | Alliance approximation | Partial | Unit Tested | Partial Parity | Derived from count: Missing for 0/1, Open for 2-23, Full for 24+. Disbandment not modeled. |

## Known Gaps

- `defAlliance` disbandment is not modeled; `Disbanded` snapshot can only be supplied externally.
- Absent-player fallback assumes not-in-group/not-in-alliance.
- C# runtime does not track the live `defAlliance` reference; approximation may diverge from Java when alliance is disbanded and re-created in the same session.
- `GameServerConnection` does not yet call the resolver to supply real inputs to the acceptance observer.

## Remaining Risks

- Enabling production resolver in the connection handler would change the observer report content; the change is non-live but could expose gaps in the approximation.
- Connecting the resolver to the connection handler requires passing a world player-lookup function, which is a new dependency for the connection.

## Next Recommended UOW

[Phase 6] UOW-2518: Wire VortexDefenderAcceptanceInputResolverService into GameServerConnection for real defender inputs

The next concrete task is to connect `VortexDefenderAcceptanceInputResolverService` to the connection handler so the acceptance observer receives real existing-defender snapshots and an approximated alliance snapshot instead of the `null` defaults. This requires:
1. Adding an optional `Func<int, Player?>? worldPlayerLookup` parameter to `GameServerConnection`
2. In `HandleVortexDefenderInvitationQuestionResponse`: look up the snapshot via `FindDefenderLocationId` + `GetSnapshot(locationId)`, call the resolver, supply `existingDefenders` and `defenderAlliance` to `VortexDefenderAcceptanceRuntimeObserverService.Observe`
3. Adding an integration test that verifies the resolver inputs flow through to the observer report

Suggested files to inspect:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexInvasionRuntime.cs` (`GetSnapshot`)
- `dotnetConversion/src/Aion.GameServer/Services/VortexDefenderAddPlayerTransitionPlanService.cs` (`VortexDefenderAcceptanceInputResolverService`)
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionVortexQuestionResponseTests.cs`

Suggested validation:

```powershell
dotnet test "dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj" --filter "FullyQualifiedName~GameServerConnectionVortexQuestionResponseTests|FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~QuestionResponseRegistryTests" --no-restore
```

Validation target: Connection handler derives real defender snapshots from runtime + world lookup and passes them to the acceptance observer; observer report reflects actual defender ids before/after.

Java/Maven is not expected. Broad-validation trigger should remain `none` if the resolver is non-live and the connection continues to disable live mutation.

Safe alternative candidates:

- Move to a different Phase 6 subsystem to diversify coverage (NPC AI dispatch, inventory mutation, skill effect, or combat damage).
- Add a `VortexDefenderAllianceRuntimeStateService` that tracks `defAlliance` equivalent in C# runtime instead of using count-based approximation.
- Add targeted Java tests for `Invasion.addPlayer` branch ordering to give a golden comparison target.

## Context Needed By Next Session

- `VortexDefenderAcceptanceInputResolverService.Resolve(snapshot, playerLookup)` maps snapshot defender ids to `VortexDefenderAddPlayerSnapshot` via player lookup and approximates `VortexDefenderAllianceSnapshot` from count.
- `VortexDefenderAllianceMaxSize = 24` is the Java `PlayerAlliance` max member count.
- `VortexInvasionRuntime.GetSnapshot(locationId)` returns the current snapshot (nullable).
- `GameServerConnection.HandleVortexDefenderInvitationQuestionResponse` currently calls `Observe(locationId, responder, questionId, responseCode)` with no `existingDefenders` or `defenderAlliance` — these fall back to null/Missing inside the service.
- All live mutation guards remain false throughout. Adding the resolver does not change this.
