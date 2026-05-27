# Phase 6 AIA Completion - Live Read-Mail Cleanup-Seal Flag Source

Date: 2026-05-27
Unit of Work: UOW-1399
Status: live read-mail attached-item packet construction now computes cleanup/seal flag context from static data.

## Completed

- Added `GameServerConnection.GetGeneralInfoWarehouseRestrictionFlag` to centralize Java's `ItemRestrictionCleanupData.hasAccountOrLegionWhStorabilityDisabled` predicate for live callers.
- Updated the `CmReadMail` branch to resolve `StaticData` once, preserve existing `ItemTemplates` behavior, and pass the computed cleanup/seal flag into `SmMailService.CreateReadPacket`.
- Preserved default-zero fallback when static data, cleanup rows, or attached item ids are unavailable.
- Added focused helper coverage for restricted, unrestricted, item-id-zero, and missing-cleanup-table cases.
- Captured read-only audit results:
  - mail read was the smallest live runtime caller for this unit;
  - broker-buy inventory add is the next narrow live caller;
  - temporary exchange remains a separate model/template/drop/task UOW.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~StaticDataLoadingTests"`.
- Result: passed 20 tests.
- First parallel `GamePacketTests` run failed with a transient .NET build lock on `Aion.GameServer.dll` while the static-data run was compiling.
- Reran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests --filter "FullyQualifiedName~GamePacketTests"`.
- Result: passed 238 tests.
- Ran `git diff --check`; only line-ending warnings were reported.

## Migration Parity Table - UOW-1399

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.mail.MailService.readMail` | `Aion.GameServer.Network.Aion.GameServerConnection` `CmReadMail` branch | Service / Packet Orchestration | Partial | Unit Tested | Partial Parity | Live C# read-mail now computes Java's cleanup/seal general-info flag from `StaticData.ItemRestrictionCleanups` and passes it into `SmMailService.CreateReadPacket`. Socket-level Java byte comparison remains unavailable. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_MAIL_SERVICE` read-letter attached item path | `Aion.GameServer.Network.Aion.ServerPackets.SmMailService` | Packet | Partial | Regression Tested | Partial Parity | UOW-1398 packet support is now consumed by the live read-mail caller. Existing packet tests verify explicit flag `3`; this unit adds caller-side flag decision coverage. |
| `com.aionemu.gameserver.dataholders.ItemRestrictionCleanupData` | `Aion.GameServer.Dataholders.ItemRestrictionCleanupTable` and `GameServerConnection.GetGeneralInfoWarehouseRestrictionFlag` | Dataholder / Utility | Partial | Unit Tested | Partial Parity | Helper returns `3` only when item id is nonzero and the cleanup table reports account or legion warehouse storage disabled. Missing table degrades to `0`. |
| `com.aionemu.gameserver.network.aion.iteminfo.GeneralInfoBlobEntry` cleanup/seal flag | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteGeneralInfoBlob` | Serialization Entry | Partial | Regression Tested | Partial Parity | Live mail read can now feed the field through packet construction. Temporary-exchange remaining seconds, runtime conditioning presence, and wall-clock fields remain unresolved. |
| `com.aionemu.gameserver.services.broker.BrokerService` buy path / `SM_INVENTORY_ADD_ITEM` result | future `GameServerConnection.HandleBuyBrokerItemAsync` cleanup flag input | Service / Packet Orchestration | Partial | No Tests | Needs Verification | Read-only audit identified broker-buy inventory add as the next narrow live caller still defaulting restricted items to flag `0`. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GamePacketTests.GameServerConnection_GetGeneralInfoWarehouseRestrictionFlag_UsesJavaCleanupPredicate` | Unit | `ItemRestrictionCleanupData.hasAccountOrLegionWhStorabilityDisabled`, `GeneralInfoBlobEntry` | Restricted cleanup row returns flag `3`; unrestricted row, item id `0`, and missing cleanup table return `0`. | Deterministic Java source predicate and C# assertions. | Does not exercise socket-level `CmReadMail`; packet serialization was covered in UOW-1398. |
| `GamePacketTests.CharacterSelectionServerPackets_WriteJavaShapedPayloads` attached-mail restricted item assertion | Regression / packet byte layout | `SM_MAIL_SERVICE.writeLetterRead`, `ItemInfoBlob.getFullBlob`, `GeneralInfoBlobEntry` | Existing guard that read-mail attached item blobs write cleanup/seal field `3` when the caller supplies it. | C# packet-byte assertion. | Not a Java runtime byte comparison. |
| `StaticDataLoadingTests` cleanup coverage | Static-data regression | `StaticData.itemCleanup`, `ItemRestrictionCleanupData` | Existing guard that C# loads cleanup rows and preserves Java predicate/defaults. | C# load/predicate assertions over Java-shaped data. | Not a live DataManager runtime comparison. |

## Remaining Risks

- Broker-buy, other inventory add/update, warehouse-add, loot, and future pet-feed/live callers still need cleanup-table or precomputed cleanup/seal context.
- No socket-level `CmReadMail` integration test yet asserts the outbound `SmMailService` blob through `GameServerConnection`.
- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Temporary-exchange remaining seconds still serialize as zero.
- Runtime conditioning presence and wall-clock expiration/dye fields can still differ from Java.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 2 C# caller/helper surfaces changed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: broker-buy cleanup flag source, remaining inventory add/update caller flag sources, warehouse-add/pet-feed live flag source, Java runtime artifact generation, temporary-exchange model/template/drop/task fields, time-normalized expiration/dye comparison
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Work Options

## Recommended Sequential Task

- Task: wire broker-buy inventory-add runtime cleanup/seal flag sourcing.
- Scope:
  - update `GameServerConnection.HandleBuyBrokerItemAsync`;
  - compute `GetGeneralInfoWarehouseRestrictionFlag(boughtItem.ItemId, staticData?.ItemRestrictionCleanups)`;
  - pass the result to `SmInventoryAddItem.CreateBrokerBuy`;
  - add focused broker-buy packet/caller coverage.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Broker-buy live caller implementation | `GameServerConnection.cs`, focused tests | Low | Narrow call site already identified by read-only audit. |
| B | Temporary exchange model UOW audit-to-implementation prep | `InventoryItem.cs`, `ItemTemplateTable.cs`, static-data tests | Medium | Separate from cleanup/seal; keep serializer changes scoped. |
| C | Remaining inventory update/add caller map | read-only `GameServerConnection.cs`, loot/broker services | Low | Useful before broad caller sweep. |

## Do Not Parallelize

- Do not edit `GameServerConnection.cs` from multiple workers at once.
- Do not edit shared progress/handoff/parity docs from sub-agents; orchestrator owns docs.
- Do not change `SmInventoryInfo.cs` for cleanup/seal while runtime caller flag sourcing is still being finished.
