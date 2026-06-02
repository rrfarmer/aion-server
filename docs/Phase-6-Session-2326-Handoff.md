# Phase 6 Session 2326 Handoff - Portal Group Bypass Source

## Startup Instructions

Read these first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2326-Completion.md`
- `docs/Phase-6-Session-2326-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive.

Java remains the source of truth. Use focused validation by default. Full `.NET` project tests, solution tests, and solution builds require a documented broad-validation trigger.

## Current State

Last completed UOW: UOW-2326, Java-shaped portal group bypass thresholds are wired into live C# portal entry preparation.

Completed portal slices relevant to the next work:

- Fresh group portal continuation allocates the next runtime instance, registers the team id, transfers the player, applies cooldown, and preserves nonzero difficulty metadata.
- Beshmundir accepted difficulty response carries Java difficulty `2` into fresh portal allocation metadata.
- Bypassed group portal validation scans member object ids for a solo registered instance before fresh group allocation.
- Bypassed group portal continuation transfers into the found member instance and does not register the team id.
- Live preparation derives `bypassGroupRequirement` from `Player.AccessLevel >= gameserver.administration.instance.enter_all` or `Player.AccountMembership >= gameserver.instances.group.requirement`.

Still not proven:

- No-group `!instanceGroupReq` branch for group-sized portals.
- Java spawn filtering via `SpawnEngine.spawnInstance(instance, difficultyId, ownerId)`.
- Alliance/league portal allocation.
- Java range observer auto-deny behavior for AI requests.
- Real-client/encrypted socket bytes for these branches.

## Commits Made

- `c12d00c4c [Phase 6][UOW-2315] Reject solo Beshmundir walk entry`
- `10b528b55 [Phase 6][UOW-2316] Show Beshmundir leader path dialog`
- `ff1354d09 [Phase 6][UOW-2317] Reject Beshmundir non-leader closed entry`
- `7a644b31f [Phase 6][UOW-2318] Register Beshmundir difficulty request`
- `c95ad6c8b [Phase 6][UOW-2319] Transfer registered group portals`
- `9d49df0a2 [Phase 6][UOW-2320] Move Beshmundir non-leader into open instance`
- `03fd0b3fa [Phase 6][UOW-2321] Move Beshmundir accepted difficulty response`
- `dd42ddc77 [Phase 6][UOW-2322] Allocate fresh group portal instances`
- `cc83107b0 [Phase 6][UOW-2323] Prove Beshmundir fresh group allocation`
- `f449bcee2 [Phase 6][UOW-2324] Carry portal difficulty into allocation`
- `db322f7cb [Phase 6][UOW-2325] Reuse group member solo portal instance`
- `[Phase 6][UOW-2326] Wire portal group bypass thresholds`

## Files Changed In Last UOW

- `dotnetConversion/src/Aion.GameServer/Configuration/GameServerOptions.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerOptionsTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `docs/Phase-6-Session-2326-Completion.md`
- `docs/Phase-6-Session-2326-Handoff.md`

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.teleport.PortalService.port` `instanceGroupReq` calculation | `Aion.GameServer.Services.PlayerEnterWorldService.PreparePortalEntryAsync` | Service Boundary | Partial | Focused Boundary Tested | Partial Parity | C# now derives group requirement bypass from Java-shaped access and membership thresholds before validation. Other Java admin-enter-all requirement interactions remain partial. |
| `com.aionemu.gameserver.configs.administration.AdminConfig.INSTANCE_ENTER_ALL` | `Aion.GameServer.Configuration.GameServerAdministrationOptions.InstanceEnterAllAccessLevel` | Config | Complete | Unit Tested | Partial Parity | Java key/default loaded and override-tested. Other admin config fields are outside this UOW. |
| `com.aionemu.gameserver.configs.main.MembershipConfig.INSTANCES_GROUP_REQ` | `Aion.GameServer.Configuration.GameServerMembershipOptions.InstancesGroupRequirement` | Config | Complete | Unit Tested | Partial Parity | Java key/default loaded and override-tested. Other membership requirement fields remain only partially modeled. |
| `com.aionemu.gameserver.model.gameobjects.player.Player.hasAccess` / `hasPermission` | `Aion.GameServer.Model.GameObjects.Player.AccessLevel` / `AccountMembership` comparisons in `PlayerEnterWorldService` | Model / Service Use | Partial | Focused Boundary Tested | Partial Parity | Comparison semantics match Java `>=` for this portal branch. No general C# `HasAccess`/`HasPermission` API was introduced. |

## Validation From Last UOW

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests.PreparePortalEntry_DerivesGroupRequirementBypassFromJavaAccountThresholds|FullyQualifiedName~GameServerOptionsTests.LoadFromJavaConfig_ReadsCoreAndNetworkDefaults|FullyQualifiedName~GameServerOptionsTests.LoadFromJavaConfig_AppliesMyGsOverridesLast|FullyQualifiedName~PortalEntryValidationServiceTests.ValidatePortalEntryPlan_GroupBypassScansMemberSoloRegistrationsLikeJava|FullyQualifiedName~GameServerConnectionInstanceCooldownTests.QueuePortalContinueTransferAsync_GroupBypassMemberSoloInstanceTransfersWithoutRegisteringTeam" --no-restore
```

Result: passed 6, failed 0, skipped 0. Pre-existing nullable/analyzer warnings remain.

Focused Java/Maven validation: skipped because no targeted Java fixture exists for this runtime permission branch. Java source review was used as source-of-truth evidence.

Broad-validation trigger: none.

Broad .NET decision: skipped full project/solution validation after focused tests passed and supplied the compile signal for the affected project.

## Next Sequential UOW

Recommended next production scope: implement Java no-group `!instanceGroupReq` group-sized portal branch.

Java artifacts to inspect:

- `game-server/src/com/aionemu/gameserver/services/teleport/PortalService.java`
- `game-server/src/com/aionemu/gameserver/services/instance/InstanceService.java`

C# artifacts likely involved:

- `dotnetConversion/src/Aion.GameServer/Services/PortalEntryValidationService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerTeleportService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PortalEntryValidationServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInstanceCooldownTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`

Specific behavior to prove: for maxPlayers `3` or `6`, no group present, and `instanceGroupReq == false`, Java uses the player object id lookup/allocation path and transfers into a fresh instance without team registration.

Focused C# command should start with:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests.PreparePortalEntry_DerivesGroupRequirementBypassFromJavaAccountThresholds|FullyQualifiedName~PortalEntryValidationServiceTests.ValidatePortalEntryPlan_GroupBypassScansMemberSoloRegistrationsLikeJava|FullyQualifiedName~GameServerConnectionInstanceCooldownTests.QueuePortalContinueTransferAsync_GroupBypassMemberSoloInstanceTransfersWithoutRegisteringTeam" --no-restore
```

Add the exact new no-group test names after implementation. Do not run a full project test or solution build unless a named broad-validation trigger is documented first.

Focused Java/Maven command: not expected unless a targeted Java fixture is added; Java source review is likely the practical source-of-truth evidence.

Broad-validation trigger: none expected for a narrow portal planner/continuation unit. If live side effects expand beyond portal preparation/continuation, document the trigger before broad validation.

## Safe Candidates

- Implement Java no-group `!instanceGroupReq` group-sized portal branch.
- Spawn-engine difficulty filtering using `WorldMapInstanceRuntimeState.DifficultyId`.
- Alliance/league fresh allocation after group behavior remains stable.

Avoid:

- Full .NET project tests or solution builds without a documented trigger.
- Evidence/reporting-only units.
- Beshmundir-only teleport shortcuts that bypass Java `PortalService.port(...)`.
- Updating `docs/PHASE-6-PROGRESS.md`.
