# Phase 6 Session 2763 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed Unit of Work

[Phase 6][UOW-2763] Wire legion Brigade General transfer questions

Commit:
- Pending at handoff creation.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/PendingLegionBrigadeGeneralTransferRequest.cs`
- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/Player.cs`
- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/QuestionResponseRegistry.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmQuestionWindow.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmLegionTests.cs`
- `docs/Phase-6-Session-2763-Completion.md`
- `docs/Phase-6-Session-2763-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION.java`
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/services/legion/LegionRestrictions.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_QUESTION_WINDOW.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`

## Tests Run

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~SmQuestionWindow" --logger "console;verbosity=minimal" --no-restore
```

Result:
- Passed: 70
- Failed: 0
- Skipped: 0

Java/Maven:
- Not run. Java source was unchanged and no narrow Java unit fixture exists for this question-response service path.

Other validation:
- `git diff --check` passed with line-ending warnings only.

## Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_LEGION` exOpcode `0x05` | `Aion.GameServer.Network.Aion.ClientPackets.CmLegion` / `GameServerConnection` | Client Packet / Live Handler | Partial | Unit Tested | Partial Parity | Read shape and live prompt dispatch are wired. Target acceptance does not yet mutate ranks. |
| `com.aionemu.gameserver.services.LegionService.startBrigadeGeneralChangeProcess` | `HandleLegionBrigadeGeneralTransferRequestAsync` | Live Service Path | Partial | Unit Tested | Partial Parity | Online target lookup, requester pending state, and requester question packet are ported. |
| `com.aionemu.gameserver.services.LegionService.appointBrigadeGeneral(Player, Player)` | `HandleLegionBrigadeGeneralTransferConfirmResponseAsync` / `HandleLegionBrigadeGeneralTransferOfferResponseAsync` | Live Question Path | Partial | Unit Tested | Partial Parity | Restriction checks, busy target, offer packet, and denial notification are ported. Accept mutation is intentionally left as the next runtime slice. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW` | `SmQuestionWindow` | Server Packet | Partial | Unit Tested | Partial Parity | Existing serializer is reused with question id `80011`; no Java golden packet fixture was generated. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` transfer ids | `SmSystemMessage` helpers | Server Packet | Partial | Unit Tested via handler sends | Partial Parity | Message ids for the prompt/deny path are added and asserted through live sends. |

## Known Gaps

- Target acceptance of question id `80011` currently consumes the pending request but does not yet promote the target, demote the former Brigade General, persist rank changes, add legion history, or broadcast `SM_LEGION_UPDATE_MEMBER`/`SM_LEGION_EDIT(0x08)`.
- No Java golden packet fixture was generated for the question packets.
- Invalid restriction branches beyond missing target and busy target are implemented but not exhaustively tested in this UOW.
- Quest-finish challenge progress remains blocked as a runtime UOW until a live C# quest-finish execution hook exists.

## Next Runtime UOW Candidate

Candidate:
- Wire the live target-accept path for Brigade General transfer rank mutation, persistence, history, and broadcasts.

Runtime Progress Gate:
- Deferred/live behavior advanced: target acceptance of `SM_QUESTION_WINDOW(80011)` currently consumes pending state but does not promote/demote ranks or notify legion members.
- Java source of truth: `LegionService.appointBrigadeGeneral(Player, Player)`, `LegionService.appointBrigadeGeneral(LegionMember)`, `LegionRestrictions.canAppointBrigadeGeneral`, `SM_LEGION_UPDATE_MEMBER`, `SM_LEGION_EDIT(0x08)`, `LegionMemberDAO.storeLegionMember`, and `LegionService.addHistory(... APPOINTED)`.
- C# runtime artifact to wire/fix: `GameServerConnection.HandleLegionBrigadeGeneralTransferOfferResponseAsync`, live `Player`/`LegionMember` rank state, existing rank persistence methods, legion history insert path, `SmLegionUpdateMember`, `SmLegionEdit.RefreshAnnouncement()`, and same-legion online broadcast helpers.
- Client-visible/state/persistence effect expected: accepting the offer demotes the former Brigade General to Centurion, promotes the target to Brigade General, persists the rank changes through the existing database shape, records legion history, and sends member/edit packets to online legion members.
- Why this is not preview-only/test-only/documentation-only: it mutates live legion/player state, persists runtime rank/history state, and sends real server packets.

Focused validation recipe:
- Add live handler tests for target acceptance:
  - former Brigade General rank becomes Centurion;
  - target rank becomes Brigade General;
  - rank persistence/history calls happen through the existing repository abstraction;
  - online requester, target, and same-legion bystanders receive `SM_LEGION_UPDATE_MEMBER` packets and `SM_LEGION_EDIT(0x08)`;
  - cross-legion or stale pending requests do not mutate ranks.
- Run:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~SmLegionUpdateMember|FullyQualifiedName~SmLegionEditTests" --logger "console;verbosity=minimal" --no-restore
```

Java/Maven:
- Not expected unless a narrow Java fixture for `LegionService.appointBrigadeGeneral` or the relevant legion packets is discovered.

Risks to watch:
- Java mutates rank through `LegionMember` state and stores offline members through `LegionMemberDAO`; C# may require explicit persistence of online rank changes because its save lifecycle differs.
- Java broadcasts both `SM_LEGION_UPDATE_MEMBER` for the old/new leaders and `SM_LEGION_EDIT(0x08)`. C# names type `0x08` as `SmLegionEdit.RefreshAnnouncement()`, so verify payload shape before relying on the helper name.
- Keep the prompt-flow UOW and accept-mutation UOW separate in parity claims; do not call full leadership-transfer parity complete until accept mutation is objectively tested.

Safe alternative runtime candidates:
- Port `SM_LEGION_DOMINION_LOC_INFO` only if paired with a live send path or runtime load that client code actually consumes.
- Add live dominion ranking broadcast after a real instance-finish score update only after the C# instance-finish path exists.
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
