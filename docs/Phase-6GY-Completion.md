# Phase 6GY Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6GX and covers Session 695.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter StorageExpansionNpcServiceTests`
  - Result: Passed, 7 tests.
- Latest packet/service validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "StorageExpansionNpcServiceTests|GamePacketTests"`
  - Result: Passed, 89 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1260 tests.

## Recent Work Completed

### Session 695 - Storage Expansion Warning Response Slice

- Added `CmDialogSelect.ExtendInventory = 47` and `CmDialogSelect.ExtendCharWarehouse = 48`.
- Added `SmQuestionWindow.WarehouseExpandWarning = 900686`.
- Added `SmInventoryUpdateItem.DecreaseKinahCube = 0x5A`.
- Added `SmSystemMessage.WarehouseExpandNotEnoughMoney = 1300831`.
- Added `PendingStorageExpansionRequest` and `QuestionResponseRequestKind.StorageExpansion`.
- Added represented storage-expansion template DTO/table types.
- Added `StorageExpansionNpcService` for cube/warehouse warning request plans and shared question-response handling.
- Routed `CM_QUESTION_RESPONSE` id `900686` through `GameServerConnection`.
- Added enter-world/login cleanup for pending storage expansion requests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.CubeExpandService.expandCube` | `Aion.GameServer.Services.StorageExpansionNpcService.RequestCubeExpansion` / `HandleResponse` | Service / Request Handler | Partial | Regression Tested | Needs Verification | Models template-gated warning question, response removal, not-enough-Kinah handling, represented `NpcExpands` mutation, Kinah decrement update type `0x5A`, cube-size packet, and success message. Static `DataManager.CUBEEXPANDER_DATA` loading, production dialog request routing, min/max NPC messages, DAO persistence, cube-limit recalculation side effects, logging, and live socket ordering are not complete. |
| `com.aionemu.gameserver.services.WarehouseService.expandWarehouse` | `Aion.GameServer.Services.StorageExpansionNpcService.RequestWarehouseExpansion` / `HandleResponse` | Service / Request Handler | Partial | Regression Tested | Needs Verification | Models template-gated warning question, response removal, not-enough-Kinah handling, represented `WarehouseNpcExpands` mutation, default Kinah decrement update type, warehouse-info refresh packets, and success message. Static `DataManager.WAREHOUSEEXPANDER_DATA` loading, production dialog request routing, min/max NPC messages, DAO persistence, warehouse-limit recalculation, and live socket ordering are not complete. |
| `com.aionemu.gameserver.dataholders.CubeExpandData` | `Aion.GameServer.Dataholders.StorageExpansionTemplateTable` | Dataholder Dependency | Partial | Unit Tested indirectly | Needs Verification | Provides represented NPC-id lookup shape for expansion templates. XML/static-data loading from `storage_expander/cube_expander.xml`, JAXB `afterUnmarshal` duplicate behavior, count parity, and DataManager wiring are not implemented in this unit. |
| `com.aionemu.gameserver.dataholders.WarehouseExpandData` | `Aion.GameServer.Dataholders.StorageExpansionTemplateTable` | Dataholder Dependency | Partial | Unit Tested indirectly | Needs Verification | Provides represented NPC-id lookup shape for expansion templates. XML/static-data loading from `storage_expander/warehouse_expander.xml`, JAXB `afterUnmarshal` duplicate behavior, count parity, and DataManager wiring are not implemented in this unit. |
| `com.aionemu.gameserver.model.templates.StorageExpansionTemplate` | `Aion.GameServer.Dataholders.StorageExpansionTemplateSummary` | DTO / Static Data Template | Partial | Regression Tested | Needs Verification | Represents NPC ids, min/max expansion levels, and per-level price lookup. XML parsing, schema validation, duplicate level behavior, and Java runtime comparison remain unverified. |
| `com.aionemu.gameserver.model.templates.expand.Expand` | `Aion.GameServer.Dataholders.StorageExpansionPrice` | DTO | Partial | Regression Tested | Needs Verification | Represents `level` and `price` values. Precision/rounding is straightforward integer handling, but XML load and Java JAXB comparison were not run. |
| `com.aionemu.gameserver.model.gameobjects.player.ResponseRequester.putRequest/respond/denyAll` | `QuestionResponseRegistry` with `QuestionResponseRequestKind.StorageExpansion` | Request Registry | Partial | Regression Tested | Needs Verification | Uses put-if-absent duplicate protection and response removal for shared question id `900686`. Java anonymous `RequestResponseHandler<Npc>` callback identity, generic/reflection behavior, logout `denyAll` callback behavior, and concurrent-map stress remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW.STR_WAREHOUSE_EXPAND_WARNING` | `Aion.GameServer.Network.Aion.ServerPackets.SmQuestionWindow.WarehouseExpandWarning` | Server Packet / Question Id | Partial | Regression Tested | Needs Verification | Question id `900686` and price payload are asserted in C# packet tests. Java golden bytes, encrypted frames, and client rendering were not compared. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_WAREHOUSE_EXPAND_NOT_ENOUGH_MONEY` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.WarehouseExpandNotEnoughMoney` | Server Packet / System Message | Partial | Regression Tested | Needs Verification | Java id `1300831` is emitted for insufficient Kinah during cube/warehouse expansion. Packet id asserted in C#; Java golden bytes/encrypted frames not compared. |
| `com.aionemu.gameserver.services.item.ItemPacketService.ItemUpdateType.DEC_KINAH_CUBE` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem.DecreaseKinahCube` | Packet Update Type | Partial | Regression Tested | Needs Verification | Java update type `0x5A` is used for cube NPC expansion Kinah decrement and asserted from serialized C# inventory-update payload. Java packet bytes and persistence side effects were not compared. |
| `com.aionemu.gameserver.services.item.ItemPacketService.ItemUpdateType.DEC_KINAH_BUY` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem.DecreaseKinahBuy` | Packet Update Type | Partial | Regression Tested | Needs Verification | Existing update type `0x1D` is used for warehouse NPC expansion default Kinah decrement. Java packet bytes and persistence side effects were not compared. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` expand actions | `Aion.GameServer.Network.Aion.ClientPackets.CmDialogSelect.ExtendInventory` / `ExtendCharWarehouse` | Client Packet / Handler Dependency | Partial | No new production route tests | Needs Verification | Dialog action constants `47` and `48` are represented, but production dialog request routing is deferred until storage-expander static data is loaded. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_QUESTION_RESPONSE` | `GameServerConnection.HandleStorageExpansionQuestionResponseAsync` | Client Packet / Handler | Partial | Regression Tested indirectly | Needs Verification | Response id `900686` now dispatches to the storage-expansion response service when a pending request exists. No end-to-end production dialog test exists because request creation/static-data loading are still missing. |
| `com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData.setNpcExpands/setWhNpcExpands` | `Player.NpcExpands` / `Player.WarehouseNpcExpands` setters | Runtime Model Dependency | Partial | Regression Tested | Needs Verification | C# now allows represented NPC expansion fields to mutate in memory. Java common-data persistence, cube/warehouse limit recalculation, serialization differences, and threading behavior remain unverified. |

## Tests Added Or Updated

- `StorageExpansionNpcServiceTests.RequestCubeExpansion_RegistersJavaSharedQuestion`
- `StorageExpansionNpcServiceTests.RequestWarehouseExpansion_DuplicateQuestionKeepsOriginalPendingRequest`
- `StorageExpansionNpcServiceTests.RequestExpansion_CannotExpandEmitsJavaCapMessages`
- `StorageExpansionNpcServiceTests.HandleResponse_DenyConsumesPendingRequestWithoutMutation`
- `StorageExpansionNpcServiceTests.HandleResponse_AcceptCubeDecreasesKinahAndExpandsNpcCubeRows`
- `StorageExpansionNpcServiceTests.HandleResponse_AcceptWarehouseDecreasesKinahAndExpandsNpcWarehouseRows`
- `StorageExpansionNpcServiceTests.HandleResponse_NotEnoughKinahConsumesPendingRequestWithoutExpansion`
- `GamePacketTests` system-message and question-window assertions for ids `1300831` and `900686`.

These tests are source-derived from Java. They do not compare against Java runtime execution, golden bytes, encrypted frames, static XML loader behavior, production dialog request routing, DAO persistence, reflection behavior, date/time behavior, or live client behavior.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 14
- Total artifacts ported or partially modeled in this handoff window: 1 storage expansion warning/response slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 14
- Total blocked/not-started artifacts: static storage-expander XML loading, production dialog request routing, NPC expansion persistence, min/max NPC failure messages, storage limit recalculation, RequestResponseHandler callback identity, socket-order validation, encrypted-frame comparison, and client validation.
- Estimated overall migration completion: 65%

## Remaining Risks

- `storage_expander/cube_expander.xml` and `storage_expander/warehouse_expander.xml` are not loaded into C# `StaticData` yet.
- Production `CM_DIALOG_SELECT` request routing for action ids `47` and `48` is not wired because template lookup is not available.
- NPC expansion persistence to `players.npc_expands` and `players.wh_npc_expands` is not implemented in this unit.
- Java min/max NPC-specific failure messages are not represented yet.
- Java cube/warehouse limit recalculation is represented only by mutable fields and outgoing packets; deeper storage object limit state is missing.
- Packet-byte, encrypted-frame, production socket-order, packet-capture, and real-client validation remain unperformed.

## Next Recommended Unit of Work

Load Java storage-expander XML into C# `StaticData` as `CubeExpansionTemplates` and `WarehouseExpansionTemplates`, then wire `CM_DIALOG_SELECT` actions `47` and `48` through `StorageExpansionNpcService.Request*Expansion` with targeting/function validation. Keep DAO persistence and min/max NPC-specific messages as explicit follow-up work unless they fit cleanly after the loader is in place.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6GX-Completion.md`
   - this handoff
3. Inspect selected Java source and nearest C# tests before touching code.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Create the next handoff document and commit the unit.
