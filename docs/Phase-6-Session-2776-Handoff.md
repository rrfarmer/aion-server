# Phase 6 Session 2776 Handoff

## Current State

The latest completed Unit of Work is `[Phase 6][UOW-2776] Guard full legion invite acceptance`.

Live legion invite acceptance now mirrors Java's max-member refusal before persistence:

- Loads Java legion max-member defaults and `gameserver.legion.level{1..8}maxmembers` overrides into C# runtime options.
- Counts current persisted legion members before saving the accepted responder.
- Sends Java system message `STR_GUILD_INVITE_CAN_NOT_ADD_MEMBER_ANY_MORE` (`1300257`) to the inviter when the legion is full.
- Avoids responder legion mutation, new `legion_members` persistence, legion history insert, member-list/info packets, add-member fanout, emblem broadcast, edit broadcast, and title broadcast on the full-legion failure branch.

## Validation Evidence

Focused C# validation passed:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~GameServerOptions" --logger "console;verbosity=minimal" --no-restore
```

Result: Passed, 93 total, 0 failed, 0 skipped.

`git diff --check` passed with line-ending warnings only.

Focused Java/Maven validation was not run because Java source was unchanged and no narrow Java unit fixture exists for this live invite path.

## Latest Runtime Progress Gate

- Deferred/live behavior advanced: live invite acceptance previously allowed a persisted add beyond Java's level max.
- Java source of truth: `LegionService.addToLegion`, `Legion.addLegionMember`, `Legion.canAddMember`, `LegionConfig.LEGION_LEVEL{1..8}_MAX_MEMBERS`, and `SM_SYSTEM_MESSAGE.STR_GUILD_INVITE_CAN_NOT_ADD_MEMBER_ANY_MORE`.
- C# runtime artifact wired/fixed: `GameServerConnection.AcceptLegionInviteAsync`, `GameServerOptions.Legion.LevelMaxMembers`, Java config loading, and `SmSystemMessage.GuildInviteCanNotAddMemberAnyMore`.
- Client-visible/state/persistence effect changed: full-legion invite acceptance now sends the inviter message `1300257` and prevents invalid DB/state mutation.
- Why this was not preview-only/test-only/documentation-only: live handler control flow now reads persisted state, sends a real server packet, and blocks persistence/mutation.

## Conservative Parity Status

| Java Artifact | C# Artifact | Status | Evidence | Remaining Gap |
| --- | --- | --- | --- | --- |
| `Legion.canAddMember` | `GameServerConnection.CanAddLegionInviteMemberAsync` | Partial parity | Full-legion handler test asserts Java failure side effects. | Uses DB count instead of Java in-memory `memberIds`; no Java golden fixture. |
| `LegionConfig.LEGION_LEVEL{1..8}_MAX_MEMBERS` | `GameServerLegionOptions.LevelMaxMembers` | Partial parity | Config loader test asserts Java key/default list. | Only the relevant keys are covered in focused tests. |
| `SM_SYSTEM_MESSAGE.STR_GUILD_INVITE_CAN_NOT_ADD_MEMBER_ANY_MORE` | `SmSystemMessage.GuildInviteCanNotAddMemberAnyMore` | Partial parity | Live handler test observes message id `1300257`. | Packet binary golden not generated. |

## Remaining Runtime Gaps

- Invite acceptance roster packet still uses only online players from the connection registry. Java sends from the full legion member cache, including offline members.
- Invite acceptance member-list chunking is not Java-complete. Java chunks at 80 rows; C# sends one chunk for the current online slice.
- Roster rows still use zero house address and door-state ids.
- C# has no full Java-equivalent in-memory `Legion` aggregate cache with `memberIds`.
- C# still does not model Java `legion.addBonus()` side effects on invite acceptance.
- Invite eligibility still has broader Java gaps such as other-faction invite config and `DeniedStatus.GUILD` behavior.

## Next Runtime UOW Recommendation

[Phase 6][Next] Load persisted legion roster for invite member list

Runtime Progress Gate:

- What deferred/live behavior is being advanced? The accepted player's `SM_LEGION_MEMBERLIST` currently includes only online same-legion players visible in the connection registry; Java uses the full legion member set.
- What Java source method or runtime path is the source of truth? `LegionService.updateLegionMemberList`, `Legion.getMembers`, and `SM_LEGION_MEMBERLIST.writeImpl`.
- What C# runtime artifact will be wired or fixed? Add a repository method for loading persisted legion member roster rows, adapt `SendLegionInviteMemberListAsync` to build rows from persisted members plus online overlays, and keep current packet serialization.
- What client-visible, state, persistence, packet, handler, scheduler, or runtime-loading effect will change? The accepted client will receive roster rows for offline persisted legion members as well as online members.
- Why is this not preview-only/test-only/documentation-only? It loads runtime DB state and sends real roster packets from the live invite-acceptance path.

Suggested C# artifacts:

- `IPlayerEnterWorldRepository` and MySQL implementation for persisted legion roster loading.
- `EmptyPlayerEnterWorldRepository` test double.
- `GameServerConnection.SendLegionInviteMemberListAsync`.
- `CmLegionTests` live invite acceptance roster assertions.

Suggested focused validation:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~PlayerEnterWorldRepository" --logger "console;verbosity=minimal" --no-restore
```

Only run broader validation if the repository query or packet row model touches shared enter-world behavior beyond the focused roster path.

## Safe Runtime Candidates

- Port Java's 80-row `SM_LEGION_MEMBERLIST` chunking for live invite acceptance after persisted roster loading exists.
- Add Java-equivalent invite eligibility for other-faction invite config and guild denial status in the live invite request path.
- Investigate the Java `legion.addBonus()` side effect and wire the smallest observable C# live player-state effect if the relevant C# stats/bonus runtime exists.
