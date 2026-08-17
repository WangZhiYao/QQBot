"""事件分发：解析平台事件 → 白名单 → 指令/AI → 回发。"""
import logging
import re

from qqbot.ai import ChatProvider
from qqbot.commands import CommandCtx, registry
from qqbot.config import Config

logger = logging.getLogger(__name__)

AT_MARKER = re.compile(r"^<@!\w+>\s*")
AI_DISABLED_HINT = "AI 功能未启用"


def clean_content(content: str, prefix: str) -> str:
    """剥离 @ 标记与首尾空白。"""
    text = AT_MARKER.sub("", content or "")
    return text.strip()


class Dispatcher:
    def __init__(self, api, config: Config, provider: ChatProvider | None = None):
        self.api = api
        self.config = config
        self.provider = provider

    async def handle_event(self, event: dict) -> None:
        """入口：任何处理异常都在此吞掉（server 已回 200 ACK）。"""
        try:
            await self._dispatch(event)
        except Exception:
            logger.exception("event handling failed: id=%s", event.get("id"))

    async def _dispatch(self, event: dict) -> None:
        etype = event.get("t")
        d = event.get("d") or {}
        if etype == "GROUP_AT_MESSAGE_CREATE":
            await self._handle_message(
                content=d.get("content", ""),
                msg_id=d.get("id"),
                group_openid=d.get("group_openid"),
                user_openid=(d.get("author") or {}).get("member_openid"),
            )
        elif etype == "C2C_MESSAGE_CREATE":
            await self._handle_message(
                content=d.get("content", ""),
                msg_id=d.get("id"),
                group_openid=None,
                user_openid=(d.get("author") or {}).get("user_openid"),
            )
        else:
            logger.debug("ignore event type: %s", etype)

    async def _handle_message(
        self, content: str, msg_id: str, group_openid: str | None, user_openid: str | None
    ) -> None:
        prefix = self.config.command_prefix
        text = clean_content(content, prefix)

        if not self._allowed(group_openid, user_openid):
            logger.info(
                "blocked by whitelist: group=%s user=%s", group_openid, user_openid
            )
            return

        reply: str
        session_key = f"group:{group_openid}" if group_openid else f"c2c:{user_openid}"
        if text.startswith(prefix):
            name, _, args = text[len(prefix):].partition(" ")
            cmds = registry()
            cmd = cmds.get(name)
            if cmd:
                ctx = CommandCtx(
                    args=args.strip(), raw_text=text,
                    group_openid=group_openid, user_openid=user_openid,
                )
                reply = await cmd["handle"](ctx)
                logger.info("command hit: %s", name)
            else:
                reply = await self._reply_ai(session_key, text)
        else:
            reply = await self._reply_ai(session_key, text)

        await self._send(group_openid, user_openid, reply, msg_id)

    async def _reply_ai(self, session_key: str, text: str) -> str:
        if self.provider is None:
            return AI_DISABLED_HINT
        return await self.provider.reply(session_key, text)

    def _allowed(self, group_openid: str | None, user_openid: str | None) -> bool:
        groups = self.config.whitelist_groups
        users = self.config.whitelist_users
        if group_openid is not None and groups and group_openid not in groups:
            return False
        if user_openid is not None and users and user_openid not in users:
            return False
        return True

    async def _send(self, group_openid, user_openid, content: str, msg_id: str | None):
        if not content:
            return
        if group_openid:
            result = await self.api.send_group_message(
                group_openid=group_openid, content=content, msg_id=msg_id
            )
        else:
            result = await self.api.send_c2c_message(
                openid=user_openid, content=content, msg_id=msg_id
            )
        logger.info("sent: status=%s", "ok" if result is not None else "failed")
