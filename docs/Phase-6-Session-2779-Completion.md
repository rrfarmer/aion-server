# Phase 6 Session 2779 Completion

## Unit of Work

[Phase 6][UOW-2779] Honor legion cross-faction invite config

## Runtime Progress Gate

- Deferred/live behavior advanced: C# legion invite request handling had Java's other-race failure packet but ignored Java's `LegionConfig.LEGION_INVITEOTHERFACTION` allowance.
- Java source of truth: `LegionService.invitePlayerToLegion`, `LegionRestrictions.canInvitePlayer`, `LegionConfig.LEGION_INVITEOTHERFACTION`, and `SM_SYSTEM_MESSAGE.STR_GUILD_INVITE_CAN_NOT_INVITE_OTHER_RACE`.
- C# runtime artifact wired/fixed: `GameServerConnection.HandleLegionInviteAsync`, `GameServerOptions.Legion.InviteOtherFactionEnabled`, Java config loading for `gameserver.legion.inviteotherfaction`, and focused invite tests.
- Client-visible/state/persistence effect changed: cross-faction legion invites still fail by default with Java message `1300311`, but when the Java config key is enabled they now store the live question request and send the invite question packet.
- Why this is not preview-only/test-only/documentation-only: it changes live invite request control flow and sends real server packets from live code.

## Java Sources Reviewed

- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/configs/main/LegionConfig.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
- `game-server/config/main/legions.properties`

## C# Runtime Changes

- Added `InviteOtherFactionEnabled` to `GameServerLegionOptions`.
- Loaded Java property `gameserver.legion.inviteotherfaction` with default `false`.
- Changed the live invite race guard to reject only when the races differ and cross-faction invites are disabled.
- Added a live handler test proving config-enabled cross-faction invite sends the normal Java question path.

## Validation Decision

- Changed surface: live legion invite request control flow plus Java config option loading.
- Specific behavior/contract: Java rejects other-race legion invites only when `LegionConfig.LEGION_INVITEOTHERFACTION` is false; otherwise it proceeds to the normal request/question send path.
- Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~GameServerOptions" --logger "console;verbosity=minimal" --no-restore
```

- Focused Java/Maven command: not run; Java source was unchanged and no narrow Java unit fixture exists for `LegionRestrictions.canInvitePlayer`.
- Broad-validation trigger: none. The change is isolated to one live invite branch and one legion config key.
- Broad .NET decision: skipped because the focused command compiles the affected project and asserts both the live invite branch and config loader behavior.
- Why this scope is sufficient: the existing default-denial test covers the Java false default, the new test covers the true config branch, and config loader assertions prove the Java property is wired.

## Validation Result

- Focused C# result: Passed, 95 total, 0 failed, 0 skipped.
- `git diff --check`: passed with line-ending warnings only.
- Existing nullable/analyzer warnings remain outside this UOW.

## Tests Added or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `HandleInfrastructurePacketAsync_LegionInviteOtherRaceAllowedByConfigSendsQuestionLikeJava` | Unit | `LegionRestrictions.canInvitePlayer` | With cross-faction invites enabled, other-race invite stores the pending request, sends `1300258`, and sends the target question window. | Live handler side-effect assertions from Java source review. | Denied-status guard remains missing. |
| `LoadFromJavaConfig_UsesJavaPropertiesWhenPresent` | Unit | `LegionConfig.LEGION_INVITEOTHERFACTION` | Java `gameserver.legion.inviteotherfaction` property flows into C# options. | Config loader assertion with Java property override. | Broader Java config coverage remains outside this UOW. |

## Conservative Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.LegionService.invitePlayerToLegion` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleLegionInviteAsync` | Live Handler | Partial | Unit Tested | Partial Parity | Other-faction config branch now matches Java. DeniedStatus.GUILD remains unimplemented. |
| `com.aionemu.gameserver.configs.main.LegionConfig.LEGION_INVITEOTHERFACTION` | `Aion.GameServer.Configuration.GameServerLegionOptions.InviteOtherFactionEnabled` | Runtime Config | Ported | Unit Tested | Partial Parity | Default and property key are modeled. |
| `SM_SYSTEM_MESSAGE.STR_GUILD_INVITE_CAN_NOT_INVITE_OTHER_RACE` | `SmSystemMessage.GuildInviteCanNotInviteOtherRace` | Server Packet Helper | Ported | Unit Tested through handler | Partial Parity | Existing message id `1300311` is sent by default branch; packet golden fixture not generated. |

## Summary Metrics

- Total Java artifacts discovered: 4
- Total artifacts ported or advanced in this UOW: 3 runtime/config/packet artifacts
- Total artifacts with verified parity: 0
- Total artifacts needing verification/partial parity: 3
- Total blocked/not-started artifacts: 3 legion invite/roster gaps remain
- Estimated overall migration completion: unchanged, conservatively still Phase 6 in progress

## Known Gaps

- Java `DeniedStatus.GUILD` rejection still is not modeled in C# legion invite request handling.
- Member-list house address and door-state ids remain zero.
- C# still does not model Java's full in-memory `Legion` aggregate and `memberIds`.
- C# still does not model Java `legion.addBonus()` side effects on invite acceptance.
- No Java golden or runtime comparison fixture was generated for the cross-faction invite branch.
