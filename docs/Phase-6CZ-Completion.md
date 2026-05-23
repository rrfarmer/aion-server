# Phase 6CZ Completion Handoff

**Created**: May 23, 2026  
**Status**: Phase 6 remains in progress; this handoff follows 6CY and covers Sessions 550-551.  
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, runtime side effects, static-data semantics, persistence behavior, scheduling, and combat math.  
**Workflow rule**: Do one focused unit of work, validate it, update the migration parity table, commit it, then repeat for as long as useful work remains.  
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.  
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx` passes with 1168 tests.

---

## Recent Work Completed

- Extended blocked team portal planning so group/alliance team plans can probe registered instances by Java team id before C# stops at unsupported fanout.
- `PortalTeamEntryPlan` now records:
  - `Disposition`
  - `RegisteredInstance`
  - `Reenter`
- Added `PortalTeamEntryDisposition` with:
  - `FreshInstanceAllocationNeeded`
  - `RegisteredInstanceTransfer`
- Preserved Java's nuance that a team-id-only registration is a registered team transfer, but the early `reenter` flag only becomes true when the player object id is also registered.
- Added `WorldMapInstanceRuntimeState.RegisterTeamId`, mirroring the stored-team-id and registered-object-id side effects of Java `WorldMapInstance.registerTeam`.
- Updated group portal planning tests to use `RegisterTeamId` rather than generic `Register(teamId)`.
- Updated `docs/PHASE-6-PROGRESS.md` through Sessions 550-551 with required migration parity tables, risks, metrics, and next recommended work.

---

## Commits In This Handoff

- `77946581f` - `Probe blocked group portal registrations`
- `c0a9559ef` - `Add team registration runtime helper`

---

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|
| `PortalService.port` group registered-instance probe | `PortalEntryValidationService.CreateUnsupportedTeamPlan` | Partial | Unit Tested | Needs Verification | C# probes by team id and records registered-transfer versus fresh-allocation-needed metadata, but still blocks before fanout. |
| `InstanceService.getRegisteredInstance(int, int)` | `WorldMapRuntimeStateTable.GetRegisteredInstance` used by team plans | Partial | Unit Tested | Needs Verification | Existing runtime registry is reused for team ids. No Java runtime comparison. |
| `WorldMapInstance.registerTeam` | `WorldMapInstanceRuntimeState.RegisterTeamId` | Partial | Unit Tested | Partial Parity | Stores one team id and registers it for lookup. Does not store a `GeneralTeam` object or lifecycle hooks. |
| `WorldMapInstance.register(int)` / `isRegistered(int)` | `WorldMapInstanceRuntimeState.Register` / `IsRegistered` / `RegisterTeamId` | Partial | Unit Tested | Partial Parity | Team ids and player ids share registered id set. C# lock behavior not runtime-compared to Java. |
| `GeneralTeam.getTeamId` | `Player.CurrentTeamId` and `RegisterTeamId(int)` | Partial | Unit Tested | Needs Verification | Numeric id only; no full group/alliance/league aggregate. |
| `PlayerGroup.getMembers` | `Player.CurrentTeamMemberObjectIds` carried in `PortalTeamEntryPlan` | Partial | Unit Tested | Needs Verification | Metadata only; no member iteration or transfer fanout. |

Metrics from this handoff window:

- Total focused sessions covered: 2
- Total commits covered: 2
- Current full validation baseline: 1168 tests passing
- Total artifacts with verified parity newly promoted in this window: 0
- Total blocked artifacts: full `GeneralTeam` storage, group model, alliance model, league model, team lifecycle cleanup, group allocation, member transfer fanout, team cooldown handling, and live client/runtime comparison
- Estimated overall migration completion: Phase 6 remains about 63% complete; blocked group portal planning now sees registered team instances and has Java-shaped team-id registration storage, but successful team portal entry remains unimplemented.

---

## Important Limits

- Group/alliance/league portal transfer fanout is still not implemented.
- `PortalTeamEntryPlan` is metadata only and is not consumed by `GameServerConnection.QueuePortalContinueTransferAsync`.
- `WorldMapInstanceRuntimeState.RegisterTeamId` models only the stored id and registered-id set, not Java's `GeneralTeam` reference.
- There is no C# group, alliance, alliance-group, or league aggregate with live members.
- Team id lifecycle, member mutation, online/offline member filtering, capacity checks, group reuse policy, and logout/disband cleanup remain missing.
- No encrypted socket integration test or live client capture validates team portal behavior.
- Java threading/locking, reflection/JAXB behavior, serialization beyond unit-tested objects, date/time behavior, precision/rounding, and live runtime behavior remain unverified.

---

## Next Unit Of Work

Recommended next unit: make blocked team portal plans consumable by the future transfer caller without changing live behavior.

Suggested scope:

1. Re-read Java:
   - `PortalService.port` group branch after `checkAndRemoveRequiredItems`
   - `PortalService.transfer`
   - `WorldMapInstance.registerTeam`
2. Re-read C#:
   - `PortalEntryInteractionService.HandleDialogSelectAsync`
   - `GameServerConnection.QueuePortalContinueTransferAsync`
   - `PortalTeamEntryPlan`
   - `PortalContinueTransferResult`
3. Add a blocked result surface:
   - introduce a transfer/planning result kind such as `UnsupportedTeamPortal`
   - ensure the dialog caller can distinguish "ready but blocked team plan" from generic validation rejection when this gets wired
   - keep production behavior non-executing for team portals
4. Tests:
   - blocked group plan with registered team instance surfaces `RegisteredInstanceTransfer`
   - blocked group plan with no registered instance surfaces `FreshInstanceAllocationNeeded`
   - future transfer caller/refusal surface returns explicit unsupported-team result without sending teleport/cooldown packets
5. Keep parity language conservative:
   - Do not claim verified parity without runtime/client comparison.
   - Document missing group allocation, `registerTeam` full object storage, member iteration, team lifecycle, and concurrency behavior.

Useful validation commands:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PortalEntryValidationServiceTests|FullyQualifiedName~PortalEntryInteractionServiceTests|FullyQualifiedName~GameServerConnectionInstanceCooldownTests|FullyQualifiedName~WorldMapRuntimeStateTests"
dotnet test dotnetConversion\AionServer.slnx
```

---

## Resume Checklist

- Start from branch `4.8`.
- Confirm `git status --short` is clean.
- Read `docs/csharp-port.md`, latest `docs/PHASE-6-PROGRESS.md` Sessions 550-551, `docs/Phase-6CY-Completion.md`, and this handoff.
- Continue with one focused Java-parity unit.
- Update the migration parity table before committing.
- Commit the unit, then repeat until the next handoff is needed.
