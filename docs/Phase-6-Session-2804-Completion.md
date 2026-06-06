# Phase 6 Session 2804 Completion

## Unit Of Work

`[Phase 6][UOW-2804] Wire live NPC-target quest finish auto rewards`

## Runtime Progress Gate

- Deferred/live behavior advanced: live `CmDialogSelect` with a non-self NPC target can now complete supported reportable auto-reward quests instead of only supporting the self-target branch.
- Java source of truth: `CM_DIALOG_SELECT.runImpl` resolves non-self known targets, applies NPC interaction guards, dispatches to `NpcController.onDialogSelect`, then `DialogService.handleQuestDialogueOrSendNextPage` creates `QuestEnv(npc, player, questId, dialogActionId)` for quest handling that can call `QuestService.finishQuest`.
- C# runtime artifact wired: `GameServerConnection.HandleDialogSelectAsync` now resolves NPC targets from the live world, applies the known-NPC hook and available `DialogService.isInteractionAllowed` equivalent facts, and reuses the live quest finish reward/state/persistence/packet pipeline with the target NPC template.
- Client-visible/state/persistence effect changed: reporting a supported quest to a live NPC target can apply rewards, mutate the player quest state to `COMPLETE`, persist through the existing quest table shape when available, and send the real stat/system/quest packets.
- Why this is not preview-only/test-only/documentation-only: the live socket handler routes a real dialog packet through world target resolution and mutates player XP/quest state while sending server packets.

## Java Parity Notes

- Java does not call `QuestService.finishQuest` directly from the NPC-target packet branch. It reaches quest finish through NPC controller, dialog service, and quest handler dispatch. Because C# dynamic quest handlers are not fully live yet, this UOW wires only the reportable auto-reward finish case that the C# runtime already supports.
- The existing self-target guard remains unchanged by default. The guard can allow non-self targets only when the live NPC-target branch has already resolved a world NPC and allowed interaction.
- C# uses the existing `_isKnownNpc` hook when available. If the hook is absent, the runtime world object lookup is the current known target evidence.

## C# Artifacts Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/QuestDialogAutoRewardGuardPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/QuestFinishSocketGuardedInputAssemblyPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestDialogAutoRewardGuardPlanServiceTests.cs`

## Validation Decision

- Changed surface: live connection dispatch and quest-finish side effect.
- Specific behavior/contract: Java NPC-target quest dialog handling can complete a reportable auto-reward quest after target resolution and interaction guards.
- Focused C# command: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestDialogAutoRewardGuardPlanServiceTests|FullyQualifiedName~QuestDialogNpcTargetBranchPlanServiceTests|FullyQualifiedName~QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests"`
- Focused Java/Maven command: not run; no narrow Java fixture exists for `CM_DIALOG_SELECT` NPC-target quest reporting.
- Broad-validation trigger: live connection dispatch and quest-finish side effect.
- Broad .NET decision: skipped after focused validation; the filtered command compiled the affected project and exercised the edited live socket boundary plus adjacent guard and NPC-target branch models.
- Why this scope is sufficient: the new boundary test sends `CmDialogSelect` with a real NPC object id, verifies the live handler applies XP, sends the success system message, sends the quest update packet, and mutates the quest to `COMPLETE`.

## Tests Run

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestDialogAutoRewardGuardPlanServiceTests|FullyQualifiedName~QuestDialogNpcTargetBranchPlanServiceTests|FullyQualifiedName~QuestDialogNpcTargetBranchInputAssemblyPlanServiceTests"
```

Result: Passed, 69 total, 0 failed, 0 skipped.

Java/Maven: not run; no narrow Java fixture exists for the socket-side NPC-target quest reporting path.

## Test Evidence

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleDialogSelectAsync_NpcTargetReportableAutoRewardQuestAppliesXpAndCompletesQuest` | Regression | Java source review: `CM_DIALOG_SELECT.runImpl`, `NpcController.onDialogSelect`, `DialogService.handleQuestDialogueOrSendNextPage`, `QuestService.finishQuest` | Live NPC-target dialog select completes a supported reportable quest and sends XP/system/quest packets | Filtered C# boundary test with live world NPC target, player mutation, and packet assertions | Does not execute arbitrary Java quest handler bodies |
| `CreatePlanFromTemplateSummary_AllowsNonSelfTargetWhenNpcBranchOptedIn` | Unit | Java source review: self target branch vs NPC target branch | Existing guard remains self-only unless the live NPC branch explicitly opts in | Focused C# guard test | Does not prove runtime mutation by itself; runtime proof comes from the boundary test |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT.runImpl` NPC-target branch | `Aion.GameServer.Network.Aion.GameServerConnection.HandleDialogSelectAsync` | Client packet handler | Partial | Regression Tested | Partial Parity | Supports live NPC-target reportable auto-reward quest finish after world NPC resolution and interaction guards; full controller/AI/QuestEngine dispatch remains incomplete. |
| `com.aionemu.gameserver.controllers.NpcController.onDialogSelect` | `Aion.GameServer.Network.Aion.GameServerConnection.TryHandleNpcTargetQuestFinishAutoRewardAsync` | Runtime dispatch slice | Partial | Regression Tested | Partial Parity | C# directly wires the supported quest-finish slice instead of invoking full NPC AI and dynamic quest handler dispatch. |
| `com.aionemu.gameserver.services.DialogService.isInteractionAllowed` | `Aion.GameServer.Services.NpcDialogInteractionAllowedPlanService` consumed by `GameServerConnection` | Runtime guard | Partial | Regression Tested | Partial Parity | Uses available subdialog, inventory, skill, abyss rank, legion presence, and level facts; summon owner, siege/zone, live ranking, and dominion calculation details remain incomplete. |
| `com.aionemu.gameserver.services.QuestService.finishQuest` | `Aion.GameServer.Network.Aion.GameServerConnection.TryHandleQuestFinishAutoRewardAsync` | Runtime quest mutation and packet send | Partial | Regression Tested | Partial Parity | Reused for NPC-target auto-reward completion with target NPC template passed into reward projection lookup. |

## Parity Status

Partial parity improved for quest dialog completion. Supported reportable auto-reward quest finish behavior now works for both self/player target and live NPC target packets when the C# reward/state path supports the quest.

## Known Gaps

- Full Java NPC controller, AI `onDialogSelect`, dynamic `QuestEngine.onDialog`, and arbitrary quest handler bodies remain incomplete.
- Live quest finish still does not support bonus rewards, challenge task completion, arbitrary quest completion callback handler bodies, mentor NPC faction title/flag side effects, or broad nearby quest refresh fanout.
- Direct reward persistence is not newly wired in quest finish for AP/DP/GP/cube/warehouse expansions. Existing later player-save behavior may persist mutated runtime state, but this UOW did not add direct quest-finish persistence calls.
- The NPC-target interaction guard uses currently modeled C# facts; summon-owner, siege/zone, live abyss ranking, and dominion calculation restrictions still need fuller runtime parity.
