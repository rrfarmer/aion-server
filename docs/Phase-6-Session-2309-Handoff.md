# Phase 6 Session 2309 Handoff - CM_FIND_GROUP Current-Team Recruitment Ids

## Startup Instructions

For the next session, read these documents first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2309-Completion.md`
- `docs/Phase-6-Session-2309-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive; current working state lives in the latest completion and handoff documents.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

UOW-2309 added live C# evidence for `CM_FIND_GROUP` action `1` and action `3` current-team recruitment ids.

Concrete evidence:

- The active player can carry `TeamMembership`, `CurrentTeamId`, and `CurrentTeamMemberObjectIds`.
- Live action `3` updates the recruitment row keyed by `CurrentTeamId`, not the solo player object id.
- Live action `1` removes the recruitment row keyed by `CurrentTeamId`.
- Action `1` broadcasts an `SmFindGroup` remove packet containing the team id and same-race recipients.
- Missing action `3` rows still emit no packet side effects.

Still not proven:

- Verified parity.
- Encrypted socket or real-client frame comparison.
- Full Java `TemporaryPlayerTeam` object parity.
- Target-NPC action `10` instance-mask lookup behavior.
- Full group/alliance invite response lifecycle parity.

## Validation From Last Session

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_ActionOneAndThreeUseCurrentTeamId" --no-restore
```

Result: passed 1, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Adjacent C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CreateDisabledFindGroupBoundaryPlan_ActionOne|FullyQualifiedName~CreateDisabledFindGroupBoundaryPlan_ActionThree" --no-restore
```

Result: passed 2, failed 0, skipped 0. An earlier parallel invocation hit a compiler file-lock on `Aion.GameServer.dll`; the command passed when rerun serially.

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

Broad-validation trigger: none. This was a test-only parity-evidence unit.

Broad .NET decision: full project/solution validation was skipped. The focused filtered test supplied the compile signal and directly covered the Java-derived behavior.

## Next Sequential UOW

Add live coverage for `CM_FIND_GROUP` action `10` target-NPC instance-mask lookup:

- action `10`: show instance groups

Java behavior to inspect:

- `CM_FIND_GROUP.readImpl/runImpl` action `10`
- `FindGroupService.showInstanceGroups(Player player, boolean update)`
- `player.getTarget() instanceof Npc`
- `DataManager.AUTO_GROUP.getRecruitableInstanceMaskIds(npc.getNpcId())`
- `SM_FIND_GROUP(26, instanceMaskIds)` and `SM_FIND_GROUP(10, instanceGroups)`

Expected Java behavior:

- Action `10` calls `showInstanceGroups(player, false)`.
- If the global anywhere option is enabled, Java sends action `26` with all recruitable instance mask ids before action `10`.
- If the anywhere option is disabled and the player targets an NPC with recruitable masks, Java sends action `26` with target-NPC masks before action `10`.
- If neither condition provides masks, Java sends only action `10`.
- Action `13` still calls `showInstanceGroups(player, true)` and sends only action `10`.

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionClientActionCompositionPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupRecruitmentPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`

## Focused Validation Recipe For Next UOW

Specific behavior to prove: live C# action `10` uses target-NPC recruitable mask ids when anywhere formation is disabled, and action `13` still emits only the update list.

Expected C# command after adding live coverage:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_ActionTenUsesTargetNpcMaskLookup" --no-restore
```

Adjacent C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_ActionTenAndThirteenSendInstanceGroupShowLists|FullyQualifiedName~CreateDisabledFindGroupBoundaryPlan_ActionTen|FullyQualifiedName~CreateDisabledFindGroupBoundaryPlan_ActionThirteen" --no-restore
```

If the adjacent filter catches unrelated tests or is slow, run the new live test first and document residual risk.

Expected Java/Maven command:

```powershell
mvn --% -pl game-server -am -DskipTests=false -Dmaven.test.skip=false -Dtest=FindGroupMutationPostTraceCaptureTest -Dsurefire.failIfNoSpecifiedTests=false test
```

If a narrower Java target-NPC fixture is found during discovery, use that instead and document the command.

Broad-validation trigger: none expected if this only adds focused live boundary coverage or narrow composition wiring without changing shared packet primitives, persistence, crypto, scheduler, or broad dispatch rules.

## Safe Candidates

- Add target-NPC action `10` mask lookup coverage if world/NPC and `AutoGroupTable` setup is easy.
- Add explicit action `13` regression coverage only as the adjacent guard for action `10`.
- Avoid new readiness/report layers unless they unblock concrete live parity behavior.
