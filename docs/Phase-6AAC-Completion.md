# Phase 6AAC Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1193
Status: Phase 6 continues; `SM_SELL_ITEM` now has a staged packet plan and concrete serializer, but live dialog routing remains incomplete.

## Session Summary

UOW-1193 implemented the next packet-shaped price-consumer item from the Phase 6 map: Java `SM_SELL_ITEM`. The C# port now has a non-live packet plan and a concrete server packet with byte-shape tests, without enabling live sell-window dispatch.

Files changed:

- `dotnetConversion/src/Aion.GameServer/Services/SmSellItemPacketPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSellItem.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/SmSellItemPacketPlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `docs/Phase-6-PricesService-Consumer-Map.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6AAC-Completion.md`

## What Changed

- Added `SmSellItemPacketPlanService` and `SmSellItemPacketPlan`.
- Added `SmSellItem` packet with opcode `62`, matching Java `ServerPacketsOpcodes.addPacketOpcode(62, SM_SELL_ITEM.class)`.
- Mirrored Java `SM_SELL_ITEM.writeImpl` fields:
  - `targetObjectId`;
  - `TradeNpcType.index()`;
  - `buyPriceRate`;
  - `showBuyTab`;
  - `showSellTab`;
  - trade-tab count;
  - trade-tab ids.
- Mirrored constructor behavior:
  - purchase template present: use template `NpcType`, `BuyPriceRate`, and `GoodsListIds`;
  - purchase template missing: use `NORMAL`, `PricesService.GetVendorSellModifier`, and no trade tabs;
  - `showBuyTab = npc.canSell()`;
  - `showSellTab = npc.canBuy() || npc.canPurchase()`.
- Kept live routing staged; `NpcDialogServiceSelectPlanService` still produces a `SellItemPacket` descriptor rather than sending the packet.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "SmSellItemPacketPlanServiceTests|SmSellItem_WritesJavaSellWindowPayload|PricesServiceTests" --nologo` passed 7 tests.
- No Java runtime packet capture was compared, so parity remains `Needs Verification`.

## Migration Parity Table - UOW-1193

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SELL_ITEM` | `Aion.GameServer.Services.SmSellItemPacketPlanService` / `Aion.GameServer.Network.Aion.ServerPackets.SmSellItem` | Packet / Planner | Partial | Unit Tested | Needs Verification | Constructor-derived fields and `writeImpl` order are staged and byte-tested from source-derived expectations. No Java runtime packet capture was compared. Live dialog routing remains disabled/staged. |
| `com.aionemu.gameserver.model.templates.tradelist.TradeListTemplate` | `Aion.GameServer.Dataholders.TradeListTemplateSummary` | DTO / Static Data Dependency | Partial | Unit Tested | Needs Verification | Planner uses purchase-template `NpcType`, `BuyPriceRate`, and `GoodsListIds`. Java uses `TradeTab` objects and `TradeNpcType` enum; C# uses summary strings and ids. Static-data normalization remains a risk. |
| `com.aionemu.gameserver.model.templates.tradelist.TradeNpcType` | C# trade NPC type mapping inside `SmSellItemPacketPlanService` | Enum / Protocol Value | Partial | Unit Tested | Needs Verification | Known Java indices for `NORMAL`, `ABYSS`, `LEGION_COIN`, `REWARD`, and `ABYSS_KINAH` are mapped. Unknown type is staged as `UnknownTradeNpcType`; Java enum inputs should already be valid. |
| `com.aionemu.gameserver.services.trade.PricesService` | `Aion.GameServer.Services.PricesService` | Service / Utility Dependency | Partial | Unit Tested | Needs Verification | Missing purchase-template fallback uses `GetVendorSellModifier`. Live config sourcing remains default/staged unless callers inject options. |
| `com.aionemu.gameserver.model.gameobjects.Npc` | `SmSellItemPacketPlanInput` runtime facts | Model / Runtime Fact Source | Partial | Unit Tested | Needs Verification | Planner accepts target object id and `canSell`/`canBuy`/`canPurchase` facts instead of reading a live NPC object. Live dialog fact assembly remains future work. |

Tests added or updated:

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `SmSellItemPacketPlanServiceTests.CreatePlan_UsesJavaPurchaseTemplateFields` | Unit | Java `SM_SELL_ITEM(Npc)` constructor | Validates purchase-template NPC type, buy-price rate, trade-tab ids, and buy/sell tab flags. | Deterministic source-derived expectations. | No live `Npc` object or Java runtime execution. |
| `SmSellItemPacketPlanServiceTests.CreatePlan_UsesVendorSellModifierWhenPurchaseTemplateMissing` | Unit | Java `SM_SELL_ITEM(Npc)` fallback to `PricesService.getVendorSellModifier` | Validates missing purchase-template fallback rate and default `NORMAL` NPC type. | Deterministic source-derived expectation. | Live config injection at dialog routing remains future work. |
| `SmSellItemPacketPlanServiceTests.CreatePlan_ReportsUnknownTradeNpcType` | Unit | C# staging guard around Java enum mapping | Validates unknown type produces a non-ready plan instead of serializing ambiguous data. | C# guard only. | Java source would normally prevent unknown enum values. |
| `GamePacketTests.SmSellItem_WritesJavaSellWindowPayload` | Unit / Packet Byte Shape | Java `SM_SELL_ITEM.writeImpl` | Validates concrete packet payload field order and primitive widths. | Source-derived byte-shape assertions. | No Java-generated golden packet capture. |

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live packet plan plus 1 concrete packet and 4 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 3 grouped categories: live dialog routing, live NPC/trade-list fact assembly, and Java runtime packet capture
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Live sell-window dialog routing remains staged and does not send `SmSellItem`.
- Runtime `Npc.canSell`, `Npc.canBuy`, `Npc.canPurchase`, and purchase-template assembly need live wiring before dispatch.
- Trade NPC type and trade-list template facts are string/static-data summaries, not Java enum/object instances.
- No Java runtime packet capture was compared.
- Serialization is source-derived only; date/time, threading, and reflection behavior are not involved in this unit.

## Next Recommended Unit of Work

Primary next unit:

- Audit paid teleport price deduction before touching live teleport handler paths.

Fallback units:

- Continue `SM_SELL_ITEM` live-readiness by adding non-live dialog fact assembly that can produce `SmSellItemPacketPlan` without sending it.
- Continue broker live-readiness by isolating packet ordering and persistence rollback behavior.

Defer live `SM_SELL_ITEM` sends until dialog routing, runtime trade-list facts, and packet order are separately tested.
