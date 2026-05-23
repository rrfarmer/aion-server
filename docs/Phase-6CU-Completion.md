# Phase 6CU Completion Handoff

**Created**: May 23, 2026  
**Status**: Phase 6 remains in progress; this handoff follows 6CT and covers Sessions 538-540.  
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, runtime side effects, static-data semantics, persistence behavior, scheduling, and combat math.  
**Workflow rule**: Do one focused unit of work, validate it, update the migration parity table, commit it, then repeat for as long as useful work remains.  
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.  
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx` passes with 1143 tests.

---

## Recent Work Completed

- Added `PortalEntryValidationService.ValidateQuestRequirements`, mirroring Java `PortalService.checkQuests` for loaded `PortalQuestRequirementSummary` entries.
- Integrated quest validation into `ValidatePortalEntryPlan` after title validation and before registered-instance/cooldown checks, matching Java guard order.
- Added quest failure packet behavior for dialog NPCs (`SM_DIALOG_WINDOW` no-right page) and non-dialog NPCs (`SM_SYSTEM_MESSAGE.STR_SKILL_CAN_NOT_USE_GROUPGATE_NO_RIGHT`, id `1300150`).
- Added `PortalEntryValidationService.ValidateRequiredItemsAndKinah`, a validation-only slice of Java `PortalService.checkAndRemoveRequiredItems`.
- Integrated item/kinah validation into `ValidatePortalEntryPlan` after level validation and before same-instance teleport planning.
- Added `SmSystemMessage.InstanceCantEnterWithoutItem()` for Java `STR_MSG_INSTANCE_CANT_ENTER_WITHOUT_ITEM` (`1400219`).
- Added `PortalEntryValidationService.CreateRequiredItemsAndKinahConsumptionPlan`, a non-mutating planner that records which inventory rows would be updated or deleted for successful portal item/kinah consumption.
- Added `PortalRequirementConsumptionPlan` and `PortalRequirementConsumptionStep` records so later production wiring can persist row updates/deletes and emit inventory packets without recomputing Java order.
- Updated `docs/PHASE-6-PROGRESS.md` through Session 540 with migration parity tables, risks, metrics, validation, and next recommended work.

---

## Commits In This Handoff

- `f43cd8c43` - `Add portal quest requirement validation`
- `104b8a1e5` - `Add portal item requirement validation`
- `8e1e3d55f` - `Plan portal requirement consumption`

---

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|
| `PortalService.checkQuests` | `PortalEntryValidationService.ValidateQuestRequirements` | Partial | Unit Tested | Partial Parity | Empty requirements, bypass, completed quest, and sufficient quest-var step behavior are represented. Java quest engine/runtime comparison is not done. |
| `PortalService.checkAndRemoveRequiredItems` validation gates | `PortalEntryValidationService.ValidateRequiredItemsAndKinah` | Partial | Unit Tested | Partial Parity | Kinah and item failure behavior is represented, including dialog/system-message branch. Successful mutation remains separate. |
| `PortalService.checkAndRemoveRequiredItems` successful removal order | `PortalEntryValidationService.CreateRequiredItemsAndKinahConsumptionPlan` | Partial | Unit Tested | Partial Parity | Non-mutating plan records item stack consumption first and kinah last. Production handler wiring, persistence, packet dispatch, and teleport remain missing. |
| `Storage.getKinah` / `Storage.getItemCountByItemId` | `Player.InventoryItems` count sums | Partial | Unit Tested | Needs Verification | C# sums current inventory rows. Java storage filtering, locking, stack order, and kinah storage semantics are not runtime-compared. |
| `Storage.decreaseByItemId` / `Storage.decreaseKinah` | `PlanDecreaseByItemId` and planned item updates/deletes | Partial | Unit Tested | Needs Verification | Count math is represented. Java packet emission, persistent state, delete queue, logging, and quest item removal callbacks are not implemented here. |
| `SM_SYSTEM_MESSAGE.STR_SKILL_CAN_NOT_USE_GROUPGATE_NO_RIGHT` | `SmSystemMessage.SkillCanNotUseGroupgateNoRight` | Complete | Regression Tested | Partial Parity | Message id `1300150` serializes in packet tests. Live-client capture not run. |
| `SM_SYSTEM_MESSAGE.STR_MSG_INSTANCE_CANT_ENTER_WITHOUT_ITEM` | `SmSystemMessage.InstanceCantEnterWithoutItem` | Complete | Regression Tested | Partial Parity | Message id `1400219` serializes in packet tests. Live-client capture not run. |
| Java item packet dispatch through `ItemPacketService` | Not yet wired for portal requirement consumption | Not Started | No Tests | Unknown | Newly discovered dependency for production mutation. Planner exposes enough row data for a later packet/persistence boundary. |

Metrics from this handoff window:

- Total focused sessions covered: 3
- Total commits covered: 3
- Current full validation baseline: 1143 tests passing
- Total artifacts with verified parity newly promoted in this window: 0
- Total blocked artifacts: production portal handler wiring, inventory persistence, inventory packet dispatch, quest item removal side effects, group/alliance/league checks, actual teleport execution, instance allocation/transfer, and live-client validation
- Estimated overall migration completion: Phase 6 remains about 60% complete; portal guard parity is stronger, but end-to-end portal use is still incomplete.

---

## Important Limits

- No production packet handler calls `ValidatePortalEntryPlan`.
- `PortalEntryPlanResult.FailurePacket` returns packet objects; it does not send them through socket/connection infrastructure.
- `SameInstanceTeleport` is only an action marker. It does not call `TeleportService.teleportTo`, mutate `Player.Position`, update known lists, or emit teleport packets.
- Required item and kinah validation is now represented, and a non-mutating consumption plan exists, but live inventory is not changed.
- No database repository applies `PortalRequirementConsumptionPlan.UpdatedItems` or `DeletedObjectIds`.
- No portal code sends `SmInventoryUpdateItem` or `SmDeleteItem` for consumed required items/kinah.
- Java side effects from `Storage.decreaseItemCount` remain missing: `PersistentState.UPDATE_REQUIRED`, delete queues, update types, logging, packet dispatch, and `QuestEngine.onItemRemoved`.
- Group/alliance/league portals are explicitly unsupported in the plan helper rather than partially emulated.
- Admin/membership bypasses are still explicit parameters, not live permission/config lookups.
- Siege ownership remains caller-supplied; `PortalPathSummary.SiegeId` is loaded but not resolved through a C# `SiegeService`.
- Duplicate same-item portal requirements need static data review because Java validates all requirements before any removal but ignores the boolean result of later `decreaseByItemId` calls.
- Java JAXB behavior, packet dispatch ordering, threading, date/time behavior beyond cooldown timestamps, precision/rounding, and live-client behavior remain unverified.

---

## Next Unit Of Work

Recommended next unit: add the narrow production-facing inventory application boundary for portal requirement consumption, without performing final teleport yet.

Suggested scope:

1. Re-read Java:
   - `PortalService.checkAndRemoveRequiredItems`
   - `Storage.decreaseByItemId`
   - `Storage.decreaseKinah`
   - `Storage.decreaseItemCount`
   - `ItemPacketService.sendItemPacket`
2. Re-read C#:
   - `PortalEntryValidationService.CreateRequiredItemsAndKinahConsumptionPlan`
   - `SmInventoryUpdateItem`
   - `SmDeleteItem`
   - existing item mutation send paths in `GameServerConnection`
3. Add a narrow application/packet boundary that:
   - accepts a successful `PortalRequirementConsumptionPlan`
   - applies planned row updates/deletes to a working inventory list or repository-facing DTO
   - prepares Java-like inventory packets: `SmDeleteItem(..., UseDeleteType)` for deleted required items, `SmInventoryUpdateItem(..., DecreaseItemUse)` for reduced required item stacks, and `SmInventoryUpdateItem(..., DecreaseKinahBuy)` for kinah
   - keeps actual teleport execution out of scope
4. Keep scope narrow:
   - Do not implement group/alliance fanout.
   - Do not implement final `TeleportService.teleportTo` yet.
   - Do not claim verified parity without runtime packet/client comparison.
5. Tests:
   - item stack delete packet is planned for consumed stack reaching zero
   - item stack update packet is planned for partial consumption
   - kinah update packet uses `DecreaseKinahBuy`
   - no packets are produced for failed consumption plans
   - original inventory is not mutated unless the new boundary is explicitly named as an apply method

Useful validation commands:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PortalEntryValidationServiceTests|FullyQualifiedName~GamePacketTests"
dotnet test dotnetConversion\AionServer.slnx
```

---

## Resume Checklist

- Start from branch `4.8`.
- Confirm `git status --short` is clean.
- Read `docs/csharp-port.md`, latest `docs/PHASE-6-PROGRESS.md` Sessions 538-540, `docs/Phase-6CT-Completion.md`, and this handoff.
- Continue with one focused Java-parity unit.
- Update the migration parity table before committing.
- Commit the unit, then repeat until the next handoff is needed.
