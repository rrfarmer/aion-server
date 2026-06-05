# Phase 6 Session 2595 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2595: Set private-store name from live packet. See
[Phase-6-Session-2595-Completion.md](Phase-6-Session-2595-Completion.md).

## Commits Made

- `ca69019` - `[Phase 6][UOW-2578] Persist abandoned quest state`
- `559431a` - `[Phase 6][UOW-2579] Persist NPC faction abort state`
- `5aa6cdf` - `[Phase 6][UOW-2580] Re-send assigned NPC faction daily quest`
- `d5a7adf` - `[Phase 6][UOW-2581] Assign NPC faction daily quest`
- `d84e7b53b` - `[Phase 6][UOW-2582] Filter NPC faction daily handlers`
- `ed8eabf35` - `[Phase 6][UOW-2583] Persist quest work item deletes`
- `a767ee5cb` - `[Phase 6][UOW-2584] Wire quest-start item use`
- `5f98483` - `[Phase 6][UOW-2585] Send quest-start rejection messages`
- `84c0f0b` - `[Phase 6][UOW-2586] Send quest-start condition messages`
- `d92efac` - `[Phase 6][UOW-2587] Reject quest-start items at normal quest cap`
- `7c7c3e3` - `[Phase 6][UOW-2588] Send quest-start inventory-item warning`
- `cd9ecd7` - `[Phase 6][UOW-2589] Send quest-start combine-skill warning`
- `0f1a440` - `[Phase 6][UOW-2590] Send quest-start rank warning`
- `b9af493` - `[Phase 6][UOW-2591] Wire keyless static-door open`
- `e2537af` - `[Phase 6][UOW-2592] Open keyed static doors`
- `0b9c1ce` - `[Phase 6][UOW-2593] Close private store from live packet`
- `a5b58e2` - `[Phase 6][UOW-2594] Open private store from live packet`
- Current commit - `[Phase 6][UOW-2595] Set private-store name from live packet`

## Session Summary

- UOW-2593 made zero-item `CM_PRIVATE_STORE` close live.
- UOW-2594 made non-empty `CM_PRIVATE_STORE` open live.
- UOW-2595 made `CM_PRIVATE_STORE_NAME` message mutation and `SM_PRIVATE_STORE_NAME` output live.
- Private-store open/close/name now mutate C# runtime state and send real packets from encoded client-packet dispatch.

## Files Changed In UOW-2595

- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/Player.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionPrivateStoreTests.cs`
- `docs/Phase-6-Session-2595-Completion.md`
- `docs/Phase-6-Session-2595-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_PRIVATE_STORE_NAME#runImpl`
- `com.aionemu.gameserver.services.PrivateStoreService#openPrivateStore`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_PRIVATE_STORE_NAME`

## C# Artifacts Touched

- `Aion.GameServer.Model.GameObjects.Player.PrivateStoreMessage`
- `Aion.GameServer.Network.Aion.GameServerConnection` `CmPrivateStoreName` dispatch
- `Aion.GameServer.Network.Aion.GameServerConnection.HandleOpenPrivateStoreNameAsync`
- `Aion.GameServer.Network.Aion.ServerPackets.SmPrivateStoreName`
- `Aion.GameServer.Tests.GameServerConnectionPrivateStoreTests`

## Validation

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionPrivateStoreTests|FullyQualifiedName~CmPrivateStoreTests|FullyQualifiedName~PrivateStoreOpenPlanServiceTests|FullyQualifiedName~PrivateStoreNameOpenCompositionPlanServiceTests" --no-restore
```

Result: passed, 17/17.

Java/Maven: not run. No narrow Java fixture exists; behavior was verified by source review against Java methods listed
above.

Broad .NET: skipped after focused live packet/state/parser coverage. The filter built `Aion.GameServer` and directly
covered the modified live packet branch plus adjacent parser and packet-plan tests.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `PrivateStoreService.openPrivateStore` message mutation | `HandleOpenPrivateStoreNameAsync` / `Player.PrivateStoreMessage` | Live state | Partial | Unit Tested | Partial Parity | Uses direct player field instead of Java `PrivateStore` object. |
| `SM_PRIVATE_STORE_NAME` broadcast | `SmPrivateStoreName` from live handler | Packet/fanout | Partial | Unit Tested | Partial Parity | Direct-send fallback covered; registry broadcast used when available. |
| Missing store precondition | `HandleOpenPrivateStoreNameAsync` silent return | Runtime guard | Partial | Unit Tested | Partial Parity | Avoids sending or mutating when no C# store state is open. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmPrivateStoreNameSetsStoreMessageAndSendsNamePacket` | Unit/live handler | `PrivateStoreService.openPrivateStore` | Open store gets message and sends `SM_PRIVATE_STORE_NAME` | Source-reviewed Java + live C# handler assertion | Does not cover registry fanout with other visible players. |
| `ProcessPacketAsync_CmPrivateStoreNameMissingStoreReturnsSilently` | Unit/live handler | Java store precondition | Missing store does not mutate or send packets | C# safety behavior aligned with Java expectation that a store exists | Java would throw if misused internally; live packet path should not crash. |

## Known Gaps

- Java's full `PrivateStore` object is not ported; C# uses `Player.PrivateStoreItems` and `Player.PrivateStoreMessage`.
- Live `CM_BUY_ITEM` private-store purchase side effects remain deferred.
- Private-store state is volatile and not persisted.
- Static-door `onOpenDoor` instance callback remains unported because no concrete live C# instance-handler execution surface was found.
- Full `CM_GROUP_LOOT` roll/bid remains blocked by missing drop-distribution runtime state.
- Real client validation was not run.

## Candidate Checks Rejected This Session

### Static-Door `onOpenDoor`

- Java source: `StaticDoorService.openStaticDoor -> WorldMapInstance.getInstanceHandler().onOpenDoor(doorId)`.
- C# has lifecycle hooks for instance create/destroy/leave but no concrete `OnOpenDoor` handler surface found.
- Adding only an interface/callback would be callback-surface scaffolding, not runtime progress.

### `CM_GROUP_LOOT`

- Java source: `CM_GROUP_LOOT.runImpl -> DropDistributionService.handleRollOrBid`.
- Still blocked by missing C# distribution fields: current index, max roll, looting team id, distribution id, in-range players, per-player status, winner, and loot group rules.
- Parser-to-packet wiring would be misleading without the live distribution state.

### `CM_HOUSE_TELEPORT_BACK`

- Java source: reads `player.getBattleReturnCoords()` and `getBattleReturnMap()`, teleports with `FADE_OUT_BEAM`, then clears battle-return state.
- C# has teleport services but no live battle-return fields/source path found, so wiring the packet now would likely be inert.

### `CM_TELEPORT_SELECT`

- Java source: validates known NPC, teleporter template, and teleport location before teleporting.
- C# lacks the teleporter template/location runtime data for this packet path, so a safe live port is not currently scoped.

## Next Recommended Runtime UOW

**UOW-2596 candidate: inspect `CM_BUY_ITEM` player-target action `0` private-store purchase and port only the smallest safe
live side-effect branch.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: buyer CM_BUY_ITEM action 0 against a player private store should mutate buyer/seller inventory and kinah state and send Java-equivalent packets for a narrow valid purchase.
- Java source method or runtime path: CM_BUY_ITEM.runImpl -> PrivateStoreService.sellStoreItem -> getBoughtItems -> decreaseItemFromPlayer -> ItemService.addItem -> kinah transfer -> seller notifications -> close store when empty.
- C# runtime artifact to wire or fix: GameServerConnection CM_BUY_ITEM player-target action 0 branch, Player.PrivateStoreItems, buyer/seller InventoryItems/kinah item state, SmInventoryUpdateItem/SmInventoryAddItem/SmDeleteItem/SmSystemMessage and close-store fanout as applicable.
- Client-visible/state effect expected: a valid purchase transfers item count and kinah, updates seller store item counts, sends inventory/notification packets, and closes the store when sold out.
- Why this is not preview-only/test-only/documentation-only if feasible: it must execute from live CM_BUY_ITEM and mutate real buyer/seller state plus send real packets.
```

If this cannot be scoped safely, select another deferred live packet/state/persistence/runtime-loading path. Do not add
more private-store purchase planner or adapter evidence.

## Safe Runtime Candidates

- A single-item private-store purchase happy path if buyer/seller inventory and kinah mutation can be implemented safely.
- A private-store purchase rejection branch only if it sends/mutates live Java-equivalent behavior from `CM_BUY_ITEM`.
- Another deferred `GameServerConnection` packet path with existing runtime state/repository/service surfaces.
- Java XML/static-data loading only when the data is immediately used by live code.

## Context Needed By Next Session

- `CM_PRIVATE_STORE` zero-item close is live as of UOW-2593.
- `CM_PRIVATE_STORE` non-empty open is live as of UOW-2594.
- `CM_PRIVATE_STORE_NAME` is live as of UOW-2595.
- Live private-store C# state currently consists of `Player.PrivateStoreItems`, `Player.PrivateStoreMessage`, and `PlayerCreatureState.PrivateShop`.
- Existing disabled private-store planner services remain in the tree but are no longer the live open/close/name path.
- Avoid preview/metadata/evidence-only work. Each next UOW must mutate live state, send real packets, persist/restore live state, load runtime-used Java data, or execute a live handler path.
