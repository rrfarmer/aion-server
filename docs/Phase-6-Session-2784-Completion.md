# Phase 6 Session 2784 Completion

## Completed UOW

[Phase 6][UOW-2784] Sync legion bonus icon on player login

## Runtime Progress Gate

- Deferred/live behavior advanced: live enter-world now mirrors Java `LegionService.onLogin` for legion bonus icon state.
- Java source of truth: `game-server/src/com/aionemu/gameserver/services/LegionService.java` `onLogin(Player)`, `game-server/src/com/aionemu/gameserver/model/team/legion/Legion.java` `addBonus()`, and `SM_ICON_INFO`.
- C# runtime artifact wired: `GameServerConnection` successful `CmEnterWorld` path after the active player is registered online.
- Client-visible/state effect: logging into an already-bonused legion sends `SmIconInfo(1, true)` to the entering client; logging in as the tenth online legion member activates `LegionBonusRuntime` and sends icon-on packets to the entering player and online legion members.
- Why this is runtime progress: this changes live login packet fanout and runtime legion bonus state, not a preview, metadata, documentation, or test-only path.

## Implementation

- Added `SyncLegionBonusOnLoginAsync(Player)` to `GameServerConnection`.
- Called the sync from successful `CmEnterWorld` after `_activePlayer` is assigned and registered in the connection registry.
- Reused `LegionBonusRuntime.IsActive` and the existing activation helper so the branch matches Java:
  - active bonus: send icon-on only to the entering player;
  - inactive bonus: call the activation path, which flips state and broadcasts at the ten-online threshold.

## Tests

- Added `ProcessPacketAsync_CmEnterWorldSendsActiveLegionBonusIconLikeJavaOnLogin`.
- Added `ProcessPacketAsync_CmEnterWorldActivatesLegionBonusAtJavaOnlineThreshold`.
- Both tests exercise the decoded `CM_ENTER_WORLD` path through `ProcessPacketAsync`, `PlayerEnterWorldService`, and the live server-packet observer.

## Validation

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~PlayerEnterWorld" --logger "console;verbosity=minimal" --no-restore
```

Result: Passed, 210 total, 0 failed, 0 skipped. Existing nullable/analyzer warnings remain outside this UOW.

Java/Maven validation: not run; no narrow Java fixture exists for `SM_ICON_INFO` login fanout. Java source was reviewed directly.

## Remaining Gaps

- Logout/disconnect bonus deactivation is still not wired through the live leave-world path.
- XP-rate consumption of active legion bonus state remains missing.
- `SM_ICON_INFO` still has no Java golden fixture.

