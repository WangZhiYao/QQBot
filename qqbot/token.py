"""AccessToken 获取与缓存：剩余寿命 <20% 时下次调用前刷新。

不缓存失败：获取异常向上抛，下次调用重试。
"""
import asyncio
import logging
import time
from typing import Awaitable, Callable

import httpx

logger = logging.getLogger(__name__)

TOKEN_URL = "https://api.bot.qq.com/app/getAppAccessToken"
REFRESH_THRESHOLD = 0.2  # 剩余寿命低于 20% 时刷新

TokenFetch = Callable[[str, str], Awaitable[dict]]


async def _http_fetch(app_id: str, client_secret: str) -> dict:
    """默认实现：真实 HTTP 调用。测试注入 fake fetch 替代。"""
    async with httpx.AsyncClient() as client:
        resp = await client.post(
            TOKEN_URL,
            json={"appId": app_id, "clientSecret": client_secret},
            timeout=10,
        )
        resp.raise_for_status()
        return resp.json()


class TokenManager:
    def __init__(self, app_id: str, client_secret: str, fetch: TokenFetch | None = None):
        self.app_id = app_id
        self.client_secret = client_secret
        self._fetch = fetch or _http_fetch
        self._token: str | None = None
        self._obtained_at: float = 0.0   # 获取时刻（monotonic）
        self._expires_at: float = 0.0    # 到期时刻（monotonic）
        self._lock = asyncio.Lock()

    async def get_token(self) -> str:
        if self._token and time.monotonic() < self._refresh_at():
            return self._token
        async with self._lock:
            # 双重检查：等锁期间可能已被其他协程刷新
            if self._token and time.monotonic() < self._refresh_at():
                return self._token
            data = await self._fetch(self.app_id, self.client_secret)
            self._token = data["access_token"]
            expires_in = float(data.get("expires_in", 7200))
            self._obtained_at = time.monotonic()
            self._expires_at = self._obtained_at + expires_in
            logger.info("access_token refreshed, expires_in=%s", expires_in)
            return self._token

    def _refresh_at(self) -> float:
        """剩余寿命跌破阈值（默认 20%）的时刻。"""
        lifetime = self._expires_at - self._obtained_at
        return self._obtained_at + lifetime * (1 - REFRESH_THRESHOLD)
