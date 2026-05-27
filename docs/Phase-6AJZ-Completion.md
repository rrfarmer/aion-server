# Phase 6AJZ Completion - Extraction Edge Rewards

Date: 2026-05-27
Unit of Work: UOW-1450
Status: Complete after validation.

## Scope

Add a focused extraction parity test slice from the UOW-1449 read-only extraction audit. This unit is test/documentation-only.

## Completed Work

- Added `CreateBreakItemPlan_UsesJavaExtractionStoneThresholds`, covering Java `EnchantService.calculateEffectiveLevel` thresholds for Alpha, Beta, Gamma, Delta, and Epsilon extraction rewards.
- Added Beta/Gamma extraction reward templates to the focused `EnchantServiceTests` fixture and allowed test-specific threshold armor templates.
- Added `HandleUseItemAsync_ExtractInventoryFullStillConsumesAndSendsDiceError`, forcing a post-consumption partial reward add by using a one-stack extraction reward and a full cube.
- The new integration test asserts Java-shaped target delete, cube update, extraction tool decrease, one reward add/cube for the fitting stack, `STR_MSG_DICE_INVEN_ERROR`, and final success animation.
- No production code changed in this unit.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~EnchantServiceTests.CreateBreakItemPlan|FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_Extract"`.
- Result: passed 12 tests.

## Migration Parity Table - UOW-1450

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.EnchantService` | `Aion.GameServer.Services.EnchantService` | Service / Extraction Planner | Partial | Unit + Regression Tested | Partial Parity | Threshold table now covers Alpha/Beta/Gamma/Delta/Epsilon reward IDs using Java effective-level rules. Missing reward-template and race behavior remain unverified. |
| `com.aionemu.gameserver.model.templates.item.actions.ExtractAction` | `GameServerConnection.HandleExtractUseItemAsync` / `CompleteExtractUseItemAsync` | Item Action / Connection Handler | Partial | Regression Tested | Partial Parity | Inventory-full-after-consumption path now asserts scheduled extraction still consumes target/tool, adds what fits, sends dice error, and completes. Runtime Java bytes remain unavailable. |
| `com.aionemu.gameserver.model.enchants.EnchantmentStone` | extraction stone constants in `EnchantService` tests / `EnchantService.GetExtractionStoneItemId` | Enum / Threshold Mapping | Partial | Unit Tested | Partial Parity | Tests cover Java threshold IDs `166000191..166000195`. Old stone item-id family mapping remains outside this break-item extraction path. |
| `com.aionemu.gameserver.model.items.storage.Storage.delete` | `SendExtractConsumedItemPacketsAsync` / `SmDeleteItem` | Storage Delete / Packet Caller | Partial | Regression Tested | Partial Parity | Inventory-full test asserts target delete and cube update still occur before reward partial add/error. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseByObjectId` | `SendExtractConsumedItemPacketsAsync` / `SmInventoryUpdateItem.DecreaseItemUse` | Storage Source Mutation | Partial | Regression Tested | Partial Parity | Inventory-full test asserts extraction tool decrease after target delete. Single-use tool delete branch already covered by existing tests. |
| `com.aionemu.gameserver.services.item.ItemService.addItem` | `InventoryAddService.CreateAddItemPlan` / `SendExtractRewardPacketsAsync` | Reward Add Service | Partial | Regression Tested | Partial Parity | Inventory-full test asserts one fitting non-stackable reward is added before dice error when remaining rewards cannot fit. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `SmSystemMessage.DiceInventoryError` | Packet | Partial | Regression Tested | Partial Parity | Test asserts message id `1390182` for `STR_MSG_DICE_INVEN_ERROR`. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_ADD_ITEM` / `SM_CUBE_UPDATE` | `SmInventoryAddItem.CreateItemCollect` / `SmCubeUpdate.CubeSizeSnapshot` | Inventory Packets | Partial | Regression Tested | Partial Parity | Test asserts reward add plus cube update for the partial fitting reward. Full Java byte comparison remains blocked. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CreateBreakItemPlan_UsesJavaExtractionStoneThresholds` | Unit / threshold table | `EnchantService.calculateEffectiveLevel`, `EnchantmentStone` thresholds | Alpha/Beta/Gamma/Delta/Epsilon reward IDs from Java effective-level thresholds with deterministic RNG. | Deterministic C# service assertions derived from reviewed Java formulas. | Does not cover old enchant-stone item-id family mapping because break-item extraction emits only Alpha-Epsilon IDs. |
| `HandleUseItemAsync_ExtractInventoryFullStillConsumesAndSendsDiceError` | Regression / connection packet order | `ExtractAction.act`, `EnchantService.breakItem`, `ItemService.addItem` | Full-cube extraction still deletes target, decreases tool, adds one fitting reward, sends reward cube, sends dice inventory error, and sends final success animation. | Deterministic C# packet assertions from reviewed Java consume-then-add semantics. | Uses test-specific reward max stack to force partial add; no Java runtime bytes. |
| Existing `HandleUseItemAsync_Extract*` and `CreateBreakItemPlan*` tests | Existing Unit / Regression | Same Java artifacts | Regression slice remained stable. | 12-test focused suite passed. | Missing reward-template and scheduled race behavior remain uncovered. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- C# still has a `MissingRewardTemplate` preflight guard; Java would consume then fail/throw inside `ItemService.addItem` if the reward template were absent.
- Scheduled race behavior remains different/untested: Java rechecks live storage and returns/logs in some failure branches, while C# plans then persists/applies.
- Inventory-full test uses a controlled one-stack reward template to force the partial-add branch; real data may use stackable extraction rewards.
- Full packet bytes remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 8 grouped artifact rows in this unit
- Total artifacts ported: 0 production artifacts changed in this unit
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 8 grouped rows
- Total blocked artifacts: Java runtime artifact generation, missing reward-template behavior, scheduled race behavior, Java packet byte comparisons
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: audit extraction missing reward-template and scheduled race behavior before changing production code.
- Pin Java's consume-then-fail behavior for absent reward templates and disappearing target/tool races.
- Decide whether C# should intentionally differ because of transaction-first persistence or whether the planner/completion split needs a closer Java-shaped failure path.

## Suggested Acceptance Criteria

- Read Java `EnchantService.breakItem`, `ExtractAction.act`, `ItemService.addItem`, and `Storage.delete/decreaseByObjectId` at the failure branches.
- Read C# `EnchantService.CreateBreakItemPlan`, `CompleteExtractUseItemAsync`, and reward send/apply helpers.
- Add focused tests if behavior can be pinned without production churn.
- If production behavior should intentionally differ, document the reason in the parity table rather than marking verified parity.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Extraction missing reward-template/race audit | Java/C# enchant source and tests | Medium | Read-only first; likely next unit owner should avoid production changes until failure semantics are documented. |
| B | Toy-pet despawn scheduling observability | `GameServerConnectionFlightZoneFanoutTests.cs` | Medium | Only if a tiny fixture hook is enough. |
| C | Java artifact tooling note | docs/source read-only | Low | No runtime parity claims without generated artifacts. |

## Do Not Parallelize

- Shared `GameServerConnection.cs` item-use changes.
- Multiple edits to `GameServerConnectionInventoryExpansionUseItemTests.cs`.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1450] Cover extraction edge rewards`.
- C# files changed in UOW-1450:
  - `dotnetConversion/tests/Aion.GameServer.Tests/EnchantServiceTests.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AJZ-Completion.md`
