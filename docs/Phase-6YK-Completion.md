# Phase 6YK Completion - UOW-1149 Trade-List No-Sell Boundary Regression

Date: May 26, 2026

## Unit of Work Summary

Continued Phase 6 trade-list parity by adding production socket-boundary regression coverage for Java's `BUY` no-sell branch. The C# boundary remains non-sending, but it now proves that a BUY-capable NPC without a trade-list template produces staged `BuyUnavailable` intent rather than a trade-list packet plan.

The Java implementation is the source of truth:
- `DialogService.onDialogSelect` checks `DataManager.TRADE_LIST_DATA.getTradeListTemplate(npc.getNpcId())`.
- If the template is missing, Java sends `SM_SYSTEM_MESSAGE.STR_BUY_SELL_HE_DOES_NOT_SELL_ITEM(npc.getObjectTemplate().getL10n())`.
- If a template exists but all goods are missing or legion-restricted, Java sends the same no-sell message.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionStorageExpansionDialogTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6YK-Completion.md`

## Implementation Notes

- Added `HandleDialogSelectAsync_BuyNoTradeListPlansNoSellMessageWithoutSending`.
- The test creates a BUY-capable NPC with template id `203061`, which is not present in the fixture `npc_trade_list`.
- The production `HandleDialogSelectAsync` boundary still sends no packets and registers no response requester.
- The observed staged plan reaches:
  - `QuestDialogNpcTargetBranchStatus.DispatchController`;
  - `NpcDialogControllerDispatchStatus.DialogServiceFallback`;
  - `NpcDialogServiceSelectStatus.BuyUnavailable`;
  - `NpcDialogServiceDescriptorKind.SystemMessageDoesNotSellItem`.
- No live `SM_SYSTEM_MESSAGE` send, localization payload, opcode, or byte serialization was enabled.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "GameServerConnectionStorageExpansionDialogTests" --nologo` passed 5 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` passed 2,157 tests.
- `dotnet test dotnetConversion/AionServer.slnx --nologo` passed 2,364 tests.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleDialogSelectAsync` | Packet / Handler | Partial | Unit Tested | Partial Parity | Production `BUY` boundary now has regression coverage for success and missing-trade-list no-sell staged outcomes. It still does not execute live Java controller dispatch or send packets. |
| `com.aionemu.gameserver.controllers.NpcController.onDialogSelect` | `GameServerConnection.CreateNonLiveBuyDialogSelectPlan` / `NpcDialogControllerDispatchPlanService` | Controller Planner | Partial | Unit Tested | Partial Parity | The no-sell regression still flows through staged talk-range and AI-not-handled facts to `DialogServiceFallback`. Live NPC AI dispatch remains disabled. |
| `com.aionemu.gameserver.services.DialogService.onDialogSelect` | `NpcDialogServiceSelectPlanService` through production boundary | Service Planner | Partial | Unit Tested | Partial Parity | Missing trade-list branch now produces a non-live `SystemMessageDoesNotSellItem` descriptor. Live `SM_SYSTEM_MESSAGE.STR_BUY_SELL_HE_DOES_NOT_SELL_ITEM` serialization/send is not implemented. |
| `com.aionemu.gameserver.dataholders.TradeListData` | `Aion.GameServer.Dataholders.TradeListTable` through `NpcDialogTradeListFactAdapterService` | Static Data Dependency | Partial | Unit Tested | Needs Verification | The production boundary uses loaded static trade lists to detect absent templates. Static-data loader coverage exists, but this unit does not compare against Java dataholder runtime behavior. |
| `com.aionemu.gameserver.dataholders.GoodsListData` | `Aion.GameServer.Dataholders.GoodsListTable` through `NpcDialogTradeListFactAdapterService` | Static Data Dependency | Partial | Unit Tested | Needs Verification | Goods-list presence is part of the adapter path. This unit covers the missing trade-list branch only; restricted/missing goods-list no-sell production cases still need boundary coverage. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_BUY_SELL_HE_DOES_NOT_SELL_ITEM` | `NpcDialogServiceDescriptorKind.SystemMessageDoesNotSellItem` | Packet Descriptor | Partial | Unit Tested | Needs Verification | C# records non-live intent only. Message id, localization parameter, binary packet bytes, and live send ordering are not verified. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionStorageExpansionDialogTests.HandleDialogSelectAsync_BuyNoTradeListPlansNoSellMessageWithoutSending` | Unit / Socket Boundary Regression | `DialogService.onDialogSelect` `BUY` missing `TradeListTemplate` branch | Production `BUY` observes a staged `BuyUnavailable` plan with a `SystemMessageDoesNotSellItem` descriptor and no `SmTradeListPacketPlan`, while sending no packets. | Source-reviewed Java branch plus production C# handler test. | No live system-message send, no message byte comparison, no restricted-goods branch, no live AI dispatch. |

## Remaining Risks

- Live no-sell system-message send remains disabled; this unit only verifies non-live descriptor intent.
- Restricted goods-list and missing goods-list production boundary cases still need explicit coverage.
- Player legion level remains a fixed staged default of `0`; real legion lookup is not wired.
- Java localization parameter `npc.getObjectTemplate().getL10n()` is not represented in the descriptor yet.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 0 new production artifacts; 1 production-boundary no-sell regression added
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: 5 blocked/partial categories: live no-sell send, message serialization/localization, restricted/missing goods boundary coverage, player legion lookup, and live AI dispatch
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a production non-sending restricted-goods `BUY` boundary regression using a goods list whose `legion_lvl` exceeds the staged player legion level, then pin Java `SM_SYSTEM_MESSAGE`/`SM_TRADELIST` byte expectations before any live send work.

Recommended starting points:
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionStorageExpansionDialogTests.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/NpcDialogTradeListFactAdapterService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/SmTradeListPacketPlanService.cs`
- `game-server/src/com/aionemu/gameserver/services/DialogService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_TRADELIST.java`

Keep live sends disabled until byte serialization, opcode/message expectations, localization payloads, runtime limited items, player legion lookup, `Npc.canSell/canBuy`, and NPC AI/controller routing are ready.

# Next Work Options

## Recommended Sequential Task

- Task: add a production non-sending restricted-goods `BUY` boundary regression.
- Why: Java's second no-sell branch depends on goods-list legion gating; the production boundary currently only proves the missing-template branch.
- Files: `GameServerConnectionStorageExpansionDialogTests.cs`, possibly fixture XML helpers, docs.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Read-only `SM_TRADE_IN_LIST` Java audit | none | Low | Safe analysis for a later trade-in slice. |
| B | Static-data trade NPC type validation test | focused static-data test file | Medium | Avoid shared loader edits unless required. |
| C | Limited-item dependency mapping | none | Low | Read-only inspection of `LimitedItemTradeService` and related model classes. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Restricted-goods production boundary regression | `GameServerConnectionStorageExpansionDialogTests.cs`, docs | live packet sends |
| Agent A | Read-only limited-item dependency map | read-only Java files | all writes |

## Do Not Parallelize

- `GameServerConnection.HandleDialogSelectAsync`: shared production socket handler.
- `StaticData.cs`: shared XML loader.
- Progress/handoff docs: orchestrator-owned.
