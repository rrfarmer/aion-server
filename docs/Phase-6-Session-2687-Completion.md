# Phase 6 Session 2687 Completion

## UOW

[Phase 6] UOW-2687: Preserve CM_CHARGE_ITEM selected item order.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: live CM_CHARGE_ITEM multi-item charging now processes selected item object ids in client packet order, matching Java's itemObjectIds list.
- Java source/runtime path: CM_CHARGE_ITEM.runImpl reads itemObjectIds into an ArrayList, resolves each inventory item in that order, then ItemChargeService.chargeItems iterates that ordered collection.
- C# runtime artifact wired: GameServerConnection.HandleChargeItemAsync.
- Client-visible/state/persistence effect: when AP/Kinah only covers part of a multi-item charge request, the item selected first in the client packet is charged first, mutating the matching inventory item/AP and emitting packets for that item.
- Why this is runtime progress: it changes live inventory/AP mutation and packet emission order from a client packet handler; it is not preview-only/test-only/documentation-only.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_CHARGE_ITEM.java`
  - Reads target NPC id, charge level, and item object ids into an `ArrayList`.
  - Checks `player.isTargeting(targetNpcObjectId)`.
  - Resolves each requested item from `player.getInventory().getItemByObjId` in packet order and appends existing items to `itemsToCharge`.
  - Calls `ItemChargeService.chargeItems(player, itemsToCharge, chargeLevel, false, true)`.
- `game-server/src/com/aionemu/gameserver/services/item/ItemChargeService.java`
  - Iterates the ordered `Collection<Item>`.
  - Processes per-item payment before charge mutation.
  - Sends per-item inventory/system/stats packets, then sends the charge-all-complete message for successful charge ways.

## C# Changes

- Changed `GameServerConnection.HandleChargeItemAsync` to iterate `packet.ItemObjectIds` directly.
- Each packet id now resolves the current item from the working inventory list before planning charge/payment/mutation.
- Removed the previous hash-set plus inventory-order iteration that could charge a different selected item first.
- Added a multi-item `CmChargeItem` test packet helper.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleChargeItemAsync_ProcessesSelectedItemsInPacketOrderLikeJava` | Unit / live connection handler | `CM_CHARGE_ITEM.runImpl` ordered `itemObjectIds` list plus `ItemChargeService.chargeItems` iteration | With two selected AP-charge items, inventory order `[7001,7002]`, packet order `[7002,7001]`, and AP for only one item, C# charges `7002`, spends AP, persists one mutation, and emits packets for `7002`. | Socket-backed connection invocation of the live private handler through reflection, runtime-loaded item templates, repository mutation counters, player AP/inventory assertions, and packet assertions. | Does not prove duplicate item-id request behavior or mixed charge-way completion ordering. |

## Validation Decision

```text
- Changed surface: live CM_CHARGE_ITEM handler loop order plus focused connection test helper.
- Specific behavior/contract: Java preserves client packet item-object-id order when charging selected items, which affects partial-success payment/mutation/packet results.
- Focused C# command attempted first: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests" --logger "console;verbosity=minimal"
- First result: timed out after 124 seconds before producing test results; this class-level filter is too broad for the current focused validation policy.
- Focused C# command used: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleChargeItemAsync_ProcessesSelectedItemsInPacketOrderLikeJava|FullyQualifiedName~HandleChargeItemAsync_ApPaymentSendsAbyssPointsPlannerPackets|FullyQualifiedName~HandleChargeItemAsync_ApPaymentRejectsInsufficientAbyssPointsWithoutSideEffects" --logger "console;verbosity=minimal"
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java fixture exists for this packet-order branch in this checkout.
- Broad-validation trigger: none. The change is isolated to one live handler loop and directly adjacent charge-item tests.
- Broad .NET decision: skipped; the narrowed filtered test built the affected projects and proved the edited runtime ordering plus adjacent AP charge success/failure behavior.
- Why this scope is sufficient: the regression exercises the exact Java-derived ordering branch where old C# behavior would choose the wrong item under limited AP.
```

Result:

- Focused C# validation passed: 3/3.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_CHARGE_ITEM.runImpl` | `GameServerConnection.HandleChargeItemAsync` | Live client packet handler | Partial | Unit Tested | Partial Parity | Selected item order now follows packet order. Complete charge-item parity is not claimed. |
| `ItemChargeService.chargeItems` | `ItemChargeService.CreateChargePlan` plus `GameServerConnection.HandleChargeItemAsync` loop | Service / live item mutation | Partial | Unit Tested indirectly | Partial Parity | Per-item iteration order is covered through the live handler. Other Java charge branches and duplicate-id behavior remain partially verified. |

## Known Gaps

- Complete `CM_CHARGE_ITEM` parity is not claimed.
- Duplicate item object ids in a single charge request were not separately tested.
- Mixed charge-way completion message ordering remains dependent on hash-set behavior and was not changed in this UOW.
- Broad class-level validation timed out; the committed evidence is the narrowed focused 3-test command.
- Java/Maven runtime comparison was not run.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2
- Total artifacts ported or extended in this UOW: 2
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 2
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%

## Next Runtime UOW Candidates

1. Inspect `CM_CHARGE_ITEM` duplicate item-id behavior or mixed charge-way complete-message ordering only if discovery confirms a concrete live packet/state mismatch.
2. Find another deferred item-use or inventory packet path where Java source and existing C# inventory services are sufficient to wire a small live mutation.
3. Move to a deferred quest, AI, zone, command, or dynamic handler path only when Java source contains a concrete non-empty runtime effect and C# has enough infrastructure to execute it.
