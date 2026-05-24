# Phase 6HC Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6HB and covers Session 699.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "GameServerConnectionStorageExpansionDialogTests|StorageExpansionNpcServiceTests|NpcDialogTargetingServiceTests"`
  - Result: Passed, 16 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1267 tests.

## Recent Work Completed

### Session 699 - Storage Expansion Production Dialog Route Tests

- Made `GameServerConnection.HandleDialogSelectAsync` internal for test assembly coverage, matching the existing migrated-handler testing pattern.
- Added `GameServerConnectionStorageExpansionDialogTests`.
- Tests use a minimal Java-shaped static-data fixture for cube and warehouse expansion XML.
- Covered production dialog route behavior for:
  - `CM_DIALOG_SELECT` action `47` / `EXTEND_INVENTORY`,
  - `CM_DIALOG_SELECT` action `48` / `EXTEND_CHAR_WAREHOUSE`.
- Verified targeted NPC/function validation, loaded template lookup, pending storage request registration, response-requester registration, and shared Java question id `900686`.
- Verified unsupported NPC function ids produce no pending request and no emitted packet.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` | `Aion.GameServer.Network.Aion.ClientPackets.CmDialogSelect` / `GameServerConnection.HandleDialogSelectAsync` | Client Packet / Handler | Partial | Regression Tested | Needs Verification | Connection-level tests now cover Java action ids `47` and `48` through the production dialog route. Full parser-to-handler socket loop, encrypted client frame processing, and live client behavior remain unverified. |
| `com.aionemu.gameserver.services.DialogService` `EXTEND_INVENTORY` branch | `GameServerConnection.HandleDialogSelectAsync` action `CmDialogSelect.ExtendInventory` | Dialog Handler | Partial | Regression Tested | Needs Verification | Targeted NPC/function validation, loaded cube-expander template lookup, pending request registration, and `SM_QUESTION_WINDOW` emission are covered. Java controller dispatch stack, live known-list rules, and client socket ordering remain unverified. |
| `com.aionemu.gameserver.services.DialogService` `EXTEND_CHAR_WAREHOUSE` branch | `GameServerConnection.HandleDialogSelectAsync` action `CmDialogSelect.ExtendCharWarehouse` | Dialog Handler | Partial | Regression Tested | Needs Verification | Targeted NPC/function validation, loaded warehouse-expander template lookup, pending request registration, and `SM_QUESTION_WINDOW` emission are covered. Java controller dispatch stack, live known-list rules, and client socket ordering remain unverified. |
| `com.aionemu.gameserver.services.CubeExpandService.expandCube` | `StorageExpansionNpcService.RequestCubeExpansion` production caller | Service Integration | Partial | Regression Tested | Needs Verification | Production dialog route now feeds loaded cube templates into the service and registers the Java shared warning question. Accept response, persistence handoff, and NPC min/max messages are covered elsewhere; storage limit recalculation, Java runtime comparison, and live validation remain missing. |
| `com.aionemu.gameserver.services.WarehouseService.expandWarehouse` | `StorageExpansionNpcService.RequestWarehouseExpansion` production caller | Service Integration | Partial | Regression Tested | Needs Verification | Production dialog route now feeds loaded warehouse templates into the service and registers the Java shared warning question. Accept response, persistence handoff, and NPC min/max messages are covered elsewhere; storage limit recalculation, Java runtime comparison, and live validation remain missing. |
| `com.aionemu.gameserver.dataholders.CubeExpandData` | `Aion.GameServer.Dataholders.StaticData.CubeExpansionTemplates` / `StorageExpansionTemplateTable` | Dataholder Dependency | Partial | Regression Tested | Needs Verification | Minimal XML fixture validates production route lookup by NPC id. Full real XML count parity, duplicate-id overwrite behavior, JAXB default behavior, and Java runtime comparison remain unverified. |
| `com.aionemu.gameserver.dataholders.WarehouseExpandData` | `Aion.GameServer.Dataholders.StaticData.WarehouseExpansionTemplates` / `StorageExpansionTemplateTable` | Dataholder Dependency | Partial | Regression Tested | Needs Verification | Minimal XML fixture validates production route lookup by NPC id. Full real XML count parity, duplicate-id overwrite behavior, JAXB default behavior, and Java runtime comparison remain unverified. |
| `com.aionemu.gameserver.controllers.NpcController.onDialogSelect` / `NpcTemplate.supportsAction` | `NpcDialogTargetingService.ValidateTargetingNpcWithFunction` | Targeting / NPC Function Dependency | Partial | Regression Tested | Needs Verification | Dialog route test proves unsupported function ids reject without side effects. C# currently validates targeted world object/function id; full Java visible-object/known-list/controller stack and distance/race nuances remain incomplete. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW.STR_WAREHOUSE_EXPAND_WARNING` | `Aion.GameServer.Network.Aion.ServerPackets.SmQuestionWindow.WarehouseExpandWarning` | Server Packet / Question Id | Partial | Regression Tested | Needs Verification | Connection-level observer sees the shared Java question id `900686` emitted for both cube and warehouse dialog actions. Java golden bytes, encrypted frames, and real-client rendering were not compared. |

## Tests Added Or Updated

- `GameServerConnectionStorageExpansionDialogTests.HandleDialogSelectAsync_StorageExpansionActionsRegisterJavaWarningQuestion`
  - Validates production dialog route for both storage expansion actions.
  - Validates loaded template lookup, pending request shape, response requester count, and question id `900686`.
- `GameServerConnectionStorageExpansionDialogTests.HandleDialogSelectAsync_StorageExpansionRejectsUnsupportedNpcAction`
  - Validates unsupported NPC function ids do not register requests or send packets.

These tests are source-derived from Java. They do not compare against Java runtime execution, Java-generated golden bytes, encrypted client-frame parser-to-handler loop, full real XML count parity, threading behavior, reflection behavior, date/time behavior, socket-order capture, or live client behavior.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 9
- Total artifacts ported or partially modeled in this handoff window: 1 storage-expansion production dialog route coverage slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 9
- Total blocked/not-started artifacts: full real-data comparison, complete controller/known-list dispatch, golden/encrypted packet comparison, storage limit recalculation, and live-client/MySQL validation.
- Estimated overall migration completion: 65%

## Remaining Risks

- Full real `storage_expander` XML count parity and duplicate NPC id overwrite behavior remain unverified.
- Java controller/known-list dispatch is represented only by targeted world NPC/function validation.
- Java golden packet bytes, encrypted frames, localized client rendering, and live client validation remain unperformed.
- Storage object cube/warehouse limit recalculation remains represented only by fields and outgoing packets.
- Live MySQL write/readback for accepted NPC expansions remains unverified.

## Next Recommended Unit of Work

Add a real static-data count/lookup comparison for cube and warehouse expansion templates loaded from the repository XML, including representative NPC ids and min/max prices, or continue to storage-limit recalculation by modeling Java `player.setCubeLimit()` / `setWarehouseLimit()` effects beyond outgoing packets.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6HB-Completion.md`
   - this handoff
3. Inspect selected Java source and nearest C# tests before touching code.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Create the next handoff document and commit the unit.
