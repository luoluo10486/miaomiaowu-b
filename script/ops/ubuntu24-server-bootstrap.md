# Ubuntu 24.04 Server Bootstrap

适用场景：

- 服务器：Ubuntu 24.04
- 规格：2 vCPU / 4 GiB / 50 GiB
- 当前后端主服务：`luoluo-admin`
- 当前配置依赖：PostgreSQL、Redis、RocketMQ、RustFS
- 可选中间件：Milvus、Attu

说明：

- 当前仓库主配置见 [luoluo-admin/src/main/resources/application.yml](D:/develop/Personal-Blog-RAG-Backend-Project/luoluo-admin/src/main/resources/application.yml)。
- 现在 `rag.vector.type=pg`，所以当前生产最小可用方案以 `PostgreSQL + pgvector` 为主，`Milvus` 可以先装好但不一定马上启用。
- 2C4G 机器不建议在同机部署本地大模型 `Ollama`，更适合直接走百炼或 SiliconFlow。

## 1. 首次登录初始化

```bash
sudo -i
timedatectl set-timezone Asia/Shanghai
apt update && apt -y upgrade
apt install -y curl wget git unzip vim htop net-tools ca-certificates gnupg lsb-release software-properties-common
mkdir -p /opt/personal-blog/{backend,frontend,middleware,logs}
```

## 2. 给 2C4G 机器补 swap

```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
grep -q '^/swapfile ' /etc/fstab || echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
swapon --show
free -h
```

## 3. 安装 Docker / Compose / Nginx / JDK 21 / Maven / Node

```bash
sudo apt install -y docker.io docker-compose-v2 nginx openjdk-21-jdk maven nodejs npm
sudo systemctl enable docker
sudo systemctl start docker
sudo systemctl enable nginx
sudo systemctl start nginx
java -version
mvn -version
docker --version
docker compose version
node -v
npm -v
```

如果你前端想用更高版本 Node，建议改成 `nvm` 安装：

```bash
curl -fsSL https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.7/install.sh | bash
source ~/.bashrc
nvm install 20
nvm use 20
node -v
```

## 4. 上传中间件编排文件

把下面两个文件传到服务器：

- [deploy/remote-server/docker-compose.middleware.yml](D:/develop/Personal-Blog-RAG-Backend-Project/deploy/remote-server/docker-compose.middleware.yml)
- [deploy/remote-server/rocketmq/broker.conf](D:/develop/Personal-Blog-RAG-Backend-Project/deploy/remote-server/rocketmq/broker.conf)

例如本机执行：

```bash
scp D:/develop/Personal-Blog-RAG-Backend-Project/deploy/remote-server/docker-compose.middleware.yml root@8.138.199.239:/opt/personal-blog/middleware/
scp D:/develop/Personal-Blog-RAG-Backend-Project/deploy/remote-server/rocketmq/broker.conf root@8.138.199.239:/opt/personal-blog/middleware/rocketmq/
```

然后执行：

```bash
mkdir -p /opt/personal-blog/middleware/rocketmq
mkdir -p /opt/personal-blog/middleware/volumes/{postgres,redis,rustfs,etcd,milvus}
mkdir -p /opt/personal-blog/middleware/volumes/rocketmq/{broker,nameserver}
mkdir -p /opt/personal-blog/middleware/volumes/rocketmq/broker/{logs,store}
mkdir -p /opt/personal-blog/middleware/volumes/rocketmq/nameserver/logs
cd /opt/personal-blog/middleware
```

## 5. 启动核心中间件

先编辑 `docker-compose.middleware.yml` 里的两个密码：

- `change_me_postgres`
- `change_me_redis`

然后启动：

```bash
cd /opt/personal-blog/middleware
docker compose -f docker-compose.middleware.yml up -d
docker ps
```

如果你也想把 Milvus 一起部署：

```bash
cd /opt/personal-blog/middleware
docker compose -f docker-compose.middleware.yml --profile milvus up -d
docker ps
```

## 6. 初始化 PostgreSQL 和 pgvector

```bash
docker exec -it personal-blog-postgres psql -U postgres -d ragent -c "CREATE EXTENSION IF NOT EXISTS vector;"
docker exec -it personal-blog-postgres psql -U postgres -d ragent -c "CREATE DATABASE personal_blog OWNER postgres;"
docker exec -it personal-blog-postgres psql -U postgres -d personal_blog -c "CREATE EXTENSION IF NOT EXISTS vector;"
```

说明：

- `ragent` 库给当前 RAG 主链路用。
- `personal_blog` 库可以给你后面统一业务库预留。
- 如果最终只保留一个库，也可以只建 `ragent`。

## 7. 验证中间件

```bash
docker ps
docker logs --tail=100 personal-blog-postgres
docker logs --tail=100 personal-blog-redis
docker logs --tail=100 personal-blog-rocketmq-broker
docker logs --tail=100 personal-blog-rustfs
ss -lntp | grep -E '5432|6379|9876|10911|8082|9000|9001|19530|8000'
```

访问地址：

- RocketMQ Dashboard: `http://服务器IP:8082`
- RustFS Console: `http://服务器IP:9001`
- RustFS API: `http://服务器IP:9000`
- Attu: `http://服务器IP:8000`

RustFS 默认账号密码：

- `rustfsadmin`
- `rustfsadmin`

## 8. 后端运行环境

上传 Jar 包后：

```bash
mkdir -p /opt/personal-blog/backend
mkdir -p /opt/personal-blog/logs
```

建议服务器环境变量至少配这些：

```bash
export SPRING_DATASOURCE_URL='jdbc:postgresql://127.0.0.1:5432/ragent?client_encoding=UTF8'
export SPRING_DATASOURCE_USERNAME='postgres'
export SPRING_DATASOURCE_PASSWORD='你的PostgreSQL密码'
export SPRING_DATA_REDIS_HOST='127.0.0.1'
export SPRING_DATA_REDIS_PORT='6379'
export SPRING_DATA_REDIS_PASSWORD='你的Redis密码'
export BAILIAN_API_KEY='你的百炼Key'
export SILICONFLOW_API_KEY='你的SiliconFlow Key'
```

手工启动命令：

```bash
cd /opt/personal-blog/backend
nohup java -Xms384m -Xmx1280m -jar luoluo-admin.jar > /opt/personal-blog/logs/luoluo-admin.out 2>&1 &
tail -f /opt/personal-blog/logs/luoluo-admin.out
```

如果你要用 `systemd`，可直接参考：

- [deploy/remote-server/systemd/luoluo-admin.service](D:/develop/Personal-Blog-RAG-Backend-Project/deploy/remote-server/systemd/luoluo-admin.service)

## 9. Nginx 反向代理

如果前端是静态打包产物，放到：

```bash
/opt/personal-blog/frontend/dist
```

Nginx 配置可直接参考：

- [deploy/remote-server/nginx/personal-blog.conf](D:/develop/Personal-Blog-RAG-Backend-Project/deploy/remote-server/nginx/personal-blog.conf)
- [script/ops/nginx/personal-blog-2c4g.conf](D:/develop/Personal-Blog-RAG-Backend-Project/script/ops/nginx/personal-blog-2c4g.conf)

上线命令：

```bash
cp -r /opt/personal-blog/frontend/dist/* /var/www/html/
cp /opt/personal-blog/deploy/remote-server/nginx/personal-blog.conf /etc/nginx/sites-available/personal-blog.conf
ln -sf /etc/nginx/sites-available/personal-blog.conf /etc/nginx/sites-enabled/personal-blog.conf
rm -f /etc/nginx/sites-enabled/default
nginx -t
systemctl reload nginx
```

如果你只是先代理后端接口，也可以把 `location /` 一并转发到 `9090`。

## 10. 常用排障命令

```bash
free -h
df -h
docker ps -a
docker compose -f /opt/personal-blog/middleware/docker-compose.middleware.yml ps
docker compose -f /opt/personal-blog/middleware/docker-compose.middleware.yml logs -f
systemctl status nginx
ss -lntp
tail -f /opt/personal-blog/logs/luoluo-admin.out
tail -f /var/log/nginx/error.log
```

## 11. 当前建议

第一阶段先这样配最稳：

- 装 `PostgreSQL + pgvector`
- 装 `Redis`
- 装 `RocketMQ`
- 装 `RustFS`
- Nginx 先只做反代

第二阶段再补：

- `Milvus`
- 独立前端发布
- `systemd` 守护
- HTTPS
