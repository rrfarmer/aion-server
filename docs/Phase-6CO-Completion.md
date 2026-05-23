# Phase 6CO Completion Handoff

**Created**: May 23, 2026  
**Status**: Phase 6 remains in progress; this handoff follows 6CN and covers Sessions 521-523.  
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, world/known-list behavior, persistence behavior, scheduling, stat formulas, observer side effects, and combat math.  
**Workflow rule**: Do one focused unit of work, validate it, update the migration parity table, commit it, then repeat for as long as useful work remains.  
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.  
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx` passes with 1080 tests.

---

## Recent Work Completed

- Added `GameServerConnection.QueueInstancePortalTransferAsync`, preserving Java `PortalService.transfer` side-effect ordering: queue delayed teleport first, then apply/persist/send the entrance cooldown update.
- Added `WorldMapInstanceRuntimeState.StartPosition` plus set-once `SetStartPositionIfMissing`, matching the guarded `PortalService.transfer` start-position initialization.
- Added `InstanceRuntimeService.CreatePortalTransferInstance` to allocate an instance, register the requester, initialize start position, and return a destination with the allocated instance id.
- Added `GameServerConnection.QueueAllocatedInstancePortalTransferAsync`, composing instance allocation/start-position planning with the teleport-then-cooldown connection boundary.
- Added structured result DTOs for the transfer boundaries so future caller wiring can inspect runtime and packet/cooldown side effects without recalculating them.
- Added connected tests for packet order (`SmTeleportLoc` before `SmInstanceInfo`), runtime allocation/registration, destination instance-id rewrite, and portal cooldown persistence handoff.
- Updated `docs/PHASE-6-PROGRESS.md` through Session 523 with migration parity tables, risks, metrics, validation, and next recommended work.

---

## Commits In This Handoff

- `7ef45d159` - `Compose instance portal transfer updates`
- `8d8b10bb6` - `Track instance portal start positions`
- `a3da9ba9d` - `Compose allocated instance portal transfer`

---

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|
| `PortalService.port` | `GameServerConnection.QueueAllocatedInstancePortalTransferAsync` + `InstanceRuntimeService.CreatePortalTransferInstance` | Partial | Unit Tested | Partial Parity | Allocation, requester registration, start-position initialization, destination instance id, teleport packet, cooldown persistence, and owner cooldown packet are composed. Full portal guards and packet-handler wiring are missing. |
| `PortalService.transfer` | `GameServerConnection.QueueInstancePortalTransferAsync` / `QueueAllocatedInstancePortalTransferAsync` | Partial | Unit Tested | Partial Parity | Source ordering is modeled: teleport request before cooldown update. World despawn/spawn, handler callbacks, live timing, and full validation remain open. |
| `InstanceService.getNextAvailableInstance` | `InstanceRuntimeService.GetNextAvailableInstance` via `CreatePortalTransferInstance` | Partial | Unit Tested | Partial Parity | Existing runtime allocation is reused. Difficulty id, handler supplier, spawn engine, instance callbacks, auto-destroy, and Panesterra restrictions remain missing. |
| `WorldMapInstance.register` / `setStartPos` | `WorldMapInstanceRuntimeState.Register` / `SetStartPositionIfMissing` | Partial | Unit Tested | Partial Parity | Requester registration and guarded start-position initialization are covered. Team registration and arbitrary mutable setter parity are not modeled. |
| `PortalCooldownList.addPortalCooldown` | `GameServerConnection.ApplyInstanceEntranceCooldownAsync` via transfer helpers | Partial | Unit Tested | Partial Parity | Cooldown mutation, optional persistence, and owner packet update are composed after teleport. Team fanout and production portal invocation are still missing. |
| `SM_TELEPORT_LOC` | `SmTeleportLoc` | Partial | Regression Tested | Partial Parity | Packet ordering is asserted; representative byte payloads are covered elsewhere. Live client timing remains unverified. |
| `SM_INSTANCE_INFO` | `SmInstanceInfo` | Partial | Unit Tested / Regression Tested | Partial Parity | Single-player update packet is composed in order. Multi-player/team constructor remains missing. |
| `WorldMapType.isPersonal` | Caller-supplied `ownerId` | Not Started | No Tests | Unknown | C# still does not infer personal-map ownership from Java `WorldMapType`; callers must pass owner id explicitly. |
| `PlayerTeam.sendPackets` / `owner.getCurrentTeam().sendPackets` | No current-team packet router | Not Started | No Tests | Unknown | Team fanout remains blocked. |

Metrics from this handoff window:

- Total focused sessions covered: 3
- Total commits covered: 3
- Current full validation baseline: 1080 tests passing
- Total artifacts with verified parity newly promoted in this window: 0
- Total blocked artifacts: production portal packet-handler wiring, full portal validation, Java `WorldMapType` personal-map metadata, difficulty/handler/spawn integration, team fanout/current-team model, MySQL integration validation for portal cooldown save, and live-client validation
- Estimated overall migration completion: Phase 6 remains about 56% complete; the instance portal transfer building blocks are now composed, but the live portal flow is not complete.

---

## Important Limits

- No production packet handler calls `QueueAllocatedInstancePortalTransferAsync` yet.
- Portal validation is still mostly missing: cooldown lockout, level/rank, group/alliance/league size, title, quest, item, and kinah checks.
- Instance difficulty, handler supplier, spawn engine, `InstanceHandler.onInstanceCreate`, auto-destroy scheduling, and event spawns are still unported.
- Team fanout for `SM_INSTANCE_INFO` remains absent.
- MySQL execution of portal-cooldown persistence is still source-derived and not integration-tested.
- Packet order is tested at the connection observer level, not with a live encrypted client.

---

## Next Unit Of Work

Recommended next unit: start a minimal source-shaped portal validation service, one Java guard at a time, before wiring the composed transfer helper into a real dialog path.

Suggested shape:

1. Re-read Java:
   - `PortalService.check` and the call order before `port`
   - `PortalCooldownList.isPortalUseDisabled`
   - `PortalService.checkPlayerSize`
   - `PortalService.checkAndRemoveRequiredItems`
2. Re-read C#:
   - `PlayerPortalCooldownService.IsPortalUseDisabled`
   - `Player.TeamMembership`
   - inventory/kinah models and mutation services, if considering item/kinah checks
   - `GameServerConnection.QueueAllocatedInstancePortalTransferAsync`
3. Keep it narrow:
   - begin with cooldown lockout because the supporting service already exists
   - return a structured validation result and optional failure packet rather than sending immediately
   - do not consume items/kinah until inventory persistence can be made Java-shaped
   - keep group/alliance/team routing gaps explicit if only `Player.TeamMembership` is available

Alternative units:

- Add MySQL integration coverage for `SavePlayerPortalCooldownsAsync`.
- Port Java `SM_INSTANCE_INFO(byte, Collection<Player>, Integer...)` multi-player constructor for future team fanout.
- Add `WorldMapType`/personal-map metadata to static world-map summaries if the XML shape contains enough information.

Useful validation commands:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInstanceCooldownTests|FullyQualifiedName~WorldMapRuntimeStateTests|FullyQualifiedName~PlayerStateTests"
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GamePacketTests|FullyQualifiedName~PlayerEnterWorldServiceTests"
dotnet test dotnetConversion\AionServer.slnx
```

---

## Resume Checklist

- Start from branch `4.8`.
- Confirm `git status --short` is clean.
- Read `docs/csharp-port.md`, latest `docs/PHASE-6-PROGRESS.md` Sessions 521-523, `docs/Phase-6CN-Completion.md`, and this handoff.
- Continue with one focused Java-parity unit.
- Update the migration parity table before committing.
- Commit the unit, then repeat until the next handoff is needed.
