# Phase 6 Session 2350 Handoff - Instance Leave Reset Messages

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2350-Completion.md`
- `docs/Phase-6-Session-2350-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

Last completed UOW: UOW-2350, ported Java `InstanceService.onLeaveInstance` reset-warning message planner behavior and missing leave-instance system-message packet helpers.

Relevant completed instance destroy/checker/leave slices:

- `InstanceEmptyInstanceCheckerService` models Java `EmptyInstanceCheckerTask`.
- Portal instance allocation passes the real checker scheduler callback.
- `InstanceRegisteredTeamDisbandService` maps registered team ids to group/alliance runtime membership state.
- `InstanceLeaveMessageService` models Java reset-warning message selection for instance leave.
- `SmSystemMessage` has helpers for `STR_MSG_LEAVE_INSTANCE`, `STR_MSG_LEAVE_INSTANCE_PARTY`, and `STR_MSG_LEAVE_INSTANCE_FORCE`.

Still not proven or not implemented:

- Live teleport/leave callback wiring for `InstanceService.onLeaveInstance`.
- Other runtime instance creation call sites need real scheduler callback review.
- Live forced-exit packet send and teleport mutation.
- Dynamic handler/auto-group destroy call sites invoking `InstanceDestroyWorkflowService`.
- Instance-scoped walker spawn plan cache parity.

## Commits Made

- `[Phase 6][UOW-2350] Port instance leave reset messages`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/src/Aion.GameServer/Services/InstanceLeaveMessageService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/InstanceLeaveMessageServiceTests.cs`
- `docs/Phase-6-Session-2350-Completion.md`
- `docs/Phase-6-Session-2350-Handoff.md`

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~InstanceLeaveMessageServiceTests|FullyQualifiedName~GamePacketTests" --no-restore
```

Result: passed 273, failed 0, skipped 0. The `GamePacketTests` class is broad but directly contains the shared system-message packet-shape assertions and ran quickly. Pre-existing nullable/analyzer warnings remain.

Documentation hygiene:

```powershell
git diff --check
```

Result: passed. Git reported line-ending normalization warnings only.

Focused Java/Maven validation: skipped because no targeted Java unit fixture exists for `InstanceService.onLeaveInstance`; Java source review was used as source-of-truth evidence.

Broad-validation trigger: none.

Broad .NET decision: skipped full project/solution validation because the filtered command compiled the affected project and directly covered the planner/packet contracts.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.instance.InstanceService.onLeaveInstance(Player)` | `Aion.GameServer.Services.InstanceLeaveMessageService.CreateLeaveMessagePlan(...)` | Service Planner | Partial | Unit Tested | Partial Parity | Reset-warning branch order is covered; live callback invocation remains unwired. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_MSG_LEAVE_INSTANCE(int)` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.LeaveInstance(int)` | Packet Helper | Complete | Unit Tested | Verified Parity | Message id `1400044` and parameter serialization covered. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_MSG_LEAVE_INSTANCE_PARTY(int)` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.LeaveInstanceParty(int)` | Packet Helper | Complete | Unit Tested | Verified Parity | Message id `1400045` and parameter serialization covered. |

## Next Sequential UOW

Recommended next production scope: wire live teleport/leave callback behavior if the existing teleport boundary can safely identify the old instance before queued teleport completion. If that boundary is too broad, review and wire the next concrete runtime instance creation call site to `InstanceEmptyInstanceCheckerService.Schedule(...)`.

Java artifacts:

- `game-server/src/com/aionemu/gameserver/services/teleport/TeleportService.java`
- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- `game-server/src/com/aionemu/gameserver/services/teleport/PortalService.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerTeleportService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/InstanceLeaveMessageService.cs`
- adjacent teleport/portal tests discovered during Work Discovery

Focused C# validation recipe:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~InstanceLeaveMessageServiceTests|FullyQualifiedName~GameServerConnectionInstanceCooldownTests.QueueInstancePortalTransferAsync_TeleportsBeforeAddingCooldownLikeJavaPortalService" --no-restore
```

Adjust the second test name after discovery if the live teleport callback is covered by a different nearest boundary test.

Focused Java/Maven command: not expected unless a targeted Java fixture is added; Java source review is likely the practical evidence.

Broad-validation trigger: live teleport side-effect wiring may be a broad trigger if it changes shared position mutation or connection dispatch. Start focused; document any escalation before running broad validation.

## Safe Candidates

- Wire live teleport/leave callback behavior and reset-warning send.
- Wire another runtime instance creation call site to the checker callback.
- Add a narrow live forced-exit adapter if packet-send and teleport mutation boundaries are ready.
- Wire an existing C# instance handler/auto-group destroy call site to `InstanceDestroyWorkflowService`.

Avoid:

- Evidence/reporting-only units.
- Full `.NET` project tests or solution builds without a documented broad-validation trigger.
- Updating `docs/PHASE-6-PROGRESS.md`.
