# Phase 6DD Completion Handoff

**Created**: May 23, 2026  
**Status**: Phase 6 remains in progress; this handoff follows 6DC and covers Session 556.  
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, runtime side effects, static-data semantics, persistence behavior, scheduling, and combat math.  
**Workflow rule**: Do one focused unit of work, validate it, update the migration parity table, commit it, then repeat for as long as useful work remains.  
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.  
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx` passes with 1172 tests.

---

## Recent Work Completed

- Added a blocked `GroupPortalCapacityPlan` under `GroupPortalTransferPlan`.
- The capacity plan documents Java's group branch guard: `instance.getPlayersInside().size() < maxPlayers`.
- Registered group-instance plans now carry runtime player count and whether the capacity guard would pass or fail before transfer.
- Fresh allocation-needed group plans mark capacity as unknown until allocation.
- Invalid/missing-team-id plans block capacity planning with an explicit missing-team-id reason.
- Extended focused transfer tests for registered-instance current player count, allocation-needed unknown capacity, and invalid-team capacity blocking.
- Updated `docs/PHASE-6-PROGRESS.md` Session 556 with required migration parity table, tests, risks, metrics, and next recommended work.

---

## Commits In This Handoff

- `401898574` - `Record blocked group portal capacity plan`

---

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|
| `PortalService.port` group capacity guard | `GroupPortalCapacityPlan` | Partial | Unit Tested | Needs Verification | C# records guard metadata but does not execute or reject transfer. |
| `WorldMapInstance.getPlayersInside` | `WorldMapInstanceRuntimeState.PlayerCount` | Partial | Unit Tested | Needs Verification | C# counts runtime player ids, not live Java `Player` objects. |
| `WorldMapInstance.getPlayerCount` / `isFull` related state | `GroupPortalCapacityState` | Partial | Unit Tested | Needs Verification | Count is compared to max players for registered instances; no production gate yet. |
| `InstanceService.getNextAvailableInstance(int, byte, int)` | `UnknownUntilInstanceAllocated` capacity state | Not Started | Unit Tested | Needs Verification | Fresh allocation remains unported. |
| `WorldMapInstance.registerTeam` | Future allocation path before capacity guard | Partial | No Tests In This Unit | Needs Verification | Numeric helper exists, but no full Java `GeneralTeam` storage/caller. |
| `PortalService.transfer` | Unsupported group result with capacity plan and null teleport/cooldown | Partial | Unit Tested | Needs Verification | Blocked plans still produce no packet, pending teleport, or cooldown write. |

Metrics from this handoff window:

- Total focused sessions covered: 1
- Total commits covered: 1 planned commit for Session 556
- Current full validation baseline: 1172 tests passing
- Total artifacts with verified parity newly promoted in this window: 0
- Total blocked artifacts: production capacity gating, full-instance rejection behavior, group allocation, full `registerTeam`, live player list parity, group member transfer fanout, alliance transfer planning, league model/transfer, team cooldown timing, team lifecycle/concurrency, and live client/runtime comparison
- Estimated overall migration completion: Phase 6 remains about 63% complete; the group capacity guard is now documented in blocked planning metadata, but successful group portal entry remains unimplemented.

---

## Important Limits

- Capacity planning is advisory metadata only and does not affect production transfer.
- Full-instance rejection and user-facing behavior are not implemented.
- Group allocation and `RegisterTeamId` are not invoked.
- There is still no C# `PlayerGroup`, alliance, alliance-group, or league aggregate with live members.
- Registered instance player counts are not runtime-compared with Java live `getPlayersInside`.
- No encrypted socket integration test or live client capture validates team portal behavior.
- Java threading/locking, reflection/JAXB behavior, serialization beyond unit-tested objects, date/time behavior, precision/rounding, and live runtime behavior remain unverified.

---

## Next Unit Of Work

Recommended next unit: add a non-executing `GroupPortalAllocationPlan`.

Suggested scope:

1. Re-read Java:
   - `PortalService.port` group branches that call `InstanceService.getNextAvailableInstance(mapId, difficult, maxPlayers)`
   - `WorldMapInstance.registerTeam(group)`
   - `PortalService.transfer`
2. Re-read C#:
   - `WorldMapRuntimeStateTable.AddWorldMapInstance`
   - `WorldMapInstanceRuntimeState.RegisterTeamId`
   - `GroupPortalTransferPlan`
   - `GameServerConnection.QueuePortalContinueTransferAsync`
3. Add blocked allocation preview metadata:
   - target world id if available from portal loc
   - difficulty id placeholder or unsupported marker
   - max players
   - intended team id registration
   - blocked reason explaining why allocation is still disabled
4. Tests:
   - allocation-needed group plan carries intended team registration metadata
   - registered group plan does not require allocation
   - no world map instance is created
   - no packets, pending teleports, cooldown saves, or team registrations occur
5. Keep parity language conservative:
   - Do not claim verified parity without runtime/client comparison.
   - Document missing live group aggregate, difficulty propagation, instance handler creation, `registerTeam`, capacity gate enforcement, member fanout, cooldown timing, and concurrency behavior.

Useful validation commands:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PortalEntryValidationServiceTests|FullyQualifiedName~PortalEntryInteractionServiceTests|FullyQualifiedName~GameServerConnectionInstanceCooldownTests|FullyQualifiedName~WorldMapRuntimeStateTests|FullyQualifiedName~PlayerEnterWorldServiceTests"
dotnet test dotnetConversion\AionServer.slnx
```

---

## Resume Checklist

- Start from branch `4.8`.
- Confirm `git status --short` is clean.
- Read `docs/csharp-port.md`, latest `docs/PHASE-6-PROGRESS.md` Session 556, `docs/Phase-6DC-Completion.md`, and this handoff.
- Continue with one focused Java-parity unit.
- Update the migration parity table before committing.
- Commit the unit, then repeat until the next handoff is needed.
