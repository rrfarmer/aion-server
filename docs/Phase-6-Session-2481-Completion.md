# Phase 6 Session 2481 Completion

## UOW

[Phase 6] UOW-2481: Add Vortex updateDefenders invitation metadata

## Status

Completed and validated with focused .NET tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/vortex/Invasion.java`

## C# Changes

- `dotnetConversion/src/Aion.GameServer/Services/VortexDefenderInvitationPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/VortexLocationServiceTests.cs`

## Implementation Notes

- Added non-live `VortexDefenderInvitationPlanService`.
- Added `VortexDefenderInvitationPlanStatus`, `VortexDefenderInvitationPlan`, and `VortexDefenderAllianceSnapshot`.
- The planner mirrors Java `Invasion.updateDefenders(Player defender)` invitation guards by:
  - skipping defenders already tracked in the invasion defender map;
  - blocking invitation when the defender alliance exists and is full;
  - recording Java request id `904306`;
  - recording `SM_QUESTION_WINDOW(904306, 0, 0)` intent only when request storage would succeed.
- Scope remains metadata-only. It does not mutate live request storage, send live packets, remove groups/alliances, create alliances, or add defenders.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `DefenderInvitationPlan_NewDefenderWithOpenAllianceRecordsQuestionWindowIntent` | Unit | `Invasion.updateDefenders` source review | New defender with available alliance/request slot records request id `904306` and question-window args `0, 0` | Focused C# test validates planned status, request id, window intent, and disabled live request/packet work | Does not execute live `RequestResponseHandler` or packet dispatch |
| `DefenderInvitationPlan_GuardsExistingDefenderAndFullAllianceLikeJava` | Unit | `Invasion.updateDefenders` source review | Existing defenders and full alliances do not install requests or send question windows | Focused C# test validates guard statuses and absent request/window metadata | Does not inspect a live Java `PlayerAlliance` |
| `DefenderInvitationPlan_RequestSlotUnavailableOmitsQuestionWindowLikeJavaPutRequestFalse` | Unit | `Invasion.updateDefenders` source review | Failed `putRequest` records request-attempt metadata but omits question-window intent | Focused C# test validates `RequestNotStored`, request id, no window intent, and no live dispatch | Does not use a live response requester |

## Validation Decision

- Changed surface: non-live Vortex defender invitation metadata and focused tests.
- Specific behavior/contract: C# updateDefenders metadata mirrors Java `Invasion.updateDefenders` guard and invitation intent by skipping existing defenders, blocking full alliances, and recording question-window request id `904306` without live request or packet dispatch.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~VortexLocationServiceTests" --no-restore
```

- Result: Passed, 48 tests. Existing nullable/analyzer warnings were emitted.
- Focused Java/Maven command: skipped; no Java source or fixtures changed and no narrow Java Vortex lifecycle/static-data fixture exists. Java source was reviewed directly.
- Broad-validation trigger: none. This UOW only adds non-live invitation metadata and tests, without enabling live request storage, packet dispatch, or alliance mutation.
- Broad .NET decision: skipped; focused validation built the affected project and covered the edited Vortex invitation metadata paths.
- Why this scope is sufficient: the new code is an inert planner over supplied defender/alliance/request-slot snapshots.

## Additional Hygiene

```powershell
git diff --check
git diff --cached --check
```

- `git diff --check` passed. Git reported an expected CRLF conversion warning for the edited test file.
- `git diff --cached --check` passed after staging. Git reported expected CRLF conversion warnings for edited/new files.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.vortex.Invasion.updateDefenders` | `Aion.GameServer.Services.VortexDefenderInvitationPlanService` | Runtime planner | Partial | Unit Tested | Partial Parity | C# models existing-defender guard, alliance-full guard, request id `904306`, request storage outcome, and question-window intent as metadata. It does not execute live response-request or packet behavior. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW(904306, 0, 0)` | `Aion.GameServer.Services.VortexDefenderInvitationPlan` | Packet intent metadata | Partial | Unit Tested | Partial Parity | C# records packet intent only when request storage would succeed. No live packet is sent. |

## Metrics

- Total Java artifacts discovered in this UOW: 2
- Total artifacts ported or extended in this UOW: 1
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 2
- Total blocked artifacts: 0
- Conservative Phase 6 parity estimate: 41%

## Remaining Risks

- Java `RequestResponseHandler.acceptRequest` behavior remains unported.
- Live request storage and packet dispatch remain disabled.
- Defender group/alliance removal before adding to the Vortex defender alliance remains metadata-only/unported.
- Live defender alliance creation and addition remain unimplemented.
