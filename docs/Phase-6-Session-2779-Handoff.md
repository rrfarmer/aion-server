# Phase 6 Session 2779 Handoff

## Current State

The latest completed Unit of Work is `[Phase 6][UOW-2779] Honor legion cross-faction invite config`.

Live legion invite request handling now matches Java's cross-faction config branch:

- `gameserver.legion.inviteotherfaction` loads into `GameServerOptions.Legion.InviteOtherFactionEnabled`.
- The default remains false, so other-race invites still send Java message `1300311`.
- When enabled, other-race invites proceed to the normal live request/question path.

## Commit

Expected commit message:

```text
[Phase 6][UOW-2779] Honor legion cross-faction invite config
```

## Validation Evidence

Focused C# validation passed:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~GameServerOptions" --logger "console;verbosity=minimal" --no-restore
```

Result: Passed, 95 total, 0 failed, 0 skipped.

`git diff --check` passed with line-ending warnings only.

Focused Java/Maven validation was not run because Java source was unchanged and no narrow Java unit fixture exists for `LegionRestrictions.canInvitePlayer`.

## Latest Runtime Progress Gate

- Deferred/live behavior advanced: C# had a hardcoded cross-faction invite rejection and ignored Java's configurable allowance.
- Java source of truth: `LegionService.invitePlayerToLegion`, `LegionRestrictions.canInvitePlayer`, `LegionConfig.LEGION_INVITEOTHERFACTION`, and `SM_SYSTEM_MESSAGE.STR_GUILD_INVITE_CAN_NOT_INVITE_OTHER_RACE`.
- C# runtime artifact wired/fixed: `GameServerConnection.HandleLegionInviteAsync`, `GameServerOptions.Legion.InviteOtherFactionEnabled`, config loading, and focused tests.
- Client-visible/state/persistence effect changed: config-enabled cross-faction invites now send the normal invite question instead of the other-race denial.
- Why this was not preview-only/test-only/documentation-only: live invite request control flow now sends different real server packets based on Java config.

## Conservative Parity Status

| Java Artifact | C# Artifact | Status | Evidence | Remaining Gap |
| --- | --- | --- | --- | --- |
| `LegionRestrictions.canInvitePlayer` | `GameServerConnection.HandleLegionInviteAsync` | Partial parity | Tests cover default other-race rejection and config-enabled question send. | Java `DeniedStatus.GUILD` branch remains missing. |
| `LegionConfig.LEGION_INVITEOTHERFACTION` | `GameServerLegionOptions.InviteOtherFactionEnabled` | Partial parity | Config loader test asserts Java key. | No broader Java config golden. |
| `SM_SYSTEM_MESSAGE.STR_GUILD_INVITE_CAN_NOT_INVITE_OTHER_RACE` | `SmSystemMessage.GuildInviteCanNotInviteOtherRace` | Partial parity | Handler tests observe message id `1300311`. | No packet golden fixture. |

## Remaining Runtime Gaps

- Java `DeniedStatus.GUILD` rejection is not implemented in the live C# legion invite request path.
- Roster rows still use zero house address and door-state ids.
- C# has no full Java-equivalent in-memory `Legion` aggregate cache with `memberIds`.
- C# still does not model Java `legion.addBonus()` side effects on invite acceptance.

## Next Runtime UOW Recommendation

[Phase 6][Next] Add live legion invite guild-deny rejection

Runtime Progress Gate:

- What deferred/live behavior is being advanced? Java rejects legion invites when the target has guild invitations denied; C# currently lacks the live `DeniedStatus.GUILD` equivalent branch.
- What Java source method or runtime path is the source of truth? `LegionRestrictions.canInvitePlayer`, `PlayerSettings.isInDeniedStatus(DeniedStatus.GUILD)`, and `SM_SYSTEM_MESSAGE.STR_MSG_REJECTED_INVITE_GUILD`.
- What C# runtime artifact will be wired or fixed? The live `GameServerConnection.HandleLegionInviteAsync` path plus the smallest available C# player deny/guild setting representation, or a narrow persisted/runtime setting loader if one already exists.
- What client-visible, state, persistence, packet, handler, scheduler, or runtime-loading effect will change? Inviting a target who denies guild invites will send the Java rejection system message to the inviter and avoid storing/sending the question window.
- Why is this not preview-only/test-only/documentation-only? It changes live invite request control flow and sends a real failure packet from live code.

Suggested discovery:

- Search C# for deny/block/invite settings on `Player`, client setting packets, and system message `STR_MSG_REJECTED_INVITE_GUILD`.
- Inspect Java `DeniedStatus`, `PlayerSettings`, and any client packet that toggles denied statuses.
- If C# has no live setting state yet, choose the next runtime candidate below instead of creating a setting-only scaffold.

Suggested focused validation if the setting state exists:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~GamePacket" --logger "console;verbosity=minimal" --no-restore
```

Java/Maven is not expected unless a narrow Java fixture for denied-status invite rejection exists or is created. Broad-validation trigger: none unless wiring denied status touches shared player setting persistence.

## Safe Runtime Candidates

- Wire active-house address and door-state lookup into `SM_LEGION_MEMBERLIST` rows if a C# housing runtime lookup already exists and can be used by live packet code.
- Investigate Java `legion.addBonus()` and wire the smallest observable live player-state effect if the relevant C# stats/bonus runtime exists.
- If denied-status state is not available, add a different Java invite request branch that can be wired entirely through existing live C# state and packets.
