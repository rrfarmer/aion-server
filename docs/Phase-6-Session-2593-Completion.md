# Phase 6 Session 2593 Completion

## UOW

[Phase 6] UOW-2593: Close private store from live packet

## Status

Completed and validated with focused live packet coverage. The C# `CM_PRIVATE_STORE` zero-item branch now follows the
Java close-store path: it clears the active private-store snapshot, exits `PRIVATE_SHOP`, restores `ACTIVE`, and sends
the Java close-private-shop emotion from live packet dispatch.

## Runtime Progress Gate

```text
Runtime progress gate:
- Deferred/live behavior being advanced: zero-item CM_PRIVATE_STORE now closes an active private store instead of recording only a disabled create/close plan.
- Java source method or runtime path: CM_PRIVATE_STORE.runImpl -> PrivateStoreService.closePrivateStore.
- C# runtime artifact wired or fixed: GameServerConnection CmPrivateStore branch, HandleClosePrivateStoreAsync, Player.PrivateStoreItems, PlayerCreatureState.PrivateShop/Active, SmEmotion ClosePrivateShop.
- Client-visible/state effect changed: active private-store item state is cleared, PRIVATE_SHOP state is removed, ACTIVE state is set, and SM_EMOTION(CLOSE_PRIVATESHOP) is sent or broadcast.
- Why this is not preview-only/test-only/documentation-only: it runs from the live client packet handler, mutates live player state, and emits a real server packet.
```

## Java Source Reviewed

- `CM_PRIVATE_STORE.runImpl` calls `PrivateStoreService.closePrivateStore(player)` when no items are listed.
- `PrivateStoreService.closePrivateStore` returns when `player.getStore() == null`.
- The Java close branch sets the store to `null`, unsets `CreatureState.PRIVATE_SHOP`, sets `CreatureState.ACTIVE`, and broadcasts `SM_EMOTION(CLOSE_PRIVATESHOP, true)`.
- The Java non-empty branch calls `PrivateStoreService.createStoreWithItems`, which remains deferred because its validation and item-listing state are broader than this close UOW.

## C# Changes

- Updated the live `CmPrivateStore` handler so `Items.Count == 0` calls `HandleClosePrivateStoreAsync`.
- Added `HandleClosePrivateStoreAsync` to clear `Player.PrivateStoreItems`, remove `PrivateShop`, set `Active`, and send/broadcast `SmEmotion(... ClosePrivateShop ...)`.
- Kept non-empty private-store creation on the existing disabled create-plan observer until Java validation and open-store state can be ported.
- Updated `GameServerConnectionPrivateStoreTests` so the zero-item packet asserts live mutation and serialized close-emotion output.

## Known Gaps

- Non-empty `CM_PRIVATE_STORE` creation is still not live; Java item validation, open-store state, listed-item sale state, and open-store emotion remain deferred.
- `CM_PRIVATE_STORE_NAME` still records a disabled open-name composition plan; live store-message mutation and fanout are not ported.
- The C# private-store model uses `PrivateStoreItems` plus player state as the current runtime representation; Java's full `Store` object is not ported.
- Real client validation was not run.

## Validation Decision

```text
Validation decision:
- Changed surface: live CM_PRIVATE_STORE zero-item packet handling.
- Specific behavior/contract: closing an active private store clears live listed items, exits PRIVATE_SHOP, restores ACTIVE, and sends SM_EMOTION(CLOSE_PRIVATESHOP).
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionPrivateStoreTests|FullyQualifiedName~CmPrivateStoreTests" --no-restore -> 7/7 passed.
- Focused Java/Maven command: none; no narrow Java fixture exists, and behavior was taken from reviewed Java source.
- Broad-validation trigger: live packet output and player state changed.
- Broad .NET decision: skipped after focused packet/state/parser coverage; the filtered test run built Aion.GameServer and drove the real ProcessPacketAsync CM_PRIVATE_STORE path.
- Why this scope is sufficient: the passing test dispatches a real encoded CM_PRIVATE_STORE packet and asserts the Java-equivalent live state and packet effects.
```

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_PRIVATE_STORE.runImpl` zero-item branch | `GameServerConnection` `CmPrivateStore` case | Live packet dispatch | Partial | Unit Tested | Partial Parity | Zero-item close branch is live; non-empty create branch remains deferred. |
| `PrivateStoreService.closePrivateStore` store clear | `Player.PrivateStoreItems = Array.Empty<...>()` | Player state | Partial | Unit Tested | Partial Parity | Uses current C# store snapshot representation, not a full Java `Store` object. |
| `unsetState(PRIVATE_SHOP)` / `setState(ACTIVE)` | `Player.SetCreatureState` calls | Player state | Partial | Unit Tested | Partial Parity | Test confirms private-shop exit and active state. |
| `SM_EMOTION(CLOSE_PRIVATESHOP)` broadcast | `SmEmotion(player, EmotionType.ClosePrivateShop, 0, 0)` | Packet/fanout | Partial | Unit Tested | Partial Parity | Fallback direct send is covered; registry broadcast is used when available. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmPrivateStoreCloseClearsStoreStateAndSendsCloseEmotion` | Unit/live handler | `PrivateStoreService.closePrivateStore` | Zero-item packet clears store items, exits private-shop state, sets active state, and sends close emotion | Source-reviewed Java + live C# handler assertion | Does not cover registry fanout with multiple visible players. |

## Summary Metrics

- Focused UOW validation: 7 tests passed.
- Runtime progress: zero-item private-store close now mutates live player state and emits a real close-shop emotion.
- Total Java artifacts touched/discovered this UOW: 2.
- Total C# artifacts touched: 3.
- Artifacts with verified parity: 0.
- Artifacts needing verification / partial parity: 4.
- Blocked artifacts: private-store create/open-name live paths, static-door instance callbacks, full group loot distribution.
- Estimated overall Phase 6 completion: incremental; still far from replacement readiness.

## Remaining Risks

- Full Java private-store sale semantics depend on a richer `Store` model that C# still lacks.
- Visibility/fanout fidelity depends on the current connection registry approximation when available.
- Existing disabled plan services remain in the tree and should not be treated as live parity evidence.

## Next Runtime Candidate

UOW-2594 candidate: live non-empty `CM_PRIVATE_STORE` create/open branch only if scoped to a real Java-equivalent
state mutation and packet effect.

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: CM_PRIVATE_STORE with listed items should create a live private-store listing instead of only producing a disabled create plan.
- Java source method or runtime path: PrivateStoreService.createStoreWithItems -> canOpenPrivateStore -> validateItem -> player.setStore(...) -> SM_EMOTION(OPEN_PRIVATESHOP).
- C# runtime artifact to wire or fix: GameServerConnection CmPrivateStore branch, Player.PrivateStoreItems/store-message state, inventory item validation/template checks, SmEmotion OpenPrivateShop fanout, relevant rejection messages.
- Client-visible/state effect expected: listed items become live seller state, PRIVATE_SHOP is entered, and visible clients receive the open-private-shop emotion; invalid items/states are rejected with Java-equivalent feedback where available.
- Why this is not preview-only/test-only/documentation-only if feasible: it must run from live CM_PRIVATE_STORE and mutate seller/player state plus send real packets.
```

If the create branch proves too broad for one safe UOW, re-plan from another deferred live packet/state/persistence path
instead of adding more private-store planners.
