# Phase 6YW Completion - UOW-1161 Trade-In Socket Boundary Observation

Date: May 26, 2026

## Unit of Work Summary

Continued Phase 6 trade-list parity by surfacing the staged `TRADE_IN` packet plan from `GameServerConnection.HandleDialogSelectAsync`.

Production packet sends remain disabled. This unit only adds non-sending observation through the existing dialog-plan observer.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmDialogSelect.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionStorageExpansionDialogTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6YW-Completion.md`

## Implementation Notes

- Added `CmDialogSelect.TradeIn = 78`.
- Routed both `BUY` and `TRADE_IN` through the renamed private `CreateNonLiveTradeDialogSelectPlan` helper.
- Kept the route non-sending: observed plans are handed to `_dialogSelectPlanObserver`, and no `SmTradeInList` packet is sent.
- Scoped limited-item fact inputs to `BUY`, since Java `TRADE_IN` only checks `TradeListData.getTradeInListTemplate` and sends `SM_TRADE_IN_LIST`.
- Extended the socket-boundary fixture with a `trade_in_list_template` for NPC `205315`.
- Added a regression that proves `TRADE_IN` produces a ready non-live `SmTradeInListPacketPlan`, does not create an `SM_TRADELIST` plan, and emits no socket packets.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "GameServerConnectionStorageExpansionDialogTests|QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests|NpcDialogServiceSelectPlanServiceTests|SmTradeInListPacketPlanServiceTests" --nologo` passed 56 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` passed 2,179 tests.
- `dotnet test dotnetConversion/AionServer.slnx --nologo` passed 2,386 tests.
- Note: an initial full-suite command used `dotnetConversion/AionServer.sln`, which does not exist in this repo. The correct `.slnx` command passed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` | `Aion.GameServer.Network.Aion.ClientPackets.CmDialogSelect` / `GameServerConnection.HandleDialogSelectAsync` | Client Packet / Socket Boundary | Partial | Unit Tested | Partial Parity | C# now exposes action `78` and observes staged `TRADE_IN` plans at the socket boundary. Other Java dialog actions and live controller dispatch remain incomplete. |
| `com.aionemu.gameserver.services.DialogService.onDialogSelect` `TRADE_IN` | `GameServerConnection.CreateNonLiveTradeDialogSelectPlan` / `QuestDialogNpcTargetBranchInputAssemblyPlanService` | Service Boundary / Planner | Partial | Unit Tested | Partial Parity | Production handler can now produce the non-live trade-in descriptor path. Live `sendPacket(new SM_TRADE_IN_LIST(...))`, NPC AI dispatch, and unsupported/no-sell socket behavior remain disabled. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_TRADE_IN_LIST` | `SmTradeInListPacketPlan` observed through `GameServerConnection` | Packet Plan / Packet Dependency | Partial | Unit Tested | Partial Parity | Ready packet plan is now observable from the production socket boundary. Serializer exists, but no socket send or Java runtime golden payload comparison exists. |
| `com.aionemu.gameserver.dataholders.TradeListData.getTradeInListTemplate` | `TradeListTable.GetTradeInListTemplate` via fixture-loaded `StaticData` | Static Data Repository | Partial | Unit Tested | Partial Parity | Fixture verifies static-data trade-in lookup can feed the boundary plan. Full Java corpus count comparison remains unverified. |
| `com.aionemu.gameserver.model.templates.tradelist.TradeListTemplate` | `TradeListTemplateSummary` consumed by `SmTradeInListPacketPlanService` | Static Data DTO | Partial | Unit Tested | Partial Parity | Boundary test covers NPC id, Java trade NPC type index mapping, fixed modifier `100`, and raw tab ids. Serialization remains source-derived rather than Java-runtime verified. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionStorageExpansionDialogTests.HandleDialogSelectAsync_TradeInListRemainsDisabledAtSocketBoundaryUntilRoutingReady` | Unit / Socket Boundary | `CM_DIALOG_SELECT.runImpl`; `DialogService.onDialogSelect` `TRADE_IN`; `TradeListData.getTradeInListTemplate`; `SM_TRADE_IN_LIST.writeImpl` | `TRADE_IN` action `78` reaches staged dialog-service planning, observes a ready `SmTradeInListPacketPlan`, omits `SM_TRADELIST` planning, and sends no packets. | Source-reviewed Java route plus deterministic C# boundary regression over XML-loaded static data. | No Java runtime socket/payload vector; live send path remains disabled. |

## Remaining Risks

- `SmTradeInList` is still not sent by `GameServerConnection`.
- Java runtime golden-vector comparison is absent for trade-in list payloads.
- `TRADE_IN` no-list fallback is planner-covered but not separately exercised at the socket boundary in this unit.
- Trade-in purchase execution/AP formulas remain separate from this list-opening path.
- Live NPC AI/controller routing, real limited-item/runtime facts, and production no-sell messaging remain prerequisites before enabling sends.
- Static-data corpus parity for trade-in and limited goods counts remains unverified.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 production non-sending trade-in observation path
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 5 blocked/partial categories: live send wiring, Java runtime vectors, production no-sell socket behavior, trade-in purchase execution, and broader NPC AI/controller routing
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Design or implement a static-data corpus comparison for trade-list/trade-in/limited-item counts, keeping it source-data focused and avoiding live send enablement until Java runtime vectors or controller prerequisites exist.

Recommended starting points:
- `dotnetConversion/src/Aion.GameServer/Dataholders/StaticData.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/TradeListTable.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/GoodsListTable.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/StaticDataLoadingTests.cs`
- `docs/TradeList-Java-Golden-Vector-Design.md`
- Java `game-server/src/com/aionemu/gameserver/dataholders/TradeListData.java`
- Java `game-server/src/com/aionemu/gameserver/model/templates/tradelist/TradeListTemplate.java`

Keep live `SM_TRADELIST` and `SM_TRADE_IN_LIST` sends disabled until runtime facts, Java runtime golden vectors, and NPC AI/controller routing are ready.

# Next Work Options

## Recommended Sequential Task

- Task: static-data corpus comparison for trade-list, trade-in, goods-list, and limited-item counts.
- Why: production observation exists for BUY and TRADE_IN, but full corpus parity remains unverified and should be measured before live sends.
- Files: dataholder tests or docs first, plus progress/handoff docs.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Java runtime artifact tooling sketch for trade-list vectors | docs only | Low | Extend the existing vector-design note without claiming runtime verification. |
| B | `TRADE_IN` no-list socket-boundary fallback regression | `GameServerConnectionStorageExpansionDialogTests.cs` | Low | Keep non-sending; verify no packet plan and no send. |
| C | Live legion-level lookup discovery | read-only code search/docs | Low | Avoid production wiring until a C# legion aggregate exists. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Static-data corpus comparison design or implementation | dataholder tests/docs/progress/handoff | live socket sends |
| Agent A | Java runtime vector tooling sketch | docs only | code files |

## Do Not Parallelize

- `GameServerConnection.HandleDialogSelectAsync`: shared production socket handler.
- Live packet send enablement.
- Progress/handoff docs: orchestrator-owned.
