# Phase 6AAL Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1202
Status: Phase 6 continues; `CM_BIND_POINT_TELEPORT` parser and opcode `244` registration now exist, but Java `runImpl` dispatch and live bind-point side effects remain disabled.

## Session Summary

UOW-1202 added parser-only support for Java `CM_BIND_POINT_TELEPORT`. This closes the missing client packet boundary found by UOW-1201 while keeping live teleport execution staged.

Files changed:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmBindPointTeleport.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmBindPointTeleportTests.cs`
- `docs/Phase-6-BindPointTeleport-LiveFanout-Audit.md`
- `docs/Phase-6-PricesService-Consumer-Map.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6AAL-Completion.md`

## What Changed

- Added `CmBindPointTeleport` with Java `readImpl` parity:
  - reads `Action` as one byte;
  - only action `1` reads `LocId` and `Kinah`;
  - action `2` and unknown actions leave `LocId` and `Kinah` at Java default zero.
- Registered opcode `244` as in-game only.
- Added parser/factory tests for action `1`, action `2`, unknown action, and state gating.
- Updated docs to make the next unit handler-composition focused instead of parser focused.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "CmBindPointTeleportTests" --nologo` passed 4 tests.
- No Java runtime comparison was run, so parity remains `Needs Verification`.

## Migration Parity Table - UOW-1202

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_BIND_POINT_TELEPORT.readImpl` | `Aion.GameServer.Network.Aion.ClientPackets.CmBindPointTeleport.ReadPayload` | Client Packet / Parser | Complete | Unit Tested | Needs Verification | Parser reads action byte and only reads `locId`/`kinah` for action `1`, matching Java source. No Java runtime packet capture was compared. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_BIND_POINT_TELEPORT.runImpl` | future non-live/live handler composition | Client Packet / Handler | Not Started | No Tests | Unknown | Java dead-player guard, action `1` dispatch to `teleport`, action `2` dispatch to `cancelTeleport`, and unknown-action no-op remain unported. |
| `com.aionemu.gameserver.network.aion.AionClientPacketFactory` | `Aion.GameServer.Network.Aion.GameClientPacketFactory` | Client Packet Registry | Partial | Unit Tested | Needs Verification | Opcode `244` is now registered as `CmBindPointTeleport` for `GameConnectionState.InGame` only. Full factory parity remains broader than this packet. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` | `Aion.GameServer.Services.BindPointTeleportOperationPlanService`; future handler composition | Service / Movement Dependency | Partial | Unit Tested | Needs Verification | Parser action `1` exposes the fields required by existing staged planners. Live hotspot lookup, fanout, scheduler, cooldown, Kinah mutation, death recheck, and movement remain unported. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.cancelTeleport` | `Aion.GameServer.Services.BindPointTeleportControlPlanService`; future handler composition | Service / Control Dependency | Partial | Unit Tested | Needs Verification | Parser action `2` is now readable, but Java `runImpl` dispatch and live task cancellation/fanout remain unported. |

Tests added:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `CmBindPointTeleportTests.TryCreatePacket_RegistersJavaHotspotOpcodeAsInGameOnly` | Opcode `244` creates `CmBindPointTeleport` in-game and rejects authed state. | Source-derived only. |
| `CmBindPointTeleportTests.ReadFrom_ActionOneReadsLocIdAndKinahLikeJava` | Action `1` reads `locId` and `kinah`. | Source-derived only. |
| `CmBindPointTeleportTests.ReadFrom_ActionTwoLeavesLocIdAndKinahAtJavaDefaults` | Action `2` reads only action and keeps default fields. | Source-derived only. |
| `CmBindPointTeleportTests.ReadFrom_UnknownActionReadsOnlyActionLikeJavaRunImplNoop` | Unknown action reads only action and keeps default fields. | Source-derived only. |

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 parser-only client packet plus opcode registration
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: 5 grouped categories: handler dispatch, live fanout, cooldown/task state, Kinah mutation, and final movement/death checks
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Java `runImpl` behavior is not ported: dead-player guard, action dispatch, and unknown-action no-op are parser-tested only where they affect field reads.
- Parser tests are source-derived; no Java-generated client packet capture was compared.
- Live handler composition must avoid executing teleport side effects until cooldown/task ownership and fanout semantics are modeled.
- Static cooldown map, scheduler timing/cancellation, scheduled Kinah decrement race, final death/about-to-die recheck, and movement remain unported.
- Reflection, serialization, date/time, threading, persistence, and movement parity remain unverified for live bind-point teleport.

## Next Work Options

### Recommended Sequential Task

- Task: Compose `CmBindPointTeleport` parser output into a non-live bind-point handler plan.
- Why: Java `runImpl` is now the missing boundary between parser fields and existing staged service planners.
- Requirements:
  - dead player -> no action;
  - action `1` -> select teleport intent with `locId` and `kinah`;
  - action `2` -> select cancel intent;
  - unknown action -> no action;
  - no live packet fanout, scheduler, cooldown mutation, Kinah mutation, or movement.
- Files:
  - Java source: `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_BIND_POINT_TELEPORT.java`
  - C# existing: `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmBindPointTeleport.cs`
  - C# suggested new service: `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportClientActionPlanService.cs`
  - C# suggested tests: `dotnetConversion/tests/Aion.GameServer.Tests/BindPointTeleportClientActionPlanServiceTests.cs`
  - docs for next unit

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Non-live client action handler plan | new service/test files | Medium | Best next step; do not edit live connection handler yet. |
| B | Audit `PacketSendUtility.broadcastPacket(..., true)` source inclusion | Java/C# fanout files read-only | Low | Useful before live packet sending. |
| C | Model scheduled Kinah-decrement failure intent | new planner/test files | Medium | Keep separate from live inventory mutation. |
| D | Continue `SM_SELL_ITEM` live-readiness fact assembly | new/non-overlapping sell-item adapter files and tests | Medium | Avoid shared dialog routing unless owned exclusively. |

### Do Not Parallelize

- `GameServerConnection` live bind-point handler: wait until non-live action planning and fanout semantics are tested.
- Shared progress/handoff/parity docs: Orchestrator should own final edits.
- `PlayerTeleportService` movement wiring: defer until task/cooldown ownership is designed.

## Context Needed By Next Session

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_BIND_POINT_TELEPORT.java`
  - `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_BIND_POINT_TELEPORT.java`
- Latest completed commits:
  - `5d1461c82 [Phase 6][UOW-1201] Audit bind point teleport live fanout`
  - next commit should be `[Phase 6][UOW-1202] Add bind point teleport client packet`
- Keep live bind-point behavior disabled until parser, non-live handler composition, cooldown/task ownership, fanout semantics, Kinah mutation, and final movement each have focused parity slices.
