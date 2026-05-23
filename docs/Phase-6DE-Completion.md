# Phase 6DE Completion Handoff

**Created**: May 23, 2026  
**Status**: Phase 6 remains in progress; this handoff follows 6DD and covers Session 557.  
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, runtime side effects, static-data semantics, persistence behavior, scheduling, and combat math.  
**Workflow rule**: Do one focused unit of work, validate it, update the migration parity table, commit it, then repeat for as long as useful work remains.  
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.  
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx` passes with 1172 tests.

---

## Recent Work Completed

- Added a blocked `GroupPortalAllocationPlan` under `GroupPortalTransferPlan`.
- `PortalContinueTransferResult.UnsupportedTeamPortal` now receives the portal location so blocked group plans can record target world id.
- Allocation-needed group plans now record target world id, max players, intended registered team id, null/unknown difficulty id, and `InstanceAllocationNotPorted`.
- Registered group-instance plans mark allocation as not needed.
- Invalid/missing-team-id plans block allocation before intended team registration is exposed.
- Extended focused transfer tests to verify allocation preview metadata and no next instance creation.
- Updated `docs/PHASE-6-PROGRESS.md` Session 557 with required migration parity table, tests, risks, metrics, and next recommended work.

---

## Commits In This Handoff

- `56c913921` - `Record blocked group portal allocation plan`

---

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|
| `PortalService.port` group allocation branches | `GroupPortalAllocationPlan` | Partial | Unit Tested | Needs Verification | C# records allocation intent but does not allocate or transfer. |
| `InstanceService.getNextAvailableInstance(int, byte, int)` | `GroupPortalAllocationState.WouldAllocateAndRegisterTeam` | Not Started | Unit Tested | Needs Verification | No runtime allocation, handler creation, or spawn execution occurs. Difficulty id is null because the current blocked path does not carry Java's `difficult` argument. |
| `WorldMapInstance.registerTeam` | `GroupPortalAllocationPlan.IntendedRegisteredTeamId` | Partial | Unit Tested | Needs Verification | Intended team id is recorded, but `RegisterTeamId` is not invoked and no `GeneralTeam` is stored. |
| `WorldMap.addInstance` / `WorldMapInstanceFactory.createWorldMapInstance` | `WorldMapRuntimeStateTable.CreateNextWorldMapInstance` intentionally not called | Not Started | Unit Tested | Needs Verification | Tests prove no next modeled instance is created for blocked previews. |
| `InstanceEngine.getNewInstanceHandler` | Allocation blocked reason | Not Started | No Tests | Unknown | Handler creation remains a newly documented dependency. |
| `SpawnEngine.spawnInstance` | Allocation blocked reason | Not Started | No Tests | Unknown | Spawn execution remains unported for group allocation. |
| `PortalService.transfer` | Unsupported group result with allocation plan and null teleport/cooldown | Partial | Unit Tested | Needs Verification | Blocked plans still produce no packet, pending teleport, cooldown write, or instance allocation. |

Metrics from this handoff window:

- Total focused sessions covered: 1
- Total commits covered: 1 planned commit for Session 557
- Current full validation baseline: 1172 tests passing
- Total artifacts with verified parity newly promoted in this window: 0
- Total blocked artifacts: Java difficulty propagation, actual group instance allocation, instance handler creation, spawn execution, auto-destroy scheduling, full `registerTeam`, live group aggregate, capacity gate enforcement, member transfer fanout, alliance/league allocation, team lifecycle/concurrency, and live client/runtime comparison
- Estimated overall migration completion: Phase 6 remains about 63% complete; group allocation intent is now documented in blocked metadata, but successful group portal entry remains unimplemented.

---

## Important Limits

- `GroupPortalAllocationPlan` is advisory metadata only.
- No C# world instance is created by the blocked group path.
- Java's `difficult` byte is not carried in the blocked C# group path yet.
- Instance handlers, spawn population, auto-destroy tasks, and full `registerTeam` behavior are not ported here.
- There is still no C# `PlayerGroup`, alliance, alliance-group, or league aggregate with live members.
- No encrypted socket integration test or live client capture validates team portal behavior.
- Java threading/locking, reflection/JAXB behavior, serialization beyond unit-tested objects, date/time behavior, precision/rounding, and live runtime behavior remain unverified.

---

## Next Unit Of Work

Recommended next unit: add a blocked group transfer execution preview.

Suggested scope:

1. Re-read Java:
   - `PortalService.transfer`
   - `WorldMapInstance.setStartPos`
   - `WorldMapInstance.register(player.getObjectId())`
   - `TeleportService.teleportTo`
   - `PortalCooldownList.addPortalCooldown`
2. Re-read C#:
   - `GroupPortalTransferPlan`
   - `GroupPortalCapacityPlan`
   - `PlayerTeleportService.QueuePendingTeleport`
   - `GameServerConnection.QueueInstancePortalTransferAsync`
   - `InstanceEntranceCooldownService.ApplyEntranceCooldown`
3. Add blocked execution-preview metadata:
   - target instance id when a registered instance is present
   - start-position intent from portal loc
   - player registration intent
   - reenter flag
   - teleport animation intent
   - cooldown-add eligibility or unknown state
4. Tests:
   - registered group plan carries target instance/start-position/registration intent
   - allocation-needed plan reports execution blocked until allocation
   - invalid team id reports execution blocked before transfer
   - no start position set, no player registration, no packets, no pending teleport, no cooldown save
5. Keep parity language conservative:
   - Do not claim verified parity without runtime/client comparison.
   - Document missing live group aggregate, actual allocation, start-position mutation, player registration, teleport queueing, cooldown timing, member fanout, and concurrency behavior.

Useful validation commands:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PortalEntryValidationServiceTests|FullyQualifiedName~PortalEntryInteractionServiceTests|FullyQualifiedName~GameServerConnectionInstanceCooldownTests|FullyQualifiedName~WorldMapRuntimeStateTests|FullyQualifiedName~PlayerEnterWorldServiceTests"
dotnet test dotnetConversion\AionServer.slnx
```

---

## Resume Checklist

- Start from branch `4.8`.
- Confirm `git status --short` is clean.
- Read `docs/csharp-port.md`, latest `docs/PHASE-6-PROGRESS.md` Session 557, `docs/Phase-6DD-Completion.md`, and this handoff.
- Continue with one focused Java-parity unit.
- Update the migration parity table before committing.
- Commit the unit, then repeat until the next handoff is needed.
