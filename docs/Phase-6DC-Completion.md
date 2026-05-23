# Phase 6DC Completion Handoff

**Created**: May 23, 2026  
**Status**: Phase 6 remains in progress; this handoff follows 6DB and covers Session 555.  
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, runtime side effects, static-data semantics, persistence behavior, scheduling, and combat math.  
**Workflow rule**: Do one focused unit of work, validate it, update the migration parity table, commit it, then repeat for as long as useful work remains.  
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.  
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx` passes with 1172 tests.

---

## Recent Work Completed

- Added a blocked `GroupPortalMemberInstanceScanPlan` under `GroupPortalTransferPlan`.
- The scan plan documents Java's loose group requirement branch where `PortalService.port` can iterate `group.getMembers()` and call `InstanceService.getRegisteredInstance(mapId, member.getObjectId())`.
- Registered group-instance plans now mark member scan as not needed.
- Fresh allocation-needed group plans now preserve member object ids as would-scan candidates and mark the scan blocked because C# lacks a live `PlayerGroup` aggregate.
- Missing-team-id plans now block member scanning before exposing candidates.
- Extended focused transfer tests for scan skipping, scan candidates, and missing-team-id scan blocking.
- Updated `docs/PHASE-6-PROGRESS.md` Session 555 with required migration parity table, tests, risks, metrics, and next recommended work.

---

## Commits In This Handoff

- `51b0813d4` - `Record blocked group portal member scan`

---

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|
| `PortalService.port` loose group member scan branch | `GroupPortalMemberInstanceScanPlan` | Partial | Unit Tested | Needs Verification | C# records scan intent/candidates but does not resolve live members or perform registered-instance lookups. |
| `PlayerGroup.getMembers` | `GroupPortalMemberInstanceScanPlan.CandidateObjectIds` | Partial | Unit Tested | Needs Verification | Object ids are metadata only; Java iterates live `Player` objects. |
| `InstanceService.getRegisteredInstance(int, member.getObjectId())` | `GroupPortalMemberInstanceScanState.WouldScanMemberObjectIds` | Not Started | Unit Tested | Needs Verification | C# does not query member registered instances yet. |
| `InstanceService.getRegisteredInstance(int, group.getTeamId())` | `GroupPortalMemberInstanceScanState.NotNeededRegisteredTeamInstance` | Partial | Unit Tested | Needs Verification | Registered team instance avoids scan in the blocked plan. |
| `InstanceService.getNextAvailableInstance(int, byte, int)` fallback | `GroupPortalTransferState.FreshInstanceAllocationNeeded` | Not Started | Unit Tested | Needs Verification | Allocation fallback remains metadata only. |
| `WorldMapInstance.registerTeam` fallback after allocation | Future group allocation/fanout path | Partial | Unit Tested | Needs Verification | Numeric helper exists, but no live team object storage or caller. |
| `PortalService.transfer` | Unsupported group result with member-scan plan and null teleport/cooldown | Partial | Unit Tested | Needs Verification | Blocked plans still produce no packets, pending teleport, or cooldown writes. |

Metrics from this handoff window:

- Total focused sessions covered: 1
- Total commits covered: 1 planned commit for Session 555
- Current full validation baseline: 1172 tests passing
- Total artifacts with verified parity newly promoted in this window: 0
- Total blocked artifacts: instanceGroupReq permission modeling, live group aggregate, member registered-instance lookup, group instance allocation, full `registerTeam`, group capacity check, group member transfer fanout, alliance transfer planning, league model/transfer, team lifecycle/concurrency, and live client/runtime comparison
- Estimated overall migration completion: Phase 6 remains about 63% complete; the member-scan branch is now documented in blocked planning metadata, but successful group portal entry is still missing.

---

## Important Limits

- `instanceGroupReq` / permission-driven loose group requirement behavior is not modeled yet.
- `GroupPortalMemberInstanceScanPlan` is non-executing metadata only.
- There is still no C# `PlayerGroup`, alliance, alliance-group, or league aggregate with live members.
- No member registered-instance lookup is performed.
- Fresh allocation, `RegisterTeamId`, capacity checks, cooldown timing, and transfer fanout remain blocked.
- No encrypted socket integration test or live client capture validates team portal behavior.
- Java threading/locking, reflection/JAXB behavior, serialization beyond unit-tested objects, date/time behavior, precision/rounding, and live runtime behavior remain unverified.

---

## Next Unit Of Work

Recommended next unit: add a non-executing group capacity guard surface for Java's `instance.getPlayersInside().size() < maxPlayers` check.

Suggested scope:

1. Re-read Java:
   - `PortalService.port` group branch final capacity guard
   - `WorldMapInstance.getPlayersInside`
   - `PortalService.transfer`
2. Re-read C#:
   - `WorldMapInstanceRuntimeState`
   - `GroupPortalTransferPlan`
   - `GameServerConnection.QueuePortalContinueTransferAsync`
3. Add a conservative capacity plan:
   - records max players
   - records current player count only if a reliable runtime source exists
   - otherwise marks count as unknown and capacity verification blocked
   - keeps transfer blocked before `PortalService.transfer`
4. Tests:
   - registered group plan carries capacity guard metadata
   - allocation-needed group plan carries capacity guard metadata
   - no packets, pending teleports, cooldown saves, or allocations occur
5. Keep parity language conservative:
   - Do not claim verified parity without runtime/client comparison.
   - Document missing live player list, full capacity check, allocation, `registerTeam`, member fanout, cooldown timing, and concurrency behavior.

Useful validation commands:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PortalEntryValidationServiceTests|FullyQualifiedName~PortalEntryInteractionServiceTests|FullyQualifiedName~GameServerConnectionInstanceCooldownTests|FullyQualifiedName~WorldMapRuntimeStateTests|FullyQualifiedName~PlayerEnterWorldServiceTests"
dotnet test dotnetConversion\AionServer.slnx
```

---

## Resume Checklist

- Start from branch `4.8`.
- Confirm `git status --short` is clean.
- Read `docs/csharp-port.md`, latest `docs/PHASE-6-PROGRESS.md` Session 555, `docs/Phase-6DB-Completion.md`, and this handoff.
- Continue with one focused Java-parity unit.
- Update the migration parity table before committing.
- Commit the unit, then repeat until the next handoff is needed.
