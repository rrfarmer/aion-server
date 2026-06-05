# Phase 6 Session 2591 Completion

## UOW

[Phase 6] UOW-2591: Wire keyless static-door open

## Status

Completed and validated with focused live packet coverage. The C# `CM_OPEN_STATICDOOR` branch now opens closed
keyless static doors by reading Java-loaded static-door XML data, mutating the per-instance static door state, and
sending/broadcasting Java-style `SM_EMOTION` open-door packet state `0x9`.

## Runtime Progress Gate

```text
Runtime progress gate:
- Deferred/live behavior being advanced: CM_OPEN_STATICDOOR no longer silently defers for closed keyless static doors.
- Java source method or runtime path: CM_OPEN_STATICDOOR.runImpl -> StaticDoorService.openStaticDoor -> StaticDoor.setOpen(true) -> GeoService.setDoorState + SM_EMOTION(OPEN_DOOR, 0x9).
- C# runtime artifact wired or fixed: GameServerConnection CM_OPEN_STATICDOOR dispatch, StaticDoorTable lookup, IStaticPlaceableStateService per-instance state mutation, SmEmotion open-door send/fanout.
- Client-visible/state effect changed: live packet handling now flips static-door runtime state from closed to open and emits the real open-door emotion packet.
- Why this is not preview-only/test-only/documentation-only: it changes live GameServerConnection packet handling, runtime world/static-door state, and packet output.
```

## Java Source Reviewed

- `CM_OPEN_STATICDOOR.readImpl/runImpl` parses the door id and calls `StaticDoorService.openStaticDoor(player, doorId)`.
- `StaticDoorService.openStaticDoor` resolves a `StaticDoor` from the player's current world map instance.
- `checkStaticDoorKey` returns true for `keyId == 0`; keyed/locked doors require Java `StaticDoor.locked` state and inventory key consumption.
- `StaticDoor.setOpen(true)` removes clickable state, adds opened state, calls `GeoService.setDoorState(worldId, instanceId, staticId, true)`, and broadcasts `SM_EMOTION(staticId, OPEN_DOOR, 0x9)`.

## C# Changes

- Added `IStaticPlaceableStateService` to `GameServerConnection` and threaded it through `GameClientSocketServer`.
- Replaced the deferred `CmOpenStaticDoor` case with live handler dispatch.
- Implemented keyless static door opening from `StaticDoorTable` and `StaticPlaceableStateService`.
- Sent/broadcast `SmEmotion(doorId, EmotionType.OpenDoor, 0x9, ...)` after a successful state flip.
- Left keyed-door behavior intentionally deferred because C# does not yet model Java `StaticDoor.locked` state or key consumption.

## Known Gaps

- Keyed static doors (`keyId > 0`) remain deferred and silent in this handler.
- Java `InstanceHandler.onOpenDoor(doorId)` callback parity is still missing.
- Broader known-list fidelity is partial: the C# path broadcasts through the connection registry when available and falls back to the active connection in isolated handler tests.
- Real client validation was not run.

## Validation Decision

```text
Validation decision:
- Changed surface: live CM_OPEN_STATICDOOR packet handling.
- Specific behavior/contract: a closed keyless static door becomes open in per-instance runtime state and sends SM_EMOTION sender=doorId, emotion=OpenDoor, state=0x9.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionOpenStaticDoorTests|FullyQualifiedName~ClientPacketFactory_ParsesOpenStaticDoorPacket|FullyQualifiedName~SpawnWorldNpcsForInstance_SetsStaticDoorStateLikeJavaStaticDoorSpawnManager" --no-restore -> 4/4 passed.
- Focused Java/Maven command: none; no narrow Java fixture exists, and behavior was taken from reviewed Java source.
- Broad-validation trigger: live packet output and runtime state changed.
- Broad .NET decision: skipped after focused packet/state coverage; the filtered run built Aion.GameServer and covered parser, spawn-state seeding, live handler mutation, keyed defer behavior, and serialized packet shape.
- Why this scope is sufficient: the passing tests drive the real ProcessPacketAsync CM_OPEN_STATICDOOR path and assert the runtime state plus emitted packet payload.
```

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_OPEN_STATICDOOR.runImpl` | `GameServerConnection.ProcessPacketAsync` | Client packet | Partial | Unit Tested | Partial Parity | Keyless branch is live; keyed and instance-handler callback branches remain missing. |
| `StaticDoorService.openStaticDoor` keyless branch | `GameServerConnection.HandleOpenStaticDoorAsync` | Runtime state | Partial | Unit Tested | Partial Parity | Resolves static data from the player's world, checks current door state, and mutates per-instance state. |
| `GeoService.setDoorState` | `StaticPlaceableStateService.SetDoorState` | World/static state | Partial | Unit Tested | Partial Parity | Existing C# per-instance state is used from live packet handling. |
| `SM_EMOTION(staticId, OPEN_DOOR, 0x9)` | `SmEmotion(doorId, EmotionType.OpenDoor, 0x9, ...)` | Packet | Partial | Unit Tested | Partial Parity | Serialized payload assertion covers sender id, emotion id, state, and speed. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_OpenStaticDoorKeylessClosedDoorSetsRuntimeStateAndSendsOpenEmotion` | Unit/live handler | `StaticDoorService.openStaticDoor`, `StaticDoor.setOpen` | Closed keyless door opens and sends open-door emotion packet | Source-reviewed Java + live C# handler assertion | Does not cover connection-registry fanout recipient count. |
| `ProcessPacketAsync_OpenStaticDoorKeyedDoorLeavesDeferredStateUntouched` | Unit/live handler | `StaticDoorService.checkStaticDoorKey` | Keyed door remains deferred until lock/key-consumption state is ported | Source-reviewed Java + no-mutation assertion | Keyed parity is still future work. |

## Summary Metrics

- Focused UOW validation: 4 tests passed.
- Runtime progress: one deferred client packet now mutates live static-door state and emits a real server packet.
- Total Java artifacts touched/discovered this UOW: 4.
- Total C# artifacts touched: 4.
- Artifacts with verified parity: 0.
- Artifacts needing verification / partial parity: 4.
- Blocked artifacts: keyed static-door open branch, static-door instance callbacks, group loot distribution.
- Estimated overall Phase 6 completion: incremental; still far from replacement readiness.

## Remaining Risks

- Keyed door semantics need a C# runtime lock/unlock model and inventory key consumption before they can be safely wired.
- Instance scripts may depend on `onOpenDoor`; C# does not yet call an instance-handler equivalent.
- Real client behavior was not validated against a running Aion client.

## Next Runtime Candidate

UOW-2592 candidate: continue `CM_OPEN_STATICDOOR` into the keyed-door branch only if discovery finds safe existing
inventory mutation/update packet helpers and a minimal per-instance locked-door runtime state shape.

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: keyed CM_OPEN_STATICDOOR attempts should consume an existing key item and open/unlock the door, or send Java's missing-key message.
- Java source method or runtime path: StaticDoorService.checkStaticDoorKey -> Inventory.decreaseByItemId -> StaticDoor.setLocked(false) -> StaticDoor.setOpen(true).
- C# runtime artifact to wire or fix: GameServerConnection.HandleOpenStaticDoorAsync, player inventory mutation/update packet path, StaticPlaceableStateService or equivalent door lock runtime state.
- Client-visible/state/persistence effect expected: key item count changes, door open/locked state changes, inventory update/delete and open-door packet/message output.
- Why this is not preview-only/test-only/documentation-only if feasible: it would execute from live CM_OPEN_STATICDOOR and mutate inventory plus world/static-door state.
```

If keyed-door work requires a metadata-only lock-state adapter first, skip it and select another deferred live packet/state path.
