"""入口：加载配置 → 组装各模块 → 启动 uvicorn。

用法：
    .venv\\Scripts\\python.exe main.py            # 本地
    uvicorn main:app --host 0.0.0.0 --port 8080  # 容器内（Dockerfile CMD）
"""
import logging

from dotenv import load_dotenv

from qqbot.api import QQBotAPI
from qqbot.config import load_config
from qqbot.dispatcher import Dispatcher
from qqbot.server import create_app
from qqbot.token import TokenManager

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(name)s %(message)s",
)

load_dotenv()  # 读 .env；容器内由 env_file 注入，重复调用无害
config = load_config()

token_manager = TokenManager(app_id=config.app_id, client_secret=config.app_secret)
api = QQBotAPI(base_url=config.api_base_url, token_manager=token_manager)
dispatcher = Dispatcher(api=api, config=config)  # provider=None：AI 后置
app = create_app(app_secret=config.app_secret, dispatcher=dispatcher)

if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=config.port)
