# Phase 6DG Completion Handoff

**Created**: May 23, 2026  
**Status**: Phase 6 remains in progress; this handoff follows 6DF and covers Session 559.  
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, runtime side effects, static-data semantics, persistence behavior, scheduling, and combat math.  
**Workflow rule**: Do one focused unit of work, validate it, update the migration parity table, commit it, then repeat for as long as useful work remains.  
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.  
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx` passes with 1173 tests.

---

## Recent Work Completed

- Added non-mutating blocked cooldown-preview metadata for group portal execution.
- `InstanceEntranceCooldownService.PreviewEntranceCooldown` now reuses the same Java-shaped cooldown calculation and membership rate lookup as the mutating path, but does not add cooldowns.
- `PortalContinueTransferResult.UnsupportedTeamPortal` now passes the player, `InstanceCooltimeTable`, options, and effective time into group planning.
- `GroupPortalExecutionPlan` records cooldown reuse time, instance cooldown rate, and whether Java would add cooldown for registered group transfers.
- Registered non-reentry group plans record positive cooldown preview when table data yields a reuse time.
- Registered reentry group plans record cooldown skipped for reentry.
- Allocation-needed and invalid-team plans keep cooldown preview unknown.
- Extended focused transfer tests for cooldown would-add, reentry skip, no cooldown save, no packet send, and no blocked-instance mutation.
- Updated `docs/PHASE-6-PROGRESS.md` Session 559 with required migration parity table, tests, risks, metrics, and next recommended work.

---

## Commits In This Handoff

- `4acfa1692` - `Preview blocked group portal cooldowns`

---

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|
| `PortalService.transfer` cooldown branch | `GroupPortalExecutionPlan` cooldown preview fields | Partial | Unit Tested | Needs Verification | C# records would-add/skipped/unknown cooldown state but does not mutate cooldowns. |
| `InstanceCooltimeData.calculateInstanceEntranceCooltime` | `InstanceEntranceCooldownService.PreviewEntranceCooldown` / `InstanceCooltimeTable.CalculateInstanceEntranceCooltime` | Partial | Unit Tested | Needs Verification | Preview reuses C# cooldown calculation and rate lookup. Runtime Java comparison and server-time edge cases remain unverified. |
| `InstanceService.getInstanceRate` | `InstanceCooldownRateService.GetInstanceRate` | Partial | Unit Tested | Needs Verification | Rate is recorded by preview. Broader membership/config behavior remains source-derived. |
| `PortalCooldownList.addPortalCooldown` | Preview result `Added` as would-add only | Not Started | Unit Tested | Needs Verification | No `Player.PortalCooldowns` mutation, repository save, or packet send occurs. |
| `SM_INSTANCE_INFO` after cooldown add | Blocked group path packet absence | Not Started | Unit Tested | Needs Verification | Tests prove no packet is sent. Positive packet path is not exercised here. |
| `ServerTime.now` / `System.currentTimeMillis` | `DateTimeOffset now` passed into preview | Partial | Unit Tested | Needs Verification | Deterministic relative cooldown tested; daily/weekly and server-clock behavior remain unverified. |
| `Player.hasPermission` for cooldown rate | `Player.AccountMembership` / `GameServerOptions.Membership.InstancesCooldown` | Partial | Existing Unit Tested | Needs Verification | Existing service is reused; this unit only checks default-rate group preview behavior. |

Metrics from this handoff window:

- Total focused sessions covered: 1
- Total commits covered: 1 planned commit for Session 559
- Current full validation baseline: 1173 tests passing
- Total artifacts with verified parity newly promoted in this window: 0
- Total blocked artifacts: actual cooldown add/persistence, `SM_INSTANCE_INFO` group/team fanout, daily/weekly runtime comparison, actual group transfer execution, start-position mutation, player registration, teleport packet queueing, allocation, full `registerTeam`, group member fanout, alliance/league execution, and live client/runtime comparison
- Estimated overall migration completion: Phase 6 remains about 63% complete; group portal planning now documents cooldown intent, but successful group portal entry remains unimplemented.

---

## Important Limits

- Cooldown preview is advisory metadata only.
- No cooldown is added to `Player.PortalCooldowns`, no cooldowns are saved, and no `SM_INSTANCE_INFO` packet is sent.
- Daily/weekly reset behavior was not newly tested through group planning.
- There is still no C# `PlayerGroup`, alliance, alliance-group, or league aggregate with live members.
- Actual group transfer execution, start-position mutation, player registration, teleport queueing, allocation, `registerTeam`, capacity enforcement, member fanout, and team lifecycle cleanup remain missing.
- No encrypted socket integration test or live client capture validates team portal behavior.
- Java threading/locking, reflection/JAXB behavior, serialization beyond unit-tested objects, date/time behavior, precision/rounding, and live runtime behavior remain unverified.

---

## Next Unit Of Work

Recommended next unit: add the first minimal live group aggregate model/resolver needed by the blocked portal plan.

Suggested scope:

1. Re-read Java:
   - `Player.getPlayerGroup`
   - `PlayerGroup.getTeamId`
   - `PlayerGroup.getMembers`
   - the group branches in `PortalService.port`
2. Re-read C#:
   - `Player.TeamMembership`
   - `Player.CurrentTeamId`
   - `Player.CurrentTeamMemberObjectIds`
   - `PortalEntryValidationService.CreateUnsupportedTeamPlan`
   - `GroupPortalTransferPlan`
3. Add a minimal C# team snapshot/resolver:
   - captures team id
   - captures live member object ids
   - can be attached to or supplied for portal planning
   - remains explicitly incomplete versus Java `PlayerGroup`
4. Tests:
   - portal planning prefers resolver/snapshot metadata when present
   - fallback to current player metadata remains unchanged
   - unsupported group transfer remains blocked
   - no teleport/cooldown/allocation side effects occur
5. Keep parity language conservative:
   - Do not claim verified parity without runtime/client comparison.
   - Document missing Java lifecycle, leader/member mutation, online/offline filtering, locking/concurrency, group disband, alliance/league integration, and packet fanout.

Useful validation commands:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PortalEntryValidationServiceTests|FullyQualifiedName~PortalEntryInteractionServiceTests|FullyQualifiedName~GameServerConnectionInstanceCooldownTests|FullyQualifiedName~WorldMapRuntimeStateTests|FullyQualifiedName~PlayerEnterWorldServiceTests"
dotnet test dotnetConversion\AionServer.slnx
```

---

## Resume Checklist

- Start from branch `4.8`.
- Confirm `git status --short` is clean.
- Read `docs/csharp-port.md`, latest `docs/PHASE-6-PROGRESS.md` Session 559, `docs/Phase-6DF-Completion.md`, and this handoff.
- Continue with one focused Java-parity unit.
- Update the migration parity table before committing.
- Commit the unit, then repeat until the next handoff is needed.
