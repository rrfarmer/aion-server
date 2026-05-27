# Phase 6AKA Completion - Extraction Failure Guards

Date: 2026-05-27
Unit of Work: UOW-1451
Status: Complete after validation.

## Scope

Audit Java extraction failure branches after UOW-1450 and pin the C# transaction-first behavior with focused tests. This unit is test/documentation-only.

## Completed Work

- Confirmed Java `EnchantService.breakItem` returns `false` when target or parent extraction tool is missing at scheduled completion, then `ExtractAction` sends the final failure usage animation.
- Confirmed Java deletes the target and decreases the tool before calling `ItemService.addItem`; a missing reward template would throw from `Objects.requireNonNull` after those mutations and before the final success animation.
- Added `HandleUseItemAsync_ExtractMissingRewardTemplateFailsWithoutMutation` to document the C# preflight guard as an intentional transaction-safety difference from Java's consume-then-throw path.
- Added `HandleUseItemAsync_ExtractMissingScheduledItemSendsFailureWithoutMutation` for both missing source tool and missing target item at scheduled completion.
- No production code changed in this unit.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests.HandleUseItemAsync_Extract"`.
- Result: passed 7 tests.

## Migration Parity Table - UOW-1451

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.item.actions.ExtractAction` | `GameServerConnection.HandleExtractUseItemAsync` / `CompleteExtractUseItemAsync` | Item Action / Scheduled Handler | Partial | Regression Tested | Partial Parity | Missing scheduled source/target tests now assert initial animation followed by final failure animation with no reward mutation, matching Java's completion-time missing-item `false` result. |
| `com.aionemu.gameserver.services.EnchantService.breakItem` | `Aion.GameServer.Services.EnchantService.CreateBreakItemPlan` | Service / Extraction Planner | Partial | Regression Tested | Partial Parity / Intentional Difference | Missing source/target at completion maps to C# failure without mutation. Missing reward template intentionally differs: C# preflights and sends failure animation without consuming, while Java consumes then throws in `ItemService.addItem`. |
| `com.aionemu.gameserver.services.item.ItemService.addItem` | `InventoryAddService.CreateAddItemPlan` / extraction reward planning | Reward Add Service | Partial | Regression Tested | Intentional Difference | Java `Objects.requireNonNull(itemTemplate)` would throw for absent reward template after target/tool mutations. C# preserves transaction-first behavior and does not mutate if reward template is absent. |
| `com.aionemu.gameserver.model.items.storage.Storage.delete` | `SendExtractConsumedItemPacketsAsync` / `ApplyBreakItemInventoryMutation` | Storage Delete / Packet Caller | Partial | Regression Tested | Partial Parity | Missing scheduled item tests assert no delete packet is sent when Java's initial live-storage check would fail. Delete-fails-after-check remains not directly simulatable in the snapshot planner. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseByObjectId` | `SendExtractConsumedItemPacketsAsync` / source mutation planner | Storage Source Mutation | Partial | Regression Tested | Partial Parity | Missing scheduled source test asserts no source decrease/delete packet and no target delete. Tool-disappears-after-target-delete Java branch remains a documented risk. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION` | `SmItemUsageAnimation` | Packet | Partial | Regression Tested | Partial Parity | Tests assert Java-shaped initial 5000ms animation and final failure end state `2` for C# guarded failures. Java missing reward-template exception may skip the final animation; C# intentionally sends failure. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleUseItemAsync_ExtractMissingRewardTemplateFailsWithoutMutation` | Regression / intentional difference | `EnchantService.breakItem`, `ItemService.addItem` | C# missing reward-template guard leaves target/tool untouched, creates no reward, and sends final failure animation. | Deterministic C# assertion against reviewed Java consume-then-throw branch, documented as intentional difference. | Does not mirror Java's unsafe post-consumption throw; no Java runtime artifact. |
| `HandleUseItemAsync_ExtractMissingScheduledItemSendsFailureWithoutMutation` | Regression / scheduled race | `EnchantService.breakItem`, `ExtractAction.act` | Missing source or target at scheduled completion sends only initial animation plus final failure animation and produces no reward. | Deterministic C# assertion from Java's first live-storage guard returning `false`. | Does not simulate Java target delete failing after the initial guard or tool decrease failing after target delete. |
| Existing `HandleUseItemAsync_Extract*` tests | Existing Regression | Same Java artifacts | Extraction success, merge, source delete, and inventory-full paths remained stable. | 7-test focused connection slice passed. | Full packet bytes remain unavailable. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Missing reward-template behavior is intentionally different for C# transaction safety; Java would consume then throw, while C# fails before mutation.
- Java tool-disappears-after-target-delete and target-delete-fails-after-guard races remain documented but not directly reproduced by the snapshot planner.
- Full packet bytes remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 0 production artifacts changed in this unit
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: Java runtime artifact generation, post-guard storage race reproduction, Java packet byte comparisons
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: move to another small, low-churn extraction or toy-pet/kisk edge.
- Best next extraction slice: add service-level documentation/tests for tool-decrease-after-target-delete behavior if a pure planner seam can represent it.
- If that would require production churn, switch to toy-pet despawn scheduling observability with a fixture-only hook.

## Suggested Acceptance Criteria

- Keep Java source comments tied to `EnchantService.breakItem`, `ExtractAction.act`, or `ToyPetSpawnAction` depending on chosen slice.
- Do not mirror Java's missing-reward-template consume-then-throw path unless explicitly deciding to trade away C# transaction safety.
- Add only focused tests unless a production seam is already present and small.
- Update the Migration Parity Table for every Java artifact touched.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Extraction post-guard race design | Java/C# enchant source and tests | Medium | Read-only first; production change likely not worth it unless a pure seam exists. |
| B | Toy-pet despawn scheduling observability | `GameServerConnectionFlightZoneFanoutTests.cs` | Medium | Only if a tiny fixture hook is enough. |
| C | Java artifact tooling note | docs/source read-only | Low | No runtime parity claims without generated artifacts. |

## Do Not Parallelize

- Shared `GameServerConnection.cs` item-use changes.
- Multiple edits to `GameServerConnectionInventoryExpansionUseItemTests.cs`.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1451] Cover extraction failure guards`.
- C# files changed in UOW-1451:
  - `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AKA-Completion.md`
