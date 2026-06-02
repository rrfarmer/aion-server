# Phase 6 Session 2311 Completion - Portal Find-Group Recruit Dialog

## Scope

Wired concrete runtime parity for portal/NPC dialog action `OPEN_INSTANCE_RECRUIT` (`105`).

Java source reviewed:

- `game-server/data/handlers/ai/portals/PortalDialogAI.java`
- `game-server/data/handlers/ai/instance/beshmundirTemple/BeshmundirsWalkAI.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- `game-server/src/com/aionemu/gameserver/model/DialogAction.java`
- `game-server/src/com/aionemu/gameserver/dataholders/AutoGroupData.java`

Java behavior used:

- `PortalDialogAI.onDialogSelect` handles `OPEN_INSTANCE_RECRUIT` by calling `FindGroupService.showInstanceGroups(player, getOwner())` and returning `true`.
- `BeshmundirsWalkAI.onDialogSelect` uses the same `OPEN_INSTANCE_RECRUIT` call.
- `FindGroupService.showInstanceGroups(Player, Npc)` asks `DataManager.AUTO_GROUP.getRecruitableInstanceMaskIds(portalNpc.getNpcId())`.
- If that lookup returns non-null, Java sends `new SM_FIND_GROUP(instanceMaskIds)` action `26`.
- This overload does not send action `10`.
- If the lookup returns null, Java sends no find-group packet.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmDialogSelect.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupClientActionPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionClientActionCompositionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`

Implemented:

- Added `CmDialogSelect.OpenInstanceRecruit = 105`.
- Added `FindGroupConnectionClientActionCompositionPlanService.CreatePortalInstanceGroupShowPlan`.
- Routed live `CM_DIALOG_SELECT` action `105` through the existing find-group portal plan.
- Sends the action `26` `SmFindGroup` packet only when the target portal NPC has recruitable masks.

Added test:

- `ProcessPacketAsync_OpenInstanceRecruitSendsPortalMaskListOnly`

The test drives live `CM_DIALOG_SELECT`, not a disabled report path. It verifies:

- portal NPC with masks sends one `SmFindGroup` action `26`,
- only the portal NPC's masks are used,
- action `10` is not sent,
- a portal NPC without masks sends nothing.

## Validation Decision

- Changed surface: live C# dialog dispatch plus a narrow find-group planner bridge.
- Specific behavior/contract: Java `OPEN_INSTANCE_RECRUIT` portal AI sends only `SM_FIND_GROUP` action `26` when the portal NPC has recruitable instance masks.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_OpenInstanceRecruitSendsPortalMaskListOnly" --no-restore
```

Result: passed 1, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain. This filtered command supplied the compile signal for the edited C# project and tests.

- Adjacent C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ShowInstanceGroupsForPortal_PlansActionTwentySixOnlyWhenPortalMasksExist" --no-restore
```

Result: passed 1, failed 0, skipped 0.

- Focused Java/Maven command:

```powershell
mvn --% -pl game-server -am -DskipTests=false -Dmaven.test.skip=false -Dtest=FindGroupMutationPostTraceCaptureTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Result: build success. `FindGroupMutationPostTraceCaptureTest` ran 32 tests, with 31 passed and 1 skipped. This is find-group source-tree sanity, not full portal AI caller proof; the portal caller behavior was verified by Java source review.

- Repository hygiene:

```powershell
git diff --check
```

Result: passed with line-ending normalization warnings only.

- Broad-validation trigger: none. The change touches live connection dispatch, so focused boundary coverage was run, but no packet primitive, crypto, persistence, scheduler, or broad shared infrastructure changed.
- Broad .NET decision: skipped full project/solution validation. The filtered boundary test and adjacent planner test directly cover the scoped Java behavior.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `ai.portals.PortalDialogAI.onDialogSelect` action `OPEN_INSTANCE_RECRUIT` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleDialogSelectAsync` | AI Handler / Client Packet Boundary | Partial | Focused Boundary Tested | Partial Parity | Live C# now handles action `105` by sending portal mask action `26` when masks exist. Other `PortalDialogAI` branches such as `INSTANCE_PARTY_MATCH` and `SELECT1_1` remain outside this UOW. |
| `ai.instance.beshmundirTemple.BeshmundirsWalkAI.onDialogSelect` action `OPEN_INSTANCE_RECRUIT` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleDialogSelectAsync` | AI Handler / Client Packet Boundary | Partial | Focused Boundary Tested | Partial Parity | Same action `105` runtime route covers this shared Java behavior, but the rest of `BeshmundirsWalkAI` is not ported here. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.showInstanceGroups(Player, Npc)` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.ShowInstanceGroupsForPortal` and `FindGroupConnectionClientActionCompositionPlanService.CreatePortalInstanceGroupShowPlan` | Service | Partial | Unit Tested / Boundary Tested | Partial Parity | Java source reviewed; C# sends action `26` only for non-null portal NPC masks and sends no action `10`. Encrypted socket bytes and full AI dispatch parity remain open. |
| `com.aionemu.gameserver.model.DialogAction.OPEN_INSTANCE_RECRUIT` | `Aion.GameServer.Network.Aion.ClientPackets.CmDialogSelect.OpenInstanceRecruit` | Constant | Complete | Boundary Tested | Verified Parity | Java constant `105` matched in C# and exercised through parsed `CM_DIALOG_SELECT` payload. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `ProcessPacketAsync_OpenInstanceRecruitSendsPortalMaskListOnly` | Boundary Runtime | Java source review of `PortalDialogAI`, `BeshmundirsWalkAI`, and `FindGroupService.showInstanceGroups(Player, Npc)` | Live C# `CM_DIALOG_SELECT` action `105` sends only action `26` for portal NPC masks and sends nothing when masks are missing. | Focused C# boundary execution plus Java source review and targeted find-group Maven sanity. | Does not prove encrypted socket bytes, real-client dialog flow, `INSTANCE_PARTY_MATCH`, or `SELECT1_1`. |
| `ShowInstanceGroupsForPortal_PlansActionTwentySixOnlyWhenPortalMasksExist` | Unit | Java source review of `FindGroupService.showInstanceGroups(Player, Npc)` | Portal planner sends action `26` only for non-null mask lists. | Adjacent focused C# unit test. | Does not exercise live `CM_DIALOG_SELECT`; covered by the boundary test above. |

## Remaining Gaps

- No verified parity claim for full `CM_DIALOG_SELECT`.
- No encrypted socket or real-client frame comparison.
- `PortalDialogAI.INSTANCE_PARTY_MATCH` action `77` and `SELECT1_1` action `1012` remain unported/unchecked in this unit.
- Full group/alliance invite response lifecycle parity remains open.
- Full `BeshmundirsWalkAI` instance-entry behavior remains outside this UOW.

## Commit

Commit message:

```text
[Phase 6][UOW-2311] Wire portal find-group recruit masks
```

## Next Recommended UOW

Continue concrete runtime parity adjacent to portal/find-group dialogs. The smallest next safe scope is `PortalDialogAI.SELECT1_1` (`1012`): Java sends `SM_DIALOG_WINDOW(objectId, 1182)` when the player is not in a team and `DataManager.AUTO_GROUP.getRecruitableInstanceMaskIds(getNpcId()) != null`.

Alternative concrete scope: inspect and wire `PortalDialogAI.INSTANCE_PARTY_MATCH` (`77`) if the C# auto-group packet/service surface is ready enough for focused boundary coverage.
