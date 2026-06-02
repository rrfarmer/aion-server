# Phase 6 Session 2310 Completion - CM_FIND_GROUP Target-NPC Instance Masks

## Scope

Added live C# boundary coverage for `CM_FIND_GROUP` action `10` target-NPC instance-mask lookup.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- `game-server/src/com/aionemu/gameserver/dataholders/AutoGroupData.java`

Java behavior used:

- Action `10` calls `FindGroupService.showInstanceGroups(player, false)`.
- Java sends action `26` only when `!isUpdate && GroupConfig.FORM_INSTANCE_GROUP_ANYWHERE`.
- Inside that enabled branch, Java prefers target-NPC masks from `DataManager.AUTO_GROUP.getRecruitableInstanceMaskIds(npc.getNpcId())`.
- If the target-NPC lookup returns null, Java falls back to all recruitable instance mask ids.
- Java then sends action `10` with same-race instance-group rows.
- Action `13` calls `showInstanceGroups(player, true)` and sends only action `10`.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

Updated:

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`

Added `ProcessPacketAsync_ActionTenUsesTargetNpcMaskLookup`, which:

- creates a target portal NPC in the test world,
- configures `FormInstanceGroupAnywhere = true`,
- configures `AutoGroupTable` with one mask for the target NPC and one unrelated global recruitable mask,
- verifies live action `10` sends action `26` with only the target-NPC mask before action `10`,
- verifies live action `13` still sends only action `10`.

The test fixture now accepts an optional `World` and passes it into both the find-group composition service and `GameServerConnection`. No product code changed in this UOW.

## Validation Decision

- Changed surface: test-only live boundary parity evidence plus test fixture plumbing for world-backed target lookup.
- Specific behavior/contract: live C# action `10` uses target-NPC recruitable mask ids when anywhere formation is enabled, and action `13` remains update-list only.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_ActionTenUsesTargetNpcMaskLookup" --no-restore
```

Result: passed 1, failed 0, skipped 0. The command built the affected project and dependencies. Pre-existing nullable/analyzer warnings remain.

- Adjacent C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_ActionTenAndThirteenSendInstanceGroupShowLists|FullyQualifiedName~CreateDisabledFindGroupBoundaryPlan_ActionTen|FullyQualifiedName~CreateDisabledFindGroupBoundaryPlan_ActionThirteen" --no-restore
```

Result: passed 4, failed 0, skipped 0.

- Focused Java/Maven command:

```powershell
mvn --% -pl game-server -am -DskipTests=false -Dmaven.test.skip=false -Dtest=FindGroupMutationPostTraceCaptureTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Result: build success. `FindGroupMutationPostTraceCaptureTest` ran 32 tests, with 31 passed and 1 skipped.

- Repository hygiene:

```powershell
git diff --check
```

Result: passed with line-ending normalization warnings only.

- Broad-validation trigger: none. This was a test-only parity-evidence unit with fixture-only plumbing.
- Broad .NET decision: skipped full project/solution validation. The focused filtered tests supplied the compile signal and directly covered the Java-derived behavior.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `10` | `Aion.GameServer.Network.Aion.GameServerConnection` / `FindGroupConnectionClientActionCompositionPlanService` / `FindGroupRecruitmentPlanService.ShowInstanceGroupsForClient` | Client Packet Handler / Service | Partial | Focused Boundary Tested | Partial Parity | Live C# coverage now proves target-NPC mask preference when anywhere formation is enabled. Verified parity is not claimed; encrypted bytes remain open. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.showInstanceGroups(Player, boolean)` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.ShowInstanceGroupsForClient` | Service | Partial | Boundary Tested / Java Fixture Tested | Partial Parity | Corrected Java rule documented: action `26` is gated by `FORM_INSTANCE_GROUP_ANYWHERE`; target NPC is preferred inside that branch, not when the option is disabled. |
| `com.aionemu.gameserver.dataholders.AutoGroupData.getRecruitableInstanceMaskIds` | `Aion.GameServer.Dataholders.AutoGroupTable.GetRecruitableInstanceMaskIds` | Data Holder | Partial | Boundary Tested | Partial Parity | Representative portal-NPC mask lookup is covered through live action `10`; full XML load-count parity remains outside this UOW. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `ProcessPacketAsync_ActionTenUsesTargetNpcMaskLookup` | Boundary Runtime | Java source review plus targeted Java find-group fixture run | Live C# action `10` sends action `26` with target-NPC masks before action `10`, and action `13` sends only action `10`. | Focused C# boundary execution and Java source/fixture validation. | Does not compare encrypted socket bytes or full auto-group XML loading. |

## Remaining Gaps

- No verified parity claim for `CM_FIND_GROUP`.
- No encrypted socket or real-client frame comparison.
- Full Java `TemporaryPlayerTeam`, group, alliance, and invite response lifecycle parity remains outside the find-group unit.
- `FindGroupService.showInstanceGroups(player, Npc portalNpc)` overload caller coverage remains separate from `CM_FIND_GROUP` action `10`.

## Commit

Commit message:

```text
[Phase 6][UOW-2310] Cover find-group target NPC masks
```

## Next Recommended UOW

Inspect the remaining find-group adjacent behavior and choose the next concrete runtime branch. Good candidates are the `FindGroupService.showInstanceGroups(player, Npc portalNpc)` overload if a C# portal/NPC dialog boundary calls it, or the group/alliance invite response lifecycle that action `12` now reaches.
