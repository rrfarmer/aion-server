# Phase 6YF Completion - UOW-1144 Trade-List Production Boundary

Date: May 26, 2026

## Unit of Work Summary

Continued Phase 6 parity work by auditing Java `CM_DIALOG_SELECT` / `DialogService.onDialogSelect` trade-list routing and adding a production socket-boundary regression for the C# `BUY` dialog action.

The Java implementation is the source of truth:
- `CM_DIALOG_SELECT.runImpl` resolves the NPC target and delegates through `NpcController.onDialogSelect`.
- `DialogService.onDialogSelect` handles `BUY` by resolving `TradeListTemplate`, applying `PricesService.getVendorBuyModifier() * tradeModifier / 100`, filtering goods by legion level, and sending `SM_TRADELIST` or `STR_BUY_SELL_HE_DOES_NOT_SELL_ITEM`.

The C# port has staged planners and adapters for trade-list facts, but production `GameServerConnection.HandleDialogSelectAsync` does not yet compose those into live `BUY` routing. This unit documents and protects that disabled boundary with a regression test so partial packet sends do not appear before the full route is ready.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionStorageExpansionDialogTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6YF-Completion.md`

## Implementation Notes

- Added `HandleDialogSelectAsync_BuyTradeListRemainsDisabledAtSocketBoundaryUntilRoutingReady`.
- The test creates a `BUY`-capable NPC and invokes production `HandleDialogSelectAsync`.
- It asserts:
  - no packets are sent;
  - no response requester state is created.
- This is not parity with Java behavior. It is a readiness guard that records the current disabled production boundary until the Java-equivalent route can be implemented safely.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "GameServerConnectionStorageExpansionDialogTests|NpcDialogTradeListFactAdapterServiceTests|QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests|NpcDialogServiceSelectPlanServiceTests" --nologo` passed 42 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` passed 2,148 tests.
- `dotnet test dotnetConversion/AionServer.slnx --nologo` passed 2,355 tests.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleDialogSelectAsync` | Packet / Handler | Partial | Unit Tested | Partial Parity | Production C# handles some dialog actions, but `BUY` remains disabled at the socket boundary. The new regression locks the no-send behavior until full Java-equivalent routing is ready. |
| `com.aionemu.gameserver.services.DialogService.onDialogSelect` | `Aion.GameServer.Services.Dialogs.NpcDialogTradeListFactAdapterService` / `NpcDialogServiceSelectPlanService` | Service | Partial | Unit Tested | Partial Parity | Staged trade-list facts/plans exist, but production does not yet execute the Java controller-to-dialog-service route or send `SM_TRADELIST` / no-sell messages. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_TRADELIST` | No live production C# send in this unit | Packet | Not Started | No Tests | Needs Verification | Packet serialization and live send remain unsupported at the production socket. This unit only prevents accidental partial live sends before routing is ready. |
| `com.aionemu.gameserver.services.trade.PricesService.getVendorBuyModifier` | `Aion.GameServer.Services.Dialogs.NpcDialogTradeListFactAdapterInput.VendorBuyModifier` | Service Dependency | Partial | Unit Tested in staged planner | Needs Verification | Vendor modifier is still an explicit staged input, not a live `PricesService` lookup. Precision/order match with Java remains unverified at runtime. |
| `com.aionemu.gameserver.dataholders.TradeListData` | `Aion.GameServer.Dataholders.TradeListTable` | Static Data | Partial | Unit Tested in staged planner | Needs Verification | Static-data access exists for staged facts. Production `HandleDialogSelectAsync` does not yet consume it for `BUY`. |
| `com.aionemu.gameserver.dataholders.GoodsListData` | `Aion.GameServer.Dataholders.GoodsListTable` | Static Data | Partial | Unit Tested in staged planner | Needs Verification | Goods-list legion-level filtering is staged only. Live player legion lookup and production packet payload composition remain disabled. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionStorageExpansionDialogTests.HandleDialogSelectAsync_BuyTradeListRemainsDisabledAtSocketBoundaryUntilRoutingReady` | Unit / Socket Boundary Regression | `CM_DIALOG_SELECT`; `DialogService.onDialogSelect`; `SM_TRADELIST` | Production `BUY` dialog handling currently emits no packet and no response requester state, so staged trade-list planning cannot leak into the live socket prematurely. | Source-reviewed Java route confirms C# is still intentionally short of parity. The test documents the disabled boundary, not verified Java parity. | No Java runtime comparison, no live `SM_TRADELIST`, no live `PricesService`, no player-legion payload filtering, no NPC controller/AI path, no no-sell message. |

## Remaining Risks

- Production `BUY` routing remains disabled and is intentionally not Java-equivalent yet.
- `SM_TRADELIST` packet serialization/send is not implemented in the live socket path.
- No-sell system message routing, live price-service lookup, player legion lookup, goods payload filtering, NPC controller/AI dispatch, and known-list validation remain unimplemented or unverified.
- This regression protects the current no-op boundary; it must be replaced or inverted once the production trade-list route is implemented.
- Java runtime comparison, threading behavior, serialization byte layout, and live `DataManager` composition remain disabled/unverified.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 0 production artifacts; 1 socket-boundary regression test added
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: 6 blocked/partial categories: production `BUY` routing, `SM_TRADELIST` packet/send, live price service, player legion/goods payload, NPC controller/AI/known-list route, and Java runtime comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Continue trade-list parity by composing a non-live production input assembly feasibility test for `BUY` without sends, or begin the smallest live `SM_TRADELIST` packet prerequisite only after packet layout and price/legion data inputs are pinned to Java.

Recommended starting points:
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/Dialogs/NpcDialogTradeListFactAdapterService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/Dialogs/NpcDialogServiceSelectPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NpcDialogTradeListFactAdapterServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NpcDialogServiceSelectPlanServiceTests.cs`

Keep live `SM_TRADELIST` sends disabled until packet layout, price modifier inputs, player legion filtering, no-sell message behavior, NPC target/controller routing, and Java comparison evidence are available.
