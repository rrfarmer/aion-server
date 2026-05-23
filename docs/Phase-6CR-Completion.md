# Phase 6CR Completion Handoff

**Created**: May 23, 2026  
**Status**: Phase 6 remains in progress; this handoff follows 6CQ and covers Sessions 530-531.  
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, runtime side effects, static-data semantics, persistence behavior, scheduling, and combat math.  
**Workflow rule**: Do one focused unit of work, validate it, update the migration parity table, commit it, then repeat for as long as useful work remains.  
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.  
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx` passes with 1110 tests.

---

## Recent Work Completed

- Added `PortalEntryValidationService.ValidateRank`, modeling Java `PortalService.checkRank` with `Player.AbyssRank.Rank >= PortalPath.minRank` and `SM_DIALOG_WINDOW(..., DialogPage.NO_RIGHT.id())` failure.
- Added `PortalEntryValidationService.ValidateTitle`, modeling Java `PortalService.checkTitle` against the active common-data title id (`Player.TitleId`), not learned-title-list membership.
- Added/extended tests for rank allow/reject, min-rank zero, title requirement missing, active-title match, learned-but-inactive title mismatch, bypass behavior, and dialog payloads.
- Updated `docs/PHASE-6-PROGRESS.md` through Session 531 with migration parity tables, risks, metrics, validation, and next recommended work.

---

## Commits In This Handoff

- `bd4597743` - `Add portal rank validation`
- `7bf498791` - `Add portal title validation`

---

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|
| `PortalService.checkRank` | `PortalEntryValidationService.ValidateRank` | Partial | Unit Tested | Partial Parity | Rank-id comparison and `NO_RIGHT` dialog failure are represented. Portal-path loading and production wiring are missing. |
| `PortalService.checkTitle` | `PortalEntryValidationService.ValidateTitle` | Partial | Unit Tested | Partial Parity | Active-title comparison and `NO_RIGHT` dialog failure are represented. Portal-path loading, title activation flow, and production wiring are missing. |
| `PortalPath.getMinRank` | Explicit `portalPathMinRank` parameter | Partial | Unit Tested | Needs Verification | Still caller-supplied because `PortalPath` XML/static-data loading does not exist. |
| `PortalPath.getTitleId` | Explicit `portalPathTitleId` parameter | Partial | Unit Tested | Needs Verification | Still caller-supplied because `PortalPath` XML/static-data loading does not exist. |
| `Player.getAbyssRank().getRank().getId` | `PlayerAbyssRank.Rank` | Partial | Unit Tested | Partial Parity | Existing C# rank id is consumed directly; no Java runtime comparison in this handoff. |
| `PlayerCommonData.getTitleId` | `Player.TitleId` | Partial | Unit Tested | Partial Parity | Tests confirm learned title ownership alone does not satisfy the guard; active title id must match. |
| `DialogPage.NO_RIGHT` / `SM_DIALOG_WINDOW` | `SmDialogWindow.NoRightPageId` / `SmDialogWindow` | Partial | Unit Tested / Regression Tested | Partial Parity | Ordinary zero-context `NO_RIGHT` payload is covered. Full `DialogPage` enum and live-client dispatch are not verified. |

Metrics from this handoff window:

- Total focused sessions covered: 2
- Total commits covered: 2
- Current full validation baseline: 1110 tests passing
- Total artifacts with verified parity newly promoted in this window: 0
- Total blocked artifacts: production portal dialog/selection wiring, full `PortalPath` static-data model, membership/admin permission integration, remaining quest/player-size/item/kinah guards, and live-client validation
- Estimated overall migration completion: Phase 6 remains about 56% complete; additional source-order portal guards are represented, but live instance entry is still incomplete.

---

## Important Limits

- No production packet handler calls the portal validation helpers yet.
- `PortalPath` static-data loading is now the largest local blocker for portal validation quality. Level, race, rank, title, and future quest/item/kinah guards are still taking explicit primitive inputs.
- Admin and membership bypasses are represented as explicit parameters, not resolved from live player/config permission services.
- Java `TitleList` learned-title membership is not involved in `checkTitle`; this handoff intentionally uses `Player.TitleId`.
- Remaining portal validation gaps include quests, group/alliance/league size, required items, kinah, same-instance teleport integration, and production transfer wiring.
- Instance difficulty, handler supplier, spawn engine, `InstanceHandler.onInstanceCreate`, auto-destroy scheduling, event spawns, and team fanout remain unported.

---

## Next Unit Of Work

Recommended next unit: add a minimal `PortalPath` static-data model/loading slice before more portal guards.

Suggested scope:

1. Re-read Java:
   - `game-server/src/com/aionemu/gameserver/model/templates/portal/PortalPath.java`
   - Portal XML holder/data classes around `PORTAL_DATA` / `PORTAL_LOC_DATA`
   - Sample XML under `game-server/data/static_data/portals`
2. Add C# summaries:
   - `PortalPathSummary` with `dialog`, `loc_id`, `siege_id`, `race`, `min_level`, `min_rank`, `kinah`, `title_id`, `err_group`, and `err_level`
   - A table lookup keyed by dialog or another Java-equivalent key, depending on the Java holder
3. Add loader/tests:
   - Parse a representative real portal path from Java XML
   - Assert the fields needed by existing validation helpers
   - Keep quest/item child lists out of scope unless they are cheap to carry structurally without mutation behavior
4. Then refactor the validation helpers in a later unit to consume `PortalPathSummary` instead of primitive parameters.

Useful validation commands:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PortalEntryValidationServiceTests|FullyQualifiedName~GamePacketTests|FullyQualifiedName~StaticDataLoadingTests"
dotnet test dotnetConversion\AionServer.slnx
```

---

## Resume Checklist

- Start from branch `4.8`.
- Confirm `git status --short` is clean.
- Read `docs/csharp-port.md`, latest `docs/PHASE-6-PROGRESS.md` Sessions 530-531, `docs/Phase-6CQ-Completion.md`, and this handoff.
- Continue with one focused Java-parity unit.
- Update the migration parity table before committing.
- Commit the unit, then repeat until the next handoff is needed.
