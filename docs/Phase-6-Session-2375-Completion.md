# Phase 6 Session 2375 Completion - Port Autogroup Member Entry Denials

## Scope

Ported the non-Harmony member-loop denial branch from `AutoGroupUtility.checkGroupRequirements` for autogroup group-entry registration.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/autogroup/AutoGroupUtility.java`
- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`

Java behavior used:

- `checkGroupRequirements` iterates `team.getMembers()` after template, leader, and team-size checks.
- The leader is skipped.
- If a non-leader member has instance cooldown, is outside the autogroup level range, or is already searching the same mask, Java sends `STR_MSG_CANT_INSTANCE_ENTER_MEMBER(memberName)` to the requester and returns false.
- Queue registration does not mutate when the member-denial branch fails.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

- Added `AutoGroupRegistrationGuardPlanStatus.BlockedMemberCannotEnter`.
- Added group/alliance runtime accessors for current `Player` objects so autogroup guards can evaluate member facts, not just ids.
- Extended `AutoGroupLookingPartyRegistrationService` group-entry checks to deny on member cooldown, level range, or already-searching state.
- Added an optional `now` parameter to `StartLooking` for deterministic cooldown tests.
- Added `SmSystemMessage.CantInstanceEnterMember(...)` for Java message id `1400187`.
- Added focused service tests for member level, cooldown, and searching denial.
- Added packet-helper coverage for message id `1400187`.

Known limitations:

- Harmony arena item validation remains unported. Java sends `STR_MSG_INSTANCE_CANT_ENTER_WITHOUT_ITEM` to the member and `STR_MSG_CANT_INSTANCE_ENTER_MEMBER(memberName)` to the requester; that needs a smaller PvP arena/inventory-focused UOW.
- If C# only has team member ids without a `PlayerGroupRuntime` or `PlayerAllianceRuntime`, member fact checks cannot evaluate level/cooldown/searching and remain partial.
- Live ingress coverage from UOW-2374 still proves one denial-message path; this UOW is service and packet-helper focused.

## Validation Decision

- Changed surface: production autogroup registration service guard logic, group/alliance runtime snapshot accessors, and one system-message packet helper.
- Specific behavior/contract: Java group-entry member-loop denial should skip the leader, reject cooldown/out-of-level/already-searching members with `STR_MSG_CANT_INSTANCE_ENTER_MEMBER(memberName)` id `1400187`, and avoid queue mutation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~GamePacketTests.SmSystemMessage" --no-restore
```

Result: passed 22, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Focused Java/Maven command: skipped. No targeted Java fixture exists for `AutoGroupUtility.checkGroupRequirements`; Java source review drove the expected branch behavior and message id.
- Broad-validation trigger: none. Runtime accessors are narrow snapshot reads, and no live dispatch, shared packet primitives, persistence, scheduler, or serialization primitives changed.
- Broad .NET decision: skipped full project/solution validation because the focused command compiled the affected project/dependencies and covered the edited service and packet helper behavior.
- Why this scope is sufficient: tests assert each Java-modeled member-denial branch, message id/parameters, and queue non-mutation for this UOW.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.autogroup.AutoGroupUtility.checkGroupRequirements(...)` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService.CreateGroupMemberRequirementGuard(...)` | Utility/Service Guard | Partial | Unit Tested | Partial Parity | Member cooldown, level-range, and already-searching denial branches are covered. Harmony item validation and id-only fallback member facts remain partial. |
| `com.aionemu.gameserver.services.AutoGroupService.isSearching(...)` | `Aion.GameServer.Services.AutoGroupLookingPartyRegistrationService.IsSearching(...)` | Service Lookup | Partial | Unit Tested | Partial Parity | Member searching check is now consumed by group-entry guard and tested through an existing queued member. Full Java queue matching/lifecycle remains missing. |
| `com.aionemu.gameserver.model.team.GeneralTeam.getMembers()` | `Aion.GameServer.Services.PlayerGroupRuntime.GetMemberPlayers(...)` / `Aion.GameServer.Services.PlayerAllianceRuntime.GetMemberPlayers(...)` | Runtime Snapshot | Partial | Unit Tested | Partial Parity | C# runtimes now expose current `Player` objects for member requirement checks. Broader Java team collection semantics and offline replacement behavior remain partial. |
| `com.aionemu.gameserver.model.gameobjects.player.PortalCooldownList.isPortalUseDisabled(...)` | `Aion.GameServer.Services.PlayerPortalCooldownService.IsPortalUseDisabled(...)` | Cooldown Lookup | Partial | Unit Tested | Partial Parity | Existing cooldown logic is now consumed by autogroup member checks. Missing `InstanceCooltimeTable` data skips this C# branch and remains a documented partial behavior. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` | Packet Helper | Partial | Unit Tested | Partial Parity | Added `STR_MSG_CANT_INSTANCE_ENTER_MEMBER` id `1400187` with packet helper test. Full Java system-message catalog remains partial. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `AutoGroupLookingPartyRegistrationServiceTests.StartLooking_GroupEntryRejectsOutOfLevelMemberLikeJavaEnterMember` | Unit | Java source review | A non-leader member outside the autogroup level range blocks requester registration with message id `1400187`. | Focused C# service test from `AutoGroupUtility.checkGroupRequirements`. | Does not cover alliance-member path separately. |
| `AutoGroupLookingPartyRegistrationServiceTests.StartLooking_GroupEntryRejectsCooldownMemberLikeJavaEnterMember` | Unit | Java source review | A non-leader member with locked portal cooldown blocks requester registration with message id `1400187`. | Focused C# service test using `PlayerPortalCooldownService`. | Uses deterministic C# cooldown data, not Java runtime comparison. |
| `AutoGroupLookingPartyRegistrationServiceTests.StartLooking_GroupEntryRejectsSearchingMemberLikeJavaEnterMember` | Unit | Java source review | A non-leader member already queued for the same mask blocks requester registration without adding the requester. | Focused C# service test using existing queue state. | Full Java queue matching lifecycle remains partial. |
| `GamePacketTests.SmSystemMessage_WritesDialogTooFarMessages` | Unit | Java source review | `CantInstanceEnterMember` serializes Java id `1400187` and member-name parameter. | Focused packet helper assertion. | Full catalog remains partial. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 5
- Total artifacts ported or extended in this UOW: 5
- Total artifacts with verified parity: 0
- Total artifacts needing verification or remaining partial: 5
- Total blocked artifacts: 0
- Estimated overall migration completion: Phase 6 remains in progress; autogroup group-entry guard parity advanced, but lifecycle parity remains partial.

## Remaining Gaps

- Harmony arena item validation branch remains missing.
- C# queue matching and live auto-instance creation remain missing.
- Java battleground registration announcement branch remains missing.
- Java penalties, quick-entry refill, cancel-enter delayed removal, and full auto-instance lifecycle remain missing.
- Periodic registration real cron callback scheduling and close task handles remain partial.
- Remaining `AutoGroupConfig` settings beyond `AUTO_GROUP_ENABLE` are not fully ported.

## Commit

Commit message:

```text
[Phase 6][UOW-2375] Port autogroup member entry denials
```
