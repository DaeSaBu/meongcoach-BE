import json
import os
from pathlib import Path
import re
import subprocess
import tempfile
import unittest


SCRIPT = Path(__file__).with_name("render-task-definition.sh")


class RenderTaskDefinitionTest(unittest.TestCase):
    def render(self, secret):
        required = re.findall(r'\$\{([A-Z0-9_]+):\?', SCRIPT.read_text())
        env = {key: value for key, value in os.environ.items()
               if key != "NOTIFICATION_INTERNAL_SECRET"}
        env.update({name: "test-value" for name in required
                    if name != "NOTIFICATION_INTERNAL_SECRET"})
        if secret is not None:
            env["NOTIFICATION_INTERNAL_SECRET"] = secret
        task = {"containerDefinitions": [{
            "name": "backend",
            "image": "old-image",
            "environment": [{"name": name, "value": "existing"} for name in [
                "DB_HOST", "DB_NAME", "DB_USERNAME", "DB_PASSWORD",
                "SPRING_PROFILES_ACTIVE", "NOTIFICATION_INTERNAL_SECRET"]],
            "secrets": [{"name": "NOTIFICATION_INTERNAL_SECRET", "valueFrom": "old-arn"}],
        }]}
        with tempfile.NamedTemporaryFile(mode="w", suffix=".json") as source:
            json.dump(task, source)
            source.flush()
            return subprocess.run(
                ["bash", str(SCRIPT), source.name, "backend", "new-image"],
                env=env, capture_output=True, text=True)

    def test_missing_or_empty_notification_secret_fails_before_rendering(self):
        for secret in (None, ""):
            with self.subTest(secret=secret):
                result = self.render(secret)
                self.assertNotEqual(result.returncode, 0)
                self.assertIn("NOTIFICATION_INTERNAL_SECRET", result.stderr)
                self.assertEqual(result.stdout, "")

    def test_notification_secret_replaces_existing_environment_and_secret(self):
        secret = 'test-secret-"quoted"-value'
        result = self.render(secret)
        self.assertEqual(result.returncode, 0, result.stderr)
        target = json.loads(result.stdout)["containerDefinitions"][0]
        self.assertEqual(target["image"], "new-image")
        self.assertEqual([item for item in target["environment"]
                          if item["name"] == "NOTIFICATION_INTERNAL_SECRET"],
                         [{"name": "NOTIFICATION_INTERNAL_SECRET", "value": secret}])
        self.assertFalse(any(item["name"] == "NOTIFICATION_INTERNAL_SECRET"
                             for item in target["secrets"]))
        self.assertIn({"name": "DB_HOST", "value": "existing"}, target["environment"])


if __name__ == "__main__":
    unittest.main()
