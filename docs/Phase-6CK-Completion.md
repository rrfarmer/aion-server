# Phase 6CK Completion Handoff

**Created**: May 23, 2026  
**Status**: Phase 6 remains in progress; this handoff follows 6CJ and covers Sessions 513-514.  
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, world/known-list behavior, persistence behavior, scheduling, stat formulas, observer side effects, and combat math.  
**Workflow rule**: Do one focused unit of work, validate it, update the migration parity table, commit it, then repeat for as long as useful work remains.  
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.  
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx` passes with 1065 tests.

---

## Recent Work Completed

- Extended `InstanceCooltimeSummary` and the static-data loader to preserve Java `InstanceCooltime.max_member_light` and `max_member_dark` values from `instance_cooltimes.xml`.
- Added `InstanceCooltimeTable.GetMaxMemberCount(worldId, race)`, matching Java `InstanceCooltimeData.getMaxMemberCount`: missing template returns `0`, `ELYOS` selects light capacity, every other value selects dark capacity.
- Added `Player`-aware `InstanceRuntimeService.GetNextAvailableInstanceForPlayer` and `GetOrRegisterInstance` overloads that derive max players from instance cooltime data and register the player's object id.
- Added focused coverage for race-specific max-member lookup, player-aware instance allocation/reuse, and bundled XML loading of max-member values.
- Added `PlayerPortalCooldownService` as a narrow C# companion for Java `PortalCooldownList`, covering expired-entry cleanup, cooldown-time lookup, disabled-entry checks against instance `maxcount`, entry-count increments, and explicit removal.
- Added focused coverage for portal cooldown disabled/expiry behavior and repeated add/remove behavior.
- Updated `docs/PHASE-6-PROGRESS.md` through Session 514 with migration parity tables, risks, metrics, validation, and next recommended work.

---

## Commits In This Handoff

- `265f6752a` - `Use instance cooltime max members`
- `b98cf2509` - `Add portal cooldown state service`

---

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|
| `InstanceCooltimeData.getMaxMemberCount` | `InstanceCooltimeTable.GetMaxMemberCount` | Complete | Unit Tested | Partial Parity | Source-shaped behavior is covered, but no Java runtime execution was performed. C# compares string race values case-insensitively rather than Java enum identity. |
| `InstanceCooltime` | `InstanceCooltimeSummary` / `StaticData.InstanceCooltimeBuilder` | Partial | Unit Tested | Partial Parity | `maxcount`, `max_member_light`, and `max_member_dark` are loaded. Other Java template fields remain outside this summary unless already consumed elsewhere. |
| `InstanceService.getNextAvailableInstance(int, Player)` | `InstanceRuntimeService.GetNextAvailableInstanceForPlayer(..., Player, InstanceCooltimeTable)` | Partial | Unit Tested | Partial Parity | C# derives `maxPlayers` from cooltime data and registers `player.ObjectId`. Difficulty ids, handlers, spawns, callbacks, auto-destroy, cooldown lockouts, and entry validation remain missing. |
| `InstanceService.getOrRegisterInstance(int, Player)` | `InstanceRuntimeService.GetOrRegisterInstance(..., Player, InstanceCooltimeTable)` | Partial | Unit Tested | Partial Parity | Same-player reuse and new-player allocation are covered. Java scans full `WorldMapInstance` registrations, including richer team/instance state still absent in C#. |
| `Player.getObjectId` / `Player.getRace` | `Player.ObjectId` / `Player.Race` | Partial | Unit Tested | Needs Verification | This unit consumes existing C# properties. Race remains a string, not Java `Race` enum, so broader model normalization remains open. |
| `Race` | string race boundary in `Player.Race` / `InstanceCooltimeTable` | Partial | Unit Tested | Needs Verification | Tested values cover ELYOS, ASMODIANS, and unknown non-ELYOS selecting dark capacity. A full Race enum port is still open. |
| `WorldMapInstance.register(int)` and max-player constructor input | `WorldMapInstanceRuntimeState.Register` / `MaxPlayers` | Partial | Unit Tested | Partial Parity | Service tests validate registration and max-player storage. Runtime instance still omits regions, zones, object dictionaries, handlers, team registrations, door state, and empty-instance tasks. |
| `PortalCooldownList.isPortalUseDisabled` | `PlayerPortalCooldownService.IsPortalUseDisabled` | Partial | Unit Tested | Partial Parity | Missing cooldowns return false, expired cooldowns are removed, and active entry counts are compared to `InstanceCooltimeSummary.MaxCount`. Missing-template behavior is defensive in C#. |
| `PortalCooldownList.getPortalCooldownTime` | `PlayerPortalCooldownService.GetPortalCooldownTime` | Complete | Unit Tested | Partial Parity | Source-shaped cooldown-time lookup and expiry removal are covered. Uses injected `DateTimeOffset` instead of Java `System.currentTimeMillis()` for deterministic tests. |
| `PortalCooldownList.addPortalCooldown` | `PlayerPortalCooldownService.AddPortalCooldown` | Partial | Unit Tested | Partial Parity | New cooldowns are created with entry count `1`; repeated adds increment count and preserve the existing reuse time. DAO persistence and `SM_INSTANCE_INFO` fanout are not wired. |
| `PortalCooldownList.removePortalCooldown` | `PlayerPortalCooldownService.RemovePortalCooldown` | Complete | Unit Tested | Partial Parity | Removes from the copied C# dictionary. Java mutable `HashMap` threading behavior is not verified. |

Metrics from this handoff window:

- Total focused sessions covered: 2
- Total commits covered: 2
- Current full validation baseline: 1065 tests passing
- Total artifacts with verified parity newly promoted in this window: 0
- Total blocked artifacts: full Java `InstanceService`, immediate portal cooldown DAO persistence, `SM_INSTANCE_INFO` player/team fanout, production portal/autogroup/teleport caller wiring, full race enum/model parity, `WorldMapInstanceFactory` handler/engine integration, full `WorldMapInstance` lifecycle, reset-time calculation/rate handling, and Java runtime/live-client validation
- Estimated overall migration completion: Phase 6 remains about 56% complete; these close two small instance-entry building blocks but the broader instance and world lifecycle remain open.

---

## Important Limits

- This is still not a full Java `InstanceService`.
- No instance cooldown lockouts, entry guards, reset-time handling, team/member validation, portal/teleport caller integration, difficulty ids, handler supplier selection, event spawns, `SpawnEngine`, `InstanceHandler`, auto-destroy scheduling, Panesterra checks, or logging were ported.
- `PlayerPortalCooldownService.AddPortalCooldown` mutates in-memory player state only; it does not persist immediately or send `SM_INSTANCE_INFO` like Java `PortalCooldownList.addPortalCooldown`.
- Race handling is still string-based in C#, while Java uses `Race`; invalid or alternate race values are only tested for the new max-member branch.
- Threading differs: this unit layers onto existing C# locks/snapshots and does not validate Java concurrent map/set behavior under live load.
- No serialization, database persistence, date/time handling, precision/rounding, reflection behavior, or packet wire format changed in this window.

---

## Next Unit Of Work

Recommended next unit: add source-shaped `InstanceCooltimeData.calculateInstanceEntranceCooltime` support for relative/daily/weekly reset calculations before wiring production callers.

Suggested shape:

1. Re-read Java:
   - `InstanceCooltimeData.calculateInstanceEntranceCooltime`
   - `InstanceService.getInstanceRate`
   - `InstanceCooltimeData`
   - `PortalService` and autogroup call sites that add portal cooldowns
2. Re-read C#:
   - `InstanceRuntimeService`
   - `InstanceCooltimeTable`
   - `PlayerPortalCooldownService`
   - `Player.PortalCooldowns`
   - any portal/teleport caller boundaries already modeled in Phase 6
3. Keep it narrow:
   - load any additional `InstanceCooltime` fields only if required for reset calculation
   - make time/rate inputs injectable for deterministic tests
   - document membership/config-rate gaps if those configs are not already modeled
   - do not attempt full handler/spawn/auto-destroy lifecycle in the same unit
   - update `docs/PHASE-6-PROGRESS.md` with a Migration Parity Table before committing

Alternative units:

- Wire one production-shaped caller boundary to `PlayerPortalCooldownService` where C# already has enough portal/autogroup context, including `SM_INSTANCE_INFO` fanout if packet ordering can be tested.
- Add Java-generated or source-derived golden-vector coverage for delayed teleport packet fields: `SM_TELEPORT_LOC`, `SM_DELETE`, `SM_PLAYER_INFO`, and `SM_SYSTEM_MESSAGE` id `1400640`.

Useful validation commands:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRuntimeStateTests|FullyQualifiedName~StaticDataLoadingTests"
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerStateTests|FullyQualifiedName~WorldMapRuntimeStateTests"
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GamePacketTests|FullyQualifiedName~GameServerConnectionFlightZoneFanoutTests|FullyQualifiedName~WorldMapRuntimeStateTests"
dotnet test dotnetConversion\AionServer.slnx
```

---

## Resume Checklist

- Start from branch `4.8`.
- Confirm `git status --short` is clean.
- Read `docs/csharp-port.md`, latest `docs/PHASE-6-PROGRESS.md` Sessions 513-514, `docs/Phase-6CJ-Completion.md`, and this handoff.
- Continue with one focused Java-parity unit.
- Update the migration parity table before committing.
- Commit the unit, then repeat until the next handoff is needed.
