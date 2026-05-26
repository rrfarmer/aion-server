# Phase 6YS Completion - UOW-1157 Vendor Buy Modifier Config Surface

Date: May 26, 2026

## Unit of Work Summary

Continued Phase 6 trade-list parity by adding a narrow C# config surface for Java `PricesConfig.VENDOR_BUY_MODIFIER` and feeding it into the non-live `BUY` trade runtime fact seam.

The production `BUY` boundary still does not send `SmTradeList` or no-sell `SmSystemMessage` packets.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Configuration/GameServerOptions.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerOptionsTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionStorageExpansionDialogTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6YS-Completion.md`

## Implementation Notes

- Added `GameServerPriceOptions.VendorBuyModifier` with Java default `100`.
- `GameServerOptions.LoadFromJavaConfig` now reads `gameserver.prices.vendor.buymod`.
- `GameServerConnection.CreateNonLiveBuyDialogSelectPlan` now passes `_options.Prices.VendorBuyModifier` to `NpcDialogTradeRuntimeFactAdapterService`.
- Updated the existing boundary test so the default vendor modifier is now an injected config value rather than an adapter-only staged default.
- Added a boundary regression proving configured `125` with fixture `sell_price_rate="80"` produces packet plan `BuyPriceModifier = 100`.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "GameServerOptionsTests|NpcDialogTradeRuntimeFactAdapterServiceTests|GameServerConnectionStorageExpansionDialogTests" --nologo` passed 14 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` passed 2,166 tests.
- `dotnet test dotnetConversion/AionServer.slnx --nologo` passed 2,373 tests.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.configs.main.PricesConfig.VENDOR_BUY_MODIFIER` | `Aion.GameServer.Configuration.GameServerPriceOptions.VendorBuyModifier` | Configuration | Partial | Unit Tested | Partial Parity | C# now loads `gameserver.prices.vendor.buymod` with Java default `100`. Other `PricesConfig` fields remain unported; runtime Java artifact comparison is still missing. |
| `com.aionemu.gameserver.services.trade.PricesService.getVendorBuyModifier` | `NpcDialogTradeRuntimeFactAdapterService` fed by `GameServerOptions.Prices.VendorBuyModifier` | Service Dependency | Partial | Unit Tested | Partial Parity | Production non-live `BUY` plan now uses the configured option instead of adapter-only default. Live Java `PricesService` parity is not fully ported. |
| `com.aionemu.gameserver.services.DialogService.onDialogSelect` | `GameServerConnection.CreateNonLiveBuyDialogSelectPlan` | Service Boundary | Partial | Unit Tested | Partial Parity | Configured vendor modifier now reaches staged `BUY` packet planning; live sends and full Java controller/AI routing remain disabled. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_TRADELIST` | `SmTradeListPacketPlan.BuyPriceModifier` | Packet Plan | Partial | Unit Tested | Partial Parity | Boundary test proves configured `125 * sell_price_rate(80) / 100 = 100` in C# plan. Java runtime packet bytes remain unverified. |
| `com.aionemu.gameserver.model.team.legion.Legion.getLegionLevel` | `NpcDialogTradeRuntimeFactAdapterPlan.PlayerLegionLevel` | Model Dependency | Partial | Unit Tested | Needs Verification | Not changed by this unit. Live legion-level lookup remains missing and staged no-legion fallback `0` is still used. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerOptionsTests.LoadFromJavaConfig_ReadsCoreAndNetworkDefaults` | Unit / Config | `PricesConfig.VENDOR_BUY_MODIFIER` annotation default | Default C# options expose vendor buy modifier `100`. | Java source-reviewed default plus C# config test. | Does not cover all `PricesConfig` fields. |
| `GameServerOptionsTests.LoadFromJavaConfig_AppliesMyGsOverridesLast` | Unit / Config | Java config override semantics and `mygs.properties` precedence | `gameserver.prices.vendor.buymod = 125` overrides the default. | Deterministic C# loader test matching existing Java-config precedence pattern. | Not runtime-compared with Java loader. |
| `GameServerConnectionStorageExpansionDialogTests.HandleDialogSelectAsync_BuyTradeListUsesConfiguredVendorBuyModifierInNonLivePlan` | Unit / Socket Boundary Regression | `DialogService.onDialogSelect`; `PricesService.getVendorBuyModifier` | Configured vendor modifier reaches the non-live `BUY` packet plan and applies Java integer math with `sell_price_rate`. | Source-reviewed Java calculation plus production C# handler test. | No live send, no Java packet bytes, no live legion lookup. |

## Remaining Risks

- Other Java `PricesConfig` fields (`DEFAULT_PRICES`, `DEFAULT_MODIFIER`, `DEFAULT_TAXES`, `VENDOR_SELL_MODIFIER`) remain outside this unit.
- Java runtime golden-vector comparison is still absent.
- Live player legion-level lookup remains missing.
- `SM_TRADELIST` and no-sell system-message sends remain disabled.
- Limited-item mutation and cron reset lifecycle remain staged.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 narrow config option and its non-live `BUY` plan consumption path
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 4 blocked/partial categories: Java runtime packet vectors, live legion lookup, live send wiring, and limited-item mutation lifecycle
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Perform the read-only `SM_TRADE_IN_LIST` Java audit, or design the static-data limited-item corpus count comparison before expanding trade-in/live trade behavior.

Recommended starting points:
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_TRADE_IN_LIST.java`
- `game-server/src/com/aionemu/gameserver/services/DialogService.java`
- `game-server/src/com/aionemu/gameserver/model/templates/tradelist/TradeListTemplate.java`
- `dotnetConversion/src/Aion.GameServer/Services/NpcDialogTradeListFactAdapterService.cs`
- `docs/TradeList-Java-Golden-Vector-Design.md`

Keep live sends disabled until runtime price/legion facts, limited-item mutation, Java runtime golden vectors, and NPC AI/controller routing are ready.

# Next Work Options

## Recommended Sequential Task

- Task: read-only Java audit for `SM_TRADE_IN_LIST` and its `DialogService` route.
- Why: trade-list `BUY` now has staged packet, limited item, price config, and verification design surfaces; trade-in remains the next nearby trade packet branch.
- Files: docs only unless starting a non-live plan scaffold after the audit.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Static-data limited-item corpus count comparison design | docs/test planning | Low | Do not claim parity until Java-generated counts exist. |
| B | Java runtime artifact tooling sketch for trade-list vectors | docs only | Low | Can extend `TradeList-Java-Golden-Vector-Design.md`. |
| C | Live legion-level lookup discovery | read-only code search/docs | Low | Avoid touching `GameServerConnection` until a C# legion aggregate exists. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | `SM_TRADE_IN_LIST` Java audit | docs/progress/handoff | live packet sends |
| Agent A | Static-data limited-item corpus count comparison design | docs only | code files |

## Do Not Parallelize

- `GameServerConnection.HandleDialogSelectAsync`: shared production socket handler.
- Live packet send enablement.
- Progress/handoff docs: orchestrator-owned.
