# Phase 6CY Completion Handoff

**Created**: May 23, 2026  
**Status**: Phase 6 remains in progress; this handoff follows 6CX and covers Sessions 548-549.  
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, runtime side effects, static-data semantics, persistence behavior, scheduling, and combat math.  
**Workflow rule**: Do one focused unit of work, validate it, update the migration parity table, commit it, then repeat for as long as useful work remains.  
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.  
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx` passes with 1165 tests.

---

## Recent Work Completed

- Added Java-shaped team-size portal requirement failures for the current unsupported group/alliance/league entry slice.
- `PortalEntryValidationService.ValidatePlayerSize` now mirrors Java `PortalService.checkPlayerSize` for:
  - group max players 3/6: `SM_DIALOG_WINDOW(err_group)` when present, otherwise `STR_MSG_ENTER_ONLY_PARTY_DON`
  - alliance max players 7-24: `STR_MSG_ENTER_ONLY_FORCE_DON`
  - league max players greater than 24: `STR_MSG_ENTER_ONLY_UNION_DON`
- Moved team-size rejection into Java guard order after mentor/race/rank/title/quest validation instead of the old early unsupported exit.
- The dialog portal caller now sends returned team requirement failure packets and stops before unsupported fanout.
- Added minimal blocked team-plan metadata:
  - `Player.CurrentTeamId`
  - `Player.CurrentTeamMemberObjectIds`
  - `PortalTeamEntryPlan`
  - `PortalTeamEntryKind`
- Grouped players who pass the no-group guard now reach `UnsupportedTeamPortal` with team id/member metadata preserved for the next planning slice.
- Updated `docs/PHASE-6-PROGRESS.md` through Sessions 548-549 with required migration parity tables, risks, metrics, and next recommended work.

---

## Commits In This Handoff

- `d8c530d82` - `Surface team portal requirement failures`
- `b2953e2f6` - `Preserve blocked group portal plan metadata`

---

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|
| `PortalService.checkPlayerSize` | `PortalEntryValidationService.ValidatePlayerSize` | Partial | Unit Tested | Partial Parity | C# now returns Java-shaped no-group/no-alliance/no-league packets. Successful team entry remains blocked. |
| `PortalService.port` guard order | `PortalEntryValidationService.ValidatePortalEntryPlan` | Partial | Unit Tested | Partial Parity | Team-size guard now runs after mentor/race/rank/title/quest for non-admin callers. Admin/membership bypass entry is still unsupported downstream. |
| `SM_SYSTEM_MESSAGE.STR_MSG_ENTER_ONLY_PARTY_DON` | `SmSystemMessage.EnterOnlyPartyDon` | Complete | Unit Tested | Needs Verification | Java message id `1390256` asserted; no live-client capture. |
| `SM_SYSTEM_MESSAGE.STR_MSG_ENTER_ONLY_FORCE_DON` | `SmSystemMessage.EnterOnlyForceDon` | Complete | Unit Tested | Needs Verification | Java message id `1400544` asserted; no live-client capture. |
| `SM_SYSTEM_MESSAGE.STR_MSG_ENTER_ONLY_UNION_DON` | `SmSystemMessage.EnterOnlyUnionDon` | Complete | Unit Tested | Needs Verification | Java message id `1401251` asserted; no live-client capture. |
| `SM_DIALOG_WINDOW` for `PortalPath.getErrGroup` | `SmDialogWindow` returned by `ValidatePlayerSize` | Partial | Unit Tested | Partial Parity | Type is asserted for err-group failure; exact payload was not newly binary-compared. |
| `PlayerGroup.getTeamId` | `Player.CurrentTeamId` | Partial | Unit Tested | Needs Verification | Carries team id metadata only; no full `PlayerGroup` aggregate or lifecycle. |
| `PlayerGroup.getMembers` | `Player.CurrentTeamMemberObjectIds` | Partial | Unit Tested | Needs Verification | Carries object ids only; no live member objects, online-state filtering, fanout, or concurrency handling. |
| `PortalService.port` group branch | `PortalTeamEntryPlan` / `PortalEntryPlanResult.TeamPlan` | Partial | Unit Tested | Needs Verification | Preserves blocked group context but does not lookup registered team instance, allocate, registerTeam, or transfer. |

Metrics from this handoff window:

- Total focused sessions covered: 2
- Total commits covered: 2
- Current full validation baseline: 1165 tests passing
- Total artifacts with verified parity newly promoted in this window: 0
- Total blocked artifacts: group portal fanout, alliance portal fanout, league model/fanout, team id registration, member transfer iteration, registered team-instance lookup, admin/membership team bypass entry, and live client/runtime comparison
- Estimated overall migration completion: Phase 6 remains about 63% complete; unsupported team portal failure packets and blocked group-plan metadata are represented, but successful team portal entry is still not implemented.

---

## Important Limits

- Group/alliance/league portal transfer fanout is still not implemented.
- `Player.CurrentTeamId` and `CurrentTeamMemberObjectIds` are metadata carriers, not full Java team models.
- There is still no C# league membership model.
- `PortalTeamEntryPlan` is not consumed by `GameServerConnection.QueuePortalContinueTransferAsync`.
- Registered group/alliance instance lookup by team id is not wired into portal planning yet.
- `WorldMapInstance.registerTeam`, member iteration, online/offline member filtering, capacity checks, difficulty handling, and Java team concurrency/lifecycle remain missing.
- No encrypted socket integration test or live client capture proves the new team requirement packet behavior.
- Java threading/locking, reflection/JAXB behavior, serialization beyond asserted packet ids/types, date/time behavior, precision/rounding, and live runtime behavior remain unverified.

---

## Next Unit Of Work

Recommended next unit: continue the group portal planning slice without executing fanout.

Suggested scope:

1. Re-read Java:
   - `PortalService.port` group branch for max players 3/6
   - `InstanceService.getRegisteredInstance(mapId, group.getTeamId())`
   - `WorldMapInstance.registerTeam`
   - `PlayerGroup.getTeamId` and `getMembers`
2. Re-read C#:
   - `PortalEntryValidationService.ValidatePortalEntryPlan`
   - `PortalTeamEntryPlan`
   - `WorldMapRuntimeStateTable.GetRegisteredInstance`
   - `WorldMapInstanceRuntimeState.Register`
3. Add a non-executing planning step:
   - for `TeamPlan.Kind == Group`, probe `WorldMapRuntimeStateTable.GetRegisteredInstance(worldId, TeamId)`
   - record whether Java would attempt registered reentry or fresh group allocation
   - preserve blocked state before any cooldown mutation, instance allocation, registration, or transfer fanout
4. Tests:
   - grouped player with a registered team instance records blocked registered-group reentry metadata
   - grouped player with no registered team instance records blocked fresh-group allocation-needed metadata
   - no-team group portal still returns Java party-only failure packet
   - solo/open-world portal plans remain unchanged
5. Keep parity language conservative:
   - Do not claim verified parity without runtime/client comparison.
   - Document missing `registerTeam`, member iteration, team lifecycle, and concurrency behavior.

Useful validation commands:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PortalEntryValidationServiceTests|FullyQualifiedName~PortalEntryInteractionServiceTests|FullyQualifiedName~GameServerConnectionInstanceCooldownTests"
dotnet test dotnetConversion\AionServer.slnx
```

---

## Resume Checklist

- Start from branch `4.8`.
- Confirm `git status --short` is clean.
- Read `docs/csharp-port.md`, latest `docs/PHASE-6-PROGRESS.md` Sessions 548-549, `docs/Phase-6CX-Completion.md`, and this handoff.
- Continue with one focused Java-parity unit.
- Update the migration parity table before committing.
- Commit the unit, then repeat until the next handoff is needed.
