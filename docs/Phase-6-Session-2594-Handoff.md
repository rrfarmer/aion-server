# Phase 6 Session 2594 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2594: Open private store from live packet. See
[Phase-6-Session-2594-Completion.md](Phase-6-Session-2594-Completion.md).

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
- Current commit - `[Phase 6][UOW-2594] Open private store from live packet`

## Session Summary

- UOW-2593 made zero-item `CM_PRIVATE_STORE` close live from packet dispatch.
- UOW-2594 made non-empty `CM_PRIVATE_STORE` create/open live from packet dispatch.
- Private-store open now has a live seller-state representation via `Player.PrivateStoreItems`, and open/close both send real `SM_EMOTION` packets.
- The broader Java `PrivateStore` object model and private-store purchase side effects remain unported.

## Files Changed In UOW-2594

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionPrivateStoreTests.cs`
- `docs/Phase-6-Session-2594-Completion.md`
- `docs/Phase-6-Session-2594-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_PRIVATE_STORE#runImpl`
- `com.aionemu.gameserver.services.PrivateStoreService#createStoreWithItems`
- `com.aionemu.gameserver.services.PrivateStoreService#canOpenPrivateStore`
- `com.aionemu.gameserver.services.PrivateStoreService#validateItem`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection` `CmPrivateStore` dispatch
- `Aion.GameServer.Network.Aion.GameServerConnection.HandleCreatePrivateStoreAsync`
- `Aion.GameServer.Model.GameObjects.Player.PrivateStoreItems`
- `Aion.GameServer.Model.GameObjects.PlayerCreatureState`
- `Aion.GameServer.Services.PrivateStoreOpenGuardPlanService`
- `Aion.GameServer.Services.PrivateStoreItemValidationPlanService`
- `Aion.GameServer.Tests.GameServerConnectionPrivateStoreTests`

## Validation

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionPrivateStoreTests|FullyQualifiedName~CmPrivateStoreTests" --no-restore
```

Result: passed, 9/9.

Java/Maven: not run. No narrow Java fixture exists; behavior was verified by source review against Java methods listed
above.

Broad .NET: skipped after focused live packet/state/parser coverage. The filter built `Aion.GameServer` and directly
covered the modified live packet branch plus adjacent private-store parser/name coverage.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `PrivateStoreService.createStoreWithItems` success | `HandleCreatePrivateStoreAsync` | Live packet/state | Partial | Unit Tested | Partial Parity | Uses `PrivateStoreItems` instead of Java `PrivateStore`. |
| `PrivateStoreService.canOpenPrivateStore` combat guard | `PrivateStoreOpenGuardPlanService` from live handler | Client feedback | Partial | Unit Tested | Partial Parity | Sends message id `1300663` from live packet dispatch. |
| `PrivateStoreService.validateItem` ordered validation | `PrivateStoreItemValidationPlanService` from live handler | Runtime validation | Partial | Indirect | Partial Parity | Success path covered; denial messages for item validation remain service-tested, not all live-handler tested. |
| `SM_EMOTION(OPEN_PRIVATESHOP)` broadcast | `SmEmotion(player, EmotionType.OpenPrivateShop, 0, 0)` | Packet/fanout | Partial | Unit Tested | Partial Parity | Direct-send fallback covered; registry broadcast used when available. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmPrivateStoreCreateSetsStoreStateAndSendsOpenEmotion` | Unit/live handler | `PrivateStoreService.createStoreWithItems` | Valid item opens store, stores ordered listing, sets private-shop state, and sends open emotion | Source-reviewed Java + live C# handler assertion | Uses packed-item allowance; template tradeability path not covered here. |
| `ProcessPacketAsync_CmPrivateStoreCreateInCombatSendsJavaDenialWithoutOpeningStore` | Unit/live handler | `PrivateStoreService.canOpenPrivateStore` | Combat guard sends `1300663` and does not open store | Source-reviewed Java + live C# handler assertion | Other guard messages remain service-tested, not all live-handler tested. |

## Known Gaps

- Java's full `PrivateStore` object is not ported; C# uses ordered `Player.PrivateStoreItems`.
- `CM_PRIVATE_STORE_NAME` still records a disabled open-name composition plan and does not mutate a live store message.
- Live `CM_BUY_ITEM` private-store purchase remains non-live; do not infer purchase parity from store-open state.
- Private-store listing state is volatile and not persisted.
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

**UOW-2595 candidate: live `CM_PRIVATE_STORE_NAME` store-message mutation and `SM_PRIVATE_STORE_NAME` fanout.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: CM_PRIVATE_STORE_NAME should set the live private-store message and broadcast SM_PRIVATE_STORE_NAME instead of only recording a disabled composition plan.
- Java source method or runtime path: PrivateStoreService.openPrivateStore -> activePlayer.getStore().setStoreMessage(name) -> broadcast SM_PRIVATE_STORE_NAME.
- C# runtime artifact to wire or fix: Player private-store message state, GameServerConnection CmPrivateStoreName branch, SmPrivateStoreName fanout.
- Client-visible/state effect expected: an open store gains the requested message and visible clients/self receive SM_PRIVATE_STORE_NAME.
- Why this is not preview-only/test-only/documentation-only if feasible: it must run from live CM_PRIVATE_STORE_NAME, mutate live store message state, and send a real packet.
```

If no live store-message field exists, add the smallest field required on the existing C# private-store runtime state and
wire it immediately from the live packet path.

## Safe Runtime Candidates

- Live `CM_PRIVATE_STORE_NAME` store-message mutation and packet fanout.
- A smaller private-store validation rejection branch only if it sends a Java-equivalent live rejection packet from `CM_PRIVATE_STORE`.
- A narrow private-store purchase side-effect UOW only after `CM_BUY_ITEM` can mutate live seller/buyer state and packet output safely.
- Another deferred `GameServerConnection` packet path with existing runtime state/repository/service surfaces.

## Context Needed By Next Session

- `CM_PRIVATE_STORE` zero-item close is live as of UOW-2593.
- `CM_PRIVATE_STORE` non-empty open is live as of UOW-2594.
- Valid open stores ordered `PrivateStoreListedItemSummary` rows in `Player.PrivateStoreItems`, sets `PrivateShop`, and sends/broadcasts `SmEmotion(OpenPrivateShop)`.
- Closing clears `PrivateStoreItems`, removes `PrivateShop`, sets `Active`, and sends/broadcasts `SmEmotion(ClosePrivateShop)`.
- `CM_PRIVATE_STORE_NAME` is the next small candidate now that live open-store state exists.
- Avoid preview/metadata/evidence-only work. Each next UOW must mutate live state, send real packets, persist/restore live state, load runtime-used Java data, or execute a live handler path.
