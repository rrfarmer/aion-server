# Phase 6 Session 2560 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2560/2560b: Register all remaining Java client opcodes — C# now covers all 185 Java opcodes

## Session Summary (UOWs 2558–2560)

| UOW | Summary |
|-----|---------|
| 2558 | CM_EXCHANGE_ADD_ITEM live handler + Player.ExchangeItems basket + item restore on cancel |
| 2559 | PlayerAllianceRuntime.ChangeLootRules + alliance path in CM_DISTRIBUTION_SETTINGS |
| 2560 | Register 16 remaining parsers (14 + 2 bonus) — all 185 Java opcodes now have C# parsers |

## Major Milestone: Complete Client Opcode Coverage

C# `GameClientPacketFactory` now registers parsers for all 185 opcodes from Java `AionClientPacketFactory`. No client packet is silently dropped at the parser layer. The gap was:

**14 parsers in UOW-2560:** CM_LEGION_SEND_EMBLEM_INFO (16), CM_LEGION_DOMINION_REQUEST_RANKING (29), CM_BUILDER_COMMAND (41), CM_BUILDER_CONTROL (42), CM_LEGION_SEND_EMBLEM (47), CM_LEGION_HISTORY (55), CM_LEGION_MODIFY_EMBLEM (59), CM_PLAYER_SEARCH (159), CM_LEGION_UPLOAD_INFO (160), CM_LEGION_UPLOAD_EMBLEM (161), CM_QUEST_SHARE (164), CM_DEBUG_COMMAND (180)

**2 final parsers in UOW-2560b:** CM_CHARACTER_EDIT (7, AUTHED), CM_CAPTCHA (14, IN_GAME)

All handlers remain deferred with Java parity notes.

## Exchange System — Full Status

| Handler | Status | Notes |
|---------|--------|-------|
| CM_EXCHANGE_REQUEST | Live | Request + question window |
| CM_EXCHANGE_ADD_KINAH | Live | ExchangeKinah tracking |
| CM_EXCHANGE_ADD_ITEM | Live | ExchangeItems dict; send UI update; cancel restore |
| CM_EXCHANGE_LOCK | Live | Lock + notify partner |
| CM_EXCHANGE_OK | Live | Kinah-only trade on double confirm; items deferred |
| CM_EXCHANGE_CANCEL | Live | Restore item UI + reset state |

## Deferred Handlers in New Parsers

| Opcode | Java handler | Deferred reason |
|--------|-------------|-----------------|
| 7 | CM_CHARACTER_EDIT | Appearance system not ported |
| 14 | CM_CAPTCHA | PunishmentService not ported |
| 16 | CM_LEGION_SEND_EMBLEM_INFO | LegionService not ported |
| 29 | CM_LEGION_DOMINION_REQUEST_RANKING | DominionService not ported |
| 41/42 | CM_BUILDER_COMMAND/CONTROL | Admin command pipeline not ported |
| 47 | CM_LEGION_SEND_EMBLEM | LegionService |
| 55 | CM_LEGION_HISTORY | LegionService |
| 59 | CM_LEGION_MODIFY_EMBLEM | LegionService |
| 159 | CM_PLAYER_SEARCH | World player list access |
| 160/161 | CM_LEGION_UPLOAD_INFO/EMBLEM | LegionService |
| 164 | CM_QUEST_SHARE | Quest engine |
| 180 | CM_DEBUG_COMMAND | Admin command pipeline |

## Validation

| UOW | Command | Result | Trigger |
|-----|---------|--------|---------|
| 2558 | `--filter "ExchangeAddKinahPlanService\|PlayerExchangeRequestService\|SmExchangeAddItemPacketTests"` | 27/27 | none |
| 2559 | `--filter "PlayerAllianceRuntime\|CmMoveItemTests"` | 38/38 | none |
| 2560 | build + `--filter "CmGatherTests"` | 4/4 | none |

## Context Needed By Next Session

### Exchange item basket
- `Player.ExchangeItems`: `Dictionary<int, long>` (objectId → committedCount); cleared on accept/cancel
- UI simulation: items appear moved without actual inventory mutation
- Cancel: `RestoreExchangeItemsToInventoryUiAsync` sends `PlayerExchangeGetBack(0x23)` packets
- Trade execution with item transfer remains deferred

### Opcode coverage
- All 185 Java client opcodes registered in C# as of this session
- C# has 186 registrations (1 additional C#-only)
- No packets silently dropped at parser layer

### Previous context
- See Phase-6-Session-2559-Handoff.md for exchange/loot/NPC-search/windstream context

## Next Recommended UOW

**UOW-2561: Exchange item trade execution (move items on confirm)**

When both players confirm and have items in ExchangeItems:
1. Remove each item from player's inventory (update InventoryItems)
2. Add partner's items to player's inventory (requires slot assignment)
3. Send SmInventoryAddItem(PlayerExchangeGet=0x21) for received items
4. Persist item ownership change per item

Alternative next UOWs:
1. **CM_PLAYER_SEARCH response** — Return empty SM_PLAYER_SEARCH (with world players if accessible)
2. **CM_LEGION_HISTORY stub** — Send empty SM_LEGION_HISTORY when legion history is requested
3. **CM_QUEST_SHARE stub** — Send group-has-no-members message for unimplemented quest share
4. **CM_CAPTCHA full handler** — Port PunishmentService.setIsNotGatherable for gathering restriction

Focused validation recipe for UOW-2561:
- Behavior: items removed from player's inventory and added to partner's on double-confirm
- Focused C# command: `dotnet test --filter "ExchangeAddKinahPlanService\|PlayerExchangeRequestService"`
- Java/Maven: not expected
- Broad-validation trigger: none

## Remaining Risks

- Exchange item transfer on confirm deferred (cross-player inventory mutation)
- Exchange: no immediate kinah persistence (saved at logout/periodic save)
- CM_REPLACE_ITEM cross-storage deferred
- All legion handlers deferred (LegionService not ported)
- CM_GATHER handler deferred (gathering controller not ported)
- XP/level-up system not ported
- CM_LEVEL_READY: SM_RIFT_ANNOUNCE, SM_CONQUEROR_PROTECTOR deferred
- Transform system not yet ported
