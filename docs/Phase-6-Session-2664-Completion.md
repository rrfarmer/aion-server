# Phase 6 Session 2664 Completion

## UOW

[Phase 6] UOW-2664: Persist and broadcast house scripts.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: normal live CM_HOUSE_SCRIPT save/remove now validates active-house ownership, mutates runtime PlayerScripts state, persists/deletes house_scripts rows, and broadcasts SM_HOUSE_SCRIPTS.
- Java source/runtime path: CM_HOUSE_SCRIPT.runImpl -> PlayerScripts.set/remove -> HouseScriptsDAO.storeScript/deleteScript -> SM_HOUSE_SCRIPTS.writeImpl.
- C# runtime artifact wired: GameServerConnection.HandleHouseScriptAsync, PlayerScripts/PlayerScript, IHousingRepository/MySqlHousingRepository script methods, SmHouseScripts.
- Client-visible/state/persistence effect: valid owner script edits are stored in the Java house_scripts table shape and visible players receive the Java house-script update/removal packet.
- Why this is runtime progress: this changes live client-packet dispatch, live player/house state, persistence, and server-packet fanout.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_HOUSE_SCRIPT.java`
  - Oversized compressed scripts send overflow and return.
  - Non-owned active-house addresses are audited and ignored.
  - `totalSize == 0` removes a script; otherwise `PlayerScripts.set` stores it.
  - Successful mutation broadcasts `new SM_HOUSE_SCRIPTS(address, scripts.get(scriptId))`.
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/PlayerScripts.java`
  - Maintains 8 script slots.
  - Decompresses zlib/Deflater bytes and validates UTF-16LE byte length before mutating state.
  - Persists through `HouseScriptsDAO` for live save/remove.
- `game-server/src/com/aionemu/gameserver/dao/HouseScriptsDAO.java`
  - Uses `house_scripts(house_id, script_id, script)` insert/update and delete.
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_HOUSE_SCRIPTS.java`
  - Writes address, script count, id, either zero length for removal or compressed length/uncompressed size/content plus 8 bytes of `0xCD` padding.

## C# Changes

- Added `PlayerScript` and `PlayerScripts` runtime state with Java script slot limit, zlib decompression, UTF-16LE decoding, and byte-length validation.
- Added `PlayerHouse.Scripts` so active houses can hold live script state.
- Added `IHousingRepository.StoreHouseScriptAsync` and `DeleteHouseScriptAsync`, with MySQL implementations using Java's `house_scripts` table shape.
- Added `SmHouseScripts` server packet with Java opcode `131` and Java-compatible add/remove payload serialization.
- Replaced the deferred `CmHouseScript` branch with `HandleHouseScriptAsync` for overflow, ownership guard, save/remove mutation, persistence, and visible-player broadcast.

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_ValidOwnerScriptPersistsStateAndBroadcastsHouseScript` | Unit / live dispatch regression | `CM_HOUSE_SCRIPT.runImpl`, `PlayerScripts.set`, `HouseScriptsDAO.storeScript`, `SM_HOUSE_SCRIPTS.writeImpl` | Valid owner script update stores decoded XML, mutates runtime script bytes, and broadcasts Java-shaped script packet. | Exercises live `GameServerConnection.ProcessPacketAsync` and byte-reads `SmHouseScripts`. | Does not prove real MySQL execution or real-client rendering. |
| `ProcessPacketAsync_RemoveScriptDeletesStateAndBroadcastsRemoval` | Unit / live dispatch regression | `CM_HOUSE_SCRIPT.runImpl`, `PlayerScripts.remove`, `HouseScriptsDAO.deleteScript`, `SM_HOUSE_SCRIPTS.writeImpl` | `totalSize == 0` deletes the script, clears runtime state, and broadcasts the zero-length removal packet. | Exercises live dispatch and byte-reads removal payload. | Does not prove real MySQL execution. |
| `ProcessPacketAsync_NonOwnerScriptMutationDoesNotPersistOrBroadcast` | Unit / live dispatch regression | `CM_HOUSE_SCRIPT.runImpl` active-house ownership guard | Address mismatch does not persist, mutate, or broadcast. | Exercises live dispatch guard. | Audit log text is not asserted. |

## Validation Decision

```text
- Changed surface: live client-packet dispatch, runtime house state, housing repository persistence, and server packet serialization.
- Specific behavior/contract: Java CM_HOUSE_SCRIPT normal save/remove mutates PlayerScripts, persists/deletes house_scripts, and broadcasts SM_HOUSE_SCRIPTS.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmHouseScriptTests" --no-restore
- Focused Java/Maven command: not run; no narrow Java runImpl/persistence fixture exists for CM_HOUSE_SCRIPT in this checkout, and Java source was reviewed directly.
- Broad-validation trigger: live packet dispatch, runtime state, persistence, and server packet fanout changed.
- Broad .NET decision: skipped after focused validation because the filtered test compiled the changed project and exercised the edited live dispatch, repository contract, and packet payload.
- Why this scope is sufficient: the UOW is isolated to CM_HOUSE_SCRIPT and SM_HOUSE_SCRIPTS; the focused tests cover update, removal, overflow, and ownership guard paths through live packet dispatch.
```

Results:

- First focused C# run exposed missing new `IHousingRepository` methods on an existing test fake; patched.
- Final focused C# validation passed: 7/7 tests.
- Existing nullable/analyzer warnings were emitted in unrelated surfaces.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_HOUSE_SCRIPT` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleHouseScriptAsync` | Client packet handler | Partial | Unit Tested | Partial Parity | Overflow, active-house ownership guard, save/remove mutation, persistence calls, and visible-player fanout are live. Full audit parity, real-client behavior, load-on-login, split-list send-to-player, and new-house foreign-key save behavior remain incomplete. |
| `com.aionemu.gameserver.model.gameobjects.player.PlayerScripts` | `Aion.GameServer.Model.GameObjects.PlayerScripts` | Runtime state | Partial | Unit Tested | Partial Parity | Script limit, invalid-id guard, zlib decompression, UTF-16LE decoding, size validation, set/remove/get are represented. `removeAll`, split-list send-to-player, DAO restore helper, and logging details remain unported. |
| `com.aionemu.gameserver.model.house.PlayerScript` | `Aion.GameServer.Model.GameObjects.PlayerScript` | DTO / runtime state | Partial | Unit Tested | Partial Parity | Runtime fields and `HasData` behavior are represented. Java `LUA_SANDBOX_FIX` is not ported in this UOW. |
| `com.aionemu.gameserver.dao.HouseScriptsDAO` | `Aion.GameServer.Data.IHousingRepository` / `MySqlHousingRepository` script methods | Repository | Partial | Unit Tested with fake | Partial Parity | Store/delete use Java table and key shape. Load/restore and delete-all are not ported; real MySQL integration was not executed. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_HOUSE_SCRIPTS` | `Aion.GameServer.Network.Aion.ServerPackets.SmHouseScripts` | Server packet | Partial | Unit Tested | Partial Parity | Single-script update/removal serialization is byte-tested, including padding and opcode. Multi-script split-list send-to-player is not ported. |

## Known Gaps

- House scripts are not restored from `house_scripts` during player/world house load.
- Java `PlayerScript.LUA_SANDBOX_FIX` login send is not ported.
- Java `PlayerScripts.sendToPlayer` split-list logic is not ported.
- Java new-house foreign-key pre-save behavior is not represented; C# only targets already-loaded houses.
- Real MySQL execution and real-client script rendering were not run.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 5
- Total artifacts ported or extended in this UOW: 5
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 5
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%

## Next Runtime UOW Candidates

1. Restore saved house scripts into `PlayerHouse.Scripts` from `house_scripts` and send Java-compatible `SM_HOUSE_SCRIPTS` packets during live enter-world/house-load flow.
2. Port `PlayerScript.LUA_SANDBOX_FIX` login packet if the existing enter-world packet sequence can wire it without broader housing redesign.
3. Find another deferred client packet branch that can send an existing deterministic packet or mutate already-modeled live state.
