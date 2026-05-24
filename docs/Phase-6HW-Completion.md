# Phase 6HW Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6HV and covers Session 719.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "IdianPolishBurnApplicationServiceTests|IdianPolishServiceTests|PlayerEnterWorldServiceTests"`
  - Result: Passed, 32 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1292 tests.

## Recent Work Completed

### Session 719 - Idian Burn Packet Application Boundary

- Added `IdianPolishBurnApplicationService.ApplyBurnPlan` and `IdianPolishBurnApplicationResult`.
- The helper updates `player.InventoryItems` from an `IdianPolishBurnPlan`.
- It creates compact `SmInventoryUpdateItem.PolishCharge` packets for Java low-charge threshold crossings.
- It creates full `SmInventoryUpdateItem.DecreaseItemUse` packets for exhausted idians.
- Missing item templates skip packet creation while still applying the in-memory inventory update.
- Added `IdianPolishBurnApplicationServiceTests.ApplyBurnPlan_UpdatesInventoryAndCreatesLowAndExhaustedPackets`.
- Added `IdianPolishBurnApplicationServiceTests.ApplyBurnPlan_UpdatesInventoryButSkipsPacketWhenTemplateMissing`.
- Added `IdianPolishBurnApplicationServiceTests.ApplyBurnPlan_NoOpsWhenPlanHasNoChanges`.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.items.IdianStone.decreasePolishCharge` | `Aion.GameServer.Services.IdianPolishBurnApplicationService.ApplyBurnPlan` | Model Helper / Packet Caller Helper | Partial | Regression Tested | Needs Verification | C# now applies planned idian burn item updates and selects low-charge versus exhaustion packet paths. Java mutates the `IdianStone` under synchronization, sends the exhaustion packet before `item.setIdianStone(null)`, marks the stone `DELETED`, and calls `ItemStoneListDAO.storeIdianStones`; C# consumes an immutable burn plan whose exhausted update already has `IdianStone = null`, so threading, mutation ordering, DAO timing, and exact exhaustion serialization remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` | Server Packet | Partial | Regression Tested with byte-level payload checks | Needs Verification | Tests confirm the low-charge compact polish blob and exhausted full update packet shape emitted by the helper. No Java golden packet capture, encrypted-frame comparison, live socket ordering check, or live-client validation was run. |
| `com.aionemu.gameserver.services.item.ItemPacketService.ItemUpdateType.POLISH_CHARGE` | `SmInventoryUpdateItem.PolishCharge` | Packet Update Type / Enum Equivalent | Partial | Regression Tested | Needs Verification | C# emits compact polish-charge updates only for `IdianPolishBurnUpdateKind.LowCharge`, matching the Java threshold event. The actual Java runtime packet from `IdianStone.decreasePolishCharge` has not been captured. |
| `com.aionemu.gameserver.services.item.ItemPacketService.ItemUpdateType.DEC_ITEM_USE` | `SmInventoryUpdateItem.DecreaseItemUse` | Packet Update Type / Enum Equivalent | Partial | Regression Tested | Needs Verification | C# emits a full decrease-item-use update for exhausted idians. Java sends the full update before clearing the item's idian reference; C# currently sends the full blob from an already-cleared item update, so the zero-charge idian blob versus missing-idian blob behavior needs Java runtime/golden verification. |
| `com.aionemu.gameserver.model.gameobjects.Item.setIdianStone` | `InventoryItem.IdianStone` updated through `IdianPolishBurnApplicationService.ApplyBurnPlan` | Runtime Model / Item Mutation | Partial | Unit Tested | Needs Verification | C# updates `Player.InventoryItems` with low-charge and exhausted idian states. Broader Java equipment-storage map mutation semantics, stat/effect refresh from `IdianStone.onUnEquip`, concurrency, serialization, and persistence flush timing remain partial. |
| `com.aionemu.gameserver.dataholders.DataManager.ITEM_DATA` item-template lookup | `Aion.GameServer.Dataholders.ItemTemplateTable` | Data Lookup Dependency | Partial | Unit Tested | Needs Verification | Missing templates suppress packet creation while preserving in-memory state. This is a C# defensive behavior for the current caller seam and is not proven Java behavior; static-data/JAXB parity and production logging/error policy remain unresolved. |
| `com.aionemu.gameserver.dao.ItemStoneListDAO.storeIdianStones` | `Aion.GameServer.Services.PlayerEnterWorldService.SaveIdianPolishBurnMutationAsync` | Repository Dependency | Partial | Regression Tested indirectly | Needs Verification | The application helper does not persist by itself; it is intended to pair with the existing exhausted-idian persistence boundary. Live DAO behavior, transaction semantics, deletion ordering, and rollback behavior remain unverified. |

## Tests Added Or Updated

- `IdianPolishBurnApplicationServiceTests.ApplyBurnPlan_UpdatesInventoryAndCreatesLowAndExhaustedPackets`
  - Validates inventory mutation, compact low-charge packet payload, exhausted full `DecreaseItemUse` packet payload shape, and no packet for non-threshold burn updates.
- `IdianPolishBurnApplicationServiceTests.ApplyBurnPlan_UpdatesInventoryButSkipsPacketWhenTemplateMissing`
  - Validates missing templates do not block in-memory polish-charge updates but suppress packet creation.
- `IdianPolishBurnApplicationServiceTests.ApplyBurnPlan_NoOpsWhenPlanHasNoChanges`
  - Validates no inventory or packet mutation for an empty plan.
- Existing `IdianPolishServiceTests` and `PlayerEnterWorldServiceTests` matched by the focused filter were rerun.

These tests are source-derived from Java. They do not compare against Java runtime execution, Java-generated golden idian-burn packets, encrypted frames, live socket ordering, threading behavior, serialization beyond local C# packet byte assertions, DAO behavior, or live client behavior.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 7
- Total artifacts ported or partially modeled in this handoff window: 1 idian burn packet/inventory application boundary slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 7
- Total blocked/not-started artifacts: production combat/effect caller invocation, exhausted-idian Java packet ordering comparison, Java runtime/golden packet comparison, live DAO comparison, synchronized/threading comparison, and live-client validation.
- Estimated overall migration completion: 65%

## Remaining Risks

- The idian application helper is not yet invoked from a production combat/effect observer caller.
- Exhausted-idian packet serialization may differ because Java sends the full item update before clearing `item.setIdianStone(null)`, while the current C# burn result has already removed the idian stone.
- Java synchronized mutation, observer ordering, `IdianStone.onUnEquip` stat/effect refresh, DAO delete timing, and production packet ordering remain unverified.
- Missing-template packet omission is a defensive C# behavior and must be revisited when the production caller has logging/error policy.
- No Java runtime, live database, or live-client comparison was run.

## Next Recommended Unit of Work

Wire the charge and idian burn application/persistence pairs into the first stable represented combat/effect observer caller, preserving Java's ordinary attack `skillId == 0` guard, dot-attacked exception, charge-bar packet gating, low-charge idian packet gating, exhausted-idian full update, and persistence ordering. If that caller remains premature, add a narrow idian observer registration/effect-refresh bridge around `IdianStone.onEquip`/`onUnEquip` and document the remaining stat/effect fanout gaps.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6HV-Completion.md`
   - this handoff
3. Inspect selected Java source and nearest C# tests before touching code.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Create the next handoff document and commit the unit.
