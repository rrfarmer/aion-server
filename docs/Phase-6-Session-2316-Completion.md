# Phase 6 Session 2316 Completion - Beshmundir Leader Path Dialog

## Scope

Wired a concrete Java AI parity slice for Beshmundir's Walk grouped-leader `INSTANCE_ENTRY` path selection.

Java source reviewed:

- `game-server/data/handlers/ai/instance/beshmundirTemple/BeshmundirsWalkAI.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_DIALOG_WINDOW.java`

Java behavior used:

- `BeshmundirsWalkAI.onDialogSelect` handles `INSTANCE_ENTRY`.
- If the player is in a group and is the group leader, Java sends `new SM_DIALOG_WINDOW(getObjectId(), 4762)` and returns `true`.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`

Implemented:

- Extended the existing `beshmundirswalk` `INSTANCE_ENTRY` branch.
- Group leaders now receive `SmDialogWindow(targetObjectId, 4762)`.
- Non-leader behavior remains unported and documented rather than guessed.

## Validation Decision

- Changed surface: live C# dialog dispatch for one AI-specific group-leader branch.
- Specific behavior/contract: Java `BeshmundirsWalkAI.INSTANCE_ENTRY` shows path selection dialog `4762` for group leaders.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_BeshmundirsWalkInstanceEntryShowsPathDialogForGroupLeader" --no-restore
```

Result: passed 1, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain. This filtered command supplied the compile signal for the affected C# project and tests.

- Focused Java/Maven command: skipped. No targeted Java test fixture exists for `BeshmundirsWalkAI.onDialogSelect`; Java source review was the source-of-truth evidence for this narrow AI branch.
- Repository hygiene:

```powershell
git diff --check
```

Result: passed with line-ending normalization warnings only.

- Broad-validation trigger: none. The change is a narrow live dialog branch.
- Broad .NET decision: skipped full project/solution validation. The focused boundary test covered the changed behavior and supplied the compile signal.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `ai.instance.beshmundirTemple.BeshmundirsWalkAI.onDialogSelect` action `INSTANCE_ENTRY` leader branch | `Aion.GameServer.Network.Aion.GameServerConnection.HandleDialogSelectAsync` | AI Handler / Client Packet Boundary | Partial | Focused Boundary Tested | Partial Parity | C# now sends `SmDialogWindow(target, 4762)` for grouped leaders on `beshmundirswalk` targets. Non-leader member-in-instance movement and difficulty selection remain unported. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW` path selection page | `Aion.GameServer.Network.Aion.ServerPackets.SmDialogWindow` | Server Packet | Partial | Boundary Tested | Partial Parity | Existing packet is exercised through the Beshmundir leader branch with dialog page `4762`; broader packet parity not re-audited in this UOW. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `ProcessPacketAsync_BeshmundirsWalkInstanceEntryShowsPathDialogForGroupLeader` | Boundary Runtime | Java source review of `BeshmundirsWalkAI.INSTANCE_ENTRY` leader branch | Live C# `CM_DIALOG_SELECT` action `65` on AI `beshmundirswalk` sends `SmDialogWindow(target, 4762)` to the group leader. | Focused C# boundary execution plus Java source review. | Does not cover non-leader member-in-instance movement or difficulty select request handling. |

## Remaining Gaps

- `BeshmundirsWalkAI` non-leader member-in-instance movement remains unported.
- `BeshmundirsWalkAI` non-leader no-member-in-instance rejection remains unported.
- `BeshmundirsWalkAI` `SELECT_NONE_1` / `SELECT_NONE_2` difficulty request handling remains unported.
- Real-client or encrypted socket bytes for this branch remain unverified.

## Commit

Commit message:

```text
[Phase 6][UOW-2316] Show Beshmundir leader path dialog
```

## Next Recommended UOW

Continue with the next smallest `BeshmundirsWalkAI` runtime branch. The safest next sequential branch is non-leader `INSTANCE_ENTRY` rejection when no group member is already inside world `300170000`, which should send `SM_SYSTEM_MESSAGE.STR_MSG_INSTANCE_DUNGEON_CANT_ENTER_NOT_OPENED()`.

