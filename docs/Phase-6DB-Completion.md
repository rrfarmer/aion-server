# Phase 6DB Completion Handoff

**Created**: May 23, 2026  
**Status**: Phase 6 remains in progress; this handoff follows 6DA and covers Session 554.  
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, runtime side effects, static-data semantics, persistence behavior, scheduling, and combat math.  
**Workflow rule**: Do one focused unit of work, validate it, update the migration parity table, commit it, then repeat for as long as useful work remains.  
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.  
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx` passes with 1172 tests.

---

## Recent Work Completed

- Added a non-executing `GroupPortalTransferPlan` DTO at the portal continuation-transfer boundary.
- `PortalContinueTransferResult` now carries `GroupTransferPlan` for unsupported group team plans.
- `GroupPortalTransferPlan` captures:
  - group team id
  - member object ids
  - max players
  - registered instance when present
  - blocked state: `RegisteredInstanceTransfer`, `FreshInstanceAllocationNeeded`, or `InvalidTeamId`
  - blocked reason: `GroupFanoutNotImplemented` or `MissingTeamId`
- Extended transfer-boundary tests for registered group plans, allocation-needed group plans, and missing-team-id group plans.
- Confirmed all three blocked group outcomes send no packets, create no pending teleport, and persist no cooldowns.
- Updated `docs/PHASE-6-PROGRESS.md` Session 554 with required migration parity table, tests, risks, metrics, and next recommended work.

---

## Commits In This Handoff

- `29248d5f8` - `Add blocked group portal transfer plan`

---

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|
| `PortalService.port` group branch after required item checks | `GroupPortalTransferPlan` / `PortalContinueTransferResult.GroupTransferPlan` | Partial | Unit Tested | Needs Verification | C# now exposes blocked group planning metadata but does not execute Java's group branch. |
| `PlayerGroup.getTeamId` | `GroupPortalTransferPlan.TeamId` | Partial | Unit Tested | Needs Verification | Numeric id is preserved; no full group aggregate/lifecycle exists. |
| `PlayerGroup.getMembers` | `GroupPortalTransferPlan.MemberObjectIds` | Partial | Unit Tested | Needs Verification | Object ids are metadata only; Java uses live `Player` members. |
| `InstanceService.getRegisteredInstance(int, int)` | `GroupPortalTransferPlan.RegisteredInstance` | Partial | Unit Tested | Needs Verification | Registered team instance metadata is carried forward; no runtime Java comparison. |
| `InstanceService.getNextAvailableInstance(int, byte, int)` | `GroupPortalTransferState.FreshInstanceAllocationNeeded` | Not Started | Unit Tested | Needs Verification | C# records allocation need but does not allocate. |
| `WorldMapInstance.registerTeam` | Future group allocation/fanout path, currently blocked by `GroupFanoutNotImplemented` | Partial | Unit Tested | Needs Verification | Prior helper stores numeric team id only; this unit does not call it. |
| `PortalService.transfer` | Unsupported group transfer result with null teleport/cooldown | Partial | Unit Tested | Needs Verification | Tests prove blocked paths have no teleport/cooldown side effects. Positive Java transfer remains missing. |

Metrics from this handoff window:

- Total focused sessions covered: 1
- Total commits covered: 1 planned commit for Session 554
- Current full validation baseline: 1172 tests passing
- Total artifacts with verified parity newly promoted in this window: 0
- Total blocked artifacts: group member solo-instance scan, group instance allocation, full `registerTeam`, group member transfer fanout, group capacity check, alliance transfer planning, league model/transfer, team cooldown timing, team lifecycle/concurrency, and live client/runtime comparison
- Estimated overall migration completion: Phase 6 remains about 63% complete; group transfer now has explicit blocked planning metadata, but successful team portal entry remains unimplemented.

---

## Important Limits

- Group/alliance/league portal transfer fanout is still not implemented.
- `GroupPortalTransferPlan` is non-executing metadata only.
- The Java loose-requirement branch that scans each group member for solo registered instances is not represented yet.
- Fresh allocation is only represented as a state; C# does not call `InstanceService.getNextAvailableInstance`, choose an instance handler, set max players, or call `RegisterTeamId`.
- There is no C# `PlayerGroup`, alliance, alliance-group, or league aggregate with live members.
- Team id lifecycle, member mutation, online/offline member filtering, capacity checks, cooldown timing, and logout/disband cleanup remain missing.
- No encrypted socket integration test or live client capture validates team portal behavior.
- Java threading/locking, reflection/JAXB behavior, serialization beyond unit-tested objects, date/time behavior, precision/rounding, and live runtime behavior remain unverified.

---

## Next Unit Of Work

Recommended next unit: model Java's loose group requirement member scan as another blocked planning detail, without executing transfer.

Suggested scope:

1. Re-read Java:
   - `PortalService.port` group branch under `if (instance == null && group != null && !instanceGroupReq)`
   - `for (Player member : group.getMembers())`
   - `InstanceService.getRegisteredInstance(mapId, member.getObjectId())`
   - fallback to `getNextAvailableInstance(mapId, difficult, maxPlayers)` and `instance.registerTeam(group)`
2. Re-read C#:
   - `GroupPortalTransferPlan`
   - `PortalTeamEntryPlan.MemberObjectIds`
   - `WorldMapRuntimeStateTable.GetRegisteredInstance`
   - `GameServerConnection.QueuePortalContinueTransferAsync`
3. Add a conservative non-executing scan surface:
   - record that Java would scan group member object ids only when loose group requirement is allowed
   - keep the result blocked if only ids are available and no live group aggregate exists
   - avoid teleport, cooldown, allocation, or registration side effects
4. Tests:
   - grouped plan records member scan candidates for the loose-requirement path
   - registered team instance still wins before scan
   - no registered team/member scan result remains allocation-needed and blocked
   - no packets, pending teleports, cooldown saves, or instance allocations occur
5. Keep parity language conservative:
   - Do not claim verified parity without runtime/client comparison.
   - Document missing live `PlayerGroup`, online filtering, member mutation, capacity checks, full allocation, `registerTeam`, cooldown timing, and concurrency behavior.

Useful validation commands:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PortalEntryValidationServiceTests|FullyQualifiedName~PortalEntryInteractionServiceTests|FullyQualifiedName~GameServerConnectionInstanceCooldownTests|FullyQualifiedName~WorldMapRuntimeStateTests|FullyQualifiedName~PlayerEnterWorldServiceTests"
dotnet test dotnetConversion\AionServer.slnx
```

---

## Resume Checklist

- Start from branch `4.8`.
- Confirm `git status --short` is clean.
- Read `docs/csharp-port.md`, latest `docs/PHASE-6-PROGRESS.md` Session 554, `docs/Phase-6DA-Completion.md`, and this handoff.
- Continue with one focused Java-parity unit.
- Update the migration parity table before committing.
- Commit the unit, then repeat until the next handoff is needed.
