# Phase 6 AHT Completion - Cleanup/Seal Static-Data Ownership Audit

Date: 2026-05-27
Unit of Work: UOW-1392
Status: Read-only cleanup/seal static-data ownership audit complete. No serializer code changed.

## Completed

- Added `docs/Phase-6-BindPointTeleport-PetFeedUnusualStorageCleanupSealStaticDataAudit.md`.
- Located Java static-data import and schema paths for `item_restriction_cleanups`.
- Reviewed Java `StaticData.itemCleanup`, `DataManager.ITEM_CLEAN_UP`, `ItemRestrictionCleanupData`, `ItemCleanupTemplate`, and `GeneralInfoBlobEntry`.
- Confirmed the packet predicate is item id match plus `awh == 0 || lwh == 0`.
- Confirmed the current Java data contains item id `188053996` with both `awh` and `lwh` disabled.
- Reviewed C# `StaticData` and `SmInventoryInfo` ownership boundaries.

## Validation

- Ran read-only source inspection.
- Ran `git diff --check`.
- No C# tests were required because this unit is docs-only.

## Migration Parity Table - UOW-1392

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.dataholders.StaticData.itemCleanup` | future `Aion.GameServer.Dataholders.StaticData.ItemRestrictionCleanups` | Static Data Root | Not Started | No Tests | Needs Verification | Java JAXB binds `<item_restriction_cleanups>` from the static-data import graph. C# loader sees the import graph but does not project this element yet. |
| `com.aionemu.gameserver.dataholders.DataManager.ITEM_CLEAN_UP` | future runtime access to `StaticData.ItemRestrictionCleanups` | Static Data Service | Not Started | No Tests | Needs Verification | Java exposes the table globally. C# should prefer explicit dependency flow into packet construction rather than serializer global lookup. |
| `com.aionemu.gameserver.dataholders.ItemRestrictionCleanupData` | future `Aion.GameServer.Dataholders.ItemRestrictionCleanupTable` | Dataholder | Not Started | No Tests | Needs Verification | Predicate is item-id match and `awh == 0 || lwh == 0`. Missing list behaves empty through `getList()`, but current Java predicate directly streams `bplist`. |
| `com.aionemu.gameserver.model.templates.restriction.ItemCleanupTemplate` | future `Aion.GameServer.Dataholders.ItemRestrictionCleanupSummary` | DTO | Not Started | No Tests | Needs Verification | Optional byte attributes default to `-1`; `trade`, `sell`, and `wh` are not used for the packet cleanup/seal flag. |
| `com.aionemu.gameserver.network.aion.iteminfo.GeneralInfoBlobEntry` cleanup/seal flag | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteGeneralInfoBlob` | Serialization Entry | Partial | Manual Only | Needs Verification | C# still writes zero. Future implementation should pass an explicit flag/boolean to avoid global static-data coupling. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Docs-only audit | Java source and XML/static-data inspection | Documents exact cleanup/seal data source, predicate, and recommended C# ownership. | Source inspection only. | No C# dataholder, parser, or packet serializer test exists yet. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- C# still lacks `ItemRestrictionCleanupTable` and packet flag plumbing.
- Temporary-exchange time, runtime conditioning presence, and time-dependent expiration/dye fields still block full warehouse-add byte comparison.
- Packet call sites without static-data access will need a default-zero bridge or deterministic flag input.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 0 source artifacts; 1 read-only audit document
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: cleanup/seal C# dataholder/parser, packet flag plumbing, Java runtime artifact generation, temporary-exchange model hydration, runtime conditioning presence, time-normalized expiration/dye comparison
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Work Options

## Recommended Sequential Task

- Task: implement C# cleanup/seal static-data projection and focused table tests.
- Scope:
  - add `ItemRestrictionCleanupTable` / summary DTO;
  - parse `item_restriction_cleanups` rows from the static-data import graph;
  - expose the table on `StaticData`;
  - test Java default byte behavior and `awh == 0 || lwh == 0` predicate.

## Safe Parallel Candidates

- Temporary exchange model audit: trace Java callers that set `temporaryExchangeTime` and plan the C# inventory-item field/hydration path.
- Broker plume context audit: inspect how Java broker packet paths access item templates for tempered plume stat pairs.
- Java tooling task: generate the first unusual-storage runtime artifact in a Maven/JDK environment.

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Agent A | Cleanup/seal dataholder/parser implementation | `StaticData.cs`, new dataholder files, static-data loading tests | packet serializers until table tests pass |
| Agent B | Temporary exchange source audit | read-only Java item/exchange source and C# repository/model files | all writes |
