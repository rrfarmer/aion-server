# Phase 6 AIK Completion - World Loot Cleanup-Seal Flag Source

Date: 2026-05-27
Unit of Work: UOW-1409
Status: live world NPC loot item collection now computes cleanup/seal flag context from static data.

## Completed

- Threaded `ItemRestrictionCleanupTable?` through `WorldNpcLootService.RequestDropItem`.
- Updated `GameServerConnection.HandleLootItemAsync` to pass `StaticData.ItemRestrictionCleanups` into world-loot item collection.
- Updated world-loot item-collect add and stack-merge update packet construction so `SmInventoryAddItem.CreateItemCollect` and `SmInventoryUpdateItem.IncreaseItemCollect` receive Java's cleanup/seal general-info flag.
- Added focused world-loot service packet tests for restricted solo-loot add and restricted stack merge carrying cleanup/seal flag `3`.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~WorldNpcLootServiceTests.RequestDropItem|FullyQualifiedName~GamePacketTests.SmInventoryAddItem_WritesCleanupSealFlagInItemBlobLikeJava|FullyQualifiedName~GamePacketTests.SmInventoryUpdateItem_WritesCleanupSealFlagInItemBlobLikeJava|FullyQualifiedName~GamePacketTests.GameServerConnection_GetGeneralInfoWarehouseRestrictionFlag_UsesJavaCleanupPredicate"`.
- Result: passed 11 tests.

## Migration Parity Table - UOW-1409

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.drop.DropService.requestDropItem` | `Aion.GameServer.Services.WorldNpcLootService.RequestDropItem` and `Aion.GameServer.Network.Aion.GameServerConnection.HandleLootItemAsync` | Service / Packet Orchestration | Partial | Regression Tested | Partial Parity | Direct solo world-loot collection now passes `StaticData.ItemRestrictionCleanups` into the C# service and computes cleanup/seal flag `3` for restricted item add/update packets. Team/group distribution, kinah split behavior, quality announcements, temp-trade checks, and pet auto-sell remain incomplete or deferred in the C# loot service. |
| `com.aionemu.gameserver.services.item.ItemService.addItem` / `com.aionemu.gameserver.model.items.storage.Storage.add` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryAddItem.CreateItemCollect` and `SmInventoryUpdateItem` as consumed by world loot | Service / Packet Orchestration | Partial | Regression Tested | Partial Parity | World-loot add/update sends now supply the explicit Java-shaped flag to existing packet wrappers. Broader `Storage.add` side effects such as quest callbacks, cube update fanout, persistence, and all remaining caller families remain incomplete. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_ADD_ITEM` item-collect branch | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryAddItem.CreateItemCollect` | Packet | Partial | Regression Tested | Partial Parity | New world-loot service test verifies restricted solo-loot item-add packets carry cleanup/seal flag `3`. Java runtime byte comparison remains unavailable. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` full-blob branch | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` | Packet | Partial | Regression Tested | Partial Parity | New world-loot service test verifies restricted stack-merge loot updates carry cleanup/seal flag `3`. Partial update families such as charge/polish remain intentionally excluded because they do not write `GENERAL_INFO`. |
| `com.aionemu.gameserver.dataholders.ItemRestrictionCleanupData` | `Aion.GameServer.Dataholders.ItemRestrictionCleanupTable` and `WorldNpcLootService.GetGeneralInfoWarehouseRestrictionFlag` | Dataholder / Utility | Partial | Unit Tested | Partial Parity | World loot uses the same Java cleanup predicate through the C# cleanup table, with a local private helper to keep the service independent of `GameServerConnection`. Missing static data still falls back to flag `0`. |
| `com.aionemu.gameserver.network.aion.iteminfo.GeneralInfoBlobEntry` cleanup/seal flag | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteGeneralInfoBlob` | Serialization Entry | Partial | Regression Tested | Partial Parity | World loot can now feed the Java-shaped field through inventory add/update packets. Temporary-exchange remaining seconds, runtime conditioning presence, and time-dependent fields remain unresolved. |
| `com.aionemu.gameserver.questEngine.handlers.models.XmlQuest` / custom reward item paths | future quest/custom reward item packet callers | Service / Packet Orchestration | Partial | No Tests | Needs Verification | Quest/custom reward item paths remain a likely next cleanup/seal caller family. They were not changed in this unit and need a read-only audit before wiring broader quest reward plumbing. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `WorldNpcLootServiceTests.RequestDropItem_AddsRestrictedSoloItemWithCleanupSealFlag` | Service regression / packet serialization | `DropService.requestDropItem`, `ItemService.addItem`, `SM_INVENTORY_ADD_ITEM`, `GeneralInfoBlobEntry` | Solo loot collection emits an item-collect add packet for restricted item `182400002` with cleanup/seal flag `3`. | C# service-level packet assertion using Java-shaped cleanup row and deterministic packet parsing. | Does not compare against Java runtime bytes; loot side effects beyond direct solo item collection are not covered. |
| `WorldNpcLootServiceTests.RequestDropItem_MergesRestrictedSoloItemWithCleanupSealFlag` | Service regression / packet serialization | `DropService.requestDropItem`, `Storage.increaseItemCount`, `SM_INVENTORY_UPDATE_ITEM`, `GeneralInfoBlobEntry` | Solo loot collection merges into an existing stack and emits `IncreaseItemCollect` with cleanup/seal flag `3`. | C# service-level packet assertion using Java-shaped cleanup row and deterministic packet parsing. | Does not compare against Java runtime bytes; persistence and quest callbacks are not covered. |
| `WorldNpcLootServiceTests.RequestDropItem_*` | Service regression | `DropService.requestDropList`, `requestDropItem`, `closeDropList`, and corpse cleanup paths | Existing focused loot tests still cover visible drop-list refresh, last-item close, team distribution deferral, limit-one rejection, decay resume, and corpse deletion. | Source-derived C# assertions. | Team/roll/pet/temp-trade/announcement Java behaviors remain incomplete. |
| `GamePacketTests.SmInventoryAddItem_WritesCleanupSealFlagInItemBlobLikeJava` | Regression / packet byte layout | `SM_INVENTORY_ADD_ITEM.writeItemInfo`, `GeneralInfoBlobEntry` | Existing guard that item-collect inventory add packets write cleanup/seal field `3` when supplied. | C# packet-byte assertion against deterministic Java source order/field value. | Not a Java runtime comparison. |
| `GamePacketTests.SmInventoryUpdateItem_WritesCleanupSealFlagInItemBlobLikeJava` | Regression / packet byte layout | `SM_INVENTORY_UPDATE_ITEM.writeImpl`, `GeneralInfoBlobEntry` | Existing guard that normal full-blob inventory update packets write cleanup/seal field `3` when supplied. | C# packet-byte assertion against deterministic Java source order/field value. | Not a Java runtime comparison. |

## Remaining Risks

- Quest/custom reward paths, other inventory add/update callers, warehouse-add live callers, and any remaining future pet-feed/live paths still need cleanup-table or precomputed cleanup/seal context.
- World-loot tests use synthetic C# static-data fixtures rather than generated Java runtime packet artifacts.
- `WorldNpcLootService` still covers direct solo item collection only; Java team item distribution, kinah split, announcements, temporary-trade restrictions, pet auto-sell, and broader drop-aware corpse flows remain incomplete.
- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Temporary-exchange remaining seconds still serialize as zero.
- Runtime conditioning presence and wall-clock expiration/dye fields can still differ from Java.

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: 2 C# caller/service surfaces changed or consumed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 7 grouped rows
- Total blocked artifacts: quest/custom reward cleanup flag sources, remaining inventory add/update caller flag sources, warehouse-add/pet-feed live flag source, Java runtime artifact generation, team/roll/pet/temp-trade loot behaviors, temporary-exchange model/template/drop/task fields, time-normalized expiration/dye comparison
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Work Options

## Recommended Sequential Task

- Task: audit quest/custom reward cleanup/seal flag callers, then wire the smallest concrete reward item packet path.
- Scope:
  - inspect Java quest/custom reward item-add paths and current C# reward packet senders;
  - identify the narrowest caller with deterministic item-template and cleanup-table access;
  - thread `ItemRestrictionCleanupTable?` or a precomputed flag into that packet construction path;
  - add focused tests for restricted reward add/update packets without claiming Java runtime byte parity.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Quest/custom reward cleanup-seal caller audit | read-only quest reward services, `GameServerConnection.cs`, Java quest reward handlers | Medium | Best next work; audit first because quest reward plumbing is broad. |
| B | Selectable decompose cleanup-seal assertion | `GameServerConnectionInventoryExpansionUseItemTests.cs` only | Low | Optional explicit coverage; code path already shares the sender wired in UOW-1408. |
| C | Remaining inventory add/update caller search | read-only `rg` over `SmInventoryAddItem` and `SmInventoryUpdateItem` call sites | Low | Useful to refresh the cleanup-seal queue before choosing the next implementation unit. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Agent A | Read-only quest/custom reward flag-source design | read-only quest reward C# and Java classes | all writes, shared docs |
| Agent B | Read-only remaining inventory caller search | read-only C# packet call sites and tests | all writes, shared docs |

## Do Not Parallelize

- Do not edit broad quest reward services and shared connection tests from multiple workers at once.
- Do not edit shared progress/handoff/parity docs from sub-agents; orchestrator owns docs.
- Do not claim verified parity for world loot until there is Java runtime packet evidence or generated artifact comparison.

## Context For Next Session

- Current phase: Phase 6, cleanup/seal full item-blob flag propagation across live inventory-producing callers.
- Last completed UOW: UOW-1409.
- Last commit planned: `[Phase 6][UOW-1409] Wire world loot cleanup seal flag source`.
- Key changed files in this handoff:
  - `dotnetConversion/src/Aion.GameServer/Services/WorldNpcLootService.cs`
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/WorldNpcLootServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCleanupSealStaticDataAudit.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCSharpBlobGapAudit.md`
  - `docs/Phase-6AIK-Completion.md`
