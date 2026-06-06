# Phase 6 Session 2780 Handoff

## Current State

The latest completed Unit of Work is `[Phase 6][UOW-2780] Honor legion invite guild-deny setting`.

Live legion invite request handling now matches another Java restriction branch:

- If the online target has the guild deny bit enabled in `PlayerSettings.Deny`, the inviter receives Java system message `1390118`.
- The denied invite does not register `SmQuestionWindow.GuildInviteDoYouAcceptInvitation`.
- The denied invite does not set `PendingLegionInviteRequest` and sends no direct question packet to the target.

## Commit

Expected commit message:

```text
[Phase 6][UOW-2780] Honor legion invite guild-deny setting
```

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmLegionTests.cs`
- `docs/Phase-6-Session-2780-Completion.md`
- `docs/Phase-6-Session-2780-Handoff.md`

## Validation Evidence

Focused C# validation passed:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests" --logger "console;verbosity=minimal" --no-restore
```

Result: Passed, 91 total, 0 failed, 0 skipped.

`git diff --check` passed with line-ending warnings only.

Focused Java/Maven validation was not run because Java source was unchanged and no narrow Java unit fixture exists for `LegionRestrictions.canInvitePlayer`.

## Latest Runtime Progress Gate

- Deferred/live behavior advanced: C# live legion invite handling now honors Java target guild-invite denial.
- Java source of truth: `LegionRestrictions.canInvitePlayer`, `PlayerSettings.isInDeniedStatus(DeniedStatus.GUILD)`, `DeniedStatus.GUILD`, and `SM_SYSTEM_MESSAGE.STR_MSG_REJECTED_INVITE_GUILD`.
- C# runtime artifact wired/fixed: `GameServerConnection.HandleLegionInviteAsync`, `PlayerSettings.DeniesGuildRequests()`, and `SmSystemMessage.MsgRejectedInviteGuild`.
- Client-visible/state/persistence effect changed: denied targets cause system message `1390118` and prevent question request state/direct question packet creation.
- Why this was not preview-only/test-only/documentation-only: live handler packet selection and pending-request mutation behavior changed.

## Conservative Parity Status

| Java Artifact | C# Artifact | Status | Evidence | Remaining Gap |
| --- | --- | --- | --- | --- |
| `LegionRestrictions.canInvitePlayer` | `GameServerConnection.HandleLegionInviteAsync` | Partial parity | Tests cover target not found, guild denied, dead, self, member, rights, race, busy, and success branches currently modeled. | Full Java `Legion` aggregate and bonus side effects remain partial. |
| `PlayerSettings.isInDeniedStatus(DeniedStatus.GUILD)` | `PlayerSettings.DeniesGuildRequests()` | Partial parity | Handler test uses the Java guild bit value through the existing C# setting model. | Java persistent-state lifecycle is not modeled 1:1. |
| `SM_SYSTEM_MESSAGE.STR_MSG_REJECTED_INVITE_GUILD` | `SmSystemMessage.MsgRejectedInviteGuild` | Partial parity | Message id and parameter are asserted in `CmLegionTests`. | No golden packet fixture. |

## Remaining Runtime Gaps

- Legion member-list rows still use zero house address and door-state ids.
- C# has no full Java-equivalent in-memory `Legion` aggregate cache with `memberIds`.
- C# still does not model Java `legion.addBonus()` side effects on invite acceptance.
- Broader player deny/settings parity is partial, though guild-deny invite handling is now wired into live code.

## Next Runtime UOW Recommendation

[Phase 6][Next] Populate legion member-list active-house fields

Runtime Progress Gate:

- What deferred/live behavior is being advanced? Java `SM_LEGION_MEMBERLIST` writes each member's active house address and door-state id; C# member-list packets currently emit zeros.
- What Java source method or runtime path is the source of truth? `SM_LEGION_MEMBERLIST.writeLegionMember`, `HousingService.findActiveHouse`, `House.getAddress().getId`, and `House.getDoorState().getId`.
- What C# runtime artifact will be wired or fixed? `SmLegionMemberList`, `LegionMemberListEntry`, and the live roster construction path in `GameServerConnection` or the repository projection that builds roster entries.
- What client-visible, state, persistence, packet, handler, scheduler, or runtime-loading effect will change? Legion roster packets sent after invite acceptance/member-list refresh will include active house address and door-state ids instead of zeros when C# has loaded house data for that player.
- Why is this not preview-only/test-only/documentation-only? It changes real `SM_LEGION_MEMBERLIST` packet payloads emitted by live legion code.

Suggested discovery:

- Inspect Java `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_MEMBERLIST.java`.
- Inspect C# `SmLegionMemberList`, `LegionMemberListEntry`, `PlayerHouse`, and `PlayerActiveHouseResolverService.FindActiveHouse`.
- Check whether roster entries are built from live online `Player` objects, repository rows, or both; use the smallest live source already carrying loaded `Player.Houses`.

Suggested focused validation:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests" --logger "console;verbosity=minimal" --no-restore
```

Java/Maven is not expected unless a narrow Java fixture is created for `SM_LEGION_MEMBERLIST`. Broad-validation trigger: none unless the UOW changes shared housing load/persistence behavior.

## Safe Runtime Candidates

- Investigate Java `legion.addBonus()` and wire the smallest observable live player-state effect if the relevant C# stats/bonus runtime exists.
- Add a different Java invite request or acceptance branch that can be wired entirely through existing live C# state and packets.
- Continue narrowing legion roster parity around Java `LegionMember` cached player fields where C# currently uses partial database/runtime projections.
