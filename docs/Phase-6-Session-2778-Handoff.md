# Phase 6 Session 2778 Handoff

## Current State

The latest completed Unit of Work is `[Phase 6][UOW-2778] Chunk legion invite member list packets`.

Live legion invite acceptance now sends roster packets closer to Java:

- Roster rows are loaded from persisted legion membership, with online overlays from the connection registry.
- The accepted player is excluded from the initial member list.
- `SM_LEGION_MEMBERLIST` sends are split into Java-sized chunks of 80 rows.
- Empty rosters still produce one first/last empty packet, matching Java `FixedElementCountSplitList<>(..., true, 80)`.
- Last chunks write negative signed counts through the existing packet serializer.

## Commit

Expected commit message:

```text
[Phase 6][UOW-2778] Chunk legion invite member list packets
```

## Validation Evidence

Focused C# validation passed:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~SmLegionMemberList" --logger "console;verbosity=minimal" --no-restore
```

Result: Passed, 89 total, 0 failed, 0 skipped.

`git diff --check` passed with line-ending warnings only.

Focused Java/Maven validation was not run because Java source was unchanged and no narrow Java unit fixture exists for this chunking path.

## Latest Runtime Progress Gate

- Deferred/live behavior advanced: accepted player's legion member-list packet fanout previously ignored Java's 80-row chunking.
- Java source of truth: `LegionService.updateLegionMemberList`, `FixedElementCountSplitList`, `ListPart.isFirst/isLast`, and `SM_LEGION_MEMBERLIST.writeImpl`.
- C# runtime artifact wired/fixed: `GameServerConnection.SendLegionInviteMemberListAsync` and focused invite acceptance tests.
- Client-visible/state/persistence effect changed: accepted clients with more than 80 roster rows now receive multiple Java-shaped member-list packets.
- Why this was not preview-only/test-only/documentation-only: live invite acceptance now sends real chunked server packets.

## Conservative Parity Status

| Java Artifact | C# Artifact | Status | Evidence | Remaining Gap |
| --- | --- | --- | --- | --- |
| `LegionService.updateLegionMemberList` | `GameServerConnection.SendLegionInviteMemberListAsync` | Partial parity | Live handler test asserts 80-row first chunk and 2-row final chunk. | House fields remain zero; no Java golden fixture. |
| `FixedElementCountSplitList` | `GameServerConnection.SplitLegionMemberList` | Partial parity | Live handler test covers needed legion member-list chunk behavior. | Not a general-purpose collection utility port. |
| `SM_LEGION_MEMBERLIST` | `SmLegionMemberList` | Partial parity | Existing packet serializer plus chunked live handler assertions. | Active-house fields are not hydrated. |

## Remaining Runtime Gaps

- Roster rows still use zero house address and door-state ids.
- C# has no full Java-equivalent in-memory `Legion` aggregate cache with `memberIds`.
- C# still does not model Java `legion.addBonus()` side effects on invite acceptance.
- Invite eligibility still has broader Java gaps such as other-faction invite config and `DeniedStatus.GUILD` behavior.

## Next Runtime UOW Recommendation

[Phase 6][Next] Add Java other-faction legion invite guard

Runtime Progress Gate:

- What deferred/live behavior is being advanced? C# legion invite request handling does not yet enforce Java's race/faction restriction and `LegionConfig.INVITEOTHERFACTION` behavior.
- What Java source method or runtime path is the source of truth? `LegionService.invitePlayerToLegion`, `LegionRestrictions.canInvite`, `LegionConfig.INVITEOTHERFACTION`, and `SM_SYSTEM_MESSAGE.STR_GUILD_INVITE_CAN_NOT_INVITE_OTHER_RACE`.
- What C# runtime artifact will be wired or fixed? `GameServerConnection.HandleLegionInviteAsync` or the adjacent invite request path, `GameServerOptions.Legion` config loading if the option is not present, and focused invite tests.
- What client-visible, state, persistence, packet, handler, scheduler, or runtime-loading effect will change? Inviting a different-race target will send the Java failure system message and avoid storing/sending the question window unless config allows cross-faction invites.
- Why is this not preview-only/test-only/documentation-only? It changes live invite request control flow and sends a real failure packet from live code.

Suggested C# artifacts:

- `GameServerConnection.HandleLegionInviteAsync`
- `GameServerOptions.GameServerLegionOptions`
- `SmSystemMessage.GuildInviteCanNotInviteOtherRace`
- `CmLegionTests`
- `GameServerOptionsTests` if a missing Java config key is added

Suggested focused validation:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~GameServerOptions" --logger "console;verbosity=minimal" --no-restore
```

Java/Maven is not expected unless a narrow Java fixture for `LegionRestrictions.canInvite` exists or is created. Broad-validation trigger: none unless the config loader change crosses shared option parsing.

## Safe Runtime Candidates

- Wire active-house address and door-state lookup into `SM_LEGION_MEMBERLIST` rows if a C# housing runtime lookup already exists and can be used by live packet code.
- Investigate Java `legion.addBonus()` and wire the smallest observable live player-state effect if the relevant C# stats/bonus runtime exists.
- Add Java-equivalent `DeniedStatus.GUILD` behavior in the live invite request path if the denial-state runtime exists in C#.
