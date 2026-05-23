# Phase 6CL Completion Handoff

**Created**: May 23, 2026  
**Status**: Phase 6 remains in progress; this handoff follows 6CK and covers Sessions 515-516.  
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, world/known-list behavior, persistence behavior, scheduling, stat formulas, observer side effects, and combat math.  
**Workflow rule**: Do one focused unit of work, validate it, update the migration parity table, commit it, then repeat for as long as useful work remains.  
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.  
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx` passes with 1070 tests.

---

## Recent Work Completed

- Extended `InstanceCooltimeSummary` and the static-data loader to preserve Java `InstanceCooltime.type`, `typevalue`, and `ent_cool_time` from `instance_cooltimes.xml`.
- Added `InstanceCooltimeTable.CalculateInstanceEntranceCooltime(worldId, now, instanceCooldownRate)` as a source-shaped helper for Java `InstanceCooltimeData.calculateInstanceEntranceCooltime`.
- Modeled `RELATIVE`, `DAILY`, and `WEEKLY` reset calculations, including `maxcount == 0` and missing-template `0`, relative `ent_cool_time == 0`, strict daily rollover after the reset time, weekly reset-day selection, and cooldown-rate shortening.
- Added deterministic tests for relative, daily, weekly, and rate behavior, plus bundled XML field loading checks.
- Added Java config bindings for `gameserver.instances.cooldown`, `gameserver.instance.cooldown_rate`, and `gameserver.instance.cooldown_rate.excluded_maps`.
- Added `InstanceCooldownRateService.GetInstanceRate(player, mapId, options)` as the narrow C# equivalent of Java `InstanceService.getInstanceRate(Player, int)`.
- Added focused tests for membership fallback, excluded maps, configured-rate usage, and the Java-shaped configured `0` rate edge.
- Updated `docs/PHASE-6-PROGRESS.md` through Session 516 with migration parity tables, risks, metrics, validation, and next recommended work.

---

## Commits In This Handoff

- `f42713459` - `Add instance entrance cooldown calculation`
- `17c8c9f41` - `Add instance cooldown rate gate`

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
| `InstanceService.getInstanceRate` | `InstanceCooldownRateService.GetInstanceRate` | Complete | Unit Tested | Partial Parity | Membership/config/excluded-map gate is now modeled. Production callers still need to use it. |
| `MembershipConfig.INSTANCES_COOLDOWN` | `GameServerMembershipOptions.InstancesCooldown` | Complete | Unit Tested | Partial Parity | Default Java config key is loaded. Dedicated environment override test is not present. |
| `InstanceConfig.INSTANCE_COOLDOWN_RATE` | `GameServerInstanceOptions.CooldownRate` | Complete | Unit Tested | Partial Parity | Default Java config key is loaded and service returns configured values. |
| `InstanceConfig.INSTANCE_COOLDOWN_RATE_EXCLUDED_MAPS` | `GameServerInstanceOptions.CooldownRateExcludedMaps` | Complete | Unit Tested | Partial Parity | Empty default and excluded-map behavior are covered. |

Metrics from this handoff window:

- Total focused sessions covered: 2
- Total commits covered: 2
- Current full validation baseline: 1070 tests passing
- Total artifacts with verified parity newly promoted in this window: 0
- Total blocked artifacts: production portal/autogroup caller integration, immediate cooldown persistence, `SM_INSTANCE_INFO` fanout, Java runtime/live-client validation, and broader shared permission-model cleanup
- Estimated overall migration completion: Phase 6 remains about 56% complete; this adds reset-time calculation and rate-gate support but not end-to-end instance entry parity.

---

## Important Limits

- `CalculateInstanceEntranceCooltime` is not yet called by portal, autogroup, teleport, or cooldown persistence flows.
- `InstanceService.getInstanceRate` is modeled as a helper, but production callers are not wired to it yet.
- A configured cooldown rate of `0` is returned unchanged like the Java gate; passing it to cooldown calculation would still be invalid later, as in Java.
- C# uses string cooltime types instead of a Java-equivalent enum.
- Server timezone handling is caller-provided through `DateTimeOffset`; Java uses `ServerTime.now()`.
- No DAO persistence, `SM_INSTANCE_INFO` fanout, database schema, packet wire format, or live client behavior changed in this window.

---

## Next Unit Of Work

Recommended next unit: wire a production-shaped portal/autogroup caller to combine `InstanceCooldownRateService`, `InstanceCooltimeTable.CalculateInstanceEntranceCooltime`, and `PlayerPortalCooldownService.AddPortalCooldown`.

Suggested shape:

1. Re-read Java:
   - `PortalService` cooldown add path around `calculateInstanceEntranceCooltime` and `addPortalCooldown`
   - `AutoInstance.onPressEnter` or nearby autogroup cooldown path
   - `PortalCooldownList.addPortalCooldown` packet/persistence fanout
2. Re-read C#:
   - `InstanceCooldownRateService`
   - `InstanceCooltimeTable.CalculateInstanceEntranceCooltime`
   - `PlayerPortalCooldownService`
   - `SmInstanceInfo`
   - current portal/teleport/autogroup surfaces
3. Keep it narrow:
   - wire one caller boundary only if packet/database side effects can be isolated and tested
   - keep DAO persistence and `SM_INSTANCE_INFO` fanout explicit in the parity table if left out
   - do not attempt full portal entry validation, handler/spawn lifecycle, or team routing in the same unit
   - update `docs/PHASE-6-PROGRESS.md` with a Migration Parity Table before committing

Alternative units:

- Add `SM_INSTANCE_INFO` fanout after cooldown changes if packet order can be tested without broader portal entry work.
- Start a shared `Player.HasPermission(byte)` helper and consolidate only the currently duplicated membership comparisons with tests.

Useful validation commands:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRuntimeStateTests|FullyQualifiedName~StaticDataLoadingTests"
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~InstanceCooldownRateServiceTests|FullyQualifiedName~GameServerOptionsTests|FullyQualifiedName~WorldMapRuntimeStateTests"
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerStateTests|FullyQualifiedName~WorldMapRuntimeStateTests"
dotnet test dotnetConversion\AionServer.slnx
```

---

## Resume Checklist

- Start from branch `4.8`.
- Confirm `git status --short` is clean.
- Read `docs/csharp-port.md`, latest `docs/PHASE-6-PROGRESS.md` Sessions 515-516, `docs/Phase-6CK-Completion.md`, and this handoff.
- Continue with one focused Java-parity unit.
- Update the migration parity table before committing.
- Commit the unit, then repeat until the next handoff is needed.
