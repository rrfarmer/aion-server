# Phase 6GZ Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6GY and covers Session 696.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "StorageExpansionNpcServiceTests|StaticData_LoadsStorageExpansionTemplatesByNpcId"`
  - Result: Passed, 8 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1261 tests.

## Recent Work Completed

### Session 696 - Storage Expander Static Data And Dialog Routing

- Added `StaticData.CubeExpansionTemplates`.
- Added `StaticData.WarehouseExpansionTemplates`.
- Parsed Java `cube_expander` and `warehouse_expander` XML entries:
  - `expansion_npc ids`,
  - nested `expand level/price`,
  - flattened NPC-id lookup.
- Routed `CM_DIALOG_SELECT` action ids `47` and `48` through `StorageExpansionNpcService` with NPC targeting/function validation.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.dataholders.CubeExpandData` | `Aion.GameServer.Dataholders.StaticData.CubeExpansionTemplates` / `StorageExpansionTemplateTable` | Dataholder | Partial | Regression Tested | Needs Verification | Loads represented `cube_expander` entries into flattened NPC-id lookup table. Focused XML snippet covers multi-NPC ids and per-level price lookup. Full real XML count parity, duplicate-id overwrite behavior, schema validation, and Java JAXB runtime comparison remain unverified. |
| `com.aionemu.gameserver.dataholders.WarehouseExpandData` | `Aion.GameServer.Dataholders.StaticData.WarehouseExpansionTemplates` / `StorageExpansionTemplateTable` | Dataholder | Partial | Regression Tested | Needs Verification | Loads represented `warehouse_expander` entries into flattened NPC-id lookup table. Focused XML snippet covers multi-NPC ids and multi-level price lookup. Full real XML count parity, duplicate-id overwrite behavior, schema validation, and Java JAXB runtime comparison remain unverified. |
| `com.aionemu.gameserver.model.templates.StorageExpansionTemplate` | `Aion.GameServer.Dataholders.StorageExpansionTemplateSummary` | DTO / Static Data Template | Partial | Regression Tested | Needs Verification | Static loader now populates NPC ids and nested level/price rows from XML. Java JAXB field defaults, malformed XML behavior, duplicate level behavior, and runtime comparison remain unverified. |
| `com.aionemu.gameserver.model.templates.expand.Expand` | `Aion.GameServer.Dataholders.StorageExpansionPrice` | DTO | Partial | Regression Tested | Needs Verification | Static loader now populates integer `level` and `price` attributes. Precision/rounding differences are not expected for int values, but malformed/overflow XML handling and Java JAXB comparison remain unverified. |
| `com.aionemu.gameserver.services.DialogService` `EXTEND_INVENTORY` branch | `GameServerConnection.HandleDialogSelectAsync` action `CmDialogSelect.ExtendInventory` | Dialog Handler | Partial | Regression Tested indirectly | Needs Verification | Production dialog routing now validates target/function and requests cube expansion using loaded templates and configured limits. End-to-end dialog request tests, Java min/max NPC-specific failure messages, persistence, and live socket ordering remain missing. |
| `com.aionemu.gameserver.services.DialogService` `EXTEND_CHAR_WAREHOUSE` branch | `GameServerConnection.HandleDialogSelectAsync` action `CmDialogSelect.ExtendCharWarehouse` | Dialog Handler | Partial | Regression Tested indirectly | Needs Verification | Production dialog routing now validates target/function and requests warehouse expansion using loaded templates. End-to-end dialog request tests, Java min/max NPC-specific failure messages, persistence, and live socket ordering remain missing. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` expand actions | `Aion.GameServer.Network.Aion.ClientPackets.CmDialogSelect.ExtendInventory` / `ExtendCharWarehouse` | Client Packet / Constants | Partial | Regression Tested indirectly | Needs Verification | Constants `47` and `48` are now consumed by production request routing. Full packet parser/dialog flow and live client behavior were not compared. |
| `com.aionemu.gameserver.services.CubeExpandService.expandCube` | `StorageExpansionNpcService.RequestCubeExpansion` production caller | Service Integration | Partial | Regression Tested | Needs Verification | Request path now has loaded template input. Still missing NPC min/max failure system-message factories, DAO persistence, common-data save, cube-limit storage object recalculation, and golden/live validation. |
| `com.aionemu.gameserver.services.WarehouseService.expandWarehouse` | `StorageExpansionNpcService.RequestWarehouseExpansion` production caller | Service Integration | Partial | Regression Tested | Needs Verification | Request path now has loaded template input. Still missing NPC min/max failure system-message factories, DAO persistence, common-data save, warehouse-limit storage object recalculation, and golden/live validation. |
| `com.aionemu.gameserver.configs.main.CustomConfig.CUBE_EXPANSION_LIMIT` / `NPC_CUBE_EXPANDS_SIZE_LIMIT` | `GameServerOptions.Custom.CubeExpansionLimit` / `NpcCubeExpandsSizeLimit` | Config Dependency | Partial | Existing Config Tested | Needs Verification | Existing C# options are now passed into cube NPC expansion routing. This unit did not revalidate property override loading, Java static config initialization timing, or live config parity. |

## Tests Added Or Updated

- `StaticDataLoadingTests.StaticData_LoadsStorageExpansionTemplatesByNpcId`
- Existing `StorageExpansionNpcServiceTests` rerun against the DTO/table shape.

These tests are source-derived from Java. They do not compare against Java runtime execution, golden bytes, encrypted frames, full real XML count parity, duplicate-id runtime behavior, production dialog end-to-end flow, DAO persistence, reflection behavior, date/time behavior, or live client behavior.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 10
- Total artifacts ported or partially modeled in this handoff window: 1 storage-expander static-data/routing slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 10
- Total blocked/not-started artifacts: real XML count parity, duplicate-id runtime comparison, min/max NPC failure messages, NPC expansion persistence, storage limit recalculation, socket-order validation, and client validation.
- Estimated overall migration completion: 65%

## Remaining Risks

- Full real `storage_expander` XML count parity is not asserted yet.
- Duplicate NPC id overwrite behavior is source-inferred from dictionary construction but not tested against Java runtime.
- Java min/max NPC-specific expansion failure messages are still not represented.
- NPC expansion persistence to `players.npc_expands` and `players.wh_npc_expands` is still missing.
- Storage object cube/warehouse limit recalculation remains represented by fields and outgoing packets only.
- End-to-end production dialog tests and live client validation remain unperformed.

## Next Recommended Unit of Work

Add the Java min/max NPC-specific failure system messages for cube/warehouse expansion and cover request-side failure planning, or add persistence support for NPC expansion mutations (`npc_expands`, `wh_npc_expands`) so accepted NPC expansion survives logout. Keep live-client/golden packet validation as a later readiness pass.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6GY-Completion.md`
   - this handoff
3. Inspect selected Java source and nearest C# tests before touching code.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Create the next handoff document and commit the unit.
