# Phase 6 Session 2562 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2562: Set FriendListStatus to ONLINE on enter-world (parity fix)

## Session Summary (UOWs 2561–2562)

| UOW | Summary |
|-----|---------|
| 2561 | Live CM_PLAYER_SEARCH handler + SmPlayerSearch (opcode 211) + pure PlayerSearchMatchService; 17 tests |
| 2562 | Set Player.FriendListStatus = ONLINE(1) on enter-world (Java parity); de-risks player search + friend lists |

## UOW-2562 Detail

Java `PlayerEnterWorldService.enterWorld` calls `player.getFriendList().setStatus(Status.ONLINE)`. The C# port set `IsOnline = true` but left `FriendListStatus` at the default `0`, which equals `Status.OFFLINE` in the Java enum (`OFFLINE=0, ONLINE=1, AWAY=3`). Effect of the bug:
- Friend lists showed every online player as offline
- CM_PLAYER_SEARCH (UOW-2561) excluded all non-staff candidates (`FriendStatusOffline` was true for everyone)

Fix: `player.FriendListStatus = 1` in `PlayerEnterWorldService.EnterWorldAsync` right after `IsOnline = true`. Added a test assertion in `PlayerEnterWorldServiceTests`.

## Validation Decisions

| UOW | Command | Result | Trigger |
|-----|---------|--------|---------|
| 2561 | `--filter "PlayerSearchMatchServiceTests"` | 17/17 | none |
| 2562 | `--filter "PlayerEnterWorldServiceTests|PlayerSearchMatchServiceTests"` | 74/74 | none |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_PLAYER_SEARCH` (159) | `HandlePlayerSearchAsync` | Handler | Complete | Unit Tested | Partial Parity | Faction-prefix name deferred |
| `SM_PLAYER_SEARCH` (211) | `SmPlayerSearch` | ServerPacket | Complete | Unit Tested | Verified Parity | Byte layout matches |
| `CM_PLAYER_SEARCH.runImpl` filter | `PlayerSearchMatchService` | Service | Complete | Unit Tested | Verified Parity | All branches tested |
| `PlayerEnterWorldService.enterWorld` setStatus(ONLINE) | `PlayerEnterWorldService.EnterWorldAsync` | Service | Complete | Unit Tested | Verified Parity | FriendListStatus=1 |

## CARRY-FORWARD: Exchange item trade execution needs a persistence design

(Repeated from Session 2561 handoff — still the highest-value blocked item.)

Cross-player item transfer cannot use the current delete/insert snapshot persistence (keyed by `item_unique_id`/ObjectId), because the giver's logout-time dirty-delete would delete the receiver's freshly-inserted row (item loss / duplicate-key). The kinah-only trade path (UOW-2554) is safe because it mutates each player's own pre-existing kinah row count.

**Required:** an atomic immediate ownership-transfer repo method:
`UPDATE inventory SET item_owner = ?, slot = ?, item_location = ? WHERE item_unique_id = ?`
plus removing the item from the giver's dirty-delete set and adding to the receiver without `New` state. Partial-stack trades additionally need IdFactory allocation + INSERT for the split-off portion.

## Next Recommended UOW

**UOW-2563: Exchange item trade execution (full-stack, with TransferItemOwnershipAsync)**

1. Add `IPlayerEnterWorldRepository.TransferItemOwnershipAsync(itemObjectId, newOwnerId, newLocation, newSlot)` + MySql impl (atomic UPDATE) + Empty/Fake impls
2. Rename `ExecuteKinahOnlyExchangeAsync` → `ExecuteExchangeTradeAsync`
3. Validate free cube slots both sides via `InventoryCapacity.GetFreeCubeSlots`
4. Full-stack items only: move InventoryItem between players' `InventoryItems` (new OwnerId/slot), call transfer repo method, send SmInventoryAddItem(PlayerExchangeGet) to receiver + SmDeleteItem to giver
5. Defer partial-stack split (document); clear ExchangeItems both sides

Simpler alternatives:
1. **CM_LEGION_HISTORY stub** — empty SM_LEGION_HISTORY so legion UI doesn't hang
2. **CM_QUEST_SHARE no-members message** — STR 1100000 (group) / 1100005 (alliance)
3. **Faction-prefixed names in player search** — port ChatUtil.toFactionPrefixedName

Focused validation recipe for UOW-2563:
- Behavior: full-stack items move between players' inventories on double-confirm; ownership persisted
- Focused C# command: `dotnet test --filter "FullyQualifiedName~PlayerExchangeRequestService|FullyQualifiedName~PlayerEnterWorldServiceTests"`
- Java/Maven: not expected
- Broad-validation trigger: none

## Remaining Risks

- Exchange item trade execution deferred (persistence design required)
- Faction-prefixed names not applied in player search results
- Exchange: no immediate kinah persistence (saved at logout/periodic save)
- CM_REPLACE_ITEM cross-storage deferred
- All legion handlers deferred (LegionService not ported)
- CM_GATHER handler deferred; XP/level-up not ported
- CM_LEVEL_READY: SM_RIFT_ANNOUNCE, SM_CONQUEROR_PROTECTOR deferred

## Context Needed By Next Session

- All 185 Java client opcodes registered in C# (Session 2560 milestone)
- Player search live; relies on FriendListStatus=ONLINE on enter-world (now fixed UOW-2562)
- Exchange: ExchangeKinah + ExchangeItems baskets on Player; kinah-only trade executes; item trade deferred
- Reusable: `PlayerSearchMatchService.Matches`, `InventoryCapacity.GetFreeCubeSlots`
