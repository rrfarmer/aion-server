# Phase 6ASM Completion - Position Correction Packets

Date: 2026-05-28
Unit of Work: UOW-1671
Status: Complete after focused packet tests

## Scope

This unit added isolated C# packet bodies for Java `SM_POSITION` and `SM_POSITION_SELF`.

These packets instantly move an object or player client-side and are used by Java movement/effect correction paths. This unit ports packet bodies only; it does not enable live movement-correction dispatch or verify the `SM_POSITION_SELF` / `CM_POSITION_SELF` response workflow.

## Completed Work

- Added `SmPosition`.
- Added `ObjectPositionSnapshot`.
- Added `SmPositionSelf`.
- Added `PositionSelfSnapshot`.
- Modeled Java `SM_POSITION` opcode `204` payload: object id, x/y/z floats, heading.
- Modeled Java `SM_POSITION_SELF` opcode `21` payload: x/y/z floats, heading.
- Added focused packet payload tests.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, file ownership, parity table, tests, risks, metrics, and next-unit guidance.

## Java Parity Notes

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_POSITION.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_POSITION_SELF.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_POSITION_SELF.java`
  - `game-server/src/com/aionemu/gameserver/model/gameobjects/VisibleObject.java`
- Java `SM_POSITION.writeImpl` writes object id, x, y, z, and heading.
- Java `SM_POSITION_SELF.writeImpl` writes x, y, z, and heading.
- C# uses snapshot records instead of live `VisibleObject` references.

## Validation

Ran:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~SmPositionPacketsTests|FullyQualifiedName~GamePacketTests"
```

Result: passed 242 tests.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| `SM_POSITION` / `SM_POSITION_SELF` packet port | packet classes/tests | Low | Yes | Small deterministic packet bodies sharing float/heading shape. |
| Run gated DB integration | DB integration harness | Medium | No | Deferred because no `AION_GAMESERVER_DB_*` env vars were present. |
| Java runtime heading vectors | PositionUtil vector tests/artifacts | Low | No | Useful for stronger heading parity, but packet unit was more direct. |
| Zone handler source audit | read-only Java/C# docs | Low | No | Useful before live callback work, but outside packet body scope. |

## File Ownership Map

| Agent | Scope | Allowed Files | Forbidden Files | Expected Output |
| --- | --- | --- | --- | --- |
| Orchestrator | Position packet classes, tests, docs, commit | `SmPosition.cs`, `SmPositionSelf.cs`, `SmPositionPacketsTests.cs`, progress/handoff docs | Java source writes, live movement dispatch, unrelated services/tests | Implemented and documented UOW-1671. |
| Sub-agents | None | None | All files | Not spawned because selected work touched a small paired packet/test unit plus shared docs. |

No sub-agent was spawned for UOW-1671 because the selected packet pair was small and shared docs remained Orchestrator-owned.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `SmPosition_WritesObjectPositionAndHeadingLikeJava` | Added | Packet writes object id, x/y/z floats, and heading in Java order. | Java `SM_POSITION.writeImpl`. |
| `SmPositionSelf_WritesCoordinatesAndHeadingLikeJava` | Added | Packet writes x/y/z floats and heading in Java order. | Java `SM_POSITION_SELF.writeImpl`. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_POSITION` | `Aion.GameServer.Network.Aion.ServerPackets.SmPosition` | Server Packet | Complete | Unit Tested | Partial Parity | C# models opcode `204` and payload fields: object id, x, y, z, heading. It uses snapshot input instead of live `VisibleObject`. No Java runtime golden/encrypted frame comparison. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_POSITION_SELF` | `Aion.GameServer.Network.Aion.ServerPackets.SmPositionSelf` | Server Packet | Complete | Unit Tested | Partial Parity | C# models opcode `21` and payload fields: x, y, z, heading. Client response `CM_POSITION_SELF` is already parsed but no live request/response workflow is verified. |
| `com.aionemu.gameserver.model.gameobjects.VisibleObject` | `ObjectPositionSnapshot`; `PositionSelfSnapshot` | DTO Projection | Partial | Unit Tested | Partial Parity | C# uses snapshots for object id, coordinates, and heading. Live object position, heading signed-byte behavior, movement controller side effects, and object equality are unported for this path. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_POSITION_SELF` | `Aion.GameServer.Network.Aion.ClientPackets.CmPositionSelf` | Client Packet Boundary | Existing | Regression Tested | Partial Parity | Existing C# parser recognizes the response packet. This unit does not verify the Java request/response workflow or live movement cancellation semantics. |

## Remaining Risks

- Live movement correction dispatch remains unported; no server path sends `SmPosition` or `SmPositionSelf`.
- Float precision is source-derived through packet buffer behavior but not compared with Java runtime frames.
- Heading signed-byte behavior for these packet paths is not runtime-compared.
- `SM_POSITION_SELF` response workflow with `CM_POSITION_SELF` remains unverified.
- Gated charge-all DB integration execution still needs a real MySQL environment.
- `docs/commit-conventions.md` is still missing.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped rows.
- Total artifacts ported: 2 server packets, 2 DTO projections, and 2 focused regressions.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 0 grouped rows explicitly marked Needs Verification; all rows are Partial Parity with documented live/runtime gaps.
- Total blocked artifacts: live movement correction dispatch, Java runtime packet capture, encrypted frame comparison, `SM_POSITION_SELF` response workflow, DB-backed charge-all integration run.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Next best unit:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| Run gated DB integration | disposable MySQL schema | Set `AION_GAMESERVER_DB_INTEGRATION=1` plus DB env vars if a DB is available. |
| Movement-correction packet-plan boundary | non-live movement/effect helper/tests | Model where Java would create `SM_POSITION` / `SM_POSITION_SELF` without live dispatch. |
| Another isolated packet parity unit | packet class/tests | Continue packet-body parity while live dispatch remains incomplete. |

## Next Work Options

## Recommended Sequential Task

- Task: run the gated DB integration suite if a disposable DB is available; otherwise add a non-live movement-correction packet-plan boundary or continue with another isolated packet parity unit.
- Why: position correction packet bodies exist now, but no non-live factory boundary or live dispatch path creates them.
- Files: likely no file changes for DB execution; otherwise focused movement packet-plan service/test pair plus docs, or exact packet files for the selected packet unit.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
| --- | --- | --- | --- | --- |
| A | Movement-correction source audit | read-only Java effects and packets | Low | Convert to writes only after ownership is reserved. |
| B | Isolated packet audit | packet Java/C# tests read-only unless selected | Low | Avoid live dispatch work. |
| C | Zone handler source audit | read-only Java zone handler files | Low | Useful before live zone callbacks. |
| D | DB integration setup check | env/read-only status | Medium | Only if a disposable DB is known to be available. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
| --- | --- | --- | --- |
| Orchestrator | Pick DB run, movement packet-plan, or next packet unit | exact selected files | Java writes, live movement dispatch, unrelated shared files |
| Read-only Agent | Audit movement correction or next packet source | read-only inspection | all writes |

## Do Not Parallelize

- Java source files: read-only only.
- Live movement dispatch, live target dispatch, live NPC target broadcast, live scheduler mutation, live nearby dispatch, live weather mutation, live actor mutation, and live generated-zone writes: still high risk and intentionally disabled.
- Shared movement/packet helpers, packet helper/test fixtures, and DB integration setup: one owner only.

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1671] Add position correction packet bodies
```

Files changed in this unit:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmPosition.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmPositionSelf.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/SmPositionPacketsTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ASM-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
