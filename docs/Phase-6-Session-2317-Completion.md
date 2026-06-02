# Phase 6 Session 2317 Completion - Beshmundir Non-Leader Closed Entry

## Scope

Wired a concrete Java AI parity slice for Beshmundir's Walk grouped non-leader `INSTANCE_ENTRY` rejection when the instance is not already opened by a group member.

Java source reviewed:

- `game-server/data/handlers/ai/instance/beshmundirTemple/BeshmundirsWalkAI.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`

Java behavior used:

- `BeshmundirsWalkAI.onDialogSelect` handles `INSTANCE_ENTRY`.
- For grouped non-leaders, Java calls `isAGroupMemberInInstance(player)`.
- If no group member has `WorldId == 300170000`, Java sends `SM_SYSTEM_MESSAGE.STR_MSG_INSTANCE_DUNGEON_CANT_ENTER_NOT_OPENED()` and returns `true`.
- The Java system message id is `1400361`.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`

Implemented:

- Added `SmSystemMessage.InstanceDungeonCantEnterNotOpened()` for Java message `1400361`.
- Extended the existing `beshmundirswalk` `INSTANCE_ENTRY` branch.
- Grouped non-leaders now check group runtime members for world `300170000`.
- If no group member is inside Beshmundir Temple, C# sends `SmSystemMessage.InstanceDungeonCantEnterNotOpened()` and returns before generic dialog routing.
- Member-in-instance portal movement remains unported and documented rather than guessed.

## Validation Decision

- Changed surface: live C# dialog dispatch for one AI-specific grouped non-leader rejection branch.
- Specific behavior/contract: Java `BeshmundirsWalkAI.INSTANCE_ENTRY` sends `STR_MSG_INSTANCE_DUNGEON_CANT_ENTER_NOT_OPENED` when a grouped non-leader tries to enter before any group member has opened the instance.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_BeshmundirsWalkInstanceEntryRejectsNonLeaderWhenInstanceNotOpened" --no-restore
```

Result: passed 1, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain. This filtered command supplied the compile signal for the affected C# project and tests.

- Focused Java/Maven command: skipped. No targeted Java test fixture exists for `BeshmundirsWalkAI.onDialogSelect`; Java source review was the source-of-truth evidence for this narrow AI branch.
- Broad-validation trigger: none. The change is a narrow live dialog branch.
- Broad .NET decision: skipped full project/solution validation. The focused boundary test covered the changed behavior and supplied the compile signal.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `ai.instance.beshmundirTemple.BeshmundirsWalkAI.onDialogSelect` action `INSTANCE_ENTRY` non-leader closed-instance branch | `Aion.GameServer.Network.Aion.GameServerConnection.HandleDialogSelectAsync` | AI Handler / Client Packet Boundary | Partial | Focused Boundary Tested | Partial Parity | C# now rejects grouped non-leaders with message `1400361` when no group member is in world `300170000`. Member-in-instance movement and difficulty selection remain unported. |
| `SM_SYSTEM_MESSAGE.STR_MSG_INSTANCE_DUNGEON_CANT_ENTER_NOT_OPENED` | `SmSystemMessage.InstanceDungeonCantEnterNotOpened()` | Server Packet | Ported Helper | Boundary Tested | Partial Packet Evidence | Helper emits message id `1400361` and is exercised through the Beshmundir boundary test. Broader packet serialization was not re-audited in this UOW. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `ProcessPacketAsync_BeshmundirsWalkInstanceEntryRejectsNonLeaderWhenInstanceNotOpened` | Boundary Runtime | Java source review of `BeshmundirsWalkAI.INSTANCE_ENTRY` non-leader closed-instance branch and `SM_SYSTEM_MESSAGE` id `1400361` | Live C# `CM_DIALOG_SELECT` action `65` on AI `beshmundirswalk` sends system message `1400361` to a grouped non-leader when no group member is in world `300170000`. | Focused C# boundary execution plus Java source review. | Does not cover member-in-instance portal movement or difficulty select request handling. |

## Remaining Gaps

- `BeshmundirsWalkAI` non-leader member-in-instance movement remains unported.
- `BeshmundirsWalkAI` `SELECT_NONE_1` / `SELECT_NONE_2` difficulty request handling remains unported.
- Real-client or encrypted socket bytes for this branch remain unverified.

## Commit

Commit message:

```text
[Phase 6][UOW-2317] Reject Beshmundir non-leader closed entry
```

## Next Recommended UOW

Continue with the next smallest `BeshmundirsWalkAI` runtime branch. The safest next sequential branch is non-leader `INSTANCE_ENTRY` follow-entry when at least one group member is already inside world `300170000`; Java calls `moveToInstance(player, (byte) 0)`, which delegates to `PortalService.port(portalPath, player, getOwner(), difficult)`.
