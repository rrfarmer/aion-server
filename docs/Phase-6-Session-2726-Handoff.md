# Phase 6 Session 2726 Handoff

## Completed UOW

[Phase 6] UOW-2726: Open live legion warehouse dialog.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: CM_DIALOG_SELECT OPEN_LEGION_WAREHOUSE now sends the successful live legion warehouse packet sequence.
- Java source/runtime path: DialogService.onDialogSelect OPEN_LEGION_WAREHOUSE -> LegionService.openLegionWarehouse -> SM_LEGION_EDIT(0x04), SM_WAREHOUSE_INFO storage 3 chunks, SM_DIALOG_WINDOW(DialogPage.LEGION_WAREHOUSE).
- C# runtime artifact wired: CmDialogSelect.OpenLegionWarehouse, GameServerConnection.HandleOpenLegionWarehouseDialogAsync, SmWarehouseInfo.CreateLegionWarehouseOpenPackets, SmDialogWindow.LegionWarehousePageId.
- Client-visible/state/persistence effect: valid legion warehouse NPC selection sends warehouse kinah, item list, final info packet, and page 25 dialog packets from live code.
- Why this is runtime progress: it wires a deferred live client packet path and sends real server packets.
```

## Commit

`[Phase 6][UOW-2726] Open live legion warehouse dialog`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmDialogSelect.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmDialogWindow.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmWarehouseInfo.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionStorageExpansionDialogTests.cs`
- `docs/Phase-6-Session-2726-Completion.md`
- `docs/Phase-6-Session-2726-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/services/DialogService.java`
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/model/DialogPage.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_WAREHOUSE_INFO.java`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.ClientPackets.CmDialogSelect`
- `Aion.GameServer.Network.Aion.GameServerConnection.HandleDialogSelectAsync`
- `Aion.GameServer.Network.Aion.GameServerConnection.HandleOpenLegionWarehouseDialogAsync`
- `Aion.GameServer.Network.Aion.ServerPackets.SmWarehouseInfo`
- `Aion.GameServer.Network.Aion.ServerPackets.SmDialogWindow`
- `Aion.GameServer.Network.Aion.ServerPackets.SmLegionEdit` as existing packet dependency

## Validation

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionStorageExpansionDialogTests|FullyQualifiedName~SmLegionEditTests" --logger "console;verbosity=minimal"
```

Result:

- Passed: 24
- Failed: 0
- Skipped: 0
- Existing warnings only.

Repository hygiene:

```powershell
git diff --check
```

Result:

- Passed with only Git CRLF working-copy warnings for touched files.

Java/Maven:

- Not run. Java source was reviewed unchanged, and no narrow Java fixture exists for this dialog branch in this checkout.

Broad .NET:

- Not run. Broad-validation trigger was live dialog packet dispatch, but the focused command built the affected project and decoded the live packet sequence and adjacent packet helper behavior.

## Conservative Parity Status

- `OPEN_LEGION_WAREHOUSE` has partial runtime parity for the successful packet-sending path.
- Full `LegionService.openLegionWarehouse` parity is not claimed because lock state, disbanding guard, enabled config, denial branches, and a full shared `LegionWarehouse` aggregate remain missing.
- `SM_WAREHOUSE_INFO` storage `3` composition has focused C# evidence but no Java golden bytes.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `DialogService.onDialogSelect` | `GameServerConnection.HandleDialogSelectAsync` | Live client packet handler | Partial | Unit Tested | Partial Parity | Successful `OPEN_LEGION_WAREHOUSE` branch is live; many dialog actions remain outside this UOW. |
| `LegionService.openLegionWarehouse` | `GameServerConnection.HandleOpenLegionWarehouseDialogAsync` | Service side effect | Partial | Unit Tested | Partial Parity | Sends Java packet sequence for successful open; Java guard/lock branches remain partial. |
| `SM_WAREHOUSE_INFO` | `SmWarehouseInfo.CreateLegionWarehouseOpenPackets` | Server packet helper | Partial | Unit Tested | Partial Parity | Storage-3 open packet shape is decoded by tests; Java golden bytes not captured. |
| `DialogPage.LEGION_WAREHOUSE` | `SmDialogWindow.LegionWarehousePageId` | Constant / packet field | Complete | Unit Tested indirectly | Partial Parity | Page id 25 is used from live code; full DialogPage enum is not modeled. |

## Known Gaps / Watchouts

- No live in-use state for legion warehouse is modeled yet.
- Java `canOpenWarehouse` denial behavior is incomplete in C#:
  - no legion should send `STR_NO_GUILD_TO_DEPOSIT`,
  - disabled config or unsupported NPC action should send `STR_CANT_USE_GUILD_STORAGE`,
  - disbanding legion should send `STR_GUILD_WAREHOUSE_CANT_USE_WHILE_DISPERSE`,
  - no permission sends `STR_GUILD_WAREHOUSE_NO_RIGHT` and is partially represented,
  - in-use should send `STR_GUILD_WAREHOUSE_IN_USE`.
- C# does not yet have a distinct shared `LegionWarehouse` aggregate; storage-3 items are represented in `Player.InventoryItems`.
- Exact Java bytes for storage-3 `SM_WAREHOUSE_INFO` were not captured.

## Next Recommended Runtime UOW

Recommended candidate: add live denial behavior for `OPEN_LEGION_WAREHOUSE` where packet helpers already exist, starting with no-legion and no-permission branches.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: invalid OPEN_LEGION_WAREHOUSE selections should send Java system-message denial packets instead of silently returning.
- Java source/runtime path: LegionService.canOpenWarehouse(Player, Npc) membership/permission branches -> SM_SYSTEM_MESSAGE.STR_NO_GUILD_TO_DEPOSIT and STR_GUILD_WAREHOUSE_NO_RIGHT.
- C# runtime artifact likely involved: GameServerConnection.HandleOpenLegionWarehouseDialogAsync and SmSystemMessage helpers.
- Client-visible/state/persistence effect expected: real clients receive Java-equivalent denial packets from live dialog dispatch.
- Why this is runtime progress: it sends real server packets from a live client packet path.
```

Suggested discovery:

```powershell
rg -n "STR_NO_GUILD_TO_DEPOSIT|STR_CANT_USE_GUILD_STORAGE|STR_GUILD_WAREHOUSE_CANT_USE_WHILE_DISPERSE|STR_GUILD_WAREHOUSE_IN_USE|GuildWarehouse|NoGuild|CantUseGuildStorage" game-server/src/com/aionemu/gameserver dotnetConversion/src/Aion.GameServer dotnetConversion/tests/Aion.GameServer.Tests
```

Suggested focused validation:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionStorageExpansionDialogTests|FullyQualifiedName~GamePacketTests" --logger "console;verbosity=minimal"
```

Narrow this after discovery to the exact dialog denial tests and `SmSystemMessage` packet tests touched. Java/Maven is not expected unless a narrow Java fixture is found. Broad-validation trigger: live dialog packet dispatch; start focused.

## Other Safe Runtime Candidates

- Model live legion warehouse in-use state and release it from close/logout/dialog-close paths.
- Add Java-derived legion warehouse capacity checks to move/split if current C# can overfill location `3`.
- Extend storage-3 `SM_WAREHOUSE_INFO` tests with Java golden bytes if a narrow fixture is created.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits before this UOW:
  - `8f276bbf4 [Phase 6][UOW-2725] Use legion level for warehouse size packets`
  - `cbf4238b1 [Phase 6][UOW-2724] Persist legion warehouse item history`
  - `1d358d9f8 [Phase 6][UOW-2723] Persist legion warehouse history`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
