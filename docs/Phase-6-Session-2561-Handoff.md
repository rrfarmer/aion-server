# Phase 6 Session 2561 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2561: Port live CM_PLAYER_SEARCH handler with SmPlayerSearch packet

## UOW-2561 Summary

Ported the social/`/who` player search. Players at or above the configured search level can now query online players through the social panel and receive results.

### Files Changed

| File | Change |
|------|--------|
| `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmPlayerSearch.cs` | New: opcode 211; count + per-row worldId/x/y/z/classId/genderId/level/status/name(27-char fixed) |
| `dotnetConversion/src/Aion.GameServer/Services/PlayerSearchMatchService.cs` | New: pure, testable filter predicate (PlayerSearchCriteria + PlayerSearchCandidate) |
| `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs` | HandlePlayerSearchAsync + ToPlayerClassId/ToPlayerGenderId; live dispatch case |
| `dotnetConversion/tests/Aion.GameServer.Tests/PlayerSearchMatchServiceTests.cs` | New: 17 tests covering all filter branches + packet bytes |

### Filter behavior (Java CM_PLAYER_SEARCH.runImpl parity)
- Searcher below `LevelToSearch` → STR_CANT_WHO_LEVEL (1400341) and return
- Non-staff searcher: candidate race must match (unless `FactionsSearchMode`); appear-offline (FriendListStatus==0) excluded; staff candidates excluded unless `SearchGmList`
- `lfgOnly==1` excludes non-LFG candidates
- name filter = case-insensitive substring
- minLevel/maxLevel (0xFF = unset); classMask bit test; region == worldId; self excluded
- 104-result cap (Java MAX_RESULTS)
- Status byte: deniedGroup ? 1 : inTeam ? 3 : lfg ? 2 : 0

### Intentional simplifications (documented)
- Faction-prefixed name (`ChatUtil.toFactionPrefixedName`) deferred — plain name sent
- Requires `Player.FriendListStatus` to be set to ONLINE (1) on enter-world for non-staff results to appear; if enter-world leaves it 0, non-staff searches return empty (pre-existing modeling dependency, not introduced here)

## Validation Decision

- Changed surface: production-code (new packet, new pure service, new handler, tests)
- Specific behavior: CM_PLAYER_SEARCH filter + SM_PLAYER_SEARCH byte layout
- Focused C# command: `dotnet test --filter "FullyQualifiedName~PlayerSearchMatchServiceTests"` → 17/17 passed
- Compile signal: `dotnet build Aion.GameServer.csproj` → 0 errors
- Java/Maven: not available; filter logic verified against CM_PLAYER_SEARCH.java source review
- Broad-validation trigger: none

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_PLAYER_SEARCH` (159) | `CmPlayerSearch` + `HandlePlayerSearchAsync` | Handler | Complete | Unit Tested | Partial Parity | Faction-prefix name deferred |
| `SM_PLAYER_SEARCH` (211) | `SmPlayerSearch` | ServerPacket | Complete | Unit Tested | Verified Parity | Byte layout matches writeImpl |
| `CM_PLAYER_SEARCH.runImpl` filter | `PlayerSearchMatchService.Matches` | Service | Complete | Unit Tested | Verified Parity | All branches tested |

## IMPORTANT FINDING: Exchange item trade execution needs a persistence design decision

UOW-2561 was originally scoped as exchange item trade execution but pivoted after analysis revealed a **persistence-correctness blocker**:

- Current inventory persistence uses per-storage dirty/deleted snapshot tracking keyed by `item_unique_id` (ObjectId), flushed at logout via delete+insert.
- A cross-player item transfer moves an item from player A's `InventoryItems` to player B's. Because `ObjectId` is the shared DB primary key:
  - If B logs out first, B INSERTs row X (owner=B).
  - When A later logs out, A's dirty-delete tracking DELETEs row X — **deleting B's item** (item loss), or a duplicate-key conflict.
- The kinah-only path (UOW-2554) avoids this because kinah is a count mutation on each player's own pre-existing kinah row, not a row ownership transfer.

**Required before item trade execution can be ported safely:** an atomic, immediate ownership-transfer repository method, e.g.:
`UPDATE inventory SET item_owner = ?, slot = ?, item_location = ? WHERE item_unique_id = ?`
executed at trade time, plus removing the item from the giver's dirty-delete set and adding to the receiver without marking it `New` (since the row already exists). Partial-stack trades additionally need IdFactory allocation + INSERT of a new row for the split-off portion.

This is a multi-step UOW with its own design + tests; do not fold it into a quick handler.

## Next Recommended UOW

**UOW-2562: Exchange item trade execution (with persistence design)**

Steps:
1. Add `IPlayerEnterWorldRepository.TransferItemOwnershipAsync(int itemObjectId, int newOwnerId, int newLocation, long newSlot)` (atomic UPDATE)
2. In `ExecuteKinahOnlyExchangeAsync` (rename to `ExecuteExchangeTradeAsync`): validate both players have free cube slots (`InventoryCapacity.GetFreeCubeSlots`) >= partner item count
3. Full-stack items: move InventoryItem between players' lists (new OwnerId, slot), call TransferItemOwnershipAsync, send SmInventoryAddItem(PlayerExchangeGet) to receiver + SmDeleteItem to giver
4. Defer partial-stack split (document)
5. Clear ExchangeItems on both

Alternative simpler UOWs:
1. **CM_LEGION_HISTORY stub** — send empty SM_LEGION_HISTORY so legion-history UI doesn't hang
2. **CM_QUEST_SHARE no-members message** — send STR group/alliance no-members message (1100000/1100005)
3. **Verify FriendListStatus on enter-world** — ensure online players get status ONLINE(1) so player search returns them; small fix if missing

Focused validation recipe for UOW-2562:
- Behavior: items move between players' inventories on double-confirm with correct persistence call
- Focused C# command: `dotnet test --filter "FullyQualifiedName~PlayerExchangeRequestService|FullyQualifiedName~ExchangeAddKinahPlanService"`
- Java/Maven: not expected
- Broad-validation trigger: none

## Remaining Risks

- Exchange item trade execution still deferred (persistence design required — see finding above)
- Player search depends on FriendListStatus being set ONLINE on enter-world (verify)
- Faction-prefixed names not applied in player search results
- Exchange: no immediate kinah persistence (saved at logout/periodic save)
- CM_REPLACE_ITEM cross-storage deferred
- All legion handlers deferred (LegionService not ported)
- CM_GATHER handler deferred
- XP/level-up system not ported
- CM_LEVEL_READY: SM_RIFT_ANNOUNCE, SM_CONQUEROR_PROTECTOR deferred

## Context Needed By Next Session

- All 185 Java client opcodes are registered in C# (completed Session 2560)
- Exchange: ExchangeKinah + ExchangeItems baskets tracked on Player; kinah-only trade executes; item trade deferred pending persistence design
- `PlayerSearchMatchService.Matches(criteria, candidate, searcherObjectId)` is the reusable filter
- `InventoryCapacity.GetFreeCubeSlots(player)` available for trade slot validation
- See Phase-6-Session-2560-Handoff.md for full opcode-coverage milestone context
