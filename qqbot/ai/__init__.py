"""AI 对话 Provider 接口（后置实现）。

将来接 GLM（zai-sdk）或 DeepSeek（openai 兼容协议）时：
新增 qqbot/ai/glm.py 或 qqbot/ai/deepseek.py 实现 ChatProvider，
在 main.py 装配处注入 dispatcher，不改动 dispatcher 与指令层。
"""
from typing import Protocol, runtime_checkable


@runtime_checkable
class ChatProvider(Protocol):
    async def reply(self, session_key: str, text: str) -> str:
        """根据会话上下文回复。session_key 形如 "group:G1" / "c2c:U1"。"""
        ...
