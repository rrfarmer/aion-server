# Phase 6 Session 2666 Handoff

## Completed UOW

[Phase 6] UOW-2666: Restore loaded house scripts.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: persisted house_scripts XML rows are restored into PlayerHouse.Scripts during player house load.
- Java source/runtime path: HouseScriptsDAO.getPlayerScripts -> addScript -> PlayerScripts.set(..., storeInDb=false); House.getPlayerScripts lazy restore.
- C# runtime artifact wired: MySqlPlayerEnterWorldRepository.LoadPlayerHousesAsync, AttachHouseScriptsAsync, RestoreHouseScripts, PlayerScripts.RestoreFromXml.
- Client-visible/state/persistence effect: saved house scripts survive player load/relog and become available to live CM_HOUSE_SCRIPT/SM_HOUSE_SCRIPTS runtime paths.
- Why this is runtime progress: this restores persisted runtime state from Java's existing house_scripts table shape into live player house state.
```

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/PlayerScripts.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldRepositoryHouseScriptRestoreTests.cs`
- `docs/Phase-6-Session-2666-Completion.md`
- `docs/Phase-6-Session-2666-Handoff.md`

## Validation

Commands run:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldRepositoryHouseScriptRestoreTests" --no-restore
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldRepositoryHouseScriptRestoreTests|FullyQualifiedName~CmHouseScriptTests" --no-restore
```

Result:

- Final focused run passed: 10
- Failed: 0
- Skipped: 0

No Java/Maven command was run; no narrow Java fixture exists for `HouseScriptsDAO.getPlayerScripts` in this checkout.

## Conservative Parity Status

- Saved house script XML is restored into C# runtime script slots during player house load.
- Full house-script parity remains partial because split-list send-to-player, exact Java zlib golden comparison, real DB execution, and real-client rendering are still unverified or unported.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.dao.HouseScriptsDAO.getPlayerScripts` / `addScript` | `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.AttachHouseScriptsAsync` / `RestoreHouseScripts` | Repository restore | Partial | Unit Tested | Partial Parity | Restores `house_scripts` rows into loaded houses. Real MySQL execution and Java lazy-loading timing are unverified. |
| `com.aionemu.gameserver.model.gameobjects.player.PlayerScripts.set(..., false)` | `Aion.GameServer.Model.GameObjects.PlayerScripts.RestoreFromXml` | Runtime state | Partial | Unit Tested | Partial Parity | Handles UTF-16LE XML compression, empty script rows, and invalid ids. Logging and exact compressed bytes remain unverified. |
| `com.aionemu.gameserver.model.house.House.getPlayerScripts` | `Aion.GameServer.Model.GameObjects.PlayerHouse.Scripts` loaded by `LoadPlayerHousesAsync` | Runtime state loading | Partial | Unit Tested | Partial Parity | C# restores eagerly during player house load; Java restores lazily per house. State availability for loaded houses is advanced. |

## Next Recommended Runtime UOW

Do a fresh Work Discovery pass over deferred live packet branches and runtime-loading gaps. A good first candidate is a live `PlayerScripts.sendToPlayer` equivalent only if a Java-backed C# call site is found; otherwise pick another smaller deferred client/server packet or persistence state gap.

Runtime progress gate for the next candidate must name:

```text
- Deferred/live behavior to advance:
- Java source/runtime path:
- C# runtime artifact likely involved:
- Client-visible/state/persistence effect expected:
- Why this is not preview-only/test-only/documentation-only:
```

Suggested focused validation starting points:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmHouseScriptTests|FullyQualifiedName~PlayerEnterWorldRepositoryHouseScriptRestoreTests" --no-restore
```

Use that only for further house-script runtime work. For a different packet/state slice, choose the nearest edited test class instead. Java/Maven is not expected unless a narrow Java fixture exists or is added.

## Blockers / Watchouts

- Do not add a speculative saved-script send-on-login path unless a Java source call is found. The only direct enter-world `SM_HOUSE_SCRIPTS` send found in this checkout was the Lua sandbox packet from UOW-2665.
- `PlayerScripts.sendToPlayer` uses Java dynamic split-list sizing; port it only when there is a live call site.
- Keep exact zlib byte parity conservative until there is Java golden evidence.
