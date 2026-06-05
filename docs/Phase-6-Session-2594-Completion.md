# Phase 6 Session 2594 Completion

## UOW

[Phase 6] UOW-2594: Open private store from live packet

## Status

Completed and validated with focused live packet coverage. The C# `CM_PRIVATE_STORE` non-empty branch now performs
Java-order open guards and item validation, stores the listed items in live seller state, enters `PRIVATE_SHOP`, and
sends the Java open-private-shop emotion.

## Runtime Progress Gate

```text
Runtime progress gate:
- Deferred/live behavior being advanced: non-empty CM_PRIVATE_STORE now opens a live private-store listing instead of only recording a disabled create plan.
- Java source method or runtime path: CM_PRIVATE_STORE.runImpl -> PrivateStoreService.createStoreWithItems -> canOpenPrivateStore -> validateItem -> setStore/state/broadcast.
- C# runtime artifact wired or fixed: GameServerConnection CmPrivateStore branch, HandleCreatePrivateStoreAsync, Player.PrivateStoreItems, PlayerCreatureState.PrivateShop, PrivateStoreOpenGuardPlanService, PrivateStoreItemValidationPlanService, SmEmotion OpenPrivateShop.
- Client-visible/state effect changed: valid listed items become live seller store state, PRIVATE_SHOP is set, visible clients/self receive SM_EMOTION(OPEN_PRIVATESHOP), and Java-equivalent denial messages are sent for guarded failures.
- Why this is not preview-only/test-only/documentation-only: it runs from live client packet dispatch, mutates live player/store state, and emits real server packets.
```

## Java Source Reviewed

- `CM_PRIVATE_STORE.runImpl` calls `PrivateStoreService.createStoreWithItems` when item count is positive.
- `PrivateStoreService.canOpenPrivateStore` rejects flying, moving, combat, trading, ride/robot, hidden, dead, chair, and already-open states in order.
- `PrivateStoreService.validateItem` rejects missing/mismatched items, invalid count, negative price, full basket, non-tradeable items, equipped items, and duplicate registrations in order.
- On success, Java creates a `PrivateStore`, adds each item to sell, sets player store, sets `CreatureState.PRIVATE_SHOP`, and broadcasts `SM_EMOTION(OPEN_PRIVATESHOP, true)`.

## C# Changes

- Updated non-empty `CmPrivateStore` dispatch to call `HandleCreatePrivateStoreAsync`.
- Added live open guard and item-validation execution using the existing Java-shaped services for guard order and denial messages.
- Added successful state mutation: `Player.PrivateStoreItems` now receives ordered listed items, and `PrivateShop` state is set.
- Added live `SmEmotion(OpenPrivateShop)` send/broadcast after state mutation.
- Added live handler tests for a valid create packet and the Java combat-mode denial message.

## Known Gaps

- C# still lacks Java's full `PrivateStore` object; this UOW uses `Player.PrivateStoreItems`, the existing C# private-store seller state used by `CM_BUY_ITEM` planning.
- Store-message mutation and `SM_PRIVATE_STORE_NAME` fanout remain deferred in `CM_PRIVATE_STORE_NAME`.
- Validation uses loaded item templates when available; packed items can be listed without template tradeability data, matching Java's `packCount > 0` allowance.
- Store opening is in-memory only; no persistence model for private-store listings is present.
- Real client validation was not run.

## Validation Decision

```text
Validation decision:
- Changed surface: live CM_PRIVATE_STORE non-empty packet handling.
- Specific behavior/contract: valid listed items become live private-store seller state, PRIVATE_SHOP is set, OPEN_PRIVATESHOP is sent; combat mode sends message id 1300663 without opening.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionPrivateStoreTests|FullyQualifiedName~CmPrivateStoreTests" --no-restore -> 9/9 passed.
- Focused Java/Maven command: none; no narrow Java fixture exists, and behavior was taken from reviewed Java source.
- Broad-validation trigger: live player state and packet output changed.
- Broad .NET decision: skipped after focused live packet/state/parser coverage; the filter built Aion.GameServer and drove the modified ProcessPacketAsync path.
- Why this scope is sufficient: the passing tests use encoded CM_PRIVATE_STORE packets and assert both live success effects and a Java guard-denial packet.
```

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

## Summary Metrics

- Focused UOW validation: 9 tests passed.
- Runtime progress: non-empty private-store create now mutates live seller state and emits a real open-shop emotion.
- Total Java artifacts touched/discovered this UOW: 4.
- Total C# artifacts touched: 4.
- Artifacts with verified parity: 0.
- Artifacts needing verification / partial parity: 4.
- Blocked artifacts: full Java `PrivateStore` object model, store-name live mutation, full private-store purchase side effects, full group loot distribution.
- Estimated overall Phase 6 completion: incremental; still far from replacement readiness.

## Remaining Risks

- Live `CM_BUY_ITEM` private-store purchase side effects are still non-live and must not be inferred from store-open parity.
- C# private-store state is currently volatile and not persisted.
- Full item-validation coverage across template tradeability, duplicates, equipped items, full basket, and negative price should be expanded as those paths are promoted from service evidence to live handler coverage.

## Next Runtime Candidate

UOW-2595 candidate: live `CM_PRIVATE_STORE_NAME` store-message mutation and `SM_PRIVATE_STORE_NAME` fanout.

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
