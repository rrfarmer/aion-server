# Phase 6 Session 2770 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed Unit of Work

[Phase 6][UOW-2770] Wire live legion invite questions

Commit:
- Pending at handoff creation.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmQuestionWindow.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/Player.cs`
- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/QuestionResponseRegistry.cs`
- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/PendingLegionInviteRequest.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmLegionTests.cs`
- `docs/Phase-6-Session-2770-Completion.md`
- `docs/Phase-6-Session-2770-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION.java`
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/LegionPermissionsMask.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_QUESTION_WINDOW.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`

## Tests Run

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~SmQuestionWindow" --logger "console;verbosity=minimal" --no-restore
```

Result:
- Passed: 81
- Failed: 0
- Skipped: 0

Java/Maven:
- Not run. Java source was unchanged and no narrow Java unit fixture exists for `LegionService.invitePlayerToLegion` or the guild invite question packet.

Other validation:
- `git diff --check` passed with line-ending warnings only.

## Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_LEGION` exOpcode `0x01` | `CmLegion` / `GameServerConnection.HandleLegionInviteAsync` | Client Packet / Live Handler | Partial | Unit Tested | Partial Parity | Read shape, dispatch, modeled restrictions, pending request registration, requester messages, and target question send are covered. |
| `com.aionemu.gameserver.services.LegionService.invitePlayerToLegion` | `GameServerConnection.HandleLegionInviteAsync` | Live Service Path | Partial | Unit Tested | Partial Parity | Question flow is live. Java target denied-guild-invite setting and accept/deny membership handling remain gaps. |
| `com.aionemu.gameserver.model.gameobjects.player.ResponseRequester.putRequest` | `QuestionResponseRegistry.PutRequest` | Runtime State | Partial | Unit Tested | Partial Parity | Successful invite stores request; duplicate question id is rejected as busy. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW` guild invite | `SmQuestionWindow.GuildInviteDoYouAcceptInvitation` | Server Packet | Complete for current fields | Unit Tested | Partial Parity | Writes question id 80001 and the three Java parameters. No Java golden fixture. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` guild invite helpers | `SmSystemMessage` guild invite methods | Server Packet | Partial | Unit Tested | Partial Parity | Message ids used by this path are covered. Denied-guild-invite setting helper is not wired because C# target setting is not modeled here. |

## Known Gaps

- `CM_QUESTION_RESPONSE` for guild invite accept/deny is not wired yet; pending invite request state is stored but response application is a separate runtime UOW.
- Java `DeniedStatus.GUILD` invite rejection is not modeled in this C# player/settings path.
- Java `LegionConfig.LEGION_INVITEOTHERFACTION` override is not exposed in C# legion options; current C# behavior denies other-race invites when both races are known.
- No Java golden fixture was generated for the guild invite question packet.
- Quest-finish challenge progress remains blocked as a runtime UOW until a live C# quest-finish execution hook exists.

## Next Runtime UOW Candidate

Candidate:
- Wire live `CM_QUESTION_RESPONSE` handling for guild invite denial, and acceptance if existing repository/runtime hooks can safely add an online target to the inviter's legion.

Runtime Progress Gate:
- Deferred/live behavior advanced: C# now stores a pending `LegionInvite` request and sends the Java invite question, but target responses to question id `80001` are not yet consumed by live code.
- Java source of truth: `LegionService.invitePlayerToLegion` anonymous `RequestResponseHandler.acceptRequest/denyRequest`, `LegionService.addToLegion`, `ResponseRequester.respond`, and `SM_SYSTEM_MESSAGE.STR_GUILD_INVITE_HE_REJECTED_INVITATION`.
- C# runtime artifact to wire/fix: `GameServerConnection.HandleQuestionResponseAsync`, `PendingLegionInviteRequest`, target `ResponseRequester.Respond(80001, response)`, requester lookup through `_connectionRegistry`, and existing legion member persistence/loading helpers if acceptance is in scope.
- Client-visible/state/persistence effect expected: denial consumes the pending request and sends the inviter the Java rejected-invitation message; acceptance should add the responder to the live legion, persist/restore through existing DB shape, and send Java-equivalent legion update packets only if repository support is available.
- Why this is not preview-only/test-only/documentation-only: it consumes live pending request state from a real question response and sends/mutates runtime effects from live handler code.

Focused validation recipe:
- Add live handler tests for question id `80001`:
  - denial consumes target `ResponseRequester` and clears `PendingLegionInviteRequest`;
  - denial sends `STR_GUILD_INVITE_HE_REJECTED_INVITATION(targetName)` to the online inviter;
  - wrong/missing request is side-effect free;
  - acceptance mutates/persists membership only if existing runtime repository methods are adequate.
- Run:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~SmQuestionWindow" --logger "console;verbosity=minimal" --no-restore
```

Java/Maven:
- Not expected unless a narrow Java fixture for `LegionService.addToLegion` or invite response is discovered.

Risks to watch:
- Do not fake add-to-legion with preview state. Acceptance only counts if it mutates live player/legion state and persists through existing database shape.
- Denial is a safe first runtime slice because it consumes the pending request and sends a real Java-equivalent packet.
- Ensure stale inviter/target lookups clear request state like Java `ResponseRequester.respond` semantics.

Safe alternative runtime candidates:
- If acceptance persistence is blocked, complete denial response first and document the acceptance blocker honestly.
- Port Java `DeniedStatus.GUILD` only if a live C# invite-denial setting exists or can be wired into actual player settings state.
- Return to challenge quest-finish progress only after a live quest-finish socket/runtime hook exists; do not use planner placeholders as migration progress.

## Context Needed Next

Start with:
- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- this handoff
- latest completion document
- current `git status --short`

Do not update `PHASE-6-PROGRESS.md` unless explicitly asked.
