# Phase 6CS Completion Handoff

**Created**: May 23, 2026  
**Status**: Phase 6 remains in progress; this handoff follows 6CR and covers Sessions 532-534.  
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, runtime side effects, static-data semantics, persistence behavior, scheduling, and combat math.  
**Workflow rule**: Do one focused unit of work, validate it, update the migration parity table, commit it, then repeat for as long as useful work remains.  
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.  
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx` passes with 1116 tests.

---

## Recent Work Completed

- Added `PortalPathTable`, `PortalPathSummary`, and `PortalPathSource`, modeling Java `Portal2Data` maps for `portal_use`, `portal_dialog`, and `portal_scroll`.
- Added static-data parsing for Java `portal_template2.xml` scalar path fields: `dialog`, `loc_id`, `siege_id`, `race`, `min_level`, `min_rank`, `kinah`, `title_id`, `err_group`, and `err_level`.
- Preserved Java race fallback lookup behavior: use/dialog lookup returns a same-race or `PC_ALL` path first, otherwise returns an opposite-race matching path so the caller can emit the invalid-race failure.
- Added `PortalPathSummary` overloads for existing portal validation helpers: level, race, rank, and title can now consume loaded path data instead of only primitive test inputs.
- Added `PortalLocTable` and `PortalLocSummary`, modeling Java `PortalLocData` / `PortalLoc` lookup by `loc_id`.
- Added static-data parsing for Java `portal_loc.xml` fields: `world_id`, `loc_id`, `x`, `y`, `z`, and optional heading `h` defaulting to zero.
- Updated `docs/PHASE-6-PROGRESS.md` through Session 534 with migration parity tables, risks, metrics, validation, and next recommended work.

---

## Commits In This Handoff

- `3f635bb63` - `Add portal path static data loading`
- `17e495dd9` - `Use portal path summaries in validation`
- `0148063b6` - `Add portal location static data loading`

---

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|
| `Portal2Data` | `PortalPathTable` | Partial | Unit Tested / Regression Tested | Partial Parity | Separate use/dialog/scroll maps, portal NPC detection, teleport dialog id default, and race fallback lookups are represented. Global `DataManager.PORTAL2_DATA` access is not wired. |
| `PortalPath` | `PortalPathSummary` | Partial | Unit Tested / Regression Tested | Partial Parity | Scalar fields are parsed and consumed by validation helpers. `quest_req` and `item_req` children remain missing. |
| `PortalUse` / `PortalDialog` / `PortalScroll` | `PortalPathTable` lookups | Partial | Unit Tested / Regression Tested | Partial Parity | Parent-key lookup shape is represented. Production portal handlers still do not use these tables. |
| `PortalService.checkEnterLevel` / `checkRace` / `checkRank` / `checkTitle` | `PortalEntryValidationService` `PortalPathSummary` overloads | Partial | Unit Tested | Partial Parity | Existing helper behavior now accepts loaded path DTOs. Full `PortalService.port` ordering is still not orchestrated. |
| `PortalLocData` | `PortalLocTable` | Partial | Unit Tested / Regression Tested | Partial Parity | Lookup by `loc_id` is represented. Duplicate-key behavior and global data-manager access are not Java-runtime verified. |
| `PortalLoc` | `PortalLocSummary` | Partial | Unit Tested / Regression Tested | Partial Parity | Destination world id, coordinates, and heading are parsed. Java mutability/setters are not ported. |

Metrics from this handoff window:

- Total focused sessions covered: 3
- Total commits covered: 3
- Current full validation baseline: 1116 tests passing
- Total artifacts with verified parity newly promoted in this window: 0
- Total blocked artifacts: production portal orchestration, `QuestReq`, `ItemReq`, group-size checks, kinah/item consumption, siege ownership lookup, duplicate-key runtime behavior, and live-client validation
- Estimated overall migration completion: Phase 6 is about 58% complete; portal template and destination data are now loaded, but live portal entry remains incomplete.

---

## Important Limits

- No production packet handler calls the portal data tables or validation overloads yet.
- `PortalPathSummary.LocId` can now resolve through `PortalLocTable`, but no orchestration helper combines them into Java `PortalService.port` behavior.
- Java `QuestReq` and `ItemReq` children are discovered but not parsed.
- Java required-item removal, kinah consumption, quest gating, group/alliance/league size validation, same-instance teleport, instance allocation, and transfer wiring remain missing.
- Admin and membership bypasses are still explicit helper parameters, not live permission/config checks.
- Siege ownership remains caller-supplied; `PortalPathSummary.SiegeId` is loaded but not resolved through a C# `SiegeService`.
- Duplicate portal path/loc key behavior is not runtime-compared to Java `HashMap.put`; current C# table construction can throw on duplicate keys.
- Float parsing for portal coordinates uses invariant culture and is source-literal tested, but no Java runtime float comparison or live teleport validation was run.

---

## Next Unit Of Work

Recommended next unit: add a narrow portal-entry orchestration helper for the solo/no-registration path.

Suggested scope:

1. Re-read Java `PortalService.port` guard order:
   - `PortalLocData.getPortalLoc(portalPath.getLocId())`
   - admin bypass boundary
   - `checkMentor`, `checkRace`, `checkRank`, `checkTitle`, then later quest/player-size gaps
   - registered instance / cooldown handling
   - `checkEnterLevel` skipped on reenter
2. Add a C# result type that can express at least:
   - allowed with resolved `PortalLocSummary`
   - missing portal location
   - existing validation failure status and packet
   - reenter flag for the existing solo registered-instance case
3. Keep scope narrow:
   - Use `PortalPathSummary`, `PortalLocTable`, `InstanceCooltimeTable`, and existing `WorldMapRuntimeStateTable`.
   - Do not implement quest checks, item removal, kinah consumption, group/alliance/league size, or actual teleport transfer in this unit.
4. Add tests with synthetic path/loc/cooltime data:
   - missing `loc_id` returns missing-location result
   - mentor/race/rank/title failures happen before cooldown
   - cooldown failure happens before level check for unregistered entry
   - reenter skips cooldown/level lockout like Java

Useful validation commands:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PortalEntryValidationServiceTests|FullyQualifiedName~StaticDataLoadingTests|FullyQualifiedName~WorldMapRuntimeStateTests"
dotnet test dotnetConversion\AionServer.slnx
```

---

## Resume Checklist

- Start from branch `4.8`.
- Confirm `git status --short` is clean.
- Read `docs/csharp-port.md`, latest `docs/PHASE-6-PROGRESS.md` Sessions 532-534, `docs/Phase-6CR-Completion.md`, and this handoff.
- Continue with one focused Java-parity unit.
- Update the migration parity table before committing.
- Commit the unit, then repeat until the next handoff is needed.
