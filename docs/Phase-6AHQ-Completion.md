# Phase 6 AHQ Completion - GeneralInfo Temp/Seal Input Audit

Date: 2026-05-27
Unit of Work: UOW-1389
Status: Read-only audit complete. No serializer code changed.

## Completed

- Added `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageGeneralInfoTempSealAudit.md`.
- Reviewed Java `GeneralInfoBlobEntry`.
- Reviewed Java `Item.getTemporaryExchangeTimeRemaining()` and `Item.setTemporaryExchangeTime(...)`.
- Reviewed Java `ItemRestrictionCleanupData`, `ItemCleanupTemplate`, and `ItemData.cleanup()`.
- Confirmed C# currently lacks:
  - an `InventoryItem` absolute temporary-exchange epoch field;
  - an item cleanup/static restriction dataholder equivalent;
  - a safe static-data ownership plan for general-info cleanup/seal flag serialization.
- Updated live-adapter readiness and progress/handoff notes.

## Validation

- Ran read-only source inspection.
- Ran `git diff --check`.
- No C# tests were required because this unit is docs-only.

## Migration Parity Table - UOW-1389

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.iteminfo.GeneralInfoBlobEntry` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteGeneralInfoBlob` | Serialization Entry | Partial | Manual Only | Needs Verification | C# still writes zero for temporary-exchange remaining seconds and cleanup/seal flag. Audit identifies required model/static-data inputs. |
| `com.aionemu.gameserver.model.gameobjects.Item` | `Aion.GameServer.Model.GameObjects.InventoryItem` | Model | Partial | Manual Only | Needs Verification | Java item carries absolute `temporaryExchangeTime` and computes remaining seconds at encode time. C# inventory item does not currently carry this field. Date/time handling remains unverified. |
| `com.aionemu.gameserver.dataholders.ItemRestrictionCleanupData` | future C# item cleanup/static restriction dataholder | Dataholder | Not Started | No Tests | Needs Verification | Java cleanup table exposes `hasAccountOrLegionWhStorabilityDisabled(itemId)`. No C# equivalent found. |
| `com.aionemu.gameserver.model.templates.restriction.ItemCleanupTemplate` | future C# item cleanup DTO | DTO | Not Started | No Tests | Needs Verification | Java cleanup rows include trade/sell/warehouse/account-warehouse/legion-warehouse result bytes. No C# equivalent found. |
| `com.aionemu.gameserver.dataholders.ItemData.cleanup` | future C# static item cleanup application | Static Data Mutation | Not Started | No Tests | Needs Verification | Java mutates item template masks from cleanup data, but `GeneralInfoBlobEntry` still queries cleanup data explicitly for the seal flag. C# ownership needs design before implementation. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Docs-only audit | Java item/general-info/static-data source review | Documents required C# inputs for future temporary-exchange and cleanup/seal serialization. | Source inspection only. | No C# implementation, no tests, no generated Java artifacts. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Temporary-exchange and expiration are wall-clock dependent and need deterministic clock or artifact-normalized expected seconds.
- Cleanup/seal flag must come from cleanup-table data, not only template masks.
- Adding static-data dependencies to packet serializers could broaden constructor/API surface if not planned carefully.
- Warehouse-add byte comparison remains guarded by runtime artifact absence and remaining blob gaps.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 0 source artifacts; 1 read-only general-info input audit document
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: temporary-exchange model hydration, item cleanup dataholder, cleanup/seal serializer input, Java runtime artifact generation, warehouse-add byte comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete

# Next Work Options

## Recommended Sequential Task

- Task: Add a read-only plume tempering stat payload audit.
- Scope:
  - inspect Java `EnchantInfoBlobEntry` plume branch and `PlumStatEnum`;
  - compare with C# `TemperingTable`, `ItemTemplateSummary.TemperingName`, and `InventoryItem.RandomPlumeBonus`;
  - identify whether C# has enough inputs to serialize the plume stat pairs without adding new static-data dependencies.

## Safe Parallel Candidates

- Java tooling task: generate the first unusual-storage runtime artifact in a Maven/JDK environment.
- Read-only static-data task: find the XML source for `item_restriction_cleanups` and assess C# loader ownership.
- Test helper task: add reusable blob-entry scanner helpers for packet tests.

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Agent A | Plume tempering payload audit | read-only Java plume/enchant files and C# tempering files | all writes |
| Agent B | Item cleanup XML/static-data source audit | read-only XML/static-data loader files | all writes |
| Orchestrator | Docs/parity integration | shared docs only after audits | production serializer files unless selected for next UOW |

## Do Not Parallelize

- General-info serializer implementation with static-data/model discovery.
- Warehouse-add byte comparison with any item-blob serializer changes.
- Shared progress/handoff docs between agents.
