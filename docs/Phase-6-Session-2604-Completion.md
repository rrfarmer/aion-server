# Phase 6 Session 2604 Completion

## UOW

[Phase 6] UOW-2604: Persist private-store purchase inventory mutations live

## Status

Completed and validated with focused live handler and adjacent service coverage. Live `CM_BUY_ITEM` private-store
purchases now call the existing player inventory persistence surface before applying success state and sending success
packets.

## Runtime Progress Gate

```text
Runtime progress gate:
- Deferred/live behavior being advanced: live private-store purchase inventory, kinah, and store mutations are no longer only memory-side when PlayerEnterWorldService is present.
- Java source method or runtime path: PrivateStoreService.sellStoreItem mutates seller inventory, buyer inventory, and kinah; InventoryDAO.store persists changed/new/deleted item rows.
- C# runtime artifact wired or fixed: GameServerConnection.TryExecutePrivateStorePurchaseAsync now calls PlayerEnterWorldService.SavePrivateStorePurchaseMutationAsync; IPlayerEnterWorldRepository/MySqlPlayerEnterWorldRepository persist the planned inventory rows.
- Client-visible/state/persistence effect changed: live success packets and in-memory success state are emitted only after the existing persistence surface accepts seller item deletes/updates, buyer item adds/updates, and buyer/seller kinah rows.
- Why this is not preview-only/test-only/documentation-only: this wires live handler mutations into the existing database persistence path.
```

## Java Source Reviewed

- `PrivateStoreService.sellStoreItem` decreases seller item count, decrements pack count for packed items, adds the
  bought item to the buyer, then decreases/increases buyer/seller kinah.
- `InventoryDAO.store(Player)` gathers dirty item rows from the player.
- `InventoryDAO.store(List<Item>, ...)` separates deleted, new, and changed items and writes them through
  `deleteItems`, `insertItems`, and `updateItems` inside the database connection path.
- Java `InventoryDAO.UPDATE_QUERY` updates full item state, including `item_count` and `pack_count`.

## C# Changes

- Added `IPlayerEnterWorldRepository.SavePrivateStorePurchaseMutationAsync`.
- Added `PlayerEnterWorldService.SavePrivateStorePurchaseMutationAsync` as the live service wrapper used by the
  connection handler.
- Added `MySqlPlayerEnterWorldRepository.SavePrivateStorePurchaseMutationAsync`, which persists seller changed/deleted
  items, buyer changed/new items, buyer kinah, and seller kinah in one transaction using the existing `inventory` table
  shape.
- Added seller private-store item persistence for `item_count` and `pack_count`.
- Changed `GameServerConnection.TryExecutePrivateStorePurchaseAsync` to persist the purchase plan before applying
  in-memory success state and success packet fanout.
- Extended the live private-store buy test fixture so it can host a real `PlayerEnterWorldService` with a capturing
  repository.

## Known Gaps

- No live MySQL integration test was run for this UOW; the focused test proves the live handler calls the persistence
  surface with Java-derived rows, not that a real database accepted the SQL.
- The private-store persistence method writes only the item fields mutated by this C# purchase path: count, seller
  pack count, inserts, deletes, and kinah count. It is not a full `InventoryDAO.store(Player)` port.
- Java exchange-log/audit writes remain unported.
- Full Java `PrivateStore` object behavior remains unported.
- If `PlayerEnterWorldService` is absent, the test/null-hosted connection path still behaves memory-only; the normal
  game socket server passes the registered service into live connections.
- Real client validation was not run.

## Validation Decision

```text
Validation decision:
- Changed surface: live CM_BUY_ITEM private-store persistence plus IPlayerEnterWorldRepository service/repository contract.
- Specific behavior/contract: a live sold-out private-store purchase calls persistence once with buyer/seller IDs, seller delete rows, buyer add rows, buyer kinah decrease, and seller kinah creation before success packets/state.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~PlayerEnterWorldService" --no-restore -> 86/86 passed.
- Focused Java/Maven command: none; no narrow Java fixture exists, and behavior was taken from reviewed Java source.
- Broad-validation trigger: live persistence wiring and repository contract changed.
- Broad .NET decision: skipped after focused live handler and adjacent service-contract coverage; the filtered command built Aion.GameServer and Aion.GameServer.Tests and directly covered the modified dispatch path.
- Why this scope is sufficient: the passing live handler test dispatches encoded CM_BUY_ITEM and asserts the repository payload used for the new persistence call while preserving the existing packet timeline assertions.
```

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.PrivateStoreService#sellStoreItem` | `Aion.GameServer.Network.Aion.GameServerConnection.TryExecutePrivateStorePurchaseAsync` | Live handler | Partial | Unit Tested | Partial Parity | Live private-store purchase now calls persistence before success state/packets; exchange logging and full PrivateStore behavior remain incomplete. |
| `com.aionemu.gameserver.dao.InventoryDAO#store` | `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.SavePrivateStorePurchaseMutationAsync` | Repository | Partial | Unit Tested | Partial Parity | Persists the private-store purchase rows through the existing inventory table shape; not a full dirty-item store implementation. |
| `com.aionemu.gameserver.dao.InventoryDAO#updateItems` | `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.SavePrivateStoreSellerItemAsync` | Repository helper | Partial | Unit Tested | Partial Parity | Seller private-store updates persist `item_count` and `pack_count`, the fields changed by the current C# purchase plan. |
| `com.aionemu.gameserver.model.gameobjects.player.Storage` | `Aion.GameServer.Services.PlayerEnterWorldService.SavePrivateStorePurchaseMutationAsync` | Service | Partial | Unit Tested | Partial Parity | Service forwards planned buyer/seller inventory and kinah mutations to the repository; full Java Storage dirty-state lifecycle remains incomplete. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmBuyItemPlayerPrivateStoreExecutesSingleItemPurchaseAndClosesStore` | Unit/live handler | `PrivateStoreService.sellStoreItem` + `InventoryDAO.store` | Live sold-out private-store purchase calls persistence with seller delete, buyer add, buyer kinah, and created seller kinah rows while preserving packet/state assertions | Java source review + live C# handler assertion | Does not execute SQL against MySQL. |
| `PlayerEnterWorldServiceTests.CapturingEnterWorldRepository` interface implementation | Unit fixture support | `InventoryDAO.store` service boundary | Keeps adjacent player-enter-world service tests compiling against the expanded repository contract | C# focused compile/test evidence | Fixture method returns default success only. |

## Summary Metrics

- Focused UOW validation: 86 tests passed.
- Runtime progress: live private-store purchases now use the existing player inventory persistence path.
- Total Java artifacts touched/discovered this UOW: 4.
- Total C# artifacts touched: 5.
- Artifacts with verified parity: 0.
- Artifacts needing verification / partial parity: 4.
- Blocked artifacts: exchange logging, full dirty-item inventory store semantics, full Java `PrivateStore` object, real MySQL/client verification.
- Estimated overall Phase 6 completion: incremental; still far from replacement readiness.

## Remaining Risks

- SQL was compile-validated but not integration-tested against a live MySQL schema in this UOW.
- The repository call is synchronous in the packet handler's async flow; no Java-equivalent locking/transaction boundary
  beyond the DB transaction was added.
- Java audit/exchange logs still do not fire for private-store sales.

## Next Runtime Candidate

UOW-2605 candidate: wire private-store sale exchange/audit logging from live `CM_BUY_ITEM` if discovery finds an
existing C# logging or database surface that can record Java-equivalent sale records without a schema change.

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: private-store sale audit/exchange side effects should be emitted from the live purchase path.
- Java source method or runtime path: PrivateStoreService.sellStoreItem log.info("[PRIVATE STORE] ...") and AuditLogger branches for invalid/private-store abuse cases.
- C# runtime artifact to wire or fix: GameServerConnection.TryExecutePrivateStorePurchaseAsync plus any existing exchange/audit/log repository or runtime logger surface discovered in dotnetConversion.
- Client-visible/state/persistence effect expected: live private-store sale or abuse attempts produce the Java-equivalent operational log/audit side effect.
- Why this is not preview-only/test-only/documentation-only if feasible: it emits a real runtime log or persistence side effect from live CM_BUY_ITEM.
```
