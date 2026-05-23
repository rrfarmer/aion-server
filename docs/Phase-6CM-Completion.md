# Phase 6CM Completion Handoff

**Created**: May 23, 2026  
**Status**: Phase 6 remains in progress; this handoff follows 6CL and covers Session 517.  
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, world/known-list behavior, persistence behavior, scheduling, stat formulas, observer side effects, and combat math.  
**Workflow rule**: Do one focused unit of work, validate it, update the migration parity table, commit it, then repeat for as long as useful work remains.  
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.  
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx` passes with 1073 tests.

---

## Recent Work Completed

- Added `InstanceEntranceCooldownService.ApplyEntranceCooldown` as a reusable composition boundary for the Java post-entry cooldown path.
- The helper resolves `InstanceService.getInstanceRate` parity through `InstanceCooldownRateService`, calculates reuse time through `InstanceCooltimeTable.CalculateInstanceEntranceCooltime`, skips mutation for reentry, and adds a portal cooldown only when reuse time is positive.
- Added `InstanceEntranceCooldownResult` so future portal/autogroup callers can make packet/persistence decisions without recalculating the cooldown.
- Added deterministic tests for positive cooldown add, reentry skip, and zero-delay no-op behavior.
- Updated `docs/PHASE-6-PROGRESS.md` through Session 517 with migration parity table, risks, metrics, validation, and next recommended work.

---

## Commits In This Handoff

- `5e9a26a98` - `Compose instance entrance cooldown updates`

---

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|
| `PortalService.transfer` | `InstanceEntranceCooldownService.ApplyEntranceCooldown` | Partial | Unit Tested | Partial Parity | C# models only the cooldown side-effect after entry: calculate delay, skip add on reenter, and add positive delay. Teleport, instance start position, item/kinah consumption, portal validation, and instance creation/registration are outside this slice. |
| `AutoInstance.onPressEnter` | `InstanceEntranceCooldownService.ApplyEntranceCooldown` | Partial | Unit Tested | Partial Parity | C# can model the non-reentry autogroup cooldown mutation, but is not wired into a C# autogroup runtime. |
| `InstanceCooltimeData.calculateInstanceEntranceCooltime` | `InstanceCooltimeTable.CalculateInstanceEntranceCooltime` via `InstanceEntranceCooldownService` | Partial | Unit Tested | Partial Parity | Composed with rate and mutation. Time remains caller-injected; Java runtime and server timezone are not validated. |
| `InstanceService.getInstanceRate` | `InstanceCooldownRateService.GetInstanceRate` via `InstanceEntranceCooldownService` | Complete | Unit Tested | Partial Parity | The composed boundary uses the C# rate helper. Production callers still need to pass real options. |
| `PortalCooldownList.addPortalCooldown` | `PlayerPortalCooldownService.AddPortalCooldown` via `InstanceEntranceCooldownService` | Partial | Unit Tested | Partial Parity | In-memory cooldown mutation is covered. Missing Java side effects: immediate DAO persistence and `SM_INSTANCE_INFO` player/team fanout. |
| `PortalCooldown` | `PlayerPortalCooldown` | Partial | Unit Tested | Partial Parity | Reuse time and entry count are validated after composed add. C# immutable record replacement differs from Java mutable object semantics. |
| No direct Java DTO; behavior is inline | `InstanceEntranceCooldownResult` | Refactored | Unit Tested | Intentional Difference | C# returns structured data so later packet/persistence fanout can be tested. Java performs inline side effects and returns void. |

Metrics from this handoff window:

- Total focused sessions covered: 1
- Total commits covered: 1
- Current full validation baseline: 1073 tests passing
- Total artifacts with verified parity newly promoted in this window: 0
- Total blocked artifacts: production portal/autogroup caller integration, immediate cooldown persistence, `SM_INSTANCE_INFO` fanout, full portal validation/teleport flow, and Java runtime/live-client validation
- Estimated overall migration completion: Phase 6 remains about 56% complete; the cooldown side-effect is reusable but end-to-end instance entry remains incomplete.

---

## Important Limits

- `InstanceEntranceCooldownService` is not yet wired into actual portal, teleport, or autogroup callers.
- Java `PortalCooldownsDAO.storePortalCooldowns(owner)` parity is still missing.
- Java `PortalCooldownList.sendEntryInfo(worldId)` / `SM_INSTANCE_INFO` fanout is still missing.
- Full portal path validation, item/kinah consumption, instance creation/registration, start-position state, and teleport packet ordering are still outside this boundary.
- No database schema, packet wire format, live client behavior, or threading model changed in this window.

---

## Next Unit Of Work

Recommended next unit: add `SM_INSTANCE_INFO` fanout support around portal cooldown changes as a testable packet-emission boundary.

Suggested shape:

1. Re-read Java:
   - `PortalCooldownList.addPortalCooldown`
   - `PortalCooldownList.sendEntryInfo`
   - `SM_INSTANCE_INFO(byte, Player, int...)`
   - team fanout path through `owner.getCurrentTeam().sendPackets(...)`
2. Re-read C#:
   - `PlayerPortalCooldownService`
   - `InstanceEntranceCooldownService`
   - `SmInstanceInfo`
   - current player/team membership models
   - `GameServerConnection` packet-send helpers, if a connection-level test boundary is feasible
3. Keep it narrow:
   - prefer a helper/result that emits `SmInstanceInfo(2, player, instanceCooltimes, worldId)` when `InstanceEntranceCooldownResult.Added`
   - if team fanout cannot be represented yet, document it as missing and keep player-only fanout tested
   - do not attempt full portal validation, teleport, DAO persistence, or live team routing in the same unit
   - update `docs/PHASE-6-PROGRESS.md` with a Migration Parity Table before committing

Alternative units:

- Add immediate portal cooldown DAO persistence boundary if repository write support is already close enough.
- Start the actual general portal caller surface only if the Java `PortalService.transfer` path can be mirrored without dragging in full portal validation.

Useful validation commands:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~InstanceEntranceCooldownServiceTests|FullyQualifiedName~PlayerStateTests|FullyQualifiedName~GamePacketTests"
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~InstanceCooldownRateServiceTests|FullyQualifiedName~GameServerOptionsTests|FullyQualifiedName~WorldMapRuntimeStateTests"
dotnet test dotnetConversion\AionServer.slnx
```

---

## Resume Checklist

- Start from branch `4.8`.
- Confirm `git status --short` is clean.
- Read `docs/csharp-port.md`, latest `docs/PHASE-6-PROGRESS.md` Sessions 516-517, `docs/Phase-6CL-Completion.md`, and this handoff.
- Continue with one focused Java-parity unit.
- Update the migration parity table before committing.
- Commit the unit, then repeat until the next handoff is needed.
