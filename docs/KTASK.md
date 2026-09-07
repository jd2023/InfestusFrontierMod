# ktask setup

Stable context, task prompt, configuration and HUMAN gates live in `.ktask/` and
are version-controlled. Logs, queue artifacts and locks are ignored. The scaffold
uses the local framework at `/home/etf/Projects/ktask`; it is not vendored or modified.
Examples in axiotask/axiomd informed the workflow; their project-specific mandates
(such as automatic pushes or licensing choices) are not imported here.

Read-only inspection:

```bash
/home/etf/Projects/ktask/ktask status
```

No workers have been launched and no gate has been acknowledged. The queue starts
with design review and then automation/first-slice approval. The configured executor
is the runner's default, not an approved model choice or a cost recommendation.
After approval, choose models according to risk and demonstrated gate strength.

Important limitation: the inspected ktask runner can require structured reports,
issue closure and remote mainline agreement; its report verification alone does
**not independently execute our project test script**. The prompt mandates the
gate, but that is not a hard execution guarantee. Before unattended implementation,
add and test a project completion adapter that runs `.ktask/verify.sh` independently
and checks required evidence/branch state. Do not call this bootstrap a hardened
autonomous pipeline. Remote-mainline verification is off because feature-branch
work must not silently push to main.

Only queue tasks with approved outcomes, module/file boundaries, regression risks,
explicit tests, performance budgets and human approvals. Use `TASK_TEMPLATE.md`.
Product questions belong in a HUMAN gate or NEEDS_INPUT report, not remediation.
Keep bounded remediation project-local; never install host tools, troubleshoot
providers, weaken checks or invent policy to force a completion result.
