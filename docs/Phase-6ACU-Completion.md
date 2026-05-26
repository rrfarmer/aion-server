# Phase 6ACU Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1263
Status: Phase 6 continues; C# now has descriptor-only attachment of player packet side-effect plans to two-way known-list operation steps. Live known-list callback dispatch and bind-point action `3` fanout remain disabled.

## Session Summary

UOW-1263 added `PlayerKnownListOperationSideEffectAttachmentService`, which consumes two-way known-list operation plans and attaches directional player `see` / `notSee` packet-intent plans to the operation steps that would trigger Java controller callbacks.

Files changed:

- `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListOperationSideEffectAttachmentService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerKnownListOperationSideEffectAttachmentServiceTests.cs`
- `docs/Phase-6-BindPointTeleport-KnownListOperationSideEffectAttachment.md`
- `docs/Phase-6-BindPointTeleport-KnownListPlayerSideEffectPlanner.md`
- `docs/Phase-6-BindPointTeleport-KnownListPopulationComposition.md`
- `docs/Phase-6-BindPointTeleport-KnownListFanoutParity.md`
- `docs/Phase-6-BindPointTeleport-KnownListTwoWayMembershipAdapter.md`
- `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
- `docs/Phase-6-BindPointTeleport-PlayerKnownListPopulation-Design.md`
- `docs/Phase-6-BindPointTeleport-ScheduledKinah-LiveAdapter-FinalReadiness.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ACU-Completion.md`

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerKnownListOperationSideEffectAttachmentServiceTests" --nologo` passed 5 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList" --nologo` passed 260 tests.
- No Java runtime packet capture was run.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1263

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.world.knownlist.KnownList.updateVisibility` | `Aion.GameServer.Services.PlayerKnownListOperationSideEffectAttachmentService.Attach` | Known-List Side-Effect Composition | Partial | Unit Tested | Partial Parity | Attaches player `see` packet descriptors to operation-plan visibility steps by direction. Does not execute live visibility recomputation, controller dispatch, or exception handling. |
| `com.aionemu.gameserver.world.knownlist.KnownList.del` | `PlayerKnownListOperationSideEffectAttachmentService.Attach` | Known-List Removal Side-Effect Composition | Partial | Unit Tested | Partial Parity | Attaches player `notSee` delete descriptors before existing `notKnow` metadata steps. Does not execute target cleanup, packet send, `notKnow`, or live map mutation. |
| `com.aionemu.gameserver.world.knownlist.KnownList.clear` | clear operation plans consumed by side-effect attachment service | Known-List Clear Side-Effect Composition | Partial | Unit Tested | Needs Verification | Clear plans with visible not-see steps can receive descriptors, but supplied animation semantics are caller metadata and no live clear iteration occurs. |
| `com.aionemu.gameserver.controllers.PlayerController.see` | `PlayerKnownListPlayerSideEffectPlanService` through attachment service | Controller Packet Intent | Partial | Unit Tested | Partial Parity | Directional see descriptors attach to candidate/owner see steps. Java `super.see`, abnormal-effect packet serialization, and live sends remain missing. |
| `com.aionemu.gameserver.controllers.PlayerController.notSee` | `PlayerKnownListPlayerSideEffectPlanService` through attachment service | Controller Packet Intent | Partial | Unit Tested | Partial Parity | Directional notSee descriptors attach to visible removal steps and preserve viewer-unspawned skip metadata. Java `super.notSee`, target cleanup, and live sends remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_INFO` / `SM_MOTION` / `SM_EMOTION` / `SM_PLAYER_STANCE` / `SM_ABNORMAL_EFFECT` / `SM_DELETE` | side-effect descriptors attached to two-way operation steps | Packet / Serialization Dependencies | Partial | Unit Tested | Needs Verification | Descriptor composition references existing packet support statuses. `SmPlayerInfo` enemy/aggro behavior remains partial; `SmPlayerStance` and `SmAbnormalEffect` remain missing. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` action `3` fanout | non-live operation side-effect attachment plus known-list population stack | Service / Fanout Prerequisite | Partial | Regression Tested | Needs Verification | Future known-list fanout can now carry operation-step packet descriptors. Live scheduled callbacks, sockets, movement, cooldown, and dispatch remain disabled. |

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: 1 operation side-effect attachment service plus 5 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: 1 live controller side-effect dispatcher, 1 live world known-list callback path, 1 `SmPlayerInfo` enemy/aggro packet gap, 1 `SmPlayerStance` packet, 1 `SmAbnormalEffect` packet, 1 live bind-point scheduled callback path, and 1 Java runtime capture path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Attachment service is non-live and unwired.
- It depends on operation-plan steps and caller-supplied directional facts.
- It does not execute live `KnownList`, `PlayerController`, packet sends, `notKnow`, or target cleanup.
- `SmPlayerInfo` enemy/aggro behavior remains partial.
- `SmPlayerStance` and `SmAbnormalEffect` packet classes are still missing.
- No Java runtime packet-order capture or golden-byte comparison was performed.
- Threading, reflection, date/time, precision/rounding, and serialization behavior remain unverified for live known-list callback dispatch.

## Next Work Options

### Recommended Sequential Task

- Task: Add a disabled integration composition that carries operation side-effect attachments through `PlayerKnownListPopulationPlanService` results.
- Scope:
  - Extend population composition with optional directional side-effect facts.
  - Attach operation side-effect descriptors to each operation plan generated by the population service.
  - Keep descriptor attachment optional or metadata-only.
  - Do not wire sockets, `GameServerConnection`, movement, scheduler, or live world lifecycle mutation.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Population result side-effect attachment composition | existing population service/test plus docs | Medium | Best next executable step; touches existing files, so keep exclusive. |
| B | `SmPlayerInfo(enemy)` serializer audit | read-only or focused packet tests | Medium | Needed before live player-see dispatch parity. |
| C | `SmPlayerStance` packet serializer prerequisite | new packet/tests/docs | Medium | Isolated packet work if Java serializer is inspected first. |
| D | `SmAbnormalEffect` packet serializer audit | read-only analysis | Medium | Depends on effect model readiness. |
| E | Attachment service edge tests | tests only | Low | Add clear-plan supplied-animation coverage without production changes. |

### Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Explorer | Inspect Java `SM_PLAYER_INFO(enemy)` and `SM_PLAYER_STANCE` packet serializer gaps | read-only Java/C# packet inspection | all writes |
| Orchestrator | Implement population-side attachment composition | `PlayerKnownListPopulationPlanService.cs`, its tests, docs | packet serializers, live `GameServerConnection`, socket dispatch |

### Do Not Parallelize

- Multiple agents editing `PlayerKnownListPopulationPlanService.cs`.
- Live known-list mutation with packet dispatch.
- `GameServerConnection` sends with descriptor attachment.
- Packet serializer implementation and population attachment integration in the same unit.
- Shared progress/handoff/parity docs; keep them orchestrator-owned.

## Context Needed By Next Session

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/controllers/PlayerController.java`
  - `game-server/src/com/aionemu/gameserver/world/knownlist/KnownList.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PLAYER_INFO.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PLAYER_STANCE.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_ABNORMAL_EFFECT.java`
- C# surfaces:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListOperationSideEffectAttachmentService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListPlayerSideEffectPlanService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListTwoWayOperationPlanService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListPopulationPlanService.cs`
  - packet classes under `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets`
- Latest completed commits:
  - `cc791d50c [Phase 6][UOW-1261] Add player known-list population composition`
  - `b5542cdf5 [Phase 6][UOW-1262] Add player known-list packet side-effect planner`
  - next commit should be `[Phase 6][UOW-1263] Attach player known-list operation side effects`

Keep live bind-point behavior disabled until Java-equivalent known-list population, source-first fanout execution, live scheduled callback dispatch, final movement packet ordering, live scheduled task ownership, concrete packet serializers, and Java packet/runtime validation have focused parity slices.
