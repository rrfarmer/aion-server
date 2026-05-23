# Phase 6CP Completion Handoff

**Created**: May 23, 2026  
**Status**: Phase 6 remains in progress; this handoff follows 6CO and covers Sessions 524-526.  
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, runtime side effects, static-data semantics, persistence behavior, scheduling, and combat math.  
**Workflow rule**: Do one focused unit of work, validate it, update the migration parity table, commit it, then repeat for as long as useful work remains.  
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.  
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx` passes with 1087 tests.

---

## Recent Work Completed

- Added `PortalEntryValidationService.ValidateCooldown`, the first source-shaped Java `PortalService.port` guard: fresh instance creation is rejected when `PortalCooldownList.isPortalUseDisabled(mapId)` is true.
- Added `SmSystemMessage.CannotMakeInstanceCoolTime()` for Java `SM_SYSTEM_MESSAGE.STR_MSG_CANNOT_MAKE_INSTANCE_COOL_TIME` (`1400043`).
- Extended portal validation planning with `ValidateCooldownForRegisteredInstance`, resolving currently supported solo registered instances before cooldown lockout and marking registered reentry when the player is outside the target world/instance.
- Added static-data support for Java `InstanceCooltime` race-specific entrance level fields: `enter_min_level_light`, `enter_max_level_light`, `enter_min_level_dark`, and `enter_max_level_dark`.
- Added/extended tests for cooldown lockout, expired cooldown removal, solo registered-instance reentry, system-message serialization, race-specific entrance-level lookup, and real static-data loading.
- Updated `docs/PHASE-6-PROGRESS.md` through Session 526 with migration parity tables, risks, metrics, validation, and next recommended work.

---

## Commits In This Handoff

- `57620d3bd` - `Add portal cooldown validation guard`
- `13bf33e9d` - `Plan portal registered cooldown checks`
- `894ffe8a4` - `Load instance entrance level limits`

---

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|
| `PortalService.port` cooldown guard | `PortalEntryValidationService.ValidateCooldown` | Partial | Unit Tested | Partial Parity | Cooldown-lock rejection and Java system message are modeled. Full guard order and live portal handler wiring remain missing. |
| `PortalService.port` registered-instance branch | `PortalEntryValidationService.ValidateCooldownForRegisteredInstance` | Partial | Unit Tested | Partial Parity | Solo registered-instance lookup and reentry flag are modeled. Group/alliance/league owner resolution is blocked by missing C# team models. |
| `PortalCooldownList.isPortalUseDisabled` | `PlayerPortalCooldownService.IsPortalUseDisabled` via validation service | Partial | Unit Tested | Partial Parity | Below-max, max-count, and expired-removal paths are covered. Missing static rows are defensively allowed in C#, unlike Java's source assumptions. |
| `SM_SYSTEM_MESSAGE.STR_MSG_CANNOT_MAKE_INSTANCE_COOL_TIME` | `SmSystemMessage.CannotMakeInstanceCoolTime` | Complete | Regression Tested | Partial Parity | Message id `1400043` is serialized through the existing packet writer; no live-client packet capture was run. |
| `InstanceCooltime` entrance-level fields | `InstanceCooltimeSummary` / `InstanceCooltimeTable` / `StaticData` | Partial | Unit Tested / Regression Tested | Partial Parity | Race-specific min/max level fields are parsed and queryable. `PortalService.checkEnterLevel` enforcement is not ported yet. |

Metrics from this handoff window:

- Total focused sessions covered: 3
- Total commits covered: 3
- Current full validation baseline: 1087 tests passing
- Total artifacts with verified parity newly promoted in this window: 0
- Total blocked artifacts: production portal dialog/selection wiring, full portal guard order, group/alliance/league team model and owner ids, portal-path template model, `SM_DIALOG_WINDOW` err-level branch, level failure system message, item/kinah mutation, and live-client validation
- Estimated overall migration completion: Phase 6 remains about 56% complete; portal validation has started, but live instance entry is still incomplete.

---

## Important Limits

- No production packet handler calls the new portal validation helpers yet.
- The registered-instance helper only supports solo player-object-id registrations. Java group/alliance/league registrations remain blocked.
- `PortalService.checkEnterLevel` is not implemented yet; this handoff only added the needed `InstanceCooltime` data.
- Portal template loading is still absent, so `portalPath.getMinLevel()` overrides and `portalPath.getErrLevel()` dialogs cannot yet be loaded from source data.
- Remaining portal validation gaps include mentor, race, rank, title, quests, group/alliance/league size, level, required items, and kinah.
- Instance difficulty, handler supplier, spawn engine, `InstanceHandler.onInstanceCreate`, auto-destroy scheduling, event spawns, and team fanout are still unported.

---

## Next Unit Of Work

Recommended next unit: port a narrow `PortalService.checkEnterLevel` equivalent.

Suggested shape:

1. Re-read Java:
   - `PortalService.checkEnterLevel`
   - `InstanceCooltime.getEnterMinLevelLight/Dark`
   - `InstanceCooltime.getEnterMaxLevelLight/Dark`
   - `SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_ENTER_LEVEL`
   - `SM_DIALOG_WINDOW` if an err-level dialog id is provided
2. Re-read C#:
   - `PortalEntryValidationService`
   - `Player.Level`, `Player.Race`
   - `InstanceCooltimeTable.GetEnterMinLevel` / `GetEnterMaxLevel`
   - existing `SmDialogWindow` and `SmSystemMessage` support
3. Keep it narrow:
   - accept explicit method parameters for `portalPathMinLevel` and `portalPathErrLevel` until full portal template loading exists
   - model the Java fallback where `portalPath.getMinLevel() == 0` uses `InstanceCooltime`
   - return a structured validation result and packet, not direct socket sends
   - document membership/admin bypass as caller-owned if the permission model is not ready

Useful validation commands:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PortalEntryValidationServiceTests|FullyQualifiedName~WorldMapRuntimeStateTests|FullyQualifiedName~GamePacketTests|FullyQualifiedName~StaticDataLoadingTests"
dotnet test dotnetConversion\AionServer.slnx
```

---

## Resume Checklist

- Start from branch `4.8`.
- Confirm `git status --short` is clean.
- Read `docs/csharp-port.md`, latest `docs/PHASE-6-PROGRESS.md` Sessions 524-526, `docs/Phase-6CO-Completion.md`, and this handoff.
- Continue with one focused Java-parity unit.
- Update the migration parity table before committing.
- Commit the unit, then repeat until the next handoff is needed.
