"""verify.py 黄金用例来自官方文档，是签名正确性的根本保证。"""
import pytest

from qqbot.verify import (
    derive_seed,
    signing_key_from_secret,
    sign_validation_response,
    verify_push_signature,
)

# ── 官方黄金用例 1：op:13 应答（event-emit 文档 DEMO）──
SECRET_1 = "DG5g3B4j9X2KOErG"
PLAIN_TOKEN = "Arq0D5A61EgUu4OxUvOp"
EVENT_TS = "1725442341"
EXPECTED_SIG_1 = (
    "87befc99c42c651b3aac0278e71ada338433ae26fcb24307bdc5ad38c1adc2d0"
    "1bcfcadc0842edac85e85205028a1132afe09280305f13aa6909ffc2d652c706"
)

# ── 推送验签（sign 文档 DEMO 的密钥；文档示例签名经核实与所示输入不配套，
#    故改用往返自洽测试。算法正确性由上方黄金用例 + 官方公布公钥字节比对保证）──
SECRET_2 = "naOC0ocQE3shWLAfffVLB1rhYPG7"
PUSH_BODY = '{ "op": 0,"d": {}, "t": "GATEWAY_EVENT_NAME"}'
PUSH_TS = "1725442341"


class TestSeedDerivation:
    def test_short_secret_repeats_to_32_bytes(self):
        # SECRET_1 长 16，重复至 32：前半+前半
        assert derive_seed(SECRET_1) == SECRET_1.encode() * 2

    def test_long_secret_truncated_to_32(self):
        assert derive_seed("x" * 40) == b"x" * 32

    def test_exactly_32_unchanged(self):
        assert derive_seed("y" * 32) == b"y" * 32


class TestSignValidation:
    def test_official_golden_vector(self):
        resp = sign_validation_response(SECRET_1, PLAIN_TOKEN, EVENT_TS)
        assert resp["plain_token"] == PLAIN_TOKEN
        assert resp["signature"] == EXPECTED_SIG_1


class TestVerifyPush:
    def test_roundtrip_accepts_own_signature(self):
        """往返自洽：同一密钥对 timestamp+body 签名，验签必须通过。"""
        from qqbot.verify import signing_key_from_secret

        key = signing_key_from_secret(SECRET_2)
        sig = key.sign((PUSH_TS + PUSH_BODY).encode("utf-8")).signature.hex()
        assert verify_push_signature(SECRET_2, PUSH_TS, PUSH_BODY, sig) is True

    def test_tampered_body_rejected(self):
        from qqbot.verify import signing_key_from_secret

        key = signing_key_from_secret(SECRET_2)
        sig = key.sign((PUSH_TS + PUSH_BODY).encode("utf-8")).signature.hex()
        assert verify_push_signature(SECRET_2, PUSH_TS, '{"op":9}', sig) is False

    def test_invalid_hex_rejected(self):
        assert verify_push_signature(SECRET_2, PUSH_TS, PUSH_BODY, "zz-not-hex") is False

    def test_wrong_length_rejected(self):
        assert verify_push_signature(SECRET_2, PUSH_TS, PUSH_BODY, "ab" * 63) is False

    def test_missing_signature_rejected(self):
        assert verify_push_signature(SECRET_2, PUSH_TS, PUSH_BODY, "") is False
