import pytest

from qqbot.commands import registry, CommandCtx
from qqbot.commands.echo import handle as echo_handle


class TestRegistry:
    def test_auto_discovers_help_and_echo(self):
        names = set(registry().keys())
        assert {"help", "echo"} <= names

    def test_registry_entries_have_metadata(self):
        reg = registry()
        for name, cmd in reg.items():
            assert cmd["name"] == name
            assert isinstance(cmd["description"], str) and cmd["description"]
            assert callable(cmd["handle"])


class TestEcho:
    @pytest.mark.asyncio
    async def test_echoes_args(self):
        ctx = CommandCtx(args="你好 世界", raw_text="/echo 你好 世界")
        assert await echo_handle(ctx) == "你好 世界"

    @pytest.mark.asyncio
    async def test_echo_empty(self):
        ctx = CommandCtx(args="", raw_text="/echo")
        assert await echo_handle(ctx) == ""


class TestHelp:
    @pytest.mark.asyncio
    async def test_help_lists_commands(self):
        from qqbot.commands.help import handle as help_handle
        ctx = CommandCtx(args="", raw_text="/help")
        text = await help_handle(ctx)
        assert "help" in text
        assert "echo" in text
