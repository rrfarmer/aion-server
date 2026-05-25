# Phase 6OJ Completion Handoff - Decompose Java Packet Observer Design

Date: May 25, 2026
Unit of Work: UOW-888
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-888] Design decompose packet observer`)

## Status

Phase 6 is still in progress. This unit added a docs-only Java packet-observer design for producing selectable-decompose Level 2 Java artifacts.

No Java runtime artifact was captured in this environment. The design narrows the recommended temporary Java diagnostic to a `PacketSendUtility.sendPacket(Player, AionServerPacket)` observer, with reflection-based decoded field extraction and explicit artifact metadata. Java remains the source of truth and parity is not verified.

## Files Changed

- `docs/Phase-6-Decompose-Java-Packet-Observer-Design.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6OJ-Completion.md`

## What Changed

- Added `docs/Phase-6-Decompose-Java-Packet-Observer-Design.md`.
- Defined the recommended temporary Java observer hook at `PacketSendUtility.sendPacket(Player, AionServerPacket)`.
- Documented single-player/scenario filters and the allowed selectable-decompose packet set.
- Documented reflection-based decoded field extraction for:
  - `SM_ITEM_USAGE_ANIMATION`
  - `SM_SYSTEM_MESSAGE`
  - `SM_INVENTORY_UPDATE_ITEM`
  - `SM_DELETE_ITEM`
  - `SM_CUBE_UPDATE`
  - `SM_SECONDARY_SHOW_DECOMPOSABLE`
  - `SM_INVENTORY_ADD_ITEM`
- Added `capture_method_details` metadata guidance for future Java JSON artifacts.
- Defined explicit item-id and object-id mapping names for future Java artifacts.
- Called out the important Java source finding that `ItemPacketService.sendStorageUpdatePacket` sends `SM_INVENTORY_ADD_ITEM` and then `SM_CUBE_UPDATE`, so future runtime artifacts must record whether selectable reward add emits that trailing cube update.
- Added pass/fail gates and stop conditions for observer-generated artifacts.
- No production Java/C# code changed.

## Tests

No tests were run because this was a documentation-only design unit.

Latest known validation remains from UOW-887:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests --no-restore
```

Result: passed, 32 tests.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore
```

Result: passed, 1453 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SELECT_DECOMPOSABLE` | `Aion.GameServer.Network.Aion.ClientPackets.CmSelectDecomposable` / selectable-decompose tests | Client Packet Handler | Partial | Regression Tested in C#; Manual Only for observer design | Partial Parity | Java source reviewed for send order and no-op branches. Observer design does not run Java or compare artifacts. Invalid index, missing selectable data, reward RNG, and runtime ordering remain unverified. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `Aion.GameServer.Network.Aion.GameServerConnection` send/broadcast helpers | Utility | Partial | Manual Only | Needs Verification | Recommended observer hook is `sendPacket(Player, AionServerPacket)` before the real connection send. Broadcast fanout/threading remains unverified; known-list must be empty for first captures. |
| `com.aionemu.gameserver.network.aion.AionConnection` | `Aion.GameServer.Network.Aion.GameServerConnection` | Game Connection | Partial | Manual Only | Needs Verification | Design avoids first-hooking `AionConnection` because recipient context is easier at `PacketSendUtility`. Encryption/frame bytes and dispatcher ordering remain unverified. |
| `com.aionemu.gameserver.network.aion.AionServerPacket` | `Aion.GameServer.Network.Aion.GameServerPacket` | Packet Serialization | Partial | Manual Only | Needs Verification | Observer records reflected packet fields before Java serialization. This does not verify byte layout, `writeImpl` byte order, or encrypted frame behavior. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION` | `Aion.GameServer.Network.Aion.ServerPackets.SmItemUsageAnimation` | Server Packet | Partial | Regression Tested in C#; Manual Only for observer design | Partial Parity | Field extraction plan covers private Java defaults including `unk2 = 1` and `unk3 = 1`. No runtime artifact or bytes exist yet. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` | Server Packet | Partial | Regression Tested in C#; Manual Only for observer design | Partial Parity | Design maps only reviewed decompose message ids to factory names. Broader factory/reflection coverage remains partial. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` | Server Packet | Partial | Regression Tested in C#; Manual Only for observer design | Partial Parity | Field extraction plan records source item count and update type. Full `ItemInfoBlob` and byte serialization remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_DELETE_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmDeleteItem` | Server Packet | Partial | Regression Tested in C#; Manual Only for observer design | Partial Parity | Field extraction plan records object id and delete type. Runtime Java artifact still needed. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE` | `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate` | Server Packet | Partial | Regression Tested in C#; Manual Only for observer design | Partial Parity | Design requires recording delete-path cube update and any reward-add trailing cube update. Existing C# guarded comparison may need follow-up if Java runtime confirms extra packets. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SECONDARY_SHOW_DECOMPOSABLE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSecondaryShowDecomposable` | Server Packet | Partial | Regression Tested in C#; Manual Only for observer design | Partial Parity | Field extraction plan records source object id and reward count. Non-empty selectable-list display remains outside first artifacts. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_ADD_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryAddItem` | Server Packet | Partial | Regression Tested in C#; Manual Only for observer design | Partial Parity | Design records add type, reward ids/counts, slot, cloth flag, and generated object ids. Object-id comparison support remains a follow-up. |
| `com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryInfo.WriteItemInfoBlob` | Serialization Utility | Partial | Regression Tested in C#; Manual Only for observer design | Needs Verification | Observer records high-level item fields but does not decode or compare full Java blob bytes. Serialization, optional entries, and equipment/temporary-data fields remain unverified. |
| `com.aionemu.gameserver.services.item.ItemPacketService` | `Aion.GameServer.Services.Items` packet writers / connection item side effects | Service / Packet Side Effects | Partial | Regression Tested in C#; Manual Only for observer design | Partial Parity | Java source shows delete sends `SM_DELETE_ITEM` then `SM_CUBE_UPDATE`; storage add sends `SM_INVENTORY_ADD_ITEM` then `SM_CUBE_UPDATE`. Runtime artifacts are required before adjusting C# expectations. |
| `com.aionemu.gameserver.services.item.ItemService` | `Aion.GameServer.Network.Aion.GameServerConnection.SendDecomposeRewardItemsAsync` / item services | Service | Partial | Regression Tested in C#; Manual Only for observer design | Needs Verification | Observer design records reward packet side effects but does not verify Java item creation, `IDFactory` allocation, expirable registration, transaction behavior, or persistence. |
| `com.aionemu.gameserver.model.items.storage.Storage` | `Aion.GameServer` inventory mutation helpers | Storage | Partial | Regression Tested in C#; Manual Only for observer design | Needs Verification | Observer design relies on Java storage/service side effects for source decrement/delete. Persistence, quest callbacks, null handling, and transaction behavior remain unverified. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Documentation / Java Design | Java source review of selectable decompose handler, send utility, item packet services, storage, and server packets | Defines a temporary Java observer for Level 2 artifact generation. | Static source inspection only. | No Java observer was implemented or run; no artifact exists; no C# comparison against Java runtime output. |

## Remaining Risks

- Java runtime capture remains blocked locally by missing Java 25/Maven tooling.
- The observer design uses reflection against private packet fields; this is acceptable for diagnostics but not production parity.
- Reflection-derived fields do not prove byte-level serialization parity.
- Java `SM_INVENTORY_ADD_ITEM` storage update may emit a trailing `SM_CUBE_UPDATE`; current C# guarded comparison may need follow-up once real runtime evidence confirms the sequence.
- Generated reward object ids need explicit artifact mappings and C# comparison support before object-id fields can be compared safely.
- Full `ItemInfoBlob` contents, encrypted frames, dispatcher/threading order under real load, persistence, and live-client behavior remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 15
- Total artifacts ported: 0 production code artifacts; 1 Java packet-observer design document added
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 15
- Total blocked artifacts: 8 blocked/not-started categories, including Java observer implementation, Java runtime artifact generation, Java loopback proof validation, SQL fixture automation, object-id comparison support, full item-info blob comparison, byte capture, and live-client validation
- Estimated overall migration completion: Phase 6 remains about 66% complete

## Next Recommended Unit of Work

If Java 25/Maven tooling is available, implement the temporary `PacketSendUtility.sendPacket` observer in a Java-capable capture branch and generate:

- `docs/parity-artifacts/java/decompose/selectable/JD-SEL-DEC-001.json`
- `docs/parity-artifacts/java/decompose/selectable/JD-SEL-DEL-001.json`

If tooling remains blocked, the next smallest code unit is object-id mapping/comparison support in `GameServerConnectionInventoryExpansionUseItemTests.cs` using the mapping names from the observer design:

- `logical_source_object_id`
- `java_source_object_id`
- `logical_reward_index_0_object_id`
- `java_reward_index_0_object_id`
- `logical_reward_index_1_object_id`
- `java_reward_index_1_object_id`

Alternative docs-only unit: add a SQL fixture appendix for the live-server capture runbook.

## Safe Parallel Work Candidates

| Candidate | Files | Parallel Safe? | Notes |
|---|---|---|---|
| Java observer implementation | Java observer/hook files plus artifact output | No | Requires Java tooling, runtime control, and one owner for diagnostic patch and capture. |
| Object-id comparison support | `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs` | No | Shared comparison helper; do sequentially. |
| SQL fixture appendix | docs only | Yes | Can proceed independently if no progress/handoff docs are edited concurrently. |
| Artifact schema review | docs only | Yes | Can audit `capture_method_details` and mapping names without production changes. |

## Do Not Parallelize

- `GameServerConnectionInventoryExpansionUseItemTests.cs` with other decompose/projection edits.
- Java observer implementation with live-server artifact capture unless one owner controls both.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, `docs/Phase-6-Decompose-Java-Capture-Contract.md`, `docs/Phase-6-Decompose-Live-Server-Capture-Runbook.md`, `docs/Phase-6-Java-Loopback-Capture-Design.md`, `docs/Phase-6-Decompose-Java-Packet-Observer-Design.md`, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting subagents.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, choose object-id mapping/comparison support or SQL fixture appendix work.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
