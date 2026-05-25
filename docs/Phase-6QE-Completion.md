# Phase 6QE Completion Handoff - ItemPurification Cube Snapshot Packets

Date: May 25, 2026
Unit of Work: UOW-935
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-935] Bridge ItemPurification cube snapshot packets`)

## Status

Phase 6 is still in progress. This unit adds a caller-provided cube snapshot bridge for ItemPurification packet plans. It deliberately does not read live storage, mutate inventory, allocate object ids, spend AP, write persistence, or wire `GameServerConnection`.

Java runtime artifact capture remains unavailable locally because this workstation has Java 8 and no Maven.

`docs/commit-conventions.md` is still missing; commit format follows `docs/orchestration-rules.md`.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmCubeUpdate.cs`
- `dotnetConversion/src/Aion.GameServer/Services/ItemPurificationPacketPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/ItemPurificationPacketPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6QE-Completion.md`

## What Changed

- Added `SmCubeUpdate.CubeSizeSnapshot`.
- Added optional `cubePacketInputsByPacketOperationIndex` to `ItemPurificationPacketPlanService.CreatePacketPlan`.
- Added `ItemPurificationCubePacketInput` with storage id/ordinal, expected source operation, expected object/item ids, item count, and expansion fields.
- Concrete cube packets are attached only when the packet-indexed snapshot matches the exact cube operation and expansion fields fit Java `writeC` byte range.
- Missing or invalid snapshots leave cube operations metadata-only.
- The existing concrete packet send adapter now sends cube packets in plan order when they are present.

## Parallel Work Discovery Summary

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Cube snapshot bridge | `SM_CUBE_UPDATE.cubeSize`, `ItemPacketService` cube follow-ups | packet plan service, cube packet DTO, focused tests | Integration Fix | Write sequential | Medium | Completed by orchestrator; shared packet-plan surface. |
| B | Cube keying/count analysis | `Storage.size`, `ItemStorage.size`, `StorageType` | read-only | Java/C# Analysis | Yes | Medium | Completed by read-only explorer; packet index recommended over object id. |
| C | Packet payload regression audit | `SM_CUBE_UPDATE.writeImpl` | read-only C# packet/tests | Test Analysis | Yes | Low | Completed by read-only explorer; snapshot factory payload test recommended. |
| D | ItemCharge AP fallback | AP spend callers | separate service/tests | Later | Medium | Deferred because cube snapshot bridge stayed bounded. |

## Tests

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "ItemPurificationPacketPlanServiceTests|GamePacketTests"
```

Result: passed, 103 tests.

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1600 tests.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE` | `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate.CubeSizeSnapshot` | Server Packet DTO | Partial | Regression Tested in C# | Partial Parity | Snapshot factory writes action `0`, cube ordinal `0`, item count, and expand bytes from explicit caller inputs. Java runtime byte comparison remains unavailable. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE.cubeSize` | `Aion.GameServer.Services.ItemPurificationPacketPlanService` plus `ItemPurificationCubePacketInput` | Packet Planner / Cube Bridge | Partial | Regression Tested in C# | Partial Parity | Concrete cube packets are attached only when caller provides packet-indexed post-mutation snapshots with matching source operation/object/item fields. Storage count source remains external and unverified. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemDeletePacket` | `ItemPurificationPacketPlanService.CreatePacketPlan` | Delete Fanout / Cube Follow-up | Partial | Regression Tested in C# | Partial Parity | Delete operations can now be followed by concrete cube packets in Java order when snapshots exist. Live delete mutation, quest removal hook, persistence, and runtime storage count capture remain missing. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket` | `ItemPurificationPacketPlanService.CreatePacketPlan` | Add Fanout / Cube Follow-up | Partial | Regression Tested in C# | Partial Parity | Target-add operations can now be followed by concrete cube packets in Java order when snapshots exist. Live `Storage.add`, quest item-get hook, persistence, and target slot allocation remain missing. |
| `com.aionemu.gameserver.model.items.storage.ItemStorage.size` | `ItemPurificationCubePacketInput.ItemsCount` | Storage Count Snapshot | Not Started for live storage capture | Manual Review + Regression Tested Consumer | Needs Verification | Java `size()` counts stored item objects after mutation. C# planner accepts explicit counts but does not compute or verify Java-equivalent live storage semantics. |
| `com.aionemu.gameserver.model.items.storage.StorageType` | `ItemPurificationPacketPlanService.CubeStorageTypeId` / `CubeStorageTypeOrdinal` | Enum / Constant Projection | Partial | Regression Tested in C# | Needs Verification | ItemPurification supports only Java `StorageType.CUBE` id/ordinal `0` for this bridge. Other storage types remain unsupported. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GamePacketTests` cube snapshot payload assertion | Regression | Java `SM_CUBE_UPDATE.writeImpl` source review | Validates `CubeSizeSnapshot(7,2,3,4)` writes `000007000000020304`. | Deterministic byte-level C# regression for Java packet field order. | Not generated from Java runtime. |
| `CreatePacketPlan_AttachesConcreteCubePacketsWhenRuntimeSnapshotsProvided` | Regression | Java `ItemPacketService` and `SM_CUBE_UPDATE.cubeSize` source review | Validates delete/add cube operations attach concrete `SmCubeUpdate` packets by packet index and preserve Java fanout order. | Deterministic C# regression with decoded payload checks. | Snapshot source is caller-provided, not live storage. |
| `CreatePacketPlan_LeavesCubeMetadataWhenRuntimeSnapshotDoesNotMatchOperation` | Regression | Java storage fanout source review | Validates mismatched source operation and out-of-byte-range expansion values leave cube operations metadata-only. | Deterministic safety regression. | Does not validate every malformed snapshot combination. |
| `SendConcretePacketsAsync_IncludesConcreteCubePacketsInPlanOrderWhenSnapshotsProvided` | Regression | Java `PacketSendUtility.sendPacket`, `ItemPacketService.sendItemDeletePacket`, and `sendStorageUpdatePacket` source review | Validates the send adapter includes concrete cube packets in plan order when snapshots are supplied. | Deterministic C# send-order regression. | No live handler wiring or Java runtime packet comparison. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Packet-index keying is intentionally order-sensitive; callers must generate snapshots from the same packet plan they send.
- The planner does not compute Java `Storage.size()` and cannot prove live storage count parity.
- Live ItemPurification mutation/emission, object-id allocation, `Storage.add`, delete persistence, quest hooks, AP/rank fanout, Kinah parity decision, and rollback/error behavior remain unimplemented.
- C# `SmCubeUpdate.CubeSize(Player)` currently counts non-kinah location-0 inventory items; Java `ItemStorage.size()` semantics for all live callers still need broader verification before claiming full cube parity.

## Summary Metrics

- Total Java artifacts discovered: 6
- Total artifacts ported: 1 narrow ItemPurification cube snapshot packet bridge slice
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 6
- Total blocked artifacts: 6 blocked/not-started categories, including Java runtime artifact generation, live ItemPurification mutation/emission, live cube snapshot capture, AP concrete fanout, object-id allocation/storage mutation, and repository/quest side effects
- Estimated overall migration completion: Phase 6 remains about 69% complete

## Next Recommended Unit of Work

Recommended sequential task:
- Add a narrow ItemPurification runtime snapshot assembler that takes an already-mutated item list/snapshot and produces packet-indexed `ItemPurificationInventoryPacketInput` plus `ItemPurificationCubePacketInput` without touching `GameServerConnection`, persistence, AP mutation, or object-id allocation.

Safe parallel candidates:

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Runtime snapshot assembler design | read-only planner/storage/item files | Medium | Decide whether assembler should be pure input projection over caller-supplied post-mutation item snapshots. |
| B | ItemCharge AP hardening fallback | ItemCharge service/test files | Medium | Independent AP caller work if snapshot assembler expands too much. |
| C | Live ItemPurification wiring audit | read-only `GameServerConnection`, purification planners | High | Analysis only; do not wire live handler until mutation/AP/persistence are scoped. |

Suggested parallel batch for the next session:

| Agent | Task | Allowed Files | Forbidden Files | Expected Result |
|---|---|---|---|---|
| Agent A | Inspect pure snapshot assembler input/output shape | read-only C# planner/storage/item files | edits, docs | Recommendation for assembler API and keying. |
| Agent B | Inspect ItemCharge AP spend hardening as fallback | read-only AP/item charge files | edits, docs | Small UOW recommendation if assembler is too broad. |
| Orchestrator | Implement only a pure assembler if bounded | new service/tests, docs | live handler, AP mutation, persistence | Code, tests, docs, commit. |

## Do Not Parallelize

- `GameServerConnection.cs` live ItemPurification handler edits.
- AP mutation/rank side effects with cube snapshot assembly.
- Object-id allocation and `Storage.add` with packet-only projection.
- Progress and handoff docs.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, latest completion/handoff, and this handoff.
2. Confirm branch status and latest commit.
3. Run Parallel Work Discovery before selecting subagents.
4. Prefer Java observer/runtime artifact work if Java 25/Maven tooling is available.
5. If still tooling-blocked, keep ItemPurification work in pure snapshot/projection boundaries until mutation/AP/persistence are ready.
6. Run focused and full tests for any C# code changes.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
