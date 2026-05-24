# Phase 6HV Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6HU and covers Session 718.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "ItemChargeBurnApplicationServiceTests|ItemChargeServiceTests|PlayerEnterWorldServiceTests"`
  - Result: Passed, 32 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1289 tests.

## Recent Work Completed

### Session 718 - Charge Burn Packet Application Boundary

- Added `ItemChargeBurnApplicationService.ApplyBurnPlan` and `ItemChargeBurnApplicationResult`.
- The helper updates `player.InventoryItems` from an `ItemChargeBurnPlan`.
- It creates `SmInventoryUpdateItem` packets with update type `Charge` only for burns whose Java-style visual charge bar step changed.
- Missing item templates skip packet creation while still applying the in-memory inventory update.
- Added `ItemChargeBurnApplicationServiceTests.ApplyBurnPlan_UpdatesInventoryAndPacketsOnlyChangedChargeBars`.
- Added `ItemChargeBurnApplicationServiceTests.ApplyBurnPlan_UpdatesInventoryButSkipsPacketWhenTemplateMissing`.
- Added `ItemChargeBurnApplicationServiceTests.ApplyBurnPlan_NoOpsWhenPlanHasNoChanges`.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.items.ChargeInfo.sendItemUpdate` | `Aion.GameServer.Services.ItemChargeBurnApplicationService.ApplyBurnPlan` | Packet Caller Helper / Service | Partial | Regression Tested | Needs Verification | C# now creates `SM_INVENTORY_UPDATE_ITEM` charge packets only for burns that cross Java's visual charge bar step. Production observer invocation, exact attack-status ordering, live socket ordering, and Java runtime comparison remain unverified. |
| `com.aionemu.gameserver.model.items.ChargeInfo.updateChargePoints` | `ItemChargeBurnApplicationService.ApplyBurnPlan` plus `ItemChargeUpdateResult.ChargeBarChanged` | Model Helper / Packet Dependency | Partial | Regression Tested | Needs Verification | The application helper consumes clamped charge updates and applies them to `Player.InventoryItems`. Java mutates the item directly under synchronization; C# applies immutable updates after a plan is produced, so threading and mutation timing remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` | Server Packet | Partial | Regression Tested with byte-level payload check | Needs Verification | Test confirms the charge update packet body uses Java's compact conditioning blob shape for the applied burn packet. Encrypted frame handling, production send ordering, and live-client behavior remain unverified. |
| `com.aionemu.gameserver.services.item.ItemPacketService.ItemUpdateType.CHARGE` | `SmInventoryUpdateItem.Charge` | Packet Update Type / Enum Equivalent | Partial | Regression Tested | Needs Verification | C# helper emits the `Charge` update type only when `ChargeBarChanged` is true. No Java golden packet capture from a live observer burn was used. |
| `com.aionemu.gameserver.model.gameobjects.player.Player` inventory item list | `Aion.GameServer.Model.GameObjects.Player.InventoryItems` | Runtime Model | Partial | Unit Tested | Needs Verification | C# inventory list is updated in memory with burn item updates. Broader Java storage/equipment map mutation semantics, concurrency, and persistence flush timing remain partial. |
| `com.aionemu.gameserver.dataholders.DataManager.ITEM_DATA` item-template lookup | `Aion.GameServer.Dataholders.ItemTemplateTable` | Data Lookup Dependency | Partial | Unit Tested | Needs Verification | Missing template causes packet omission while retaining in-memory update; this is a C# safety behavior for an incomplete caller seam, not verified Java behavior. JAXB/reflection/static-data parity and production logging/retry behavior remain unverified. |

## Tests Added Or Updated

- `ItemChargeBurnApplicationServiceTests.ApplyBurnPlan_UpdatesInventoryAndPacketsOnlyChangedChargeBars`
  - Validates inventory mutation, packet creation only for `ChargeBarChanged`, and byte-level compact charge blob payload.
- `ItemChargeBurnApplicationServiceTests.ApplyBurnPlan_UpdatesInventoryButSkipsPacketWhenTemplateMissing`
  - Validates missing templates do not block in-memory charge state updates but suppress packet creation.
- `ItemChargeBurnApplicationServiceTests.ApplyBurnPlan_NoOpsWhenPlanHasNoChanges`
  - Validates no inventory or packet mutation for an empty plan.
- Existing `ItemChargeServiceTests` and `PlayerEnterWorldServiceTests` matched by the focused filter were rerun.

These tests are source-derived from Java. They do not compare against Java runtime execution, Java-generated golden observer-burn packets, encrypted frames, live socket ordering, threading behavior, serialization beyond the local packet byte assertion, or live client behavior.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 6
- Total artifacts ported or partially modeled in this handoff window: 1 charge burn packet/inventory application boundary slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 6
- Total blocked/not-started artifacts: production combat/effect caller invocation, packet ordering comparison, Java runtime/golden packet comparison, synchronized/threading comparison, and live-client validation.
- Estimated overall migration completion: 65%

## Remaining Risks

- The application helper is not yet invoked from a production combat/effect observer caller.
- Packet ordering relative to `SM_ATTACK_STATUS`, observer notifications, persistence, and stat updates is still unknown without Java capture or a production caller test.
- Missing-template packet omission is a defensive C# behavior and must be revisited when the production caller has logging/error policy.
- Java synchronized mutation and persistent-state flush timing remain unverified.
- No live-client or Java runtime packet comparison was run.

## Next Recommended Unit of Work

Wire the charge burn application/persistence pair into the first stable represented combat/effect observer caller, preserving Java's ordinary attack `skillId == 0` guard and dot-attacked exception; if that caller still lacks a safe packet send seam, mirror this application+packet helper for idian burn plans (`POLISH_CHARGE` low-charge and full item update on exhaustion).

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6HU-Completion.md`
   - this handoff
3. Inspect selected Java source and nearest C# tests before touching code.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Create the next handoff document and commit the unit.
