# Phase 6 Session 2780 Completion

## Unit of Work

[Phase 6][UOW-2780] Honor legion invite guild-deny setting

## Runtime Progress Gate

- Deferred/live behavior advanced: C# legion invite request handling did not honor Java's target-side guild invite deny setting.
- Java source of truth: `LegionService.LegionRestrictions.canInvitePlayer`, `PlayerSettings.isInDeniedStatus(DeniedStatus.GUILD)`, `DeniedStatus.GUILD`, and `SM_SYSTEM_MESSAGE.STR_MSG_REJECTED_INVITE_GUILD`.
- C# runtime artifact wired/fixed: `GameServerConnection.HandleLegionInviteAsync`, `PlayerSettings.DeniesGuildRequests()`, and `SmSystemMessage.MsgRejectedInviteGuild`.
- Client-visible/state/persistence effect changed: inviting an online player whose persisted/runtime deny bitmask contains guild denial now sends system message `1390118` to the inviter and does not store or send a legion invite question request.
- Why this is not preview-only/test-only/documentation-only: it changes the live invite handler's packet-send branch and prevents live pending-request state mutation.

## Java Sources Reviewed

- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/DeniedStatus.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/PlayerSettings.java`
- `game-server/src/com/aionemu/gameserver/dao/PlayerSettingsDAO.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`

## C# Runtime Changes

- Added `SmSystemMessage.MsgRejectedInviteGuild(string)` for Java message id `1390118`.
- Inserted the guild-deny invite guard in `HandleLegionInviteAsync` immediately after target lookup, matching Java's restriction order.
- Reused the existing persisted/runtime C# deny bitmask surface through `PlayerSettings.DeniesGuildRequests()`.
- Added a live handler test proving the denial message is sent and no question request or direct packet is created.

## Validation Decision

- Changed surface: live legion invite request control flow and one server packet helper.
- Specific behavior/contract: Java rejects a legion invite before death/self/member/race checks when the target has `DeniedStatus.GUILD` enabled.
- Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests" --logger "console;verbosity=minimal" --no-restore
```

- Focused Java/Maven command: not run; Java source was unchanged and no narrow Java unit fixture exists for `LegionRestrictions.canInvitePlayer`.
- Broad-validation trigger: none. The change is isolated to one live handler branch and a packet helper, with the deny bitmask load/save surface already existing.
- Broad .NET decision: skipped because the focused command compiles the affected project and asserts the Java-derived handler side effects.
- Why this scope is sufficient: the edited test class exercises the packet helper, live invite dispatch, sent system message, and absence of pending question state/direct packets.

## Validation Result

- Focused C# result: Passed, 91 total, 0 failed, 0 skipped.
- `git diff --check`: passed with line-ending warnings only.
- Existing nullable/analyzer warnings remain outside this UOW.

## Tests Added or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `HandleInfrastructurePacketAsync_LegionInviteGuildDeniedTargetSendsRejectLikeJava` | Unit | `LegionRestrictions.canInvitePlayer` | Target `DenyGuildRequests` sends message `1390118`, does not register a question, and sends no direct question packet. | Live handler side-effect assertions from Java source review. | Full invite service still has other partial parity gaps. |
| `SmSystemMessage_LegionNoticeHelpersUseJavaIdsAndParameters` | Unit | `SM_SYSTEM_MESSAGE.STR_MSG_REJECTED_INVITE_GUILD` | The helper uses message id `1390118` and target-name parameter. | Java message id reviewed from source. | No packet golden fixture for this message. |

## Conservative Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.LegionService.LegionRestrictions.canInvitePlayer` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleLegionInviteAsync` | Live Handler | Partial | Unit Tested | Partial Parity | Guild-deny branch order now matches Java. Remaining invite gaps include full Java legion aggregate side effects and broader settings behavior. |
| `com.aionemu.gameserver.model.gameobjects.player.PlayerSettings` | `Aion.GameServer.Model.GameObjects.PlayerSettings` | Model | Partial | Unit Tested through handler | Partial Parity | Existing deny bitmask is reused and loaded/saved through `player_settings`; persistent-state tracking differs from Java. |
| `com.aionemu.gameserver.model.gameobjects.player.DeniedStatus` | `Aion.GameServer.Model.GameObjects.PlayerSettings` constants | Enum/Constants | Partial | Unit Tested through handler | Partial Parity | C# models deny values as constants instead of an enum; guild bit value `8` is used by live code. |
| `SM_SYSTEM_MESSAGE.STR_MSG_REJECTED_INVITE_GUILD` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.MsgRejectedInviteGuild` | Server Packet Helper | Ported | Unit Tested | Partial Parity | Message id and parameter are tested; no golden packet fixture was generated. |

## Summary Metrics

- Total Java artifacts discovered: 5
- Total artifacts ported or advanced in this UOW: 4 runtime/model/packet artifacts
- Total artifacts with verified parity: 0
- Total artifacts needing verification/partial parity: 4
- Total blocked/not-started artifacts: 3 legion roster/invite gaps remain
- Estimated overall migration completion: unchanged, conservatively still Phase 6 in progress

## Known Gaps

- C# still does not model Java's full in-memory `Legion` aggregate and `memberIds`.
- C# still does not model Java `legion.addBonus()` side effects on invite acceptance.
- Legion member-list rows still write zero house address and door-state ids.
- No Java golden or runtime comparison fixture was generated for the guild-deny branch.
