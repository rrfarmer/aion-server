# Phase 6 Session 2319 Handoff - Registered Group Portal Transfer

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2319-Completion.md`
- `docs/Phase-6-Session-2319-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

Last completed UOW: UOW-2319, registered group-instance portal continuation.

Completed Beshmundir/portal slices relevant to the next work:

- Beshmundir solo/non-group `INSTANCE_ENTRY`: sends `SmSystemMessage.EnterOnlyPartyDon()`.
- Beshmundir group leader `INSTANCE_ENTRY`: sends `SmDialogWindow(targetObjectId, 4762)`.
- Beshmundir grouped non-leader closed-instance `INSTANCE_ENTRY`: sends `SmSystemMessage.InstanceDungeonCantEnterNotOpened()`.
- Beshmundir `SELECT_NONE_1` / `SELECT_NONE_2`: registers question `902050`, sends Java-shaped `SmQuestionWindow`, then dialog `4762`.
- Registered group portal continuation: transfers player into existing team instance, queues teleport, and applies/skips cooldown according to reentry.

Still not proven:

- Beshmundir non-leader member-in-instance movement.
- Beshmundir difficulty question acceptance movement.
- Fresh group-instance allocation and `registerTeam(group)`.
- Generic portal dialog live routing for team plans.
- Java range observer auto-deny behavior for AI requests.
- Real-client/encrypted socket bytes for these branches.

## Commits Made

- `c12d00c4c [Phase 6][UOW-2315] Reject solo Beshmundir walk entry`
- `10b528b55 [Phase 6][UOW-2316] Show Beshmundir leader path dialog`
- `ff1354d09 [Phase 6][UOW-2317] Reject Beshmundir non-leader closed entry`
- `7a644b31f [Phase 6][UOW-2318] Register Beshmundir difficulty request`
- `[Phase 6][UOW-2319] Transfer registered group portals`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerTeleportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInstanceCooldownTests.cs`
- `docs/Phase-6-Session-2319-Completion.md`
- `docs/Phase-6-Session-2319-Handoff.md`

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.teleport.PortalService.port` group registered-instance branch | `Aion.GameServer.Network.Aion.GameServerConnection.QueuePortalContinueTransferAsync` | Service / Connection Boundary | Partial | Focused Boundary Tested | Partial Parity | Registered group instance transfer now sets start position, registers player, queues teleport, and applies/skips cooldown according to reentry. Fresh group allocation and alliance/league branches remain unported. |
| `com.aionemu.gameserver.world.WorldMapInstance.register` / `registerTeam` transfer usage | `Aion.GameServer.World.WorldMapInstanceRuntimeState` | Runtime State | Partial | Focused Boundary Tested | Partial Parity | Existing runtime state is used by registered group transfer. Broader WorldMapInstance parity was not re-audited. |
| `com.aionemu.gameserver.services.teleport.TeleportService.teleportTo` via `PortalService.transfer` | `QueueInstancePortalTransferAsync` / `QueueDelayedTeleportAsync` | Teleport Service | Partial | Focused Boundary Tested | Partial Parity | Uses `TeleportAnimation.FadeOutBeam` and existing delayed teleport packet path. Full TeleportService parity remains partial. |

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~QueuePortalContinueTransferAsync_RegisteredGroupInstanceTransfersAndAppliesCooldown|FullyQualifiedName~QueuePortalContinueTransferAsync_RegisteredGroupReentryTransfersWithoutCooldown|FullyQualifiedName~QueuePortalContinueTransferAsync_GroupPlanWithoutRegisteredInstanceRecordsAllocationNeededWithoutPackets" --no-restore
```

First run failed 3/3 due to team plans being checked after the generic action guard. After moving the team-plan branch before that guard, the same focused command passed 3/3. Pre-existing nullable/analyzer warnings remain.

Focused Java/Maven validation: skipped because no targeted Java fixture exists for this `PortalService.port(...)` group branch. Java source review was used as source-of-truth evidence.

Repository hygiene:

```powershell
git diff --check
```

Result: passed with line-ending normalization warnings only.

Broad-validation trigger: shared portal movement continuation changed.

Broad .NET decision: skipped full project/solution validation after focused tests passed. The branch is isolated to registered group continuation, and the focused command covered non-reentry, reentry, and fresh-allocation-still-blocked paths.

## Next Sequential UOW

Recommended next runtime scope: Beshmundir's Walk non-leader follow-entry when a group member is already inside world `300170000`.

Java artifacts to inspect:

- `game-server/data/handlers/ai/instance/beshmundirTemple/BeshmundirsWalkAI.java`
- `game-server/src/com/aionemu/gameserver/services/teleport/PortalService.java`
- `game-server/src/com/aionemu/gameserver/model/templates/portal/PortalPath.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PortalEntryValidationService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerTeleportService.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/PortalPathTable.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInstanceCooldownTests.cs`

Specific behavior to prove: Java `BeshmundirsWalkAI.INSTANCE_ENTRY` grouped non-leader branch calls `moveToInstance(player, (byte) 0)` when any group member is in world `300170000`; `moveToInstance` resolves `DataManager.PORTAL2_DATA.getPortalUsePath(getNpcId(), player)` and calls `PortalService.port(...)`.

Focused C# command should target the new Beshmundir boundary test only, plus the registered group transfer test if the continuation path is reused directly.

Focused Java/Maven command: not expected unless a targeted Java fixture is added; Java source review is likely the practical source-of-truth evidence.

Broad-validation trigger: possible if the next UOW changes shared portal validation or live generic portal dialog routing. Name the trigger before any unfiltered project/solution validation.

## Safe Candidates

- Beshmundir non-leader follow-entry using existing registered group transfer support.
- Beshmundir difficulty acceptance only after the same portal-use-path resolution is available.
- Generic `PortalEntryInteractionService` team-plan continuation bridge if that is smaller than a Beshmundir-specific portal-use path.

Avoid:

- Full .NET project tests or solution builds without a documented trigger.
- Fresh group allocation until `registerTeam(group)` behavior is scoped and tested.
- Beshmundir-only teleport shortcuts that bypass Java `PortalService.port(...)`.
- Evidence/reporting-only units.
- Updating `docs/PHASE-6-PROGRESS.md`.
