# Testing and evidence

Run `./.ktask/verify.sh` from any directory. It runs `verifyAll`, preserves the log
in `build/verification/full-gate.log`, requires a nonzero count of passing required
GameTests and rejects severe output. There is no skip flag. Use Bash and ripgrep.

Current layers:

- `python3 scripts/check_design_docs.py --self-test` and its normal invocation:
  prerequisite grammar, unknown references/cycles, block and armor-family guide
  coverage, permanent material parents, unique catalog IDs, specimen-count rounding,
  native supply surplus arithmetic, milestone dependency/acceptance coverage,
  intact Markdown tables,
  local links, questions-only entries and shared-capacity armor contracts.
  Both run in the authoritative gate. These are structural documentation
  checks, not proof of recipe balance, gameplay implementation or artistic quality.

- `python3 scripts/check_armor_balance.py --self-test` and normal invocation:
  fixed-point learning allocation, shared capacity and the armor specification's
  threshold/cost arithmetic. Calculation checks only, not measured gameplay pacing.

- `./gradlew :core:test`: three pure tests covering quota exhaustion, repeated
  refusal, invalid limits, next tick, clock rewind and long timestamp extremes.
- `:core:verifyBoundary`: rejects production dependencies in the Java-only module.
- `verifyDistribution`: opens the built JAR, checks real entry point/core/metadata,
  rejects unresolved metadata placeholders and development/legacy content.
- `runGameTestServer`: loads the real production mod plus a separate development
  mod; checks the core contract across actual server ticks. Requires one test.
- `runClient` / `runServer`: isolated normal development launch configurations.
  They do not silently copy saves, accept EULAs or alter the prototype.

## Not established yet

No production gameplay, recipes, persistence, UI or assets means no claims of
gameplay coverage. Automated client screenshots/goldens, data-generation freshness,
equipment/recipe/menu tests, optional-mod profiles, packaged deployment smoke,
multiplayer soak, crash recovery and measured dense-base TPS/FPS remain tasks.
The bootstrap gate must grow with each approved feature; it is not the final gate
for the complete mod. GameTests alone do not replace visual or gameplay review.

Run graphical and headless gates as separate invocations. Bound each fixture and
keep it in development sources. Capture readiness must verify the actual server
and client state, including quota-delayed work—not merely wait an arbitrary time.
Golden approval and appearance changes require human review.
