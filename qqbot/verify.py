"""Ed25519 签名（纯函数）。

两个方向共用 seed 派生：AppSecret 重复拼接至 >=32 字节，取前 32 字节。

- op:13 回调地址验证：用私钥对 event_ts + plain_token 签名作答。
- 事件推送验签：用公钥验证 X-Signature-Ed25519 对 timestamp + raw_body 的签名。
"""
from nacl.signing import SigningKey

SEED_SIZE = 32


def derive_seed(secret: str) -> bytes:
    """AppSecret 重复拼接至 >=32 字节后截取前 32 字节。"""
    data = secret.encode("utf-8")
    while len(data) < SEED_SIZE:
        data = data + data
    return data[:SEED_SIZE]


def signing_key_from_secret(secret: str) -> SigningKey:
    return SigningKey(derive_seed(secret))


def sign_validation_response(secret: str, plain_token: str, event_ts: str) -> dict:
    """op:13 应答：对 event_ts + plain_token 签名（hex）。"""
    key = signing_key_from_secret(secret)
    signed = key.sign((event_ts + plain_token).encode("utf-8"))
    return {"plain_token": plain_token, "signature": signed.signature.hex()}


def verify_push_signature(
    secret: str, timestamp: str, raw_body: str, signature_hex: str
) -> bool:
    """验证平台推送签名：timestamp + raw_body，签名长度必须 64 字节。"""
    if not signature_hex:
        return False
    try:
        sig = bytes.fromhex(signature_hex)
    except ValueError:
        return False
    if len(sig) != 64 or (sig[63] & 0xE0) != 0:
        return False
    verify_key = signing_key_from_secret(secret).verify_key
    message = (timestamp + raw_body).encode("utf-8")
    try:
        verify_key.verify(message, sig)
        return True
    except Exception:
        return False
