# Phase 6ASI Completion - Look At Object Packet

Date: 2026-05-28
Unit of Work: UOW-1667
Status: Complete after focused packet tests

## Scope

This unit added the isolated C# packet body for Java `SM_LOOKATOBJECT`.

The packet is used by Java `NpcController.onTargetChanged` to broadcast NPC target/heading changes. This unit only ports the packet body and snapshot input; it does not enable live NPC target-change dispatch or broadcasting.

## Completed Work

- Added `SmLookAtObject`.
- Added `LookAtObjectSnapshot`.
- Modeled Java opcode `40`.
- Modeled Java payload order: visible object id, target object id, heading.
- Modeled Java no-target behavior by writing target id `0`.
- Added focused packet payload tests.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, file ownership, parity table, tests, risks, metrics, and next-unit guidance.

## Java Parity Notes

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LOOKATOBJECT.java`
  - `game-server/src/com/aionemu/gameserver/controllers/NpcController.java`
  - `game-server/src/com/aionemu/gameserver/model/gameobjects/VisibleObject.java`
  - `game-server/src/com/aionemu/gameserver/utils/PacketSendUtility.java`
- Java constructor captures `visibleObject`, `targetObjectId` as zero when target is null, and `heading`.
- Java `writeImpl` writes `writeD(objectId)`, `writeD(targetObjectId)`, and `writeC(heading)`.
- C# uses `LookAtObjectSnapshot` instead of live `VisibleObject` references.

## Validation

Ran:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~SmLookAtObjectPacketTests|FullyQualifiedName~GamePacketTests"
```

Result: passed 242 tests.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| Live-disabled target connection seam | `GameServerConnection.cs` and tests | Medium | No | Deferred because it touches a very large live connection handler and target references are still snapshot-only. |
| Run gated DB integration | DB integration harness | Medium | No | Deferred because no `AION_GAMESERVER_DB_*` env vars were present. |
| `SM_LOOKATOBJECT` packet port | packet class/tests | Low | Yes | Small deterministic missing packet body. |
| Zone handler source audit | read-only Java/C# docs | Low | No | Useful before live callback work, but outside packet body scope. |

## File Ownership Map

| Agent | Scope | Allowed Files | Forbidden Files | Expected Output |
| --- | --- | --- | --- | --- |
| Orchestrator | `SM_LOOKATOBJECT` packet class, tests, docs, commit | `SmLookAtObject.cs`, `SmLookAtObjectPacketTests.cs`, progress/handoff docs | Java source writes, live NPC target dispatch, live connection targeting, unrelated services/tests | Implemented and documented UOW-1667. |
| Sub-agents | None | None | All files | Not spawned because selected work touched one packet/test pair plus shared docs. |

No sub-agent was spawned for UOW-1667 because the selected packet was small and shared docs remained Orchestrator-owned.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `SmLookAtObject_WritesObjectTargetAndHeadingLikeJava` | Added | Packet writes object id, target object id, and heading in Java order. | Java `SM_LOOKATOBJECT.writeImpl`. |
| `SmLookAtObject_WritesZeroTargetWhenJavaVisibleObjectHasNoTarget` | Added | No-target snapshot writes target id zero and heading byte. | Java constructor null-target branch. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LOOKATOBJECT` | `Aion.GameServer.Network.Aion.ServerPackets.SmLookAtObject` | Server Packet | Complete | Unit Tested | Partial Parity | C# models opcode `40` and payload fields: visible object id, target id, heading. Null target is represented by snapshot target id `0`. No Java runtime golden/encrypted frame comparison was produced. |
| `com.aionemu.gameserver.model.gameobjects.VisibleObject` | `LookAtObjectSnapshot` | DTO Projection | Partial | Unit Tested | Partial Parity | C# uses a snapshot for object id, target id, and heading instead of the live Java object/reference hierarchy. Live heading range, target reference nullability, and object equality behavior remain unverified. |
| `com.aionemu.gameserver.controllers.NpcController.onTargetChanged` | future NPC target-change dispatch; packet class only in this unit | Controller Boundary | Not Started | No Tests | Needs Verification | Java broadcasts `SM_LOOKATOBJECT` when NPC target changes and other AI conditions pass. C# only has the packet body; live NPC controller/broadcast behavior remains unported. |
| `com.aionemu.gameserver.utils.PacketSendUtility.broadcastPacket` | future broadcast integration | Utility Boundary | Not Started | No Tests | Needs Verification | Newly documented dependency for live `SM_LOOKATOBJECT` use. Recipient selection, source inclusion, ordering, threading, and visibility remain unported for this path. |

## Remaining Risks

- Live `NpcController.onTargetChanged` behavior remains unported; no `SM_LOOKATOBJECT` broadcast occurs.
- C# uses `LookAtObjectSnapshot` instead of live `VisibleObject` references.
- Heading byte range/overflow behavior is source-derived but not compared against Java runtime frames.
- Broadcast recipient selection, packet ordering, threading, and source-player inclusion/exclusion remain unverified.
- No Java runtime golden frame or encrypted frame comparison was produced for `SM_LOOKATOBJECT`.
- Gated charge-all DB integration execution still needs a real MySQL environment.
- `docs/commit-conventions.md` is still missing.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped rows.
- Total artifacts ported: 1 server packet, 1 DTO projection, and 2 focused regressions.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 2 grouped rows explicitly marked Needs Verification; remaining rows are Partial Parity with documented non-live gaps.
- Total blocked artifacts: live NPC target-change dispatch, broadcast utility integration, Java runtime packet capture, encrypted frame comparison, DB-backed charge-all integration run.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Next best unit:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| Run gated DB integration | disposable MySQL schema | Set `AION_GAMESERVER_DB_INTEGRATION=1` plus DB env vars if a DB is available. |
| NPC target-change packet-plan boundary | non-live NPC controller helper/tests | Use `SmLookAtObject` to model Java `NpcController.onTargetChanged` packet creation without live broadcast. |
| Another isolated packet parity unit | packet class/tests | Continue small packet-body parity while live dispatch remains incomplete. |

## Next Work Options

## Recommended Sequential Task

- Task: run the gated DB integration suite if a disposable DB is available; otherwise add a non-live NPC target-change packet-plan boundary for `NpcController.onTargetChanged`.
- Why: `SM_LOOKATOBJECT` packet body exists now, but the Java NPC target-change send/broadcast boundary is not modeled.
- Files: likely no file changes for DB execution; otherwise a focused NPC target-change plan service/test pair plus docs.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
| --- | --- | --- | --- | --- |
| A | NPC target-change source audit | read-only `NpcController` and packet source | Low | Convert to writes only after ownership is reserved. |
| B | Isolated packet audit | packet Java/C# tests read-only unless selected | Low | Avoid live dispatch work. |
| C | Zone handler source audit | read-only Java zone handler files | Low | Useful before live zone callbacks. |
| D | DB integration setup check | env/read-only status | Medium | Only if a disposable DB is known to be available. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
| --- | --- | --- | --- |
| Orchestrator | Pick DB run, NPC packet-plan boundary, or next packet unit | exact selected files | Java writes, live NPC broadcast, unrelated shared files |
| Read-only Agent | Audit NPC target-change or next packet source | read-only inspection | all writes |

## Do Not Parallelize

- Java source files: read-only only.
- Live target dispatch, live NPC target broadcast, live nearby dispatch, live weather mutation, live actor mutation, and live generated-zone writes: still high risk and intentionally disabled.
- Shared targeting helpers, packet helper/test fixtures, and DB integration setup: one owner only.

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1667] Add look-at-object packet body
```

Files changed in this unit:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmLookAtObject.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/SmLookAtObjectPacketTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ASI-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
