# Infestus Frontier V4

Minecraft 1.21.1 / NeoForge mod. Grow and evolve a living industrial colony across the Overworld, Nether and End.

| Document | Purpose |
|---|---|
| [VISION.md](VISION.md) | Product direction and permanent rules |
| [ROADMAP.md](ROADMAP.md) | Milestones, current status and player outcomes |
| [AGENTS.md](AGENTS.md) | Shared repository rules and role reading map |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Technical decisions every feature conforms to |
| [ENGINEERING.md](ENGINEERING.md) | Development and repair |
| [REVIEW.md](REVIEW.md) | Independent review and validation |
| [docs/design/](docs/design/) | Designs by topic: mining, forestry, animal farming, armor, substrate, guide tree, progression map. VISION takes precedence |

ROADMAP_OLD.md is retained for owner comparison only. LICENSE.txt and CREDITS.txt hold licensing and asset records.

```bash
bash scripts/demo.sh         # playable demo
./gradlew runClient          # own world
bash scripts/verify.sh       # full code check
```

Focused commands are in ENGINEERING.

Ktask configuration is local to each computer. `config/ktask-roles.json` records what each role reads for code, document and art tasks. After cloning on a computer that will run tasks, apply it with `python3 scripts/configure_ktask.py`; add `--refresh-pending` to update pending tasks. Neither starts a task or changes models or checks. Review lenses replace the ordinary review prompt, so give them their own reading instructions before using them.
