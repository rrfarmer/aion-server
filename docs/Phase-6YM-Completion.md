# Phase 6YM Completion - UOW-1151 Trade-List Missing-Goods Boundary Regression

Date: May 26, 2026

## Unit of Work Summary

Continued Phase 6 trade-list parity by adding production socket-boundary regression coverage for Java's `BUY` no-sell branch where a trade-list tab references a missing goods list. The C# boundary remains non-sending and records staged intent only.

The Java implementation is the source of truth:
- `DialogService.onDialogSelect` loops `TradeListTemplate.TradeTab` entries.
- Java skips a tab when `DataManager.GOODSLIST_DATA.getGoodsListById(tab.getId())` returns null.
- If every tab is skipped, Java sends `SM_SYSTEM_MESSAGE.STR_BUY_SELL_HE_DOES_NOT_SELL_ITEM`.
- `SM_TRADELIST` applies the same missing-goods skip when constructing the packet tab list.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionStorageExpansionDialogTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6YM-Completion.md`

## Implementation Notes

- Added `HandleDialogSelectAsync_BuyMissingGoodsPlansNoSellMessageWithoutSending`.
- Extended the socket-boundary fixture static data with NPC trade-list template `203063` and trade tab id `131`.
- Intentionally did not define goods list `131`.
- The observed plan records missing goods id `131`, no sellable goods, no `SmTradeListPacketPlan`, and a non-live `SystemMessageDoesNotSellItem` descriptor.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "GameServerConnectionStorageExpansionDialogTests" --nologo` passed 7 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` passed 2,159 tests.
- `dotnet test dotnetConversion/AionServer.slnx --nologo` passed 2,366 tests.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.DialogService.onDialogSelect` | `NpcDialogServiceSelectPlanService` through production boundary | Service Planner | Partial | Unit Tested | Partial Parity | Missing-goods no-sell branch is now covered through `HandleDialogSelectAsync`. Live system-message send and Java byte/message comparison remain missing. |
| `com.aionemu.gameserver.dataholders.GoodsListData.getGoodsListById` | `Aion.GameServer.Dataholders.GoodsListTable.GetGoodsListById` | Static Data Repository | Partial | Unit Tested | Needs Verification | The boundary records missing goods id `131` when the trade-list tab has no loaded goods list. Full Java dataholder parity remains unverified. |
| `com.aionemu.gameserver.model.templates.tradelist.TradeListTemplate.TradeTab` | `Aion.GameServer.Dataholders.TradeListTemplateSummary.GoodsListIds` | Static Data DTO | Partial | Unit Tested | Needs Verification | Fixture proves a trade tab can reference missing goods and flow to no-sell intent. Real static-data corpus parity remains unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_TRADELIST` | `Aion.GameServer.Services.SmTradeListPacketPlan` absence from missing-goods branch | Packet Plan | Partial | Unit Tested | Partial Parity | C# withholds the staged packet plan when no goods list is sellable because all referenced ids are missing. Binary packet serialization/opcode `253` remains unimplemented. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_BUY_SELL_HE_DOES_NOT_SELL_ITEM` | `NpcDialogServiceDescriptorKind.SystemMessageDoesNotSellItem` | Packet Descriptor | Partial | Unit Tested | Needs Verification | The descriptor is non-live intent only. Localization and byte-level system-message parity remain unverified. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionStorageExpansionDialogTests.HandleDialogSelectAsync_BuyMissingGoodsPlansNoSellMessageWithoutSending` | Unit / Socket Boundary Regression | `DialogService.onDialogSelect` `BUY`; `GoodsListData.getGoodsListById`; `SM_TRADELIST` constructor filtering | Production `BUY` observes missing goods id `131`, no sellable goods, no packet plan, and no-sell descriptor while sending no packets. | Source-reviewed Java branch plus production C# handler test with loaded trade-list XML and omitted goods-list XML. | No live system-message send, no message byte comparison, no live player legion lookup. |

## Remaining Risks

- Live no-sell system-message sends remain disabled.
- Java localization parameter and packet bytes are not represented or compared.
- Live player legion lookup and limited-item lookup remain missing before `SM_TRADELIST` can go live.
- Real static-data corpus parity for unusual trade-list/goods-list combinations remains unverified.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 0 new production artifacts; 1 production-boundary missing-goods regression added
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 4 blocked/partial categories: live no-sell send/localization, packet byte comparison, player legion lookup, and limited-item lookup
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Start pinning Java packet/message byte expectations for `SM_SYSTEM_MESSAGE.STR_BUY_SELL_HE_DOES_NOT_SELL_ITEM` and `SM_TRADELIST`, or perform the read-only limited-item dependency map before implementing serializer/live-send boundaries.

Recommended starting points:
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionStorageExpansionDialogTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/SmTradeListPacketPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_TRADELIST.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
- `game-server/src/com/aionemu/gameserver/services/LimitedItemTradeService.java`

Keep live sends disabled until byte serialization, opcode/message expectations, localization payloads, runtime limited items, player legion lookup, `Npc.canSell/canBuy`, and NPC AI/controller routing are ready.

# Next Work Options

## Recommended Sequential Task

- Task: pin packet/message expectations for no-sell and `SM_TRADELIST`, beginning with source-level opcode/layout documentation or Java fixture bytes if tooling is available.
- Why: the production boundary now covers success, missing template, restricted goods, and missing goods as non-live staged plans; serializer/live-send work needs objective Java evidence next.
- Files: likely packet tests/services/docs only at first.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Read-only limited-item dependency map | none | Low | Inspect `LimitedItemTradeService`, `LimitedTradeNpc`, and packet use before implementing C# fields. |
| B | Read-only `SM_TRADE_IN_LIST` Java audit | none | Low | Safe analysis for a later trade-in slice. |
| C | Static-data trade NPC type validation test | focused static-data test file | Medium | Avoid shared loader edits unless required. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Packet/message expectation pinning | packet tests, docs | live packet sends |
| Agent A | Read-only limited-item dependency map | read-only Java files | all writes |

## Do Not Parallelize

- `GameServerConnection.HandleDialogSelectAsync`: shared production socket handler.
- Live packet send enablement.
- Progress/handoff docs: orchestrator-owned.
