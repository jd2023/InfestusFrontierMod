"""Budget the entire worker, including nested qualification commands."""

import unittest

from ktask_timeouts import worker_timeout


class TimeoutTests(unittest.TestCase):
    def test_long_qualification_has_implementation_and_shutdown_margin(self):
        config = dict(timeout=10860, verification_timeout=3600, autoresolve_attempts=2,
                      max_retries=1, max_remediation_attempts=0, limit_max_wait_seconds=3600)
        policy = dict(worker_timeout=7200, task_timeouts={'IF-107':10800})
        worker = worker_timeout('IF-107', config, policy)
        self.assertEqual(10800, worker)
        self.assertEqual(7200, worker_timeout('IF-002', config, policy))

    def test_outer_timeout_cannot_truncate_worker(self):
        with self.assertRaises(ValueError):
            worker_timeout('IF-107', dict(timeout=7200), dict(worker_timeout=10800))

    def test_native_retry_count_does_not_expand_one_worker_attempt(self):
        config = dict(timeout=7260, verification_timeout=100, limit_max_wait_seconds=200,
                      max_retries=1, max_remediation_attempts=0, autoresolve_attempts=0)
        self.assertEqual(7200, worker_timeout('IF-001', config, {}))
        config['max_retries'] = 4
        self.assertEqual(7200, worker_timeout('IF-001', config, {}))

    def test_invalid_or_unbounded_budgets_refuse(self):
        for value in (True, 0, -1, 1000000):
            with self.subTest(value=value), self.assertRaises(ValueError):
                worker_timeout('IF-001', dict(timeout=10860), dict(worker_timeout=value))

    def test_runner_rejected_values_and_unbounded_wait_refuse(self):
        baseline = dict(timeout=10860, limit_max_wait_seconds=3600)
        for key, value in [('autoresolve_attempts', 3), ('verification_timeout', 0),
                           ('limit_max_wait_seconds', 0)]:
            with self.subTest(key=key), self.assertRaises(ValueError):
                worker_timeout('IF-001', dict(baseline, **{key: value}), {})
        with self.assertRaises(ValueError):
            worker_timeout('IF-001', dict(timeout=10860), {})
        self.assertEqual(7200, worker_timeout('IF-001', dict(timeout=10860, limit_auto_wait=False), {}))
