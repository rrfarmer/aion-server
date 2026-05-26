# Phase 6AAA Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1191
Status: Phase 6 continues; player-mail send-cost pricing is now staged and used by the C# send-mail handler, but live runtime price facts and broader mail side effects remain incomplete.

## Session Summary

UOW-1191 returned to the Phase 6 price-consumer map and extracted Java `MailService.sendMail` cost calculation into a pure C# planner. The existing C# send-mail handler now delegates final Kinah calculation to that planner, adding the missing `PricesService.getPriceForService(...)` step while leaving persistence and fanout flow unchanged.

Files changed:

- `dotnetConversion/src/Aion.GameServer/Services/MailSendCostPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/MailSendCostPlanServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `docs/Phase-6-PricesService-Consumer-Map.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6AAA-Completion.md`

## What Changed

- Added `MailSendCostPlanService` and `MailSendCostPlan`.
- Ported Java `MailService.getQualityPriceRate`:
  - `MYTHIC` / `EPIC`: `0.05`;
  - `UNIQUE` / `LEGEND`: `0.04`;
  - `RARE`: `0.03`;
  - default: `0.02`.
- Ported Java `MailService.sendMail` cost calculation:
  - normal letter base cost/factor: `10` / `1`;
  - express letter base cost/factor: `500` / `5`;
  - attached-Kinah commission: `(long)(attachedKinah * 0.01f * costFactor)`;
  - attached-item commission: `(long)(template.Price * qualityRate * attachedItemCount * costFactor)`;
  - service price: `PricesService.GetPriceForService(baseCost + commissions, senderRace, ...)`;
  - final total: `servicePrice + attachedKinah`.
- Replaced the local `GameServerConnection.HandleSendMailAsync` formula with `MailSendCostPlanService.CreatePlan(...)`.
- Updated the price consumer map to mark player mail send-cost pricing as partial/unit-tested.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "MailSendCostPlanServiceTests|PricesServiceTests" --nologo` passed 12 tests.
- No Java runtime comparison was run, so parity remains `Needs Verification`.

## Migration Parity Table - UOW-1191

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.mail.MailService` | `Aion.GameServer.Services.MailSendCostPlanService` / `Aion.GameServer.Network.Aion.GameServerConnection.HandleSendMailAsync` | Service / Connection Handler | Partial | Unit Tested | Needs Verification | Send-cost base fee, cost factor, commissions, item-quality rate, central price formula, and final attached-kinah addition are ported. Recipient validation, courier-pass consumption, attachment persistence, mailbox fanout, rollback behavior, and packet ordering remain broader live risks. |
| `com.aionemu.gameserver.services.trade.PricesService` | `Aion.GameServer.Services.PricesService` | Service / Utility Dependency | Partial | Unit Tested | Needs Verification | Mail send cost now uses the central service formula. No live `Influence` source or Java runtime comparison was used. |
| `com.aionemu.gameserver.model.templates.item.ItemTemplate` | `Aion.GameServer.Dataholders.ItemTemplateSummary` | DTO / Static Data Dependency | Partial | Unit Tested | Needs Verification | Planner reads `Price` and string `Quality`. Java uses `ItemQuality` enum and template price. Static-data normalization remains a verification risk. |
| `com.aionemu.gameserver.model.gameobjects.LetterType` | C# mail letter type id constants in `MailSendCostPlanService` | Enum / Protocol Value | Partial | Unit Tested | Needs Verification | Normal `0` and express `1` are represented as constants. Java `BLACKCLOUD` rejection remains in the caller, not the planner. |
| `com.aionemu.gameserver.model.gameobjects.player.Player` | `Aion.GameServer.Model.GameObjects.Player` / sender race string | Model / Runtime Fact Source | Partial | Unit Tested | Needs Verification | Planner accepts sender race string for `PricesService`; Java uses `Race` enum. Live config/influence plumbing remains absent. |
| `com.aionemu.gameserver.model.siege.Influence` | `Aion.GameServer.Services.PriceInfluenceRates` | Runtime Fact Input | Not Started | Unit Tested | Needs Verification | Tests inject influence rates. The send-mail handler currently uses default price facts because live influence sourcing is still missing. |

Tests added or updated:

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `MailSendCostPlanServiceTests.GetQualityPriceRate_UsesJavaMailQualityMapping` | Unit | Java `MailService.getQualityPriceRate` | Validates item-quality commission rates for mythic, epic, unique, legend, rare, and default qualities. | Deterministic source-derived expectations. | C# quality is a string, not Java `ItemQuality` enum. |
| `MailSendCostPlanServiceTests.CreatePlan_UsesJavaMailCommissionsAndPricesService` | Unit | Java `MailService.sendMail`; Java `PricesService.getPriceForService` | Validates express base cost/factor, attached-kinah commission, attached-item commission, price-service adjustment, and final attached-kinah addition. | Deterministic source-derived expectation with Java-style `float` commission truncation and price-service truncation. | No Java runtime execution; live handler uses default price facts. |
| `MailSendCostPlanServiceTests.CreatePlan_DefaultNormalLetterMatchesJavaBaseline` | Unit | Java default `MailService.sendMail` normal-letter path | Validates no-attachment normal mail remains `10` with default price facts. | Deterministic source-derived expectation. | No recipient validation, persistence, or packet behavior covered. |

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 pure mail send-cost planner plus 1 handler call-site replacement and 3 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: 3 grouped categories: live influence/config sourcing, mail persistence/fanout rollback, and concrete packet-order validation
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Live send-mail config/influence facts are not wired into `HandleSendMailAsync`; it uses default price facts.
- Courier pass handling for untradeable mail items remains live-handler logic outside the cost planner.
- Mail persistence, attachment item persistence, rollback behavior, recipient mailbox fanout, and packet ordering remain partial.
- C# item quality and race are strings, not Java enums.
- No Java runtime comparison was executed.
- No date/time behavior changed except the existing live mail timestamp path remains outside this planner; no reflection behavior changed; serialization remains covered only indirectly by existing mail packet tests.

## Next Recommended Unit of Work

Primary next unit:

- Add a pure broker-registration commission calculator from Java `BrokerService` before touching live broker registration.

Fallback units:

- Add an `SM_SELL_ITEM` packet plan and byte tests.
- Audit paid teleport price deduction before touching live teleport handler paths.

Defer live mail influence/config routing until a shared runtime price-fact source exists.
