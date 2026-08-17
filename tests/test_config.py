import textwrap

from qqbot.config import load_config


def _write(tmp_path, yaml_text: str, env: dict):
    cfg_file = tmp_path / "config.yaml"
    cfg_file.write_text(textwrap.dedent(yaml_text), encoding="utf-8")
    return load_config(cfg_file, env)


class TestLoadConfig:
    def test_defaults(self, tmp_path):
        cfg = _write(tmp_path, "{}", {
            "QQ_APP_ID": "app1", "QQ_APP_SECRET": "sec1",
        })
        assert cfg.app_id == "app1"
        assert cfg.app_secret == "sec1"
        assert cfg.api_base_url == "https://api.sgroup.qq.com"
        assert cfg.port == 8080
        assert cfg.command_prefix == "/"
        assert cfg.whitelist_groups == []
        assert cfg.whitelist_users == []

    def test_custom_values(self, tmp_path):
        cfg = _write(tmp_path, """
            command_prefix: "!"
            whitelist:
              groups: ["G1"]
              users: ["U1"]
        """, {
            "QQ_APP_ID": "app1", "QQ_APP_SECRET": "sec1",
            "API_BASE_URL": "https://api.bot.qq.com", "PORT": "9000",
        })
        assert cfg.command_prefix == "!"
        assert cfg.whitelist_groups == ["G1"]
        assert cfg.whitelist_users == ["U1"]
        assert cfg.api_base_url == "https://api.bot.qq.com"
        assert cfg.port == 9000

    def test_missing_credentials_raise(self, tmp_path):
        import pytest
        with pytest.raises(ValueError):
            _write(tmp_path, "{}", {"QQ_APP_ID": "only-id"})
