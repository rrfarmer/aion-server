# Phase 6 PricesService Consumer Map

Date: May 26, 2026
Scope: Java `com.aionemu.gameserver.services.trade.PricesService` consumers and current C# parity status.
Source of truth: Java project.

## Purpose

This map records every Java caller found in the Phase 6 price scan so future units can choose the next implementation target without re-discovering the same surface. Do not treat any row as verified parity unless a test or runtime comparison says so.

## Java Consumer Inventory

| Java Consumer | Java Price API | C# Artifact / Surface | Current Status | Risk | Recommended Next Action | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PRICES` | `getGlobalPrices`, `getGlobalPricesModifier`, `getTaxes` | `Aion.GameServer.Services.SmPricesPacketPlanService`; `Aion.GameServer.Network.Aion.ServerPackets.SmPrices` | Partial | Medium | Keep non-live until live influence facts exist. | Packet byte tests exist for deterministic injected influence. Live enter-world still sends baseline `SmPrices`. |
| `com.aionemu.gameserver.services.player.PlayerEnterWorldService` | sends `new SM_PRICES()` | `Aion.GameServer.Network.Aion.GameServerConnection` enter-world send | Partial | Medium/High | Defer live wiring until runtime `Influence` source exists. | Threading/packet-order risk because enter-world packet fanout is broad. |
| `com.aionemu.gameserver.model.trade.TradeList` | `getBuyPrice`, `getVendorBuyModifier` | `SmTradeListPacketPlanService`, `NpcDialogTradeListFactAdapterService`, `TradeApFormulaService` | Partial | Medium | Audit buy-price calculations separately from packet serialization. | Current C# has trade-list packet/fact planners and AP formulas; kinah buy-price formula parity still needs a focused row/test. |
| `com.aionemu.gameserver.services.DialogService` | `getVendorBuyModifier`; sends `SM_SELL_ITEM` | `NpcDialogServiceSelectPlanService`; `QuestDialogNpcTargetBranchInputAssemblyPlanService`; `SmTradeListPacketPlanService` | Partial | Medium | Keep buy-window planning non-live; add explicit vendor-buy modifier provenance if needed. | Existing non-live plan uses configured modifier inputs. Live sends remain intentionally staged. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SELL_ITEM` | `getVendorSellModifier` fallback | No direct `SmSellItem` C# packet found in this scan | Not Started | Medium | Port packet plan before live sell-window send. | Serialization and NPC trade-list fallback behavior need packet tests. |
| `com.aionemu.gameserver.services.TradeService` | `getVendorSellModifier`, `getSellReward`, `getVendorBuyModifier` | `TradeApFormulaService` plus future sell/buy service plans | Partial | Medium/High | Split sell-reward pure formula from item/AP mutation paths. | Sell-to-shop mutates inventory and may fan out packets; avoid live side effects until packet/repository boundaries are isolated. |
| `com.aionemu.gameserver.services.item.ItemSocketService` | `getPriceForService(650, race)` | `Aion.GameServer.Services.ItemSocketService` | Partial | Medium | Later wire live config/influence; no immediate action. | UOW-1184 added optional price/influence seam and tests. Live DAO/packet side effects remain staged. |
| `com.aionemu.gameserver.services.item.ItemRemodelService` | `getPriceForService(1000, race)` | `Aion.GameServer.Services.ItemRemodelService` | Partial | Medium | Later wire live config/influence; no immediate action. | UOW-1185 added optional price/influence seam and tests. Live mutation/packet side effects remain staged. |
| `com.aionemu.gameserver.services.StigmaService` | `getPriceForService(selectedBaseFee, race)` | `Aion.GameServer.Services.StigmaService` | Partial | Medium | Later wire `EquipmentService` caller with live config/influence. | UOW-1186 replaced local baseline stub and added adjusted-price test. |
| `com.aionemu.gameserver.services.ArmsfusionService` | `getPriceForService(basePricePerLevelSquared * level * level, race)` | `Aion.GameServer.Services.ArmsfusionPricePlanService` | Partial | Medium | Keep non-live until DAO/packet/audit execution boundaries are ported. | UOW-1190 covers staged `fusionWeapons` and `breakWeapons` plans. Live DAO store, packet sends, audit logging, and handler routing remain unported. |
| `com.aionemu.gameserver.services.teleport.TeleportService` | `getPriceForService(basePrice, race)` | `Aion.GameServer.Services.PlayerTeleportService` and connection teleport handlers | Partial | Medium/High | Audit paid teleport request path before implementation. | Current C# teleport work focuses movement/spawn, not paid transport price deduction. Packet/order and map-change side effects are broad. |
| `com.aionemu.gameserver.services.craft.RelinquishCraftStatus` | `getPriceForService(basePrice, race)` | `CraftSkillUpdateService` and future relinquish plan | Partial / Not Started | Medium | Candidate if relinquish-specific planner exists or can be added cleanly. | Existing craft-skill learn kinah path is not the same Java relinquish flow. |
| `com.aionemu.gameserver.services.LegionService` | `getPriceForService(LEGION_EMBLEM_REQUIRED_KINAH, race)` | Legion DB/hydration surfaces; no focused emblem price planner found | Partial / Not Started | High | Defer until legion emblem workflow surface is isolated. | Legion contribution, ranking, emblem persistence, and packet fanout are broad. |
| `com.aionemu.gameserver.services.BrokerService` | `getPriceForService(registrationCommission, race)` | `Aion.GameServer.Services.BrokerRegistrationCommissionPlanService`; `BrokerRepository`; `GameServerConnection` broker handlers | Partial | High | Keep persistence/packet ordering staged; later wire live config/influence facts into broker registration. | UOW-1192 extracted Java registration commission rates, minimum commission branch, and central price formula into a pure planner used by `HandleBrokerRegisterItemAsync`. Persistence and packet order remain high conflict. |
| `com.aionemu.gameserver.services.mail.MailService` | `getPriceForService(baseCost + commissions, race)` | `Aion.GameServer.Services.MailSendCostPlanService`; `MailRepository`; `GameServerConnection` mail handlers | Partial | High | Keep persistence/fanout staged; later wire live config/influence facts into send-mail handler. | UOW-1191 extracted Java base-cost, commission, item-quality rate, and central price formula into a pure planner used by `HandleSendMailAsync`. Courier pass handling, persistence, and recipient fanout remain broader live risks. |

## Migration Parity Table - PricesService Consumer Map

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.trade.PricesService` | `Aion.GameServer.Services.PricesService` | Service / Utility | Partial | Unit Tested | Needs Verification | Pure formulas are ported and consumed by several staged callers. No Java runtime comparison has been executed. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PRICES` | `Aion.GameServer.Services.SmPricesPacketPlanService` / `Aion.GameServer.Network.Aion.ServerPackets.SmPrices` | Packet / Plan Service | Partial | Unit Tested | Needs Verification | Non-live packet plan exists; live enter-world wiring remains default-only. |
| `com.aionemu.gameserver.model.trade.TradeList` | `Aion.GameServer.Services.SmTradeListPacketPlanService` / `Aion.GameServer.Services.TradeApFormulaService` | Model / Packet Planner | Partial | Unit Tested | Needs Verification | AP formulas and packet plan surfaces exist; kinah buy-price formula consumer needs focused audit/test. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SELL_ITEM` | future C# sell-item packet/service caller | Packet / Consumer | Not Started | No Tests | Unknown | Java fallback uses `PricesService.getVendorSellModifier`. No direct C# packet equivalent was found in this scan. |
| `com.aionemu.gameserver.services.TradeService` | future C# trade sell/buy service plans plus `TradeApFormulaService` | Service | Partial | Unit Tested | Needs Verification | Pure AP branches have coverage. Kinah sell reward and vendor modifier live inventory paths remain unported. |
| `com.aionemu.gameserver.services.item.ItemSocketService` | `Aion.GameServer.Services.ItemSocketService` | Service / Item Mutation Planner | Partial | Unit Tested | Needs Verification | Price consumer seam ported in UOW-1184. Live DAO/packet behavior remains staged. |
| `com.aionemu.gameserver.services.item.ItemRemodelService` | `Aion.GameServer.Services.ItemRemodelService` | Service / Item Mutation Planner | Partial | Unit Tested | Needs Verification | Price consumer seam ported in UOW-1185. Live DAO/packet behavior remains staged. |
| `com.aionemu.gameserver.services.StigmaService` | `Aion.GameServer.Services.StigmaService` | Service / Equipment Planner | Partial | Unit Tested | Needs Verification | Price consumer seam ported in UOW-1186. Live config/influence routing and effect execution remain staged. |
| `com.aionemu.gameserver.services.ArmsfusionService` | `Aion.GameServer.Services.ArmsfusionPricePlanService` | Service / Item Mutation Planner | Partial | Unit Tested | Needs Verification | `fusionWeapons` and `breakWeapons` now have non-live validation/mutation planners. Live DAO store, packet sends, audit logging, and handler routing remain unported. |
| `com.aionemu.gameserver.services.teleport.TeleportService` | `Aion.GameServer.Services.PlayerTeleportService` / `Aion.GameServer.Network.Aion.GameServerConnection` | Service / Movement | Partial | Unit Tested | Needs Verification | Existing C# teleport work focuses movement/spawn. Paid transport price deduction is not ported. |
| `com.aionemu.gameserver.services.craft.RelinquishCraftStatus` | future C# relinquish craft-status planner | Service | Not Started | No Tests | Unknown | Existing C# craft-skill kinah learn flow is separate from Java relinquish craft status. |
| `com.aionemu.gameserver.services.LegionService` | future C# legion emblem workflow | Service | Partial / Not Started | No Tests | Unknown | Legion data hydration exists elsewhere, but emblem price workflow is not isolated. |
| `com.aionemu.gameserver.services.BrokerService` | `Aion.GameServer.Services.BrokerRegistrationCommissionPlanService` / `Aion.GameServer.Data.BrokerRepository` / `Aion.GameServer.Network.Aion.GameServerConnection` | Service / Repository / Connection Handler | Partial | Unit Tested | Needs Verification | Registration commission rates, minimum-fee branch, and central `PricesService.getPriceForService` call are staged and used by the broker registration handler with default price facts. Live config/influence sourcing, persistence, and packet-order parity remain partial. |
| `com.aionemu.gameserver.services.mail.MailService` | `Aion.GameServer.Services.MailSendCostPlanService` / `Aion.GameServer.Network.Aion.GameServerConnection` | Service / Repository / Connection Handler | Partial | Unit Tested | Needs Verification | Send-cost base fee, attached-kinah commission, item-quality commission, and `PricesService.getPriceForService` are staged and used by the send-mail handler with default price facts. Live config/influence sourcing, courier pass behavior, persistence rollback, and recipient fanout remain partial. |
| `com.aionemu.gameserver.model.siege.Influence` | `Aion.GameServer.Services.PriceInfluenceRates` | Runtime Fact Input | Not Started | Unit Tested | Needs Verification | Every live price consumer still depends on this missing runtime source for true Java parity. |

## Tests Referenced

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `PricesServiceTests.CreateSnapshot_UsesJavaInfluencePriceAndTaxRules` | Central global price/tax/vendor modifier formulas. | Source-derived only. |
| `PricesServiceTests.PriceCalculations_FloorEachJavaDoublePercentageStep` | Java-style truncation after each percentage step. | Source-derived only. |
| `SmPricesPacketPlanServiceTests.*` | Non-live `SM_PRICES` packet payload bytes from injected price facts. | Source-derived packet bytes only. |
| `ItemSocketServiceTests.RemoveManastone_UsesJavaPricesServiceForKinahFee` | Manastone removal fee uses central price formula. | Source-derived only. |
| `ItemRemodelServiceTests.CreateRemodelPlan_UsesJavaPricesServiceForKinahFee` | Remodel fee uses central price formula. | Source-derived only. |
| `StigmaServiceTests.NotifyEquipAction_UsesJavaPricesServiceForKinahFee` | Stigma equip fee uses central price formula. | Source-derived only. |
| `ArmsfusionPricePlanServiceTests.GetBasePricePerLevelSquared_UsesJavaQualityMapping` | Java armsfusion quality-to-base-price mapping. | Source-derived only. |
| `ArmsfusionPricePlanServiceTests.CreatePlan_UsesJavaPriceFormulaForMainWeaponLevelAndQuality` | Java armsfusion base price and central price formula application. | Source-derived only. |
| `ArmsfusionPricePlanServiceTests.CreateFusionPlan_SucceedsWithJavaValidationOrderAndNonLiveMutations` | Java armsfusion success-side validation, price, and planned item/kinah mutations. | Source-derived only. |
| `ArmsfusionPricePlanServiceTests.CreateFusionPlan_FollowsJavaFailureOrder` | Java armsfusion failure ordering through missing/equipped, fusibility, kinah, temporary exchange, fused state, type, level, and improvement checks. | Source-derived only. |
| `ArmsfusionPricePlanServiceTests.CreateBreakPlan_SucceedsWithJavaSetFusionedItemNullMutations` | Java armsfusion decompose success-side fused item clearing and fusion-stone removal. | Source-derived only. |
| `ArmsfusionPricePlanServiceTests.CreateBreakPlan_FollowsJavaFailureOrder` | Java armsfusion decompose missing-target and not-fused failure ordering. | Source-derived only. |
| `MailSendCostPlanServiceTests.GetQualityPriceRate_UsesJavaMailQualityMapping` | Java mail item-quality commission rates. | Source-derived only. |
| `MailSendCostPlanServiceTests.CreatePlan_UsesJavaMailCommissionsAndPricesService` | Java mail base cost, commission, central price formula, and attached-kinah final total. | Source-derived only. |
| `MailSendCostPlanServiceTests.CreatePlan_DefaultNormalLetterMatchesJavaBaseline` | Default normal no-attachment mail cost remains `10`. | Source-derived only. |
| `BrokerRegistrationCommissionPlanServiceTests.CreatePlan_UsesJavaRegisteredItemCountRate` | Java broker registration commission rate threshold at registered item count `10`. | Source-derived only. |
| `BrokerRegistrationCommissionPlanServiceTests.CreatePlan_AppliesJavaMinimumBeforePriceService` | Java minimum broker commission branch bypasses price service. | Source-derived only. |
| `BrokerRegistrationCommissionPlanServiceTests.CreatePlan_UsesJavaPricesServiceForNonMinimumCommission` | Java broker commission central price formula application. | Source-derived only. |

## Remaining Risks

- Live `Influence`/siege ownership is not ported, so live price and tax values still cannot match Java world state.
- C# still uses string race values in staged callers, while Java uses `Race` enum.
- Several Java price consumers combine pricing with inventory mutation, packet send order, DAO writes, scheduled tasks, or recipient fanout.
- `SM_SELL_ITEM` has no direct C# equivalent found in this scan; serialization parity is unknown.
- Broker and player mail price paths are high conflict because they live in `GameServerConnection` and repository transactions.
- No Java runtime comparison was executed for this map.

## Next Recommended Unit

Prefer one of these:

1. Add an `SM_SELL_ITEM` packet plan and byte tests before any live sell-window routing.
2. Keep broker persistence/packet ordering staged and add live config/influence sourcing only after price fact plumbing exists.
3. Add a pure broker-registration commission calculator before touching the live broker registration handler.
4. Add a focused `SM_SELL_ITEM` packet plan and byte tests before any live sell-window routing.
