# Phase 6 Session 2318 Handoff - Beshmundir Difficulty Request

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2318-Completion.md`
- `docs/Phase-6-Session-2318-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

Last completed UOW: UOW-2318, Beshmundir's Walk difficulty-selection request setup.

Completed Beshmundir slices:

- Solo/non-group `INSTANCE_ENTRY`: sends `SmSystemMessage.EnterOnlyPartyDon()`.
- Group leader `INSTANCE_ENTRY`: sends `SmDialogWindow(targetObjectId, 4762)`.
- Grouped non-leader `INSTANCE_ENTRY` when no group member is in world `300170000`: sends `SmSystemMessage.InstanceDungeonCantEnterNotOpened()`.
- `SELECT_NONE_1` / `SELECT_NONE_2`: registers question `902050`, sends `SmQuestionWindow(..., "300170000", ChatUtil.L10n(902051/902052))`, then sends dialog `4762`.

Still not proven:

- Accepting Beshmundir difficulty question and moving through `PortalService.port(...)`.
- Non-leader member-in-instance movement.
- Java range observer auto-deny behavior for the Beshmundir question.
- Real-client/encrypted socket bytes for these branches.

## Commits Made

- `c12d00c4c [Phase 6][UOW-2315] Reject solo Beshmundir walk entry`
- `10b528b55 [Phase 6][UOW-2316] Show Beshmundir leader path dialog`
- `ff1354d09 [Phase 6][UOW-2317] Reject Beshmundir non-leader closed entry`
- `[Phase 6][UOW-2318] Register Beshmundir difficulty request`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmDialogSelect.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmQuestionWindow.cs`
- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/QuestionResponseRegistry.cs`
- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/PendingKiskBindRequest.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`
- `docs/Phase-6-Session-2318-Completion.md`
- `docs/Phase-6-Session-2318-Handoff.md`

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `ai.instance.beshmundirTemple.BeshmundirsWalkAI.onDialogSelect` actions `SELECT_NONE_1` / `SELECT_NONE_2` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleDialogSelectAsync` | AI Handler / Client Packet Boundary | Partial | Focused Boundary Tested | Partial Parity | C# now registers and sends the Java-shaped difficulty confirmation request, then dialog `4762`. Accepted movement remains unported. |
| `SM_QUESTION_WINDOW.STR_INSTANCE_DUNGEON_WITH_DIFFICULTY_ENTER_CONFIRM` | `SmQuestionWindow.InstanceDungeonWithDifficultyEnterConfirm` | Server Packet Constant | Complete | Boundary Tested | Partial Packet Evidence | Constant id `902050` is exercised through the Beshmundir boundary test. |
| `DialogAction.SELECT_NONE_1` / `SELECT_NONE_2` | `CmDialogSelect.SelectNone1` / `SelectNone2` | Client Packet Constants | Complete | Boundary Tested | Partial Parity | Constants `4763` and `4848` are used by the Beshmundir handler. |
| `AIActions.addRequest` request-registration behavior | `QuestionResponseRegistry.PutRequest` plus Beshmundir pending request payload | Request Registry | Partial | Focused Boundary Tested | Partial Parity | The branch preserves put-if-absent setup and sends the question only on registration success. Range observer auto-deny and accepted movement are not ported for this AI request. |

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_BeshmundirsWalkDifficultySelectionRegistersQuestionAndReopensDialog" --no-restore
```

Result: passed 2, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Focused Java/Maven validation: skipped because no targeted Java fixture exists for `BeshmundirsWalkAI.onDialogSelect`. Java source review was used as source-of-truth evidence for the narrow branch.

Repository hygiene:

```powershell
git diff --check
```

Result: passed with line-ending normalization warnings only.

Broad-validation trigger: none.

Broad .NET decision: skipped full project/solution validation. Focused boundary validation covered the changed branch and supplied the compile signal.

## Next Sequential UOW

Recommended next runtime scope: team-instance portal movement needed by Beshmundir follow-entry and difficulty acceptance.

Java artifacts to inspect:

- `game-server/src/com/aionemu/gameserver/services/teleport/PortalService.java`
- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`
- `game-server/src/com/aionemu/gameserver/world/WorldMapInstance.java`
- `game-server/data/handlers/ai/instance/beshmundirTemple/BeshmundirsWalkAI.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Services/PortalEntryValidationService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerTeleportService.cs`
- `dotnetConversion/src/Aion.GameServer/World/WorldMapInstanceRuntimeState.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInstanceCooldownTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`

Specific behavior to prove: Java `PortalService.port(...)` for group-sized instances resolves or creates the registered group instance by team id, registers the team/player, transfers with `TeleportAnimation.FADE_OUT_BEAM`, and applies cooldown after teleport unless reentering.

Focused C# command should be chosen after discovery. If the change stays in a single service, start with the edited service test class. If live `GameServerConnection` portal transfer is touched, use the smallest filtered boundary test that exercises that branch.

Focused Java/Maven command: not expected unless a targeted Java fixture is added; Java source review is likely the practical source-of-truth evidence.

Broad-validation trigger: possible if shared portal movement, instance allocation, or teleport services are changed. Name the trigger before any unfiltered project/solution validation.

## Safe Candidates

- Extend existing portal team plan support without wiring live movement, if runtime movement dependencies are still too broad.
- Implement Beshmundir non-leader member-in-instance movement only after group portal transfer support is objectively available.
- Implement Beshmundir difficulty question acceptance only after group portal transfer support is objectively available.

Avoid:

- Full .NET project tests or solution builds without a documented trigger.
- Beshmundir-only teleport shortcuts that bypass Java `PortalService.port(...)`.
- Evidence/reporting-only units.
- Updating `docs/PHASE-6-PROGRESS.md`.
