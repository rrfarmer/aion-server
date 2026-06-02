# Phase 6 Session 2315 Handoff - Beshmundir Solo Entry Rejection

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2315-Completion.md`
- `docs/Phase-6-Session-2315-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

Last completed UOW: UOW-2315, Beshmundir's Walk solo `INSTANCE_ENTRY` rejection.

Completed in UOW-2315:

- Added `CmDialogSelect.InstanceEntry = 65`.
- Added a narrow AI-name guard for `beshmundirswalk` in `GameServerConnection.HandleDialogSelectAsync`.
- Solo/non-group players selecting Beshmundir's Walk `INSTANCE_ENTRY` now receive `SmSystemMessage.EnterOnlyPartyDon()` and do not fall into generic portal handling.

Still not proven:

- Full `BeshmundirsWalkAI` parity.
- Group leader path-selection dialog.
- Non-leader member-in-instance movement.
- Difficulty selection request handling.
- Real-client/encrypted socket bytes for this branch.

## Commits Made

- `e930a9847 [Phase 6][UOW-2314] Dispatch find-group joined-team cleanup`
- `[Phase 6][UOW-2315] Reject solo Beshmundir walk entry`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmDialogSelect.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`
- `docs/Phase-6-Session-2315-Completion.md`
- `docs/Phase-6-Session-2315-Handoff.md`

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `ai.instance.beshmundirTemple.BeshmundirsWalkAI.onDialogSelect` action `INSTANCE_ENTRY` solo branch | `Aion.GameServer.Network.Aion.GameServerConnection.HandleDialogSelectAsync` | AI Handler / Client Packet Boundary | Partial | Focused Boundary Tested | Partial Parity | C# now rejects non-group players with `SmSystemMessage.EnterOnlyPartyDon()` for `beshmundirswalk` targets. Other branches remain unported. |
| `com.aionemu.gameserver.model.DialogAction.INSTANCE_ENTRY` | `Aion.GameServer.Network.Aion.ClientPackets.CmDialogSelect.InstanceEntry` | Constant | Complete | Boundary Tested | Verified Parity | Java value `65` matched and exercised through parsed `CM_DIALOG_SELECT`. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_MSG_ENTER_ONLY_PARTY_DON` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.EnterOnlyPartyDon` | Server Packet Factory | Complete | Boundary Tested | Verified Parity | Message id `1390256` exercised through the Beshmundir live dialog branch. |

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_BeshmundirsWalkInstanceEntryRejectsSoloPlayer" --no-restore
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

Recommended next runtime scope: `BeshmundirsWalkAI.INSTANCE_ENTRY` grouped-leader path selection.

Java artifacts to inspect:

- `game-server/data/handlers/ai/instance/beshmundirTemple/BeshmundirsWalkAI.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_DIALOG_WINDOW.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmDialogSelect.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerGroupRuntime.cs`

Specific behavior to prove: when the Beshmundir's Walk target uses action `INSTANCE_ENTRY` and the player is the group leader, Java sends `SM_DIALOG_WINDOW(targetObjectId, 4762)` and returns `true`.

Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_BeshmundirsWalkInstanceEntryShowsPathDialogForGroupLeader" --no-restore
```

Focused Java/Maven command: not expected unless a new targeted Java fixture is added; Java source review is likely the practical source-of-truth evidence for this AI branch.

Broad-validation trigger: none expected.

## Safe Candidates

- Grouped-leader Beshmundir's Walk `INSTANCE_ENTRY` path dialog.
- Non-leader Beshmundir's Walk rejection when no group member is in instance, if current C# team/world state can prove it narrowly.
- Continue Find Group/portal runtime gaps only when they are production behavior, not evidence-only coverage.

Avoid:

- Full .NET project tests or solution builds without a documented trigger.
- More evidence propagation/reporting-only units.
- Updating `docs/PHASE-6-PROGRESS.md`.

