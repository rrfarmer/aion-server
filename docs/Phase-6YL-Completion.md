# Phase 6YL Completion - UOW-1150 Trade-List Restricted-Goods Boundary Regression

Date: May 26, 2026

## Unit of Work Summary

Continued Phase 6 trade-list parity by adding production socket-boundary regression coverage for Java's `BUY` no-sell path where a trade-list template exists but all goods lists are restricted above the player's legion level. The boundary is still non-sending; this unit only records staged intent.

The Java implementation is the source of truth:
- `DialogService.onDialogSelect` reads the trade-list template and loops its tabs.
- For each tab, Java calls `DataManager.GOODSLIST_DATA.getGoodsListById(tab.getId())`.
- Java skips tabs when the goods list is missing or `goodsList.getLegionLevel() > legionLevel`.
- If no tab remains sellable, Java sends `SM_SYSTEM_MESSAGE.STR_BUY_SELL_HE_DOES_NOT_SELL_ITEM`.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionStorageExpansionDialogTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6YL-Completion.md`

## Implementation Notes

- Added `HandleDialogSelectAsync_BuyRestrictedGoodsPlansNoSellMessageWithoutSending`.
- Extended the socket-boundary fixture static data with:
  - NPC trade-list template `203062`;
  - trade tab id `130`;
  - goods list id `130` with `legion_lvl="5"`.
- The production boundary currently supplies staged `PlayerLegionLevel = 0`, so the goods list is correctly treated as restricted.
- The observed plan records restricted goods id `130`, no sellable goods, no `SmTradeListPacketPlan`, and a non-live `SystemMessageDoesNotSellItem` descriptor.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "GameServerConnectionStorageExpansionDialogTests" --nologo` passed 6 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` passed 2,158 tests.
- `dotnet test dotnetConversion/AionServer.slnx --nologo` passed 2,365 tests.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.DialogService.onDialogSelect` | `NpcDialogServiceSelectPlanService` through production boundary | Service Planner | Partial | Unit Tested | Partial Parity | Restricted-goods no-sell branch is now covered through `HandleDialogSelectAsync`. Live `SM_SYSTEM_MESSAGE` send and Java byte/message comparison remain missing. |
| `com.aionemu.gameserver.model.templates.goods.GoodsList.getLegionLevel` | `Aion.GameServer.Dataholders.GoodsListSummary.LegionLevel` | Static Data DTO | Partial | Unit Tested | Needs Verification | Fixture proves `legion_lvl` participates in production-boundary planning. Full Java static-data parity and runtime player legion lookup remain unverified. |
| `com.aionemu.gameserver.dataholders.GoodsListData.getGoodsListById` | `Aion.GameServer.Dataholders.GoodsListTable.GetGoodsListById` | Static Data Repository | Partial | Unit Tested | Needs Verification | The boundary detects goods id `130` as restricted. Missing goods-list behavior still needs production-boundary coverage. |
| `com.aionemu.gameserver.model.team.legion.Legion.getLegionLevel` | `NpcDialogTradeListFactAdapterInput.PlayerLegionLevel = 0` at production boundary | Runtime Fact Dependency | Not Started | Unit Tested as explicit default | Needs Verification | The restricted-goods test depends on the current staged default. Live player legion lookup is still absent and must be wired before live sends. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_TRADELIST` | `Aion.GameServer.Services.SmTradeListPacketPlan` absence from restricted branch | Packet Plan | Partial | Unit Tested | Partial Parity | C# correctly withholds the staged packet plan when all goods are above player legion level. Binary packet serialization/opcode `253` remains unimplemented. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_BUY_SELL_HE_DOES_NOT_SELL_ITEM` | `NpcDialogServiceDescriptorKind.SystemMessageDoesNotSellItem` | Packet Descriptor | Partial | Unit Tested | Needs Verification | The descriptor is non-live intent only. Localization and byte-level system-message parity remain unverified. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionStorageExpansionDialogTests.HandleDialogSelectAsync_BuyRestrictedGoodsPlansNoSellMessageWithoutSending` | Unit / Socket Boundary Regression | `DialogService.onDialogSelect` `BUY`; `GoodsList.getLegionLevel`; `SM_TRADELIST` constructor filtering | Production `BUY` observes restricted goods id `130`, no sellable goods, no packet plan, and no-sell descriptor while sending no packets. | Source-reviewed Java branch plus production C# handler test with loaded XML `legion_lvl`. | No live player legion lookup, no live system-message send, no message byte comparison, no missing-goods branch. |

## Remaining Risks

- Live player legion lookup remains missing; the boundary still uses staged `PlayerLegionLevel = 0`.
- Missing goods-list production boundary coverage is not yet added.
- Live no-sell localization and packet serialization are still blocked.
- `SM_TRADELIST` byte serialization and limited-item data are still not ready for live send enablement.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 0 new production artifacts; 1 production-boundary restricted-goods regression added
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: 5 blocked/partial categories: live legion lookup, missing-goods coverage, live no-sell send/localization, `SM_TRADELIST` serialization, and limited-item lookup
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add the missing-goods production boundary regression, then start pinning Java packet/message byte expectations for no-sell and `SM_TRADELIST`.

Recommended starting points:
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionStorageExpansionDialogTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/NpcDialogTradeListFactAdapterService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/SmTradeListPacketPlanService.cs`
- `game-server/src/com/aionemu/gameserver/services/DialogService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_TRADELIST.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`

Keep live sends disabled until byte serialization, opcode/message expectations, localization payloads, runtime limited items, player legion lookup, `Npc.canSell/canBuy`, and NPC AI/controller routing are ready.

# Next Work Options

## Recommended Sequential Task

- Task: add a production non-sending missing-goods `BUY` boundary regression.
- Why: Java treats missing `GoodsListData.getGoodsListById(tab.getId())` the same as restricted goods; the socket boundary should prove that branch before live sends.
- Files: `GameServerConnectionStorageExpansionDialogTests.cs`, docs.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Read-only `SM_TRADE_IN_LIST` Java audit | none | Low | Safe analysis for a later trade-in slice. |
| B | Static-data trade NPC type validation test | focused static-data test file | Medium | Avoid shared loader edits unless required. |
| C | Limited-item dependency mapping | none | Low | Read-only inspection of `LimitedItemTradeService` and related model classes. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Missing-goods production boundary regression | `GameServerConnectionStorageExpansionDialogTests.cs`, docs | live packet sends |
| Agent A | Read-only limited-item dependency map | read-only Java files | all writes |

## Do Not Parallelize

- `GameServerConnection.HandleDialogSelectAsync`: shared production socket handler.
- `StaticData.cs`: shared XML loader.
- Progress/handoff docs: orchestrator-owned.
