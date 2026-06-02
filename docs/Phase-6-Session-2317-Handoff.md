# Phase 6 Session 2317 Handoff - Beshmundir Non-Leader Closed Entry

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2317-Completion.md`
- `docs/Phase-6-Session-2317-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

Last completed UOW: UOW-2317, Beshmundir's Walk grouped non-leader `INSTANCE_ENTRY` closed-instance rejection.

Completed Beshmundir slices:

- Solo/non-group `INSTANCE_ENTRY`: sends `SmSystemMessage.EnterOnlyPartyDon()`.
- Group leader `INSTANCE_ENTRY`: sends `SmDialogWindow(targetObjectId, 4762)`.
- Grouped non-leader `INSTANCE_ENTRY` when no group member is in world `300170000`: sends `SmSystemMessage.InstanceDungeonCantEnterNotOpened()`.

Still not proven:

- Non-leader member-in-instance movement.
- Difficulty selection request handling for `SELECT_NONE_1` and `SELECT_NONE_2`.
- Real-client/encrypted socket bytes for these branches.

## Commits Made

- `c12d00c4c [Phase 6][UOW-2315] Reject solo Beshmundir walk entry`
- `10b528b55 [Phase 6][UOW-2316] Show Beshmundir leader path dialog`
- `[Phase 6][UOW-2317] Reject Beshmundir non-leader closed entry`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`
- `docs/Phase-6-Session-2317-Completion.md`
- `docs/Phase-6-Session-2317-Handoff.md`

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `ai.instance.beshmundirTemple.BeshmundirsWalkAI.onDialogSelect` action `INSTANCE_ENTRY` non-leader closed-instance branch | `Aion.GameServer.Network.Aion.GameServerConnection.HandleDialogSelectAsync` | AI Handler / Client Packet Boundary | Partial | Focused Boundary Tested | Partial Parity | C# now sends message `1400361` for grouped non-leaders when no group member is in world `300170000`. Other Beshmundir branches remain unported. |
| `SM_SYSTEM_MESSAGE.STR_MSG_INSTANCE_DUNGEON_CANT_ENTER_NOT_OPENED` | `SmSystemMessage.InstanceDungeonCantEnterNotOpened()` | Server Packet | Ported Helper | Boundary Tested | Partial Packet Evidence | Existing system-message packet path is exercised through the Beshmundir rejection branch. |

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_BeshmundirsWalkInstanceEntryRejectsNonLeaderWhenInstanceNotOpened" --no-restore
```

Result: passed 1, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Focused Java/Maven validation: skipped because no targeted Java fixture exists for `BeshmundirsWalkAI.onDialogSelect`. Java source review was used as source-of-truth evidence for the narrow branch.

Broad-validation trigger: none.

Broad .NET decision: skipped full project/solution validation. Focused boundary validation covered the changed branch and supplied the compile signal.

## Next Sequential UOW

Recommended next runtime scope: `BeshmundirsWalkAI.INSTANCE_ENTRY` grouped non-leader follow-entry when at least one group member is already inside Beshmundir Temple.

Java artifacts to inspect:

- `game-server/data/handlers/ai/instance/beshmundirTemple/BeshmundirsWalkAI.java`
- `game-server/src/com/aionemu/gameserver/services/teleport/PortalService.java`
- `game-server/src/com/aionemu/gameserver/model/templates/portal/PortalPath.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PortalEntryValidationService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerGroupRuntime.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`

Specific behavior to prove: when the Beshmundir's Walk target uses action `INSTANCE_ENTRY`, the player is grouped but not leader, and at least one group member has `WorldId == 300170000`, Java calls `moveToInstance(player, (byte) 0)` and routes through `PortalService.port(...)`.

Focused C# command should target the new behavior only once implemented.

Focused Java/Maven command: not expected unless a targeted Java fixture is added; Java source review is likely the practical source-of-truth evidence for this AI branch.

Broad-validation trigger: only if the implementation touches shared portal movement, instance allocation, or teleport services beyond the Beshmundir-specific boundary.

## Safe Candidates

- Non-leader Beshmundir's Walk member-in-instance portal entry if C# portal difficulty/instance state dependencies are present.
- Difficulty select request handling only if `SM_QUESTION_WINDOW.STR_INSTANCE_DUNGEON_WITH_DIFFICULTY_ENTER_CONFIRM` and question-response plumbing are available.
- A tiny system-message packet serialization test for `SmSystemMessage.InstanceDungeonCantEnterNotOpened()` if packet coverage becomes necessary.

Avoid:

- Full .NET project tests or solution builds without a documented trigger.
- Evidence/reporting-only units.
- Updating `docs/PHASE-6-PROGRESS.md`.
