"""Admission of task execution and enclosing runner time budgets."""


def budgets(task_id, config, policy):
    """Return worker/runner seconds; reserve outer shutdown and verification time."""
    worker = policy.get('task_timeouts', {}).get(task_id, policy.get('worker_timeout', 7200))
    outer = config.get('timeout', 14400)
    verification = config.get('verification_timeout', 900)
    wait = config.get('limit_max_wait_seconds', 86400) if config.get('limit_auto_wait', True) else 0
    attempts = [config.get('max_retries', 2), config.get('max_remediation_attempts', 1),
                config.get('autoresolve_attempts', 0)]
    if (type(worker) is not int or not 60 <= worker <= 10800
            or type(outer) is not int or not worker + 60 <= outer <= 14400
            or any(type(value) is not int or not 0 <= value <= 5 for value in attempts)
            or attempts[2] > 2 or verification == 0
            or (config.get('limit_auto_wait', True) and wait == 0)
            or any(type(value) is not int or not 0 <= value <= 7200 for value in (verification, wait))):
        raise ValueError('Invalid execution budget or missing outer shutdown margin')
    retries, remediation, autoresolve = attempts
    repair_limit = autoresolve or max(1, remediation, max(0, retries - 1))
    runner = (1 + repair_limit) * (outer + verification + wait + 60) + 60
    if runner > 86400:
        raise ValueError('Enclosing runner budget exceeds24 hours')
    return worker, runner
