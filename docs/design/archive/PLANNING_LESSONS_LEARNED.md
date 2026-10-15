# Planning Complex Projects Without Overengineering the Process

## Core finding

The planning approach optimized for an appearance of completeness and control instead of demonstrated implementation success.

A complex, ambiguous project was converted into a large, precise-looking plan before enough of its architecture, gameplay, testing methods, and task boundaries had been validated together. Additional rules and verification machinery then attempted to compensate for uncertainty that the plan had not actually resolved.

The result was a process that consumed substantial effort while repeatedly requiring intervention. Stronger models became routine repair workers rather than occasional reviewers, undermining the intended execution model.

The central lesson is:

> A plan is successful when it enables reliable, economical progress—not when it is comprehensive, internally consistent, or repeatedly reviewed.

## 1. Distinguish clarity from apparent precision

### Systematic error

Detailed descriptions, numerical limits, dependency lists, and acceptance criteria made unresolved design decisions look settled.

Precision is valuable only when it describes a coherent behavior. An exact number does not resolve uncertainty about whether a mechanic is enjoyable, whether two systems compose correctly, or whether the proposed implementation boundary is sensible.

This creates **false certainty**: implementation discovers the real design problems, but the process treats them as failures to follow an already-complete plan.

### Better approach

Separate three categories:

- **Commitments:** intended player experience, essential capabilities, safety constraints, and explicit exclusions.
- **Working designs:** the current proposed mechanics and interfaces.
- **Tuning hypotheses:** numerical values and tradeoffs that require testing.

These categories do not require extensive labels throughout the documentation. They require a clear policy about what may change and how.

Keep the overall direction stable while allowing implementation evidence to improve the working design.

### Checklist

- [ ] Does each precise requirement resolve a real decision?
- [ ] Are untested numbers presented as starting values rather than established balance?
- [ ] Can an implementer distinguish a product commitment from an engineering choice?
- [ ] Does the plan identify the uncertainties that could invalidate its structure?
- [ ] Have important interactions been tested rather than merely described?

## 2. Use prototypes to improve the design, not decorate the documentation

### Systematic error

Experiment results were sometimes recorded as accomplishments rather than translated into gameplay and engineering decisions.

A list of successful experiments does not explain how the eventual product should work. It can make a document longer without reducing implementation uncertainty.

A prototype also proves less than it may appear to prove. A feature working in isolation does not establish its progression, resource economy, persistence, multiplayer behavior, or interaction with other systems.

### Better approach

For each experiment, extract:

1. What capability was demonstrated?
2. Under what conditions did it work?
3. What limitations or failure modes appeared?
4. What design decision follows?
5. What remains unproven?

Put the resulting decision into the document that owns the behavior. Keep experimental records separate from the product specification.

For example, an experiment demonstrating connected storage should inform capacity rules, connection behavior, failure handling, construction, and player feedback—not become a paragraph congratulating the experiment.

### Checklist

- [ ] Has each relevant experiment changed a concrete design decision?
- [ ] Are demonstrated capabilities separated from assumptions about scale or integration?
- [ ] Does the gameplay description explain what the player actually does?
- [ ] Have prototype limitations become explicit implementation risks?
- [ ] Can the product specification stand alone without an experiment chronology?

## 3. Plan broadly, but specify implementation incrementally

### Systematic error

The project was broken into a large execution queue too early.

A long-range roadmap and an executable task queue are different artifacts. The roadmap describes direction and dependencies. An implementation task requires enough certainty to produce a bounded, verifiable result.

Writing distant work at implementation-level detail creates maintenance obligations before the underlying systems exist. Later discoveries invalidate assumptions across many tasks.

### Better approach

Use rolling planning:

- Maintain a high-level end-to-end product map.
- Define the next playable milestone concretely.
- Prepare a small batch of implementation tasks.
- Execute that batch.
- Adjust architecture, estimates, and subsequent tasks using the results.

Long-term features can have prerequisites and intended outcomes without pretending that their exact implementation is already known.

**Planning depth should decrease with distance from executable evidence.**

### Checklist

- [ ] Is the next milestone described as a usable experience?
- [ ] Are only near-term tasks specified in implementation detail?
- [ ] Do later milestones preserve options where important uncertainty remains?
- [ ] Can discoveries update future work without rewriting the whole plan?
- [ ] Is there a clear distinction between roadmap completeness and execution readiness?

## 4. Define task size by independent outcomes, not document length

### Systematic error

Some tasks bundled several independent features behind a short title or shared module name.

A task can look small on paper while requiring new storage behavior, construction rules, recipes, persistence, interface changes, rendering, and integration tests.

This multiplies the number of ways the task can fail. It also makes review less complete: attention shifts between unrelated concerns, and later reviews discover defects missed earlier.

### Better approach

A task should have one primary observable outcome.

Supporting changes may cross several modules when necessary. Conversely, several behaviors inside one module may still require separate tasks.

A useful splitting question is:

> Can one part be correct, useful, and independently accepted while another part remains unfinished?

If yes, they are candidates for separate tasks.

Do not split so aggressively that every task delivers an unusable fragment. Prefer small vertical slices: a real behavior through its necessary layers.

### Example

Too broad:

> Implement construction parts, storage, lighting, recipes, structure validation, and visual qualification.

More workable:

- Deliver one construction part with its obtainability, placement, appearance, and basic tests.
- Deliver structure validation against an existing small assembly.
- Deliver a storage component with insertion, withdrawal, persistence, and recovery.
- Deliver the lighting mutation and verify its appearance and actual lighting behavior.

Each task closes a distinct loop.

### Task-sizing checklist

- [ ] One primary user-visible or infrastructure outcome.
- [ ] No unrelated feature added merely because it shares a module.
- [ ] Inputs and prerequisites already exist or are explicitly supplied.
- [ ] Success can be demonstrated without completing another unfinished task.
- [ ] Failure cases are small enough to examine together.
- [ ] The expected diff can be meaningfully reviewed in one pass.
- [ ] New infrastructure is justified by this task’s actual needs.

File count and diff size are warning signals, not arbitrary acceptance limits.

## 5. Design boundaries that support change rather than prohibit it

### Systematic error

Architectural ownership was turned into editing permission.

File allowlists and frozen configuration prevented workers from repairing legitimate prerequisite defects. A task could discover the correct fix and still be forced to stop because the fix belonged elsewhere.

That defeats autonomous development.

### Better approach

Separate:

- **Product scope:** what outcome is authorized.
- **Behavior ownership:** where each rule belongs.
- **Change surface:** which files must change to implement or repair that outcome.

Product scope should remain controlled. Behavior ownership should remain clear. The change surface must be allowed to follow the engineering evidence.

A worker should be able to repair a prerequisite in its owning module, update affected callers, add regressions, and continue the original task.

Review should reject unrelated work or broken abstractions—not a necessary change to an unpredicted file.

### Checklist

- [ ] Does each behavior have one clear owner?
- [ ] Can necessary repairs cross module boundaries?
- [ ] Are interface changes evaluated for their impact rather than automatically forbidden?
- [ ] Can tests, build configuration, and harnesses be corrected within the task?
- [ ] Are product changes distinguished from routine engineering decisions?
- [ ] Is human approval reserved for meaningful changes in authority or intended experience?

## 6. Keep orchestration smaller than the work it coordinates

### Systematic error

Project-specific infrastructure duplicated responsibilities already provided by the task runner.

Additional execution wrappers, process supervision, delivery transactions, fingerprints, evidence registries, and recovery state introduced new failure modes unrelated to the product.

Every extra mechanism needed its own configuration, testing, debugging, and compatibility maintenance. The development process became a second software project.

### Better approach

Assign each orchestration responsibility once:

- The runner owns selection, retries, execution limits, progress, and reporting.
- The build and test tools determine executable correctness.
- Independent review evaluates concerns that cannot be reliably automated.
- Git records changes and supports recovery.

Add a project hook only when it supplies a necessary capability that the existing tools lack.

Before writing process code, state the missing capability and explain why configuration or an existing tool cannot provide it.

### Checklist before adding infrastructure

- [ ] Is there an observed problem rather than a hypothetical concern?
- [ ] Does an existing tool already own this responsibility?
- [ ] Can configuration solve it?
- [ ] Is the proposed mechanism smaller than the problem it addresses?
- [ ] Does it avoid duplicating state?
- [ ] Can it fail without trapping otherwise valid work?
- [ ] Is its maintenance cost justified by demonstrated benefit?

## 7. Validate behavior, not compliance with a ceremony

### Systematic error

Some verification requirements emphasized the production and formatting of evidence rather than the behavior being verified.

Evidence bookkeeping became capable of rejecting useful work for procedural reasons. Passing the bookkeeping could also create misleading confidence without establishing gameplay quality.

### Better approach

Use the simplest evidence that reliably supports a claim:

| Claim | Suitable evidence |
|---|---|
| A rule produces the right result | Focused behavioral test |
| A saved state survives reload | Real serialization and reload test |
| A player interaction works | Platform or in-game interaction test |
| A model looks correct | Inspected captures from relevant states and angles |
| A system scales | Measurements under a representative workload |
| A task is delivered | Normal Git history and remote confirmation |

A wrapper is not inherently stronger than the tool it wraps. A receipt is not proof of correctness. A screenshot is not visual validation unless someone inspects it.

### Checklist

- [ ] Does each check detect a meaningful failure?
- [ ] Does the failing test fail for the intended reason?
- [ ] Does the test exercise the actual interaction path?
- [ ] Are evidence requirements proportionate to the change?
- [ ] Can ordinary tool output be used directly?
- [ ] Are flaky checks investigated rather than repeatedly rerun until green?
- [ ] Can a defective check be repaired without waiving its intended requirement?

## 8. Make review comprehensive, bounded, and consistent

### Systematic error

Review findings arrived in successive batches, while workers repaired the named examples narrowly.

Some findings represented genuine defects. Others exposed ambiguity in the scope of broad requirements. Without a clear distinction, review can become an expanding set of obligations rather than verification of a defined outcome.

### Better approach

Review must distinguish:

- **Acceptance defects:** missing required behavior or demonstrated incorrect behavior.
- **Risk findings:** credible concerns requiring a targeted reproduction or measurement.
- **Improvements:** useful changes that are not necessary for this task’s acceptance.

A concern should not become a blocker solely because an undesirable outcome can be imagined.

For example, restrictions on autonomous scanning or world mutation should not automatically require replacing ordinary platform behavior. The review must establish the relevant execution path, additional risk introduced by the mod, and applicable requirement.

Workers should repair the **failure class**, not only the reported instance. A defect in one placement path should trigger inspection of related placement paths.

### Review checklist

- [ ] Is each blocker tied to an explicit requirement or demonstrated defect?
- [ ] Is the affected scenario supported and realistically reachable?
- [ ] Is severity proportional to its consequences?
- [ ] Have related paths been examined together?
- [ ] Are findings actionable and reported as a consolidated set?
- [ ] Does a later review explain whether a finding is newly introduced or previously missed?
- [ ] Are optional improvements kept out of blocking acceptance?

A clean review is evidence of inspection, not a guarantee that no defects remain.

## 9. Match retry policy to iterative engineering

### Systematic error

A small fixed repair budget converted productive progress into repeated terminal failures.

Review-driven development naturally requires iteration. If the workflow expects a reviewer to find meaningful defects, it must also provide enough opportunity to correct them.

However, unlimited retries would hide stalled reasoning and uncontrolled cost.

### Better approach

Use a configurable, finite repair budget that reflects observed task behavior.

Distinguish:

- A repair that resolves defects and passes new tests.
- An unchanged repetition of the same failed approach.
- An infrastructure interruption.
- A genuine unresolved product decision.

These should not all consume effort or terminate in the same way.

Increase repair allowance when work is converging. Reassess the task when the same problem repeats without progress. Do not use retries to compensate indefinitely for an oversized task.

### Checklist

- [ ] Does the runner support the configured budget?
- [ ] Are productive repairs distinguishable from repeated unsuccessful attempts?
- [ ] Are usage limits and service failures reported accurately?
- [ ] Is successful work preserved across interruption?
- [ ] Does repair receive the actual findings and relevant prior evidence?
- [ ] Is there a clear stopping condition for lack of progress?

## 10. Measure the economics of the whole task

### Systematic error

Model selection was evaluated at the individual-call level rather than the complete implementation cycle.

A less expensive initial worker does not save money if most tasks require extensive reimplementation by a more expensive model, multiple full reviews, and repeated validation.

Similarly, planning expenditure is not justified merely because it produces more detailed documents.

### Better approach

Measure total accepted-task cost:

> Planning + implementation + repair + review + validation + human intervention.

Track:

- First-attempt acceptance rate.
- Repair attempts per accepted task.
- Repeated failure classes.
- Total model cost and elapsed time.
- Human interventions.
- Defects found after acceptance.
- Useful playable progress.

Keep model roles explicit and verify that the configured models match those roles.

If the repair model routinely implements substantial portions of the feature, reassess task design and model assignment. Do not describe that as an occasional escalation.

### Checklist

- [ ] Are configured models the intended models?
- [ ] Are costs measured through acceptance rather than initial implementation?
- [ ] Is expensive repair exceptional or routine?
- [ ] Are planning changes improving measured execution outcomes?
- [ ] Is review finding fewer repeated defects over time?
- [ ] Does automation reduce human coordination rather than relocate it?

## 11. Treat documentation as an operational tool

### Systematic error

Long documents, overlapping descriptions, and generic language increased reading cost without reliably improving decisions.

Documentation also drifted toward process commentary and historical explanation instead of current gameplay facts.

### Better approach

Document each rule in one owning place. Other documents should reference or summarize it only where necessary for a different purpose.

The minimum useful documentation should answer:

- What experience are we building?
- How do the major systems interact?
- What owns each behavior?
- What must the next implementation deliver?
- How will correctness be established?
- Which product decisions remain unresolved?

Do not turn unresolved engineering work into questions for the user. Research, initial numerical proposals, API investigation, and routine implementation choices belong to development.

### Checklist

- [ ] Does every section support a current decision or implementation?
- [ ] Is each rule owned in one place?
- [ ] Are terminology and states defined precisely?
- [ ] Are historical notes absent from current specifications?
- [ ] Are open questions genuinely product-level questions?
- [ ] Can a reader identify the relevant requirements without reading everything?

## 12. Use execution evidence as the readiness gate

### Systematic error

Repeated planning reviews were treated as sufficient evidence that the full queue was ready.

They were not.

Reviews can identify omissions and contradictions. They cannot establish that a particular model, task format, test environment, and acceptance process will work economically together.

### Better approach

Qualify the development process with a small, representative implementation batch.

Include more than easy setup work. Exercise:

- A deterministic rule.
- A persisted stateful behavior.
- A player-facing interaction or visual.
- An integration boundary.
- A repair following independent rejection.

Define acceptable cost and intervention expectations before expanding execution. Choose initial targets appropriate to the project, then revise them using observed results.

### Readiness checklist

- [ ] Representative tasks have completed through the real workflow.
- [ ] Task boundaries survived implementation without major redesign.
- [ ] Review findings were resolved without routine human intervention.
- [ ] The selected implementation model handled the intended role.
- [ ] Failure and interruption recovery worked.
- [ ] Total costs fit the project’s expectations.
- [ ] Recurring defects led to improvements in subsequent task preparation.

Until these conditions are demonstrated, describe the process as a pilot—not a production-ready execution plan.

## Recommended process for the next large project

1. **Define the product boundaries.** Establish the intended experience, non-goals, essential constraints, and major unresolved choices.
2. **Prototype the highest-risk interactions.** Test the assumptions most likely to invalidate the design.
3. **Extract decisions into concise specifications.** Record behavior and limitations, not a catalogue of experiments.
4. **Define a small playable milestone.** Include enough integration to expose architectural weaknesses.
5. **Prepare a small batch of independently acceptable tasks.** Make each outcome concrete and its validation practical.
6. **Execute using the existing runner and normal development tools.** Avoid custom orchestration unless a specific missing capability is demonstrated.
7. **Review and repair within the same workflow.** Preserve real requirements while allowing necessary engineering changes.
8. **Measure results before expanding the queue.** Adjust task size, model roles, review criteria, and retry budgets from evidence.
9. **Repeat with progressively larger product capability—not progressively larger process machinery.**

## Final principle

**Uncertainty cannot be eliminated by writing more instructions.**

It must be reduced through targeted experiments, small integrated deliveries, meaningful tests, and feedback that changes the next decision. The process should make those activities easier—not become another system that must be debugged before the product can advance.
