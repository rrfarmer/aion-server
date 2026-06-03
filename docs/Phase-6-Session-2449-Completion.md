# Phase 6 Session 2449 Completion

## UOW

[Phase 6] UOW-2449: Bridge Vortex portal pass tracking into active invasion runtime

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/controllers/RVController.java`
- `game-server/src/com/aionemu/gameserver/model/vortex/VortexLocation.java`
- `game-server/src/com/aionemu/gameserver/services/VortexService.java`
- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`
- `game-server/src/com/aionemu/gameserver/services/vortex/DimensionalVortex.java`

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Services/RiftPortalUseService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/VortexInvasionRuntime.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/RiftPortalUseServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

## Implementation Notes

- Added `VortexInvasionRuntime.RecordPortalPass` to mirror Java `RVController.acceptRequest` storing a vortex passer in `passedPlayers`.
- Added `VortexInvasionRuntime.AddInvaderFromPassedPortal` to mirror Java `VortexLocation.onEnterZone` promoting a passed invader into `Invasion.addPlayer(player, true)`.
- `RiftPortalUseService` now optionally receives `VortexInvasionRuntime` and `VortexLocationService`. When a vortex portal is accepted and an active runtime location exists, it records the passed player alongside the existing `RiftPortalState` pass count.
- The runtime requires an active invasion state before recording portal passes. This preserves Java sequencing: `VortexService.startInvasion` creates the active invasion before spawned vortex portal use.
- Scope is intentionally limited to portal pass tracking and active-invader promotion. Online `Invasion.kickPlayer` packet/teleport behavior, defender invitation/alliance logic, and full Vortex spawn/start/stop lifecycle wiring remain future work.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `AcceptPortal_ForVortexRift_RecordsPassedPlayerInVortexRuntimeLikeJavaController` | Unit | `RVController.acceptRequest` | Accepted vortex portal use teleports, updates portal passed count, and records the passer in `VortexInvasionRuntime` | Runtime snapshot has passed player and no active invader until zone promotion | Uses injected resolver instead of full spawned portal/location lookup |
| `AddInvaderFromPassedPortal_PromotesOnlyRecordedPortalPassLikeJavaZoneEntry` | Unit | `VortexLocation.onEnterZone` | Only a player recorded in passed-player state can be promoted into active invaders | Unpassed player is blocked, passed player joins, duplicate join is ignored | Does not handle defender prompt/alliance branch |

## Validation Decision

- Changed surface: production-capable Vortex portal pass state mutation through `RiftPortalUseService` and `VortexInvasionRuntime`.
- Specific behavior/contract: Java vortex portal acceptance records `passedPlayers`, and Java zone entry promotes passed invaders into active invasion participants.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~Vortex|FullyQualifiedName~RiftPortalUseServiceTests|FullyQualifiedName~RiftServiceTests|FullyQualifiedName~PlayerAllianceOfflineTimeoutDispatchServiceTests" --no-restore
```

- Result: Passed, 34 tests. Existing nullable/analyzer warnings were emitted.
- Focused Java/Maven command: skipped; no Java source or Java fixtures changed, and no narrow Java Vortex fixture exists for this runtime slice.
- Broad-validation trigger: present because production-capable portal use can now mutate shared Vortex runtime state when an active Vortex runtime exists.
- Broad .NET decision: full project/solution validation was skipped after focused validation passed because the mutation is isolated to Vortex/Rift portal pass state, and the focused command built the affected project while covering Vortex runtime, rift portal use, rift service adjacency, and alliance timeout cleanup that consumes active invader state.
- Why this scope is sufficient: no packet primitives, persistence mapping, database schema, crypto, scheduler, or socket framing changed. Full spawned Vortex lifecycle remains explicitly unclaimed.

## Additional Hygiene

```powershell
git diff --check
```

- Passed with expected LF-to-CRLF working-tree warnings for touched C# files.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.controllers.RVController.acceptRequest` | `Aion.GameServer.Services.RiftPortalUseService.AcceptPortal` plus `VortexInvasionRuntime.RecordPortalPass` | Controller/service side effect | Partial | Unit Tested | Partial Parity | Vortex portal pass tracking is now bridged to runtime; Java team removal and open notice are handled in `RiftPortalInteractionService`; full request handler lifecycle remains partial. |
| `com.aionemu.gameserver.model.vortex.VortexLocation.onEnterZone` | `Aion.GameServer.Services.VortexInvasionRuntime.AddInvaderFromPassedPortal` | Zone/runtime side effect | Partial | Unit Tested | Partial Parity | Passed invader promotion is ported as a runtime method; no live zone handler wiring yet. |
| `com.aionemu.gameserver.services.vortex.Invasion.addPlayer` | `Aion.GameServer.Services.VortexInvasionRuntime.AddInvaderFromPassedPortal` | Vortex participant mutation | Partial | Unit Tested | Partial Parity | Invader participant insertion is covered; Java group/alliance removal and alliance create/add logic are not ported in this method. |
| `com.aionemu.gameserver.services.VortexService` | `Aion.GameServer.Services.VortexInvasionRuntime` and `VortexLocationService` | Service/runtime | Partial | Unit Tested | Partial Parity | Active invasion state exists and can track portal passers/invaders; start/stop spawn lifecycle remains unported. |

## Metrics

- Total Java artifacts discovered in this UOW: 5
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 41%

## Remaining Risks

- Full Vortex active lifecycle population is still incomplete: C# does not yet port `VortexService.startInvasion`, `stopInvasion`, spawn/despawn, or generator death observer wiring.
- Online Vortex kick parity remains incomplete: Java sends `1401452` or `1401476`, may send `1401474`, and teleports online invaders home.
- Defender zone entry prompt and defender alliance logic are not ported.
- Live zone handler wiring for `VortexLocation.onEnterZone/onLeaveZone` is not present; tests call runtime methods directly.
