import pytest
from fastapi.testclient import TestClient

from qqbot.server import create_app
from qqbot.verify import signing_key_from_secret

SECRET = "DG5g3B4j9X2KOErG"


class RecordingDispatcher:
    def __init__(self):
        self.events = []

    async def handle_event(self, event):
        self.events.append(event)


def make_client():
    disp = RecordingDispatcher()
    app = create_app(app_secret=SECRET, dispatcher=disp)
    return TestClient(app), disp


def sign_body(secret: str, timestamp: str, body: str) -> str:
    key = signing_key_from_secret(secret)
    return key.sign((timestamp + body).encode()).signature.hex()


OP13_BODY = '{"d":{"plain_token":"Arq0D5A61EgUu4OxUvOp","event_ts":"1725442341"},"op":13}'


class TestCallbackValidation:
    def test_op13_returns_plain_token_and_signature(self):
        client, _ = make_client()
        resp = client.post(
            "/qqbot/webhook", content=OP13_BODY,
            headers={"X-Signature-Ed25519": "00", "X-Signature-Timestamp": "1"},
        )
        assert resp.status_code == 200
        data = resp.json()
        assert data["plain_token"] == "Arq0D5A61EgUu4OxUvOp"
        assert len(data["signature"]) == 128  # 64 字节 hex


class TestSignatureEnforcement:
    def test_missing_signature_401(self):
        client, _ = make_client()
        resp = client.post(
            "/qqbot/webhook",
            content='{"op":0,"t":"C2C_MESSAGE_CREATE","d":{}}',
        )
        assert resp.status_code == 401

    def test_bad_signature_401(self):
        client, _ = make_client()
        resp = client.post(
            "/qqbot/webhook",
            content='{"op":0,"t":"C2C_MESSAGE_CREATE","d":{}}',
            headers={"X-Signature-Ed25519": "ab" * 64, "X-Signature-Timestamp": "1"},
        )
        assert resp.status_code == 401

    def test_valid_signature_dispatches_and_acks(self):
        client, disp = make_client()
        body = '{"op":0,"id":"E9","t":"C2C_MESSAGE_CREATE","d":{"id":"M9"}}'
        ts = "1725442341"
        resp = client.post(
            "/qqbot/webhook", content=body,
            headers={
                "X-Signature-Ed25519": sign_body(SECRET, ts, body),
                "X-Signature-Timestamp": ts,
            },
        )
        assert resp.status_code == 200
        assert resp.json() == {"op": 12}
        assert disp.events[0]["id"] == "E9"


class TestHealthz:
    def test_healthz_ok(self):
        client, _ = make_client()
        assert client.get("/healthz").json() == {"status": "ok"}


class TestGarbageInput:
    def test_non_json_401(self):
        client, _ = make_client()
        resp = client.post(
            "/qqbot/webhook", content="not-json",
            headers={"X-Signature-Ed25519": "ab" * 64, "X-Signature-Timestamp": "1"},
        )
        assert resp.status_code == 401  # 验签失败先行（签名对不上垃圾体）
