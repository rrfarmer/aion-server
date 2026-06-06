# Phase 6 Session 2665 Handoff

## Completed UOW

[Phase 6] UOW-2665: Send house Lua sandbox script on enter-world.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: successful live CM_ENTER_WORLD now sends Java's housing Lua sandbox script packet.
- Java source/runtime path: PlayerEnterWorldService.onLogin -> SM_HOUSE_SCRIPTS(0, PlayerScript.LUA_SANDBOX_FIX); PlayerScript.LUA_SANDBOX_FIX -> CompressUtil.compress(UTF-16LE XML).
- C# runtime artifact wired: PlayerScript.LuaSandboxFix, PlayerScripts.Compress, and GameServerConnection enter-world packet sequence.
- Client-visible/state/persistence effect: clients receive a real SM_HOUSE_SCRIPTS sandbox packet before SM_ENTER_WORLD_CHECK.
- Why this is runtime progress: this sends a real Java-equivalent server packet from live enter-world dispatch.
```

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/PlayerScripts.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `docs/Phase-6-Session-2665-Completion.md`
- `docs/Phase-6-Session-2665-Handoff.md`

## Validation

Commands run:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CmEnterWorldSendsLuaSandboxHouseScriptBeforeEnterWorldCheck" --no-restore
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CmEnterWorldSendsLuaSandboxHouseScriptBeforeEnterWorldCheck|FullyQualifiedName~CmHouseScriptTests" --no-restore
```

Result:

- Final focused run passed: 8
- Failed: 0
- Skipped: 0

No Java/Maven command was run; no narrow Java test exists for this `PlayerEnterWorldService` login packet in this checkout.

## Conservative Parity Status

- Live enter-world now sends Java's `SM_HOUSE_SCRIPTS(0, PlayerScript.LUA_SANDBOX_FIX)` packet before `SmEnterWorldCheck`.
- Full house-script parity remains incomplete: saved scripts are not restored from `house_scripts`, split-list send-to-player is not ported, and exact Java zlib byte identity is not golden-compared.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.player.PlayerEnterWorldService` sandbox script send | `Aion.GameServer.Network.Aion.GameServerConnection` enter-world branch | Live packet dispatch | Partial | Unit Tested | Partial Parity | Sandbox script packet is live and ordered before `SmEnterWorldCheck`; other enter-world ordering remains partial. |
| `com.aionemu.gameserver.model.house.PlayerScript.LUA_SANDBOX_FIX` | `Aion.GameServer.Model.GameObjects.PlayerScript.LuaSandboxFix` | DTO / static runtime script | Complete | Unit Tested | Partial Parity | Content is UTF-16LE encoded, zlib-compressed, and byte-read through `SmHouseScripts`. Exact Java compressed bytes are not golden-compared. |
| `com.aionemu.gameserver.utils.xml.CompressUtil.compress` | `Aion.GameServer.Model.GameObjects.PlayerScripts.Compress` | Utility | Partial | Unit Tested | Partial Parity | Mirrors zlib compression semantics for C# packet emission; Java byte identity remains unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_HOUSE_SCRIPTS` | `Aion.GameServer.Network.Aion.ServerPackets.SmHouseScripts` | Server packet | Partial | Unit Tested | Partial Parity | Sandbox login packet plus single-script update/removal payloads are tested. Split-list multi-script flow remains missing. |

## Next Recommended Runtime UOW

Restore saved house scripts from `house_scripts` during player house load.

Runtime progress gate for the next UOW:

```text
- Deferred/live behavior to advance: persisted house script XML rows should be restored into PlayerHouse.Scripts when player houses are loaded.
- Java source/runtime path: HouseScriptsDAO.getPlayerScripts -> addScript -> PlayerScripts.set(..., storeInDb=false); House.getPlayerScripts lazy restore.
- C# runtime artifact likely involved: MySqlPlayerEnterWorldRepository.LoadPlayerHousesAsync, PlayerScripts restore helper, PlayerHouse.Scripts, focused repository/service tests.
- Client-visible/state/persistence effect expected: saved house script state survives relog and is available to live CM_HOUSE_SCRIPT/runtime house script paths.
- Why this is not preview-only/test-only/documentation-only: it restores persisted runtime state from the Java database shape into live player house structures.
```

Suggested focused validation:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~CmHouseScriptTests" --no-restore
```

Narrow this after inspecting the exact test class or helper that can prove loaded `PlayerHouse.Scripts`; a repository helper/unit test may be enough if it exercises the live load path. Java/Maven is not expected unless a narrow Java fixture is added or found.

## Blockers / Watchouts

- Do not invent a saved-script send-on-login path unless a Java source call is found. In this checkout, the only direct enter-world `SM_HOUSE_SCRIPTS` send found was the Lua sandbox fix packet.
- Restore-on-load must use Java's UTF-16LE XML plus zlib compression and script id limit.
- Keep exact compressed-byte parity conservative unless backed by a Java golden.
