# Phase 6 Session 2315 Completion - Beshmundir Solo Entry Rejection

## Scope

Wired a concrete Java AI parity slice for Beshmundir's Walk `INSTANCE_ENTRY` solo-player rejection.

Java source reviewed:

- `game-server/data/handlers/ai/instance/beshmundirTemple/BeshmundirsWalkAI.java`
- `game-server/src/com/aionemu/gameserver/model/DialogAction.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`

Java behavior used:

- `BeshmundirsWalkAI.onDialogSelect` handles `INSTANCE_ENTRY`.
- If the player is not in a group, Java sends `SM_SYSTEM_MESSAGE.STR_MSG_ENTER_ONLY_PARTY_DON()` and returns `true`.
- The Beshmundir's Walk NPC template uses AI name `beshmundirswalk` and template id `730231`.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmDialogSelect.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`

Implemented:

- Added `CmDialogSelect.InstanceEntry = 65`.
- Added a narrow `beshmundirswalk` target guard in `HandleDialogSelectAsync`.
- Solo/non-group players selecting `INSTANCE_ENTRY` on Beshmundir's Walk now receive `SmSystemMessage.EnterOnlyPartyDon()` and the branch returns before generic portal routing.
- Added focused boundary coverage for the Java message id `1390256`.

## Validation Decision

- Changed surface: live C# dialog dispatch for one AI-specific portal/action branch.
- Specific behavior/contract: Java `BeshmundirsWalkAI.INSTANCE_ENTRY` rejects non-group players with `STR_MSG_ENTER_ONLY_PARTY_DON`.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_BeshmundirsWalkInstanceEntryRejectsSoloPlayer" --no-restore
```

Result: passed 1, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain. This filtered command supplied the compile signal for the affected C# project and tests.

- Focused Java/Maven command: skipped. No targeted Java test fixture exists for `BeshmundirsWalkAI.onDialogSelect`; Java source review was the source-of-truth evidence for this narrow AI branch.
- Repository hygiene:

```powershell
git diff --check
```

Result: passed with line-ending normalization warnings only.

- Broad-validation trigger: none. The change is a narrow live dialog branch and one client-packet constant.
- Broad .NET decision: skipped full project/solution validation. The focused boundary test covered the changed behavior and supplied the compile signal.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `ai.instance.beshmundirTemple.BeshmundirsWalkAI.onDialogSelect` action `INSTANCE_ENTRY` solo branch | `Aion.GameServer.Network.Aion.GameServerConnection.HandleDialogSelectAsync` | AI Handler / Client Packet Boundary | Partial | Focused Boundary Tested | Partial Parity | C# now rejects non-group players with `SmSystemMessage.EnterOnlyPartyDon()` for `beshmundirswalk` targets. Group leader path selection and member-in-instance entry remain unported. |
| `com.aionemu.gameserver.model.DialogAction.INSTANCE_ENTRY` | `Aion.GameServer.Network.Aion.ClientPackets.CmDialogSelect.InstanceEntry` | Constant | Complete | Boundary Tested | Verified Parity | Java value `65` matched and exercised through parsed `CM_DIALOG_SELECT`. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_MSG_ENTER_ONLY_PARTY_DON` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.EnterOnlyPartyDon` | Server Packet Factory | Complete | Boundary Tested | Verified Parity | Existing C# message id `1390256` is now exercised through the Beshmundir live dialog branch. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `ProcessPacketAsync_BeshmundirsWalkInstanceEntryRejectsSoloPlayer` | Boundary Runtime | Java source review of `BeshmundirsWalkAI.INSTANCE_ENTRY` solo branch | Live C# `CM_DIALOG_SELECT` action `65` on AI `beshmundirswalk` sends `SmSystemMessage.EnterOnlyPartyDon()` to a solo player. | Focused C# boundary execution plus Java source review. | Does not cover group leader path-selection dialog, non-leader member-in-instance movement, or difficulty select request handling. |

## Remaining Gaps

- `BeshmundirsWalkAI` group leader path-selection dialog (`SM_DIALOG_WINDOW(..., 4762)`) remains unported.
- `BeshmundirsWalkAI` non-leader member-in-instance movement remains unported.
- `BeshmundirsWalkAI` `SELECT_NONE_1` / `SELECT_NONE_2` difficulty request handling remains unported.
- Real-client or encrypted socket bytes for this branch remain unverified.

## Commit

Commit message:

```text
[Phase 6][UOW-2315] Reject solo Beshmundir walk entry
```

## Next Recommended UOW

Continue with the next smallest `BeshmundirsWalkAI` runtime branch if discovery confirms required C# dependencies are present. The safest next sequential branch is grouped-leader `INSTANCE_ENTRY`, which should send `SM_DIALOG_WINDOW(target, 4762)` instead of generic portal entry.

