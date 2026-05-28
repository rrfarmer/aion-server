# Phase 6AQM Completion - ItemCharge Selected Storage Audit

Date: 2026-05-28
Unit of Work: UOW-1619
Status: Complete after focused validation

## Scope

This unit audited the remaining ItemCharge selected-item storage-location question from earlier work. The question was whether C# still incorrectly required selected charge targets to be cube items and thereby differed from Java for equipped items.

The conclusion is conservative: no code change is warranted in this unit. Java selected charge lookup uses inventory storage object-id lookup, and Java equipped items remain inventory-backed items with an equipped flag. C# `HandleChargeItemAsync` filters selected targets by `Location == 0` and does not reject `IsEquipped`, matching the intended selected-equipped behavior already covered by regression tests.

## Completed Work

- Reviewed Java `CM_CHARGE_ITEM.runImpl`.
- Reviewed Java `ItemChargeService.chargeItems` and `chargeItem`.
- Reviewed Java `Equipment.equipItem` and `getEquippedItemByObjId`.
- Reviewed Java `StorageType.CUBE`, `ItemStorage.getItemByObjId`, and `Storage.getItemByObjId`.
- Reviewed C# `GameServerConnection.HandleChargeItemAsync` selected target lookup.
- Reviewed existing selected-equipped C# regression coverage.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, Migration Parity Table, risks, metrics, and next-unit guidance.

## Validation

Focused ItemCharge/game packet tests passed:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ItemChargeServiceTests|FullyQualifiedName~GamePacketTests|FullyQualifiedName~HandleChargeItemAsync_SelectedEquippedItemCanBeChargedLikeJavaInventoryLookup"
```

Result: 256 passed, 0 failed.

A broader class-level filter including all `GameServerConnectionInventoryExpansionUseItemTests` initially failed in unrelated `ProcessPacketAsync_CompositeStonesSendsConsumedPacketsInJavaOrderForMixedDeletes`; rerunning that exact test passed 1 test.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| ItemCharge selected-item storage lifecycle audit | read-only Java/C# charge files and docs | Low | Yes | Confirms no code change is warranted for selected equipped lookup. |
| Nearby controller-position/map-region adapter | nearby adapter/report files | Medium | No | Safe only if kept metadata-only. |
| Live nearby refresh dispatch | world/connection services | High | No | Deferred; requires production timing and socket behavior. |
| Java protection serializer implementation | Java serializer/generated artifacts | High | No | Blocked by Java tooling/runtime artifact strategy. |

No sub-agent was spawned because this was read-only analysis plus orchestrator-owned documentation.

## Tests Referenced

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `HandleChargeItemAsync_SelectedEquippedItemCanBeChargedLikeJavaInventoryLookup` | Existing / referenced | Selected equipped item with `Location == 0` can be charged, remains equipped, and sends expected update/stat packets. | Source-derived from Java `CM_CHARGE_ITEM.runImpl` and `Equipment.equipItem` inventory-backed lookup. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_CHARGE_ITEM` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleChargeItemAsync`; `CmChargeItem` | Client Packet Handler | Partial | Regression Tested | Partial Parity | Java resolves selected item ids through `player.getInventory().getItemByObjId`. C# selected targets require `Location == CubeStorageId` and allow equipped items. Live targeting/audit behavior and Java runtime comparison remain unverified. |
| `com.aionemu.gameserver.services.item.ItemChargeService` | `Aion.GameServer.Services.ItemChargeService` | Service | Partial | Regression Tested + Unit Tested | Partial Parity | Audit found no selected storage lookup change needed. Full Java side effects, packet ordering, persistence timing, and AP/stat observers remain partial. |
| `com.aionemu.gameserver.model.gameobjects.player.Equipment` | `InventoryItem.IsEquipped` plus selected handler lookup | Model / Equipment State | Partial | Regression Tested through selected charge handler | Needs Verification | Java equipment references the same inventory-backed item object. C# uses an inventory item equipped flag; full object identity/runtime comparison remains missing. |
| `com.aionemu.gameserver.model.items.storage.StorageType` | `GameServerConnection.CubeStorageId` / `InventoryItem.Location` | Enum / Storage Identifier | Partial | Regression Tested through handlers | Partial Parity | Java `StorageType.CUBE.getId()` is 0; C# uses `CubeStorageId = 0`. Other storage types are outside this unit. |
| `com.aionemu.gameserver.model.items.storage.ItemStorage` / `Storage` | `Player.InventoryItems` object-id lookup patterns | Storage / Collection | Partial | Manual Source Audit + Regression Tested selected charge | Needs Verification | Java uses `ConcurrentHashMap` object-id lookup. C# uses list scans and replacement; collection ordering, concurrency, and mutation identity remain unverified. |

## Remaining Risks

- No new code change was made; this unit documents the audit result.
- Java inventory/equipment object identity is not fully modeled in C#.
- Java storage uses concurrent maps; C# handler paths use list snapshots and replacement.
- Live targeting/audit behavior for invalid target NPCs, packet ordering, persistence timing, AP side effects, and stats observer fanout remain partial.
- Serialization, encrypted packet bytes, date/time handling, and Java runtime comparisons remain unverified.
- `docs/commit-conventions.md` was requested by startup flow but is absent in this repository.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped rows.
- Total artifacts ported: 0 code artifacts; 1 completed read-only parity audit with docs.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 2 grouped rows explicitly marked Needs Verification.
- Total blocked artifacts: Java runtime selected-charge comparison, full inventory/equipment object identity comparison, concurrent storage mutation comparison, encrypted packet/frame comparison, live persistence timing comparison.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Next best unit:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| ItemCharge persistence/packet ordering audit | Java `ItemChargeService.chargeItems`/`chargeItem`; C# handler/repository path | Compare success and failed payment ordering before any code change. |
| Nearby controller-position/map-region adapter | nearby adapter/report files if narrow | Keep metadata-only; no production sends. |
| Java protection serializer implementation | Java serializer/generated artifacts | Use only if Java tooling/runtime artifact strategy is ready. |

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1619] Audit item charge selected storage lookup
```

Files changed in this unit:

- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6AQM-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
