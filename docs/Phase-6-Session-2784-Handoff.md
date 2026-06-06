# Phase 6 Session 2784 Handoff

## Latest Completed UOW

[Phase 6][UOW-2784] Sync legion bonus icon on player login

## Current State

- `SmIconInfo` exists and is used by live legion bonus activation, deactivation, and login sync paths.
- `LegionBonusRuntime` tracks active bonus state by legion id.
- Invite acceptance activates the bonus at ten online legion members and sends icon-on packets.
- Explicit live self-leave and kick paths send icon-off packets and clear runtime bonus state when online membership falls below ten.
- Successful enter-world now follows Java `LegionService.onLogin`: it sends icon-on to the entering player when the bonus is already active, or activates and fans out icon-on when the login reaches the ten-online threshold.
- Focused C# validation passed:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~PlayerEnterWorld" --logger "console;verbosity=minimal" --no-restore
```

Result: Passed, 210 total, 0 failed, 0 skipped. Existing nullable/analyzer warnings remain outside this UOW.

## Runtime Progress Gate For Next UOW

Recommended next UOW: `[Phase 6][UOW-2785] Wire legion bonus deactivation on logout`

- Deferred/live behavior advanced: Java `LegionService.onLogout` calls `legion.removeBonus()` after storing member state and releasing legion warehouse use.
- Java source of truth: `LegionService.onLogout(Player)` and `Legion.removeBonus()`.
- C# runtime artifact to wire/fix: `GameServerConnection.LeavePlayerWorldAsync` / live disconnect path after unregistering the active player from online lookups, reusing `RemoveLegionBonusIfEligibleAsync`.
- Client-visible/state effect: when an online member disconnects and legion online count drops below ten, remaining online legion members receive `SmIconInfo(1, false)` and `LegionBonusRuntime` is cleared.
- Why this is not preview-only/test-only/documentation-only: it sends real server packets and mutates live legion bonus runtime state from the disconnect/leave-world path.

## Safe Runtime Candidates

1. Wire logout/disconnect bonus deactivation from Java `LegionService.onLogout` through live `LeavePlayerWorldAsync`.
2. Connect quest/combat XP reward code to `LegionBonusRuntime.IsActive`, matching `Rates.calcXpRate`'s 1.1x legion bonus.
3. Add a narrow Java-side golden fixture for `SM_ICON_INFO` only if it directly unblocks packet parity confidence for runtime packet fanout.

## Suggested Focused Validation

Specific behavior to validate: live disconnect/leave-world deactivates the legion bonus when the active member falling offline leaves fewer than ten online legion members, and sends icon-off only to remaining online legion members.

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~PlayerEnterWorld" --logger "console;verbosity=minimal" --no-restore
```

Java/Maven: not expected unless a narrow Java fixture is added; Java source should be reviewed.

Broad-validation trigger: live disconnect packet fanout and runtime state mutation. Start focused and document whether broader validation is still needed.

## Known Gaps

- Logout/disconnect bonus deactivation remains missing.
- XP-rate consumption of active legion bonus state remains missing.
- No Java golden fixture exists for `SM_ICON_INFO`.

## Files Touched In Session 2784

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmLegionTests.cs`
- `docs/Phase-6-Session-2784-Completion.md`
- `docs/Phase-6-Session-2784-Handoff.md`

