# Phase 6CQ Completion Handoff

**Created**: May 23, 2026  
**Status**: Phase 6 remains in progress; this handoff follows 6CP and covers Sessions 527-529.  
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, runtime side effects, static-data semantics, persistence behavior, scheduling, and combat math.  
**Workflow rule**: Do one focused unit of work, validate it, update the migration parity table, commit it, then repeat for as long as useful work remains.  
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.  
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx` passes with 1103 tests.

---

## Recent Work Completed

- Added `PortalEntryValidationService.ValidateEnterLevel`, modeling Java `PortalService.checkEnterLevel` with portal-path min-level override, race-specific `InstanceCooltime` fallback, max-level enforcement, explicit bypass input, and failure packet selection.
- Added `SmSystemMessage.CantInstanceEnterLevel()` for Java `SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_ENTER_LEVEL` (`1400179`).
- Widened portal validation failure packets to `GameServerPacket` so helpers can return either system messages or dialog windows.
- Added `Player.IsMentor`, `InstanceCooltimeSummary.CanEnterMentor`, `InstanceCooltimeTable.CanEnterMentor`, XML parsing for `can_enter_mentor`, `SmSystemMessage.MentorCantEnter(worldId)`, and `PortalEntryValidationService.ValidateMentor`.
- Added `PortalEntryValidationService.ValidateRace`, `SmDialogWindow.NoRightPageId`, and `SmSystemMessage.MovePortalErrorInvalidRace()`.
- Added/extended tests for level guard min/max behavior, err-level dialog payload, mentor allow/reject behavior, race guard `PC_ALL`/mismatch/siege-supplied failure paths, system-message serialization, and static-data loading.
- Updated `docs/PHASE-6-PROGRESS.md` through Session 529 with migration parity tables, risks, metrics, validation, and next recommended work.

---

## Commits In This Handoff

- `45c2598b9` - `Add portal enter level validation`
- `bf4d44004` - `Add portal mentor validation`
- `6140d855b` - `Add portal race validation`

---

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|
| `PortalService.checkEnterLevel` | `PortalEntryValidationService.ValidateEnterLevel` | Partial | Unit Tested | Partial Parity | Min/max-level behavior and failure packet choice are represented. Portal-path static loading, membership/admin permission integration, and production wiring are missing. |
| `PortalService.checkMentor` | `PortalEntryValidationService.ValidateMentor` | Partial | Unit Tested | Partial Parity | Mentor/template allow-reject behavior is represented. Mentor lifecycle and live group side effects remain missing. |
| `PortalService.checkRace` | `PortalEntryValidationService.ValidateRace` | Partial | Unit Tested | Partial Parity | Portal race mismatch and supplied siege failure outcomes are represented. Portal-path loading and SiegeService fortress ownership are missing. |
| `InstanceCooltime.can_enter_mentor` | `InstanceCooltimeSummary.CanEnterMentor` / `InstanceCooltimeTable.CanEnterMentor` / `StaticData` | Partial | Unit Tested / Regression Tested | Partial Parity | Field is parsed and exposed. Broader Java JAXB runtime comparison was not run. |
| `SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_ENTER_LEVEL` | `SmSystemMessage.CantInstanceEnterLevel` | Complete | Regression Tested | Partial Parity | Message id `1400179` is source-derived and serialized by tests; no live-client capture. |
| `SM_SYSTEM_MESSAGE.STR_MSG_MENTOR_CANT_ENTER` | `SmSystemMessage.MentorCantEnter` | Complete | Regression Tested | Partial Parity | Message id `1400766` plus world-id parameter are covered by packet tests; no live-client capture. |
| `SM_SYSTEM_MESSAGE.STR_MOVE_PORTAL_ERROR_INVALID_RACE` | `SmSystemMessage.MovePortalErrorInvalidRace` | Complete | Regression Tested | Partial Parity | Message id `901354` is covered by packet tests; no live-client capture. |
| `DialogPage.NO_RIGHT` / `SM_DIALOG_WINDOW` | `SmDialogWindow.NoRightPageId` / `SmDialogWindow` | Partial | Unit Tested / Regression Tested | Partial Parity | Ordinary zero-context `NO_RIGHT` branch is covered for portal validation. Full DialogPage enum is not ported. |

Metrics from this handoff window:

- Total focused sessions covered: 3
- Total commits covered: 3
- Current full validation baseline: 1103 tests passing
- Total artifacts with verified parity newly promoted in this window: 0
- Total blocked artifacts: production portal dialog/selection wiring, full `PortalPath` static-data model, `SiegeService` fortress ownership, membership/admin permission integration, mentor lifecycle/team side effects, remaining rank/title/quest/player-size/item/kinah guards, and live-client validation
- Estimated overall migration completion: Phase 6 remains about 56% complete; multiple source-order portal guards are represented, but live instance entry is still incomplete.

---

## Important Limits

- No production packet handler calls the portal validation helpers yet.
- `PortalPath` static-data loading is still absent, so level/race/rank/title/quest/item/kinah guard inputs are explicit parameters.
- `SiegeService.getFortress` and fortress race ownership are not ported for the race guard.
- Admin and membership bypasses are represented as explicit parameters, not resolved from live player/config permission services.
- Java mentor lifecycle, group mentor conversion, force-leave behavior, and visible-player mentor status fanout are not modeled.
- Remaining portal validation gaps include rank, title, quests, group/alliance/league size, required items, kinah, same-instance teleport integration, and production transfer wiring.
- Instance difficulty, handler supplier, spawn engine, `InstanceHandler.onInstanceCreate`, auto-destroy scheduling, event spawns, and team fanout remain unported.

---

## Next Unit Of Work

Recommended next unit: decide whether to continue source-order guards with explicit parameters or first add the `PortalPath` static-data model.

Best next slice:

1. Port `PortalService.checkRank` narrowly:
   - Java source: `PortalService.checkRank`
   - C# source: `Player.AbyssRank`, `PortalEntryValidationService`
   - Inputs: explicit `portalPathMinRank` and `npcObjectId` until `PortalPath` loading exists
   - Failure packet: `SM_DIALOG_WINDOW(npc.getObjectId(), DialogPage.NO_RIGHT.id())`
2. Alternative, more foundational slice:
   - Add minimal `PortalPath` summaries and XML loading for `race`, `siege_id`, `min_level`, `min_rank`, `title_id`, `err_level`, and `err_group`
   - Then refactor validation helpers to consume the loaded portal-path summary instead of explicit primitive parameters

Useful validation commands:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PortalEntryValidationServiceTests|FullyQualifiedName~WorldMapRuntimeStateTests|FullyQualifiedName~GamePacketTests|FullyQualifiedName~StaticDataLoadingTests"
dotnet test dotnetConversion\AionServer.slnx
```

---

## Resume Checklist

- Start from branch `4.8`.
- Confirm `git status --short` is clean.
- Read `docs/csharp-port.md`, latest `docs/PHASE-6-PROGRESS.md` Sessions 527-529, `docs/Phase-6CP-Completion.md`, and this handoff.
- Continue with one focused Java-parity unit.
- Update the migration parity table before committing.
- Commit the unit, then repeat until the next handoff is needed.
