"""Validate a hook's deadline against native ktask limits."""


def worker_timeout(task_id, config, policy):
    """Return one attempt's deadline, leaving ktask room for child cleanup."""
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
    return worker


def evidence_timeout(phase, task_id, config, policy):
    """Qualification shares the worker deadline; focused tests retain a shorter cap."""
    worker = worker_timeout(task_id, config, policy)
    return min(worker, 1800) if phase in ('red', 'green') else worker
