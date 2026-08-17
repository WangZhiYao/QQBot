import pytest

from qqbot.dispatcher import Dispatcher, clean_content
from qqbot.config import Config


def make_config(groups=None, users=None, prefix="/"):
    return Config(
        app_id="a", app_secret="s", api_base_url="https://x",
        port=8080, command_prefix=prefix,
        whitelist_groups=groups or [], whitelist_users=users or [],
    )


class FakeAPI:
    def __init__(self):
        self.sent = []

    async def send_group_message(self, group_openid, content, msg_id=None, msg_seq=None):
        self.sent.append(("group", group_openid, content, msg_id))

    async def send_c2c_message(self, openid, content, msg_id=None, msg_seq=None):
        self.sent.append(("c2c", openid, content, msg_id))


def make_dispatcher(api, config=None):
    return Dispatcher(api=api, config=config or make_config())


GROUP_EVENT = {
    "op": 0, "id": "E1", "t": "GROUP_AT_MESSAGE_CREATE",
    "d": {
        "id": "M0", "content": " /echo 你好",
        "group_openid": "G1",
        "author": {"member_openid": "U1"},
    },
}

C2C_EVENT = {
    "op": 0, "id": "E2", "t": "C2C_MESSAGE_CREATE",
    "d": {
        "id": "M1", "content": "在吗",
        "author": {"user_openid": "U2"},
    },
}


class TestCleanContent:
    def test_strips_at_marker(self):
        assert clean_content("<@!12345> /help", prefix="/") == "/help"

    def test_strips_leading_space(self):
        assert clean_content("  /echo hi", prefix="/") == "/echo hi"

    def test_no_marker_just_strip(self):
        assert clean_content("hello", prefix="/") == "hello"


class TestGroupMessage:
    @pytest.mark.asyncio
    async def test_command_routed_and_replied_passively(self):
        api = FakeAPI()
        d = make_dispatcher(api)
        await d.handle_event(GROUP_EVENT)
        assert api.sent == [("group", "G1", "你好", "M0")]  # 带 msg_id = 被动

    @pytest.mark.asyncio
    async def test_whitelist_blocks_unlisted_group(self):
        api = FakeAPI()
        d = make_dispatcher(api, config=make_config(groups=["OTHER"]))
        await d.handle_event(GROUP_EVENT)
        assert api.sent == []

    @pytest.mark.asyncio
    async def test_unknown_command_falls_to_ai_placeholder(self):
        api = FakeAPI()
        d = make_dispatcher(api)
        event = {
            "op": 0, "id": "E3", "t": "GROUP_AT_MESSAGE_CREATE",
            "d": {"id": "M2", "content": "/nosuch", "group_openid": "G1",
                  "author": {"member_openid": "U1"}},
        }
        await d.handle_event(event)
        assert api.sent[0][2] == "AI 功能未启用"

    @pytest.mark.asyncio
    async def test_non_command_without_provider_hints_ai(self):
        api = FakeAPI()
        d = make_dispatcher(api)
        await d.handle_event(C2C_EVENT)
        assert api.sent[0][2] == "AI 功能未启用"
        assert api.sent[0][3] == "M1"  # 被动


class TestProviderInjection:
    @pytest.mark.asyncio
    async def test_provider_receives_text_and_session(self):
        api = FakeAPI()

        class FakeProvider:
            async def reply(self, session_key, text):
                return f"[{session_key}] {text}"

        d = Dispatcher(api=api, config=make_config(), provider=FakeProvider())
        await d.handle_event(C2C_EVENT)
        assert api.sent[0][2] == "[c2c:U2] 在吗"


class TestErrorIsolation:
    @pytest.mark.asyncio
    async def test_handler_exception_does_not_propagate(self):
        api = FakeAPI()

        class ExplodingAPI(FakeAPI):
            async def send_group_message(self, *a, **k):
                raise RuntimeError("send boom")

        d = Dispatcher(api=ExplodingAPI(), config=make_config())
        await d.handle_event(GROUP_EVENT)  # 不应抛出
