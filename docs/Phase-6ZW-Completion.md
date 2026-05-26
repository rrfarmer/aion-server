# Phase 6ZW Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1187
Status: Phase 6 continues; remaining price consumers are now mapped and ranked, but several live price paths are still incomplete.

## Session Summary

UOW-1187 created a durable `PricesService` consumer map after UOW-1186 completed the stigma equip price seam. This unit made no runtime code changes; it documented every Java `PricesService` caller found in the scan, matched each to the current C# surface, and ranked the next implementation candidates by risk.

Files changed:

- `docs/Phase-6-PricesService-Consumer-Map.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ZW-Completion.md`

## What Changed

- Added `docs/Phase-6-PricesService-Consumer-Map.md`.
- Recorded Java consumers for:
  - `SM_PRICES`;
  - `PlayerEnterWorldService`;
  - `TradeList`;
  - `DialogService`;
  - `SM_SELL_ITEM`;
  - `TradeService`;
  - `ItemSocketService`;
  - `ItemRemodelService`;
  - `StigmaService`;
  - `ArmsfusionService`;
  - `TeleportService`;
  - `RelinquishCraftStatus`;
  - `LegionService`;
  - `BrokerService`;
  - `MailService`;
  - `Influence`.
- Identified high-conflict areas:
  - broker registration commission in `GameServerConnection`;
  - player mail send cost in `GameServerConnection`;
  - live enter-world `SM_PRICES` routing before `Influence` exists;
  - live sell-window/sell-item packet routing before a C# `SM_SELL_ITEM` plan exists.
- Identified safer next candidates:
  - pure armsfusion price planner;
  - pure mail-send cost planner;
  - pure broker-registration commission calculator;
  - `SM_SELL_ITEM` packet plan with byte tests.

## Validation

- Documentation-only unit; no dotnet tests were required.
- `git diff --check` passed with existing line-ending warnings only.
- No Java runtime comparison was run, so parity remains `Needs Verification` or `Unknown` for unmapped/unported rows.

## Migration Parity Table - UOW-1187

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.trade.PricesService` | `Aion.GameServer.Services.PricesService` | Service / Utility | Partial | Unit Tested | Needs Verification | Audit map now covers known Java consumers. Pure formulas are ported, but no Java runtime comparison has been executed. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PRICES` | `Aion.GameServer.Services.SmPricesPacketPlanService` / `Aion.GameServer.Network.Aion.ServerPackets.SmPrices` | Packet / Plan Service | Partial | Unit Tested | Needs Verification | Non-live packet plan exists; live enter-world wiring remains default-only. |
| `com.aionemu.gameserver.model.trade.TradeList` | `Aion.GameServer.Services.SmTradeListPacketPlanService` / `Aion.GameServer.Services.TradeApFormulaService` | Model / Packet Planner | Partial | Unit Tested | Needs Verification | AP formulas and packet plan surfaces exist; kinah buy-price formula consumer needs focused audit/test. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SELL_ITEM` | future C# sell-item packet/service caller | Packet / Consumer | Not Started | No Tests | Unknown | Java fallback uses `PricesService.getVendorSellModifier`. No direct C# packet equivalent was found in this scan. |
| `com.aionemu.gameserver.services.TradeService` | future C# trade sell/buy service plans plus `TradeApFormulaService` | Service | Partial | Unit Tested | Needs Verification | Pure AP branches have coverage. Kinah sell reward and vendor modifier live inventory paths remain unported. |
| `com.aionemu.gameserver.services.item.ItemSocketService` | `Aion.GameServer.Services.ItemSocketService` | Service / Item Mutation Planner | Partial | Unit Tested | Needs Verification | Price consumer seam ported in UOW-1184. Live DAO/packet behavior remains staged. |
| `com.aionemu.gameserver.services.item.ItemRemodelService` | `Aion.GameServer.Services.ItemRemodelService` | Service / Item Mutation Planner | Partial | Unit Tested | Needs Verification | Price consumer seam ported in UOW-1185. Live DAO/packet behavior remains staged. |
| `com.aionemu.gameserver.services.StigmaService` | `Aion.GameServer.Services.StigmaService` | Service / Equipment Planner | Partial | Unit Tested | Needs Verification | Price consumer seam ported in UOW-1186. Live config/influence routing and effect execution remain staged. |
| `com.aionemu.gameserver.services.ArmsfusionService` | future C# armsfusion planner | Service / Item Mutation | Not Started | No Tests | Unknown | Formula appears narrow, but no C# service equivalent was found in this scan. |
| `com.aionemu.gameserver.services.teleport.TeleportService` | `Aion.GameServer.Services.PlayerTeleportService` / `Aion.GameServer.Network.Aion.GameServerConnection` | Service / Movement | Partial | Unit Tested | Needs Verification | Existing C# teleport work focuses movement/spawn. Paid transport price deduction is not ported. |
| `com.aionemu.gameserver.services.craft.RelinquishCraftStatus` | future C# relinquish craft-status planner | Service | Not Started | No Tests | Unknown | Existing C# craft-skill kinah learn flow is separate from Java relinquish craft status. |
| `com.aionemu.gameserver.services.LegionService` | future C# legion emblem workflow | Service | Partial / Not Started | No Tests | Unknown | Legion data hydration exists elsewhere, but emblem price workflow is not isolated. |
| `com.aionemu.gameserver.services.BrokerService` | `Aion.GameServer.Data.BrokerRepository` / `Aion.GameServer.Network.Aion.GameServerConnection` | Service / Repository / Connection Handler | Partial | Manual Only | Needs Verification | Broker persistence and packet handlers exist. Registration commission currently remains baseline and should be isolated into a non-live calculator before live changes. |
| `com.aionemu.gameserver.services.mail.MailService` | `Aion.GameServer.Data.MailRepository` / `Aion.GameServer.Network.Aion.GameServerConnection` | Service / Repository / Connection Handler | Partial | Manual Only | Needs Verification | Player mail persistence/packet surfaces exist. Send-cost price formula and commission logic need a pure planner before live changes. |
| `com.aionemu.gameserver.model.siege.Influence` | `Aion.GameServer.Services.PriceInfluenceRates` | Runtime Fact Input | Not Started | Unit Tested | Needs Verification | Every live price consumer still depends on this missing runtime source for true Java parity. |

Tests added or updated:

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None; documentation-only audit | Manual / Static Audit | Java `rg` caller scan and C# `rg` surface scan | Validates durable discovery and risk ranking, not runtime behavior. | Source scan only. | No compile/runtime behavior changed; no Java runtime comparison. |

## Summary Metrics

- Total Java artifacts discovered: 15 grouped artifact rows in this unit
- Total artifacts ported: 0 code artifacts; 1 durable audit document added
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 10 grouped rows
- Total blocked artifacts: 5 grouped rows with unknown/not-started or high-conflict surfaces (`SM_SELL_ITEM`, armsfusion, relinquish craft, legion emblem, live influence)
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Live `Influence`/siege ownership is not ported, so live price and tax values still cannot match Java world state.
- C# still uses string race values in staged callers, while Java uses `Race` enum.
- Several Java price consumers combine pricing with inventory mutation, packet send order, DAO writes, scheduled tasks, or recipient fanout.
- `SM_SELL_ITEM` has no direct C# equivalent found in this scan; serialization parity is unknown.
- Broker and player mail price paths are high conflict because they live in `GameServerConnection` and repository transactions.
- No Java runtime comparison was executed for this map.

## Next Recommended Unit of Work

Primary next unit:

- Implement one map-ranked pure planner before touching live connection code.

Suggested order:

1. Pure armsfusion price planner, if no existing C# armsfusion service appears in a deeper search.
2. Pure mail-send cost planner for `MailService.sendMail`.
3. Pure broker-registration commission calculator.
4. `SM_SELL_ITEM` packet plan with byte tests.

## Do Not Parallelize

- `docs/PHASE-6-PROGRESS.md`, `docs/Phase-6-PricesService-Consumer-Map.md`, and Phase 6 handoff docs: orchestrator-owned shared migration history.
- `GameServerConnection.cs` broker/mail paths until a pure planner exists.
- Live price consumers requiring runtime `Influence` unless the unit is explicitly a staged default-only seam.
