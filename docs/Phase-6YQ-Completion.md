# Phase 6YQ Completion - UOW-1155 Trade Runtime Fact Seam

Date: May 26, 2026

## Unit of Work Summary

Continued Phase 6 trade-list parity by replacing inline staged `BUY` price and legion constants with an explicit non-live runtime fact adapter. The production `BUY` boundary still does not send `SmTradeList` or no-sell `SmSystemMessage` packets.

The Java implementation is the source of truth:
- `DialogService.onDialogSelect` `BUY` obtains `PricesService.getVendorBuyModifier()`.
- `PricesService.getVendorBuyModifier()` returns `PricesConfig.VENDOR_BUY_MODIFIER`.
- `DialogService.onDialogSelect` and `SM_TRADELIST` use `player.getLegion() == null ? 0 : player.getLegion().getLegionLevel()`.
- `SM_TRADELIST` receives `PricesService.getVendorBuyModifier() * tradeModifier / 100`.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/NpcDialogTradeRuntimeFactAdapterService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/QuestDialogNpcTargetBranchInputAssemblyPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NpcDialogTradeRuntimeFactAdapterServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionStorageExpansionDialogTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6YQ-Completion.md`

## Implementation Notes

- Added `NpcDialogTradeRuntimeFactAdapterService` to expose staged runtime facts for trade-list `BUY` planning.
- Default C# facts now explicitly model Java no-legion fallback as level `0` and staged vendor buy modifier as `100`.
- Source labels make clear when facts are staged defaults versus caller-injected runtime values.
- `QuestDialogNpcTargetBranchInputAssemblyPlan` now carries the runtime fact plan for boundary observability.
- `GameServerConnection.CreateNonLiveBuyDialogSelectPlan` now derives trade-list and limited-item adapter inputs from the runtime fact plan.
- The runtime fact adapter remains non-live even when injected values are supplied; real config and legion-service ownership is still pending.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "NpcDialogTradeRuntimeFactAdapterServiceTests|GameServerConnectionStorageExpansionDialogTests|QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests|NpcDialogTradeListFactAdapterServiceTests|NpcDialogLimitedItemFactAdapterServiceTests|SmTradeListPacketPlanServiceTests" --nologo` passed 42 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` passed 2,165 tests.
- `dotnet test dotnetConversion/AionServer.slnx --nologo` passed 2,372 tests.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.DialogService.onDialogSelect` | `Aion.GameServer.Network.Aion.GameServerConnection.CreateNonLiveBuyDialogSelectPlan` / `QuestDialogNpcTargetBranchInputAssemblyPlanService` | Service Boundary / Planner | Partial | Unit Tested | Partial Parity | Production `BUY` still does not send packets, but its staged plan now carries explicit runtime fact metadata for price and legion inputs. Live NPC AI/controller dispatch remains disabled. |
| `com.aionemu.gameserver.services.trade.PricesService.getVendorBuyModifier` | `Aion.GameServer.Services.NpcDialogTradeRuntimeFactAdapterService` | Service Adapter | Partial | Unit Tested | Needs Verification | C# records staged default `100` and source metadata. Real `PricesConfig.VENDOR_BUY_MODIFIER` runtime config plumbing is not yet wired; no Java runtime comparison exists. |
| `com.aionemu.gameserver.configs.main.PricesConfig.VENDOR_BUY_MODIFIER` | `NpcDialogTradeRuntimeFactAdapterPlan.VendorBuyModifier` | Configuration Dependency | Not Started | Unit Tested | Needs Verification | Newly discovered dependency. C# does not yet load or use Java-equivalent price config for this boundary; staged default is explicit and non-live. |
| `com.aionemu.gameserver.model.team.legion.Legion.getLegionLevel` | `NpcDialogTradeRuntimeFactAdapterPlan.PlayerLegionLevel` | Model Dependency | Partial | Unit Tested | Needs Verification | C# records Java no-legion fallback `0`. Live `player.getLegion()`/`LegionService` equivalent and real legion-level lookup are missing. |
| `com.aionemu.gameserver.model.gameobjects.player.Player.getLegion` | `Aion.GameServer.Model.GameObjects.Player.LegionId` plus `NpcDialogTradeRuntimeFactAdapterInput.PlayerLegionId` | Model Dependency | Partial | Unit Tested | Needs Verification | C# carries `LegionId`, but not a live `Legion` object or level. This is an intentional staged gap, not verified parity. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_TRADELIST` | `Aion.GameServer.Services.SmTradeListPacketPlan` / `SmTradeList` | Packet Plan / Packet | Partial | Unit Tested | Partial Parity | Packet plan still receives price modifier as `VendorBuyModifier * sellPriceRate / 100` and filters by staged legion level. Runtime facts are now visible, but packet sends and Java golden vectors remain missing. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `NpcDialogTradeRuntimeFactAdapterServiceTests.CreatePlan_UsesJavaNoLegionAndDefaultVendorModifierAsExplicitStagedFacts` | Unit / Adapter | `DialogService.onDialogSelect`; `PricesService.getVendorBuyModifier`; `Legion.getLegionLevel` | Adapter records staged no-legion level `0`, vendor modifier `100`, Java breadcrumbs, and non-live source labels. | Source-reviewed Java branch plus deterministic C# adapter test. | No live `PricesConfig` lookup, no live `LegionService`, no Java runtime capture. |
| `NpcDialogTradeRuntimeFactAdapterServiceTests.CreatePlan_CanCarryInjectedRuntimeFactsWithoutMarkingLiveLookupComplete` | Unit / Adapter | Same Java sources | Injected values flow into trade-list and limited-item fact inputs without claiming live lookup completion. | Deterministic C# seam test that keeps `IsLive=false`. | Injected values are not sourced from production runtime services. |
| `GameServerConnectionStorageExpansionDialogTests.HandleDialogSelectAsync_BuyTradeListRemainsDisabledAtSocketBoundaryUntilRoutingReady` | Unit / Socket Boundary Regression | `CM_DIALOG_SELECT`; `DialogService.onDialogSelect BUY`; `SM_TRADELIST` | Production `BUY` observes explicit runtime fact metadata and still produces a non-live ready packet plan without sending. | Source-reviewed Java route plus production C# handler test. | No live send, no live price/legion lookup, no Java packet bytes. |

## Remaining Risks

- Live `PricesConfig.VENDOR_BUY_MODIFIER` loading/use remains unimplemented for the trade-list boundary.
- Live `Player.getLegion()`/`Legion.getLegionLevel()` equivalent remains missing; C# only carries `LegionId` at this boundary.
- `SM_TRADELIST` still is not sent by `GameServerConnection`.
- Java runtime golden-vector comparison is still absent, so packet and fact parity are source-reviewed only.
- Limited-item mutation, cron reset scheduling, and live buy-count/sell-limit updates remain missing.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live runtime fact adapter seam plus production `BUY` composition of that seam
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: 5 blocked/partial categories: live price config lookup, live legion lookup, Java runtime packet vectors, live packet sends, and limited-item mutation lifecycle
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Draft Java runtime golden-vector design/capture notes for `SM_TRADELIST` and the no-sell `SM_SYSTEM_MESSAGE`, or implement a narrow non-live `PricesConfig.VENDOR_BUY_MODIFIER` config surface before live `BUY` send enablement.

Recommended starting points:
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_TRADELIST.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
- `game-server/src/com/aionemu/gameserver/services/trade/PricesService.java`
- `game-server/src/com/aionemu/gameserver/configs/main/PricesConfig.java`
- `dotnetConversion/src/Aion.GameServer/Services/NpcDialogTradeRuntimeFactAdapterService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionStorageExpansionDialogTests.cs`

Keep live sends disabled until runtime price/legion facts, limited-item mutation, Java runtime golden vectors, and NPC AI/controller routing are ready.

# Next Work Options

## Recommended Sequential Task

- Task: draft Java runtime golden-vector design/capture notes for trade-list and no-sell packet artifacts.
- Why: `SmTradeList` and the no-sell helper are source-derived but not runtime-compared; this is the main reason parity cannot be marked verified.
- Files: docs only, unless adding a non-live fixture skeleton.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Narrow `PricesConfig.VENDOR_BUY_MODIFIER` config surface | `GameServerOptions.cs`, focused tests, runtime fact adapter | Medium | Avoid live send wiring; keep default `100` parity explicit. |
| B | Read-only `SM_TRADE_IN_LIST` Java audit | docs only | Low | Safe analysis for a later trade-in slice. |
| C | Static-data limited-item corpus count comparison design | docs/test planning | Low | Do not claim parity until Java-generated counts exist. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Java runtime golden-vector design notes | docs only | live packet sends |
| Agent A | Read-only `SM_TRADE_IN_LIST` Java audit | docs only | code files |

## Do Not Parallelize

- `GameServerConnection.HandleDialogSelectAsync`: shared production socket handler.
- Live packet send enablement.
- Progress/handoff docs: orchestrator-owned.
