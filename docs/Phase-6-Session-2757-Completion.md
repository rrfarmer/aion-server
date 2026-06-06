# Phase 6 Session 2757 Completion

## Unit of Work

[Phase 6][UOW-2757] Wire live legion level up

## Runtime Progress Gate

- Deferred/live behavior advanced: `CM_LEGION` exOpcode `0x0E` now executes live legion level-up behavior instead of being parser-only.
- Java source of truth: `CM_LEGION.runImpl` case `0x0E`, `LegionService.requestChangeLevel`, `LegionService.changeLevel`, `LegionRestrictions.canChangeLevel`, `Legion.getKinahPrice`, `Legion.hasRequiredMembers`, `Legion.getContributionPrice`, `SM_LEGION_EDIT` type `0x00`, `SM_SYSTEM_MESSAGE.STR_GUILD_EVENT_LEVELUP`, and `LegionHistoryAction.LEVEL_UP`.
- C# runtime artifact wired/fixed: `GameServerConnection.HandleLegionAsync`, new `HandleLegionLevelUpAsync`, `GameServerOptions.Legion` level requirement keys, `IPlayerEnterWorldRepository.CountLegionMembersAsync`, `SaveLegionLevelUpMutationAsync`, `LegionHistoryActions.LevelUp`, and level-up system-message helpers.
- Client-visible/state/persistence effect changed: a valid Brigade General request can decrease live Kinah, persist the new legion level through the existing Java schema, mutate online same-legion `Player.LegionLevel`, add `LEVEL_UP` history, and send `SmLegionEdit.Level(newLevel)` plus `STR_GUILD_EVENT_LEVELUP(newLevel)` to online legion members.
- Why this is not preview-only/test-only/documentation-only: this UOW wires a deferred live client-packet branch and adds real runtime state, persistence, history, inventory, and packet side effects.

## Java Sources Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION.java`
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/Legion.java`
- `game-server/src/com/aionemu/gameserver/configs/main/LegionConfig.java`
- `game-server/config/main/legions.properties`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/LegionHistoryAction.java`
- `game-server/src/com/aionemu/gameserver/services/ChallengeTaskService.java`
- `game-server/sql/aion_gs.sql`

## C# Runtime Changes

- Added Java legion level requirement config keys for Kinah, member count, contribution, and challenge-task requirement.
- Added live `CM_LEGION 0x0E` dispatch.
- Added Java restriction messages for no right, max level, missing challenge task, not enough Kinah, not enough members, and not enough contribution.
- Added DB-backed legion member counting from `legion_members.legion_id`.
- Added DB-backed level-up mutation that updates `inventory.item_count` and `legions.level` in one transaction.
- Added live same-legion fanout of `SmLegionEdit.Level(newLevel)` and `GuildEventLevelUp(newLevel)`.

## Validation Decision

- Changed surface: live client-packet dispatch, inventory state, legion state, database persistence, history insertion, config keys, and server-packet output.
- Specific behavior/contract: Java `CM_LEGION 0x0E` level-up restrictions and successful level-up side effects: Kinah decrement, member/contribution checks, level mutation, `LEVEL_UP` history, `SM_LEGION_EDIT` type `0x00`, and `STR_GUILD_EVENT_LEVELUP`.
- Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~SmLegionEditTests|FullyQualifiedName~PlayerEnterWorldServiceTests" --logger "console;verbosity=minimal" --no-restore
```

- Focused Java/Maven command: not run; Java source was unchanged and no narrow Java unit fixture exists for this service path.
- Broad-validation trigger: live dispatch/state/persistence side effects.
- Broad .NET decision: skipped after focused validation because the selected tests compiled the affected project and directly exercised the edited live handler, packet shape, repository fake contract, and adjacent service fake.
- Why this scope is sufficient: the UOW touched one live `CM_LEGION` branch plus its direct packet/repository/config surfaces; no shared packet primitive, socket primitive, scheduler, or schema migration changed.

## Validation Result

- Focused C# result: Passed, 136 total, 0 failed, 0 skipped.
- `git diff --check`: passed; line-ending warnings only.
- Existing nullable/analyzer warnings remain outside this UOW.
- Java/Maven was not run for the reason above.

## Tests Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `ReadFrom_LevelUpConsumesJavaEmptyFields` | Unit | `CM_LEGION.readImpl` case `0x0E` | Parser consumes Java empty `D/H` fields for level-up. | Java source review plus parser assertion. | Does not execute runtime behavior. |
| `SmSystemMessage_LegionNoticeHelpersUseJavaIdsAndParameters` | Unit | `SM_SYSTEM_MESSAGE` methods around legion level-up | Level-up restriction/event helper message ids and parameters. | Java source review plus C# constants. | Does not validate client localization. |
| `HandleInfrastructurePacketAsync_LevelUpRejectsNonBrigadeGeneralLikeJava` | Unit | `LegionRestrictions.canChangeLevel` | Non-BG level-up request sends `1300315` and does not mutate. | Java source review plus live handler assertion. | No real client validation. |
| `HandleInfrastructurePacketAsync_LevelUpRejectsInsufficientKinahBeforeMemberCheckLikeJava` | Unit | `LegionRestrictions.canChangeLevel` order | Insufficient Kinah sends `1300319` before member counting. | Java source review plus repository call assertions. | No static-data inventory packet is sent in this no-runtime-context test. |
| `HandleInfrastructurePacketAsync_LevelUpRejectsMissingChallengeTaskForLevelFivePlusLikeJavaDefault` | Unit | `LegionRestrictions.canChangeLevel` and `ChallengeTaskService.canRaiseLegionLevel` | Java default challenge-task gate blocks level 5+ while C# lacks the runtime service. | Java source review plus live handler assertion. | This is a conservative block, not full challenge-task parity. |
| `HandleInfrastructurePacketAsync_LevelUpMutatesKinahLevelHistoryAndBroadcastsLikeJava` | Unit | `LegionService.requestChangeLevel/changeLevel` | Success decrements Kinah, saves level, inserts `LEVEL_UP` history, updates online same-legion level, and broadcasts level edit/event packets. | Java source review plus live handler and packet-byte assertions. | DB integration and real client validation not run. |

## Conservative Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_LEGION` | `Aion.GameServer.Network.Aion.ClientPackets.CmLegion` and `GameServerConnection.HandleLegionAsync` | Client Packet / Runtime Dispatch | Partial | Unit Tested | Partial Parity | `0x0E` is now live; many other legion subactions remain partial or deferred. |
| `com.aionemu.gameserver.services.LegionService` | `GameServerConnection.HandleLegionLevelUpAsync` | Service Logic | Partial | Unit Tested | Partial Parity | Level-up happy path and core restrictions are live. Java has a shared in-memory `Legion`; C# persists `legions.level` immediately to keep DB-backed runtime facts consistent. Challenge-task success path remains blocked. |
| `com.aionemu.gameserver.services.LegionService.LegionRestrictions` | `GameServerConnection.HandleLegionLevelUpAsync` | Service Logic | Partial | Unit Tested | Partial Parity | BG, max-level, challenge-task, Kinah, member-count, and contribution restrictions are covered. Exact Java ChallengeTaskService success behavior is not ported. |
| `com.aionemu.gameserver.model.team.legion.Legion` | `GameServerOptions.GameServerLegionOptions` and `Player` legion fields | Model / Config | Partial | Unit Tested | Partial Parity | Level requirement tables are loaded from Java config keys and used by live code. Full Legion aggregate behavior is not ported. |
| `com.aionemu.gameserver.dao.LegionDAO` | `MySqlPlayerEnterWorldRepository.SaveLegionLevelUpMutationAsync` and `CountLegionMembersAsync` | Repository | Partial | Unit Tested | Needs Verification | Uses existing Java schema for `legions.level`, `inventory.item_count`, and `legion_members.legion_id`; no DB integration test was run in this UOW. |
| `com.aionemu.gameserver.model.team.legion.LegionHistoryAction` | `LegionHistoryActions` | Enum Metadata | Partial | Unit Tested | Partial Parity | Added `LEVEL_UP` constant using existing Java id/type mapping. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `SmSystemMessage` | Server Packet | Partial | Unit Tested | Partial Parity | Added reviewed level-up restriction/event helpers. |

## Summary Metrics

- Total Java artifacts discovered: 7
- Total artifacts ported in this UOW: 7 partial runtime/packet/repository/config artifacts
- Total artifacts with verified parity: 0
- Total artifacts needing verification/partial parity: 7
- Total blocked artifacts: 1 (`ChallengeTaskService.canRaiseLegionLevel` success path)
- Estimated overall migration completion: unchanged, conservatively still Phase 6 in progress

## Known Gaps

- Java `ChallengeTaskService.canRaiseLegionLevel` is not ported; C# currently blocks level 5+ raises with the Java challenge-task message when the Java-default requirement is enabled.
- Java mutates a shared in-memory `Legion` object and `requestChangeLevel` passes `save=false`; C# has no shared Legion aggregate, so this UOW persists `legions.level` immediately as the runtime source for loaded legion facts.
- No DB integration test was run for `SaveLegionLevelUpMutationAsync`.
- No real client validation was performed.
- Inventory Kinah update packet is sent only when runtime item templates are loaded; the focused tests validate state/persistence capture and broadcast packets without a static-data runtime context.
