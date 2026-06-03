# Phase 6 Session 2510 Completion

## UOW

[Phase 6] UOW-2510: Add guarded Vortex update-alliance runtime adapter

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`
- `com.aionemu.gameserver.model.vortex.VortexLocation.getPlayers`
- `com.aionemu.gameserver.model.gameobjects.player.Player.getRace`
- `com.aionemu.gameserver.model.gameobjects.player.ResponseRequester.putRequest`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_QUESTION_WINDOW.java`

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Services/VortexDefenderAllianceUpdatePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

## Implementation Notes

- Added `VortexDefenderAllianceUpdateRuntimeAdapterService`, an opt-in adapter that accepts a `VortexLocationSummary`, live `Player` candidates from the location, existing defender snapshots, and defender-alliance state.
- The adapter snapshots supplied players, reuses `VortexDefenderAllianceUpdatePlanService` for Java-style defender-race filtering, then passes only selected defender `Player` instances into `VortexDefenderInvitationBatchRuntimeAdapterService`.
- The report exposes update-alliance counts, skipped location-player ids, batch registration counts, request storage counts, question-window intent counts, and Java source traceability for `Invasion.updateAlliance -> Invasion.updateDefenders`.
- Java `String.equals` race matching is represented with ordinal string comparison through the existing update-alliance planner; lower-case race strings do not match.
- Scope remains guarded. It does not scan a production `VortexLocation` container, call `SendPacketAsync`, execute callbacks, mutate groups, mutate alliances, mutate defender maps, schedule, spawn, despawn, or start portals.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `DefenderAllianceUpdateRuntimeAdapter_FiltersJavaDefenderRaceAndRegistersOnlySelectedPlayers` | Unit | `Invasion.updateAlliance`, `Invasion.updateDefenders`, `ResponseRequester.putRequest`, `SM_QUESTION_WINDOW` source review | Runtime update-alliance adapter filters supplied players by exact defender race, invokes batch registration for selected defenders only, preserves occupied-slot and already-defender outcomes, and creates packet intent only for stored requests | Focused C# test validates selected/skipped ids, registration/rejection/skip counts, request registry mutation only for selected defenders, lower-case race skip, occupied request preservation, Java source traceability, and disabled side-effect flags | Supplied `Player` list stands in for production `VortexLocation.getPlayers().values()` |
| `DefenderAllianceUpdateRuntimeAdapter_EmptyLocationPlayersAvoidsRegistrationMutation` | Unit | `Invasion.updateAlliance` source review | Null/empty location player input produces no update-defenders calls and no request/packet-intent mutation | Focused C# test validates empty selected/skipped ids, zero registration counts, empty batch report, full-alliance state carry-through, and disabled side-effect flags | Does not execute Java runtime |

## Validation Decision

- Changed surface: one C# opt-in update-alliance runtime adapter/report plus focused tests.
- Specific behavior/contract: Java `Invasion.updateAlliance` iterates `getVortexLocation().getPlayers().values()`, selects players whose `player.getRace().equals(getVortexLocation().getDefendersRace())`, and calls `updateDefenders` for each selected defender. C# now applies the existing planner filter to supplied live `Player` instances, invokes guarded batch registration for selected defenders only, and keeps live side effects disabled.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~QuestionResponseRegistryTests" --no-restore
```

- Result: Passed, 125 tests. Existing nullable/analyzer warnings were emitted from unrelated files and prior test lines.
- Focused Java/Maven command: skipped; no Java source or fixtures changed, and no narrow Java update-alliance fixture exists for this seam.
- Broad-validation trigger: none. This adapter is opt-in and does not enable packet dispatch, live lifecycle dispatch, scheduler behavior, persistence, or shared runtime mutation.
- Broad .NET decision: full project/solution validation skipped because the focused command covered the edited Vortex tests and adjacent response-registry contract.
- Why this scope is sufficient: the changed surface is isolated to guarded composition over existing planner and batch runtime adapters; focused tests cover exact race filtering, selected-player registration, skipped-player non-mutation, occupied request slots, already-defender skip, disabled live sends, and disabled gameplay mutation.

## Additional Hygiene

```powershell
git diff --check
git diff --cached --check
```

- `git diff --check` passed. Git reported expected CRLF conversion warnings for edited C# source.
- `git diff --cached --check` passed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.vortex.Invasion.updateAlliance` | `Aion.GameServer.Services.VortexDefenderAllianceUpdateRuntimeAdapterService` | Runtime Vortex update-alliance adapter | Partial | Unit Tested | Partial Parity | C# now filters supplied live `Player` candidates by exact defender race and invokes guarded batch registration for matching defenders. Production `VortexLocation.getPlayers()` container wiring, live lifecycle dispatch, packet sending, and gameplay mutations remain disabled. |
| `com.aionemu.gameserver.services.vortex.Invasion.updateDefenders` | `Aion.GameServer.Services.VortexDefenderInvitationBatchRuntimeAdapterService` / `Aion.GameServer.Services.VortexDefenderUpdateDefendersRegistrationRuntimeAdapterService` | Runtime Vortex defender registration adapter | Partial | Unit Tested | Partial Parity | C# update-alliance runtime path reuses guarded update-defenders registration and packet-intent adapters. Live callback execution and defender/alliance mutations remain disabled. |
| `com.aionemu.gameserver.model.vortex.VortexLocation.getPlayers` | `IReadOnlyList<Aion.GameServer.Model.GameObjects.Player>` input to `VortexDefenderAllianceUpdateRuntimeAdapterService` | Runtime location player source | Partial | Unit Tested | Partial Parity | C# accepts supplied location players rather than reading a production Vortex location container. Collection map semantics and production lifecycle integration remain unimplemented. |
| `com.aionemu.gameserver.model.gameobjects.player.Player.getRace` | `Aion.GameServer.Model.GameObjects.Player.Race` / `VortexZonePlayerSnapshot.Race` | Player state field | Partial | Unit Tested | Partial Parity | Exact ordinal string matching is covered for matching defender race and lower-case non-match. Broader Java race enum/object semantics remain outside this seam. |

## Metrics

- Total Java artifacts discovered in this UOW: 5
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 45%

## Remaining Risks

- Live Vortex `updateAlliance` is still not wired to production `VortexLocation.getPlayers().values()`.
- The runtime adapter accepts supplied player candidates; it does not own world/location container lookup or map semantics.
- `SM_QUESTION_WINDOW` packet dispatch remains disabled.
- Vortex response-handler callbacks, acceptance flow, add-player transition, and alliance mutation remain metadata-only.
- Production world/location/alliance containers remain absent from the Vortex start path.
