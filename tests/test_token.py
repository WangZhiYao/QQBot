import pytest

from qqbot.token import TokenManager

FAKE_NOW = 1_000_000.0


def make_manager(**kwargs):
    return TokenManager(app_id="app1", client_secret="sec1", **kwargs)


@pytest.fixture
def frozen_now(monkeypatch):
    import qqbot.token as token_mod
    state = {"now": FAKE_NOW}
    monkeypatch.setattr(token_mod.time, "monotonic", lambda: state["now"])
    return state


class TestTokenManager:
    @pytest.mark.asyncio
    async def test_fetches_and_caches(self, frozen_now):
        calls = []

        async def fake_fetch(app_id, client_secret):
            calls.append((app_id, client_secret))
            return {"access_token": "T1", "expires_in": 7200}

        mgr = make_manager(fetch=fake_fetch)
        t1 = await mgr.get_token()
        t2 = await mgr.get_token()
        assert t1 == t2 == "T1"
        assert len(calls) == 1  # 第二次命中缓存

    @pytest.mark.asyncio
    async def test_refreshes_when_below_20pct(self, frozen_now):
        async def fake_fetch(app_id, client_secret):
            return {"access_token": "T1", "expires_in": 100}

        mgr = make_manager(fetch=fake_fetch)
        await mgr.get_token()
        frozen_now["now"] += 85  # 已过 85%，剩余 15% < 20%
        assert await mgr.get_token() == "T1"
        assert mgr._token == "T1"

    @pytest.mark.asyncio
    async def test_refresh_actually_refetches(self, frozen_now):
        state = {"n": 0}

        async def fake_fetch(app_id, client_secret):
            state["n"] += 1
            return {"access_token": f"T{state['n']}", "expires_in": 100}

        mgr = make_manager(fetch=fake_fetch)
        assert await mgr.get_token() == "T1"
        frozen_now["now"] += 85
        assert await mgr.get_token() == "T2"  # 重新获取
        assert state["n"] == 2

    @pytest.mark.asyncio
    async def test_fetch_failure_propagates_and_retries_next_call(self, frozen_now):
        state = {"fail": True}

        async def fake_fetch(app_id, client_secret):
            if state["fail"]:
                raise RuntimeError("network down")
            return {"access_token": "T1", "expires_in": 7200}

        mgr = make_manager(fetch=fake_fetch)
        with pytest.raises(RuntimeError):
            await mgr.get_token()
        state["fail"] = False
        assert await mgr.get_token() == "T1"  # 失败不缓存，下次重试
