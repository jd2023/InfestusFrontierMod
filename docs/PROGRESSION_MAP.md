# Progression map — concept draft 0.1

Status: **discussion document, not an approved implementation specification**.
Companion to [VISION.md](../VISION.md). All stage names, organisms, substances,
structures, trials and numbers below are proposals unless identified as an owner
requirement. Names are working labels, not registry IDs or a finalized ontology.
This describes the full ambition, not the scope of the first release.

**Subsequent owner feedback:** this map did not convey enough of how play feels.
Read the newer [design notebook](ideas_and_progression_feedback.md) and
[assessment](IDEAS_REVIEW.md) before developing a stage. They expand the owner's
specific ideas and propose revisions to resources and experiences. This older
rank sequence remains a reference, not a requirement to preserve every named reagent.

**2026-09-08 experiment update:** notebook version 0.3 records Manual Leaching as
an explicit owner-selected feature, multiple distinct downward mining approaches,
ground-level construction/lit access, and suit fuel/HUD evidence. Read §§3 and 18
before assigning mining or armor unlocks. Hazardous biomass overflow (§§8, 33) and
leaching-assisted burrowing (§26) remain proposals; no rank below implicitly
approves them or a random reservoir-rupture rule. Exact levels remain open.

For a quick read, start with the [rank map](#3-the-map-at-a-glance),
[alternative campaign routes](#52-four-viable-campaign-routes) and
[armor tradeoffs](#57-armor-exceptional-within-a-role-limited-across-roles).
The full stage walkthrough and design review explain the reasoning behind them.

## 1. What progression should feel like

At first, the player learns to grow a useful patch of living ground. Much later,
they operate a civilization whose specialized organs span several worlds. The
transformation is not from weak armor to invulnerability: it is from solving one
local problem to designing reliable systems with competing needs.

**The reward for understanding a system is the ability to automate it. The reward
for automating it is a new design space, not a larger bill for the same recipe.**

Established direction:

- Long, demanding play built around experiments, planning and optimization, not
  inflated ingredient counts, AFK training or repetitive boss kills.
- Meaningful player levels unlock more capable mutations and multiblock families.
- Several viable paths and DNA combinations; players can compare efficient designs.
- Powerful, specialized equipment. A generalist must sacrifice peak capability.
- Increasing reliance on automation and deliberate base layout.
- Continuing operations in the Overworld, Nether and End, with dimension-native
  substrates and structures that cannot simply be moved home.
- A special dimension and new boss as a **very-long-term design horizon**.
- A player-controlled colony: no hostile takeover or indefinite autonomous spread.
- A varied living ecosystem, not a flat, cleared desert. Transformed vegetation
  should eventually have useful roles; the exact tree mechanics remain undecided.
- Bio-armor enchantment support is reopened, not removed by this draft. Potions
  are a promising integration opportunity. Vanilla systems outside our equipment
  remain intact.

### Inspiration, interpreted rather than copied

GTNH presents named ages with changing infrastructure challenges, persistent
production needs and alternative routes within milestones. The lesson for us is
legible eras and problems that reward rebuilding intelligently—not importing its
recipe tree, quest dependency or advertised completion time. [GTNH official overview](https://www.gtnewhorizons.com/)

Blood Magic's growing altar ties physical construction to capability; its runes
offer functional upgrades, and later rituals introduce configurable automation.
The useful distinction is **structure tier versus structure specialization**.
Our biology should use its own construction language rather than reproduce altar
rings or sacrifice loops. [Blood Altars](https://github.com/WayofTime/BloodMagic/wiki/Blood-altars),
[Rituals](https://github.com/WayofTime/BloodMagic/wiki/Rituals)

Mekanism's processing chain adds supporting processes and reuses earlier machines
as yield increases. Its reactor tutorial also exposes the importance of supply,
waste and partial-loading failure. We can borrow interdependent production and
visible hazards without inheriting destructive unload behavior. [Ore Processing](https://wiki.aidancbrady.com/wiki/Ore_Processing),
[Fission Reactor Tutorial](https://wiki.aidancbrady.com/wiki/Fission_Reactor_Tutorial)

The MekaSuit documentation describes energy-backed protection and configurable
modules. Here the proposed limit is different: ample fuel must **not** remove
anatomical incompatibilities or grant universal protection. [MekaSuit](https://wiki.aidancbrady.com/wiki/MekaSuit)

These references describe their own versions; they are not compatibility research
or dependencies for this mod. Everything below is an original design proposal.

## 2. Three kinds of advancement, not three XP bars

### Player level: Symbiotic Rank

A proposed persistent rank records what complexity the player can safely bind and
direct. Rank is earned through discoveries and short, observable demonstrations,
not a repeatable XP currency. It unlocks a *class of possibilities*, not every
recipe in that class. The guide shows the next rank's requirements and alternatives.

Examples: establish a balanced circulation loop; mature a native Nether culture;
complete a controlled hazardous process; stabilize an End organ. Holding someone
else's completed machine is not equivalent to understanding or commissioning it.
However, cooperation is legitimate: a team should not rebuild the same base once
per member. Provisional policy: authorized participants can share commissioning
credit, with a small personal binding step. Research ownership is still open.

Ranks do not disappear on death or a broken machine. Damaged infrastructure can
stop production, but never delete earned knowledge. Existing safe low-tier tools
remain usable during recovery. Rank affects biological binding and advanced
commissioning, not the right to place ordinary building blocks or visit dimensions.

### Research: a branching genetic atlas

The DNA bank accumulates specimens until a species' genome is complete. A partial
capture can teach a basic trait; completion makes reliable directed mutation
possible. Exact sample counts are deliberately absent here.

The atlas branches by biological function: structure, locomotion, exchange,
perception and regulation. Nether and End research extend those branches rather
than reset them. Unlocking a stronger chamber does not automatically complete DNA.
No rank requires collecting every species or owning content from another mod.

### Equipment and organs: expression, not an endless power ladder

An item has a bounded anatomical capacity and a chosen expression of known traits.
Usage refines an installed behavior; captured DNA supplies new behaviors and
alternative architectures. A well-used early specialist remains useful later.
Higher ranks allow more demanding combinations, not unlimited simultaneous traits.

Thus: **rank grants permission; research grants options; a constructed organism
and its supporting base determine what actually works.**

## 3. The map at a glance

These levels are milestone labels, not yet balance numbers or a quest chain.

| Rank | Era | New design problem | Signature unlock | Permanent base consequence |
|---|---|---|---|---|
| R0 | Contact | Learn the biological language | Culture, sampler, controlled substrate | A useful living garden |
| R1 | Circulation | Balance supply and demand | Digestion, reservoir, tissue transport | A serviceable resource backbone |
| R2 | Differentiation | Choose capabilities under constraints | DNA bank, mutation chamber, first specialist suits | Research and equipment workshops |
| R3 | Thermogenesis | Establish production in a hostile environment | Nether-native Emberbed and thermal organs | A working Nether outpost |
| R4 | Containment | Operate high-grade substances safely | Catalytic organs, isolation and recovery | Separated production districts |
| R5 | Anchoring | Build where local conditions invalidate old assumptions | End-native Anchorbed and spatial organs | A working End outpost |
| R6 | Confluence | Coordinate three persistent economies | Cross-dimensional circulation and graft transit | A distributed base |
| R7 | Synthesis | Optimize competing outputs and withstand disruption | Configurable synthesis arrays and advanced DNA combinations | A resilient biological civilization |
| R8 | Foreign Symbiosis | Establish a foothold in an alien ecology | Fourth-dimension Reciprocity Bed | A fourth operational settlement |
| R9 | Coexistence | Solve a combined ecological and combat challenge | New boss discovery and alternative organ architectures | Open-ended specialization, not godhood |

R0–R7 form a proposed complete three-dimension campaign. R8–R9 are an expansion
horizon; R7 must be satisfying without them.

**Read each row as an era entered at that rank.** Its listed tools are available
for research/construction during that era; its exit demonstration qualifies the
next rank. R0 is the starting state. For example, R2 tools let the player earn R3;
R3's native Nether nursery does not require completing its own R3 exit first.
R7's optional expansion preparation leads to R8; R8 settlement opens R9's boss
chapter. R9 completion closes that chapter, not an automatic R10 power tier.

The rows show a recommended commissioning sequence, not a prohibition on
exploration or parallel research. Aquatic, field/combat and ecological research
branch from R2 and contribute alternative solutions through later eras; they are
not three compulsory completions. Vanilla Nether/End travel remains ungated by
rank. An experienced player can scout the End before R4 and settle it later.

## 4. Stage-by-stage journey

Each stage includes its first useful reward, alternatives, a base-design puzzle,
an exit demonstration and a reason its infrastructure survives the next stage.
“Demonstration” means a bounded, inspectable scenario using the player's build—not
an instruction to stand still while a progress bar fills.

### R0 — Contact: make a small place come alive

**Fantasy:** discover that life can be a building material and a tool.

The first session teaches one culture recipe using accessible vanilla materials,
one deliberate placement, one useful interaction and one visible mutation.
Recipe discovery, advancement and the guide explain the same sequence. The player
should get value before a large multiblock or a full suit is affordable.

Two possible openings:

- **Cultivator:** establish a small garden, gather plant specimens and turn common
  organic surplus into initial feedstock.
- **Scavenger:** process common animal or hostile-mob remains, gather a structural
  specimen and create a field-useful biological component.

Both reach the same starter culture without rare biome hunting. An acquisition
fallback uses common resources; exploration offers options, not a lottery ticket.

Substrate spreads only through deliberate actions. A biomass-loaded expansion
spore is a later convenience candidate: explicit area, finite charge, no new
controllers spawned by its descendants. Ordinary living ground never expands
indefinitely merely because it exists.

**Puzzle:** where should the garden go so it can connect to storage and workshops
without demolishing the landscape? The first tissue path climbs or follows terrain.

**Exit demonstration:** create and use one intentional patch, obtain a useful
biological output and identify what fed it. No mandatory tree removal or armor set.

**Continuing value:** starter cultures remain construction stock; low-grade inputs
feed later local services. Visual growth adds undergrowth and variation rather than
erasing the garden after harvest.

**Review:** a hidden recipe or a mandatory rare drop makes this onboarding fail.

### R1 — Circulation: your first automated organism

**Fantasy:** the base begins doing useful work while you do something else.

Unlock a small digestive structure, an expandable biomass reservoir, internal
tissue conduits and a basic service organ. Throughput/direction, starvation and
full outputs must be readable in-world and through diagnostics. Splits, joins,
vertical runs and crossings are available before layouts become expensive.

Two viable economies:

- **Garden economy:** renewable cultivated inputs, larger footprint, predictable
  batches and relatively simple handling.
- **Salvage economy:** varied surplus supplied through inventories, compact intake,
  but sorting and buffering are more important.

A vanilla farm, hopper arrangement or compatible mod can supply either economy.
No requirement to build a mob grinder; helpers are optional and not prerequisites.

**Puzzle:** reserve enough feedstock for the colony's useful services while feeding
construction. One large tank is easy, but separate reserves and priorities prevent
a construction order from starving the workshop. Long paths and uphill runs must
have clear, finite transport rules—not an expensive real-fluid simulation.

**Exit demonstration:** with inputs supplied automatically, serve two bounded
demands without hand-feeding the machines. Stop an output and show that processing
pauses safely; restore it and recover. Initial reserve is disclosed rather than
secretly forbidden. Repeated demand must reveal a functioning replenishment path.

**Continuing value:** reservoirs, digestion and low-grade conduits remain useful
as local branches. New tiers add specialized services rather than replace every
block with a differently colored version.

**Review:** if the only challenge is filling a larger tank, redesign this stage.

### R2 — Differentiation: choose what your biology is good at

**Fantasy:** grow equipment and organs around an intended job.

Unlock a DNA bank, a first mutation chamber, a readable mutation preview and
primitive potion-compatible preparation. The chamber has a stable biological
core and a few configurable attachments. It should look like a place an organism
develops, not a cube with a crafting slot.

Research can emphasize:

- **Movement and exploration:** useful travel, climbing or aquatic preparation.
- **Structure and defense:** impact protection, harvesting support or field combat.
- **Exchange and cultivation:** feedstock efficiency, filtration or research yield.

Choose a route to rank credit, not a permanent character class. Other branches
remain learnable. A particular mutation can require a full genome, but ranking up
must accept several suitable genomes rather than one obscure animal.

**Puzzle:** schedule a mutation batch that needs a clean input and steady supply
while the base continues serving other consumers. Decide whether to add a buffer,
separate a route or use a slower chamber profile. Usage practice refines a chosen
trait; it does not require AFK-running or repeatedly injuring yourself.

**Exit demonstration:** complete one genome, grow a meaningful adaptation and use
it for its intended task; supply a repeatable chamber cycle automatically. The
player can compare two plausible mutation previews before committing resources.

**Continuing value:** the atlas, chamber and workshop become the place for suit
refits. Early movement, utility and ecological traits participate in late designs.

**Review:** a mandatory perfect suit before any trait becomes useful is too steep.

### Parallel branch — the deep ocean is a destination, not a detour tax

Available from R2, with later refinements. An aquatic suit emphasizes breathing,
swimming, underwater visibility and stable underwater work. A prepared habitat or
vanilla breathing supplies provide an alternative to fully specializing armor.

Candidate rewards include selective membranes and efficient exchange traits.
They can improve cooling, filtration and field endurance. A land-based process
can reach the same required specification with different cost/space tradeoffs;
an ocean base is attractive, but not a fourth mandatory base hidden in early play.

Do not invent a global pressure-damage mechanic just to make the suit necessary.
Any special deep-water hazard would need a separate proposal. Standard oceans
remain understandable Minecraft environments.

### R3 — Thermogenesis: the Nether becomes home territory

**Fantasy:** make a place that is hostile to you hospitable to useful organisms.

Enter through an ordinary Nether portal with ordinary protective planning. Initial
biological settlement uses common cultured material, local Nether resources and
a modest imported nutrient reserve. It must not require the product it will make.

Propose **Emberbed**, substrate that can be matured only in the Nether. A thermal
organ operates only on an active Emberbed foundation in that dimension. Lava in an
Overworld room does not substitute for the native environmental condition. Exported
products are useful elsewhere; the producing ecology must stay in the Nether.

First unlocks: a native nursery, a heat-exchange organ, suitable containment and
the initial thermal armor branch. The early output is a stabilized heat-bearing
vesicle, not the highest-grade dangerous material.

Alternative layouts:

- **Buffered hearth:** lower output, large safe reserve and predictable exchange.
- **Pulsed hearth:** compact cycles with better peak output, but more demanding
  inlet/outlet scheduling and cooldown capacity.

Neither depends on water behaving as it does in the Overworld. The basic dry
cooling/heat-recovery path is available before advanced imported membranes.

**Puzzle:** preserve a safe work corridor and isolate the culture from hot
processing. Keep the reserve needed for restarting separate from production stock.
Design around slopes and caverns; do not require a perfectly flat industrial slab.

**Exit demonstration:** mature Emberbed locally and repeatedly produce a stabilized
thermal output with automatic feeding and safe shutdown. An unloaded support chunk
must pause the process, not secretly continue heating it.

**Continuing value:** Nether-grown thermal membranes and high-grade reagents are
consumed by advanced synthesis and mutation. A bag of first-visit loot cannot
replace the production facility forever.

**Review:** carrying a charged vesicle home is allowed; moving the entire productive
Nether ecology home must not be the optimal solution.

### R4 — Containment: power becomes a design responsibility

**Fantasy:** master a process powerful enough to demand architecture.

Unlock catalytic processing, specialized conduit linings, sensing/priority organs,
isolation valves and recovery chambers. These must all be available before the
first dangerous operating mode. Danger is opt-in operation, never rogue colony growth.

Propose **catalytic ichor**, a high-grade reaction medium produced using Nether
thermal output and a refined biological input. It enables more demanding mutation
and membrane production. It is not simply “biomass, but worth a thousand times more.”

Two operating strategies:

- **Dilute process:** more volume and footprint, slower response, lower exposure
  risk and forgiving control windows.
- **Concentrated process:** compact and high-throughput, with stricter separation,
  buffer headroom and emergency neutralization requirements.

**Puzzle:** reuse byproducts without allowing an output blockage to stop the safety
loop. More cooling competes with productive organ space. Aggressive throughput can
increase consumable and recovery cost rather than offer a free linear upgrade.

**Exit demonstration:** meet a specified product grade, then survive a disclosed
input interruption or output blockage. A controller must perform the response;
the player cannot pass by babysitting every valve. The challenge is short and
diagnostic, not a long endurance timer.

**Failure:** warn, throttle, isolate; if the player deliberately overrides safety,
a bounded local incident may ruin a batch and injure an exposed operator. Recovery
uses already available equipment. No spreading corruption, world-scale explosion,
offline poisoning or disappearing research.

**Continuing value:** low-risk lines remain economical for routine work; hazardous
lines serve particular grades. Mature players still choose where risk is worth it.

**Review:** if a restart or chunk boundary can cause an accident, the design fails
before balance is considered. Hardware lag is not a gameplay challenge.

### R5 — Anchoring: build a real End settlement

**Fantasy:** turn precarious footholds into a connected, productive habitat.

The vanilla dragon is an expedition milestone, not our campaign finale. Proposed
research uses encounter evidence or a renewable aftermath sample, never exclusive
ownership of the dragon egg. Players who arrive after another team killed the
dragon need a valid route. No repeated dragon kills for routine production.

A portable starter nursery uses R4 material and locally available End resources.
It creates **Anchorbed**, matured only in the End. Spatial organs require active
Anchorbed in the End even if their finished membranes can be exported.

Unlock an anchor nursery, spatial membrane organ, local landing/return support and
End-specialized adaptations. Starter settlement is possible without advanced
flight, a completed End genome or cross-dimensional biological transit.

Alternative layouts:

- **Anchored bastion:** a protected, buffered installation concentrated on one
  island, with longer physical routes to gathering locations.
- **Stepping-stone outposts:** smaller bounded installations and short local links,
  with more distributed reserves and maintenance access.

**Puzzle:** arrival safety, accessible service routes and interruptions matter as
much as production speed. High-grade spatial output needs a timed active cycle,
not permanent ticking of every decorative block. Timing is local process phase,
not “come back next real-world Tuesday.”

**Exit demonstration:** operate a native organ, produce repeated spatial membranes,
and prove safe arrival/refusal at the local transfer point when its destination
is obstructed. Return travel is secured before a risky field objective.

**Continuing value:** the End conditions spatial membranes used in distant links,
precision mutation and advanced synthesis. Shipping chorus fruit home does not
replace this function.

**Review:** make the End settlement worthwhile before giving it an export quota.
Its mobility, staging and spatial crafting should help the player while building it.

### R6 — Confluence: three worlds, one designed system

**Fantasy:** your distant bases function as organs of one organism.

Unlock **Graft Gates** for long-distance/cross-dimensional travel and specialized
transfer organs for bounded cargo. Names and exact transit mechanism are open.
Gates require constructed endpoints and validated arrival space, not an arbitrary
coordinate and unlimited teleportation. Separate personal travel from bulk freight
capacity so one does not silently grant the other for free.

The three persistent specialties now interlock:

- Overworld biological cultivation supplies refined nutrients and recovery media.
- Nether thermal organs produce high-grade catalysts and thermal membranes.
- End organs condition spatial membranes and phase-sensitive products.

Each settlement has a low-throughput local restart path; advanced throughput depends
on imports. Losing a shipment must not create a circular dependency that prevents
every site from restarting. Useful local reserves make a distributed design robust.

Alternative architectures:

- **Hub and spoke:** centralized refining and scheduling, simpler oversight but a
  larger consequence when the hub saturates.
- **Regional autonomy:** more local processing and duplicated support, less freight
  and better isolation, but larger total footprint.
- **Hybrid:** central research and rare processes, regional routine services.

**Puzzle:** allocating transport to regeneration, production and expedition demand.
A higher rate at one organ may make the whole system worse by monopolizing a link.
Choose priorities, batch size, reserves and recovery paths rather than lay a single
universal vein and forget about it.

**Exit demonstration:** satisfy a combined production order with genuine native
processing in all three dimensions; then recover from a scheduled link outage
without loss or manual relaying. No requirement for all dimensions to remain loaded
simultaneously: bounded endpoint buffers and staged active sessions are valid.

**Continuing value:** travel convenience makes the multi-base game enjoyable. It
does not remove native production conditions or turn every dimension into a lobby.

**Review:** without an approved chunk-loading system, no trial may require one.
Remote suspension is normal operation, not a punishment to work around.

### R7 — Synthesis: the long post-End game

**Fantasy:** design a biological solution other players might want to study.

Unlock **Synthesis Arrays**: configurable assemblies of chambers, exchange organs,
membranes and regulating tissue. Their construction follows constraints, not one
huge fixed hologram. Choose a core function and vary attachments, branches and
processing profiles. Bigger is not automatically better.

Advanced DNA combinations can change how an organ works: selective extraction,
burst operation, recovery of a particular byproduct, or lower peak demand. The same
genome may inform equipment and infrastructure through distinct expressions.
Nothing here raises the armor's ultimate capacity to “install everything.”

Alternative endgame projects:

- **Lean civilization:** minimize material/energy consumed per useful product.
- **Expedition civilization:** sustain specialized suits, consumables, safe transit
  and rapid deployment at several frontiers.
- **Resilient civilization:** keep critical services available through interrupted
  supply and recover without sacrificing expensive batches.
- **Compact civilization:** meet a service target in little space without external
  support hidden outside the measurement boundary.

**Puzzle:** a composite order needs several incompatible grades or profiles.
Decide between dedicated lines, buffered batch sharing and parallel arrays.
Capturing maximum yield everywhere may lose to a simpler design with fewer
transport steps and lower service overhead.

**Exit demonstration:** choose one mastery project and meet an understandable
service envelope. This recognizes achievement, not the only “correct” base.
There is no obligatory million-item trophy or single universal final machine.

**Continuing value:** publish layouts, compare mutation plans, rebuild around a new
terrain or partner's specialization, and attempt voluntary constrained trials.
The campaign has a satisfying conclusion here even if no fourth dimension ships.

**Review:** the most capable machinery should create more interesting choices than
the early chamber. If it erases intermediates, risk and specialization, revise it.

**Optional expansion preparation:** a bounded three-world synthesis experiment
produces the first aperture seed using only R7-or-earlier resources. It reveals
evidence of a different regulatory ecology and qualifies entry to R8. Neither the
seed nor the return kit requires material from the still-unvisited fourth world.

### R8 — Foreign Symbiosis: a fourth world with a different rule

**Very-long-term expansion concept. Working name: the Palimpsest.**

This is a layered alien biosphere: older mineralized growth supports newer
membranes, hanging gardens and immense dormant organs. It is not another Nether
with higher damage. Its distinctive problem is **reciprocity**: productive native
organs require complementary biological conditions in a bounded local habitat.

The colony remains yours. The dimension has its own native ecology; it cannot
hijack your substrate or spread into other worlds. Large environmental shifts
should be visual or localized to registered habitats, not world-wide block rewrites.

The entry aperture is constructed from R7 capabilities distributed across the
three existing dimensions. Its first expedition kit contains a protected return
method that does not require killing the new boss or receiving another shipment.

On arrival, a low-grade native starter process establishes **Reciprocity Bed**.
It can exist actively only here. Its first functional output, **regulatory matrix**,
allows an organ to switch between two incompatible *modes* safely. It does not
allow both modes at full strength simultaneously.

Two settlement strategies:

- **Sheltered enclave:** buffer environmental inputs and maintain a narrow stable
  operating profile; reliable but resource-intensive.
- **Responsive habitat:** alternate complementary organ groups as local conditions
  change; efficient but requires planning, reserves and understandable control.

**Puzzle:** expansion must preserve useful relationships among planted organisms.
A brute-force cleared rectangle loses productive habitat functions. Trees and
canopies may participate, but their final implementation is not assumed here.
Different arrangements should satisfy the habitat contract; never require a
single decorative arrangement under the guise of ecology.

**Exit demonstration:** establish a repeatable native process and maintain a safe
return route through a local operating-cycle change. Imports support advanced
production, but the starter ecology can restart on previously unlocked materials.

**Continuing value:** native regulatory matrix enables alternative synthesis and
equipment architectures across the colony. It complements the other dimensions'
exports rather than replacing all of them with a superior universal substance.

**Review:** exploration and settlement must be enjoyable before the boss is available.
If this is merely a boss room and a rare ore, it does not justify a dimension.

### R9 — Coexistence: a boss that tests the civilization you built

**Very-long-term boss concept. Working name: the First Pattern.**

A vast native organism regulates a region of the Palimpsest. It responds to a
player-initiated challenge within a defined arena; it does not raid unattended
bases. Its anatomy and behavior express biological regulation rather than a
reskinned familiar franchise creature.

The encounter combines three skills without requiring three simultaneous players:

1. **Read:** telegraphed actions reveal which defensive organ or metabolic mode is
   active. DNA observations and the guide explain the rules before repeated failure.
2. **Reconfigure:** the player's staged apparatus delivers one of several valid
   counterconditions. The boss has a finite, disclosed response repertoire—not
   an omniscient counter to whatever build the player chose.
3. **Execute:** movement, positioning and attacks exploit the resulting openings.
   Infrastructure creates opportunities; it does not fight an entire AFK battle.

Two approaches might be a robust supply line creating fewer, longer openings, or
a lean timing-based apparatus creating frequent short ones. They demand different
gear/DNA plans. A solo player can preconfigure both responses; multiplayer adds
roles, not a required headcount.

Failure costs a bounded attempt's supplies and recovery effort, not the three-world
base. The route back remains available. Restarting an encounter does not require
repeating the entire logistics campaign. The boss cannot spawn unlimited minions,
projectiles or loot; arena and encounter counts need explicit server limits.

**Reward:** complete a unique regulatory discovery and unlock new arrangements of
organs, controlled mode-switching, and architectural/visual trophies. The discovery
is guaranteed for legitimate completion and shareable under the chosen team policy.
Routine products do not demand repeated boss kills. Optional rematches vary tested
conditions and reward distinction, not endlessly escalating armor stats.

**Afterward:** tune fourth-world habitats, build a different specialization, attempt
efficiency challenges and support other players' expeditions. The boss resolves a
chapter; it does not give creative flight, universal immunity or infinite biomass.

**Review:** ordinary strong weapons must contribute; no secret “only our sword
works” rule. Infrastructure requirements need visible encounter logic, not arbitrary
damage immunity applied solely to prevent other mods from participating.

## 5. Decisions and cross-cutting systems

The stage sequence is a first pass. The following sections stress-test its
dependencies, equipment balance, automation requirements and long-term depth.

### 5.1 Alternative progression models

We should choose how levels feel before committing to their names or count.

| Model | How advancement works | Strength | Main risk |
|---|---|---|---|
| A. Rank spine | A visible personal rank unlocks each major era; several achievements qualify | Clear next goals and strong sense of arrival | Can feel like a permit system or checklist |
| B. Research web | Multiple research branches converge on advanced designs; rank summarizes completed capability | Exploration and specialization feel natural | Players can become lost or accidentally hit hidden dependencies |
| C. Dimensional mastery | Separate Overworld, thermal, spatial and alien proficiencies combine | Encourages parallel work and cooperative roles | Too many counters; a specialist can get trapped behind unrelated chores |

**Recommended concept:** A's legible rank spine, B's choice of qualifying research,
and C's dimension-native infrastructure. Show one main player rank and a readable
atlas, not four additional XP bars. Rank is an acknowledgment of meaningful work.
Do not make the player construct a bureaucratic “rank machine” at every stage.

Where possible, a milestone is the first successful useful process itself. For
example, commissioning a chamber under a variable load produces a component the
player wanted, while demonstrating their network. A guide challenge that exists
only to hand out permission should be optional, or redesigned into useful play.

### 5.2 Four viable campaign routes

These are starting priorities and architectural styles, not locked classes.
All routes eventually establish the three required dimensional production roles.

| Route | Early advantage | Midgame preference | Late competitive strength | Real tradeoff |
|---|---|---|---|---|
| Ecologist | Cultivation, plant genetics, efficient feedstock | Distributed gardens and low-risk refining | Low resource cost, graceful recovery | More land and slower peak response |
| Expedition specialist | Mobility, defensive traits, field samples | Early Nether scouting, focused outposts | Fast deployment and strong environment-specific equipment | More reliance on reserves and supply planning |
| Biological engineer | Early sorting, circulation and process control | Parallel organs, compact productive districts | Throughput and constrained-footprint solutions | Complex interactions and expensive mistakes during commissioning |
| Marine/alchemical researcher | Exchange membranes and potion preparation | Aquatic infrastructure, selective processing | Excellent grade control and consumable efficiency | A narrower first specialization and additional preparation |

An ecologist may develop cooling membranes before scaling thermal production. An
engineer may accept a less efficient dry process to commission it earlier. An
expedition specialist may use vanilla potions and protected routes while deferring
expensive armor. None should need to replay another route's entire early campaign.

For multiplayer, these roles can cooperate through shared infrastructure and
trade. A solo player can acquire the same abilities sequentially. Research sharing,
team changes and personal binding need an explicit ownership design before code.

### 5.3 Make the dimensions structurally necessary

“Only found in the Nether” is not enough: stockpiling that item still encourages
one central Overworld factory. **The production operation itself must need a
native substrate, a valid structure and that dimension's conditions.**

| Site | Native substrate proposal | Work that stays local | Export and recurring use | Imports for advanced operation |
|---|---|---|---|---|
| Overworld | Verdant Bed, a specialized living substrate | Diverse culture renewal and high-grade recovery media | Nutrient cultures and repair media for mutation/processing | Thermal refinement products; later spatial conditioning |
| Nether | Emberbed | Thermal maturation and high-grade catalyst production | Thermal membranes and catalyst charges for advanced organs | Refined nutrients, exchange membranes and recovery media |
| End | Anchorbed | Spatial membrane conditioning and anchor growth | Membranes consumed by advanced synthesis, freight and gate operation | Nutrient cultures and stabilized thermal products |
| Palimpsest, horizon only | Reciprocity Bed | Regulatory matrix maturation in a native habitat | Mode-regulation material for alternative high-tier designs | Products and support from all three established worlds |

These are proposed *local process requirements*, not universal dimension checks on
all items. Players can carry products, samples and ordinary substrate freely.
Special beds moved elsewhere become dormant or unsuitable foundations; they do
not silently disappear or damage the environment. Revalidation after moving,
reforming or reloading must prevent an active foreign machine from bypassing this.

Start the specialized Overworld bed at R2 using only early materials. Keep the
ordinary starter substrate useful anywhere it is deliberately placed. Native beds
add visible ecological identities, not four colors of the same flat surface.

The persistent economy has an essential distinction:

- **Bootstrap:** earlier-tier supplies plus local ordinary resources can establish
  a low-rate process and restart an interrupted settlement.
- **Optimization:** imported high-grade products improve quality, throughput or
  advanced operating modes. They never retroactively become necessary to restart
  the very process that produces their ingredients.

Finite buffers are allowed; bulk stockpiling can postpone a shipment but cannot
replace all recurring native production. Nevertheless, if the player stops
requesting advanced work, the colony can rest safely. This is not a survival tax
that forces daily visits to every dimension.

**Loading constraint:** actual remote production runs only where chunks are loaded.
Cross-world design must work through capped buffers and resumable shipments, with
no mandatory offline processing. An explicit chunk loader remains a separate
approval, not an invisible gate feature. Do not fake remote world simulation.

### 5.4 A small biological construction language

New rank should change the *available relationships* among components, not merely
permit a bigger reservoir. Proposed reusable parts:

- **Chambers:** contain a reaction, specimen or developing item.
- **Membranes:** select what crosses a boundary and what remains separate.
- **Exchange organs:** trade one process condition for another at a known cost.
- **Reservoirs:** provide capacity, reserves and isolation; shape remains bounded.
- **Conduit tissue:** carries a specified resource, with deliberate crossings,
  junctions, direction and priority.
- **Regulating organs:** respond to measured conditions and change local modes.
- **Support growth:** living surroundings that can provide selected functional
  services; trees and bushes are candidates, not settled mechanics.

Stable multiblocks express a clear core anatomy; modular attachments allow several
valid capacities and profiles. Arrays combine these structures. There should be
valid compact, elongated, terraced and vertically separated arrangements within
explicit size/connection limits. Structure-validation errors explain the problem.

Example: a chamber has limited exchange surface. Adding a protective membrane can
improve product grade but reduce flow. A second exchange organ restores flow at
the expense of productive volume. Two smaller chambers may beat one enormous
chamber when their downtime alternates. Those are puzzles; “add another identical
ring of casing” is primarily a construction cost.

Planning grows in importance through service corridors, reserved expansion space,
separated resource circuits, hazard isolation, reachable terminals and restart
reserves. Early relocation tools should preserve content and reduce rebuilding
tedium. They must not move an active dimension-bound process across its boundary.

### 5.5 The colony should look worth living in

Preserve ground cover, undergrowth, trunks and canopy as visual layers. Some
proposed roles are nutrient exchange, selective secretion, habitat support and
non-destructive harvesting. We must discuss which belong to plants or trees before
assigning them permanent mechanics. This map does **not** approve passive forest
assimilation, autonomous wood farming or any specific tree output.

Functional ecology should give players a reason to retain these layers. Avoid a
single mandatory tree arrangement or a universal “more plants = more power” bonus.
Several habitats and structural substitutes should serve different needs, while
clearing everything must not be the best solution to every production problem.

Inactive growth can remain attractive without producing anything. Texture variation,
membranes, local pulses and subtle movement can convey life without permanent
server tickers, mobs or an entity per leaf. Finished colonies should have readable
districts and vegetation, not visual noise covering every exposed face.

### 5.6 DNA: completion, combination and meaningful practice

The player still collects many samples to complete a genome. Make that finite,
visible and useful along the way. Repeated legitimate samples count; variety can
offer an efficient alternative, but a player should not need dozens of unique
biomes, named individuals or real-world play sessions per species.

Three acquisition paths can coexist:

1. Field sampling yields broad discoveries and environment-specific evidence.
2. Cultivation or controlled observation automates repeat sampling of a known source.
3. Processing compatible remains supplies material evidence, with an appropriate
   yield/coverage tradeoff; a mob farm is a choice, not the only research method.

Higher ranks demand better separation or verification, not merely ten times more
of the same low-grade sample. The DNA bank stops accepting useless duplicates at
completion unless they have a defined bounded material use. Do not store an
ever-growing history of all sampled entities to enforce research rules.

A genome unlocks a family of expressions. For example, a thermal trait can inform
protective equipment, an exchange membrane or a catalyst-support organ; these are
distinct recipes with distinct limits, not one copyable universal bonus.

**Usage development:** a player selects a trait to train, sees its bounded progress
and receives refinement while performing its useful activity. DNA mutation can
establish a usable baseline without exercise grinding. Practice can improve
efficiency or handling but cannot unlock every incompatible branch simultaneously.
Refitting is deliberate at a workshop, with a modest disclosed cost and preview;
no reroll lottery, hidden failure chance or permanently ruined unique equipment.

Design against obvious optimal chores: running into walls, falling onto healing
loops, repeatedly equipping gear, and sampling the same entity every tick. Use
bounded counters and meaningful event categories, not invasive behavioral tracking
or a promise of perfect anti-AFK detection.

### 5.7 Armor: exceptional within a role, limited across roles

Each suit has three proposed constraints: finite organ capacity, a sustainable
metabolic output, and physiological compatibility. Fuel storage affects endurance;
it does not expand anatomy or make incompatible systems coexist. Higher ranks
unlock better architectures and refinements while retaining a final hard ceiling.

| Suit direction | Where it excels | What it deliberately does not solve |
|---|---|---|
| Thermal | Sustained fire/lava operations, heat handling, secure hot-surface work | Deep-water exchange, top swim performance, universal physical protection |
| Pelagic | Underwater breathing, visibility, mobility and work | Sustained lava exposure and heavy impact specialization |
| Bastion | Physical defense, bracing, protecting a work position | Rapid traversal, strong elemental protection and effortless escape |
| Wayfarer | Traversal, reach and expedition economy | Peak defense and sustained extreme-environment work |
| Spatial | Controlled short displacement, landing support and End work | Unlimited flight, arbitrary teleports and immunity to falling into the void |
| Generalist | Convenient travel and brief exposure to several conditions | Specialist endurance, peak movement or the highest protective operating envelope |

Illustrative thought experiment, **not balance values**: a mature suit has 12
capacity units. A sustained thermal suite uses 9; an aquatic suite uses 9. A
4/4/4 generalist can carry several useful minor traits, but cannot fit either
complete specialist architecture. Traits need meaningful thresholds and curves;
this cannot be solved merely by attaching a small penalty to an otherwise universal
set. Later research must not quietly raise capacity until both specialist suites fit.

Separate reserves and output: enough biomass to operate for an hour is not enough
metabolic output to regenerate, sprint, shield and blink at maximum simultaneously.
Convenient refills can remove travel chores without removing this competition.

Full-suit functions require the relevant coherent mutations across armor pieces.
Mixed pieces retain their ordinary local benefits, but should not collect several
complete-set bonuses. Curios organs can offer a deliberate utility or modifier;
they cannot bypass the same capacity, incompatibility or activation limits.

Defense retains readable vulnerabilities and an exhaustion fallback. A thermal
specialist can plausibly work in lava while still being vulnerable to combat or
other hazards. No blanket claim that it absorbs every damage type. Low-fuel
warnings and a safe reserve make retreat possible; surprise instant death when a
hidden counter expires is not desirable balance.

Equipment swapping remains useful preparation, but must not yield instantaneous
universal protection: show acclimation/activation rules, clear effects on removal,
and preserve ability cooldowns across unequipping. Do not punish ordinary gear
changes or require a long wait every time someone leaves their base.

Weapons and tools follow the same logic: selective harvesting versus bulk work,
reach versus speed, precision sampling versus destructive output. A boss reward
may unlock a new attack or harvesting approach, not a universal one-hit tool.

#### Candidate mutation paths through the ranks

These examples give the atlas concrete shape without fixing recipes or asserting
real biological mechanisms. Species and effects still need their own design review.

| Path | Early usable expression | Later directed DNA combination | Mastery remains limited by |
|---|---|---|---|
| Thermal | Brief heat tolerance and visible reserve at R2 | Magma-cube/blaze-inspired heat handling, completed captures and Nether membranes at R3–R4 | Exchange capacity and specialist anatomy; no blanket combat immunity |
| Pelagic | Longer breathing or better underwater work at R2 | Turtle/squid/dolphin-derived alternatives for protection, sensing or movement | Choose a durable working suit or agile exploration suit; neither becomes a lava suit |
| Locomotion | Practiced efficient movement at R2 | Alternative climbing, bracing or burst-movement expressions from captured organisms | Activity-specific output and incompatible body plans, not unlimited speed stacking |
| Spatial | Safe landing support during R5 settlement | Complete enderman research plus End-conditioned organs permits a controlled blink variant | Valid destinations, finite range/output and persistent cooldown; no wall/claim bypass |
| Regulatory | R7 predictable switching of production profiles | Fourth-world research permits richer, deliberately sequenced profiles at R8–R9 | Transition cost and one active incompatible mode at a time |

Thermal and aquatic examples must have several viable genomes or trait expressions
where practical, not a mandatory checklist of every species named in the row.
The first protective step precedes the dangerous sampling trip. A player can use
vanilla preparation, a supported habitat or an earlier safe trait to acquire the
next sample; advanced armor never requires farming its own already-protected use.

Boss or dangerous-mob DNA can unlock a distinct behavior, but no single gene should
be a prerequisite for all builds. In particular, “captured powerful creature” does
not mean “copy all of its immunities.” Combat encounters supply discoveries; a
production chain should not require endless Wither or dragon resummoning.

### 5.8 Enchantments and potions: decisions, not silent exclusions

The owner is reconsidering enchantments on bio armor. **No removal is approved.**

| Option | Benefit | Cost/risk |
|---|---|---|
| A. Normal enchantments | Familiar Minecraft compatibility | Mutations, protection and repair may multiply into a universal suit |
| B. Curated compatibility | Keeps selected enchantment value alongside biology | Requires explicit rules, explanations and maintenance for modded enchantments |
| C. Non-enchantable bio armor | Clean, self-contained mutation balance | Loses a familiar progression system and can disappoint pack players |

Proposed experiment order: test A against specialist constraints; compare B if
stacking undermines them; choose C only after discussing its player cost. Do not
arbitrarily strip existing enchantments, disable enchanting globally or silently
apply the armor decision to all tools and weapons. Document anvil, repair, loot,
commands and imported/modded equipment behavior when the policy is chosen.

Potions offer a distinct kind of progression: **temporary chemistry versus
persistent anatomy**. Proposed uses include preparation for expeditions, controlled
delivery from a bounded reservoir, and feedstock for particular mutation processes.
Brewing remains useful; discovering a DNA trait need not make every corresponding
potion ingredient obsolete.

A potion-processing organ could specialize in duration, selective delivery or
resource recovery. Each profile needs a cost and a limit. It must not duplicate
effects, create free bottles or turn one potion into a permanent full-strength
ability. Unknown mod effects are not automatically safe processing ingredients.

Fire Resistance may legitimately let an aquatic specialist make a short Nether
trip. That is a useful combination, not an exploit to suppress. The thermal suit
must earn its identity through endurance and additional working capabilities,
not just replicate that one potion. If cheap permanent potion automation makes
the two suits equivalent, revise the design; do not claim specialization works
while ignoring that combination. Apply the same test to beacons and other mods.

### 5.9 Power and substances: introduce new constraints gradually

Avoid five interchangeable progress bars. Each material or condition must create
a different decision. “Biomass” is feedstock, not a magical unit that directly
replaces every ingredient, energy source, catalyst and gene.

| Introduction | Proposed resource/condition | Useful capability | New handling problem | Safe low-tech response |
|---|---|---|---|---|
| R0–R1 | Bulk biomass and ordinary nutrients | Growth, repair, basic work | Supply balance and full storage | Pause intake/consumption; retain contents |
| R2 | Refined culture and selective membranes | Reliable mutation and separation | Grade versus yield; clean versus mixed streams | Segregate or reprocess a batch |
| R3 | Heat-bearing vesicles | Thermal work and specialized materials | Rate of charging/discharging; heat recovery | Stop feeding and use the supplied dry recovery path |
| R4 | Catalytic ichor | High-grade synthesis and demanding mutations | Concentration, incompatible contacts, recovery capacity | Isolate and neutralize in a bounded chamber |
| R5–R6 | Conditioned spatial membranes | Anchoring, precision work and transport | Destination validity and active process phase | Refuse, buffer, pause and resume |
| R8 | Regulatory matrix | Deliberate switching between organ profiles | Complementary habitat conditions and mode transitions | Retreat to a stable local mode |

Matter and useful work are accounted for separately. Byproduct recycling recovers
some inputs or useful work; it cannot multiply substrate, free energy and catalysts
in a closed loop. A waste output needs a safe bounded handling path at the same
tier as its producer. Dumping into the world is never the intended solution.

Danger increases through operation choices and stricter containment, not arbitrary
random disasters. Higher output may need more relief capacity or isolation space.
Indicators should show where the margin is being consumed. Shutdown preserves
resources or produces a disclosed bounded residue; it does not erase inventories.

Prefer explicit machine-contained conditions over a new simulated global heat,
pressure or contamination field. No wall-clock spoilage while a player is offline.
Chunk unload, restart and server lag must not bypass a safety interlock or advance
only the hazardous half of a process. Breaking a loaded container needs a defined
safe recovery path, not an incidental cloud of damage across neighboring bases.

External energy integration is a later adapter decision. An FE input, if supported,
may help a defined process; abundant power cannot replace DNA, native environmental
conditions, anatomy or every biological reagent.

### 5.10 Automation gates without bulk grinding

A rank demonstration should measure useful service under a clear constraint.
The machinery produces actual useful goods during it. Example: maintain two
different output grades across a disclosed demand change with a stated initial
reserve and a capped input rate. The puzzle is routing, conversion and buffers—not
crafting a million identical items.

Automation is necessary because multiple consumers need concurrent or sequenced
responses that the installed system can reliably provide. A mature design should
reproduce the result unattended during the test window. Players may use native
logic, vanilla redstone or compatible automation; do not require proof that every
upstream hopper belongs to this mod. Avoid brittle world-wide automation detection.

Qualification observes bounded controller events: valid output, admitted input,
quality, reserve state and safe refusal. A handcrafted cache can support a run,
but the repeated service envelope must expose whether replenishment works.
Exact windows and thresholds need prototypes; the claimed anti-stockpile property
is a design objective to test, not something this document proves.

Long play should come from learning new constraints, exploring, choosing a branch,
building infrastructure and iterating. Once a process is understood, tools should
make repeating it easier. Avoid long growth timers as substitute content. A useful
batch may take time, but the player should have worthwhile parallel work and
legible bottlenecks, not merely wait to be allowed to start the next chapter.

Replayability also requires avoiding arbitrary random recipes. Let terrain,
available inputs, optional research and chosen performance objectives change the
best design. Provide at least two starter solutions and explain principles, while
leaving ample room for solutions better than the examples.

### 5.11 Competition: compare designs, not hours spent online

Propose optional local benchmark contracts that players can share as reports.
No global leaderboard service or account integration is required. Every comparison
specifies game/mod versions, enabled integrations, available DNA, rank, inputs,
initial reserves, footprint, active chunks, equipment and the demand scenario.

Use separate categories rather than one weighted “best colony” score:

- Material efficiency: accepted useful output per consumed input, with byproducts
  credited only under the same declared accounting rules.
- Service rate: valid sustained throughput under the same resource/space limits.
- Compactness: footprint and support volume for a specified output envelope.
- Resilience: recovery and maintained critical service after a defined interruption.
- Equipment design: task completion and consumption under the same environmental
  and mutation restrictions; no vague damage-per-second contest across unlike roles.

Measurements include remote supporting sites and imported resources, or explicitly
declare them as priced inputs. A giant factory hidden outside the test boundary
cannot make a tiny front-end qualify as a self-contained compact design.

Growth in server cost is a hard eligibility constraint, not a way to earn points.
Thousands of redundant machines, loose entities or loaded chunks cannot be a
competitive strategy. TPS varies with hardware; report work counts and environment
alongside profiler evidence instead of calling raw TPS a fair global score.

There may be a best design for one fixed test. The goal is several competing
objectives and contexts, not a promise that mathematics never produces an optimum.
Shareable layouts and mutation plans should make copying accessible while giving
experienced players reasons to adapt and improve them.

## 6. Iteration review: reject attractive but broken progression

This review is a paper-design check, not gameplay validation. Each stage above
was reviewed first for its new decision, then for its dependency and failure paths.

| Trap found while drafting | Revision in this concept | What a prototype must prove |
|---|---|---|
| Native product needed to start its own producer | Earlier-tier starter cultures and local bootstrap paths | Empty-store restart works in each dimension |
| Automatic growth quietly becomes endless | Manual ground expansion; optional finite-area, finite-charge spore | Bounds survive save/reload and overlapping deployments |
| Tree function is prematurely locked in | Habitat roles remain examples; tree behavior needs its own discussion | A useful tree system preserves visual richness and player control |
| Endgame means a bag of dimension loot | Productive native beds and recurring advanced outputs | Imported blocks/products cannot replace the native operation |
| Three-world operation secretly requires chunk loaders | Buffered, resumable work; no simultaneous-loaded-world trial | Repeated unload/resume conserves resources and makes progress |
| Higher armor tiers eventually fit everything | Final anatomy ceiling and incompatible specialist architectures | Generalist loses meaningful peak capability even with abundant fuel |
| Potions/enchantments erase the armor tradeoff | Explicit compatibility alternatives and combined-effect tests | Specialized jobs remain distinct under normal external effects |
| Efficiency means stripping every plant | Functional habitat options and retained visual layers | At least two useful ecological layouts beat the cleared-floor default for relevant jobs |
| Automation proof becomes a chore or exclusive pipe requirement | Useful commissioning batches and interface-observed service contracts | Manual hand-feeding is insufficient, but legitimate external automation works |
| Dangerous materials punish crashes instead of mistakes | Safe pause/restart and bounded explicit operating incidents | Failure injection never produces runaway world effects |
| New boss requires its own exclusive loot to reach it | R7 entry kit and R8 starter ecology precede boss rewards | First encounter is reachable without trading or prior kills |
| Final reward deletes the game | New modes, architectural options and prestige, not universal upgrades | Existing specialist suites and dimension roles remain relevant |

### A worked design comparison to test early

Suppose a mutation workshop and construction organ share nutrient supply. A player
can choose a single central reservoir with priorities, separate local buffers, or
alternating production batches. The workshop needs stable grade; construction
tolerates pauses. All three layouts can meet the same ordinary demand.

Now interrupt the feed for a short interval and introduce a construction burst.
The central design may recover quickly but needs correct priority; the distributed
design may preserve the workshop at greater footprint; batching may need less
storage but complete the construction order later. Add a membrane mutation that
improves grade at reduced flow, then compare again.

This small puzzle is a better first proof of the intended game than building a
huge late-game reactor immediately. If it has only one sensible answer, adding
more tiers will not create the missing depth.

## 7. Testability and performance conditions for the roadmap

This document changes no runtime behavior. Before implementing any stage, discuss
material cost under [PERFORMANCE.md](PERFORMANCE.md) and set measured acceptance
criteria. Broad ambition is not authorization for unbounded simulation.

- Pure rules: rank qualifications, DNA completion, capacity/incompatibility,
  process conservation, reserves, grades and safe state transitions.
- GameTests: real native-dimension requirements, multiblock formation/split,
  storage refusals, integration interfaces, interrupted processes and advancement
  discovery. Verify behavior, not just registration.
- Client fixtures: visible directions, readable grades/failures, armor silhouettes,
  living ecology, translucent structures, biological motion and guide navigation.
- Save/network tests: earned ranks, ownership, research, active batches and transit
  survive restart; clients cannot mint qualifications, outputs or abilities.
- Abuse tests: duplicate sampling, equipment swaps, copied data, all-effects armor,
  imported/moved native beds, full output, overlapping spores and boss disconnects.
- Scale tests: shared budgets across many machines, bounded route/structure work,
  partial loading, bounded packets/particles, maximum arena population and sustained
  zero loose output from background processes.
- Human playtests: two viable solutions per early puzzle, understandable recovery,
  useful first-session rewards, no mandatory AFK behavior, and reasons to revisit
  each established dimension that are more interesting than errands.

Each process needs a maximum structure size, active operation count, work budget,
buffer capacity and overload response before implementation. Long-distance veins
use bounded topology work, not a full network scan per transfer. Biological art
must be judged in dense scenes; “client-side” does not mean performance-free.

Completion claims remain specific: a passing rank test is not proof of balance;
a passing screenshot comparison is not proof of beauty; a small server fixture is
not evidence that a hundred linked bases are affordable.

## 8. How to turn this concept into milestones

Do not queue ten rank implementations. First settle the player experience and
validate the reusable systems with narrow vertical slices.

- [ ] Review the rank-spine/research-web recommendation and whether rank should be
  experienced as personal symbiosis, scientific mastery or something else.
- [ ] Choose the first route pair and the first useful 30-minute experience.
- [ ] Build and compare the small circulation puzzle before expanding the catalog.
- [ ] Define DNA sample completion and the first two genuinely different armor jobs.
- [ ] Compare enchantment policies and potion combinations with actual equipment.
- [ ] Discuss transformed trees and plants separately: function, appearance, control,
  harvest and regeneration boundaries.
- [ ] Prototype one native-dimension process with a non-circular bootstrap and safe
  restart; only then design the larger interdimensional economy.
- [ ] Approve native substrates, major substances and their naming/ontology.
- [ ] Define ownership and shared research without mandatory repeated team chores.
- [ ] Define long-distance transit and operation with unloaded endpoints before
  approving any explicit chunk-loading ability.
- [ ] Establish two useful configurations for the first modular multiblock family.
- [ ] Set duration and difficulty targets from playtests; distinguish active problem
  solving, construction, travel, repeated gathering and passive waiting.
- [ ] Ratify a satisfying three-dimension R7 endpoint independently of expansion.
- [ ] Keep the fourth dimension and boss in a horizon backlog until the base game
  has demonstrated depth, stability and acceptable server cost.

The core review question for every proposed addition:

**What can a thoughtful player design differently because this exists?**

If the answer is only “make the previous thing again, but more,” it needs another pass.
