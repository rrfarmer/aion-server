# Phase 6 Session 2728 Handoff

## Completed UOW

[Phase 6] UOW-2728: Send cannot-use legion warehouse denial.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: unsupported OPEN_LEGION_WAREHOUSE selections by legion members send Java's cannot-use-legion-storage system message instead of silently returning.
- Java source/runtime path: DialogService.onDialogSelect OPEN_LEGION_WAREHOUSE -> LegionService.openLegionWarehouse -> LegionService.canOpenWarehouse unsupported-action branch.
- C# runtime artifact wired: GameServerConnection.HandleOpenLegionWarehouseDialogAsync and SmSystemMessage.CantUseGuildStorage.
- Client-visible/state/persistence effect: real clients receive `STR_CANT_USE_GUILD_STORAGE` system-message id `1300279` from live dialog dispatch.
- Why this is runtime progress: it sends a real server packet from a live client packet path.
```

## Commit

`[Phase 6][UOW-2728] Send cannot-use legion warehouse denial`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionStorageExpansionDialogTests.cs`
- `docs/Phase-6-Session-2728-Completion.md`
- `docs/Phase-6-Session-2728-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/services/DialogService.java`
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
- `game-server/src/com/aionemu/gameserver/model/templates/npc/NpcTemplate.java` action support behavior by call site

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.HandleOpenLegionWarehouseDialogAsync`
- `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage`
- `Aion.GameServer.Services.NpcDialogTargetingService` as an existing targeting dependency
- `Aion.GameServer.Tests.GameServerConnectionStorageExpansionDialogTests`
- `Aion.GameServer.Tests.GamePacketTests`

## Validation

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionStorageExpansionDialogTests|FullyQualifiedName~GamePacketTests|FullyQualifiedName~NpcDialogTargetingServiceTests" --logger "console;verbosity=minimal"
```

Result:

- Passed: 310
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

- Not run. Java source was reviewed unchanged, and no narrow Java fixture exists for the `LegionService.canOpenWarehouse` unsupported-action branch in this checkout.

Broad .NET:

- Not run. Broad-validation trigger was live dialog packet dispatch, but the focused command compiled the affected project and decoded the emitted denial packet payload from the live handler.

## Conservative Parity Status

- `OPEN_LEGION_WAREHOUSE` now has partial runtime parity for success, no-legion denial, no-permission denial, and unsupported-action denial.
- Full `LegionService.canOpenWarehouse` parity is not claimed because config-disabled, disbanding, in-use locking, and a full shared `LegionWarehouse` aggregate remain missing.
- The touched system message id matches reviewed Java source, but exact Java golden bytes were not captured.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `DialogService.onDialogSelect` | `GameServerConnection.HandleDialogSelectAsync` | Live client packet handler | Partial | Unit Tested | Partial Parity | `OPEN_LEGION_WAREHOUSE` success/no-legion/no-right/unsupported-action branches are live; other dialog actions remain mixed live/non-live. |
| `LegionService.canOpenWarehouse` | `GameServerConnection.HandleOpenLegionWarehouseDialogAsync` | Service side effect | Partial | Unit Tested | Partial Parity | Membership, unsupported-action, and permission denial sends are covered; config-disabled, disbanding, and in-use branches remain gaps. |
| `SM_SYSTEM_MESSAGE.STR_CANT_USE_GUILD_STORAGE` | `SmSystemMessage.CantUseGuildStorage` | Server packet helper | Complete | Unit Tested | Partial Parity | Java id `1300279` verified by packet decoder and live dispatch; no Java golden bytes. |
| `NpcTemplate.supportsAction(DialogAction.OPEN_LEGION_WAREHOUSE)` | `NpcTemplateSummary.SupportsDialogAction` / `NpcDialogTargetingService` | Target/action guard | Partial | Unit Tested | Partial Parity | Existing unsupported-action result is consumed in Java guard order for this branch; no template model changes were made. |

## Known Gaps / Watchouts

- No live C# equivalent for Java `LegionConfig.LEGION_WAREHOUSE` is checked in this path.
- No live C# legion disbanding state is wired for `STR_GUILD_WAREHOUSE_CANT_USE_WHILE_DISPERSE`.
- No live shared `LegionWarehouse` in-use lock is modeled for `STR_GUILD_WAREHOUSE_IN_USE`.
- `CM_CLOSE_DIALOG` currently closes mailbox state live, but AI dialog-finish and legion warehouse lock release are still non-live.
- C# still represents legion warehouse items through `Player.InventoryItems` location `3`.

## Next Recommended Runtime UOW

Recommended candidate: add a small live legion warehouse in-use state for open/close paths, then send Java's in-use denial when another player has the warehouse locked.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: successful OPEN_LEGION_WAREHOUSE should mark the legion warehouse in use, CM_CLOSE_DIALOG should release it, and another player should receive Java's in-use denial.
- Java source/runtime path: LegionWarehouse.setInUse/getCurrentUser/unsetInUse, LegionService.canOpenWarehouse in-use branch, and DialogService.onCloseDialog release branch.
- C# runtime artifact likely involved: a small runtime state holder keyed by legion id, GameServerConnection.HandleOpenLegionWarehouseDialogAsync, GameServerConnection.HandleCloseDialog, and SmSystemMessage helper for `STR_GUILD_WAREHOUSE_IN_USE` id `1300280`.
- Client-visible/state/persistence effect expected: clients sharing a legion get Java-equivalent lock behavior; the first opener receives open packets, concurrent opener receives an in-use denial, and close releases the lock.
- Why this is runtime progress: it mutates live runtime state and sends a real server denial packet from live client packet paths.
```

Suggested discovery:

```powershell
rg -n "CmCloseDialog|HandleCloseDialog|NpcDialogCloseSideEffectPlanService|LegionWarehouse\\.setInUse|unsetInUse|getCurrentUser|STR_GUILD_WAREHOUSE_IN_USE|WarehouseInUse|LegionWarehouseRuntime|LegionId" game-server/src/com/aionemu/gameserver dotnetConversion/src/Aion.GameServer dotnetConversion/tests/Aion.GameServer.Tests
```

Suggested focused validation:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionStorageExpansionDialogTests|FullyQualifiedName~GamePacketTests|FullyQualifiedName~NpcDialogSideEffectServiceTests" --logger "console;verbosity=minimal"
```

Narrow after discovery. Java/Maven is not expected unless a narrow Java fixture is found. Broad-validation trigger: live runtime state mutation and live dialog packet dispatch; start focused.

## Other Safe Runtime Candidates

- Add live disbanding-state denial once a C# legion disbanding flag/source exists.
- Add Java-derived legion warehouse capacity checks to move/split if current C# can overfill location `3`.
- Add exact Java golden bytes for the legion warehouse system messages if a narrow Java fixture is created.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits before this UOW:
  - `72f4a7abb [Phase 6][UOW-2727] Send legion warehouse open denials`
  - `1960af595 [Phase 6][UOW-2726] Open live legion warehouse dialog`
  - `8f276bbf4 [Phase 6][UOW-2725] Use legion level for warehouse size packets`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
