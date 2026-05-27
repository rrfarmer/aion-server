# AP Extraction Atomicity Audit

Date: 2026-05-27
Unit of Work: UOW-1438

## Scope

Document the known Java/C# transaction-boundary difference for AP extraction after UOW-1436 aligned the packet-visible delete/cube order.

Java source of truth:

- `game-server/src/com/aionemu/gameserver/model/templates/item/actions/ApExtractAction.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`

C# implementation:

- `dotnetConversion/src/Aion.GameServer/Services/ApExtractService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`

## Java Behavior

`ApExtractAction.act` executes the AP extraction mutation in three visible steps:

1. `inventory.delete(targetItem)`
   - Removes the target from storage.
   - Sends `SM_DELETE_ITEM(target, DEFAULT)` with delete mask `0x00`.
   - Sends `SM_CUBE_UPDATE`.
2. `inventory.decreaseByObjectId(parentItem.getObjectId(), 1)`
   - If the tool stack remains, sends `SM_INVENTORY_UPDATE_ITEM` with `DEC_ITEM_USE`.
   - If the tool stack is exhausted, sends `SM_DELETE_ITEM(tool, USE)` with delete mask `0x17`, then `SM_CUBE_UPDATE`.
3. `AbyssPointsService.addAp(player, ap)`
   - Runs only if the target delete and tool consume both succeeded.

The theoretical partial edge is therefore:

- Target delete succeeds and target delete/cube packets are sent.
- Tool consume fails.
- No tool consume packet is sent.
- No AP gain message or `SM_ABYSS_RANK` is sent.
- Java does not roll back the target delete.

## Reachability Assessment

The partial edge was source-reviewed as theoretical under normal same-client gameplay:

- Java packet handling processes one packet at a time for a client, so duplicate AP-extract packets from the same connection should not interleave between target delete and tool consume.
- `Storage.delete` triggers item-removed side effects, but the reviewed path does not remove the extraction tool.
- The edge could matter only if another thread or invalid runtime state removes/corrupts the tool during the tiny synchronous window between target delete and tool consume.

Because no ordinary Java gameplay path was identified, this edge is not currently treated as a required live behavior.

## C# Behavior

C# intentionally keeps AP extraction atomic:

- `ApExtractService.CreateMutationPlan` produces either a complete success plan or a failure.
- `PlayerEnterWorldRepository.SaveApExtractActionMutationAsync` stores the target delete, tool mutation, and abyss-rank update in one transaction.
- `GameServerConnection.HandleApExtractUseItemAsync` sends no success packets and mutates no runtime state when persistence fails.

This is an intentional C# persistence safety difference. It avoids sending a target-delete success packet for a database mutation that cannot be committed. Do not mark this as verified Java parity without runtime evidence.

## Decision

Keep the C# atomic AP extraction boundary unless Java runtime captures or production evidence show the partial target-delete/tool-consume failure edge is observable and important.

If the edge must be modeled later, add an explicit fault-injection result shape such as `TargetDeletedButToolConsumeFailed` and test:

- Runtime target removal.
- `SM_DELETE_ITEM(target, 0x00)`.
- `SM_CUBE_UPDATE`.
- No tool consume packet.
- No AP gain message.
- No `SM_ABYSS_RANK`.

## Migration Parity Table - UOW-1438

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.item.actions.ApExtractAction` | `Aion.GameServer.Services.ApExtractService` / `GameServerConnection.HandleApExtractUseItemAsync` | Item Action / Service / Connection Packet Caller | Refactored | Regression Tested in prior unit | Intentional Difference | Packet-visible success path was aligned in UOW-1436. The theoretical Java target-delete-success/tool-consume-failure edge remains intentionally atomic in C# unless runtime evidence shows it matters. |
| `com.aionemu.gameserver.model.items.storage.Storage.delete` | `PlayerEnterWorldRepository.SaveApExtractActionMutationAsync` | Storage / Persistence Boundary | Refactored | Manual Only | Intentional Difference | Java sends during storage mutation and persists dirty state later; C# persists target/tool/AP mutation atomically before sending success packets. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseByObjectId` | `ApExtractService.CreateMutationPlan` / repository transaction | Storage Count Mutation | Refactored | Manual Only | Intentional Difference | Java could theoretically fail tool consume after target delete; C# does not expose a partial plan shape. Normal same-client Java packet processing makes the edge unlikely. |
| `com.aionemu.gameserver.services.abyss.AbyssPointsService.addAp` | `Aion.GameServer.Services.AbyssPointsService.CreateAddApPlan` plus AP extraction save/send path | Service | Partial | Regression Tested in prior units | Partial Parity | Local AP gain packets are covered in existing AP extraction tests. Legion contribution, siege callback, and deeper rank-change side effects remain broader AP parity gaps. |

## Remaining Risks

- No Java runtime capture proves the theoretical partial edge is impossible.
- C# has no fault-injection seam for target-deleted/tool-consume-failed AP extraction.
- Broader AP side effects remain tracked separately: legion contribution, siege callbacks, rank-limited equipment persistence, and abyss skill refresh details.
- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
