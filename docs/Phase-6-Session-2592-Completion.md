# Phase 6 Session 2592 Completion

## UOW

[Phase 6] UOW-2592: Open keyed static doors

## Status

Completed and validated with focused live packet coverage. The C# `CM_OPEN_STATICDOOR` branch now handles Java-style
keyed static doors: missing keys send `STR_CANNOT_OPEN_DOOR_NEED_KEY_ITEM`, and present keys are consumed from live
inventory before the door is unlocked, opened, and announced with `SM_EMOTION(OPEN_DOOR, 0x9)`.

## Runtime Progress Gate

```text
Runtime progress gate:
- Deferred/live behavior being advanced: keyed CM_OPEN_STATICDOOR attempts now execute Java key checks instead of remaining deferred.
- Java source method or runtime path: StaticDoorService.checkStaticDoorKey -> Inventory.decreaseByItemId -> StaticDoor.setLocked(false) -> StaticDoor.setOpen(true).
- C# runtime artifact wired or fixed: GameServerConnection.HandleOpenStaticDoorAsync, TryUnlockStaticDoorAsync, StaticPlaceableStateService locked-door state, SmSystemMessage.CannotOpenDoorNeedKeyItem.
- Client-visible/state effect changed: missing-key attempts send message id 1300723; valid key attempts decrement/delete the key item, unlock/open the door state, send inventory update/delete, and send the open-door emotion.
- Why this is not preview-only/test-only/documentation-only: it changes live packet handling, live player inventory state, live static-door state, and real server packet output.
```

## Java Source Reviewed

- `StaticDoorService.checkStaticDoorKey` returns false silently for `keyId == 1`.
- For unlocked doors, Java returns true without consuming another key.
- For locked keyed doors, Java calls `player.getInventory().decreaseByItemId(keyId, 1)`.
- Missing key sends `SM_SYSTEM_MESSAGE.STR_CANNOT_OPEN_DOOR_NEED_KEY_ITEM()` message id `1300723`.
- Successful key consumption calls `door.setLocked(false)` and then `StaticDoor.setOpen(true)`.

## C# Changes

- Added per-instance static-door locked-state tracking to `IStaticPlaceableStateService` / `StaticPlaceableStateService`.
- Updated `HandleOpenStaticDoorAsync` so `keyId == 1` stays Java-silent, `keyId >= 2` defaults locked, and unlocked keyed doors can open without another key.
- Added `TryUnlockStaticDoorAsync` to find and consume a live key item by `ItemId`.
- Added `SmSystemMessage.CannotOpenDoorNeedKeyItem()` for message id `1300723`.
- Added keyed-door tests for missing key and stack decrement/unlock/open behavior.

## Known Gaps

- Single-count key deletion sends `SM_DELETE_ITEM`; focused tests currently cover stack decrement, not deletion.
- Inventory persistence is not immediate in this handler; it follows the existing live in-memory mutation style used by nearby item-use branches.
- Java `WorldMapInstance.getInstanceHandler().onOpenDoor(doorId)` callback parity is still missing.
- Real client validation was not run.

## Validation Decision

```text
Validation decision:
- Changed surface: live CM_OPEN_STATICDOOR keyed-door packet handling.
- Specific behavior/contract: missing key sends message id 1300723; present key stack decrements, locked state becomes false, door opens, and open-door packet follows inventory update.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionOpenStaticDoorTests|FullyQualifiedName~ClientPacketFactory_ParsesOpenStaticDoorPacket|FullyQualifiedName~SpawnWorldNpcsForInstance_SetsStaticDoorStateLikeJavaStaticDoorSpawnManager" --no-restore -> 5/5 passed.
- Focused Java/Maven command: none; no narrow Java fixture exists, and behavior was taken from reviewed Java source.
- Broad-validation trigger: live packet output, inventory state, and static-door state changed.
- Broad .NET decision: skipped after focused packet/state coverage; the filter built Aion.GameServer and covered parser, spawn-state seeding, keyless open, missing-key feedback, keyed stack decrement, locked-state flip, and open-door packet output.
- Why this scope is sufficient: the passing tests drive the real ProcessPacketAsync CM_OPEN_STATICDOOR path and assert the Java-equivalent keyed branch effects.
```

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `StaticDoorService.checkStaticDoorKey` missing-key branch | `GameServerConnection.TryUnlockStaticDoorAsync` | Client feedback | Partial | Unit Tested | Partial Parity | Sends `1300723` from live handler when no matching key item exists. |
| `Inventory.decreaseByItemId(keyId, 1)` | `TryUnlockStaticDoorAsync` inventory mutation | Inventory state | Partial | Unit Tested | Partial Parity | Stack decrement path is covered; single-stack deletion path is implemented but not separately tested. |
| `StaticDoor.setLocked(false)` | `StaticPlaceableStateService.SetDoorLockedState(..., false)` | World/static state | Partial | Unit Tested | Partial Parity | Tracks per-instance locked state in runtime memory. |
| `StaticDoor.setOpen(true)` | `GameServerConnection.HandleOpenStaticDoorAsync` | World/static state + packet | Partial | Unit Tested | Partial Parity | Reuses UOW-2591 open-state mutation and open-door emotion packet. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_OpenStaticDoorKeyedDoorWithoutKeySendsNeedKeyMessage` | Unit/live handler | `StaticDoorService.checkStaticDoorKey`, `SM_SYSTEM_MESSAGE` | Missing key leaves door closed and sends `1300723` | Source-reviewed Java + live C# handler assertion | Does not cover named-key message; Java path used here sends generic key message. |
| `ProcessPacketAsync_OpenStaticDoorKeyedDoorWithKeyConsumesKeyUnlocksAndOpens` | Unit/live handler | `Inventory.decreaseByItemId`, `StaticDoor.setLocked`, `StaticDoor.setOpen` | Key stack decrements, door unlocks/opens, inventory update precedes open-door packet | Source-reviewed Java + live C# handler assertion | Does not cover key stack deletion at count 1. |

## Summary Metrics

- Focused UOW validation: 5 tests passed.
- Runtime progress: keyed static doors now affect live inventory and static-door runtime state from a real client packet.
- Total Java artifacts touched/discovered this UOW: 5.
- Total C# artifacts touched: 4.
- Artifacts with verified parity: 0.
- Artifacts needing verification / partial parity: 4.
- Blocked artifacts: static-door instance callbacks, full group loot distribution.
- Estimated overall Phase 6 completion: incremental; still far from replacement readiness.

## Remaining Risks

- Instance handlers/scripts may depend on the missing `onOpenDoor` callback.
- Inventory persistence semantics for consumed keys may need tightening when the broader inventory save scheduler is reviewed.
- Real client behavior was not validated against a running Aion client.

## Next Runtime Candidate

UOW-2593 candidate: static-door `onOpenDoor` instance callback only if C# has a concrete live instance handler surface
that can execute behavior from the live door-open path.

If no live instance-handler surface exists, skip callback scaffolding and select another deferred packet/state path.
