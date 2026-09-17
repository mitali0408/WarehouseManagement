# Case Study Scenarios to discuss

## Scenario 1: Cost Allocation and Tracking
**Situation**: The company needs to track and allocate costs accurately across different Warehouses and Stores. The costs include labor, inventory, transportation, and overhead expenses.

**Task**: Discuss the challenges in accurately tracking and allocating costs in a fulfillment environment. Think about what are important considerations for this, what are previous experiences that you have you could related to this problem and elaborate some questions and considerations

**Questions you may have and considerations:**

The core challenge is that costs don't naturally map 1:1 to the entities we're tracking. A truck delivering to three Stores splits transportation cost how — by volume, by revenue, by distance leg? Labor at a Warehouse serving multiple Business Units needs an allocation key too. Get the allocation model wrong and every downstream report (P&L per store, warehouse profitability) is quietly wrong.

Considerations:
- **Direct vs. allocated costs**: some costs (a Store's own staff) attach cleanly; others (regional overhead, shared transport) need an allocation formula that's transparent and auditable — finance will ask "how was this number derived" and needs a real answer, not a black box.
- **Granularity and time**: costs need to be tracked at a resolution that supports the reporting need (daily? per shipment?) without generating unmanageable data volume. This ties directly into the Warehouse/Store data model — every cost record needs a clean, stable foreign key to the entity it's allocated to.
- **The "replace" operation matters here**: if a Warehouse is archived and a new one created under the same Business Unit Code, cost records need to stay attributable to the correct physical entity/time period, even though the code is shared. Cost records should be tied to the Warehouse's surrogate ID (the specific instance), not just the Business Unit Code, so history doesn't get silently merged.

Questions I'd ask:
- Is there an existing chart of accounts or cost-center structure this needs to align with?
- What's the required reporting granularity — daily, weekly, per-shipment?
- Are allocation rules fixed/rule-based, or do they need to be configurable by finance without a code change?

From experience: allocation logic hardcoded into services early on tends to become a maintenance nightmare the moment finance wants a new allocation rule — better to model allocation rules as data/config from day one.

## Scenario 2: Cost Optimization Strategies
**Situation**: The company wants to identify and implement cost optimization strategies for its fulfillment operations. The goal is to reduce overall costs without compromising service quality.

**Task**: Discuss potential cost optimization strategies for fulfillment operations and expected outcomes from that. How would you identify, prioritize and implement these strategies?

**Questions you may have and considerations:**

This is as much a data/observability problem as an operations one — you can't optimize what you can't see broken down. Three steps:

**Identify**: Instrument cost data well enough to spot anomalies and trends — cost-per-unit-shipped by Warehouse, cost variance vs. budget, underutilized Warehouse capacity. Look for outliers, not just averages.

**Prioritize**: Use a simple impact-vs-effort lens — e.g., consolidating routes or Store-to-Warehouse assignments (data/analytics-driven, no process change) is lower-risk than renegotiating labor contracts (high effort, org-wide). Prioritize whatever gives the fastest, safest signal that the approach works.

**Implement**: Roll changes out incrementally and measurably — pilot on a subset of Warehouses/Stores, compare against a control group, then scale. Good historical cost data (Scenario 1) becomes essential here — without a clean baseline you can't prove the optimization worked.

Questions I'd ask:
- Is there tolerance for a pilot/phased rollout, or does this need to apply company-wide at once?
- What's the current baseline — do we already have reliable historical cost data to measure against, or does that need to be built first?

## Scenario 3: Integration with Financial Systems
**Situation**: The Cost Control Tool needs to integrate with existing financial systems to ensure accurate and timely cost data. The integration should support real-time data synchronization and reporting.

**Task**: Discuss the importance of integrating the Cost Control Tool with financial systems. What benefits the company would have from that and how would you ensure seamless integration and data synchronization?

**Questions you may have and considerations:**

The main value here is trust and timeliness — finance needs to close books on real numbers, not estimates reconciled weeks later. But integration is also where projects quietly go wrong, so this needs care rather than just "build an API."

Considerations:
- **Sync pattern**: true real-time (event-driven — publish a cost event whenever a Warehouse/Store cost record changes) vs. periodic batch reconciliation. Real-time is more valuable but adds complexity (idempotency, ordering, failure handling); only worth pushing for if the business genuinely needs live numbers rather than "as of yesterday." This system's own Task 2 solution is a direct, concrete example of this tradeoff: propagating a Store change to a downstream system only after the local transaction commits, with explicit failure handling (log-and-alert, not silent swallow) for when the downstream call itself fails — the same pattern would apply to a real-time financial-system integration.
- **Source of truth**: this system should own operational cost data; the financial system owns the ledger. Integration needs to be one-directional in terms of authority even if data flows both ways, or reconciliation conflicts arise about which system is "right."
- **Data contract stability**: financial systems are typically far less tolerant of schema drift than internal systems. A versioned, explicit contract (event schema or API contract) is safer than exposing internal domain models directly.
- **Auditability**: every value that flows into finance needs to be traceable back to the source record — critical for audits and disputes.

Questions I'd ask:
- What's the actual system (SAP, NetSuite, custom ledger) and what integration patterns does it already support?
- Does finance need real-time data, or is daily/nightly reconciliation acceptable?
- What's the reconciliation process when numbers don't match?

## Scenario 4: Budgeting and Forecasting
**Situation**: The company needs to develop budgeting and forecasting capabilities for its fulfillment operations. The goal is to predict future costs and allocate resources effectively.

**Task**: Discuss the importance of budgeting and forecasting in fulfillment operations and what would you take into account designing a system to support accurate budgeting and forecasting?

**Questions you may have and considerations:**

This is furthest from "pure engineering" and closest to "make sure the data model can even support the ask." Forecasting is only as good as the historical data feeding it, so the most important engineering contribution is making sure cost/operational history is clean, consistent, and queryable over time — which loops directly back to Scenario 1 and how "replace" preserves history.

Considerations:
- Budgets are typically set per period (quarter/year) per cost center (Warehouse/Store) — the data model needs a first-class notion of "budget vs. actual" comparison, not just raw actuals.
- Forecasting needs enough historical continuity to be meaningful — which is exactly why the Warehouse replace operation must preserve, not sever, cost history under the same Business Unit Code.
- Engineering scope should stay at "provide clean, well-structured historical and real-time data plus the APIs to compare against a budget" — the actual forecasting model (statistical, ML, or simpler trend-based) is a separate concern, not something to assume ownership of without more info.

Questions I'd ask:
- Who owns forecasting methodology — is this system expected to generate forecasts, or just supply clean data to a separate finance/BI tool?
- What's the budget cycle (annual, quarterly, rolling)?

## Scenario 5: Cost Control in Warehouse Replacement
**Situation**: The company is planning to replace an existing Warehouse with a new one. The new Warehouse will reuse the Business Unit Code of the old Warehouse. The old Warehouse will be archived, but its cost history must be preserved.

**Task**: Discuss the cost control aspects of replacing a Warehouse. Why is it important to preserve cost history and how this relates to keeping the new Warehouse operation within budget?

**Questions you may have and considerations:**

The business reason for reusing the Business Unit Code on replace is continuity — the new Warehouse is treated as the *same business unit* going forward, even though it's a new physical/technical entity. For cost control, that continuity is exactly what preserves the ability to track:

- **Budget continuity**: if the Business Unit Code carries the budget history, the new Warehouse should inherit (or be evaluated against) the trailing cost trend of its predecessor — otherwise it starts "budgeting blind."
- **Trend analysis across the replace boundary**: if cost history got orphaned to the archived Warehouse and disconnected from the new one, the ability to see "did this replacement actually reduce costs" is lost — presumably the whole point of many warehouse replacements (moving to a more efficient facility, better location, etc.).
- **Two-tier identity**: the Business Unit Code should be the durable "cost center" identity for reporting/budgeting purposes, while each Warehouse instance (old and new) is its own entity with its own operational data and a lifecycle (active → archived). Cost records stay attributed to the specific Warehouse instance (for accuracy — different fixed costs, different lease, etc.) but are queryable/rolled-up by Business Unit Code (for continuity). This is precisely the two-tier identity model the implementation uses: `businessUnitCode` as the durable identity replace preserves across history, and a separate database row/id as the specific instance — the same shape a cost ledger for this warehouse would need.

Questions I'd ask:
- When a Warehouse is replaced, should the new Warehouse start with a fresh budget baseline, or inherit/adjust from the old one's trailing average?
- Is there a required "before/after" cost comparison report expected as a deliverable of the replace operation itself, or is that a downstream BI concern?

---

**On what key information to gather before defining the boundaries of the work**: across all five scenarios, the recurring theme is that the most useful information to gather up front is (1) who the primary consumer of this cost data is — ops, finance, or both — since that changes the granularity and latency requirements, (2) what already exists (is there a legacy cost tool being replaced, or is this greenfield), and (3) what the actual pain point is today — is data missing, inaccurate, too slow, or just hard to report on? That framing keeps the scope anchored to "how do we solve the real problem" rather than "how do we build a generically complete cost system," which is usually the scope trap in exercises like this and in real projects alike.

## Instructions for Candidates
Before starting the case study, read the [BRIEFING.md](BRIEFING.md) to quickly understand the domain, entities, business rules, and other relevant details.

**Analyze the Scenarios**: Carefully analyze each scenario and consider the tasks provided. To make informed decisions about the project's scope and ensure valuable outcomes, what key information would you seek to gather before defining the boundaries of the work? Your goal is to bridge technical aspects with business value, bringing a high level discussion; no need to deep dive.