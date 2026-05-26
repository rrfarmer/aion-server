# Phase 6ACY Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1267
Status: Phase 6 continues; C# `SmPlayerStance` now supports Java's object-id/state payload. Live known-list player-see dispatch remains disabled, and `SmAbnormalEffect` remains the next missing packet serializer in the player-see sequence.

## Session Summary

UOW-1267 added a focused C# `SmPlayerStance` packet serializer and updated the player known-list side-effect descriptor to mark stance packet support as available.

Files changed:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmPlayerStance.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListPlayerSideEffectPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerKnownListPlayerSideEffectPlanServiceTests.cs`
- `docs/Phase-6-BindPointTeleport-SmPlayerStance.md`
- `docs/Phase-6-BindPointTeleport-KnownListPlayerSideEffectPlanner.md`
- `docs/Phase-6-BindPointTeleport-KnownListOperationSideEffectAttachment.md`
- `docs/Phase-6-BindPointTeleport-KnownListPopulationSideEffectIntegration.md`
- `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
- `docs/Phase-6-BindPointTeleport-PlayerKnownListPopulation-Design.md`
- `docs/Phase-6-BindPointTeleport-ScheduledKinah-LiveAdapter-FinalReadiness.md`
- `docs/Phase-6-BindPointTeleport-KnownListFanoutParity.md`
- `docs/Phase-6-BindPointTeleport-SmPlayerInfoViewerRace.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ACY-Completion.md`

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "SmPlayerStance|PlayerKnownListPlayerSideEffectPlanServiceTests" --nologo` passed 8 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList|SmPlayerInfo|SmPlayerStance" --nologo` passed 268 tests.
- No Java runtime packet capture was run.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1267

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_STANCE` | `Aion.GameServer.Network.Aion.ServerPackets.SmPlayerStance` | Packet / Serialization | Complete | Unit Tested | Partial Parity | C# writes object id then state and uses opcode 31. No Java runtime golden-byte capture was executed, so parity is source-derived but not verified parity. |
| `com.aionemu.gameserver.controllers.PlayerController.sendPlayerInfoPackets` | `PlayerKnownListPlayerSideEffectPlanService` stance descriptor | Controller Packet Intent / Packet Dependency | Partial | Unit Tested | Partial Parity | Descriptor support now points to concrete `SmPlayerStance` and preserves Java state `1` ordering. It still does not instantiate/send packets or compute live `isUnderStance`. |
| `com.aionemu.gameserver.controllers.PlayerController.startStance` | `SmPlayerStance(player, 1)` packet prerequisite only | Controller / Broadcast Dependency | Partial | Unit Tested | Needs Verification | Packet can represent the broadcast payload, but stance observer registration, effect handling, and live broadcast are not ported in this unit. |
| `com.aionemu.gameserver.controllers.PlayerController.stopStance` | `SmPlayerStance(player, 0)` packet prerequisite only | Controller / Broadcast Dependency | Partial | Unit Tested | Needs Verification | Packet can represent the broadcast payload, but stance observer removal, effect removal, and live broadcast are not ported in this unit. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` action `3` fanout | player known-list descriptor stack with `SmPlayerStance` packet support | Service / Fanout Prerequisite | Partial | Regression Tested | Needs Verification | Future known-list fanout has one fewer packet serializer blocker. Live scheduled callbacks, sockets, movement, cooldown, and dispatch remain disabled. |

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 focused packet serializer plus 1 focused packet test theory and 1 descriptor support update
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: 1 live stance observer/broadcast path, 1 live controller side-effect dispatcher, 1 `SmAbnormalEffect` packet, 1 active-player context computation path, and 1 Java runtime capture path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- `SmPlayerStance` has no Java runtime golden-byte validation.
- Live `PlayerController.startStance` / `stopStance` observer registration, effect removal, and broadcast ordering are not ported.
- Known-list `sendPlayerInfoPackets` remains descriptor-only.
- `SmAbnormalEffect` remains missing.
- `SmPlayerInfo` still needs live active-player context computation before real player-see sends.
- Threading, reflection, date/time, precision/rounding, and broader live packet ordering remain unverified.

## Next Work Options

### Recommended Sequential Task

- Task: Audit Java `SM_ABNORMAL_EFFECT` packet serializer and supporting effect model dependencies.
- Scope:
  - Inspect Java `SM_ABNORMAL_EFFECT`, effect controller inputs, and caller branches from `PlayerController.see`.
  - Determine whether a small scalar C# packet-input model can represent the payload without live effect runtime.
  - If the serializer shape is tight, add a focused packet class/tests; if broad, create a readiness/audit document and choose a smaller effect prerequisite.
  - Keep live known-list dispatch and socket sends disabled.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | `SmAbnormalEffect` serializer audit | read-only Java/C# inspection, docs | Medium | Recommended first because effect payload may be broader than stance. |
| B | Player-info/stance descriptor-to-packet input bridge | new service/tests/docs | Medium | Should remain non-live and avoid `GameServerConnection`. |
| C | Population descriptor fanout trace bridge | new service/tests/docs | Medium | Can consume descriptors without live sends. |
| D | Java packet-observer design for player-see sequence | docs/read-only | Low/Medium | Useful before claiming runtime parity. |
| E | Live player-see dispatch readiness checklist refresh | docs/read-only | Low/Medium | Useful after abnormal-effect audit. |

### Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Explorer | Inspect Java `SM_ABNORMAL_EFFECT` serializer and effect model inputs | read-only Java/C# inspection | all writes |
| Orchestrator | Decide whether next unit is packet implementation or audit-only docs | docs or new packet/tests after audit | live `GameServerConnection`, world known-list services |

### Do Not Parallelize

- Multiple agents editing `GamePacketTests.cs`.
- `SmAbnormalEffect` implementation and live player-see dispatch in the same unit.
- Effect runtime modeling and descriptor-to-packet bridge in the same unit unless the audit proves both are tiny.
- Shared progress/handoff/parity docs; keep them orchestrator-owned.

## Context Needed By Next Session

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_ABNORMAL_EFFECT.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PLAYER_STANCE.java`
  - `game-server/src/com/aionemu/gameserver/controllers/PlayerController.java`
  - effect controller/model classes referenced by `SM_ABNORMAL_EFFECT`
- C# surfaces:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmPlayerStance.cs`
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmPlayerInfo.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListPlayerSideEffectPlanService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- Latest completed commits:
  - `f27c3120d [Phase 6][UOW-1265] Add SmPlayerInfo enemy flag`
  - `5f5acf149 [Phase 6][UOW-1266] Add SmPlayerInfo viewer race context`
  - UOW-1267 should be committed as `[Phase 6][UOW-1267] Add SmPlayerStance packet`
- Next commit after this handoff should be `[Phase 6][UOW-1268] ...` for the abnormal-effect audit/packet slice or another selected safe unit.

Keep live bind-point behavior disabled until Java-equivalent known-list population, source-first fanout execution, live scheduled callback dispatch, final movement packet ordering, live scheduled task ownership, concrete packet serializers, and Java packet/runtime validation have focused parity slices.
