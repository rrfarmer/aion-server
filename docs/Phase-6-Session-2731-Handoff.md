# Phase 6 Session 2731 Handoff

## Completed UOW

[Phase 6] UOW-2731: Deny legion warehouse while legion is disbanding.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: live OPEN_LEGION_WAREHOUSE now rejects disbanding legions.
- Java source/runtime path: LegionDAO.loadLegion -> Legion.setDisbandTime; Legion.isDisbanding; LegionService.canOpenWarehouse disbanding branch.
- C# runtime artifact wired: Player.LegionDisbandTime/IsLegionDisbanding, MySqlPlayerEnterWorldRepository.LoadPlayerAsync, GameServerConnection.HandleOpenLegionWarehouseDialogAsync, and SmSystemMessage.GuildWarehouseCantUseWhileDisbanding.
- Client-visible/state/persistence effect: existing DB column `legions.disband_time` is hydrated into live player state and drives a real system-message id `1300333` from live dialog dispatch.
- Why this is runtime progress: it loads existing database state into runtime C# structures used by live code and sends a real server packet from a live client path.
```

## Commit

`[Phase 6][UOW-2731] Deny disbanding legion warehouse opens`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/Player.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionStorageExpansionDialogTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldRepositoryDatabaseIntegrationTests.cs`
- `docs/Phase-6-Session-2731-Completion.md`
- `docs/Phase-6-Session-2731-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/dao/LegionDAO.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/Legion.java`
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
- `game-server/sql/aion_gs.sql`

## C# Artifacts Touched

- `Aion.GameServer.Model.GameObjects.Player`
- `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository`
- `Aion.GameServer.Network.Aion.GameServerConnection.HandleOpenLegionWarehouseDialogAsync`
- `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage`
- `Aion.GameServer.Tests.GameServerConnectionStorageExpansionDialogTests`
- `Aion.GameServer.Tests.GamePacketTests`
- `Aion.GameServer.Tests.PlayerEnterWorldRepositoryDatabaseIntegrationTests`

## Validation

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionStorageExpansionDialogTests|FullyQualifiedName~GamePacketTests|FullyQualifiedName~PlayerEnterWorldRepositoryDatabaseIntegrationTests" --logger "console;verbosity=minimal"
```

Result:

- Passed: 329
- Failed: 0
- Skipped: 0
- Existing nullable/analyzer warnings only.

Repository hygiene:

```powershell
git diff --check
```

Result:

- Passed with only Git CRLF working-copy warnings for touched files.

Java/Maven:

- Not run. Java source and schema were reviewed unchanged; no narrow Java fixture exists for this guard branch in the checkout.

Broad .NET:

- Not run. Broad-validation trigger was live dialog dispatch plus DB runtime projection, but the focused command compiled the affected project and exercised the changed live branch. Actual DB execution remains opt-in.

## Conservative Parity Status

- `OPEN_LEGION_WAREHOUSE` now has partial runtime parity for success, no-legion denial, unsupported-action/config-style denial when target unsupported, disbanding denial, no-permission denial, in-use denial, same-user reopen, close release, and logout release.
- Full `LegionService.canOpenWarehouse` parity is not claimed because Java `LegionConfig.LEGION_WAREHOUSE` is not yet wired in C#.
- Full Java `Legion` parity is not claimed; C# only projects the disband time needed by live behavior.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `LegionDAO.loadLegion` | `MySqlPlayerEnterWorldRepository.LoadPlayerAsync` | Repository / runtime hydration | Partial | Opt-in Integration Covered | Partial Parity | Projects `legions.disband_time`; full Java Legion loading remains incomplete. |
| `Legion.isDisbanding` | `Player.IsLegionDisbanding` | Runtime model | Partial | Unit Tested via live handler | Partial Parity | Uses `LegionDisbandTime > 0`; does not model full Java `Legion`. |
| `LegionService.canOpenWarehouse` | `GameServerConnection.HandleOpenLegionWarehouseDialogAsync` | Live client packet handler branch | Partial | Unit Tested | Partial Parity | Disbanding branch now live; config-disabled branch remains missing. |
| `SM_SYSTEM_MESSAGE.STR_GUILD_WAREHOUSE_CANT_USE_WHILE_DISPERSE` | `SmSystemMessage.GuildWarehouseCantUseWhileDisbanding` | Server packet helper | Complete | Unit Tested | Partial Parity | Java id `1300333` verified by packet decoder and live dispatch; no Java golden bytes. |

## Known Gaps / Watchouts

- Java `LegionConfig.LEGION_WAREHOUSE` is not wired into C# options or the live open guard.
- CM_LEGION leave/kick/member-removal remains deferred in C#; do not select that UOW until a live path exists.
- Opt-in DB integration assertions for `disband_time` were updated but not executed here.
- C# still uses `Player.InventoryItems` location `3` rather than a full Java `LegionWarehouse` aggregate.

## Next Recommended Runtime UOW

Recommended candidate: wire the Java legion warehouse enabled config into C# and the live open guard.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: disabling `gameserver.legion.warehouse` should reject live OPEN_LEGION_WAREHOUSE before disbanding/permission/in-use/open behavior.
- Java source/runtime path: LegionConfig.LEGION_WAREHOUSE, game-server/config/main/legions.properties, LegionService.canOpenWarehouse `!LegionConfig.LEGION_WAREHOUSE || !npc.supportsAction(...)` -> STR_CANT_USE_GUILD_STORAGE.
- C# runtime artifact likely involved: GameServerOptions/GameServerCustomOptions or a new legion option group, GameServerOptions.Load property mapping, GameServerConnection.HandleOpenLegionWarehouseDialogAsync, and existing SmSystemMessage.CantUseGuildStorage.
- Client-visible/state/persistence effect expected: with the C# option disabled, a live warehouse-open dialog selection sends system-message id `1300279` instead of opening or locking the warehouse.
- Why this is runtime progress: it loads Java config into runtime C# options and sends a real server packet from live dialog dispatch.
```

Suggested focused validation:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerOptionsTests|FullyQualifiedName~GameServerConnectionStorageExpansionDialogTests|FullyQualifiedName~GamePacketTests" --logger "console;verbosity=minimal"
```

Adjust the options test class name after discovery if needed. Java/Maven is not expected unless Java source or fixtures change. Broad-validation trigger: live config loading plus live dialog packet dispatch; focused command should be enough.

## Other Safe Runtime Candidates

- Add Java-derived legion warehouse capacity checks to move/split if current C# can overfill location `3`.
- Persist Java-equivalent legion warehouse item state from a live logout path if the current database shape and C# item location `3` behavior can be safely wired.
- Revisit legion leave/kick/member-removal lock release only after CM_LEGION or an equivalent membership-removal path becomes live.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits before this UOW:
  - `0912734a4 [Phase 6][UOW-2730] Release legion warehouse lock on logout`
  - `1c5ee008b [Phase 6][UOW-2729] Lock live legion warehouse opens`
  - `e6db023a4 [Phase 6][UOW-2728] Send cannot-use legion warehouse denial`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
