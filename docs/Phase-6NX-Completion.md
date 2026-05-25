# Phase 6NX Completion Handoff - Java Decompose Harness Feasibility Audit

Date: May 25, 2026
Unit of Work: UOW-876
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-876] Audit Java decompose capture feasibility`)

## Status

Phase 6 is still in progress. This unit returned from C# crypto fallback work to the Java runtime comparison path and completed a read-only feasibility audit for selectable-decompose packet-order capture.

The key finding is conservative: a no-socket fake Java `AionConnection` is not a clean seam. Java `AConnection.sendPacket` is final, depends on private registered `SelectionKey` state, and `AionConnection` owns private final `Crypt` plus a scheduled heartbeat. Selectable-decompose capture is still feasible, but as an integration-style Java loopback socket harness or a live-server capture, not as a tiny fake-connection unit test.

## Files Changed

- `docs/Phase-6-Java-Decompose-Harness-Feasibility.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6NX-Completion.md`

## What Changed

- Added `docs/Phase-6-Java-Decompose-Harness-Feasibility.md`.
- Documented Parallel Work Discovery for Java runtime comparison workstreams.
- Audited:
  - `AConnection`
  - `AionConnection`
  - `AionServerPacket`
  - `CM_SELECT_DECOMPOSABLE`
  - `PacketSendUtility`
  - `Player`
  - `PlayerStorage`
  - `Storage`
  - `ItemService`
  - `ItemPacketService`
  - `ItemFactory`
  - `IDFactory`
  - `DecomposableItemsData`
- Documented the Java source-reviewed selectable-decompose packet order.
- Documented fixture blockers:
  - final/non-overridable Java send path
  - private `SelectionKey`
  - private final `Crypt`
  - scheduled heartbeat from connection construction
  - online-player requirement
  - known-list requirement
  - `DataManager` static-data requirement
  - `ItemFactory` / `IDFactory` / DAO initialization risk
- Updated `docs/PHASE-6-PROGRESS.md` with Session 876 parity table, risks, metrics, and next recommended UOW.

## Tests

No .NET tests were run because this was a documentation/source-audit-only unit with no production code or test code changes.

Previous full validation remains from Session 875:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --no-restore
```

Result: passed, 1450 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.commons.network.AConnection` | `Aion.Commons.Network` connection primitives / `Aion.GameServer.Network.Aion.GameServerConnection` | Connection Base | Partial | Manual Only | Needs Verification | Java `sendPacket` is final and touches private `SelectionKey`; this blocks a clean fake connection capture seam. C# socket behavior remains tested separately, but Java runtime comparison is not implemented. Reflection differences and threading/scheduler side effects are unresolved. |
| `com.aionemu.gameserver.network.aion.AionConnection` | `Aion.GameServer.Network.Aion.GameServerConnection` | Game Connection | Partial | Manual Only | Needs Verification | Java constructor schedules heartbeat and owns private final `Crypt`; queue capture likely needs real dispatcher/socket or fragile reflection. No Java runtime capture exists yet. Threading and encryption-state differences remain risks. |
| `com.aionemu.gameserver.network.aion.AionServerPacket` | `Aion.GameServer.Network.Aion.GameServerPacket` | Packet Serialization | Partial | Manual Only | Needs Verification | Java packet serialization requires an `AionConnection` because `write` calls `con.encrypt`. Broader frame-byte parity and deterministic Java crypt capture remain missing. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SELECT_DECOMPOSABLE` | `Aion.GameServer.Network.Aion.ClientPackets.CmSelectDecomposable` | Client Packet Handler | Partial | Regression Tested in C#; Manual Only for Java audit | Partial Parity | Java packet order was source-reviewed: usage animation, success message, source decrement/delete side effect, secondary clear, reward add. Runtime Java packet capture and byte comparison remain missing. Random reward count must be fixed by min=max fixture. |
| `com.aionemu.gameserver.model.gameobjects.player.Player` | `Aion.GameServer.Model.GameObjects.Player.Player` | Domain Model | Partial | Manual Only | Needs Verification | Java online state requires a non-null client connection; constructor initializes many real subsystems and leaves world position null. Harness needs explicit connection, known-list, and possibly position/world setup. |
| `com.aionemu.gameserver.model.items.storage.PlayerStorage` | `Aion.GameServer.Model.Items.Storage.PlayerStorage` / inventory services | Storage | Partial | Manual Only | Needs Verification | Java storage routes add/decrease/delete through owner-aware packet side effects. Source decrement/delete runtime capture remains unverified. |
| `com.aionemu.gameserver.model.items.storage.Storage` | `Aion.GameServer.Model.Items.Storage.Storage` / inventory services | Storage | Partial | Manual Only | Needs Verification | Java `decreaseByObjectId` updates item counts, persistent state, deleted-items queue, quest removal callback, and item packets. Persistence/DAO and side-effect parity are not verified by this audit. |
| `com.aionemu.gameserver.services.item.ItemService` | `Aion.GameServer.Services.Items.InventoryAddService` / item services | Service | Partial | Manual Only | Needs Verification | Java reward add uses `DataManager.ITEM_DATA`, `ItemFactory`, `IDFactory`, expirable registration, storage capacity, and item packet add/update side effects. Runtime fixture needs deterministic static data and ID allocation. |
| `com.aionemu.gameserver.services.item.ItemPacketService` | `Aion.GameServer.Services.Items` packet writers | Service / Packet Side Effects | Partial | Manual Only | Needs Verification | Java delete/update/add packet type selection was source-reviewed, including `ItemAddType.DECOMPOSABLE` and `ItemUpdateType.INC_ITEM_COLLECT`. Exact packet bytes and source-delete/decrement runtime order remain unverified. |
| `com.aionemu.gameserver.dataholders.DecomposableItemsData` | `Aion.GameServer.Data.StaticData` decomposable item data | Data Holder | Partial | Manual Only | Needs Verification | Java selectable data returns a copy, so handler filtering is isolated. Harness needs either real XML load or minimal reflective fixture data. Serialization/XML defaults were not verified in this unit. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `Aion.GameServer.Network.Aion.GameServerConnection` send/broadcast helpers | Utility | Partial | Manual Only | Needs Verification | Java `sendPacket` drops packets unless player is online; `broadcastPacketAndReceive` sends self before known-list players. Empty known-list fixture is required to make order deterministic. |
| `com.aionemu.gameserver.services.item.ItemFactory` | `Aion.GameServer.Services.Items.ItemFactory` / item creation services | Utility | Partial | Manual Only | Needs Verification | Java reward creation calls global `IDFactory` and `DataManager.ITEM_DATA`; this can pull DAO/database dependencies into an in-process harness. |
| `com.aionemu.gameserver.utils.idfactory.IDFactory` | `Aion.GameServer.Services.IdFactory` / object id allocation | Utility | Partial | Manual Only | Needs Verification | Java singleton initializes used IDs from multiple DAOs. Test fixture must avoid accidental production DB assumptions or explicitly configure a test DB. |

## Remaining Risks

- Java runtime comparison remains unimplemented.
- In-process capture may require fragile reflection against `AConnection.key`, `AionConnection.sendMsgQueue`, private final `Crypt`, or scheduler state.
- Live Java socket capture may require DB/static-data/player fixture setup beyond the current C# test fixtures.
- Java `IDFactory` and `ItemFactory` can pull DAO/database dependencies into reward-add capture.
- Decompose reward count must be deterministic (`min == max`) before comparing output.
- Encryption byte parity cannot be claimed until Java key generation/capture is controlled.
- Threading differences remain unresolved: Java dispatcher/packet processor/scheduler ordering differs from C# async/socket-loop execution.
- Serialization differences remain unresolved for item update/add/delete packet bytes.

## Summary Metrics

- Total Java artifacts discovered: 13
- Total artifacts ported: 0 code artifacts; 1 feasibility audit document added
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 13
- Total blocked artifacts: 8 blocked/not-started categories, including Java runtime harness, deterministic connection capture, Java crypt byte vectors, fixture DB/static-data setup, source decrement/delete runtime comparison, reward-add packet byte comparison, broadcast fanout comparison, and live-client validation
- Estimated overall migration completion: Phase 6 remains about 66% complete

## Next Recommended Unit of Work

Create the decompose Java capture fixture contract and artifact schema before writing harness code.

Suggested scope:

- Define scenario ids for selectable decrement and selectable delete variants.
- Define exact static-data fixture inputs:
  - source item template
  - reward item template
  - decomposable selectable data
  - min/max reward counts fixed for determinism
- Define player/account/inventory setup:
  - online connection requirements
  - known-list shape
  - source item object ids/counts
  - database/ID allocation assumptions
- Define capture artifact schema:
  - Java revision
  - packet class sequence
  - decoded important fields
  - optional unencrypted body bytes
  - optional encrypted frame bytes
  - unsupported fields/gaps
- Decide whether the first implementation should be Java loopback socket capture or live-server capture.

## Safe Parallel Work Candidates

| Candidate | Files | Parallel Safe? | Notes |
|---|---|---|---|
| Fixture contract document | docs only | Yes, but orchestrator-owned | Best next step; avoid writing harness code until pass/fail schema is explicit. |
| Java loopback socket design notes | docs only / read-only Java | Yes as analysis | Can map dispatcher/bootstrap requirements separately. |
| Live-server capture runbook draft | docs only | Yes as analysis | Useful fallback if loopback fixture proves too broad. |
| C# comparison fixture schema | docs or future test data | Maybe | Wait until Java artifact schema is stable. |

## Do Not Parallelize

- Progress and handoff docs.
- Java capture implementation with fixture contract changes in the same shared files.
- Any changes to Java networking/dispatcher production code.
- Any changes involving static `DataManager` setup plus C# comparison tests until ownership boundaries are clear.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, `docs/Phase-6-Java-Decompose-Harness-Feasibility.md`, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting subagents.
4. Use Java as source of truth and preserve breadcrumbs.
5. Start with the fixture contract/artifact schema for Java decompose capture.
6. Avoid harness implementation until the capture schema and fixture requirements are explicit.
7. Run focused and full tests for any code changes.
8. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
9. Create the next handoff and commit the completed unit.
