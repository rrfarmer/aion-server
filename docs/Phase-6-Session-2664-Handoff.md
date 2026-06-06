# Phase 6 Session 2664 Handoff

## Completed UOW

[Phase 6] UOW-2664: Persist and broadcast house scripts.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: live CM_HOUSE_SCRIPT normal save/remove now validates active-house ownership, mutates PlayerScripts, persists/deletes house_scripts, and broadcasts SM_HOUSE_SCRIPTS.
- Java source/runtime path: CM_HOUSE_SCRIPT.runImpl -> PlayerScripts.set/remove -> HouseScriptsDAO.storeScript/deleteScript -> SM_HOUSE_SCRIPTS.writeImpl.
- C# runtime artifact wired: GameServerConnection.HandleHouseScriptAsync, PlayerScripts/PlayerScript, IHousingRepository/MySqlHousingRepository script methods, SmHouseScripts.
- Client-visible/state/persistence effect: valid house-script edits now change runtime house state, touch the Java-compatible DB table, and fan out update/removal packets to visible players.
- Why this is runtime progress: this changes live packet dispatch, state, persistence, and server packet fanout.
```

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Data/HousingRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/PlayerHouse.cs`
- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/PlayerScripts.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmHouseScripts.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmHouseScriptTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionBuyItemTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/HousingWorldServiceTests.cs`
- `docs/Phase-6-Session-2664-Completion.md`
- `docs/Phase-6-Session-2664-Handoff.md`

## Validation

Command run:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmHouseScriptTests" --no-restore
```

Result:

- Final run passed: 7
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

No Java/Maven command was run; no narrow Java `CM_HOUSE_SCRIPT.runImpl` persistence/fanout fixture exists in this checkout.

## Conservative Parity Status

- `CM_HOUSE_SCRIPT` now has live partial parity for overflow, ownership guard, normal save, normal remove, runtime state mutation, persistence call, and single-script `SM_HOUSE_SCRIPTS` fanout.
- Full house-script parity is not complete because restore-on-load, split-list send-to-player, `LUA_SANDBOX_FIX`, delete-all, real database integration, and real-client rendering remain unverified or unported.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_HOUSE_SCRIPT` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleHouseScriptAsync` | Client packet handler | Partial | Unit Tested | Partial Parity | Live save/remove now works for already-loaded active houses. Audit details, new-house pre-save, restore-on-load, and real-client behavior remain incomplete. |
| `com.aionemu.gameserver.model.gameobjects.player.PlayerScripts` | `Aion.GameServer.Model.GameObjects.PlayerScripts` | Runtime state | Partial | Unit Tested | Partial Parity | Script slots, set/remove/get, zlib decompression, UTF-16LE decode, and size validation are represented. Split-list send/load/delete-all/logging are missing. |
| `com.aionemu.gameserver.model.house.PlayerScript` | `Aion.GameServer.Model.GameObjects.PlayerScript` | DTO / runtime state | Partial | Unit Tested | Partial Parity | Fields and `HasData` are represented; `LUA_SANDBOX_FIX` is not ported. |
| `com.aionemu.gameserver.dao.HouseScriptsDAO` | `Aion.GameServer.Data.IHousingRepository` / `MySqlHousingRepository` script methods | Repository | Partial | Unit Tested with fake | Partial Parity | Store/delete use the Java table shape. Load and delete-all are missing; real DB execution is unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_HOUSE_SCRIPTS` | `Aion.GameServer.Network.Aion.ServerPackets.SmHouseScripts` | Server packet | Partial | Unit Tested | Partial Parity | Single-script update/removal payload is byte-tested. Multi-script split-list flow is not ported. |

## Next Recommended Runtime UOW

Restore and send saved house scripts during live player house load/enter-world.

Runtime progress gate for the next UOW:

```text
- Deferred/live behavior to advance: saved house scripts should be restored from house_scripts into PlayerHouse.Scripts and sent to the owning client when entering the world or loading house owner info.
- Java source/runtime path: HouseScriptsDAO.getPlayerScripts -> House constructor/player house load -> PlayerScripts.sendToPlayer; also inspect PlayerEnterWorldService's SM_HOUSE_SCRIPTS login send.
- C# runtime artifact likely involved: PlayerEnterWorldRepository/HousingRepository load flow, PlayerHouse.Scripts, GameServerConnection enter-world packet sequence, SmHouseScripts.
- Client-visible/state/persistence effect expected: saved scripts survive relog and the client receives restored script packets.
- Why this is not preview-only/test-only/documentation-only: it restores persisted runtime state and sends live server packets from the enter-world flow.
```

Suggested focused validation:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmHouseScriptTests|FullyQualifiedName~PlayerEnterWorld" --no-restore
```

Narrow this further after inspecting the exact edited load/enter-world tests. Java/Maven is not expected unless a narrow Java fixture for script restore/send exists or is added.

## Blockers / Watchouts

- Do not add script readiness/planner scaffolding.
- Do not claim full `CM_HOUSE_SCRIPT` or `SM_HOUSE_SCRIPTS` parity.
- Restore-on-load must respect Java UTF-16LE + Deflater/ZLib byte behavior and script slot limit.
- Multi-script split-list behavior may require careful packet body sizing; keep any next UOW narrow.
