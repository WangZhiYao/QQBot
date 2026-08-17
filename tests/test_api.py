import httpx
import pytest

from qqbot.api import QQBotAPI

pytestmark = pytest.mark.asyncio


class FakeAsyncClient:
    """记录请求的最小 httpx.AsyncClient 替身。"""

    def __init__(self, responses=None):
        self.requests = []
        self._responses = responses or []

    def __call__(self):
        return self

    async def __aenter__(self):
        return self

    async def __aexit__(self, *exc):
        return False

    async def post(self, url, **kwargs):
        self.requests.append((url, kwargs))
        if self._responses:
            item = self._responses.pop(0)
            if isinstance(item, Exception):
                raise item
            return item
        return httpx.Response(200, json={"id": "M1"}, request=httpx.Request("POST", url))


class FakeTM:
    async def get_token(self):
        return "TOK"


def make_api(client):
    return QQBotAPI(
        base_url="https://api.sgroup.qq.com",
        token_manager=FakeTM(),
        client_factory=lambda: client,
    )


class TestSendGroupMessage:
    @pytest.mark.asyncio
    async def test_passive_reply_with_msg_id(self):
        c = FakeAsyncClient()
        api = make_api(c)
        await api.send_group_message(
            group_openid="G1", content="hi", msg_id="M0"
        )
        url, kwargs = c.requests[0]
        assert url == "https://api.sgroup.qq.com/v2/groups/G1/messages"
        assert kwargs["json"] == {
            "msg_type": 0, "content": "hi", "msg_id": "M0"
        }
        assert kwargs["headers"]["Authorization"] == "QQBot TOK"

    @pytest.mark.asyncio
    async def test_active_message_no_msg_id(self):
        c = FakeAsyncClient()
        api = make_api(c)
        await api.send_group_message(group_openid="G1", content="push")
        _, kwargs = c.requests[0]
        assert "msg_id" not in kwargs["json"]

    @pytest.mark.asyncio
    async def test_msg_seq_passed(self):
        c = FakeAsyncClient()
        api = make_api(c)
        await api.send_group_message(
            group_openid="G1", content="again", msg_id="M0", msg_seq=2
        )
        _, kwargs = c.requests[0]
        assert kwargs["json"]["msg_seq"] == 2


class TestSendC2CMessage:
    @pytest.mark.asyncio
    async def test_url_and_payload(self):
        c = FakeAsyncClient()
        api = make_api(c)
        await api.send_c2c_message(openid="U1", content="yo", msg_id="M0")
        url, kwargs = c.requests[0]
        assert url == "https://api.sgroup.qq.com/v2/users/U1/messages"
        assert kwargs["json"]["content"] == "yo"
        assert kwargs["json"]["msg_id"] == "M0"


class TestErrorHandling:
    @pytest.mark.asyncio
    async def test_non_2xx_returns_none_and_logs(self, caplog):
        c = FakeAsyncClient(
            responses=[httpx.Response(
                400,
                json={"code": 40034100, "message": "rate limited"},
                request=httpx.Request("POST", "https://x"),
            )]
        )
        api = make_api(c)
        result = await api.send_group_message(group_openid="G1", content="x")
        assert result is None

    @pytest.mark.asyncio
    async def test_network_error_returns_none(self):
        c = FakeAsyncClient(responses=[httpx.ConnectError("boom")])
        api = make_api(c)
        assert await api.send_c2c_message(openid="U1", content="x") is None
