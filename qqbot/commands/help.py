"""/help —— 列出所有指令。"""
from qqbot.commands import CommandCtx, registry

name = "help"
description = "显示本帮助"


async def handle(ctx: CommandCtx) -> str:
    cmds = registry()
    lines = ["可用指令："]
    for name in sorted(cmds):
        lines.append(f"/{name} —— {cmds[name]['description']}")
    return "\n".join(lines)
