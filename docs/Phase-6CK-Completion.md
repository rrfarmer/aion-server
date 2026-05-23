# Phase 6CK Completion Handoff

**Created**: May 23, 2026  
**Status**: Phase 6 remains in progress; this handoff follows 6CJ and covers Session 513.  
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, world/known-list behavior, persistence behavior, scheduling, stat formulas, observer side effects, and combat math.  
**Workflow rule**: Do one focused unit of work, validate it, update the migration parity table, commit it, then repeat for as long as useful work remains.  
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.  
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx` passes with 1063 tests.

---

## Recent Work Completed

- Extended `InstanceCooltimeSummary` and the static-data loader to preserve Java `InstanceCooltime.max_member_light` and `max_member_dark` values from `instance_cooltimes.xml`.
- Added `InstanceCooltimeTable.GetMaxMemberCount(worldId, race)`, matching Java `InstanceCooltimeData.getMaxMemberCount`: missing template returns `0`, `ELYOS` selects light capacity, every other value selects dark capacity.
- Added `Player`-aware `InstanceRuntimeService.GetNextAvailableInstanceForPlayer` and `GetOrRegisterInstance` overloads that derive max players from instance cooltime data and register the player's object id.
- Added focused coverage for race-specific max-member lookup, player-aware instance allocation/reuse, and bundled XML loading of max-member values.
- Updated `docs/PHASE-6-PROGRESS.md` through Session 513 with migration parity table, risks, metrics, validation, and next recommended work.

---

## Commits In This Handoff

- `265f6752a` - `Use instance cooltime max members`

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

Metrics from this handoff window:

- Total focused sessions covered: 1
- Total commits covered: 1
- Current full validation baseline: 1063 tests passing
- Total artifacts with verified parity newly promoted in this window: 0
- Total blocked artifacts: full Java `InstanceService`, cooldown/entry validation, full race enum/model parity, production portal/teleport caller wiring, `WorldMapInstanceFactory` handler/engine integration, full `WorldMapInstance` lifecycle, and Java runtime/live-client validation
- Estimated overall migration completion: Phase 6 remains about 56% complete; this closes the player-aware allocation/member-count gap but the broader instance and world lifecycle remain open.

---

## Important Limits

- This is still not a full Java `InstanceService`.
- No instance cooldown lockouts, entry guards, reset-time handling, team/member validation, portal/teleport caller integration, difficulty ids, handler supplier selection, event spawns, `SpawnEngine`, `InstanceHandler`, auto-destroy scheduling, Panesterra checks, or logging were ported.
- Race handling is still string-based in C#, while Java uses `Race`; invalid or alternate race values are only tested for the new max-member branch.
- Threading differs: this unit layers onto existing C# locks/snapshots and does not validate Java concurrent map/set behavior under live load.
- No serialization, database persistence, date/time handling, precision/rounding, reflection behavior, or packet wire format changed in this window.

---

## Next Unit Of Work

Recommended next unit: add a source-shaped instance cooldown/entry-validation service boundary around the new cooltime member data.

Suggested shape:

1. Re-read Java:
   - `InstanceService` entry/allocation methods around `getNextAvailableInstance`
   - `InstanceCooltimeData`
   - portal/instance entry guards that consume max counts or cooldown templates
2. Re-read C#:
   - `InstanceRuntimeService`
   - `InstanceCooltimeTable`
   - `Player.PortalCooldowns`
   - any portal/teleport caller boundaries already modeled in Phase 6
3. Keep it narrow:
   - add a service predicate/result object for source-shaped instance entry facts if it has a clean Java source anchor
   - document missing cooldown reset scheduling and production caller wiring
   - do not attempt full handler/spawn/auto-destroy lifecycle in the same unit
   - update `docs/PHASE-6-PROGRESS.md` with a Migration Parity Table before committing

Alternative unit:

- Add Java-generated or source-derived golden-vector coverage for delayed teleport packet fields: `SM_TELEPORT_LOC`, `SM_DELETE`, `SM_PLAYER_INFO`, and `SM_SYSTEM_MESSAGE` id `1400640`.

Useful validation commands:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRuntimeStateTests|FullyQualifiedName~StaticDataLoadingTests"
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GamePacketTests|FullyQualifiedName~GameServerConnectionFlightZoneFanoutTests|FullyQualifiedName~WorldMapRuntimeStateTests"
dotnet test dotnetConversion\AionServer.slnx
```

---

## Resume Checklist

- Start from branch `4.8`.
- Confirm `git status --short` is clean.
- Read `docs/csharp-port.md`, latest `docs/PHASE-6-PROGRESS.md` Sessions 512-513, `docs/Phase-6CJ-Completion.md`, and this handoff.
- Continue with one focused Java-parity unit.
- Update the migration parity table before committing.
- Commit the unit, then repeat until the next handoff is needed.
