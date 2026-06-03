# Phase 6 Session 2563 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2563: Port exchange item trade execution (full-stack) with atomic ownership transfer

## Session Summary (UOWs 2561–2563)

| UOW | Summary |
|-----|---------|
| 2561 | Live CM_PLAYER_SEARCH + SmPlayerSearch (211) + pure PlayerSearchMatchService; 17 tests |
| 2562 | Set FriendListStatus = ONLINE(1) on enter-world (parity fix; de-risks player search + friend lists) |
| 2563 | Exchange item trade execution (full-stack) + TransferItemOwnershipAsync repo method; trading system now complete |

## MILESTONE: Player-to-player trading is fully live

The complete exchange flow now works end-to-end:

| Step | Handler | Status |
|------|---------|--------|
| Request | HandleExchangeRequestAsync | Live |
| Add kinah | HandleExchangeAddKinahAsync (Player.ExchangeKinah) | Live |
| Add item | HandleExchangeAddItemAsync (Player.ExchangeItems) | Live |
| Lock | HandleExchangeLockAsync | Live |
| Confirm | HandleExchangeOkAsync | Live |
| Execute trade | ExecuteExchangeTradeAsync | Live (full-stack items + kinah) |
| Cancel | HandleExchangeCancelAsync (+ item UI restore) | Live |

### Trade execution (ExecuteExchangeTradeAsync) — Java ExchangeService.performTrade parity
1. Resolve both players' committed items from `Player.ExchangeItems`
2. **Partial-stack guard**: if any committed count < stack count, abort cleanly (cancel both, restore UI) — deferred pending IdFactory split
3. **Free-slot validation**: `InventoryCapacity.GetFreeCubeSlots` >= incoming item count; on failure send STR_EXCHANGE_CANT_EXCHANGE_HEAVY (1300359) / STR_PARTNER_TOO_HEAVY (1300357) and clean up
4. Kinah non-negative validation
5. Send SM_EXCHANGE_CONFIRMATION(Success=0) to both
6. Transfer items both directions; exchange kinah
7. Clean up exchange state (clears baskets)

### Persistence-safety design (the key finding from Session 2561, now resolved)
- New repo method `TransferItemOwnershipAsync(itemObjectId, previousOwnerId, newOwnerId, newLocation, newSlot)`:
  `UPDATE inventory SET item_owner = ?, item_location = ?, slot = ?, is_equipped = 0 WHERE item_unique_id = ? AND item_owner = ?` — atomic, executed immediately at trade time.
- Giver: item removed from in-memory `InventoryItems` **without** `TrackDeletedItem` → logout will not double-delete the transferred row.
- Receiver: `Updated`-state copy added → the logout snapshot (`SaveInventoryItemFullStateAsync`, a pure UPDATE-by-`item_unique_id`, never INSERT) is idempotent.
- This is safe because the C# port INSERTs new item rows immediately at creation, and the logout snapshot only UPDATEs existing rows.

### Intentional deferral
- Partial-stack item trades (e.g., giving 20 of a 50 stack) abort cleanly instead of splitting; full-stack item trades + kinah are live. Documented divergence pending IdFactory split + INSERT support.
- Kinah persistence remains logout/periodic-save based (the kinah row is mutated in memory; not immediately persisted) — unchanged from UOW-2554.

## Validation Decisions

| UOW | Command | Result | Trigger |
|-----|---------|--------|---------|
| 2561 | `--filter "PlayerSearchMatchServiceTests"` | 17/17 | none |
| 2562 | `--filter "PlayerEnterWorldServiceTests\|PlayerSearchMatchServiceTests"` | 74/74 | none |
| 2563 | `--filter "GameServerConnectionExchangeTradeTests"` | 2/2 | none |
| 2563 | `--filter "ExchangeAddKinahPlanService\|PlayerExchangeRequestService\|GameServerConnectionExchangeTradeTests\|PlayerEnterWorldServiceTests"` | 84/84 | none |
| 2563 | `dotnet build Aion.GameServer.csproj` | 0 errors | compile signal |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `ExchangeService.performTrade` | `ExecuteExchangeTradeAsync` | Service | Complete | Unit Tested | Partial Parity | Partial-stack split deferred |
| `ExchangeService.removeItemsFromInventory` + `putItemToInventory` | `TransferTradeItemsAsync` | Service | Complete | Unit Tested | Partial Parity | Full-stack only |
| `InventoryDAO.store` (ownership) | `TransferItemOwnershipAsync` | Repository | Complete | Manual Only | Needs Verification | SQL not DB-integration-tested yet |
| `ItemPacketService.ItemAddType.PLAYER_EXCHANGE_GET` | `SmInventoryAddItem.PlayerExchangeGet` | Const | Complete | Unit Tested | Verified Parity | 0x21 |
| `ExchangeService.validateInventorySize` | free-slot check in ExecuteExchangeTradeAsync | Service | Complete | Manual Only | Partial Parity | uses GetFreeCubeSlots |

## Next Recommended UOW

**UOW-2564: Partial-stack exchange trade splits**

Complete the deferred path. KEY FINDING (verified this session): the existing repo method
`SaveItemSplitMutationAsync(playerObjectId, sourceItem, newItem)` is **directly reusable for a
cross-player split** — it UPDATEs the source row count `WHERE item_owner = playerObjectId` (pass the
giver) and INSERTs `newItem` using the new item's own `OwnerId` field (set it to the receiver). One
transaction, no new repo method needed.

Steps:
1. Add an optional `int? objectId = null` parameter to `CopyInventoryItem` (default keeps ObjectId) so the
   receiver's split item can get a fresh id. (Low risk: optional param, default preserves all call sites.)
2. In `TransferTradeItemsAsync`, branch:
   - Full-stack (committed == item.Count): current path (TransferItemOwnershipAsync).
   - Partial-stack (committed < item.Count): 
     - `reducedSource = CopyInventoryItem(item, count: item.Count - committed)`; `ReplaceInventoryItemFor(giver, reducedSource)` (no giver packet — the add-item step already showed the reduced count via PutToExchange).
     - `newId = _idFactory.NextId()`; `received = CopyInventoryItem(item, objectId: newId, count: committed, ownerId: receiver, location: cube, slot: FirstAvailableSlot, isEquipped: false, packCount: unwrapped)`.
     - Persist via `_playerEnterWorldService.SaveItemSplitMutationAsync(giver, reducedSource, received)`.
     - Add `received` to receiver in memory; send `SmInventoryAddItem.CreatePlayerExchangeGet`.
3. Remove the partial-stack abort guard in `ExecuteExchangeTradeAsync`. If `_idFactory == null`, keep the abort fallback (cannot allocate ids).
4. Test: extend `GameServerConnectionExchangeTradeTests` harness to pass an `IDFactory` and a fake repo capturing the split; assert giver count reduced + receiver gets a new-id item with committed count.

Alternatives:
1. **DB integration test for TransferItemOwnershipAsync** — opt-in test against the Dockerized MySQL to raise its parity status to Verified (currently Needs Verification).
2. **CM_LEGION_HISTORY stub** — empty SM_LEGION_HISTORY so legion-history UI doesn't hang.
3. **Faction-prefixed names in player search** — port ChatUtil.toFactionPrefixedName.
4. **CM_QUEST_SHARE no-members message** — STR 1100000/1100005.

Focused validation recipe for UOW-2564:
- Behavior: partial-stack trade splits a stack; giver keeps remainder, receiver gets a new row with committed count
- Focused C# command: `dotnet test --filter "FullyQualifiedName~GameServerConnectionExchangeTradeTests"`
- Java/Maven: not expected
- Broad-validation trigger: none

## Remaining Risks

- `TransferItemOwnershipAsync` SQL is unit-covered only at the in-memory level; not yet DB-integration-tested (Needs Verification)
- Partial-stack item trades abort instead of splitting (deferred)
- Kinah trade not immediately persisted (logout/periodic save)
- Trade item transfer is best-effort on persistence failure (logs warning, proceeds in memory) — a DB failure mid-trade could diverge memory from DB
- Faction-prefixed names not applied in player search
- All legion handlers deferred; CM_GATHER deferred; XP/level-up not ported
- CM_LEVEL_READY: SM_RIFT_ANNOUNCE, SM_CONQUEROR_PROTECTOR deferred

## Context Needed By Next Session

- Exchange trading complete (full-stack); `Player.ExchangeKinah` + `Player.ExchangeItems` baskets cleared on accept/cancel/complete
- `TransferItemOwnershipAsync` is the atomic ownership-transfer repo method (giver→receiver), keyed by item_unique_id + previous owner
- Persistence model: logout snapshot UPDATEs by item_unique_id only; new rows INSERTed immediately at creation — never rely on logout to INSERT
- `SmInventoryAddItem.PlayerExchangeGet = 0x21`; `SmInventoryUpdateItem.PlayerExchangeGet/GetBack/PutToExchange`; `SmDeleteItem.PutToExchangeDeleteType = 0x26`
- All 185 Java client opcodes registered (Session 2560); player search live (Session 2561) and returns results due to FriendListStatus fix (Session 2562)
