# Phase 6ASN Completion - Movement Correction Packet Plan

Date: 2026-05-28
Unit of Work: UOW-1672
Status: Complete after focused planner and packet tests

## Scope

This unit added a non-live C# planning boundary for Java movement-correction packet creation around `SM_POSITION` and `SM_POSITION_SELF`.

The planner records where Java would broadcast an object correction, broadcast-and-receive an object correction, or send a self correction that expects `CM_POSITION_SELF`. It does not enable live movement dispatch, live `PacketSendUtility`, world mutation, effect-controller mutation, skill-engine dispatch, or AI handler runtime behavior.

## Completed Work

- Added `MovementCorrectionPacketPlanService`.
- Added `MovementCorrectionPacketPlan`.
- Added `MovementCorrectionPacketPlanStatus`.
- Modeled Java `broadcastPacketAndReceive(..., new SM_POSITION(object))` as a non-live plan with `ShouldBroadcastPacket` and `ShouldBroadcastAndReceive`.
- Modeled Java `broadcastPacket(..., new SM_POSITION(object))` as a non-live plan with broadcast-only intent.
- Modeled Java `SM_POSITION_SELF` as a non-live owner-send plan that expects `CM_POSITION_SELF`.
- Added a C# safety block for invalid object ids before creating `SmPosition`.
- Added focused tests that assert plan decisions and packet payload bytes.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, file ownership, parity table, tests, risks, metrics, and next-unit guidance.

## Java Parity Notes

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_POSITION.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_POSITION_SELF.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_POSITION_SELF.java`
  - `game-server/src/com/aionemu/gameserver/skillengine/effect/ConfuseEffect.java`
  - `game-server/src/com/aionemu/gameserver/skillengine/effect/FearEffect.java`
  - `game-server/src/com/aionemu/gameserver/skillengine/effect/SimpleRootEffect.java`
  - `game-server/data/handlers/ai/instance/eternalBastion/EternalBastionMountableAI.java`
  - `game-server/src/com/aionemu/gameserver/utils/PacketSendUtility.java`
- Java `ConfuseEffect.endEffect` and `FearEffect.endEffect` abort movement and call `broadcastPacketAndReceive` with `SM_POSITION`.
- Java `SimpleRootEffect.startEffect` updates world position and calls `broadcastPacket` with `SM_POSITION` for non-player sub effects.
- Java `EternalBastionMountableAI.tryMountNpc` updates player position, calls `broadcastPacketAndReceive` with `SM_POSITION`, applies a skill, and deletes the NPC owner.
- Java `SM_POSITION_SELF` documents a `CM_POSITION_SELF` response. The current grep found no direct Java construction site.
- C# uses snapshot records and non-live send-intent flags instead of live `VisibleObject`, `PacketSendUtility`, `World`, effect, skill, or AI state.

## Validation

Ran:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~MovementCorrectionPacketPlanServiceTests|FullyQualifiedName~SmPositionPacketsTests|FullyQualifiedName~GamePacketTests"
```

Result: passed 246 tests.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| Movement-correction packet-plan boundary | service/tests/docs | Low | Yes | Small non-live boundary for packet creation and send intent after UOW-1671 packet bodies. |
| Run gated DB integration | DB integration harness | Medium | No | Deferred because no disposable DB environment is present. |
| Another isolated packet audit | packet class/tests | Low | No | Still viable after this boundary is documented. |
| Java runtime position packet vectors | vector artifacts/tests | Low | No | Useful for stronger packet parity, but planner boundary was the immediate documented gap. |

## File Ownership Map

| Agent | Scope | Allowed Files | Forbidden Files | Expected Output |
| --- | --- | --- | --- | --- |
| Orchestrator | Movement-correction planner, tests, docs, commit | `MovementCorrectionPacketPlanService.cs`, `MovementCorrectionPacketPlanServiceTests.cs`, progress/handoff docs | Java source writes, live movement dispatch, world mutation, effect controller mutation, AI mount integration, unrelated services/tests | Implemented and documented UOW-1672. |
| Sub-agents | None | None | All files | Not spawned because selected work touched one small service/test pair plus shared docs. |

No sub-agent was spawned for UOW-1672 because the selected service boundary was small and shared docs remained Orchestrator-owned.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `CreateBroadcastObjectPlan_CreatesSmPositionAndBroadcastReceiveIntentLikeJavaEffects` | Added | `SmPosition` packet creation, broadcast-and-receive intent, object id/x/y/z/heading payload. | Java `ConfuseEffect.endEffect`, `FearEffect.endEffect`, `EternalBastionMountableAI.tryMountNpc`, and `SM_POSITION.writeImpl`. |
| `CreateBroadcastObjectPlan_CanModelSimpleRootBroadcastWithoutReceive` | Added | `SmPosition` packet creation with broadcast-only intent. | Java `SimpleRootEffect.startEffect` and `SM_POSITION.writeImpl`. |
| `CreateBroadcastObjectPlan_BlocksInvalidObjectBeforePacketCreation` | Added | Invalid snapshot object id creates no packet or send intent. | C# safety boundary for Java live `VisibleObject` requirement. |
| `CreateSelfPlan_CreatesSmPositionSelfAndOwnerResponseIntentLikeJavaPacketDoc` | Added | `SmPositionSelf` packet creation, owner-send intent, expected response flag, x/y/z/heading payload. | Java `SM_POSITION_SELF.writeImpl` and `CM_POSITION_SELF` packet doc. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_POSITION` | `Aion.GameServer.Network.Aion.ServerPackets.SmPosition`; `Aion.GameServer.Services.MovementCorrectionPacketPlanService.CreateBroadcastObjectPlan` | Server Packet / Service Boundary | Complete packet, Partial planner integration | Unit Tested | Partial Parity | C# planner creates `SmPosition` from an `ObjectPositionSnapshot` and records broadcast intent. It does not read a live `VisibleObject`, mutate world position, dispatch to known lists, include/exclude the source connection, or verify Java runtime packet frames. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_POSITION_SELF` | `Aion.GameServer.Network.Aion.ServerPackets.SmPositionSelf`; `Aion.GameServer.Services.MovementCorrectionPacketPlanService.CreateSelfPlan` | Server Packet / Service Boundary | Complete packet, Partial planner integration | Unit Tested | Partial Parity | C# planner creates `SmPositionSelf`, records owner-send intent, and records expected `CM_POSITION_SELF`. No direct Java construction site was found by current grep, and the live request/response workflow remains unverified. |
| `com.aionemu.gameserver.skillengine.effect.ConfuseEffect` | `Aion.GameServer.Services.MovementCorrectionPacketPlanService.CreateBroadcastObjectPlan` | Effect Boundary | Partial | Unit Tested | Partial Parity | Java aborts movement and broadcasts/receives `SM_POSITION` on effect end. C# models only packet creation and broadcast intent; abnormal-state cleanup, movement abort, AI state changes, scheduled confuse task behavior, threading, and effect-controller side effects remain unported here. |
| `com.aionemu.gameserver.skillengine.effect.FearEffect` | `Aion.GameServer.Services.MovementCorrectionPacketPlanService.CreateBroadcastObjectPlan` | Effect Boundary | Partial | Unit Tested | Partial Parity | Java aborts movement and broadcasts/receives `SM_POSITION` on effect end. C# models only packet creation and broadcast intent; abnormal-state cleanup, movement abort, AI state changes, observer behavior, scheduled fear task behavior, threading, and effect-controller side effects remain unported here. |
| `com.aionemu.gameserver.skillengine.effect.SimpleRootEffect` | `Aion.GameServer.Services.MovementCorrectionPacketPlanService.CreateBroadcastObjectPlan(receiveAfterBroadcast: false)` | Effect Boundary | Partial | Unit Tested | Partial Parity | Java updates world position for sub effects and broadcasts `SM_POSITION` for non-player effected creatures. C# models packet creation and broadcast-without-receive intent only; target-location calculation, `World.updatePosition`, player `onStopMove`, abnormal state, and non-player filtering remain outside this unit. |
| `ai.instance.eternalBastion.EternalBastionMountableAI` | `Aion.GameServer.Services.MovementCorrectionPacketPlanService.CreateBroadcastObjectPlan` | AI Handler Boundary | Not Started | Unit Tested boundary only | Needs Verification | Java updates player position to the mountable NPC, broadcasts/receives `SM_POSITION`, applies a skill, and deletes the NPC. C# only has a reusable packet-plan boundary; handler runtime, inventory decrement, race skill choice, world mutation, skill engine, and NPC deletion are not ported in this unit. |
| `com.aionemu.gameserver.utils.PacketSendUtility.broadcastPacketAndReceive` | `Aion.GameServer.Services.MovementCorrectionPacketPlan.ShouldBroadcastAndReceive` | Utility Boundary | Partial | Unit Tested boundary only | Needs Verification | C# records intent but performs no live broadcast. Recipient selection, ordering, visibility, source inclusion, encryption, response handling, and threading remain unverified. |
| `com.aionemu.gameserver.utils.PacketSendUtility.broadcastPacket` | `Aion.GameServer.Services.MovementCorrectionPacketPlan.ShouldBroadcastPacket` | Utility Boundary | Partial | Unit Tested boundary only | Needs Verification | C# records broadcast-only intent for `SimpleRootEffect` shape. Live packet dispatch and known-list behavior are not implemented here. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_POSITION_SELF` | `Aion.GameServer.Network.Aion.ClientPackets.CmPositionSelf`; `MovementCorrectionPacketPlan.ExpectsClientPositionSelfResponse` | Client Packet Boundary | Existing parser, Partial workflow | Regression Tested existing parser; Unit Tested planner intent | Partial Parity | Existing C# parser remains the response boundary. This unit only records that `SM_POSITION_SELF` expects a response; it does not verify the Java live response workflow or movement cancellation semantics. |
| `com.aionemu.gameserver.model.gameobjects.VisibleObject` | `ObjectPositionSnapshot`; `PositionSelfSnapshot`; `MovementCorrectionPacketPlan` | DTO Projection | Partial | Unit Tested | Partial Parity | C# still uses snapshots for object id, x/y/z, and heading. Live object reference behavior, heading signed-byte source type, position mutation, equality, null behavior, and threading remain unported for this path. |

## Remaining Risks

- Live movement correction dispatch remains unported; no server path calls `MovementCorrectionPacketPlanService`.
- Java `PacketSendUtility.broadcastPacketAndReceive` and `broadcastPacket` semantics are only recorded as intent. Recipient selection, source inclusion, packet ordering, visibility, encryption, and threading are not verified.
- Effect end/start side effects in `ConfuseEffect`, `FearEffect`, and `SimpleRootEffect` remain unported for this unit.
- `EternalBastionMountableAI` runtime behavior remains not started outside the packet-plan dependency.
- `SM_POSITION_SELF` has no currently discovered direct Java construction site and the `CM_POSITION_SELF` response workflow remains unverified.
- Float precision and heading signed-byte behavior are source-derived through packet buffer tests but not Java runtime-compared.
- Gated charge-all DB integration execution still needs a real MySQL environment.
- `docs/commit-conventions.md` is still missing.

## Summary Metrics

- Total Java artifacts discovered: 10 grouped rows.
- Total artifacts ported: 1 non-live movement-correction packet-plan service, 1 plan DTO, 1 status enum, and 4 focused planner regressions.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 3 grouped rows explicitly marked Needs Verification; remaining rows are Partial Parity with documented live/runtime gaps.
- Total blocked artifacts: live movement correction dispatch, live `PacketSendUtility` semantics, effect controller side effects, world position mutation, AI mount runtime integration, Java runtime packet capture, encrypted frame comparison, `SM_POSITION_SELF` response workflow, DB-backed charge-all integration run.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Next best unit:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| Run gated DB integration | disposable MySQL schema | Set `AION_GAMESERVER_DB_INTEGRATION=1` plus DB env vars if a DB is available. |
| Java runtime position packet vectors | vector artifacts/tests | Capture Java outputs for `SM_POSITION` and `SM_POSITION_SELF` float/heading payloads and compare C# against them. |
| Another isolated packet parity unit | packet class/tests | Continue packet-body parity while live dispatch remains incomplete. |
| Movement-correction effect outcome planner | non-live effect helper/tests | Model `ConfuseEffect.endEffect` and `FearEffect.endEffect` side-effect ordering without live mutation. |

## Next Work Options

## Recommended Sequential Task

- Task: run the gated DB integration suite if a disposable DB is available; otherwise add Java runtime/golden vector coverage for `SM_POSITION`/`SM_POSITION_SELF`, continue with another isolated packet parity unit, or add a narrow non-live effect outcome planner for `ConfuseEffect.endEffect`/`FearEffect.endEffect`.
- Why: movement-correction packets and a non-live planner now exist, but no Java runtime packet vectors or live effect/movement dispatch are verified.
- Files: likely no file changes for DB execution; otherwise focused vector/test files, exact packet files for the selected packet unit, or one effect planner service/test pair plus docs.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
| --- | --- | --- | --- | --- |
| A | Java position packet vector audit | read-only Java packet/call-site inspection, optional new vector test files if selected | Low | Keep live dispatch disabled. |
| B | Isolated packet audit | read-only packet source discovery | Low | Convert to writes only after ownership is reserved. |
| C | Effect source audit | read-only `ConfuseEffect`/`FearEffect`/`SimpleRootEffect` analysis | Low | Useful before a non-live effect outcome planner. |
| D | DB integration setup check | env/read-only status | Medium | Only if a disposable DB is known to be available. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
| --- | --- | --- | --- |
| Orchestrator | Pick DB run, vector coverage, next packet unit, or effect planner | exact selected files | Java writes, live movement dispatch, live effect mutation, live packet broadcast, unrelated shared files |
| Read-only Agent | Audit Java call sites or next packet source | read-only inspection | all writes |

## Do Not Parallelize

- Java source files: read-only only.
- Live movement dispatch, live packet broadcast, live effect-controller mutation, live world position mutation, live skill-engine dispatch, live target dispatch, live NPC target broadcast, live scheduler mutation, live nearby dispatch, live weather mutation, live actor mutation, and live generated-zone writes: still high risk and intentionally disabled.
- Shared movement/packet helpers, packet helper/test fixtures, DB integration setup, progress docs, and handoff docs: one owner only.

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1672] Add movement correction packet planner
```

Files changed in this unit:

- `dotnetConversion/src/Aion.GameServer/Services/MovementCorrectionPacketPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/MovementCorrectionPacketPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ASN-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
