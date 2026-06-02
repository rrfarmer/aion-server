# Phase 6 Session 2316 Handoff - Beshmundir Leader Path Dialog

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2316-Completion.md`
- `docs/Phase-6-Session-2316-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

Last completed UOW: UOW-2316, Beshmundir's Walk grouped-leader `INSTANCE_ENTRY` path-selection dialog.

Completed Beshmundir slices:

- Solo/non-group `INSTANCE_ENTRY`: sends `SmSystemMessage.EnterOnlyPartyDon()`.
- Group leader `INSTANCE_ENTRY`: sends `SmDialogWindow(targetObjectId, 4762)`.

Still not proven:

- Non-leader member-in-instance movement.
- Non-leader no-member-in-instance rejection.
- Difficulty selection request handling for `SELECT_NONE_1` and `SELECT_NONE_2`.
- Real-client/encrypted socket bytes for these branches.

## Commits Made

- `c12d00c4c [Phase 6][UOW-2315] Reject solo Beshmundir walk entry`
- `[Phase 6][UOW-2316] Show Beshmundir leader path dialog`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`
- `docs/Phase-6-Session-2316-Completion.md`
- `docs/Phase-6-Session-2316-Handoff.md`

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `ai.instance.beshmundirTemple.BeshmundirsWalkAI.onDialogSelect` action `INSTANCE_ENTRY` leader branch | `Aion.GameServer.Network.Aion.GameServerConnection.HandleDialogSelectAsync` | AI Handler / Client Packet Boundary | Partial | Focused Boundary Tested | Partial Parity | C# now sends `SmDialogWindow(target, 4762)` for grouped leaders on `beshmundirswalk` targets. Other Beshmundir branches remain unported. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW` path selection page | `Aion.GameServer.Network.Aion.ServerPackets.SmDialogWindow` | Server Packet | Partial | Boundary Tested | Partial Parity | Existing packet is exercised through the Beshmundir leader branch with dialog page `4762`. |

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_BeshmundirsWalkInstanceEntryShowsPathDialogForGroupLeader" --no-restore
```

Result: passed 1, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Focused Java/Maven validation: skipped because no targeted Java fixture exists for `BeshmundirsWalkAI.onDialogSelect`. Java source review was used as source-of-truth evidence for the narrow branch.

Repository hygiene:

```powershell
git diff --check
```

Result: passed with line-ending normalization warnings only.

Broad-validation trigger: none.

Broad .NET decision: skipped full project/solution validation. Focused boundary validation covered the changed branch and supplied the compile signal.

## Next Sequential UOW

Recommended next runtime scope: `BeshmundirsWalkAI.INSTANCE_ENTRY` non-leader rejection when no group member is already inside Beshmundir Temple.

Java artifacts to inspect:

- `game-server/data/handlers/ai/instance/beshmundirTemple/BeshmundirsWalkAI.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerGroupRuntime.cs`
- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/Player.cs`

Specific behavior to prove: when the Beshmundir's Walk target uses action `INSTANCE_ENTRY`, the player is grouped but not leader, and no group member has `WorldId == 300170000`, Java sends `SM_SYSTEM_MESSAGE.STR_MSG_INSTANCE_DUNGEON_CANT_ENTER_NOT_OPENED()` and returns `true`.

Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_BeshmundirsWalkInstanceEntryRejectsNonLeaderWhenNoMemberInInstance" --no-restore
```

Focused Java/Maven command: not expected unless a new targeted Java fixture is added; Java source review is likely the practical source-of-truth evidence for this AI branch.

Broad-validation trigger: none expected.

## Safe Candidates

- Non-leader Beshmundir's Walk no-member-in-instance rejection.
- Non-leader Beshmundir's Walk member-in-instance portal entry only if C# portal difficulty/instance state dependencies are present.
- Difficulty select request handling only if `SM_QUESTION_WINDOW.STR_INSTANCE_DUNGEON_WITH_DIFFICULTY_ENTER_CONFIRM` and question-response plumbing are available.

Avoid:

- Full .NET project tests or solution builds without a documented trigger.
- Evidence/reporting-only units.
- Updating `docs/PHASE-6-PROGRESS.md`.

