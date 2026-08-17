"""/echo <text> —— 原样回复。"""
from qqbot.commands import CommandCtx

name = "echo"
description = "复读一遍你发的话：/echo <内容>"


async def handle(ctx: CommandCtx) -> str:
    return ctx.args
