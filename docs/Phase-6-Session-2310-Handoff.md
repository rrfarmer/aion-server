# Phase 6 Session 2310 Handoff - CM_FIND_GROUP Target-NPC Instance Masks

## Startup Instructions

For the next session, read these documents first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2310-Completion.md`
- `docs/Phase-6-Session-2310-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive; current working state lives in the latest completion and handoff documents.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

UOW-2310 added live C# evidence for `CM_FIND_GROUP` action `10` target-NPC instance-mask lookup.

Concrete evidence:

- Test fixture can now pass `World` into the find-group composition service and `GameServerConnection`.
- Live action `10` resolves the active player's target object from `World`.
- With `FormInstanceGroupAnywhere = true`, live action `10` sends action `26` with the target-NPC mask list before action `10`.
- The target-NPC mask list is preferred over the global recruitable mask list.
- Live action `13` still sends only action `10`.

Important correction:

- Prior handoff wording said target-NPC masks were expected when anywhere formation was disabled. Java source shows the opposite: action `26` is only sent when `GroupConfig.FORM_INSTANCE_GROUP_ANYWHERE` is enabled; target-NPC lookup is preferred inside that enabled branch.

Still not proven:

- Verified parity.
- Encrypted socket or real-client frame comparison.
- Full auto-group XML loading parity for this branch.
- `FindGroupService.showInstanceGroups(player, Npc portalNpc)` overload caller coverage.
- Full group/alliance invite response lifecycle parity.

## Validation From Last Session

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_ActionTenUsesTargetNpcMaskLookup" --no-restore
```

Result: passed 1, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Adjacent C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_ActionTenAndThirteenSendInstanceGroupShowLists|FullyQualifiedName~CreateDisabledFindGroupBoundaryPlan_ActionTen|FullyQualifiedName~CreateDisabledFindGroupBoundaryPlan_ActionThirteen" --no-restore
```

Result: passed 4, failed 0, skipped 0.

Focused Java/Maven validation:

```powershell
mvn --% -pl game-server -am -DskipTests=false -Dmaven.test.skip=false -Dtest=FindGroupMutationPostTraceCaptureTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Result: build success. `FindGroupMutationPostTraceCaptureTest` ran 32 tests, with 31 passed and 1 skipped.

Repository hygiene:

```powershell
git diff --check
```

Result: passed with line-ending normalization warnings only.

Broad-validation trigger: none. This was a test-only parity-evidence unit with fixture-only plumbing.

Broad .NET decision: full project/solution validation was skipped. The focused filtered tests supplied the compile signal and directly covered the Java-derived behavior.

## Next Sequential UOW

Inspect find-group adjacent behavior and choose the next concrete runtime branch.

Recommended first discovery target:

- `FindGroupService.showInstanceGroups(player, Npc portalNpc)` overload and its Java callers.

Java behavior to inspect:

- callers of `showInstanceGroups(Player, Npc)`
- portal/NPC dialog handlers that may expose server-wide recruitment masks
- `DataManager.AUTO_GROUP.getRecruitableInstanceMaskIds(portalNpc.getNpcId())`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupRecruitmentPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- portal/NPC dialog services if a C# caller exists
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs` or a narrower portal test class

Alternative concrete candidate:

- Continue from action `12` into group/alliance invite response lifecycle parity, because `CM_FIND_GROUP` now dispatches those request side effects but the full response lifecycle is still a known gap.

## Focused Validation Recipe For Next UOW

For `showInstanceGroups(player, Npc portalNpc)` caller coverage:

Specific behavior to prove: the C# caller, if present, sends action `26` only when the portal NPC has recruitable masks, matching Java's overload.

Expected C# command after adding coverage:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroup|FullyQualifiedName~Portal" --no-restore
```

Narrow this filter during discovery to the exact edited test class; do not run it broadly if a specific class is found.

Expected Java/Maven command:

```powershell
mvn --% -pl game-server -am -DskipTests=false -Dmaven.test.skip=false -Dtest=FindGroupMutationPostTraceCaptureTest -Dsurefire.failIfNoSpecifiedTests=false test
```

If a narrower Java portal fixture is found during discovery, use that instead and document the command.

Broad-validation trigger: none expected unless the UOW changes shared portal dispatch, packet primitives, persistence, crypto, scheduler, or broad runtime routing.

## Safe Candidates

- Find and cover the C# caller for `ShowInstanceGroupsForPortal` if it exists.
- If no caller exists, document the missing runtime boundary and move to group/alliance invite response lifecycle parity.
- Avoid new readiness/report layers unless they unblock concrete runtime behavior.
