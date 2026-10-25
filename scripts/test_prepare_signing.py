import base64
import hashlib
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

from prepare_signing import SigningError, prepare


class SigningTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temp = tempfile.TemporaryDirectory()
        cls.root = Path(cls.temp.name)
        cls.environ = dict(os.environ, TINTA_VIVA_KEYSTORE_PASSWORD="ci-test-password-only")
        cls.key = cls.root / "fixture.p12"
        subprocess.run(
            ["keytool", "-genkeypair", "-keystore", str(cls.key), "-storetype", "PKCS12",
             "-alias", "tinta-viva", "-keyalg", "RSA", "-keysize", "2048", "-validity", "30",
             "-dname", "CN=CI fixture", "-storepass:env", "TINTA_VIVA_KEYSTORE_PASSWORD"],
            env=cls.environ, capture_output=True, check=True,
        )
        certificate = subprocess.run(
            ["keytool", "-exportcert", "-keystore", str(cls.key), "-storetype", "PKCS12",
             "-alias", "tinta-viva", "-storepass:env", "TINTA_VIVA_KEYSTORE_PASSWORD"],
            env=cls.environ, capture_output=True, check=True,
        ).stdout
        cls.pin = cls.root / "certificate.sha256"
        cls.pin.write_text(hashlib.sha256(certificate).hexdigest())
        cls.environ["TINTA_VIVA_KEYSTORE_BASE64"] = base64.b64encode(cls.key.read_bytes()).decode()

    @classmethod
    def tearDownClass(cls):
        cls.temp.cleanup()

    def test_missing_secrets_never_generate_a_replacement_key(self):
        destination = self.root / "missing.p12"
        self.assertFalse(prepare({}, destination, self.pin))
        self.assertFalse(destination.exists())

    def test_incomplete_secrets_stop_distribution(self):
        with self.assertRaises(SigningError):
            prepare({"TINTA_VIVA_KEYSTORE_PASSWORD": "partial"}, self.root / "partial.p12", self.pin)

    def test_same_private_key_is_restored_in_independent_build_directories(self):
        for name in ["build-one/key.p12", "build-two/key.p12"]:
            destination = self.root / name
            self.assertTrue(prepare(self.environ, destination, self.pin))
            self.assertEqual(self.key.read_bytes(), destination.read_bytes())
            self.assertEqual(0o600, destination.stat().st_mode & 0o777)

    def test_an_unexpected_certificate_is_rejected_and_removed(self):
        pin = self.root / "wrong.sha256"
        pin.write_text("0" * 64)
        destination = self.root / "wrong.p12"
        with self.assertRaises(SigningError):
            prepare(self.environ, destination, pin)
        self.assertFalse(destination.exists())

    def test_an_invalid_password_does_not_leave_a_signing_key_on_disk(self):
        destination = self.root / "password.p12"
        with self.assertRaises(SigningError):
            prepare(dict(self.environ, TINTA_VIVA_KEYSTORE_PASSWORD="incorrect"), destination, self.pin)
        self.assertFalse(destination.exists())


if __name__ == "__main__":
    unittest.main()
