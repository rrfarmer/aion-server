# Trade List Vendor-Buy Modifier Live Ownership Audit

Date: May 26, 2026
Unit of Work: UOW-1171

## Purpose

This audit verifies the ownership path for the vendor-buy modifier used by Java `DialogService.onDialogSelect` when composing `SM_TRADELIST`.

Java remains the source of truth. This audit does not enable live `SM_TRADELIST`, `SM_TRADE_IN_LIST`, or no-sell packet sends.

## Java Source Breadcrumbs

- `game-server/src/com/aionemu/gameserver/services/DialogService.java`
- `game-server/src/com/aionemu/gameserver/services/trade/PricesService.java`
- `game-server/src/com/aionemu/gameserver/configs/main/PricesConfig.java`
- `game-server/config/main/prices.properties`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_TRADELIST.java`

## Java Behavior

`DialogService.onDialogSelect` computes the buy price modifier for `BUY` as:

```java
PricesService.getVendorBuyModifier() * tradeModifier / 100
```

where `tradeModifier` is:

```java
tradeListTemplate.getSellPriceRate()
```

`PricesService.getVendorBuyModifier()` is a direct static return:

```java
return PricesConfig.VENDOR_BUY_MODIFIER;
```

`PricesConfig.VENDOR_BUY_MODIFIER` is bound from:

```java
@Property(key = "gameserver.prices.vendor.buymod", defaultValue = "100")
public static int VENDOR_BUY_MODIFIER;
```

The checked-in Java default file has:

```properties
gameserver.prices.vendor.buymod = 100
```

No influence-rate calculation, tax calculation, floating point precision, date/time handling, reflection behavior, or threading behavior is involved in this `SM_TRADELIST` modifier. The trade-list branch uses Java integer arithmetic after reading the configured integer.

## Current C# State

Relevant C# files:

- `dotnetConversion/src/Aion.GameServer/Configuration/GameServerOptions.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/NpcDialogTradeRuntimeFactAdapterService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/QuestDialogNpcTargetBranchInputAssemblyPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerOptionsTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionStorageExpansionDialogTests.cs`

Current observations:

- `GameServerOptions.LoadFromJavaConfig` reads `gameserver.prices.vendor.buymod` into `GameServerPriceOptions.VendorBuyModifier`, defaulting to `100`.
- `GameServerOptionsTests.LoadFromJavaConfig_ReadsCoreAndNetworkDefaults` validates the default value `100`.
- `GameServerOptionsTests.LoadFromJavaConfig_AppliesMyGsOverridesLast` validates a `mygs.properties` override of `gameserver.prices.vendor.buymod = 125`.
- `GameServerConnection.CreateNonLiveTradeDialogSelectPlan` passes `_options.Prices.VendorBuyModifier` into `NpcDialogTradeRuntimeFactAdapterInput`.
- `GameServerConnectionStorageExpansionDialogTests.HandleDialogSelectAsync_BuyTradeListUsesConfiguredVendorBuyModifierInNonLivePlan` validates the configured `125` reaches the non-live trade runtime facts and produces `125 * 80 / 100 = 100` for a source fixture with `sell_price_rate="80"`.
- `NpcDialogTradeRuntimeFactAdapterPlan.IsLive` remains `false`, so the value is usable for staged planning but does not imply live send readiness.

## Parity Assessment

The C# config ownership path is sufficient for the currently staged trade-list planner:

1. Same property key: `gameserver.prices.vendor.buymod`.
2. Same default: `100`.
3. Same Java-style override source: `mygs.properties` loaded after default config directories.
4. Same integer arithmetic at the trade-list plan boundary.

This is Partial Parity rather than Verified Parity because:

- No Java runtime vector has captured a non-default vendor-buy modifier in a real `SM_TRADELIST`.
- Live `SM_TRADELIST` sends remain disabled.
- The C# `SM_PRICES`, taxes, global prices, and influence-rate surfaces are broader `PricesService` behavior and are not verified by this trade-list audit.
- Runtime reloading of Java static config is not modeled; both current Java and C# startup config paths are effectively startup-loaded for this use.

## Unsupported Or Unverified Behavior

- `PricesService.getGlobalPrices`, `getGlobalPricesModifier`, `getTaxes`, `getPriceForService`, `getBuyPrice`, and `getSellReward` are not verified by this audit.
- `SM_SELL_ITEM` and sell modifier behavior are outside this unit.
- Race/influence based price behavior is outside this unit.
- Java runtime packet vectors for non-default vendor-buy modifier remain missing.
- C# live sends remain disabled and must stay disabled until vector/verifier gates are satisfied.

## Readiness Gates Before Live Send Wiring

Before live `BUY` send wiring, require:

1. Java runtime vector for `buy-non-default-price`.
2. C# verifier comparing `buyPriceModifier` and `SM_TRADELIST` bytes for that vector.
3. Existing configured modifier tests remain green.
4. No mismatch between plan-level integer arithmetic and Java packet constructor inputs.

## Next Recommended Unit

Continue Java vector generator skeleton feasibility, or design the C# vector verifier artifact reader while waiting for generated Java artifacts.
