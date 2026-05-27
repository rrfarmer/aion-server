# Phase 6 AIL Completion - Quest/Custom Reward Cleanup-Seal Caller Audit

Date: 2026-05-27
Unit of Work: UOW-1410
Status: read-only quest/custom reward cleanup-seal caller audit complete.

## Completed

- Reviewed Java quest completion item reward sources: `QuestService.finishQuest`, `AbstractQuestHandler.giveQuestItem`, and XML quest `GiveItemOperation`.
- Reviewed Java custom level reward sources: `BonusPackService.addPlayerCustomReward` and `FactionPackService.addPlayerCustomReward`.
- Reviewed current C# quest reward projection, non-item reward services, custom reward mail planning, and remaining inventory add/update call sites.
- Created `docs/Phase-6-QuestCustomReward-CleanupSealCallerAudit.md`.
- Identified the next narrower implementation target: pet-feed normal-cube unlock metadata already carries `GeneralInfoWarehouseRestrictionFlag` but does not pass it into `SmInventoryAddItem.CreateAllSlot`.

## Validation

- Read-only audit; no `dotnet test` run for this unit.
- Used `rg` and source reads to verify Java/C# caller ownership and the pet-feed normal-cube metadata gap.

## Migration Parity Table - UOW-1410

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.QuestService.finishQuest` item reward loop | `Aion.GameServer.Services.QuestFinishRewardTemplateXmlProjectionExtractor` and future quest item reward executor | Service / Reward Orchestration | Partial | Manual Only | Needs Verification | Java immediately calls `ItemService.addItem` for fixed/selectable/extended/class/bonus quest reward items. C# currently projects metadata and non-item reward side effects, but no audited live item add/update packet sender exists. Missing methods include concrete inventory mutation, persistence, packet fanout, selectable reward packet assertions, and cleanup/seal flag sourcing. |
| `com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler.giveQuestItem` | future quest work-item grant executor | Service / Quest Work Item | Not Started | No Tests | Needs Verification | Java work-item grants use `ItemAddType.QUEST_WORK_ITEM` with `ItemUpdateType.INC_ITEM_COLLECT`. C# has quest-drop/work-item planning surfaces, but no direct packet-equivalent grant path was changed or verified in this unit. |
| `com.aionemu.gameserver.questEngine.handlers.models.xmlQuest.operations.GiveItemOperation` | future XML quest give-item operation executor | Service / XML Quest Operation | Not Started | No Tests | Needs Verification | Java XML quest operation delegates to `ItemService.addItem`; C# operation execution remains gated/non-live in the audited docs and services. |
| `com.aionemu.gameserver.services.BonusPackService.addPlayerCustomReward` | `Aion.GameServer.Services.CustomLevelRewardPlanService`, `SystemMailRewardPlanService`, and persistence services | Service / Custom Reward Mail | Partial | Unit Tested | Partial Parity | C# custom rewards plan system mail and attached items; UOW-1399 already covered live read-mail cleanup/seal serialization. Sending/persistence fanout remains opt-in/gated and not Java runtime verified. |
| `com.aionemu.gameserver.services.FactionPackService.addPlayerCustomReward` | `Aion.GameServer.Services.CustomLevelRewardPlanService`, `SystemMailRewardPlanService`, and persistence services | Service / Custom Reward Mail | Partial | Unit Tested | Partial Parity | C# has faction reward planning/mail surfaces, but live execution remains gated. Race filtering and mail persistence have source-derived tests, not Java runtime packet comparison. |
| `com.aionemu.gameserver.services.toypet.PetService` rejected-food unlock packet path / `ItemPacketService.sendItemUnlockPacket` | `Aion.GameServer.Services.ToyPet.PetFeedPacketMetadataBridge.ConstructFoodItemUnlock` | Service / Packet Metadata | Partial | No Tests For Cube Cleanup Flag | Needs Verification | Newly discovered implementation candidate: `PetFeedUnlockPacketContext.GeneralInfoWarehouseRestrictionFlag` is passed to warehouse/unusual/legion unlock packet branches, but normal-cube `SmInventoryAddItem.CreateAllSlot` still defaults to cleanup/seal flag `0`. |

## Tests Added Or Updated

No tests were added in this read-only audit unit.

Existing relevant coverage:

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `QuestFinishRewardTemplateXmlProjectionExtractorTests` | Unit / projection | `QuestService.getRewardItems` | Existing tests cover XML reward item projection metadata. | Source-derived C# metadata assertions. | Does not mutate inventory or serialize packets. |
| `CustomLevelRewardExecutionServiceTests` and `SystemMailReward*Tests` | Unit / persistence planning | `BonusPackService`, `FactionPackService`, `SystemMailService.sendMail` | Existing tests cover custom reward mail planning and persistence operation ordering. | Source-derived C# assertions. | Does not compare Java runtime mail packets; live persistence remains opt-in/gated. |
| `GamePacketTests.SmInventoryAddItem_WritesCleanupSealFlagInItemBlobLikeJava` | Regression / packet byte layout | `SM_INVENTORY_ADD_ITEM.writeItemInfo`, `GeneralInfoBlobEntry` | Existing packet guard confirms supplied cleanup/seal flag `3` reaches the full item blob. | C# packet-byte assertion against deterministic Java source order/field value. | Does not prove any quest/custom caller supplies the flag. |

## Remaining Risks

- Quest reward item execution is broad and still needs concrete C# inventory mutation, persistence, packet fanout, reward-selection, and callback-ordering surfaces before live cleanup/seal wiring.
- Custom reward mail paths must be validated through mail attachment serialization/read paths rather than immediate inventory add/update assumptions.
- The pet-feed normal-cube unlock gap is non-sending metadata today and still needs focused packet metadata tests.
- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Temporary-exchange remaining seconds, runtime conditioning presence, and time-normalized expiration/dye comparison remain unresolved item-blob risks.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 0 C# runtime surfaces changed in this read-only audit
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: quest/custom reward live item packet sender, quest work-item grant executor, XML quest give-item executor, custom reward live mail/send/runtime comparison, pet-feed normal-cube unlock cleanup flag source, Java runtime artifact generation
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Work Options

## Recommended Sequential Task

- Task: wire pet-feed normal-cube unlock cleanup/seal metadata.
- Scope:
  - update `PetFeedPacketMetadataBridge.ConstructFoodItemUnlock` cube branch to pass `context.GeneralInfoWarehouseRestrictionFlag` into `SmInventoryAddItem.CreateAllSlot`;
  - add a focused `PetFeedPacketMetadataBridge` test that builds restricted normal-cube unlock context and parses the generated `SmInventoryAddItem.ALL_SLOT` blob for cleanup/seal flag `3`;
  - keep this as metadata-only unless the live pet-feed runtime caller already supplies deterministic context.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Pet-feed normal-cube cleanup flag implementation | `PetFeedPacketMetadataBridge.cs` and focused tests | Low | Best next unit; one owner only because implementation/test names should align. |
| B | Remaining inventory add/update caller search refinement | read-only `GameServerConnection.cs`, service packet planners | Low | Can prepare the next queue while A edits pet-feed. |
| C | Quest item reward executor design audit | read-only quest reward projection/planning services and Java `QuestService` | Medium | Useful later, but too broad for the immediate cleanup-seal implementation. |

## Do Not Parallelize

- Do not edit `PetFeedPacketMetadataBridge.cs` and its focused tests from multiple workers at once.
- Do not edit shared progress/handoff/parity docs from sub-agents; orchestrator owns docs.
- Do not claim verified parity for quest/custom rewards until live packet sender behavior is implemented and compared against deterministic evidence.

## Context For Next Session

- Current phase: Phase 6, cleanup/seal full item-blob flag propagation across live or staged inventory-producing callers.
- Last completed UOW: UOW-1410.
- Last commit planned: `[Phase 6][UOW-1410] Audit quest custom reward cleanup seal callers`.
- Key changed files in this handoff:
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6-QuestCustomReward-CleanupSealCallerAudit.md`
  - `docs/Phase-6AIL-Completion.md`
