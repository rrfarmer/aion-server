# Phase 6MR Completion Handoff - Item Usage Animation Schema Metadata

Date: May 24, 2026
Unit of Work: UOW-844
Branch: `4.8`
Commit: pending at handoff creation (`[Phase 6][UOW-844] Model item usage animation schema`)

## Status

Phase 6 is still in progress. This unit added a non-live, source-derived schema/golden metadata projection for Java `SM_ITEM_USAGE_ANIMATION`. Java remains the source of truth. The work improves evidence around constructor defaults, field order, byte truncation, and write-time side effects, but does not yet change the live C# packet API or runtime item-use side-effect behavior.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/PlayerSummonSkillExecutionService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerSummonSkillExecutionServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6MR-Completion.md`

## What Changed

- Added `ProjectMercenaryNpcSkillItemUsageAnimationPacketSchemaGolden`.
- Added `PlayerSummonKnownObjectNpcSkillItemUsageAnimationPacketSchemaGolden` plus schema field/write-kind/constructor/caller-category enums.
- Added deterministic source-derived samples for:
  - Java 3-arg constructor defaults: target self, `time=0`, `end=1`, `unk2=1`, `unk3=1`.
  - Java targeted positive-time skill-start constructor: target object, item object, `time=3000`, `end=0`, `unk2=1`, `unk3=0`.
  - Java full 10-arg pet-skill variant: explicit byte fields and `unk3=15360`.
- Recorded Java `writeImpl` field order:

```text
writeD(playerObjId)
writeD(targetObjId)
writeD(itemObjId)
writeD(itemId)
writeD(time)
writeC(end)
writeC(unk)
writeC(unk1)
writeC(unk2)
writeD(unk3)
```

- Recorded Java positive-time write-time side effect:
  - `World.getInstance().getPlayer(playerObjId)`
  - `player.getInventory().getItemByObjId(itemObjId)`
  - `player.setUsingItem(item)`
- Recorded known C# gaps:
  - no direct Java-shaped 3-arg, 5-arg, or targeted 7-arg overloads in `SmItemUsageAnimation`
  - no packet-write-time `usingItem` mutation in C# serialization
  - scheduler-side `Player.UsingItemObjectId` is not equivalent for every positive-time packet send

## Tests

Focused:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerSummonSkillExecutionServiceTests|StaticDataNpcSkillTests"
```

Result: passed, 70 tests.

Full:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj
```

Result: passed, 1420 tests.

New/updated test:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `PlayerSummonSkillExecutionServiceTests.ProjectMercenaryNpcSkillItemUsageAnimationPacketSchemaGolden_EncodesJavaConstructorDefaults` | Payload hex for 3-arg defaults, targeted positive-time skill-start sample, and 10-arg pet-skill sample; little-endian fields; `writeC` truncation; `unk2`/`unk3` defaults; positive-time `usingItem` risk flags; missing Java null guards. | Source-derived from Java packet and caller audit; no live Java runtime or packet-byte capture. |

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION` | `PlayerSummonKnownObjectNpcSkillItemUsageAnimationPacketSchemaGolden` / existing `SmItemUsageAnimation` | Packet / Schema Metadata | Partial | Unit Tested / Golden metadata | Partial Parity | Constructor defaults, field order, byte truncation, and side-effect risks recorded. Live C# packet still lacks some Java-shaped overloads and write-time `usingItem` mutation. |
| `com.aionemu.gameserver.network.aion.ServerPacketsOpcodes` | `SmItemUsageAnimation.PacketOpCode` | Packet Opcode Metadata | Partial | Manual Only | Needs Verification | Java and C# both use opcode `183`; frame/opcode golden comparison was not added. |
| `com.aionemu.gameserver.network.aion.AionServerPacket` | item-usage schema writer metadata | Packet Utility | Partial | Unit Tested as metadata | Needs Verification | Primitive write metadata only; no live Java writer/frame/crypto comparison. |
| `com.aionemu.gameserver.world.World` | positive-time lookup risk flags | Runtime Lookup Dependency | Not Started | Unit Tested as metadata | Needs Verification | Java no-null-guard world/player lookup recorded only. |
| `com.aionemu.gameserver.model.gameobjects.player.Player` | `Player.UsingItemObjectId` / side-effect metadata | Player State Dependency | Partial | Unit Tested as metadata | Needs Verification | Java write-time `usingItem` mutation differs from C# scheduler-side state mutation. |
| `com.aionemu.gameserver.model.gameobjects.player.Inventory` | positive-time item lookup risk flags | Inventory Dependency | Not Started | Unit Tested as metadata | Needs Verification | Java item lookup null is tolerated but player/inventory null is not; no live C# parity yet. |
| `com.aionemu.gameserver.skillengine.model.Skill` | item-usage caller category metadata | Runtime / Packet Caller | Partial | Unit Tested as metadata | Needs Verification | Non-combat item start/end animation order recorded, not executed. |
| `com.aionemu.gameserver.controllers.PlayerController` | cancel item-use caller notes | Runtime / Packet Caller | Partial | Manual Only | Needs Verification | Cancel `time=0,end=3` behavior and `usingItem` clearing remain live gaps. |
| `com.aionemu.gameserver.services.toypet.PetService` | `ExtendedPetSkillUse` schema sample | Service / Packet Caller | Partial | Unit Tested as metadata | Partial Parity | 10-arg sample shape recorded; no pet skill/cooldown/inventory behavior. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | caller-category/send-helper notes | Utility / Fanout Dependency | Partial | Manual Only | Needs Verification | Caller send-helper differences remain unverified. |
| `com.aionemu.gameserver.model.templates.item.actions.*` | caller-category notes | Dynamic Item Action Callers | Not Started | Manual Only | Needs Verification | Representative end codes and variants discovered; concrete item action behavior not ported here. |

## Remaining Risks

- The metadata helper does not execute Java or C# packet serialization.
- C# callers can still miss Java defaults because `SmItemUsageAnimation` lacks Java-shaped overloads/factories.
- Java `usingItem` mutation happens during packet write; C# currently uses scheduling-side state mutation.
- Java positive-time writer null/exception behavior remains unverified in C#.
- Caller fanout and visibility/order differences remain likely until direct packet call-site audits are completed.
- Scheduled item-use ordering, cancellation semantics, opcode/frame/crypto, dynamic item actions, resource/item mutation, threading, reflection, and live-client behavior remain missing or unverified.

## Summary Metrics

- Total Java artifacts discovered: 11
- Total artifacts ported: 1 represented non-live `SM_ITEM_USAGE_ANIMATION` schema/golden metadata slice plus tests
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 11
- Total blocked artifacts: 37 blocked/not-started categories
- Estimated overall migration completion: Phase 6 remains about 66% complete

## Next Recommended Unit of Work

Continue NPC/item skill packet parity by adding Java-shaped `SmItemUsageAnimation` constructor/factory coverage and direct packet byte tests for all Java overload defaults, while documenting or bridging the `usingItem` write-time side-effect difference without broad `GameServerConnection` refactors.

Suggested scope:

- Add safe Java-shaped overloads or named factories to `SmItemUsageAnimation` for:
  - `(playerObjId, itemObjId, itemId)`
  - `(playerObjId, itemObjId, itemId, time, end)`
  - targeted `(playerObjId, targetObjId, itemObjId, itemId, time, end, unk)`
- Add direct packet byte tests in `GamePacketTests.cs` for every Java overload default.
- Keep write-time `usingItem` mutation documented unless a clean runtime boundary exists.
- Avoid broad `GameServerConnection` refactors in the constructor/factory unit.
- Update progress docs and create the next handoff after tests.

## Safe Parallel Work Candidates

| Candidate | Files | Parallel Safe? | Notes |
|---|---|---|---|
| Java overload/caller audit refinement | Java source only | Yes | Read-only; focus on less common end codes and send helpers. |
| C# packet overload/factory implementation | `SmItemUsageAnimation.cs` | Yes, if exclusive | Keep scoped to packet API only. |
| Direct packet byte tests | `GamePacketTests.cs` | Yes, if separate from service tests | Can run in parallel with packet implementation only if constructors/factories are agreed first. |
| Runtime side-effect design note | read-only / docs later | Yes | Analyze possible `usingItem` bridge without editing `GameServerConnection`. |
| Progress/handoff docs | `docs/PHASE-6-PROGRESS.md`, next handoff | No | Orchestrator-owned after tests pass. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Worker A | Add Java-shaped `SmItemUsageAnimation` overloads/factories | `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmItemUsageAnimation.cs` | `GameServerConnection.cs`, docs, tests unless assigned |
| Worker B | Add direct packet byte tests for overload defaults after API shape is known | `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs` | production files, docs |
| Explorer C | Read-only side-effect bridge analysis for `usingItem` | read-only | all writes |

## Do Not Parallelize

- `GameServerConnection.cs`: many item-use call sites and pending scheduler behavior; avoid concurrent edits unless one agent owns the whole file.
- `PlayerSummonSkillExecutionService.cs` and `PlayerSummonSkillExecutionServiceTests.cs`: shared metadata/test files used heavily in Phase 6.
- Progress and handoff docs: Orchestrator-owned.

## Resume Checklist

1. Read `docs/csharp-port.md`, orchestration docs, `docs/PHASE-6-PROGRESS.md`, and this handoff.
2. Confirm branch status and latest commit.
3. Run parallel work discovery before selecting subagents.
4. Use Java as source of truth and preserve breadcrumbs in code.
5. Keep the next unit small: likely `SmItemUsageAnimation` overload/factory plus direct packet byte tests.
6. Run focused and full tests.
7. Update Migration Parity Table, Remaining Risks, Summary Metrics, and Next Recommended Unit.
8. Create the next handoff and commit the completed unit.
