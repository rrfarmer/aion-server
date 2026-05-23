# Phase 6CT Completion Handoff

**Created**: May 23, 2026  
**Status**: Phase 6 remains in progress; this handoff follows 6CS and covers Sessions 535-537.  
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, runtime side effects, static-data semantics, persistence behavior, scheduling, and combat math.  
**Workflow rule**: Do one focused unit of work, validate it, update the migration parity table, commit it, then repeat for as long as useful work remains.  
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.  
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx` passes with 1126 tests.

---

## Recent Work Completed

- Added `PortalEntryValidationService.ValidatePortalEntryPlan`, a partial Java-shaped plan helper for `PortalService.port`'s early solo/open-world path.
- The helper resolves `PortalPathSummary.LocId` through `PortalLocTable`, applies mentor/race/rank/title before registered-instance/cooldown, applies level only when not reentering, carries the resolved location, optional registered instance, and reenter flag, and explicitly marks team-sized portals unsupported.
- Added `PortalEntryPlanAction.SameInstanceTeleport`, representing Java's `mapId == player.getWorldId()` branch as a plan action with resolved target coordinates/heading, without executing teleport side effects.
- Added structural nested portal requirements: `PortalQuestRequirementSummary` and `PortalItemRequirementSummary`.
- Refactored static-data portal-path loading so `quest_req` and `item_req` children attach to the correct `PortalPathSummary`.
- Updated `docs/PHASE-6-PROGRESS.md` through Session 537 with migration parity tables, risks, metrics, validation, and next recommended work.

---

## Commits In This Handoff

- `e95bf1235` - `Add portal entry plan validation`
- `652c6942c` - `Add same-instance portal plan action`
- `01e119960` - `Load portal path requirements`

---

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|
| `PortalService.port` early guard/cooldown order | `PortalEntryValidationService.ValidatePortalEntryPlan` | Partial | Unit Tested | Partial Parity | Location lookup, mentor/race/rank/title ordering, solo registered-instance/cooldown handling, reenter skip, and level ordering are represented. Quest, player-size, item/kinah, allocation, transfer, and packet dispatch remain missing. |
| `PortalService.port` same-map branch | `PortalEntryPlanAction.SameInstanceTeleport` | Partial | Unit Tested | Partial Parity | C# now marks the same-instance teleport branch and carries `PortalLocSummary`. Actual `TeleportService.teleportTo` state mutation and fanout remain unported. |
| `PortalPath.getQuestReq` | `PortalPathSummary.QuestRequirements` | Partial | Unit Tested / Regression Tested | Partial Parity | Nested quest requirements are loaded structurally. `PortalService.checkQuests` is not implemented. |
| `PortalPath.getItemReq` | `PortalPathSummary.ItemRequirements` | Partial | Unit Tested / Regression Tested | Partial Parity | Nested item requirements are loaded structurally. `checkAndRemoveRequiredItems`, item deletion, and kinah handling are not implemented. |
| `QuestReq` / `ItemReq` | `PortalQuestRequirementSummary` / `PortalItemRequirementSummary` | Partial | Unit Tested / Regression Tested | Partial Parity | Scalar XML fields are carried as immutable snapshots. Java setter/mutable JAXB behavior is not represented. |

Metrics from this handoff window:

- Total focused sessions covered: 3
- Total commits covered: 3
- Current full validation baseline: 1126 tests passing
- Total artifacts with verified parity newly promoted in this window: 0
- Total blocked artifacts: production portal handler wiring, quest validation, item validation/removal, kinah consumption, group/alliance/league checks, actual teleport execution, instance allocation/transfer, and live-client validation
- Estimated overall migration completion: Phase 6 remains about 58% complete; portal data and early plan behavior are improving, but end-to-end portal use is still incomplete.

---

## Important Limits

- No production packet handler calls `ValidatePortalEntryPlan`.
- `PortalEntryPlanResult.FailurePacket` returns packet objects; it does not send them through socket/connection infrastructure.
- `SameInstanceTeleport` is only an action marker. It does not call `TeleportService.teleportTo`, mutate `Player.Position`, update known lists, or emit teleport packets.
- Java `checkAndRemoveRequiredItems` runs before same-map teleport; C# now has structural item requirements but does not enforce or remove them.
- Java `checkQuests` is still missing; C# now has structural quest requirements but does not inspect player quest state.
- Group/alliance/league portals are explicitly unsupported in the plan helper rather than partially emulated.
- Admin/membership bypasses are still explicit parameters, not live permission/config lookups.
- Siege ownership remains caller-supplied; `PortalPathSummary.SiegeId` is loaded but not resolved through a C# `SiegeService`.
- Duplicate portal key behavior, Java JAXB runtime behavior, packet dispatch ordering, date/time behavior beyond cooldown timestamps, precision/rounding, and live-client behavior remain unverified.

---

## Next Unit Of Work

Recommended next unit: add a narrow `ValidateQuestRequirements` helper for `PortalPathSummary.QuestRequirements`.

Suggested scope:

1. Re-read Java:
   - `PortalService.checkQuests`
   - `QuestReq`
   - `QuestState` and `QuestStatus` fields used by the check
   - current C# player quest-state model and tests
2. Add a helper on `PortalEntryValidationService` that:
   - allows empty requirement lists
   - supports explicit bypass matching `MembershipConfig.INSTANCES_QUEST_REQ`
   - checks each loaded `PortalQuestRequirementSummary`
   - returns a `SM_DIALOG_WINDOW(npcObjectId, DialogPage.NO_RIGHT.id())` style failure if current C# quest state can support the Java condition
3. Keep scope narrow:
   - Do not implement quest engine progression.
   - Do not wire the helper into production packet handling until its semantics are clear.
   - If C# quest state lacks Java fields needed for exact parity, document the exact gap and mark status as Partial / Needs Verification.
4. Tests:
   - no requirements allows
   - bypass allows
   - missing/incomplete quest rejects
   - completed or sufficient-step quest allows, based strictly on Java `checkQuests`

Useful validation commands:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PortalEntryValidationServiceTests|FullyQualifiedName~StaticDataLoadingTests"
dotnet test dotnetConversion\AionServer.slnx
```

---

## Resume Checklist

- Start from branch `4.8`.
- Confirm `git status --short` is clean.
- Read `docs/csharp-port.md`, latest `docs/PHASE-6-PROGRESS.md` Sessions 535-537, `docs/Phase-6CS-Completion.md`, and this handoff.
- Continue with one focused Java-parity unit.
- Update the migration parity table before committing.
- Commit the unit, then repeat until the next handoff is needed.
