"""FastAPI 路由：/qqbot/webhook（验签 + 分发）与 /healthz。"""
import json
import logging

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from qqbot.verify import sign_validation_response, verify_push_signature

logger = logging.getLogger(__name__)

OP_CALLBACK_VALIDATION = 13
OP_HTTP_CALLBACK_ACK = 12


def create_app(app_secret: str, dispatcher) -> FastAPI:
    app = FastAPI(title="QQBot", docs_url=None, redoc_url=None)

    @app.post("/qqbot/webhook")
    async def webhook(request: Request):
        raw_body = (await request.body()).decode("utf-8", errors="replace")

        # op:13 回调地址验证：不验签，直接应答签名（官方 Go 示例行为）
        try:
            payload = json.loads(raw_body)
        except Exception:
            payload = None
        if isinstance(payload, dict) and payload.get("op") == OP_CALLBACK_VALIDATION:
            try:
                d = payload.get("d") or {}
                return sign_validation_response(
                    app_secret, d.get("plain_token", ""), str(d.get("event_ts", ""))
                )
            except Exception:
                logger.exception("op:13 respond failed")
                return JSONResponse(status_code=400, content={"error": "bad payload"})

        # 其余请求：验签
        sig = request.headers.get("X-Signature-Ed25519", "")
        ts = request.headers.get("X-Signature-Timestamp", "")
        if not verify_push_signature(app_secret, ts, raw_body, sig):
            logger.warning("signature verify failed: ts=%s", ts)
            return JSONResponse(status_code=401, content={"error": "unauthorized"})

        if not isinstance(payload, dict):
            logger.warning("non-json body after signature pass")
            return JSONResponse(status_code=400, content={"error": "bad json"})

        # 事件处理不抛异常（dispatcher 内部兜底），完成后回 ACK
        await dispatcher.handle_event(payload)
        return {"op": OP_HTTP_CALLBACK_ACK}

    @app.get("/healthz")
    async def healthz():
        return {"status": "ok"}

    return app
