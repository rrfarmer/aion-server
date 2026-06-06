# Phase 6 Session 2727 Handoff

## Completed UOW

[Phase 6] UOW-2727: Send legion warehouse open denials.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: invalid OPEN_LEGION_WAREHOUSE selections send Java denial packets instead of silently returning for no-legion and no-permission branches.
- Java source/runtime path: DialogService.onDialogSelect OPEN_LEGION_WAREHOUSE -> LegionService.openLegionWarehouse -> LegionService.canOpenWarehouse membership/permission checks.
- C# runtime artifact wired: GameServerConnection.HandleOpenLegionWarehouseDialogAsync and SmSystemMessage.NoGuildToDeposit/GuildWarehouseNoRight.
- Client-visible/state/persistence effect: real clients receive `STR_NO_GUILD_TO_DEPOSIT` or `STR_GUILD_WAREHOUSE_NO_RIGHT` system-message packets from live dialog dispatch.
- Why this is runtime progress: it sends real server packets from a live client packet path.
```

## Commit

`[Phase 6][UOW-2727] Send legion warehouse open denials`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionStorageExpansionDialogTests.cs`
- `docs/Phase-6-Session-2727-Completion.md`
- `docs/Phase-6-Session-2727-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.HandleOpenLegionWarehouseDialogAsync`
- `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage`
- `Aion.GameServer.Tests.GameServerConnectionStorageExpansionDialogTests`
- `Aion.GameServer.Tests.GamePacketTests`

## Validation

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionStorageExpansionDialogTests|FullyQualifiedName~GamePacketTests" --logger "console;verbosity=minimal"
```

Result:

- Passed: 304
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

- Not run. Java source was reviewed unchanged, and no narrow Java fixture exists for the `LegionService.canOpenWarehouse` branches in this checkout.

Broad .NET:

- Not run. Broad-validation trigger was live dialog packet dispatch, but the focused command compiled the affected project and decoded both emitted denial packet payloads from the live handler.

## Conservative Parity Status

- `OPEN_LEGION_WAREHOUSE` now has partial runtime parity for the successful packet sequence, no-legion denial, and no-permission denial.
- Full `LegionService.canOpenWarehouse` parity is not claimed because config-disabled/unsupported NPC action, disbanding, in-use locking, and a full shared `LegionWarehouse` aggregate remain missing.
- The touched system message ids match reviewed Java source, but exact Java golden bytes were not captured.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `DialogService.onDialogSelect` | `GameServerConnection.HandleDialogSelectAsync` | Live client packet handler | Partial | Unit Tested | Partial Parity | `OPEN_LEGION_WAREHOUSE` success/no-legion/no-right branches are live; other dialog actions remain mixed live/non-live. |
| `LegionService.canOpenWarehouse` | `GameServerConnection.HandleOpenLegionWarehouseDialogAsync` | Service side effect | Partial | Unit Tested | Partial Parity | Membership and permission denial sends are covered; config-disabled, unsupported NPC action, disbanding, and in-use branches remain gaps. |
| `SM_SYSTEM_MESSAGE.STR_NO_GUILD_TO_DEPOSIT` | `SmSystemMessage.NoGuildToDeposit` | Server packet helper | Complete | Unit Tested | Partial Parity | Java id `1300278` verified by packet decoder and live dispatch; no Java golden bytes. |
| `SM_SYSTEM_MESSAGE.STR_GUILD_WAREHOUSE_NO_RIGHT` | `SmSystemMessage.GuildWarehouseNoRight` | Server packet helper | Complete | Unit Tested | Partial Parity | Java id `1300322` verified by packet decoder and live dispatch; no Java golden bytes. |

## Known Gaps / Watchouts

- Unsupported legion warehouse NPC/action currently returns from `NpcDialogTargetingService.ValidateTargetingNpcWithFunction` before Java's `STR_CANT_USE_GUILD_STORAGE` branch can send.
- No live C# equivalent for Java `LegionConfig.LEGION_WAREHOUSE` is checked in this path.
- No live C# legion disbanding state is wired for `STR_GUILD_WAREHOUSE_CANT_USE_WHILE_DISPERSE`.
- No live shared `LegionWarehouse` in-use lock is modeled for `STR_GUILD_WAREHOUSE_IN_USE`.
- C# still represents legion warehouse items through `Player.InventoryItems` location `3`.

## Next Recommended Runtime UOW

Recommended candidate: add live `STR_CANT_USE_GUILD_STORAGE` denial for `OPEN_LEGION_WAREHOUSE` when the player is a legion member but the selected NPC/action cannot use legion storage.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: unsupported or disabled OPEN_LEGION_WAREHOUSE selections should send Java's cannot-use-legion-storage system message instead of silently returning.
- Java source/runtime path: LegionService.canOpenWarehouse(Player, Npc) `!LegionConfig.LEGION_WAREHOUSE || !npc.getObjectTemplate().supportsAction(DialogAction.OPEN_LEGION_WAREHOUSE)` -> SM_SYSTEM_MESSAGE.STR_CANT_USE_GUILD_STORAGE.
- C# runtime artifact likely involved: GameServerConnection.HandleOpenLegionWarehouseDialogAsync, NpcDialogTargetingService targeting result handling, and SmSystemMessage helper for message id `1300279`.
- Client-visible/state/persistence effect expected: real clients receive a denial packet from the live dialog dispatch path when legion storage is not available on the selected target.
- Why this is runtime progress: it sends a real server packet from a live client packet path.
```

Suggested discovery:

```powershell
rg -n "ValidateTargetingNpcWithFunction|NpcDialogTargetingResult|STR_CANT_USE_GUILD_STORAGE|CantUseGuildStorage|OpenLegionWarehouse|supportsAction\\(DialogAction.OPEN_LEGION_WAREHOUSE" game-server/src/com/aionemu/gameserver dotnetConversion/src/Aion.GameServer dotnetConversion/tests/Aion.GameServer.Tests
```

Suggested focused validation:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionStorageExpansionDialogTests|FullyQualifiedName~GamePacketTests|FullyQualifiedName~NpcDialogTargetingServiceTests" --logger "console;verbosity=minimal"
```

Narrow after discovery. Java/Maven is not expected unless a narrow Java fixture is found. Broad-validation trigger: live dialog packet dispatch; start focused.

## Other Safe Runtime Candidates

- Model live legion warehouse in-use state and release it from close/logout/dialog-close paths.
- Add Java-derived legion warehouse capacity checks to move/split if current C# can overfill location `3`.
- Add exact Java golden bytes for the legion warehouse system messages if a narrow Java fixture is created.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits before this UOW:
  - `1960af595 [Phase 6][UOW-2726] Open live legion warehouse dialog`
  - `8f276bbf4 [Phase 6][UOW-2725] Use legion level for warehouse size packets`
  - `cbf4238b1 [Phase 6][UOW-2724] Persist legion warehouse item history`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
