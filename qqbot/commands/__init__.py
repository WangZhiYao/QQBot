"""指令注册：目录内自动发现。一个指令一个文件，暴露 name/description/handle。"""
import importlib
from dataclasses import dataclass
from pathlib import Path

Command = dict  # {"name": str, "description": str, "handle": async fn}


@dataclass
class CommandCtx:
    """指令执行上下文。"""
    args: str            # 指令名后的参数串（已 strip）
    raw_text: str        # 原始完整正文
    group_openid: str | None = None
    user_openid: str | None = None


def registry() -> dict[str, Command]:
    """扫描 commands/ 目录下所有模块，聚合指令。"""
    cmds: dict[str, Command] = {}
    pkg_dir = Path(__file__).parent

    for file in sorted(pkg_dir.glob("*.py")):
        if file.name.startswith("_"):
            continue
        mod = importlib.import_module(f"qqbot.commands.{file.stem}")
        cmds[mod.name] = {
            "name": mod.name,
            "description": mod.description,
            "handle": mod.handle,
        }
    return cmds
