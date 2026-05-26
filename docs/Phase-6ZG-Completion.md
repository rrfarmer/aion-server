# Phase 6ZG Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1171
Status: Phase 6 continues; trade-list/trade-in live sends remain disabled.

## Session Summary

UOW-1171 audited the vendor-buy modifier ownership path for trade-list `BUY` planning. The configured property path is already staged and tested in C#, but Java runtime packet vectors are still required before live sends.

Files changed:

- `dotnetConversion/src/Aion.GameServer/Services/NpcDialogTradeRuntimeFactAdapterService.cs`
- `docs/TradeList-VendorBuyModifier-LiveOwnership-Audit.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ZG-Completion.md`

## What Changed

- Added `TradeList-VendorBuyModifier-LiveOwnership-Audit.md`.
- Documented Java `PricesService.getVendorBuyModifier()` as a direct return of `PricesConfig.VENDOR_BUY_MODIFIER`.
- Documented the Java property key `gameserver.prices.vendor.buymod` and default `100`.
- Compared the C# path through `GameServerOptions.Prices.VendorBuyModifier`, `GameServerConnection.CreateNonLiveTradeDialogSelectPlan`, and `NpcDialogTradeRuntimeFactAdapterService`.
- Confirmed existing tests cover:
  - default vendor-buy modifier `100`;
  - `mygs.properties` override value `125`;
  - staged socket-boundary composition of `125 * 80 / 100 = 100`.
- Updated the adapter breadcrumb so it no longer implies the vendor-buy config path is unwired.

No live packet sends or production socket behavior changed.

## Validation

- `git diff --check` passed with existing line-ending warnings only.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "GameServerOptionsTests|NpcDialogTradeRuntimeFactAdapterServiceTests|GameServerConnectionStorageExpansionDialogTests" --nologo` passed 17 tests.

## Migration Parity Table - UOW-1171

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.trade.PricesService` | `Aion.GameServer.Configuration.GameServerPriceOptions` / `NpcDialogTradeRuntimeFactAdapterService` | Service / Config Fact | Partial | Unit Tested | Partial Parity | Trade-list vendor-buy modifier path is source-reviewed and staged-config tested. Broader prices service methods, influence/tax math, sell modifier, and service price calculations are outside this unit. |
| `com.aionemu.gameserver.configs.main.PricesConfig` | `Aion.GameServer.Configuration.GameServerOptions.LoadFromJavaConfig` / `GameServerPriceOptions` | Configuration | Partial | Unit Tested | Partial Parity | Same key `gameserver.prices.vendor.buymod`, same default `100`, and `mygs.properties` override are covered by existing tests. Other `PricesConfig` keys are not fully audited here. |
| `com.aionemu.gameserver.services.DialogService` | `Aion.GameServer.Network.Aion.GameServerConnection.CreateNonLiveTradeDialogSelectPlan` / `QuestDialogNpcTargetBranchInputAssemblyPlanService` | Service Boundary | Partial | Unit Tested | Partial Parity | Staged boundary passes configured vendor-buy modifier and performs Java integer math with `sell_price_rate`. Live send remains disabled. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_TRADELIST` | `Aion.GameServer.Services.SmTradeListPacketPlanService` / `SmTradeList` | Packet / Plan Consumer | Partial | Unit Tested | Needs Verification | Configured modifier reaches staged packet plan, but Java runtime vector for non-default modifier is still absent. |

Tests added or updated:

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None added; existing `GameServerOptionsTests.LoadFromJavaConfig_ReadsCoreAndNetworkDefaults` | Unit / Config | `PricesConfig.VENDOR_BUY_MODIFIER` default; `prices.properties` | Validates C# default vendor-buy modifier `100`. | Existing deterministic config test from Java property key/default. | Does not execute Java config loader. |
| None added; existing `GameServerOptionsTests.LoadFromJavaConfig_AppliesMyGsOverridesLast` | Unit / Config | Java config override order with `mygs.properties` | Validates `gameserver.prices.vendor.buymod = 125` override is loaded. | Existing deterministic config test from Java property key and override convention. | Does not execute Java config loader. |
| None added; existing `GameServerConnectionStorageExpansionDialogTests.HandleDialogSelectAsync_BuyTradeListUsesConfiguredVendorBuyModifierInNonLivePlan` | Unit / Socket Boundary | `DialogService.onDialogSelect BUY`; `PricesService.getVendorBuyModifier`; `SM_TRADELIST` modifier input | Validates configured `125` reaches staged runtime facts and composes `125 * 80 / 100 = 100` without sending packets. | Deterministic C# boundary regression from source-reviewed Java arithmetic. | No Java runtime vector and no live send. |

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 0 new runtime behavior; 1 adapter breadcrumb cleanup plus 1 vendor-buy ownership audit
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: 4 blocked/partial categories: Java non-default-price vector, C# vector verifier, broader PricesService parity, and live packet sends
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Java runtime vector for `buy-non-default-price` is still missing.
- Live `SM_TRADELIST`, `SM_TRADE_IN_LIST`, and no-sell sends remain disabled.
- Broader `PricesService` behavior for `SM_PRICES`, taxes, influence, service prices, buy prices, and sell rewards remains outside this trade-list audit.
- C# vector verifier artifacts are still missing.

## Next Recommended Unit of Work

Primary next unit:

- Continue Java vector generator skeleton feasibility.

Suggested scope:

- Inspect Java Maven/test layout for where a test-only parity runner could compile without production startup.
- Identify whether a generator skeleton can be added without touching production packet classes.
- Document exact files/packages and command shape if code generation is not yet safe.
- Keep generated artifacts absent until the generator can run reproducibly.

Safe parallel candidates:

- C# vector verifier artifact reader design, without generated Java artifacts.
- DB integration proof for `Player.LegionLevel` hydration if an integration database is available.
- Broader `PricesService` parity audit for `SM_PRICES`, taxes, influence, service prices, and sell rewards.

Do not start live packet send wiring until Java vector artifacts and C# verifier tests exist.
