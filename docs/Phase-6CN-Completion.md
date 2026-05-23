# Phase 6CN Completion Handoff

**Created**: May 23, 2026  
**Status**: Phase 6 remains in progress; this handoff follows 6CM and covers Sessions 518-520.  
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, world/known-list behavior, persistence behavior, scheduling, stat formulas, observer side effects, and combat math.  
**Workflow rule**: Do one focused unit of work, validate it, update the migration parity table, commit it, then repeat for as long as useful work remains.  
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.  
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx` passes with 1077 tests.

---

## Recent Work Completed

- Added the Java single-instance update shape to `SmInstanceInfo`, including the cooldown-id header and one-instance filtered payload.
- Added `InstanceEntranceCooldownService.CreateEntryInfoPacket` so future portal/autogroup callers can convert a successful cooldown mutation into Java `PortalCooldownList.sendEntryInfo(worldId)`'s mode `2` packet.
- Added `GameServerConnection.ApplyInstanceEntranceCooldownAsync` as the current owner-connection dispatch boundary: it applies the cooldown, optionally persists, and sends the owner `SmInstanceInfo` packet when Java's add guard succeeds.
- Added `IPlayerEnterWorldRepository.SavePlayerPortalCooldownsAsync` / `MySqlPlayerEnterWorldRepository.SavePlayerPortalCooldownsAsync` for Java `PortalCooldownsDAO.storePortalCooldowns` delete-then-insert behavior.
- Logout persistence now also writes portal cooldowns alongside skill, item, and house-object cooldowns.
- Added connected-socket tests for owner packet dispatch and repository handoff, plus packet-byte coverage for the single-world update payload.
- Updated `docs/PHASE-6-PROGRESS.md` through Session 520 with migration parity tables, risks, metrics, validation, and next recommended work.

---

## Commits In This Handoff

- `ddf56351e` - `Add instance cooldown entry info packet`
- `5f3255a77` - `Send instance cooldown entry updates`
- `1200e2976` - `Persist instance entrance cooldowns`

---

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|
| `PortalCooldownList.addPortalCooldown` | `InstanceEntranceCooldownService.ApplyEntranceCooldown` + `GameServerConnection.ApplyInstanceEntranceCooldownAsync` | Partial | Unit Tested | Partial Parity | C# can mutate cooldowns, persist through the enter-world repository when available, and send the owner update packet. Full portal/autogroup callers are not wired yet. |
| `PortalCooldownList.sendEntryInfo` | `InstanceEntranceCooldownService.CreateEntryInfoPacket` + owner `SendPacketAsync` | Partial | Unit Tested | Partial Parity | Owner-only packet branch is represented and byte-checked. Java team fanout remains missing. |
| `SM_INSTANCE_INFO(byte, Player, Integer...)` | `SmInstanceInfo(byte, Player, InstanceCooltimeTable, int, Func<DateTimeOffset>?)` | Partial | Unit Tested / Regression Tested | Partial Parity | Single-world updates write the cooldown id header and one instance entry. Multi-player collection constructor is not ported. |
| `PortalCooldownsDAO.storePortalCooldowns` | `IPlayerEnterWorldRepository.SavePlayerPortalCooldownsAsync` / `MySqlPlayerEnterWorldRepository.SavePlayerPortalCooldownsAsync` | Partial | Unit Tested | Partial Parity | Delete-then-insert-active behavior is implemented against existing schema, but no MySQL integration test has run. C# uses one connection for delete+inserts; Java opens separate connections. |
| `PlayerService.storePlayer` | `MySqlPlayerEnterWorldRepository.SavePlayerLogoutAsync` | Partial | No Tests | Needs Verification | Logout now includes portal cooldown save, but fake-repository logout tests do not execute SQL. |
| `PortalService.transfer` | Future caller; current C# helper only | Partial | Unit Tested | Needs Verification | Cooldown/send/persist side effect exists, but start position, instance registration, teleport, portal guards, item/kinah removal, and packet ordering remain outside this handoff. |
| `AutoInstance.onPressEnter` | Future caller; current C# helper only | Partial | No Tests | Needs Verification | The helper can serve future autogroup entry, but C# autogroup runtime and handler callback are not wired. |
| `PlayerTeam.sendPackets` / `owner.getCurrentTeam().sendPackets` | No C# current-team packet router | Not Started | No Tests | Unknown | Team fanout remains blocked by missing team model/routing support. |

Metrics from this handoff window:

- Total focused sessions covered: 3
- Total commits covered: 3
- Current full validation baseline: 1077 tests passing
- Total artifacts with verified parity newly promoted in this window: 0
- Total blocked artifacts: production portal/autogroup caller wiring, MySQL integration validation for portal cooldown save, team packet fanout/current-team model, teleport packet ordering, Java multi-player `SM_INSTANCE_INFO` constructor, and live-client validation
- Estimated overall migration completion: Phase 6 remains about 56% complete; this closes several cooldown update building blocks but not end-to-end instance entry parity.

---

## Important Limits

- `GameServerConnection.ApplyInstanceEntranceCooldownAsync` is not called by a real portal, teleport, or autogroup packet path yet.
- Packet ordering relative to `SM_TELEPORT_LOC`, world despawn/spawn, and `CM_TELEPORT_ANIMATION_DONE` is not validated.
- Team fanout is still missing; only owner packet dispatch is covered.
- MySQL portal-cooldown save SQL is source-derived and not integration-tested.
- C# uses injected/UTC milliseconds for deterministic tests; Java uses `System.currentTimeMillis()`.
- Full portal validation, item/kinah consumption, instance creation/registration, start-position state, and live client behavior remain open.

---

## Next Unit Of Work

Recommended next unit: wire the cooldown/send/persist helper into the narrowest actual portal or teleport entry surface currently present in C#.

Suggested shape:

1. Re-read Java:
   - `PortalService.port`
   - `PortalService.transfer`
   - `PortalCooldownList.addPortalCooldown`
   - `SM_TELEPORT_LOC` / delayed teleport packet order around `TeleportService.teleportTo`
2. Re-read C#:
   - `GameServerConnection.QueueDelayedTeleportAsync`
   - `GameServerConnection.ApplyInstanceEntranceCooldownAsync`
   - `PlayerTeleportService`
   - current portal/rift/autogroup-adjacent surfaces
3. Keep it narrow:
   - prefer one explicit caller boundary that already has a `Player`, `worldId`, `reenter`, `InstanceCooltimeTable`, and owner connection
   - do not port the full portal validation matrix in the same unit
   - keep team fanout explicit if current-team routing is still absent
   - if no safe production caller exists, add a MySQL-backed regression for `SavePlayerPortalCooldownsAsync` instead

Alternative units:

- Add a DB integration test for `SavePlayerPortalCooldownsAsync` using the existing MySQL fixture path.
- Port Java `SM_INSTANCE_INFO(byte, Collection<Player>, Integer...)` multi-player constructor as a prerequisite for team update fanout.
- Start a minimal current-team packet fanout abstraction if existing group/alliance work has enough shape to support it safely.

Useful validation commands:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInstanceCooldownTests|FullyQualifiedName~InstanceEntranceCooldownServiceTests|FullyQualifiedName~GamePacketTests|FullyQualifiedName~PlayerStateTests"
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~GameServerConnectionFlightZoneFanoutTests"
dotnet test dotnetConversion\AionServer.slnx
```

---

## Resume Checklist

- Start from branch `4.8`.
- Confirm `git status --short` is clean.
- Read `docs/csharp-port.md`, latest `docs/PHASE-6-PROGRESS.md` Sessions 518-520, `docs/Phase-6CM-Completion.md`, and this handoff.
- Continue with one focused Java-parity unit.
- Update the migration parity table before committing.
- Commit the unit, then repeat until the next handoff is needed.
