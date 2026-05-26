# Phase 6AAW Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1213
Status: Phase 6 continues; bind-point teleport now has parser, action selection, operation/control packet intents, fanout intents, request-level composition, runtime task/cooldown state semantics, scheduled Kinah decrement/failure intent, final movement gate intent, scheduled callback composition metadata, a `TeleportService.teleportTo` side-effect audit, a non-live teleport side-effect planner, callback-side side-effect metadata composition, and a live-adapter readiness checklist. Live connection dispatch, real scheduler/cooldown mutation, live inventory mutation, persistent known-list parity, and movement remain disabled.

## Session Summary

UOW-1213 added `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`, a read-only gate checklist for bind-point live adapter work. The checklist confirms that `GameServerConnection` dispatch should remain disabled and identifies concrete prerequisites before live work: bind-point failure system-message helpers/tests, a no-op handler composition bridge, runtime task/cooldown ownership, live Kinah mutation/persistence, source-included fanout tests, and live movement side-effect ownership.

Files changed:

- `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
- `docs/Phase-6-BindPointTeleport-LiveFanout-Audit.md`
- `docs/Phase-6-PricesService-Consumer-Map.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6AAW-Completion.md`

## Validation

- Documentation/source-audit unit; no product code changed and no new tests were added.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport" --nologo` passed 63 tests.
- No Java runtime comparison was run, so parity remains `Needs Verification`.

## Migration Parity Table - UOW-1213

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_BIND_POINT_TELEPORT` | `Aion.GameServer.Network.Aion.ClientPackets.CmBindPointTeleport`; future live adapter | Client Packet / Handler Boundary | Partial | Unit Tested | Needs Verification | Parser/opcode coverage exists, but live `runImpl` dispatch is not wired. Handler-level no-op composition bridge is recommended before live side effects. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` | existing bind-point planner stack; future live service/adapter | Service / Movement | Partial | Unit Tested | Needs Verification | Non-live metadata covers price, requirements, operation, scheduler/cooldown facts, scheduled Kinah, callback, final movement, and side effects. Live hotspot lookup, task execution, Kinah mutation, cooldown mutation, fanout, and movement remain unported. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.cancelTeleport` | `BindPointTeleportControlPlanService`; future task owner/live adapter | Service / Control Flow | Partial | Unit Tested | Needs Verification | Non-live cancel intent exists. Live `TaskId.SKILL_USE` lookup/cancel and action `2` fanout remain blocked on runtime task owner. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.onLogin` | `BindPointTeleportControlPlanService`; future cooldown owner/login bridge | Service / Login Control Flow | Partial | Unit Tested | Needs Verification | Non-live cooldown packet intent exists. Static cooldown ownership and login broadcast remain unported. Date/time and threading behavior are unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` bind-point failure helpers | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` | Packet / System Message | Partial | Manual Only | Needs Verification | Generic constructor can emit IDs, but named helpers/tests are missing for `1300689`, `1300691`, and `1300961`. Add before live failure sends. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_BIND_POINT_TELEPORT` | `Aion.GameServer.Network.Aion.ServerPackets.SmBindPointTeleport` | Packet / Serialization | Partial | Unit Tested | Needs Verification | Source-derived packet tests exist for action payloads. Live fanout and Java runtime capture remain missing. |
| `com.aionemu.gameserver.services.teleport.TeleportService.teleportTo(Player,int,float,float,float)` | `BindPointTeleportTeleportToSideEffectPlanService`; future live movement adapter | Service / Movement Dependency | Partial | Unit Tested | Needs Verification | Side-effect metadata is composed into callback planning, but live action abort, despawn/spawn, packet sends, pet move, callbacks, and movement remain unported. |

Tests added:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| None in UOW-1213 | Readiness audit for live adapter gates and next prerequisite order. | Manual source/C# inspection only. |

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: 0 live artifacts; 1 read-only readiness checklist completed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 7 grouped rows
- Total blocked artifacts: 6 grouped categories: live dispatch, runtime task/cooldown ownership, system-message helper coverage, inventory mutation/persistence, known-list fanout, and live movement adapter
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Live `GameServerConnection` dispatch remains disabled and should stay disabled until prerequisites are satisfied.
- The readiness checklist is manual evidence, not executable parity.
- Missing system-message helpers could cause live failure branches to use ad hoc IDs unless fixed first.
- Runtime task/cooldown ownership, Kinah mutation/persistence, known-list fanout, and movement execution remain unported.
- Reflection behavior did not change. Serialization, threading, date/time, movement, known-list, and persistence parity remain unverified for live bind-point teleport.

## Next Work Options

### Recommended Sequential Task

- Task: Add concrete `SmSystemMessage` helpers and packet tests for bind-point failure messages.
- Why: Live failure branches need named, tested message constructors before any adapter sends packets.
- Suggested files:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
  - existing progress/handoff docs
- Message IDs:
  - `STR_CANNOT_MOVE_TO_AIRPORT_NOT_ENOUGH_FEE` -> `1300689`
  - `STR_CANNOT_MOVE_TO_AIRPORT_NO_ROUTE` -> `1300691`
  - `STR_FLYING_TIME_NOT_READY` -> `1300961`

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Bind-point failure system-message helpers/tests | `SmSystemMessage.cs`, `GamePacketTests.cs` | Low/Medium | Best next code unit. |
| B | Handler-level no-op composition bridge | new service/test pair only | Medium | Must remain non-live and avoid `GameServerConnection` dispatch. |
| C | Runtime task/cooldown owner design doc | new doc only | Low/Medium | Useful before scheduler code. |
| D | Continue `SM_SELL_ITEM` live-readiness fact assembly | new/non-overlapping sell-item adapter files and tests | Medium | Avoid shared dialog routing unless owned exclusively. |

### Do Not Parallelize

- `GameServerConnection` live bind-point dispatch: wait until readiness prerequisites are satisfied.
- Shared progress/handoff/parity docs: Orchestrator should own final edits.
- Runtime scheduler/cooldown state and live movement adapter in the same unit: too much shared behavior and side-effect risk.

## Context Needed By Next Session

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
- C# surfaces:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- Readiness doc:
  - `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
- Latest completed commit:
  - `b620456a4 [Phase 6][UOW-1212] Compose bind point teleport side effect metadata`
  - next commit should be `[Phase 6][UOW-1213] Add bind point teleport live adapter readiness`
- Keep live bind-point behavior disabled until teleport side-effect ordering, cooldown mutation/fanout execution, live inventory mutation/packets, live movement packet ordering, and live known-list fanout each have focused parity slices.
