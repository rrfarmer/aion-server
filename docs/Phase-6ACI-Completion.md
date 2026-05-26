# Phase 6ACI Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1251
Status: Phase 6 continues; bind-point scheduled Kinah now has SQL/send adapter seams, visible-distance fanout characterization, source-first known-list trace metadata, player known-list membership metadata, non-live send-policy metadata, and a disabled source-first fanout execution-plan composition. Live known-list population, socket execution, movement, and `GameServerConnection` dispatch remain disabled.

## Session Summary

UOW-1251 added `BindPointTeleportKnownListFanoutExecutionPlanService`. It composes a `BindPointTeleportFanoutPlan`, optional `PlayerKnownListMembershipSnapshot`, source-first trace metadata, and send-policy metadata into one disabled execution plan. It keeps `SendsPackets=false`.

Files changed:

- `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportKnownListFanoutExecutionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/BindPointTeleportKnownListFanoutExecutionPlanServiceTests.cs`
- `docs/Phase-6-BindPointTeleport-KnownListFanoutParity.md`
- `docs/Phase-6-BindPointTeleport-KnownListFanoutExecutionPlan.md`
- `docs/Phase-6-BindPointTeleport-KnownListFanoutSendPolicy.md`
- `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
- `docs/Phase-6-BindPointTeleport-ScheduledKinah-LiveAdapter-FinalReadiness.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ACI-Completion.md`

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "BindPointTeleportKnownListFanoutExecutionPlanServiceTests" --nologo` passed 3 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport" --nologo` passed 199 tests.
- No Java runtime fanout capture was run.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1251

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` scheduled action `3` fanout | `Aion.GameServer.Services.BindPointTeleportKnownListFanoutExecutionPlanService` | Service / Disabled Execution Plan | Partial | Unit Tested | Needs Verification | Composes packet fanout plan, membership snapshot, source-first trace, and send-policy metadata. It does not execute the scheduled callback or send packets. |
| `com.aionemu.gameserver.utils.PacketSendUtility.broadcastPacket(Player,AionServerPacket,boolean)` | `BindPointTeleportKnownListFanoutExecutionPlanService`; trace/send-policy services | Network Utility / Expected Fanout Composition | Partial | Regression Tested | Needs Verification | Source-first ordering, known-list membership, online gating, and failure continuation are composed as metadata. No live socket send or Java runtime capture. |
| `com.aionemu.gameserver.world.knownlist.KnownList.forEachPlayer` | `PlayerKnownListMembershipService`; membership adapter; execution plan service | Known-List Traversal Composition | Partial | Regression Tested | Needs Verification | Membership snapshots feed the trace and execution plan. Live world known-list population and runtime ordering remain unverified. |
| `com.aionemu.gameserver.utils.PacketSendUtility.sendPacket(Player,AionServerPacket)` | `BindPointTeleportKnownListFanoutSendPolicyService` inside execution plan | Packet Utility / Send Policy Composition | Partial | Regression Tested | Needs Verification | Online/failure policy is composed but no actual send occurs. |

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 1 disabled execution-plan composition service plus 3 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: 1 live world known-list population path, 1 live socket send adapter, 1 live movement adapter, 1 `GameServerConnection` dispatch path, and 1 Java runtime capture path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- The composed plan is still disabled and never sends packets.
- Live known-list population, connection lookup, and Java logging are not executed.
- Scheduled Kinah callback dispatch, final movement, `GameServerConnection`, and Java runtime fanout capture remain disabled or missing.
- Reflection behavior did not change. Date/time behavior did not change. Serialization, threading, packet-order, dirty-state persistence, live known-list membership, and movement parity remain `Needs Verification`.

## Next Work Options

### Recommended Sequential Task

- Task: Audit live C# world/player state for the safest future source of real known-list population.
- Scope:
  - Inspect current player visibility/registry surfaces.
  - Identify whether a shared player-known-list can be populated without destabilizing NPC/housing/summon visibility.
  - Keep socket execution and `GameServerConnection` dispatch disabled.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Live known-list population audit | docs only | Low/Medium | Best next fanout blocker. |
| B | Disabled opt-in socket executor boundary | new service/test pair | Medium | Should consume execution plan but stay unwired. |
| C | Java packet/fanout observer design | docs only | Low/Medium | Useful before runtime capture tooling exists. |
| D | Movement side-effect readiness audit | docs only | Low/Medium | Still blocked after fanout. |

### Do Not Parallelize

- Live known-list population and socket execution in one unit.
- `GameServerConnection` dispatch with disabled execution-plan metadata.
- Shared progress/handoff/parity docs; keep them orchestrator-owned.

## Context Needed By Next Session

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/utils/PacketSendUtility.java`
  - `game-server/src/com/aionemu/gameserver/world/knownlist/KnownList.java`
  - `game-server/src/com/aionemu/gameserver/utils/collections/CollectionUtil.java`
  - `game-server/src/com/aionemu/gameserver/model/gameobjects/player/Player.java`
  - `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`
- C# surfaces:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListMembershipService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportKnownListFanoutMembershipAdapterService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportKnownListFanoutTraceService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportKnownListFanoutSendPolicyService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportKnownListFanoutExecutionPlanService.cs`
  - `docs/Phase-6-BindPointTeleport-KnownListFanoutExecutionPlan.md`
- Latest completed commits:
  - `a7ac76360 [Phase 6][UOW-1249] Add bind point teleport known-list membership metadata`
  - `e7946efdd [Phase 6][UOW-1250] Add bind point teleport known-list send policy metadata`
  - next commit should be `[Phase 6][UOW-1251] Add bind point teleport known-list execution plan metadata`

Keep live bind-point behavior disabled until known-list membership population, source-first fanout execution, live socket sends, final movement packet ordering, live scheduled task ownership, and Java packet/runtime validation have focused parity slices.

