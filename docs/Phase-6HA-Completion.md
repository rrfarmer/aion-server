# Phase 6HA Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6GZ and covers Session 697.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "StorageExpansionNpcServiceTests|GamePacketTests"`
  - Result: Passed, 91 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1263 tests.

## Recent Work Completed

### Session 697 - Storage Expansion NPC Min/Max Messages

- Added Java NPC-specific cube/warehouse min/max expansion system-message factories:
  - `SmSystemMessage.InventoryCantExtendBelowNpcMinimum` / Java id `1300436`,
  - `SmSystemMessage.InventoryCantExtendAboveNpcMaximum` / Java id `1300437`,
  - `SmSystemMessage.WarehouseCantExtendBelowNpcMinimum` / Java id `1300438`,
  - `SmSystemMessage.WarehouseCantExtendAboveNpcMaximum` / Java id `1300439`.
- Updated `StorageExpansionNpcService.RequestCubeExpansion` and `RequestWarehouseExpansion` so min/max template failures are handled with Java-visible packets rather than returning not-handled.
- Source-derived NPC-name parameter uses `ChatUtil.L10n(npc.Template.NameId)` when available, matching Java `npc.getObjectTemplate().getL10n()`. A raw-name fallback remains for incomplete represented templates with no `NameId`.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.CubeExpandService.expandCube` | `Aion.GameServer.Services.StorageExpansionNpcService.RequestCubeExpansion` | Service / Request Planner | Partial | Regression Tested | Needs Verification | Request-side NPC min/max branches now emit represented Java system messages. Still missing DAO persistence, common-data save, deeper cube-limit storage object recalculation, Java runtime/golden comparison, and live socket-order validation. |
| `com.aionemu.gameserver.services.WarehouseService.expandWarehouse` | `Aion.GameServer.Services.StorageExpansionNpcService.RequestWarehouseExpansion` | Service / Request Planner | Partial | Regression Tested | Needs Verification | Request-side NPC min/max branches now emit represented Java system messages. Still missing DAO persistence, common-data save, warehouse-limit storage object recalculation, Java runtime/golden comparison, and live socket-order validation. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_EXTEND_INVENTORY_CANT_EXTEND_DUE_TO_MINIMUM_EXTEND_LEVEL_BY_THIS_NPC` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.InventoryCantExtendBelowNpcMinimum` | Server Packet / System Message | Partial | Regression Tested | Needs Verification | C# serialized packet id `1300436` and parameter order are asserted. The level parameter uses Java's `minExpansionLevel - 1`. Java golden bytes, encrypted frames, localized client rendering, and runtime comparison were not performed. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_EXTEND_INVENTORY_CANT_EXTEND_MORE_DUE_TO_MAXIMUM_EXTEND_LEVEL_BY_THIS_NPC` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.InventoryCantExtendAboveNpcMaximum` | Server Packet / System Message | Partial | Regression Tested | Needs Verification | C# serialized packet id `1300437` and parameter order are asserted. Cube maximum uses `Math.Min(template.MaxExpansionLevel, NPC_CUBE_EXPANDS_SIZE_LIMIT)` per Java. Java golden bytes, encrypted frames, localized client rendering, and runtime comparison were not performed. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_EXTEND_CHAR_WAREHOUSE_CANT_EXTEND_DUE_TO_MINIMUM_EXTEND_LEVEL_BY_THIS_NPC` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.WarehouseCantExtendBelowNpcMinimum` | Server Packet / System Message | Partial | Regression Tested | Needs Verification | C# serialized packet id `1300438` and parameter order are asserted. The level parameter uses Java's `minExpansionLevel - 1`. Java golden bytes, encrypted frames, localized client rendering, and runtime comparison were not performed. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_EXTEND_CHAR_WAREHOUSE_CANT_EXTEND_MORE_DUE_TO_MAXIMUM_EXTEND_LEVEL_BY_THIS_NPC` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.WarehouseCantExtendAboveNpcMaximum` | Server Packet / System Message | Partial | Regression Tested | Needs Verification | C# serialized packet id `1300439` and parameter order are asserted. Warehouse maximum uses template max level per Java. Java golden bytes, encrypted frames, localized client rendering, and runtime comparison were not performed. |
| `com.aionemu.gameserver.model.templates.L10n.getL10n` / `NpcTemplate.getL10nId` | `Aion.GameServer.Utils.ChatUtil.L10n` / `NpcTemplateSummary.NameId` | Localization Dependency | Partial | Regression Tested indirectly | Needs Verification | Storage expansion NPC-name parameters now use represented client l10n tokens when `NameId > 0`. Fallback to raw template name is a C# defensive difference for incomplete test/static summaries; real Java NPC templates require `name_id`. |
| `com.aionemu.gameserver.model.templates.StorageExpansionTemplate.getMinExpansionLevel/getMaxExpansionLevel/getPrice` | `StorageExpansionTemplateSummary.MinExpansionLevel` / `MaxExpansionLevel` / `GetPrice` | DTO Dependency | Partial | Regression Tested | Needs Verification | Existing min/max/price shape now drives handled failure packets. Duplicate level behavior, malformed XML behavior, Java JAXB comparison, and full real-data parity remain unverified. |
| `com.aionemu.gameserver.configs.main.CustomConfig.NPC_CUBE_EXPANDS_SIZE_LIMIT` | `GameServerOptions.Custom.NpcCubeExpandsSizeLimit` argument to `RequestCubeExpansion` | Config Dependency | Partial | Regression Tested indirectly | Needs Verification | Cube NPC max failure uses the configured NPC cube cap in the request planner. Property-file override loading and Java config initialization timing were not revalidated in this unit. |

## Tests Added Or Updated

- `GamePacketTests` system-message assertions for ids `1300436`, `1300437`, `1300438`, and `1300439`.
- `StorageExpansionNpcServiceTests.RequestExpansion_BelowNpcMinimumEmitsJavaNpcSpecificMessages`.
- `StorageExpansionNpcServiceTests.RequestExpansion_AboveNpcMaximumEmitsJavaNpcSpecificMessages`.

These tests are source-derived from Java. They do not compare against Java runtime execution, Java-generated golden bytes, encrypted frames, live-client localization/rendering, production socket ordering, reflection behavior, threading behavior, date/time behavior, or full real-data parity.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 9
- Total artifacts ported or partially modeled in this handoff window: 1 storage-expansion NPC min/max failure-message slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 9
- Total blocked/not-started artifacts: NPC expansion persistence, storage limit recalculation, full real-data comparison, end-to-end dialog tests, golden/encrypted packet comparison, and live-client validation.
- Estimated overall migration completion: 65%

## Remaining Risks

- NPC expansion persistence to `players.npc_expands` and `players.wh_npc_expands` is still missing.
- Storage object cube/warehouse limit recalculation remains represented by fields and outgoing packets only.
- Full real `storage_expander` XML count parity and duplicate NPC id overwrite behavior remain unverified.
- End-to-end production dialog tests for action ids `47` and `48` remain missing.
- Java golden packet bytes, encrypted frames, localized client rendering, and live client validation remain unperformed.
- The C# fallback from missing `NameId` to raw NPC name is intentionally defensive for incomplete represented templates; real static-data parity depends on NPC templates retaining Java `name_id` values.

## Next Recommended Unit of Work

Add persistence support for accepted NPC expansion mutations (`npc_expands`, `wh_npc_expands`) so cube/warehouse NPC expansion survives logout, then cover the repository boundary with source-derived tests. Keep full real XML count comparison and end-to-end dialog/socket validation as follow-up readiness work.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6GZ-Completion.md`
   - this handoff
3. Inspect selected Java source and nearest C# tests before touching code.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Create the next handoff document and commit the unit.
