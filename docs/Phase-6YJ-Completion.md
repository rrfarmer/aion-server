# Phase 6YJ Completion - UOW-1148 Trade-List Production Planner Boundary

Date: May 26, 2026

## Unit of Work Summary

Continued Phase 6 trade-list parity by replacing the production `BUY` pure no-op boundary with a non-sending staged planner invocation. `GameServerConnection.HandleDialogSelectAsync` still does not send `SM_TRADELIST` or no-sell packets, but it now builds the staged `CM_DIALOG_SELECT` / `NpcController` / `DialogService` plan for observable readiness.

The Java implementation is the source of truth:
- `CM_DIALOG_SELECT.runImpl` resolves the target and reaches `NpcController.onDialogSelect`.
- `NpcController.onDialogSelect` checks talk range, gives AI first chance, then falls back to `DialogService.onDialogSelect`.
- `DialogService.onDialogSelect` `BUY` sends `SM_TRADELIST` when a trade list exists and at least one goods list is allowed by legion level; otherwise it sends `STR_BUY_SELL_HE_DOES_NOT_SELL_ITEM`.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmDialogSelect.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionStorageExpansionDialogTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6YJ-Completion.md`

## Implementation Notes

- Added `CmDialogSelect.Buy = 2`.
- Added optional `dialogSelectPlanObserver` test hook to `GameServerConnection`.
- Added `CreateNonLiveBuyDialogSelectPlan` inside `GameServerConnection`.
- The production boundary now:
  - resolves the target NPC from the world;
  - respects the optional known-NPC predicate;
  - computes Java-style NPC talk range;
  - supplies static trade-list/goods-list facts when available;
  - derives staged `Npc.canSell/canBuy` flags from target template action support;
  - invokes the observer with a non-live staged plan;
  - returns without sending any packet.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "GameServerConnectionStorageExpansionDialogTests|QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests|SmTradeListPacketPlanServiceTests" --nologo` passed 29 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` passed 2,156 tests.
- `dotnet test dotnetConversion/AionServer.slnx --nologo` passed 2,363 tests.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleDialogSelectAsync` | Packet / Handler | Partial | Unit Tested | Partial Parity | Production `BUY` reaches the staged non-live planner and remains non-sending. Java live controller dispatch, packet sends, audit paths, and known-list object model are still partial. |
| `com.aionemu.gameserver.controllers.NpcController.onDialogSelect` | `GameServerConnection.CreateNonLiveBuyDialogSelectPlan` / `NpcDialogControllerDispatchPlanService` | Controller Planner | Partial | Unit Tested | Partial Parity | C# derives talk-range and AI-not-handled facts to reach staged `DialogServiceFallback`. Live NPC AI dispatch is not executed. |
| `com.aionemu.gameserver.services.DialogService.onDialogSelect` | `QuestDialogNpcTargetBranchInputAssemblyPlanService` through production boundary | Service Planner | Partial | Unit Tested | Partial Parity | Production boundary now observes staged `BUY` success with trade-list facts. Live `SM_TRADELIST` and no-sell system message sends remain disabled. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_TRADELIST` | `Aion.GameServer.Services.SmTradeListPacketPlan` observed from production boundary | Packet Plan | Partial | Unit Tested | Partial Parity | Production boundary can assemble packet-shape metadata but still does not serialize or send a packet. Opcode `253`, binary layout, limited items, and Java byte comparison remain missing. |
| `com.aionemu.gameserver.services.trade.PricesService.getVendorBuyModifier` | `NpcDialogTradeListFactAdapterInput.VendorBuyModifier = 100` at production boundary | Service Dependency | Partial | Unit Tested as explicit default | Needs Verification | C# uses a staged fixed default because live `PricesService`/config plumbing is not wired. Precision/order and config parity remain unverified. |
| `com.aionemu.gameserver.model.team.legion.Legion.getLegionLevel` | `NpcDialogTradeListFactAdapterInput.PlayerLegionLevel = 0` at production boundary | Runtime Fact Dependency | Not Started | Unit Tested as explicit default | Needs Verification | C# production boundary assumes no legion level for now. Live player legion-level lookup remains missing. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionStorageExpansionDialogTests.HandleDialogSelectAsync_BuyTradeListRemainsDisabledAtSocketBoundaryUntilRoutingReady` | Unit / Socket Boundary Regression | `CM_DIALOG_SELECT`; `NpcController.onDialogSelect`; `DialogService.onDialogSelect` `BUY`; `SM_TRADELIST` | Production `BUY` builds a non-live staged plan with `DialogServiceFallback`, `BuyTradeList`, and ready packet-plan metadata while sending no packets. | Source-reviewed Java route plus production C# handler test. | No live packet send, no live no-sell branch, no live AI dispatch, no Java runtime/byte comparison. |

## Remaining Risks

- Production `BUY` still returns before live `SM_TRADELIST` or no-sell system message sends.
- Live `PricesService.getVendorBuyModifier`, player legion level, `LimitedItemTradeService`, `Npc.canSell/canBuy` runtime methods, NPC AI dispatch, and Java known-list semantics remain partial or missing.
- Packet serialization/opcode `253` and Java byte comparison remain blocked.
- The observer is test-only plumbing and must not be treated as user-visible parity.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 production non-sending planner boundary plus 1 client-packet action constant
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: 7 blocked/partial categories: live packet send, no-sell send, live AI dispatch, price service/config, player legion level, limited-item lookup, and byte comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a production non-sending no-sell `BUY` boundary regression using missing or legion-restricted goods, then begin `SmTradeList` binary packet serialization only after Java byte expectations are pinned down.

Recommended starting points:
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionStorageExpansionDialogTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/SmTradeListPacketPlanService.cs`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_TRADELIST.java`
- `game-server/src/com/aionemu/gameserver/services/DialogService.java`

Keep live `SM_TRADELIST` sends disabled until byte serialization, opcode `253`, Java comparison evidence, runtime limited items, player legion lookup, `Npc.canSell/canBuy`, no-sell fallback, and NPC AI/controller routing are ready.

# Next Work Options

## Recommended Sequential Task

- Task: add a production non-sending no-sell `BUY` boundary regression.
- Why: the success branch is now observable; the Java no-sell branch needs equivalent staged evidence before live sends.
- Files: `GameServerConnectionStorageExpansionDialogTests.cs`, possibly small fixture helpers, docs.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Read-only `SM_TRADE_IN_LIST` Java audit | none | Low | Safe analysis for a later trade-in slice. |
| B | Static-data trade NPC type validation test | focused static-data test file | Medium | Avoid shared loader edits unless required. |
| C | Limited-item dependency mapping | none | Low | Read-only inspection of `LimitedItemTradeService` and related model classes. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | No-sell production boundary regression | `GameServerConnectionStorageExpansionDialogTests.cs`, docs | live packet sends, `StaticData.cs` |
| Agent A | Read-only limited-item dependency map | read-only Java files | all writes |

## Do Not Parallelize

- `GameServerConnection.HandleDialogSelectAsync`: shared production socket handler.
- `StaticData.cs`: shared XML loader.
- Progress/handoff docs: orchestrator-owned.
