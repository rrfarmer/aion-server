# Phase 6CL Completion Handoff

**Created**: May 23, 2026  
**Status**: Phase 6 remains in progress; this handoff follows 6CK and covers Session 515.  
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, world/known-list behavior, persistence behavior, scheduling, stat formulas, observer side effects, and combat math.  
**Workflow rule**: Do one focused unit of work, validate it, update the migration parity table, commit it, then repeat for as long as useful work remains.  
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.  
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx` passes with 1068 tests.

---

## Recent Work Completed

- Extended `InstanceCooltimeSummary` and the static-data loader to preserve Java `InstanceCooltime.type`, `typevalue`, and `ent_cool_time` from `instance_cooltimes.xml`.
- Added `InstanceCooltimeTable.CalculateInstanceEntranceCooltime(worldId, now, instanceCooldownRate)` as a source-shaped helper for Java `InstanceCooltimeData.calculateInstanceEntranceCooltime`.
- Modeled `RELATIVE`, `DAILY`, and `WEEKLY` reset calculations, including `maxcount == 0` and missing-template `0`, relative `ent_cool_time == 0`, strict daily rollover after the reset time, weekly reset-day selection, and cooldown-rate shortening.
- Added deterministic tests for relative, daily, weekly, and rate behavior, plus bundled XML field loading checks.
- Updated `docs/PHASE-6-PROGRESS.md` through Session 515 with migration parity table, risks, metrics, validation, and next recommended work.

---

## Commits In This Handoff

- `f42713459` - `Add instance entrance cooldown calculation`

---

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|
| `InstanceCooltimeData.calculateInstanceEntranceCooltime` | `InstanceCooltimeTable.CalculateInstanceEntranceCooltime` | Partial | Unit Tested | Partial Parity | Relative/daily/weekly formulas are covered with deterministic C# tests. Java runtime, server timezone, membership/config rate lookup, and production caller paths are not validated. |
| `InstanceCooltime.getCoolTimeType` / `getTypeValue` / `getEntCoolTime` | `InstanceCooltimeSummary.CoolTimeType` / `TypeValue` / `EntCoolTime` | Partial | Unit Tested | Partial Parity | Required reset fields are loaded. Level/mentor fields remain outside this slice. |
| `InstanceCoolTimeType` | string cooltime type values | Refactored | Unit Tested | Needs Verification | C# uses string values rather than a Java-equivalent enum. Invalid values return `0` instead of Java logging a warning. |
| `InstanceService.getInstanceRate` | caller-supplied `instanceCooldownRate` | Partial | Unit Tested | Needs Verification | Rate formula is tested, but config and membership access checks are not wired. |
| `ServerTime.now` | `DateTimeOffset now` argument | Refactored | Unit Tested | Intentional Difference | Time is injected for deterministic tests. Production callers must pass a correctly-zoned value. |
| `java.time.DayOfWeek` weekly reset mapping | private day mapping in `InstanceCooltimeTable` | Partial | Unit Tested | Partial Parity | Mon-Sun mapping and weekly selection are covered. Exception type differs for invalid day strings. |

Metrics from this handoff window:

- Total focused sessions covered: 1
- Total commits covered: 1
- Current full validation baseline: 1068 tests passing
- Total artifacts with verified parity newly promoted in this window: 0
- Total blocked artifacts: `InstanceService.getInstanceRate` config/membership wiring, production portal/autogroup caller integration, immediate cooldown persistence, `SM_INSTANCE_INFO` fanout, and Java runtime/live-client validation
- Estimated overall migration completion: Phase 6 remains about 56% complete; this adds reset-time calculation support but not end-to-end instance entry parity.

---

## Important Limits

- `CalculateInstanceEntranceCooltime` is not yet called by portal, autogroup, teleport, or cooldown persistence flows.
- `InstanceService.getInstanceRate` remains incomplete: C# does not yet evaluate `MembershipConfig.INSTANCES_COOLDOWN`, `InstanceConfig.INSTANCE_COOLDOWN_RATE`, or excluded maps.
- C# uses string cooltime types instead of a Java-equivalent enum.
- Server timezone handling is caller-provided through `DateTimeOffset`; Java uses `ServerTime.now()`.
- No DAO persistence, `SM_INSTANCE_INFO` fanout, database schema, packet wire format, or live client behavior changed in this window.

---

## Next Unit Of Work

Recommended next unit: add the missing `InstanceService.getInstanceRate` membership/config gate so production callers can supply a Java-shaped cooldown rate.

Suggested shape:

1. Re-read Java:
   - `InstanceService.getInstanceRate(Player, int)`
   - `MembershipConfig.INSTANCES_COOLDOWN`
   - `InstanceConfig.INSTANCE_COOLDOWN_RATE`
   - `InstanceConfig.INSTANCE_COOLDOWN_RATE_EXCLUDED_MAPS`
   - `Player.hasPermission(byte)`
2. Re-read C#:
   - `Player.AccessLevel` / account membership fields
   - `GameServerOptions.Membership`
   - `GameServerOptions` config loading helpers
   - `InstanceCooltimeTable.CalculateInstanceEntranceCooltime`
3. Keep it narrow:
   - add config properties only for the Java keys needed by this rate gate
   - add a small service/helper with deterministic unit tests
   - do not wire full portal entry, DAO persistence, or packet fanout in the same unit unless the caller boundary is already clean
   - update `docs/PHASE-6-PROGRESS.md` with a Migration Parity Table before committing

Alternative units:

- Wire a production-shaped portal/autogroup caller to calculate entrance cooldowns and update `PlayerPortalCooldownService`.
- Add `SM_INSTANCE_INFO` fanout after cooldown changes if packet order can be tested without broader portal entry work.

Useful validation commands:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRuntimeStateTests|FullyQualifiedName~StaticDataLoadingTests"
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerStateTests|FullyQualifiedName~WorldMapRuntimeStateTests"
dotnet test dotnetConversion\AionServer.slnx
```

---

## Resume Checklist

- Start from branch `4.8`.
- Confirm `git status --short` is clean.
- Read `docs/csharp-port.md`, latest `docs/PHASE-6-PROGRESS.md` Sessions 514-515, `docs/Phase-6CK-Completion.md`, and this handoff.
- Continue with one focused Java-parity unit.
- Update the migration parity table before committing.
- Commit the unit, then repeat until the next handoff is needed.
