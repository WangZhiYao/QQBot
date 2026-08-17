"""QQ OpenAPI 封装：发群消息 / 发 C2C 消息。

msg_id 传 → 被动回复；不传 → 主动消息（频控：群 1000 条/天）。
非 2xx 或网络异常：记日志，返回 None（不向调用方抛）。
"""
import logging
from typing import Callable, Optional

import httpx

logger = logging.getLogger(__name__)

# 需要在日志中显式识别的错误码
KNOWN_ERROR_CODES = {
    40034100: "主动消息超过频控限制",
    40034005: "回复 msg_id 已过期（5 分钟/60 分钟窗口）",
}


class QQBotAPI:
    def __init__(
        self,
        base_url: str,
        token_manager,
        client_factory: Callable[[], httpx.AsyncClient] | None = None,
    ):
        self.base_url = base_url.rstrip("/")
        self.token_manager = token_manager
        self._client_factory = client_factory or httpx.AsyncClient

    async def send_group_message(
        self,
        group_openid: str,
        content: str,
        msg_id: Optional[str] = None,
        msg_seq: Optional[int] = None,
    ) -> Optional[dict]:
        """发群消息。msg_id 不传 = 主动消息。"""
        return await self._send(
            f"/v2/groups/{group_openid}/messages", content, msg_id, msg_seq
        )

    async def send_c2c_message(
        self,
        openid: str,
        content: str,
        msg_id: Optional[str] = None,
        msg_seq: Optional[int] = None,
    ) -> Optional[dict]:
        """发 C2C 私聊消息。msg_id 不传 = 主动消息。"""
        return await self._send(
            f"/v2/users/{openid}/messages", content, msg_id, msg_seq
        )

    async def _send(self, path: str, content: str, msg_id, msg_seq) -> Optional[dict]:
        payload: dict = {"msg_type": 0, "content": content}
        if msg_id:
            payload["msg_id"] = msg_id
        if msg_seq is not None:
            payload["msg_seq"] = msg_seq
        token = await self.token_manager.get_token()
        headers = {"Authorization": f"QQBot {token}"}
        try:
            async with self._client_factory() as client:
                resp = await client.post(
                    self.base_url + path, json=payload, headers=headers, timeout=10
                )
            if resp.status_code >= 400:
                body = resp.text
                code = None
                try:
                    code = resp.json().get("code")
                except Exception:
                    pass
                hint = KNOWN_ERROR_CODES.get(code, "")
                logger.error(
                    "send failed: %s status=%s code=%s %s body=%s",
                    path, resp.status_code, code, hint, body,
                )
                return None
            return resp.json()
        except httpx.HTTPError as e:
            logger.error("send network error: %s %s", path, e)
            return None
