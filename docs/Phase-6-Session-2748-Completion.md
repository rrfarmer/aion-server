# Phase 6 Session 2748 Completion

## Unit of Work

[Phase 6][UOW-2748] Wire live legion permission edits

## Runtime Progress Gate

- Deferred/live behavior advanced: `CM_LEGION` exOpcode `0x0D` now changes live legion permission masks instead of remaining parser-only.
- Java source of truth: `CM_LEGION.readImpl` case `0x0D`, `CM_LEGION.runImpl` case `0x0D`, `LegionService.changePermissions`, `Legion.setLegionPermissions`, `SM_LEGION_EDIT.writeImpl` type `0x02`, and `SM_SYSTEM_MESSAGE.STR_GUILD_CHANGE_RIGHT_DONT_HAVE_RIGHT`.
- C# runtime artifact wired/fixed: `GameServerConnection.HandleLegionAsync`, active `Player` legion permission snapshot fields, `SmLegionEdit.Permissions`, `SmSystemMessage`, and focused `CmLegionTests`.
- Client-visible/state effect changed: brigade generals can send a live permission edit packet that mutates the active legion permission state and returns a Java-shaped `SM_LEGION_EDIT` type `0x02`; unauthorized members receive the Java no-right system message.
- Why this is not preview-only/test-only/documentation-only: this UOW wires a deferred live client packet path, mutates live player/legion permission state, and sends real server packets from live code.

## Java Sources Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION.java`
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/Legion.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_EDIT.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`

## C# Runtime Changes

- Added `SmSystemMessage.GuildChangeRightDontHaveRight()` for Java message id `1300283`.
- Wired `CM_LEGION` exOpcode `0x0D` in `GameServerConnection.HandleLegionAsync`.
- Implemented Java-style brigade-general guard.
- Mutated the active player's loaded legion permission snapshot fields from the parsed signed-short values.
- Sent `SmLegionEdit.Permissions` with the updated deputy, centurion, legionary, and volunteer masks.

## Validation Decision

- Changed surface: live connection dispatch, runtime legion permission state, and server-packet output.
- Specific behavior/contract: Java `LegionService.changePermissions` requires a brigade general, mutates the four permission masks, and broadcasts `SM_LEGION_EDIT` type `0x02`; non-BG members receive `STR_GUILD_CHANGE_RIGHT_DONT_HAVE_RIGHT`.
- Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests" --logger "console;verbosity=minimal" --no-restore
```

- Focused Java/Maven command: not run; Java source was unchanged and no narrow Java unit fixture exists for this packet/service path.
- Broad-validation trigger: live connection dispatch and runtime state mutation.
- Broad .NET decision: skipped after focused validation because the selected tests compile the game-server project and directly exercise the changed live packet branch and packet payload.
- Why this scope is sufficient: `CmLegionTests` already covers signed-short parsing and now covers both authorization branches, live state mutation, and the Java packet order for the emitted edit packet.

## Validation Result

- Focused C# result: Passed, 19 total, 0 failed, 0 skipped.
- Existing nullable/analyzer warnings remain outside this UOW.
- Java/Maven was not run for the reason above.

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `HandleInfrastructurePacketAsync_EditPermissionsWithoutBrigadeGeneralSendsNoRightLikeJava` | Unit | `LegionService.changePermissions` | Non-BG legion members receive message id `1300283` and permission masks do not mutate. | Java source review plus live handler packet assertion. | Does not cover non-member path beyond existing legion guard. |
| `HandleInfrastructurePacketAsync_EditPermissionsMutatesRuntimeStateAndSendsEditLikeJava` | Unit | `LegionService.changePermissions` and `SM_LEGION_EDIT.writeImpl` type `0x02` | Brigade general edit mutates four runtime masks and sends type `0x02` with the masks in Java order. | Java source review plus live handler packet assertion. | Broadcast fanout to other online legion members is not implemented. |

## Tests Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `SmSystemMessage_LegionNoticeHelpersUseJavaIdsAndParameters` | Unit | `SM_SYSTEM_MESSAGE` helpers | Includes `STR_GUILD_CHANGE_RIGHT_DONT_HAVE_RIGHT` id `1300283`. | Java source review plus id assertion. | Broader system message surface is not claimed verified. |

## Conservative Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_LEGION` | `Aion.GameServer.Network.Aion.ClientPackets.CmLegion` and `GameServerConnection.HandleLegionAsync` | Client Packet / Runtime Dispatch | Partial | Unit Tested | Partial Parity | ExOpcode `0x0D` is now live; exOpcodes `0x07`, `0x08`, and `0x09` remain live from prior UOWs; other subactions are deferred. |
| `com.aionemu.gameserver.services.LegionService` | `GameServerConnection.HandleLegionPermissionChangeAsync` | Service Logic | Partial | Unit Tested | Partial Parity | Core permission mutation and no-right branch are live. Java broadcasts to all online legion members through shared `Legion`; C# sends to active connection only. |
| `com.aionemu.gameserver.model.team.legion.Legion` | `Aion.GameServer.Model.GameObjects.Player` legion permission snapshot fields | Runtime State | Partial | Unit Tested | Partial Parity | C# mutates the active player's loaded snapshot; shared aggregate and cross-member synchronization remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_EDIT` | `Aion.GameServer.Network.Aion.ServerPackets.SmLegionEdit` | Server Packet | Partial | Unit Tested | Partial Parity | Type `0x02` permission payload is covered through live handler output; broader packet variants were already partial. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` | Server Packet Helper | Partial | Unit Tested | Partial Parity | Added no-right helper id `1300283`; broader helper surface is not claimed verified. |

## Summary Metrics

- Total Java artifacts discovered: 5
- Total artifacts ported in this UOW: 5 partial runtime artifacts
- Total artifacts with verified parity: 0
- Total artifacts needing verification/partial parity: 5
- Total blocked artifacts: 0
- Estimated overall migration completion: unchanged, conservatively still Phase 6 in progress

## Known Gaps

- Java broadcasts permission edits to all online legion members; C# currently sends the edit packet only to the active connection.
- C# still lacks Java's shared `Legion` aggregate, so other loaded player snapshots are not updated.
- Java persistence timing for changed legion permission masks remains Needs Verification; `LegionService.changePermissions` itself does not directly call `LegionDAO.storeLegion`.
- No real client validation was performed.
