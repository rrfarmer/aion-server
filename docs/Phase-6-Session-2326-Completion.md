# Phase 6 Session 2326 Completion - Portal Group Bypass Source

## Scope

Wired Java's `instanceGroupReq == false` account source into live C# portal entry preparation.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/teleport/PortalService.java`
- `game-server/src/com/aionemu/gameserver/configs/administration/AdminConfig.java`
- `game-server/src/com/aionemu/gameserver/configs/main/MembershipConfig.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/Player.java`

Java behavior used:

- `instanceGroupReq` is false when `player.hasAccess(AdminConfig.INSTANCE_ENTER_ALL)` or `player.hasPermission(MembershipConfig.INSTANCES_GROUP_REQ)`.
- `Player.hasAccess(byte)` compares account access level with `>=`.
- `Player.hasPermission(byte)` compares account membership with `>=`.
- Java defaults are `gameserver.administration.instance.enter_all = 2` and `gameserver.instances.group.requirement = 10`.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Configuration/GameServerOptions.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerOptionsTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`

Implemented:

- Added `GameServerMembershipOptions.InstancesGroupRequirement` from Java key `gameserver.instances.group.requirement`, default `10`.
- Added `GameServerAdministrationOptions.InstanceEnterAllAccessLevel` from Java key `gameserver.administration.instance.enter_all`, default `2`.
- `PlayerEnterWorldService.PreparePortalEntryAsync(...)` now derives effective group-requirement bypass from `Player.AccessLevel` or `Player.AccountMembership` when the explicit bypass flag is not supplied.
- Live preparation can now reach the previously ported member solo-instance scan through Java-shaped account thresholds.

Known limitations:

- Java no-group `!instanceGroupReq` group-sized allocation branch remains unported.
- Java admin `INSTANCE_ENTER_ALL` also participates in other checks in `PortalService.port`; this UOW only wired the group-requirement source needed by the current portal allocation branch.
- Alliance/league portal allocation remains unsupported.

## Validation Decision

- Changed surface: production portal preparation plus config option loading.
- Specific behavior/contract: live portal preparation supplies `bypassGroupRequirement` when `AccessLevel >= INSTANCE_ENTER_ALL` or `AccountMembership >= INSTANCES_GROUP_REQ`, and config defaults/overrides use Java keys.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests.PreparePortalEntry_DerivesGroupRequirementBypassFromJavaAccountThresholds|FullyQualifiedName~GameServerOptionsTests.LoadFromJavaConfig_ReadsCoreAndNetworkDefaults|FullyQualifiedName~GameServerOptionsTests.LoadFromJavaConfig_AppliesMyGsOverridesLast|FullyQualifiedName~PortalEntryValidationServiceTests.ValidatePortalEntryPlan_GroupBypassScansMemberSoloRegistrationsLikeJava|FullyQualifiedName~GameServerConnectionInstanceCooldownTests.QueuePortalContinueTransferAsync_GroupBypassMemberSoloInstanceTransfersWithoutRegisteringTeam" --no-restore
```

Result: passed 6, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

- Focused Java/Maven command: skipped. No targeted Java fixture exists for this runtime permission branch; Java source review was used as source-of-truth evidence.
- Broad-validation trigger: none. The change is a narrow service/config path and focused tests built the affected project/dependencies.
- Broad .NET decision: skipped full project/solution validation.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.teleport.PortalService.port` `instanceGroupReq` calculation | `Aion.GameServer.Services.PlayerEnterWorldService.PreparePortalEntryAsync` | Service Boundary | Partial | Focused Boundary Tested | Partial Parity | C# now derives group requirement bypass from Java-shaped access and membership thresholds before validation. Other Java admin-enter-all requirement interactions remain partial. |
| `com.aionemu.gameserver.configs.administration.AdminConfig.INSTANCE_ENTER_ALL` | `Aion.GameServer.Configuration.GameServerAdministrationOptions.InstanceEnterAllAccessLevel` | Config | Complete | Unit Tested | Partial Parity | Java key/default loaded and override-tested. Other admin config fields are outside this UOW. |
| `com.aionemu.gameserver.configs.main.MembershipConfig.INSTANCES_GROUP_REQ` | `Aion.GameServer.Configuration.GameServerMembershipOptions.InstancesGroupRequirement` | Config | Complete | Unit Tested | Partial Parity | Java key/default loaded and override-tested. Other membership requirement fields remain only partially modeled. |
| `com.aionemu.gameserver.model.gameobjects.player.Player.hasAccess` / `hasPermission` | `Aion.GameServer.Model.GameObjects.Player.AccessLevel` / `AccountMembership` comparisons in `PlayerEnterWorldService` | Model / Service Use | Partial | Focused Boundary Tested | Partial Parity | Comparison semantics match Java `>=` for this portal branch. No general C# `HasAccess`/`HasPermission` API was introduced. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `PreparePortalEntry_DerivesGroupRequirementBypassFromJavaAccountThresholds` | Boundary Unit | Java source review of `PortalService.port`, `Player.hasAccess`, and `Player.hasPermission` | Access level `2` or membership `10` causes live preparation to reuse the member solo-instance scan and leave team id unregistered. | Focused C# boundary test plus Java source review. | Does not cover other admin-enter-all checks or real-client bytes. |
| `LoadFromJavaConfig_ReadsCoreAndNetworkDefaults` / `LoadFromJavaConfig_AppliesMyGsOverridesLast` | Config Unit | Java config annotations in `AdminConfig` and `MembershipConfig` | Default and override values for the two portal threshold keys are loaded. | Focused C# config tests plus Java source review. | Does not prove every admin/membership key. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported/extended in this UOW: 4
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: unchanged, conservatively partial.

## Remaining Gaps

- Java no-group `!instanceGroupReq` group-sized allocation branch remains unported.
- Java spawn filtering via `SpawnEngine.spawnInstance(instance, difficultyId, ownerId)` remains unported.
- Alliance/league fresh allocation remains unsupported.
- Other Java admin/membership portal requirement bypasses remain partial.
- Real-client/encrypted socket bytes remain unverified.

## Commit

Commit message:

```text
[Phase 6][UOW-2326] Wire portal group bypass thresholds
```

## Next Recommended UOW

Implement Java no-group `!instanceGroupReq` group-sized portal branch: when default group requirement is disabled and the player is not grouped, Java allocates a group-sized instance for the player object id path and transfers the player without team registration.
