# Phase 6AAK Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1201
Status: Phase 6 continues; bind-point teleport live fanout insertion points are audited, and the next prerequisite is the missing `CM_BIND_POINT_TELEPORT` opcode `244` parser/registration.

## Session Summary

UOW-1201 performed a read-only audit across Java bind-point teleport packet/service flow and the C# connection/teleport surfaces. The audit found that C# has no `CmBindPointTeleport` client packet class and no opcode `244` registration, so live bind-point fanout, scheduling, Kinah mutation, and movement should remain disabled until the client packet boundary is ported.

Files changed:

- `docs/Phase-6-BindPointTeleport-LiveFanout-Audit.md`
- `docs/Phase-6-PricesService-Consumer-Map.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6AAK-Completion.md`

Related recent units:

- UOW-1195 / commit `60cec509d`: added `BindPointTeleportPricePlanService`.
- UOW-1196 / commit `f835c9eac`: added `BindPointTeleportRequirementsPlanService`.
- UOW-1197 / commit `7502b7e00`: added initial `BindPointTeleportOperationPlanService`.
- UOW-1198 / commit `d433d4b40`: added `SmBindPointTeleport`.
- UOW-1199 / commit `4b60343c1`: added `BindPointTeleportControlPlanService`.
- UOW-1200 / commit `448715b60`: added concrete bind-point operation packet intents.

## What Changed

- Added a dedicated live fanout insertion audit document for bind-point teleport.
- Documented Java `CM_BIND_POINT_TELEPORT` behavior:
  - opcode `244`;
  - reads action byte;
  - only action `1` reads `locId` and `kinah`;
  - dead players return before service dispatch;
  - action `1` calls `teleport`;
  - action `2` calls `cancelTeleport`.
- Documented that C# currently lacks:
  - `CmBindPointTeleport`;
  - opcode `244` registration;
  - `GameServerConnection` handler branch;
  - bind-point-specific cooldown/task owner;
  - live fanout, Kinah mutation, and final movement.
- Updated the prices consumer map and progress log so the next unit starts from parser registration instead of live fanout wiring.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "BindPointTeleport|SmBindPointTeleport" --nologo` passed 18 tests.
- No Java runtime comparison was run, so parity remains `Needs Verification` / `Unknown` depending on artifact state.

## Migration Parity Table - UOW-1201

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_BIND_POINT_TELEPORT` | future `Aion.GameServer.Network.Aion.ClientPackets.CmBindPointTeleport` | Client Packet / Parser | Not Started | No Tests | Unknown | Java opcode `244` reads action byte, and only action `1` reads `locId` and `kinah`. C# has no parser or packet-factory registration. Missing methods: parser, state registration, handler dispatch, dead-player guard, action no-op behavior. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` | `Aion.GameServer.Services.BindPointTeleportOperationPlanService`; future live handler/service | Service / Movement | Partial | Unit Tested | Needs Verification | Staged price, requirements, operation order, and packet intents exist. Live hotspot lookup, packet fanout, `TaskId.SKILL_USE` scheduling, Kinah mutation, cooldown map, death/about-to-die recheck, and final movement remain unported. Threading behavior remains unverified. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.cancelTeleport` | `Aion.GameServer.Services.BindPointTeleportControlPlanService`; future live handler/service | Service / Control Flow | Partial | Unit Tested | Needs Verification | Non-live cancel intent exists. Live task lookup/cancel and action `2` visible fanout remain unported. Threading/cancellation behavior remains unverified. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.onLogin` | `Aion.GameServer.Services.BindPointTeleportControlPlanService`; future enter-world cooldown bridge | Service / Login Control Flow | Partial | Unit Tested | Needs Verification | Non-live login cooldown packet intent exists. Static cooldown ownership, date/time-left calculation, and `broadcastPacketAndReceive` source-player inclusion remain unported. Date/time and threading behavior remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_BIND_POINT_TELEPORT` | `Aion.GameServer.Network.Aion.ServerPackets.SmBindPointTeleport` | Server Packet / Serialization | Partial | Unit Tested | Needs Verification | Opcode `296` and action payload branches are source-derived unit tested. No Java runtime packet capture or live broadcast comparison was run. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `Aion.GameServer.Network.Aion.IGameClientConnectionRegistry`; `GameServerConnection.SendPacketAsync` | Fanout Utility Dependency | Partial | Manual Only | Needs Verification | Existing C# fanout helpers can send to visible players and specific players, but Java `broadcastPacket(..., true)` and `broadcastPacketAndReceive` semantics need explicit source-inclusion and visibility tests for hotspot packets. |
| `com.aionemu.gameserver.model.TaskId.SKILL_USE` | future bind-point runtime task owner using `Aion.GameServer.Utils.ScheduledTask` | Scheduler Dependency | Partial | Manual Only | Needs Verification | C# has general scheduling primitives and unrelated pending item-use task patterns, but no bind-point `TaskId.SKILL_USE` slot. Cancellation and callback ordering remain unported. |
| `com.aionemu.gameserver.services.teleport.TeleportService.teleportTo` | `Aion.GameServer.Services.PlayerTeleportService` | Movement Dependency | Partial | Manual Only | Needs Verification | C# has immediate and pending teleport helpers, but no bind-point final movement path. Java death/about-to-die recheck, same-world/world-change behavior, known-list packet order, and persistence side effects remain unverified. |

Tests run:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `BindPointTeleportPricePlanServiceTests.*` | Existing distance/client-price reconciliation coverage. | Source-derived only. |
| `BindPointTeleportRequirementsPlanServiceTests.*` | Existing world/race/Kinah/cooldown guard-order coverage. | Source-derived only. |
| `BindPointTeleportOperationPlanServiceTests.*` | Existing operation order and packet-intent coverage. | Source-derived only. |
| `BindPointTeleportControlPlanServiceTests.*` | Existing cancel and login cooldown intent coverage. | Source-derived only. |
| `GamePacketTests.SmBindPointTeleport_WritesJavaHotspotPayloads` | Existing opcode `296` and action payload branch coverage. | Source-derived only. |

## Summary Metrics

- Total Java artifacts discovered: 8 grouped artifact rows in this unit
- Total artifacts ported: 0 live artifacts; 1 read-only insertion audit completed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 7 grouped rows
- Total blocked artifacts: 5 grouped categories: client parser/registration, live fanout, cooldown/task state, Kinah mutation, and final movement/death checks
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Parser parity is currently blocked by the missing `CmBindPointTeleport` class and opcode `244` registration.
- Live fanout semantics need source-player inclusion tests before Java `broadcastPacket(..., true)` or `broadcastPacketAndReceive` can be claimed.
- Cooldown storage is a static Java map keyed by player object id; C# needs an explicit owner and concurrency policy.
- Scheduled Kinah decrement can fail after initial requirement checks; this race is still only metadata in C#.
- Death/about-to-die recheck occurs after the final 1 second delay, not at request time only.
- No Java runtime comparison was executed. Reflection behavior did not change; serialization remains source-derived for `SmBindPointTeleport`; date/time, threading, movement, and persistence parity are unverified.

## Next Work Options

### Recommended Sequential Task

- Task: Add `CmBindPointTeleport` parser coverage and opcode `244` registration without live teleport side effects.
- Why: The audit found the live request boundary is missing in C#; parser parity must exist before handler composition, scheduling, cooldown state, or live fanout.
- Files:
  - Java source: `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_BIND_POINT_TELEPORT.java`
  - Java source: `game-server/src/com/aionemu/gameserver/network/aion/AionClientPacketFactory.java`
  - C# target: `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmBindPointTeleport.cs`
  - C# target: `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientPacketFactory.cs`
  - C# tests: packet factory/client packet tests
  - docs for next unit

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Add parser-only `CmBindPointTeleport` and tests | client packet file, packet factory, packet tests | Medium | Keep handler live side effects out. |
| B | Model scheduled Kinah-decrement failure intent | new planner/test files | Medium | Separate from parser and live inventory mutation. |
| C | Continue `SM_SELL_ITEM` live-readiness fact assembly | new/non-overlapping sell-item adapter files and tests | Medium | Avoid shared dialog routing unless owned exclusively. |
| D | Audit bind-point fanout source-inclusion semantics | Java PacketSendUtility/C# connection registry read-only | Low | Useful before live broadcast wiring. |

### Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Agent A | Parser-only `CmBindPointTeleport` implementation | client packet file, packet factory, focused parser tests | live handler/scheduler/movement writes |
| Agent B | Read-only fanout semantic audit | Java `PacketSendUtility`, C# registry files | all writes |
| Orchestrator | Integrate parser and docs | progress, prices map, handoff | live teleport execution until parser tests pass |

If sub-agent tooling is unavailable, implement the parser-only unit sequentially.

### Do Not Parallelize

- `GameServerConnection` live bind-point handler: it will touch parser dispatch, world state, packet order, scheduler, and movement.
- Shared progress/handoff/parity docs: Orchestrator should own final edits.
- `PlayerTeleportService` movement wiring: defer until the bind-point runtime task/cooldown owner exists.

## Context Needed By Next Session

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/network/aion/AionClientPacketFactory.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_BIND_POINT_TELEPORT.java`
  - `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_BIND_POINT_TELEPORT.java`
- Latest completed commit before this handoff:
  - `448715b60 [Phase 6][UOW-1200] Add bind point teleport packet intents`
- Next commit should be `[Phase 6][UOW-1201] Audit bind point teleport live fanout`.
- Keep bind-point live behavior disabled until parser, non-live handler composition, cooldown/task ownership, fanout semantics, Kinah mutation, and final movement each have focused parity slices.
