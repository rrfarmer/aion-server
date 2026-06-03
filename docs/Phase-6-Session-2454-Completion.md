# Phase 6 Session 2454 Completion

## UOW

[Phase 6] UOW-2454: Add Vortex removal rift-entry update planner

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/controllers/RVController.java`
- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_RIFT_ANNOUNCE.java`
- `game-server/src/com/aionemu/gameserver/services/rift/RiftInformer.java`

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Services/VortexPassedPlayerSyncRiftEntryUpdateService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexPassedPlayerSyncRiftEntryUpdateServiceTests.cs`

## Implementation Notes

- Added a non-live planner that consumes `VortexPassedPlayerSyncPlan` and an active `RiftPortalState`.
- The planner applies Java `RVController.syncPassed(true)` semantics through `portal.SyncPassed(syncPlan.UsePassedPlayerCount, syncPlan.PassedPlayerCount)`.
- The planner creates a `SmRiftAnnounce(portal, isMaster: false)` packet intent matching Java `RiftInformer.sendRiftInfo` entry-update behavior.
- Missing sync-plan and missing-portal cases are represented explicitly and do not mutate portal state or create packet intents.
- Scope remains intentionally non-live. This UOW does not resolve spawned portal state, broadcast to worlds, or call connection dispatch.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `CreatePlan_AppliesPassedCountAndCreatesPortalEntryUpdatePacketLikeJavaSyncPassed` | Unit | `RVController.syncPassed(true)` and `SM_RIFT_ANNOUNCE(controller, false)` | Planner updates `UsedEntries` to remaining passed-player count and serializes the non-master rift entry update packet | C# packet payload validates action `3`, portal object id, used entries, remaining time, and vortex type | Does not dispatch packet to live worlds |
| `CreatePlan_MissingSyncPlanDoesNotMutatePortalOrCreatePacket` | Unit | `Invasion.kickPlayer` produces sync metadata before rift fanout | Missing removal sync metadata does not mutate the portal or create a packet | C# guard test preserves existing `UsedEntries` | Does not cover live removal flow |
| `CreatePlan_MissingPortalKeepsSyncPlanAsMetadataOnly` | Unit | `RiftInformer.sendRiftInfo` requires active controller state | Missing portal keeps sync metadata only and avoids packet creation | C# guard test keeps the sync plan intact | Portal resolution remains future work |

## Validation Decision

- Changed surface: Vortex removal rift-entry update planner and focused packet-intent tests.
- Specific behavior/contract: Java `Invasion.kickPlayer` removes a player from `passedPlayers`, calls `RVController.syncPassed(true)`, and `RiftInformer.sendRiftInfo` emits `SM_RIFT_ANNOUNCE(controller, false)` with updated entry count.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexPassedPlayerSyncRiftEntryUpdateServiceTests|FullyQualifiedName~VortexLocationServiceTests|FullyQualifiedName~RiftAnnouncePacketTests|FullyQualifiedName~RiftPortalUseServiceTests" --no-restore
```

- Result: Passed, 30 tests.
- Focused Java/Maven command: skipped; no Java source or fixtures changed, and the source-of-truth behavior was reviewed directly in `RVController.syncPassed(true)`, `Invasion.kickPlayer`, `RiftInformer`, and `SM_RIFT_ANNOUNCE`.
- Broad-validation trigger: none. This UOW adds a planner/adapter and packet-intent tests only; it does not enable live fanout, scheduler wiring, production portal lookup, packet primitive changes, persistence, or connection dispatch.
- Broad .NET decision: skipped; focused validation built the affected project and covered planner behavior plus adjacent Vortex, rift packet, and portal-use surfaces.
- Why this scope is sufficient: the change is isolated to a non-live adapter and keeps all broadcast behavior as future work.

## Additional Hygiene

```powershell
git diff --check
```

- Passed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.controllers.RVController.syncPassed` | `Aion.GameServer.Services.VortexPassedPlayerSyncRiftEntryUpdateService` plus existing `RiftPortalState.SyncPassed` | Controller/runtime state update | Partial | Unit Tested | Partial Parity | Removal sync metadata can now be applied to a supplied portal state using Java `passedPlayers.size()` semantics. Live portal resolution remains unported. |
| `com.aionemu.gameserver.services.rift.RiftInformer.sendRiftInfo` | `Aion.GameServer.Services.VortexPassedPlayerSyncRiftEntryUpdateService` | Rift update planning | Partial | Unit Tested | Partial Parity | Planner creates the non-master `SmRiftAnnounce` packet intent but does not broadcast to world players. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_RIFT_ANNOUNCE` | `Aion.GameServer.Network.Aion.ServerPackets.SmRiftAnnounce` | Packet | Partial | Unit Tested | Partial Parity | New tests verify the removal-side entry-update packet payload for Vortex portals. |

## Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 3
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 41%

## Remaining Risks

- Removal-side live fanout is still incomplete because active Vortex removal flow does not resolve a spawned `RiftPortalState`.
- `RiftInformer.sendRiftInfo(getWorldsList(this))` world targeting is not yet wired for removals.
- Full Vortex start/stop spawn lifecycle remains incomplete.
- Defender alliance lifecycle and live zone handler wiring remain incomplete.
- Future live fanout will be a broad-validation trigger if it invokes `IGameClientConnectionRegistry` or production portal state.
