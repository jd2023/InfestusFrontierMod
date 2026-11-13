# Infestus Frontier V4

Minecraft 1.21.1 / NeoForge mod. Grow and evolve a living industrial colony across the Overworld, Nether and End.

| Document | Purpose |
|---|---|
| [VISION.md](VISION.md) | Product direction and permanent rules |
| [ROADMAP.md](ROADMAP.md) | Milestones, current status and player outcomes |
| [AGENTS.md](AGENTS.md) | Shared repository rules and role reading map |
| [ENGINEERING.md](ENGINEERING.md) | Development and repair |
| [REVIEW.md](REVIEW.md) | Independent review and validation |
| [docs/design/](docs/design/) | Detailed designs awaiting consolidation; VISION takes precedence |
| [DOCUMENTATION_REVIEW.md](DOCUMENTATION_REVIEW.md) | Temporary consolidation report |

ROADMAP_OLD.md is retained for owner comparison only. LICENSE.txt and CREDITS.txt hold licensing and asset records.

```bash
bash scripts/demo.sh         # playable demo
./gradlew runClient          # own world
bash scripts/verify.sh       # full code check
```

Build requirements and focused commands are in ENGINEERING. For ktask on another computer, apply the versioned role instructions with `python3 scripts/configure_ktask.py`; this configures roles but does not start tasks.
