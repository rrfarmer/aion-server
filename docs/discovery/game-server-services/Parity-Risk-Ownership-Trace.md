# Game-Server Parity-Risk Ownership Trace

Date: 2026-05-29

## Purpose

This document narrows the "likely exists but still needs ownership tracing" bucket.

It does not claim full parity. It documents where C# ownership is now concrete enough to separate:

- implemented or strongly represented surfaces
- planned or partially represented surfaces
- unresolved behavior that still needs a deeper pass

## Estimated Completion Snapshot

These estimates convert the current ownership trace into explicit complete-versus-remaining numbers for the parity-risk queue.

| Area | Estimated complete | Estimated remaining | Interpretation |
| --- | ---: | ---: | --- |
| `reward` | 60% | 40% | hidden/renamed ownership; bonus, faction, starter-kit, and system-mail reward paths are clear, but advent, veteran, and web rewards remain open |
| `duel` | 55% | 45% | partial runtime ownership; request and result planning exist, but scheduler and live death side effects are still incomplete |
| `mail` | 55% | 45% | partial runtime ownership; system-mail persistence is clear, but full formatter and general mail behavior remain open |
| `trade` | 65% | 35% | hidden/renamed ownership; pricing, list assembly, and packet planning are clear, while live purchase mutation still needs proof |
| `toypet` | 55% | 45% | partial runtime ownership; feed, spawn, and persistence exist, but adoption and full mood lifecycle remain open |
| `housing` | 70% | 30% | hidden/renamed ownership; runtime registration, auction, maintenance, and visibility are clear, while broader visit and instance side effects remain open |
| `summons` | 40% | 60% | partial runtime ownership; summon packet and execution surfaces exist, but owner lifecycle and controller parity remain open |
| `vortex` | 30% | 70% | partial runtime ownership; location lookup is represented, but invasion and activation lifecycle coverage is still weak |
| `warehouse` | 45% | 55% | hidden/renamed ownership; warehouse expansion is present, but broader shared-storage behavior still needs tracing |
| `respawn` | 45% | 55% | hidden/renamed ownership; revive and NPC spawn flows exist, but timers and cleanup breadth remain unclear |
| `skill learn` | 70% | 30% | hidden/renamed ownership; skill, motion, and emotion learn flows are clearly split but represented |
| `weather` | 60% | 40% | hidden/renamed ownership; weather transition and broadcast planning exist, but full runtime breadth still needs verification |

## Mail

### Traced C# Owners

- `MailRepository.cs`
- `MailSendCostPlanService.cs`
- `SystemMailRewardPlanService.cs`
- `SystemMailRewardPersistencePlanService.cs`
- `HouseAuctionRepository.cs`
- online mailbox update handling in `GameClientSocketServer.cs`

### What Is Clearly Accounted For

- system mail letter persistence
- attached item persistence for system mail
- mailbox counter updates
- auction refund mail insertion
- mail send cost planning

### What Still Needs Tracing

- full `MailFormatter` parity
- siege-result and abyss-siege mail coverage
- general non-system `MailService` ownership beyond the current repository and reward flows

## Reward

### Traced C# Owners

- `CustomLevelRewardPlanService.cs`
- `CustomLevelRewardExecutionService.cs`
- `CustomLevelRewardRepository.cs`
- `StarterKitLevelChangePlanService.cs`
- `SystemMailRewardPlanService.cs`

### What Is Clearly Accounted For

- bonus-pack planning and execution
- faction-pack planning and execution
- reward receipt persistence
- system-mail reward generation for these custom reward flows
- starter-kit feature gating and level-change planning surface

### What Still Needs Tracing

- explicit `AdventService` ownership
- explicit `VeteranRewardService` ownership
- explicit `WebRewardService` runtime ownership
- the current C# codebase contains a comment that web rewards are still deferred

## Trade

### Traced C# Owners

- `PricesService.cs`
- `TradeApFormulaService.cs`
- `TradeListTable.cs`
- `NpcDialogServiceSelectPlanService.cs`
- `NpcDialogLimitedItemFactAdapterService.cs`
- `SmTradeListPacketPlanService.cs`
- `SmTradeInListPacketPlanService.cs`

### What Is Clearly Accounted For

- global and service price calculations
- trade-list and trade-in static-data indexing
- NPC trade-list packet planning
- limited-item discovery and packet shaping
- Java `LimitedItemTradeService.start` static scanning shape

### What Still Needs Tracing

- live purchase mutation and buy-count persistence
- complete runtime ownership for limited-item purchase updates
- broader top-level `TradeService` parity beyond current list assembly and pricing

## Toy Pet

### Traced C# Owners

- `Services/ToyPet/` helpers including `PetFeedCalculator.cs`, `PetFeedProgress.cs`, `PetFeedServiceOperationPlan.cs`, and `PetFeedXmlProjection.cs`
- `PlayerPetsRepositoryPlan.cs`
- `PlayerPetRowProjection.cs`
- toy-pet spawn handling in `GameServerConnection.cs`
- pet spawn packet and known-list ownership in `SmPet.cs` and `PlayerKnownListPet*`

### What Is Clearly Accounted For

- pet feed calculation and feed progress modeling
- feed operation planning
- mood persistence planning
- toy-pet spawn item-action handling
- pet spawn packet construction and visibility packet support

### What Still Needs Tracing

- explicit adoption-service ownership equivalent to Java `PetAdoptionService`
- full live pet mood/runtime lifecycle ownership
- confirmation that the whole Java `PetService` surface is represented, not just feed/spawn/packet slices

## Duel

### Traced C# Owners

- `PlayerDuelRequestService.cs`
- duel response handling in `PlayerEnterWorldService.cs`
- duel world flags in `WorldMapRuntimeState.cs` and `WorldMapSummary.cs`
- duel-aware branches in `PlayerDeathWorkflowPlanService.cs`

### What Is Clearly Accounted For

- duel request, accept, reject, and withdraw handling
- duel registration and active-duel tracking
- duel start packets
- duel loss and draw result packet plans

### What Still Needs Tracing

- the duel draw scheduler is explicitly not ported in the current service
- death workflow reports that duel loss and HP/MP restoration are planned but not executed in the live workflow
- broader duel side effects still need a deeper parity pass

## Housing

### Traced C# Owners

- `HousingWorldService.cs`
- `HousingVisibilityService.cs`
- `HouseDoorStateService.cs`
- `HouseAuctionTimingService.cs`
- `HouseMaintenanceTimingService.cs`
- `HouseAuctionRepository.cs`
- housing-related request handling in `GameServerConnection.cs`

### What Is Clearly Accounted For

- housing world registration and door-state application
- housing visibility tracking
- house auction loading, registration, and bid placement
- rent and settings repository hooks
- auction-result refund mail insertion

### What Still Needs Tracing

- broader visit and house-instance side effects
- full parity of all housing-facing packets and state transitions
- end-to-end coverage of Java `HousingService` and `HousingBidService` beyond the current auction/timing surfaces

## Resulting Interpretation

These areas should no longer be treated as pure unknowns:

- `mail`
- `trade`
- `toypet`
- `duel`
- `housing`

They are still parity-risk areas, but the risk is now mostly ownership completeness and live side effects, not total absence.

`reward` remains mixed: bonus/faction/starter-kit style reward work is clearly represented, while advent, veteran, and web reward ownership is still not demonstrated.

## Next Deep-Dive Order

1. `reward`
2. `duel`
3. `mail`
4. `toypet`
5. `trade`
6. `housing`
