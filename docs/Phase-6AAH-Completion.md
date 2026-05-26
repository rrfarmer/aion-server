# Phase 6AAH Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1198
Status: Phase 6 continues; bind-point teleport packet serialization is staged, but live fanout, cooldowns, Kinah mutation, and movement remain incomplete.

## Session Summary

UOW-1198 ported Java `SM_BIND_POINT_TELEPORT` serialization and opcode shape into a concrete C# server packet. This closes the packet prerequisite for future bind-point teleport broadcast planners without wiring live sends.

Files changed:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmBindPointTeleport.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `docs/Phase-6-PricesService-Consumer-Map.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6AAH-Completion.md`

Related recent units:

- UOW-1195 / commit `60cec509d`: added `BindPointTeleportPricePlanService`.
- UOW-1196 / commit `f835c9eac`: added `BindPointTeleportRequirementsPlanService`.
- UOW-1197 / commit `7502b7e00`: added `BindPointTeleportOperationPlanService`.

## What Changed

- Added `SmBindPointTeleport` with opcode `296`, matching Java `ServerPacketsOpcodes.addPacketOpcode(296, SM_BIND_POINT_TELEPORT.class)`.
- Added factory helpers for the three known Java action branches:
  - action `1`: start, writes action/player id/locId;
  - action `2`: cancel, writes action/player id only;
  - action `3`: cooldown, writes action/player id/locId/cooldown.
- Added a packet byte-shape test that parses all three action payloads.
- Kept live behavior out of scope: no broadcast integration, no cooldown storage, no scheduler, no Kinah mutation, and no movement.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "SmBindPointTeleport_WritesJavaHotspotPayloads" --nologo` passed 1 test.
- No Java runtime packet capture was compared, so parity remains `Needs Verification`.

## Migration Parity Table - UOW-1198

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_BIND_POINT_TELEPORT` | `Aion.GameServer.Network.Aion.ServerPackets.SmBindPointTeleport` | Packet / Serialization | Partial | Unit Tested | Needs Verification | Opcode `296` and `writeImpl` action branches are source-derived tested. Action `1` writes locId, action `2` writes only action/player id, action `3` writes locId and cooldown. No Java runtime packet capture was compared. |
| `com.aionemu.gameserver.network.aion.ServerPacketsOpcodes` | `Aion.GameServer.Network.Aion.ServerPackets.SmBindPointTeleport.PacketOpCode` | Opcode Registry | Partial | Unit Tested | Needs Verification | C# packet constant is `296`, matching Java registration. Central registry/golden runtime comparison is not involved. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService` | `Aion.GameServer.Services.BindPointTeleportOperationPlanService` / `Aion.GameServer.Network.Aion.ServerPackets.SmBindPointTeleport` | Service / Packet Dependency | Partial | Unit Tested | Needs Verification | Operation intent can now reference a concrete packet type, but live `PacketSendUtility.broadcastPacket`, task scheduling, cooldown map mutation, Kinah decrement, death recheck, and movement remain unported. |
| `com.aionemu.gameserver.custom.pvpmap.PvpMapHandler` | future C# custom PvP map handler | Runtime Handler Dependency | Not Started | No Tests | Unknown | Java custom PvP map also emits actions `1` and `3` and calls cancel. C# runtime handler parity is Phase 7/future work; this unit only records the dependency. |

Tests added:

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GamePacketTests.SmBindPointTeleport_WritesJavaHotspotPayloads` | Unit / Packet Byte Shape | Java `SM_BIND_POINT_TELEPORT.writeImpl`; Java opcode registration | Validates opcode `296` and all three known action payload branches. | Deterministic source-derived expectation. | No Java-generated golden packet or live broadcast path. |

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 1 concrete bind-point teleport packet serializer plus 1 focused packet test
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 3 grouped rows
- Total blocked artifacts: 4 grouped categories: live packet fanout, cooldown scheduling, inventory mutation, and movement/death checks
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Live `PacketSendUtility.broadcastPacket` / `broadcastPacketAndReceive` use is still not wired.
- Java action `2` intentionally ignores `locId` in the payload; C# factory accepts `locId` for call-site symmetry but does not serialize it.
- Static cooldown map, scheduler timing, Kinah mutation, death/about-to-die recheck, and final movement remain unported.
- Custom PvP map handler call sites remain future dynamic-handler work.
- No Java runtime comparison was executed. Reflection, date/time, and threading behavior did not change; serialization is source-derived only.

## Next Work Options

### Recommended Sequential Task

- Task: Model bind-point `cancelTeleport` and/or `onLogin` intent planners using `SmBindPointTeleport`.
- Why: The packet shape now exists, and these branches can be staged without live scheduler or movement work.
- Files:
  - likely `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportCancelPlanService.cs`
  - likely `dotnetConversion/tests/Aion.GameServer.Tests/BindPointTeleportCancelPlanServiceTests.cs`
  - optionally a separate `BindPointTeleportLoginPlanService`
  - `docs/Phase-6-PricesService-Consumer-Map.md`
  - `docs/PHASE-6-PROGRESS.md`
  - next Phase 6 handoff

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Model cancel intent planner | new cancel planner and tests | Low/Medium | Depends only on Java `cancelTeleport` and concrete packet metadata. |
| B | Model onLogin cooldown packet intent | new login planner and tests | Medium | Needs scalar cooldown facts; avoid live static map. |
| C | Compose packet-output metadata into existing operation planner | `BindPointTeleportOperationPlanService.cs` and tests | Medium | Sequential if touching same service/test files. |
| D | Continue `SM_SELL_ITEM` live-readiness fact assembly | new/non-overlapping sell-item adapter files and tests | Medium | Avoid shared dialog routing unless owned exclusively. |

### Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Agent A | Model cancel intent planner | cancel planner/test files only | shared docs, operation planner, connection handlers |
| Agent B | Model onLogin cooldown intent planner | login planner/test files only | shared docs, operation planner, connection handlers |
| Orchestrator | Integrate, test, update docs, commit | shared docs and final review | do not overlap with agent-owned files until integration |

If sub-agent tooling is unavailable or the scope stays small, do cancel first sequentially, then onLogin in a later unit.

### Do Not Parallelize

- `GameServerConnection` teleport handlers: live movement, packet order, and scheduling are high-conflict.
- Shared progress/handoff/parity docs: Orchestrator should own final edits.
- `PlayerTeleportService` live routing: broad movement side effects and existing tests make it unsuitable for concurrent edits.
- `GamePacketTests.cs`: shared packet test file; one owner at a time.

## Context Needed By Next Session

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_BIND_POINT_TELEPORT.java`
  - `game-server/src/com/aionemu/gameserver/custom/pvpmap/PvpMapHandler.java`
- Latest completed commits:
  - `7502b7e00 [Phase 6][UOW-1197] Add bind point teleport operation plan`
  - next commit should be `[Phase 6][UOW-1198] Add bind point teleport packet`
- Keep bind-point packet/planners non-live until fanout, cooldown scheduling, Kinah mutation, and movement have their own parity slices.
