"""配置加载：.env 环境变量（凭证）+ config.yaml（行为配置）合并为 dataclass。"""
import os
from dataclasses import dataclass, field
from pathlib import Path

import yaml


@dataclass
class Config:
    app_id: str
    app_secret: str
    api_base_url: str
    port: int
    command_prefix: str
    whitelist_groups: list[str] = field(default_factory=list)
    whitelist_users: list[str] = field(default_factory=list)


def load_config(
    yaml_path: Path | str = "config.yaml",
    env: dict[str, str] | None = None,
) -> Config:
    """env 参数供测试注入；生产走 os.environ（.env 由 python-dotenv 预先加载）。"""
    env = env if env is not None else dict(os.environ)

    app_id = env.get("QQ_APP_ID", "")
    app_secret = env.get("QQ_APP_SECRET", "")
    if not app_id or not app_secret:
        raise ValueError("QQ_APP_ID / QQ_APP_SECRET 未配置（检查 .env）")

    yaml_data: dict = {}
    path = Path(yaml_path)
    if path.exists():
        with open(path, encoding="utf-8") as f:
            yaml_data = yaml.safe_load(f) or {}

    whitelist = yaml_data.get("whitelist") or {}
    return Config(
        app_id=app_id,
        app_secret=app_secret,
        api_base_url=env.get("API_BASE_URL", "https://api.sgroup.qq.com"),
        port=int(env.get("PORT", "8080")),
        command_prefix=yaml_data.get("command_prefix", "/"),
        whitelist_groups=list(whitelist.get("groups") or []),
        whitelist_users=list(whitelist.get("users") or []),
    )
