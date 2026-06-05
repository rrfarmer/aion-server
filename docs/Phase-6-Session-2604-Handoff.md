# Phase 6 Session 2604 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2604: Persist private-store purchase inventory mutations live. See
[Phase-6-Session-2604-Completion.md](Phase-6-Session-2604-Completion.md).

## Commits Made

- `a5b58e2` - `[Phase 6][UOW-2594] Open private store from live packet`
- `8a487a0` - `[Phase 6][UOW-2595] Set private-store name from live packet`
- `03ea474` - `[Phase 6][UOW-2596] Execute private-store purchase from live packet`
- `e7af410` - `[Phase 6][UOW-2597] Keep partial private-store purchases live`
- `f704c0b` - `[Phase 6][UOW-2598] Preserve missing private-store sale rows live`
- `51bc46c` - `[Phase 6][UOW-2599] Send private-store delete and cube packets live`
- `d15dc2f` - `[Phase 6][UOW-2600] Snapshot private-store cube updates live`
- `8b85fa6` - `[Phase 6][UOW-2601] Send private-store seller kinah add live`
- `e5ab413` - `[Phase 6][UOW-2602] Order private-store seller messages live`
- `60a17a2` - `[Phase 6][UOW-2603] Interleave private-store buyer fanout live`
- Current commit - `[Phase 6][UOW-2604] Persist private-store purchases live`

## Session Summary

- Java review confirmed `PrivateStoreService.sellStoreItem` mutates seller item count/pack count, buyer inventory, and
  buyer/seller kinah.
- Java `InventoryDAO.store` persists deleted, new, and changed inventory rows through the existing `inventory` table.
- C# live private-store purchases previously mutated in-memory state and sent success packets without calling a
  persistence surface.
- `GameServerConnection.TryExecutePrivateStorePurchaseAsync` now calls `PlayerEnterWorldService` persistence before
  applying success state and sending success packets.
- `MySqlPlayerEnterWorldRepository` now has a private-store purchase transaction that writes seller delete/update rows,
  buyer update/insert rows, and buyer/seller kinah rows.

## Files Changed In UOW-2604

- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `docs/Phase-6-Session-2604-Completion.md`
- `docs/Phase-6-Session-2604-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.PrivateStoreService#sellStoreItem`
- `com.aionemu.gameserver.dao.InventoryDAO#store`
- `com.aionemu.gameserver.dao.InventoryDAO#insertItems`
- `com.aionemu.gameserver.dao.InventoryDAO#updateItems`
- `com.aionemu.gameserver.dao.InventoryDAO#deleteItems`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.TryExecutePrivateStorePurchaseAsync`
- `Aion.GameServer.Services.PlayerEnterWorldService.SavePrivateStorePurchaseMutationAsync`
- `Aion.GameServer.Data.IPlayerEnterWorldRepository.SavePrivateStorePurchaseMutationAsync`
- `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.SavePrivateStorePurchaseMutationAsync`
- `Aion.GameServer.Data.EmptyPlayerEnterWorldRepository`
- `Aion.GameServer.Tests.GameServerConnectionBuyItemTests`
- `Aion.GameServer.Tests.PlayerEnterWorldServiceTests`

## Validation

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~PlayerEnterWorldService" --no-restore
```

Result: passed, 86/86.

Java/Maven: not run. No narrow Java fixture exists; behavior was verified by source review against Java methods listed
above.

Broad .NET: skipped after focused live handler and adjacent service/repository-contract coverage. The filtered command
built `Aion.GameServer` and `Aion.GameServer.Tests` and directly covered the modified live `ProcessPacketAsync` path.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.PrivateStoreService#sellStoreItem` | `Aion.GameServer.Network.Aion.GameServerConnection.TryExecutePrivateStorePurchaseAsync` | Live handler | Partial | Unit Tested | Partial Parity | Live private-store purchase now calls persistence before success state/packets; exchange logging and full PrivateStore behavior remain incomplete. |
| `com.aionemu.gameserver.dao.InventoryDAO#store` | `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.SavePrivateStorePurchaseMutationAsync` | Repository | Partial | Unit Tested | Partial Parity | Persists private-store purchase rows through the existing inventory table shape; not a full dirty-item store implementation. |
| `com.aionemu.gameserver.dao.InventoryDAO#updateItems` | `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.SavePrivateStoreSellerItemAsync` | Repository helper | Partial | Unit Tested | Partial Parity | Seller private-store updates persist `item_count` and `pack_count`, the fields changed by the current C# purchase plan. |
| `com.aionemu.gameserver.model.gameobjects.player.Storage` | `Aion.GameServer.Services.PlayerEnterWorldService.SavePrivateStorePurchaseMutationAsync` | Service | Partial | Unit Tested | Partial Parity | Service forwards planned buyer/seller inventory and kinah mutations to the repository; full Java Storage dirty-state lifecycle remains incomplete. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmBuyItemPlayerPrivateStoreExecutesSingleItemPurchaseAndClosesStore` | Unit/live handler | `PrivateStoreService.sellStoreItem` + `InventoryDAO.store` | Live sold-out private-store purchase calls persistence with seller delete, buyer add, buyer kinah, and created seller kinah rows while preserving packet/state assertions | Java source review + live C# handler assertion | Does not execute SQL against MySQL. |
| `PlayerEnterWorldServiceTests.CapturingEnterWorldRepository` interface implementation | Unit fixture support | `InventoryDAO.store` service boundary | Keeps adjacent player-enter-world service tests compiling against the expanded repository contract | C# focused compile/test evidence | Fixture method returns default success only. |

## Known Gaps

- No live MySQL integration test was run for UOW-2604.
- Private-store persistence is not a full `InventoryDAO.store(Player)` dirty-state lifecycle port.
- Java exchange-log/audit writes are not ported.
- Java's full `PrivateStore` object is still not ported.
- Buyer existing-stack update fanout is supported by grouped metadata and persistence payloads but lacks a focused live
  timeline branch.
- Buyer inventory-full denial is already wired through buyer messages but still lacks live branch coverage.
- Insufficient-kinah denial, stale seller item counts, missing item templates, and mixed present/missing seller item
  behavior still need live branch coverage or fixes where discovery finds a runtime mismatch.
- Static-door `onOpenDoor` instance callback remains unported because no concrete live C# instance-handler execution
  surface was found.
- Full `CM_GROUP_LOOT` roll/bid remains blocked by missing drop-distribution runtime state.
- Real client validation was not run.

## Candidate Checks Rejected This Session

### Persistence As Preview Plan Only

- The handoff candidate required real persistence. A preview/persistence-intent-only unit would not pass the Runtime
  Progress Gate.
- The selected implementation wires the live handler through `PlayerEnterWorldService` and the existing MySQL repository
  surface instead.

## Next Recommended Runtime UOW

**UOW-2605 candidate: wire private-store sale exchange/audit logging from live `CM_BUY_ITEM` if discovery finds an
existing C# logging or database surface that can record Java-equivalent sale records without a schema change.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: private-store sale audit/exchange side effects should be emitted from the live purchase path.
- Java source method or runtime path: PrivateStoreService.sellStoreItem log.info("[PRIVATE STORE] ...") and AuditLogger branches for invalid/private-store abuse cases.
- C# runtime artifact to wire or fix: GameServerConnection.TryExecutePrivateStorePurchaseAsync plus any existing exchange/audit/log repository or runtime logger surface discovered in dotnetConversion.
- Client-visible/state/persistence effect expected: live private-store sale or abuse attempts produce the Java-equivalent operational log/audit side effect.
- Why this is not preview-only/test-only/documentation-only if feasible: it emits a real runtime log or persistence side effect from live CM_BUY_ITEM.
```

Suggested focused validation if this UOW includes a runtime logging fix:

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~PrivateStore" --no-restore
```

Java/Maven: not expected unless a narrow Java fixture is discovered. Broad-validation trigger: none unless a shared
logging/persistence primitive is changed; start focused and document any broad skip.

## Safe Runtime Candidates

- Private-store sale or abuse audit/logging side effects from live `CM_BUY_ITEM`, only if an existing runtime logging
  surface exists.
- Private-store MySQL integration validation only if paired with a runtime SQL fix discovered in the repository path.
- Mixed present/missing private-store purchase branch if discovery finds a live state, packet, or persistence mismatch
  beyond UOW-2598 and UOW-2604.
- Private-store insufficient-kinah or stale seller-count denials only if they require a runtime send/state/persistence
  fix.
- Another deferred `GameServerConnection` packet path with existing runtime state/repository/service surfaces.
- Java XML/static-data loading only when the data is immediately used by live code.

## Context Needed By Next Session

- `CM_PRIVATE_STORE` zero-item close is live as of UOW-2593.
- `CM_PRIVATE_STORE` non-empty open is live as of UOW-2594.
- `CM_PRIVATE_STORE_NAME` is live as of UOW-2595.
- `CM_BUY_ITEM` player action `0` sold-out single-item purchase is live as of UOW-2596.
- `CM_BUY_ITEM` player action `0` partial-stack/non-closing purchase is live as of UOW-2597, including seller pack-count decrement.
- `CM_BUY_ITEM` player action `0` missing seller inventory item branch is live as of UOW-2598, including Java's kinah-transfer behavior.
- `CM_BUY_ITEM` private-store packet fanout sends Java delete type and cube updates as of UOW-2599 for covered sold-out/new-item branches.
- `CM_BUY_ITEM` multi-row private-store purchase cube counts use per-item snapshots as of UOW-2600.
- `CM_BUY_ITEM` seller-without-kinah fanout sends Java's zero-count kinah add/cube before kinah update as of UOW-2601.
- `CM_BUY_ITEM` seller sale messages precede seller kinah packets as of UOW-2602.
- `CM_BUY_ITEM` buyer item add/update packets precede the corresponding seller sale message as of UOW-2603.
- `CM_BUY_ITEM` private-store purchase inventory/kinah mutations call the existing player inventory persistence surface as of UOW-2604.
- Existing disabled private-store planner services are only parity evidence when applied by a live handler.
- Avoid preview/metadata/evidence-only work. Each next UOW must mutate live state, send real packets, persist/restore
  live state, load runtime-used Java data, or execute a live handler path.
