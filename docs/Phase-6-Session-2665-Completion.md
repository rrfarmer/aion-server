# Phase 6 Session 2665 Completion

## UOW

[Phase 6] UOW-2665: Send house Lua sandbox script on enter-world.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: successful live enter-world now sends Java's housing Lua sandbox script packet before SM_ENTER_WORLD_CHECK.
- Java source/runtime path: PlayerEnterWorldService.onLogin -> new SM_HOUSE_SCRIPTS(0, PlayerScript.LUA_SANDBOX_FIX); PlayerScript.LUA_SANDBOX_FIX -> CompressUtil.compress(UTF-16LE XML).
- C# runtime artifact wired: PlayerScript.LuaSandboxFix, PlayerScripts.Compress, and GameServerConnection's enter-world packet sequence.
- Client-visible/state/persistence effect: the entering client receives a real SM_HOUSE_SCRIPTS packet containing the sandbox script before enter-world acknowledgement.
- Why this is runtime progress: this sends a real Java-equivalent server packet from the live CM_ENTER_WORLD path.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/player/PlayerEnterWorldService.java`
  - Sends `client.sendPacket(new SM_HOUSE_SCRIPTS(0, PlayerScript.LUA_SANDBOX_FIX))` before `SM_ENTER_WORLD_CHECK`.
- `game-server/src/com/aionemu/gameserver/model/house/PlayerScript.java`
  - Builds `LUA_SANDBOX_FIX` from UTF-16LE XML that disables unsafe Lua globals, compressed with `CompressUtil.compress`.
- `game-server/src/com/aionemu/gameserver/utils/xml/CompressUtil.java`
  - Uses Java `Deflater`/zlib compression.
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_HOUSE_SCRIPTS.java`
  - Serializes single script id/content/uncompressed size plus 8 bytes of padding.

## C# Changes

- Added `PlayerScript.LuaSandboxFix` with Java's sandbox XML content, UTF-16LE encoding, and zlib compression.
- Added `PlayerScripts.Compress` to mirror Java `CompressUtil.compress`.
- Sent `new SmHouseScripts(0, PlayerScript.LuaSandboxFix)` from successful `CM_ENTER_WORLD` dispatch before `SmEnterWorldCheck`.

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmEnterWorldSendsLuaSandboxHouseScriptBeforeEnterWorldCheck` | Unit / live dispatch regression | `PlayerEnterWorldService.onLogin`, `PlayerScript.LUA_SANDBOX_FIX`, `SM_HOUSE_SCRIPTS.writeImpl` | Live `CM_ENTER_WORLD` sends `SmHouseScripts` before `SmEnterWorldCheck`; payload contains script id 0, compressed sandbox XML, uncompressed size, and Java padding. | Exercises `GameServerConnection.ProcessPacketAsync` and byte-reads the emitted packet. | Does not prove real-client execution of the Lua script. |

## Validation Decision

```text
- Changed surface: live enter-world packet dispatch and shared house-script packet/model helpers.
- Specific behavior/contract: Java PlayerEnterWorldService sends SM_HOUSE_SCRIPTS(0, PlayerScript.LUA_SANDBOX_FIX) before SM_ENTER_WORLD_CHECK.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CmEnterWorldSendsLuaSandboxHouseScriptBeforeEnterWorldCheck|FullyQualifiedName~CmHouseScriptTests" --no-restore
- Focused Java/Maven command: not run; no narrow Java test exists for PlayerEnterWorldService's sandbox packet in this checkout, and the Java source path was reviewed directly.
- Broad-validation trigger: live enter-world dispatch and server packet fanout changed.
- Broad .NET decision: skipped after focused validation because the filtered tests compiled the changed project and exercised the live enter-world packet path plus adjacent SM_HOUSE_SCRIPTS update/remove behavior.
- Why this scope is sufficient: the UOW adds one deterministic login packet and reuses the existing house-script packet serializer; the tests verify order and packet payload bytes.
```

Results:

- Focused C# validation passed: 8/8 tests.
- Existing nullable/analyzer warnings were emitted in unrelated surfaces on the first focused run; the final adjacent run passed without failures.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.player.PlayerEnterWorldService` sandbox script send | `Aion.GameServer.Network.Aion.GameServerConnection` enter-world branch | Live packet dispatch | Partial | Unit Tested | Partial Parity | Sandbox script packet is sent before `SmEnterWorldCheck` for successful enter-world. Many other Java enter-world ordering details remain partial. |
| `com.aionemu.gameserver.model.house.PlayerScript.LUA_SANDBOX_FIX` | `Aion.GameServer.Model.GameObjects.PlayerScript.LuaSandboxFix` | DTO / static runtime script | Complete | Unit Tested | Partial Parity | UTF-16LE sandbox XML is compressed and sent in a byte-tested packet. Exact Java compressed byte stream may vary by zlib implementation/level, so parity is kept conservative. |
| `com.aionemu.gameserver.utils.xml.CompressUtil.compress` | `Aion.GameServer.Model.GameObjects.PlayerScripts.Compress` | Utility | Partial | Unit Tested | Partial Parity | Uses .NET `ZLibStream` to produce zlib-compressed bytes accepted by the paired decompressor. Java compression-level byte identity has not been golden-compared. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_HOUSE_SCRIPTS` | `Aion.GameServer.Network.Aion.ServerPackets.SmHouseScripts` | Server packet | Partial | Unit Tested | Partial Parity | Login sandbox packet and single-script update/removal payloads are byte-tested. Multi-script split-list flow remains unported. |

## Known Gaps

- Saved player house scripts are still not restored from `house_scripts` during player/house load.
- Java `House.sendScripts(Player)` and `PlayerScripts.sendToPlayer` split-list behavior remain unported.
- Exact Java compressed bytes for `LUA_SANDBOX_FIX` were not golden-compared against a Java fixture.
- Real-client Lua execution was not validated.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%

## Next Runtime UOW Candidates

1. Restore saved house scripts into `PlayerHouse.Scripts` from `house_scripts` during player house load, using Java `HouseScriptsDAO.getPlayerScripts` as the source of truth.
2. Add a narrow Java/C# compression golden comparison for `PlayerScript.LUA_SANDBOX_FIX` only if it directly gates script packet/client behavior.
3. Find another deferred client packet branch that can send a deterministic Java-equivalent server packet or mutate already-modeled state.
