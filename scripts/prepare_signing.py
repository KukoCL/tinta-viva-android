"""Restore the permanent signing key from Actions secrets, without logging credentials."""

import argparse
import base64
import hashlib
import os
from pathlib import Path
import subprocess


class SigningError(Exception):
    pass


def prepare(environ, destination, certificate_pin):
    encoded = environ.get("TINTA_VIVA_KEYSTORE_BASE64", "").strip()
    password = environ.get("TINTA_VIVA_KEYSTORE_PASSWORD", "")
    if not encoded and not password:
        return False
    if not encoded or not password:
        raise SigningError("Configura los dos secretos de firma; no se generará una clave nueva.")
    try:
        data = base64.b64decode(encoded, validate=True)
    except ValueError:
        raise SigningError("El secreto de la clave no contiene base64 válido.") from None
    destination = Path(destination)
    destination.parent.mkdir(parents=True, exist_ok=True)
    try:
        with os.fdopen(os.open(destination, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600), "wb") as output:
            os.chmod(destination, 0o600)
            output.write(data)
        certificate = subprocess.run(
            ["keytool", "-exportcert", "-keystore", str(destination), "-storetype", "PKCS12",
             "-alias", "tinta-viva", "-storepass:env", "TINTA_VIVA_KEYSTORE_PASSWORD"],
            env=dict(environ), capture_output=True, check=False,
        )
        if certificate.returncode:
            raise SigningError("No se pudo abrir la clave de firma con la contraseña configurada.")
        actual = hashlib.sha256(certificate.stdout).hexdigest()
        expected = Path(certificate_pin).read_text().strip().lower()
        if actual != expected:
            raise SigningError("El certificado es distinto de la firma permanente; se detiene la distribución.")
    except Exception:
        destination.unlink(missing_ok=True)
        raise
    return True


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--certificate", default="signing/certificate.sha256", type=Path)
    args = parser.parse_args()
    try:
        ready = prepare(os.environ, args.output, args.certificate)
    except SigningError as error:
        print(f"::error::{error}")
        return 1
    with open(os.environ["GITHUB_OUTPUT"], "a") as output:
        output.write(f"ready={str(ready).lower()}\n")
    if not ready:
        print("::warning::Faltan los secretos de firma permanente. Se ejecutarán los controles, pero no se publicará ningún APK.")
        return 0
    version = 1000 + int(os.environ["GITHUB_RUN_NUMBER"])
    with open(os.environ["GITHUB_ENV"], "a") as output:
        output.write(f"TINTA_VIVA_KEYSTORE_PATH={args.output.resolve()}\n")
        output.write(f"TINTA_VIVA_VERSION_CODE={version}\n")
    print("Firma permanente verificada. La clave privada no se incluye en artefactos ni cachés.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
