# Phase 6YC Completion - UOW-1141 Player Talk Radius Fact

Date: May 26, 2026

## Unit Of Work

UOW-1141: `[Phase 6][UOW-1141] Add player talk range radius fact`

## Summary

UOW-1141 audits Java player bound-radius sourcing and wires the player-side radius into the shared NPC talk-range helper added in UOW-1140. Java sets player front/side bound radius to `0.25f`; C# now carries that as an explicit player fact and the existing NPC dialog/portal callers pass it into range evaluation.

This remains staged dialog geometry work. It does not call production `GameServerConnection`, live `DataManager`, NPC AI, packet sends, packet serialization, Java runtime comparison, or broader Java `PositionUtil` helpers.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/Player.cs`
- `dotnetConversion/src/Aion.GameServer/Services/NpcDialogRequestService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PortalEntryInteractionService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NpcDialogRequestServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6YC-Completion.md`

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PositionUtilServiceTests\|NpcDialogRequestServiceTests\|PortalEntryInteractionServiceTests" --nologo` | Passed: 16 tests. |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --nologo` | Passed: 2,143 tests. |
| `dotnet test dotnetConversion/AionServer.slnx --nologo` | Passed: 2,350 tests. |

## Migration Parity Table - UOW-1141

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.account.PlayerAccountData.updateBoundingRadius` | `Aion.GameServer.Model.GameObjects.Player.BoundRadius` | Model Fact | Partial | Unit Tested through caller | Partial Parity | C# now carries the Java player max front/side radius value `0.25f` used by `PositionUtil`. Java upper bound height from appearance is not modeled for this path because talk-range uses max front/side only. |
| `com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData.getBoundRadius` | `Player.BoundRadius` | Model Fact | Partial | Unit Tested through caller | Partial Parity | Player-side range expansion is now available to dialog callers. Full Java `BoundRadius` front/side/upper object shape is not ported for players. |
| `com.aionemu.gameserver.utils.PositionUtil.isInTalkRange(Creature, Npc)` | `PositionUtilService.IsInNpcTalkRange` plus player caller inputs | Utility Method | Partial | Unit Tested | Partial Parity | Player-to-NPC talk range now includes both NPC and player radii. Java runtime comparison was not executed; non-player creature callers remain future work. |
| `com.aionemu.gameserver.controllers.NpcController.onDialogRequest` | `Aion.GameServer.Services.NpcDialogRequestService` | Service / Dialog Request | Partial | Unit Tested | Partial Parity | Dialog request range now includes Java player radius. Production controller path, known-list runtime, and packet send integration remain partial. |
| `com.aionemu.gameserver.controllers.NpcController.onDialogSelect` / portal AI path | `Aion.GameServer.Services.PortalEntryInteractionService` | Service / Portal Dialog | Partial | Existing Unit Coverage | Partial Parity | Portal dialog range now includes Java player radius before static portal handling. Production NPC AI route and live packet ordering beyond existing portal tests remain unverified. |

## Tests Added Or Updated

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `NpcDialogRequestServiceTests.RequestDialog_UsesPlayerBoundRadiusForJavaTalkRange` | Player-to-NPC dialog at a boundary distance succeeds only when the player `0.25f` radius is included. | Source-reviewed Java `PlayerAccountData.updateBoundingRadius` and `PositionUtil.isInTalkRange`. |

## Remaining Risks

- C# does not yet model full player `BoundRadius` front/side/upper as a value object; only the talk-range-relevant max front/side value is carried.
- House-object talk range and non-player creature radius sourcing remain unwired.
- Java runtime comparison, broader `PositionUtil` methods, production dialog routing through `GameServerConnection`, NPC AI, packet sends, threading, and live `DataManager` integration remain disabled/unverified.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 partial player radius fact plus 2 caller input updates
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 5 blocked/partial categories: full player radius object shape, house-object talk range, non-player creature radius sourcing, production dialog routing, and Java runtime comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete; this unit closes the player-side radius gap for staged NPC talk-range callers.

## Next Recommended Unit Of Work

Audit and model house-object talk range or continue the trade-list runtime-readiness audit before attempting production `GameServerConnection` dialog routing.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | House-object talk-range audit | Java house-object model/controllers and current C# housing surfaces; read-only first | Medium | Independent if it avoids `PositionUtilService.cs` writes until a concrete helper slice is chosen. |
| B | Trade-list runtime-readiness audit | Docs/tests only unless routing is clearly ready | Medium | Can proceed independently of radius work. |
| C | Non-player creature radius audit | Java pet/summon/static-object templates and current C# models; read-only first | Medium | Needed for future non-player callers, not current player-to-NPC dialog request/select. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Agent A | Read-only house-object talk-range audit | Read-only | All writes |
| Agent B | Read-only trade-list runtime-readiness audit | Read-only | All writes |

Keep implementation sequential if changing `PositionUtilService.cs`, player model surfaces, or shared dialog caller files.

## Do Not Parallelize

- `PositionUtilService.cs`: shared geometry utility.
- `Player.cs`: central model fact surface.
- `NpcDialogRequestService.cs`, `PortalEntryInteractionService.cs`, and future `GameServerConnection.cs` dialog routing: one owner at a time.
- Phase 6 progress/handoff docs: orchestrator-owned.
