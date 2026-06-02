# Phase 6 Session 2318 Completion - Beshmundir Difficulty Request

## Scope

Wired a concrete Java AI parity slice for Beshmundir's Walk `SELECT_NONE_1` / `SELECT_NONE_2` difficulty-selection request setup.

Java source reviewed:

- `game-server/data/handlers/ai/instance/beshmundirTemple/BeshmundirsWalkAI.java`
- `game-server/src/com/aionemu/gameserver/ai/AIActions.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_QUESTION_WINDOW.java`
- `game-server/src/com/aionemu/gameserver/model/DialogAction.java`
- `game-server/src/com/aionemu/gameserver/utils/ChatUtil.java`

Java behavior used:

- `SELECT_NONE_1` is action `4763`; `SELECT_NONE_2` is action `4848`.
- Both branches call `AIActions.addRequest(..., SM_QUESTION_WINDOW.STR_INSTANCE_DUNGEON_WITH_DIFFICULTY_ENTER_CONFIRM, request, "300170000", ChatUtil.l10n(pathL10nId))`.
- `STR_INSTANCE_DUNGEON_WITH_DIFFICULTY_ENTER_CONFIRM` is question id `902050`.
- `AIActions.addRequest` registers a response handler with range `5` and sends `SM_QUESTION_WINDOW(questionId, ai.getObjectId(), 5, params)` only if registration succeeds.
- After the request setup, Java sends `new SM_DIALOG_WINDOW(getObjectId(), 4762)`.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmDialogSelect.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmQuestionWindow.cs`
- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/QuestionResponseRegistry.cs`
- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/PendingKiskBindRequest.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`

Implemented:

- Added dialog action constants `SelectNone1 = 4763` and `SelectNone2 = 4848`.
- Added question id constant `InstanceDungeonWithDifficultyEnterConfirm = 902050`.
- Added pending request kind/payload for Beshmundir difficulty selection.
- `beshmundirswalk` `SELECT_NONE_1/2` now registers a pending request, sends `SmQuestionWindow(902050, npcObjectId, 5, "300170000", ChatUtil.L10n(902051/902052))`, then sends `SmDialogWindow(npcObjectId, 4762)`.
- Added a response branch that removes the registered pending request. The accepted portal movement remains a documented gap because Java routes through `PortalService.port(...)` team-instance movement.

## Validation Decision

- Changed surface: live C# dialog/question dispatch for one AI-specific branch, plus constants for existing packet/registry types.
- Specific behavior/contract: Java `BeshmundirsWalkAI.SELECT_NONE_1/2` registers and sends question `902050` with range `5`, world parameter `300170000`, path l10n `902051` or `902052`, then reopens dialog page `4762`.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_BeshmundirsWalkDifficultySelectionRegistersQuestionAndReopensDialog" --no-restore
```

Result: passed 2, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain. This filtered command supplied the compile signal for the affected C# project and tests.

- Focused Java/Maven command: skipped. No targeted Java test fixture exists for `BeshmundirsWalkAI.onDialogSelect`; Java source review was the source-of-truth evidence for this narrow AI branch.
- Repository hygiene:

```powershell
git diff --check
```

Result: passed with line-ending normalization warnings only.

- Broad-validation trigger: none. Packet primitive serialization, shared portal movement, persistence, and scheduler behavior were not changed.
- Broad .NET decision: skipped full project/solution validation. The focused boundary test covered the changed branch and supplied the compile signal.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `ai.instance.beshmundirTemple.BeshmundirsWalkAI.onDialogSelect` actions `SELECT_NONE_1` / `SELECT_NONE_2` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleDialogSelectAsync` | AI Handler / Client Packet Boundary | Partial | Focused Boundary Tested | Partial Parity | C# now registers and sends the Java-shaped difficulty confirmation request, then dialog `4762`. Accepted response movement through `PortalService.port(...)` remains unported. |
| `SM_QUESTION_WINDOW.STR_INSTANCE_DUNGEON_WITH_DIFFICULTY_ENTER_CONFIRM` | `SmQuestionWindow.InstanceDungeonWithDifficultyEnterConfirm` | Server Packet Constant | Complete | Boundary Tested | Partial Packet Evidence | Constant id `902050` is exercised through the Beshmundir boundary test. Broader `SmQuestionWindow` serialization was not re-audited. |
| `DialogAction.SELECT_NONE_1` / `SELECT_NONE_2` | `CmDialogSelect.SelectNone1` / `SelectNone2` | Client Packet Constants | Complete | Boundary Tested | Partial Parity | Constants `4763` and `4848` are used by the Beshmundir handler. Broader dialog registry coverage was not re-audited. |
| `AIActions.addRequest` request-registration behavior | `QuestionResponseRegistry.PutRequest` plus Beshmundir pending request payload | Request Registry | Partial | Focused Boundary Tested | Partial Parity | The branch preserves put-if-absent request setup and sends the question only on registration success. Range observer auto-deny and accepted movement are still not ported for this AI request. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `ProcessPacketAsync_BeshmundirsWalkDifficultySelectionRegistersQuestionAndReopensDialog` | Boundary Runtime | Java source review of `BeshmundirsWalkAI.SELECT_NONE_1/2`, `AIActions.addRequest`, `SM_QUESTION_WINDOW`, `DialogAction`, and `ChatUtil.l10n` | Live C# `CM_DIALOG_SELECT` on AI `beshmundirswalk` sends `SmQuestionWindow(902050, npc, 5, "300170000", l10n)` before `SmDialogWindow(npc, 4762)` and registers a pending request. | Focused C# boundary execution plus Java source review. | Does not cover accepting the question and moving through `PortalService.port(...)`. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 5
- Total artifacts ported/extended in this UOW: 4
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: unchanged, conservatively partial.

## Remaining Gaps

- Beshmundir accepted difficulty response movement remains unported.
- Beshmundir non-leader member-in-instance movement remains unported.
- C# portal team-instance movement currently returns unsupported from the generic portal validation path.
- Java range observer auto-deny for this AI request is not modeled in C#.
- Real-client or encrypted socket bytes for this branch remain unverified.

## Commit

Commit message:

```text
[Phase 6][UOW-2318] Register Beshmundir difficulty request
```

## Next Recommended UOW

Continue with team portal movement discovery before implementing Beshmundir follow-entry or accepted difficulty movement. The next safe sequential unit is to inspect and, if feasible, extend the existing C# portal team plan so a grouped Beshmundir member can reuse/register the group instance exactly like Java `PortalService.port(...)`.
