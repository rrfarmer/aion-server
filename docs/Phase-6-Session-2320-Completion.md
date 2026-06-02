# Phase 6 Session 2320 Completion - Beshmundir Non-Leader Follow Entry

## Scope

Implemented the Java `BeshmundirsWalkAI.INSTANCE_ENTRY` grouped non-leader follow-entry branch.

Java source reviewed:

- `game-server/data/handlers/ai/instance/beshmundirTemple/BeshmundirsWalkAI.java`
- `game-server/src/com/aionemu/gameserver/services/teleport/PortalService.java`

Java behavior used:

- Grouped non-leaders first check whether any group member is in world `300170000`.
- If no member is inside, Java sends `STR_MSG_INSTANCE_DUNGEON_CANT_ENTER_NOT_OPENED`.
- If a member is inside, Java calls `moveToInstance(player, (byte) 0)`.
- `moveToInstance` resolves `DataManager.PORTAL2_DATA.getPortalUsePath(getNpcId(), player)` and calls `PortalService.port(portalPath, player, getOwner(), difficult)`.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`

Implemented:

- Beshmundir grouped non-leader follow-entry now resolves the NPC `portal_use` path from loaded static data.
- The branch runs portal-entry preparation and uses the registered group continuation path from UOW-2319.
- Validation rejections send their failure packet when available.
- Missing runtime/static data or missing portal-use path remains side-effect-free, matching Java's null-path no-op.

## Validation Decision

- Changed surface: Beshmundir live dialog dispatch plus registered group portal continuation reuse.
- Specific behavior/contract: Java Beshmundir grouped non-leader with a group member in world `300170000` enters through `PortalService.port(...)` using the NPC portal-use path.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_BeshmundirsWalkInstanceEntryRejectsSoloPlayer|FullyQualifiedName~ProcessPacketAsync_BeshmundirsWalkInstanceEntryShowsPathDialogForGroupLeader|FullyQualifiedName~ProcessPacketAsync_BeshmundirsWalkInstanceEntryRejectsNonLeaderWhenInstanceNotOpened|FullyQualifiedName~ProcessPacketAsync_BeshmundirsWalkInstanceEntryMovesNonLeaderWhenGroupMemberInside|FullyQualifiedName~QueuePortalContinueTransferAsync_RegisteredGroupInstanceTransfersAndAppliesCooldown|FullyQualifiedName~QueuePortalContinueTransferAsync_RegisteredGroupReentryTransfersWithoutCooldown" --no-restore
```

Result: passed 6, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain. The filtered command supplied the compile signal for the affected C# project and tests.

- Focused Java/Maven command: skipped. No targeted Java fixture exists for this runtime handler branch; Java source review was the practical source-of-truth evidence.
- Repository hygiene:

```powershell
git diff --check
```

Result: passed with line-ending normalization warnings only.

- Broad-validation trigger: live dialog dispatch and portal continuation reuse.
- Broad .NET decision: skipped full project/solution validation after the focused command covered the Beshmundir entry branches and registered group continuation paths. No packet primitives, serialization helpers, persistence schema, or shared infrastructure were changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `data.handlers.ai.instance.beshmundirTemple.BeshmundirsWalkAI.onDialogSelect` `INSTANCE_ENTRY` grouped non-leader follow branch | `Aion.GameServer.Network.Aion.GameServerConnection.HandleDialogSelectAsync` | Runtime Handler / Connection Boundary | Partial | Focused Boundary Tested | Partial Parity | Solo, leader path dialog, non-leader closed-instance rejection, and non-leader registered-instance follow-entry are covered. Difficulty question acceptance movement remains unported. |
| `data.handlers.ai.instance.beshmundirTemple.BeshmundirsWalkAI.moveToInstance` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleBeshmundirsWalkMoveToInstanceAsync` | Runtime Handler Helper | Partial | Focused Boundary Tested | Partial Parity | Resolves `portal_use` path and uses portal preparation/continuation. Java difficulty parameter is not yet wired for leader question acceptance. |
| `com.aionemu.gameserver.services.teleport.PortalService.port` group registered-instance branch | `Aion.GameServer.Network.Aion.GameServerConnection.QueuePortalContinueTransferAsync` | Service / Connection Boundary | Partial | Focused Boundary Tested | Partial Parity | Reused for Beshmundir follow-entry. Fresh group allocation, alliance/league branches, and generic team portal routing remain incomplete. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `ProcessPacketAsync_BeshmundirsWalkInstanceEntryMovesNonLeaderWhenGroupMemberInside` | Boundary Runtime | Java source review of `BeshmundirsWalkAI` and `PortalService.port` | Grouped non-leader with a member inside resolves the use path, transfers to registered group instance, sends teleport/instance packets, registers the player, and applies cooldown. | Focused C# boundary execution plus Java source review. | Does not cover difficulty acceptance or real-client encrypted bytes. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2
- Total artifacts ported/extended in this UOW: 3
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: unchanged, conservatively partial.

## Remaining Gaps

- Beshmundir difficulty question acceptance movement remains unported.
- Java request range observer auto-deny remains unported.
- Fresh group-instance allocation and `registerTeam(group)` remain unported.
- Generic portal dialog live routing for registered team plans remains incomplete.
- Real-client or encrypted socket bytes for these Beshmundir branches remain unverified.

## Commit

Commit message:

```text
[Phase 6][UOW-2320] Move Beshmundir non-leader into open instance
```

## Next Recommended UOW

Implement Beshmundir difficulty question acceptance movement. Use the stored pending Beshmundir request, Java `AIRequest.acceptRequest`, and the same portal-use-path movement helper, while preserving the documented Java quirk that request id `902050` currently selects difficulty `2`.
