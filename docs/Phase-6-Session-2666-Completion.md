# Phase 6 Session 2666 Completion

## UOW

[Phase 6] UOW-2666: Restore loaded house scripts.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: player house load now restores persisted house_scripts rows into live PlayerHouse.Scripts state.
- Java source/runtime path: HouseScriptsDAO.getPlayerScripts -> addScript -> PlayerScripts.set(..., storeInDb=false); House.getPlayerScripts lazy restore.
- C# runtime artifact wired: MySqlPlayerEnterWorldRepository.LoadPlayerHousesAsync, AttachHouseScriptsAsync, RestoreHouseScripts, and PlayerScripts.RestoreFromXml.
- Client-visible/state/persistence effect: saved house script XML survives relog/player-house load and is available to live CM_HOUSE_SCRIPT/SM_HOUSE_SCRIPTS runtime paths.
- Why this is runtime progress: this restores persisted runtime state from Java's house_scripts table shape into live player house objects.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/dao/HouseScriptsDAO.java`
  - `getPlayerScripts(int houseId)` creates `PlayerScripts`, selects `script_id, script` for one house ordered by `date_added`, and calls `addScript`.
  - `addScript` treats null/empty XML as an empty script and otherwise encodes XML as UTF-16LE, compresses it, and calls `scripts.set(..., false)`.
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/PlayerScripts.java`
  - `set(..., storeInDb=false)` validates script id and decompressed size before installing the runtime `PlayerScript`.
- `game-server/src/com/aionemu/gameserver/model/house/House.java`
  - `getPlayerScripts` lazily calls `reloadPlayerScripts`, which delegates to `HouseScriptsDAO.getPlayerScripts`.

## C# Changes

- Added `PlayerScripts.RestoreFromXml` for Java `HouseScriptsDAO.addScript` semantics:
  - null/empty XML restores an empty slot,
  - non-empty XML is UTF-16LE encoded, zlib-compressed, then validated through `Set`.
- Updated `MySqlPlayerEnterWorldRepository.LoadPlayerHousesAsync` to query `house_scripts` for loaded houses and restore rows into each `PlayerHouse.Scripts`.
- Added `RestoreHouseScripts` and `HouseScriptRestoreRow` as a focused helper used by the live repository path and tests.

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `RestoreHouseScripts_CompressesUtf16XmlIntoMatchingPlayerScriptSlot` | Unit / runtime restore helper | `HouseScriptsDAO.addScript`, `PlayerScripts.set(..., false)` | DB XML is UTF-16LE encoded, zlib-compressed, installed in the requested script slot, and decodes back to the original XML. | Exercises the same helper called by `LoadPlayerHousesAsync`. | Does not execute against a real MySQL database. |
| `RestoreHouseScripts_EmptyXmlRestoresJavaEmptyScriptSlot` | Unit / runtime restore helper | `HouseScriptsDAO.addScript` empty branch | Empty XML restores an empty runtime script with size 0. | Covers Java empty-string behavior. | Does not cover null DB values because Java schema marks script NOT NULL. |
| `RestoreHouseScripts_IgnoresUnknownHouseAndInvalidScriptIdLikeJava` | Unit / runtime restore helper | `PlayerScripts.isInvalidScriptId` | Unknown house rows and out-of-range script ids do not mutate slots. | Covers Java invalid-id guard and C# house-id filter behavior. | Does not assert logging. |

## Validation Decision

```text
- Changed surface: runtime persistence restore and shared PlayerScripts state.
- Specific behavior/contract: Java HouseScriptsDAO.getPlayerScripts restores UTF-16LE XML rows into compressed PlayerScripts slots without re-storing to DB.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldRepositoryHouseScriptRestoreTests|FullyQualifiedName~CmHouseScriptTests" --no-restore
- Focused Java/Maven command: not run; no narrow Java fixture exists for HouseScriptsDAO.getPlayerScripts in this checkout, and Java source was reviewed directly.
- Broad-validation trigger: persistence restore path and shared runtime state changed.
- Broad .NET decision: skipped after focused validation because the filtered tests compiled the changed project and covered restore semantics plus adjacent live CM_HOUSE_SCRIPT state/packet behavior.
- Why this scope is sufficient: the repository change is limited to loading house_scripts rows into PlayerHouse.Scripts; the tests verify the Java restore semantics used by that live path and protect the existing live script handler.
```

Results:

- Focused C# validation passed: 10/10 tests.
- Existing nullable/analyzer warnings were emitted in unrelated surfaces on the first focused run.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.dao.HouseScriptsDAO.getPlayerScripts` / `addScript` | `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.AttachHouseScriptsAsync` / `RestoreHouseScripts` | Repository restore | Partial | Unit Tested | Partial Parity | C# restores rows for all loaded player houses from Java's `house_scripts` table shape and orders rows by house/date. Real MySQL execution and per-house lazy-loading behavior are unverified. |
| `com.aionemu.gameserver.model.gameobjects.player.PlayerScripts.set(..., false)` | `Aion.GameServer.Model.GameObjects.PlayerScripts.RestoreFromXml` | Runtime state | Partial | Unit Tested | Partial Parity | UTF-16LE XML compression, empty-slot restore, and invalid script id guard are covered. Logging details and exact Java compressed-byte identity remain unverified. |
| `com.aionemu.gameserver.model.house.House.getPlayerScripts` lazy restore | `Aion.GameServer.Model.GameObjects.PlayerHouse.Scripts` loaded by `LoadPlayerHousesAsync` | Runtime state loading | Partial | Unit Tested | Partial Parity | C# restores during player house load instead of Java's lazy per-house getter. State effect is equivalent for loaded houses; lazy timing differs and is documented. |

## Known Gaps

- Restore is unit-tested through the live repository helper but not against a real MySQL instance.
- Java lazy-load timing is not identical; C# eagerly restores scripts during player house load.
- `PlayerScripts.sendToPlayer` split-list behavior remains unported.
- Exact Java zlib byte identity is not golden-compared.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 3
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%

## Next Runtime UOW Candidates

1. Port Java `PlayerScripts.sendToPlayer` split-list send behavior only if a live C# call site can be found or introduced from a Java-backed runtime path.
2. Add a narrow Java/C# compression golden comparison for house scripts if exact compressed bytes become a client-visible blocker.
3. Find another deferred live client packet branch that can send a deterministic packet or mutate already-modeled runtime state.
