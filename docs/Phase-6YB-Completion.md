# Phase 6YB Completion - UOW-1140 NPC Talk Range Helper

Date: May 26, 2026

## Unit Of Work

UOW-1140: `[Phase 6][UOW-1140] Centralize NPC talk range geometry`

## Summary

UOW-1140 audits Java `PositionUtil.isInTalkRange` and centralizes the duplicated C# NPC dialog range checks behind a shared helper. The helper models Java's world/instance guard, `talkDistance + 1`, object-radius expansion, and strict squared-distance boundary.

This remains non-live geometry/caller consolidation work. It does not call production `GameServerConnection`, live `DataManager`, NPC AI, packet sends, packet serialization, Java runtime comparison, or broader Java `PositionUtil` helpers.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/PositionUtilService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/NpcDialogRequestService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PortalEntryInteractionService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PositionUtilServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6YB-Completion.md`

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PositionUtilServiceTests\|NpcDialogRequestServiceTests\|PortalEntryInteractionServiceTests\|NpcDialogControllerDispatchPlanServiceTests\|QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests" --nologo` | Passed: 40 tests. |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` | Passed: 2,142 tests. |
| `dotnet test dotnetConversion/AionServer.slnx --nologo` | Passed: 2,349 tests. |

## Migration Parity Table - UOW-1140

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.utils.PositionUtil` | `Aion.GameServer.Services.PositionUtilService` | Utility | Partial | Unit Tested | Partial Parity | NPC talk-range geometry now mirrors Java's world/instance guard, `talkDistance + 1`, object-radius expansion, and strict squared comparison. C# does not yet source a live player/creature bound radius, so caller behavior still defaults that side to `0`. House-object overload and broader `PositionUtil` methods remain unported. |
| `com.aionemu.gameserver.utils.PositionUtil.isInTalkRange(Creature, Npc)` | `PositionUtilService.IsInNpcTalkRange` | Utility Method | Partial | Unit Tested | Partial Parity | Non-live helper covers deterministic formula and radius expansion. Java runtime comparison was not executed; floating-point behavior is source-reviewed C# `float` arithmetic matching Java `float`-style inputs. |
| `com.aionemu.gameserver.controllers.NpcController.onDialogRequest` | `Aion.GameServer.Services.NpcDialogRequestService` | Service / Dialog Request | Partial | Unit Tested | Partial Parity | Existing dialog-request caller now uses the shared helper. Response packet choice remains tested; live controller dispatch and player object-template radius sourcing remain incomplete. |
| `com.aionemu.gameserver.controllers.NpcController.onDialogSelect` / portal AI path | `Aion.GameServer.Services.PortalEntryInteractionService` | Service / Portal Dialog | Partial | Unit Tested | Partial Parity | Existing portal dialog caller now uses the shared helper before static portal handling. Production NPC AI route and live packet ordering beyond existing portal tests remain unverified. |
| `com.aionemu.gameserver.utils.PositionUtil.isInTalkRange(Creature, HouseObject<?>)` | No dedicated C# house-object caller yet; `PositionUtilService.IsInObjectTalkRange` can model the formula with supplied facts | Utility Method | Partial | Unit Tested as generic helper | Needs Verification | Generic helper accepts target talking distance and radii, but no live C# house-object model/caller is wired. |

## Tests Added Or Updated

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `PositionUtilServiceTests.IsInNpcTalkRange_UsesJavaStrictSquaredRange` | Distances strictly below the calculated talk range pass, and exactly-on-boundary fails. | Source-reviewed Java strict `< range * range`. |
| `PositionUtilServiceTests.IsInNpcTalkRange_RejectsDifferentWorldOrInstance` | Different world or instance returns false before distance math. | Source-reviewed Java guard. |
| `PositionUtilServiceTests.IsInNpcTalkRange_AddsCreatureAndNpcBoundRadiiLikeJavaNonCenterRange` | Optional creature radius expands the accepted range together with NPC radius. | Source-reviewed Java `centerToCenter=false` radius expansion. |

## Remaining Risks

- Player/creature bound-radius sourcing is not modeled in the current production callers, so they still pass `0` for the creature radius.
- House-object talk range has only generic helper support, not a concrete C# house-object model or caller.
- Java runtime comparison, broader `PositionUtil` methods, production dialog routing through `GameServerConnection`, NPC AI, packet sends, threading, and live `DataManager` integration remain disabled/unverified.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 partial talk-range utility helper plus 2 caller consolidations
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 5 blocked/partial categories: live player/creature radius sourcing, house-object model/caller, broader `PositionUtil` geometry, production dialog routing, and Java runtime comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete; this unit centralizes the NPC talk-range formula and documents the remaining live-radius gap before production dialog routing.

## Next Recommended Unit Of Work

Add a live-radius fact bridge for player/creature object-template radii or, if the required player template surface is still missing, keep it explicit and move to a read-only trade-list runtime-readiness audit before wiring production dialog routing.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Player/creature radius sourcing audit | `Player`, static player template/model surfaces; read-only first | Medium | Needed before NPC talk range can include the creature side in production callers. |
| B | House-object talk-range model audit | Java house-object model/controllers and current C# housing surfaces; read-only first | Medium | Independent if it avoids `PositionUtilService.cs` writes. |
| C | Trade-list runtime-readiness audit | Docs/tests only unless routing is clearly ready | Medium | Can proceed independently of range helper implementation. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Agent A | Read-only player/creature radius sourcing audit | Read-only | All writes |
| Agent B | Read-only house-object talk-range audit | Read-only | All writes |

Keep implementation sequential if changing `PositionUtilService.cs`, player model surfaces, or shared dialog caller files.

## Do Not Parallelize

- `PositionUtilService.cs`: shared geometry utility.
- `NpcDialogRequestService.cs`, `PortalEntryInteractionService.cs`, and future `GameServerConnection.cs` dialog routing: one owner at a time.
- Player model/template radius surfaces until ownership and dependencies are clear.
- Phase 6 progress/handoff docs: orchestrator-owned.
