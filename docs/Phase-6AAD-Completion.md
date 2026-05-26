# Phase 6AAD Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1194
Status: Phase 6 continues; ordinary teleporter transportation price/decrement planning is staged, but live teleport routing and bind-point distance pricing remain incomplete.

## Session Summary

UOW-1194 audited the paid teleport price surface and implemented a pure planner for Java `TeleportService.checkKinahForTransportation`. The planner calculates ordinary teleporter transportation cost, applies the HiPass one-Kinah override, and stages the Kinah item update without executing live movement or inventory mutation.

Files changed:

- `dotnetConversion/src/Aion.GameServer/Services/TeleportTransportationPricePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/TeleportTransportationPricePlanServiceTests.cs`
- `docs/Phase-6-PricesService-Consumer-Map.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6AAD-Completion.md`

## What Changed

- Added `TeleportTransportationPricePlanService` and `TeleportTransportationPricePlan`.
- Ported ordinary Java transportation pricing:
  - HiPass effect active -> price `1`;
  - otherwise `PricesService.GetPriceForService(locationBasePrice, player.Race, ...)`;
  - sufficient Kinah -> staged Kinah decrement matching Java `ItemUpdateType.DEC_KINAH_FLY`;
  - insufficient Kinah -> `NotEnoughKinah` with required transportation price.
- Documented discovered but unported `BindPointTeleportService.calculateTeleportationPrice` as a separate follow-up because it uses distance and client-sent-price reconciliation rather than `PricesService`.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "TeleportTransportationPricePlanServiceTests|PricesServiceTests" --nologo` passed 6 tests.
- No Java runtime comparison was run, so parity remains `Needs Verification`.

## Migration Parity Table - UOW-1194

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.teleport.TeleportService` | `Aion.GameServer.Services.TeleportTransportationPricePlanService` / `Aion.GameServer.Services.PlayerTeleportService` | Service / Movement | Partial | Unit Tested | Needs Verification | Ordinary `checkKinahForTransportation` price/decrement planning is ported. Live teleporter validation, siege/quest guards, flypath validation, movement/spawn, packets, and task ordering remain outside this unit. |
| `com.aionemu.gameserver.services.trade.PricesService` | `Aion.GameServer.Services.PricesService` | Service / Utility Dependency | Partial | Unit Tested | Needs Verification | Ordinary teleport transportation now has a staged central price formula consumer. No live `Influence` source or Java runtime comparison was used. |
| `com.aionemu.gameserver.model.gameobjects.player.Player` | `Aion.GameServer.Model.GameObjects.Player` | Model / Runtime Fact Source | Partial | Unit Tested | Needs Verification | Planner reads race string and inventory item snapshots. Java uses `Race` enum and live inventory mutation. Threading/live object identity remain unverified. |
| `com.aionemu.gameserver.model.gameobjects.player.Storage` | `Aion.GameServer.Model.GameObjects.InventoryItem` | Model / Inventory State | Partial | Unit Tested | Needs Verification | C# returns a copied Kinah item update instead of executing `tryDecreaseKinah(ItemUpdateType.DEC_KINAH_FLY)`. Inventory update packet type is documented but not emitted here. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService` | future C# bind-point teleport price planner | Service / Movement | Not Started | No Tests | Unknown | Discovered dependency with separate distance/client-price reconciliation. Intentionally not ported in this ordinary teleporter-price unit. |

Tests added or updated:

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `TeleportTransportationPricePlanServiceTests.CreatePlan_UsesJavaPricesServiceAndStagesFlyKinahDecrease` | Unit | Java `TeleportService.checkKinahForTransportation`; Java `PricesService.getPriceForService` | Validates central price formula and staged Kinah decrement for ordinary teleport transportation. | Deterministic source-derived expectation. | No live movement or Java runtime execution. |
| `TeleportTransportationPricePlanServiceTests.CreatePlan_UsesHiPassOneKinahOverrideBeforePricesService` | Unit | Java HiPass branch in `checkKinahForTransportation` | Validates HiPass forces price `1` before price-service adjustment. | Deterministic source-derived expectation. | Effect detection itself is passed as a boolean fact. |
| `TeleportTransportationPricePlanServiceTests.CreatePlan_ReportsNotEnoughKinahWithRequiredPrice` | Unit | Java `STR_MSG_NOT_ENOUGH_KINA(transportationPrice)` branch | Validates insufficient Kinah returns the required transportation price and no staged Kinah update. | Deterministic source-derived expectation. | Concrete system-message packet is not emitted. |

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 ordinary teleport transportation price planner plus 3 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: 3 grouped categories: live teleport routing, live influence/effect sourcing, and bind-point teleport distance pricing
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Live teleporter routing does not consume this planner yet.
- Live `Influence`/config sourcing remains absent for production teleport prices.
- HiPass detection is an injected boolean, not a live effect-controller query.
- Movement/spawn/flypath validation, delayed teleport task ordering, and packet sends remain broad teleport risks.
- Bind-point teleport price calculation is a separate Java service and remains unported.
- No Java runtime comparison was executed; no reflection or serialization behavior changed in this unit.

## Next Recommended Unit of Work

Primary next unit:

- Add a pure bind-point teleport distance-price planner from Java `BindPointTeleportService.calculateTeleportationPrice`.

Fallback units:

- Continue `SM_SELL_ITEM` live-readiness by composing non-live dialog facts into `SmSellItemPacketPlan`.
- Continue broker live-readiness by isolating packet ordering and persistence rollback behavior.

Defer live teleport price deduction until movement routing, packet order, effect sourcing, and inventory mutation are separately tested.
