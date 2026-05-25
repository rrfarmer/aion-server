# Phase 6OK Completion Handoff - Decompose Object-Id Mapping Comparison

Date: May 25, 2026
Unit of Work: UOW-889
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-889] Add decompose object id mapping`)

## Status

Phase 6 is still in progress. This unit extended the guarded selectable-decompose Java artifact comparison so future Java artifacts can use real runtime object ids while comparing against the deterministic C# fixture object ids.

No Java runtime artifact was captured in this environment. The new support is comparison-readiness only and is verified against synthetic live-server-shaped JSON.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6OK-Completion.md`

## What Changed

- Added `CompareSelectableDecomposeJavaArtifacts_WithDeclaredObjectIdMapping_NormalizesMappedObjectIds`.
- Extended the synthetic Java artifact builder to declare source and reward object-id mappings under `fixture.id_mapping`.
- The guarded comparison now compares:
  - client packet `payload_fields.object_id`
  - `SM_ITEM_USAGE_ANIMATION.decoded_fields.item_object_id`
  - source `object_id` in `SM_INVENTORY_UPDATE_ITEM` and `SM_DELETE_ITEM`
  - `SM_SECONDARY_SHOW_DECOMPOSABLE.decoded_fields.source_object_id`
  - generated reward `SM_INVENTORY_ADD_ITEM.decoded_fields.object_id`
- The existing mapping normalizer handles declared Java object ids exactly like declared Java item ids.
- Unmapped Java numeric values still compare exactly.
- No production Java/C# code changed.

## Tests

Focused:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests --no-restore
```

Result: passed, 33 tests.

Full:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore
```

Result: passed, 1454 tests.

Added/updated tests:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `CompareSelectableDecomposeJavaArtifacts_WithDeclaredObjectIdMapping_NormalizesMappedObjectIds` | Guarded comparison accepts different Java source/reward object ids only when `fixture.id_mapping` explicitly maps them to logical C# fixture object ids. | Uses synthetic contract-shaped JSON; no Java runtime artifact yet. |
| `CompareSelectableDecomposeJavaArtifacts_WhenPresent_ComparesContractFields` | Future Java artifacts must include comparable source/reward object-id fields along with existing item/count/type fields. | Guarded path is ready, but Java artifacts are absent locally. |

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SELECT_DECOMPOSABLE` | `Aion.GameServer.Network.Aion.ClientPackets.CmSelectDecomposable` / guarded comparison helper | Client Packet Handler | Partial | Regression Tested; Java Artifact Comparison Guard Added | Partial Parity | Comparison now checks selectable source object id in the client payload with explicit mapping support. Java runtime artifacts remain absent, so parity is not verified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION` | `Aion.GameServer.Network.Aion.ServerPackets.SmItemUsageAnimation` | Server Packet | Partial | Regression Tested; Java Artifact Comparison Guard Added | Partial Parity | Guard now compares mapped `item_object_id` in addition to item id/time/end/unknown3. Java constructor defaults and bytes still need runtime artifact evidence. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` | Server Packet | Partial | Regression Tested; Java Artifact Comparison Guard Added | Partial Parity | Guard now compares mapped source `object_id` on the decrement path. Full blob serialization and Java byte order remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_DELETE_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmDeleteItem` | Server Packet | Partial | Regression Tested; Java Artifact Comparison Guard Added | Partial Parity | Guard now compares mapped source `object_id` on the delete path. Runtime Java artifact is still missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SECONDARY_SHOW_DECOMPOSABLE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSecondaryShowDecomposable` | Server Packet | Partial | Regression Tested; Java Artifact Comparison Guard Added | Partial Parity | Guard now compares mapped `source_object_id` for the secondary clear packet. Non-empty selectable-list display remains outside the first artifact comparison. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_ADD_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryAddItem` | Server Packet | Partial | Regression Tested; Java Artifact Comparison Guard Added | Partial Parity | Guard now compares mapped generated reward `object_id` plus reward item id/count/slot/cloth flag. Java `IDFactory` allocation and trailing cube update behavior remain unverified. |
| `com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteItemInfoBlob` | Serialization Utility | Partial | Regression Tested; Java Artifact Comparison Guard Added | Partial Parity | Touched only through source/reward decoded field comparison. Full blob serialization, optional entries, and equipment/temporary fields remain unverified. |
| `com.aionemu.gameserver.services.item.ItemPacketService` | `Aion.GameServer.Services.Items` packet writers / connection item side effects | Service / Packet Side Effects | Partial | Regression Tested; Java Artifact Comparison Guard Added | Partial Parity | Comparison is now ready for mapped object ids emitted by Java item packet services. Runtime Java sequence, including possible reward-add trailing `SM_CUBE_UPDATE`, remains unverified. |
| `com.aionemu.gameserver.services.item.ItemService` | `Aion.GameServer.Network.Aion.GameServerConnection.SendDecomposeRewardItemsAsync` / item services | Service | Partial | Regression Tested; Java Artifact Comparison Guard Added | Needs Verification | Object-id mapping support prepares for generated reward ids but does not verify Java item creation, `IDFactory`, expirable registration, or persistence behavior. |
| `com.aionemu.gameserver.model.items.storage.Storage` | `Aion.GameServer` inventory mutation helpers | Storage | Partial | Regression Tested; Java Artifact Comparison Guard Added | Needs Verification | Source object-id comparison now covers the emitted mutation packets, but Java storage persistence, quest callbacks, null behavior, and transaction behavior remain unverified. |

## Remaining Risks

- Java runtime capture remains blocked locally because Java 25 JDK and Maven are unavailable; neither the loopback proof nor packet observer has been run.
- Java artifact files do not exist yet, so object-id normalization has only been tested against synthetic JSON.
- The id/object-id mapping remains numeric and broad; future artifacts should keep mappings precise to avoid masking unrelated numeric mismatches.
- Future Java artifacts may include a reward-add trailing `SM_CUBE_UPDATE`; current comparison still expects the existing C# packet order until runtime evidence requires a focused parity fix.
- Byte-level payload/frame parity, full item-info blob parity, Java `IDFactory` allocation, persistence, threading/order under real dispatcher load, and live-client behavior remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 10
- Total artifacts ported: 0 production code artifacts; 1 object-id mapping/comparison path added to guarded comparison tests
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 10
- Total blocked artifacts: 8 blocked/not-started categories, including Java observer implementation, Java runtime artifact generation, Java loopback proof validation, SQL fixture automation, reward-add trailing cube update parity, full item-info blob comparison, byte capture, and live-client validation
- Estimated overall migration completion: Phase 6 remains about 66% complete

## Next Recommended Unit of Work

If Java 25/Maven tooling is available, implement the temporary Java packet observer or run the loopback proof to generate:

- `docs/parity-artifacts/java/decompose/selectable/JD-SEL-DEC-001.json`
- `docs/parity-artifacts/java/decompose/selectable/JD-SEL-DEL-001.json`

If tooling remains blocked, good next units are:

- Add a SQL fixture appendix for the live-server capture runbook.
- Add a guarded comparison mode for optional Java-observed reward-add trailing `SM_CUBE_UPDATE` once the artifact shape is documented.
- Continue another isolated Phase 6 gameplay slice outside the decompose capture path if it does not touch shared decompose test helpers.

## Safe Parallel Work Candidates

| Candidate | Files | Parallel Safe? | Notes |
|---|---|---|---|
| Java observer implementation | Java observer/hook files plus artifact output | No | Runtime capture and diagnostic patch should have one owner. |
| SQL fixture appendix | docs only | Yes | Can proceed independently if no progress/handoff docs are edited concurrently. |
| Optional reward-add cube-update comparison design | docs or same decompose test file | Docs yes; test no | Test helper changes should remain sequential. |
| Artifact schema review | docs only | Yes | Can audit mapping/capture metadata independently. |

## Do Not Parallelize

- `GameServerConnectionInventoryExpansionUseItemTests.cs` with other decompose/projection edits.
- Java observer implementation with live-server artifact capture unless one owner controls both.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, `docs/Phase-6-Decompose-Java-Capture-Contract.md`, `docs/Phase-6-Decompose-Live-Server-Capture-Runbook.md`, `docs/Phase-6-Java-Loopback-Capture-Design.md`, `docs/Phase-6-Decompose-Java-Packet-Observer-Design.md`, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting subagents.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, choose SQL fixture appendix work, optional reward-add cube-update comparison design, or an isolated non-decompose Phase 6 gameplay slice.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
