# Phase 6MS Completion Handoff - Item Usage Animation Packet API

Date: May 24, 2026
Unit of Work: UOW-845
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-845] Add item usage animation overloads`)

## Status

Phase 6 is still in progress. This unit added Java-shaped constructor overloads to the live C# `SmItemUsageAnimation` packet and expanded direct packet byte tests for Java constructor defaults and byte-cast behavior. Java remains the source of truth. Runtime `usingItem` write-time side effects and broad item-use call-site migration remain intentionally out of scope.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmItemUsageAnimation.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6MS-Completion.md`

## What Changed

- Added Java-shaped overloads to `SmItemUsageAnimation`:
  - `(playerObjectId, itemObjectId, itemId)`
  - `(playerObjectId, itemObjectId, itemId, time, end)`
  - `(playerObjectId, targetObjectId, itemObjectId, itemId, time, end, unknown)`
- Preserved Java defaults:
  - 3-arg constructor: `target=self`, `time=0`, `end=1`, `unk=0`, `unk1=0`, `unk2=1`, `unk3=1`
  - 5-arg constructor: `target=self`, `unk=0`, `unk1=0`, `unk2=1`, `unk3=0`
  - 6/7-arg constructors: final Java `unk` argument maps to final `writeD(unk3)`, not the byte `unk` field
- Extended direct packet byte tests in `GamePacketTests` for:
  - existing 6-arg sample
  - new 3-arg defaults
  - new 5-arg defaults
  - new targeted 7-arg defaults
  - full 10-arg byte truncation sample

## Tests

Packet focused:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "GamePacketTests"
```

Result: passed, 90 tests.

Broader focused:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerSummonSkillExecutionServiceTests|StaticDataNpcSkillTests|GamePacketTests"
```

Result: passed, 160 tests.

Full:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1420 tests.

Updated test:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `GamePacketTests.SerializesRepresentativeGameServerPackets` | Adds direct byte assertions for Java-shaped `SmItemUsageAnimation` overload defaults and full 10-arg byte-cast behavior. | Source-derived from Java `SM_ITEM_USAGE_ANIMATION.writeImpl`; no Java runtime golden vector. |

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION` | `Aion.GameServer.Network.Aion.ServerPackets.SmItemUsageAnimation` | Packet | Partial | Unit Tested / Packet Byte Tested | Partial Parity | Java-shaped constructor defaults are now represented and tested in C#. Remaining gaps include Java runtime golden bytes, opcode/frame/crypto, write-time `usingItem` mutation, live fanout/callers, and live-client validation. |
| `com.aionemu.gameserver.network.aion.AionServerPacket` | `SmItemUsageAnimation.WritePayload` / `PacketBuffer` tests | Packet Utility | Partial | Packet Byte Tested | Partial Parity | Direct C# payload bytes match source-reviewed Java write order/defaults for selected samples. No live Java writer/frame comparison. |
| `com.aionemu.gameserver.network.aion.ServerPacketsOpcodes` | `SmItemUsageAnimation.PacketOpCode` | Packet Opcode Metadata | Partial | Manual Only | Needs Verification | Opcode remains `183`; frame/opcode golden validation was not added. |
| `com.aionemu.gameserver.world.World` | unresolved live lookup dependency | Runtime Lookup Dependency | Not Started | No Tests | Needs Verification | Positive-time Java writer world/player lookup remains unimplemented in packet serialization. |
| `com.aionemu.gameserver.model.gameobjects.player.Player` | `Player.UsingItemObjectId` / unresolved write-time side effect | Player State Dependency | Partial | No Tests for this unit | Needs Verification | C# still uses scheduler-side item-use state rather than Java packet write-time mutation. |
| `com.aionemu.gameserver.model.gameobjects.player.Inventory` | unresolved positive-time item lookup dependency | Inventory Dependency | Not Started | No Tests | Needs Verification | Java item lookup/null/exception behavior remains unverified. |
| `com.aionemu.gameserver.skillengine.model.Skill` | Java-shaped packet overloads available to callers | Runtime / Packet Caller | Partial | No Tests for live callers | Needs Verification | Overloads reduce future caller-default mistakes; live skill/item execution not changed. |
| `com.aionemu.gameserver.services.toypet.PetService` | full 10-arg packet byte test | Service / Packet Caller | Partial | Packet Byte Tested for packet shape | Partial Parity | 10-arg packet shape covered; pet service behavior remains missing/unverified. |

## Remaining Risks

- Java positive-time packet serialization mutates `Player.usingItem`; C# packet serialization still does not.
- Existing `GameServerConnection` call sites were not migrated to the new overloads.
- Direct packet tests are source-derived, not Java-generated golden vectors.
- Opcode/frame/crypto, socket queueing, fanout, item-use scheduling, cancellation, dynamic item actions, inventory/resource mutation, threading, reflection, and live-client behavior remain missing or unverified.

## Summary Metrics

- Total Java artifacts discovered: 8
- Total artifacts ported: 1 live packet API/default-overload slice plus direct packet-byte tests
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 8
- Total blocked artifacts: 31 blocked/not-started categories
- Estimated overall migration completion: Phase 6 remains about 66% complete

## Next Recommended Unit of Work

Continue item-use parity by auditing positive-time `SmItemUsageAnimation` call sites in `GameServerConnection` and adding a narrow trace/test that documents which C# paths set `Player.UsingItemObjectId` after positive-time animation versus Java's packet write-time `Player.usingItem` mutation.

Suggested scope:

- Inspect positive-time `new SmItemUsageAnimation(... time > 0 ...)` calls in `GameServerConnection`.
- Identify which paths call `SchedulePendingItemUseAsync` or otherwise set pending item state after animation.
- Add a narrow metadata trace or focused test for positive-time item-use state behavior.
- Do not refactor broad item-use call sites unless the scope remains small and testable.
- Keep Java's write-time mutation documented as a parity gap unless a clean runtime bridge is implemented.

## Safe Parallel Work Candidates

| Candidate | Files | Parallel Safe? | Notes |
|---|---|---|---|
| Positive-time call-site audit | `GameServerConnection.cs` read-only | Yes | Count and categorize start-animation paths. |
| Pending item state test design | tests read-only or isolated test file | Yes | Determine existing test seams before edits. |
| Runtime side-effect bridge analysis | read-only | Yes | Explore whether packet serialization can access world/player safely without packet-layer coupling. |
| Progress/handoff docs | docs | No | Orchestrator-owned after tests pass. |

## Do Not Parallelize

- `GameServerConnection.cs` implementation edits: many item-use paths share one large file; keep single-writer.
- `SmItemUsageAnimation.cs`: stable after this unit; avoid churn unless explicitly needed.
- Progress and handoff docs: Orchestrator-owned.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, and this handoff.
2. Confirm branch status and latest commit.
3. Run parallel work discovery before selecting subagents.
4. Use Java as source of truth and preserve breadcrumbs in code.
5. Keep the next unit small: likely positive-time item-use state trace/test.
6. Run focused and full tests.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
