# 2核4G 服务器稳态调优参考

## Java 启动参数

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
pkill -f "luoluo-admin.jar" || true
nohup java -Xms384m -Xmx1280m -jar /opt/personal-blog/backend/luoluo-admin.jar > /opt/personal-blog/logs/luoluo-admin.out 2>&1 &
```

## 2GB swap

```bash
fallocate -l 2G /swapfile
chmod 600 /swapfile
mkswap /swapfile
swapon /swapfile
grep -q '^/swapfile ' /etc/fstab || echo '/swapfile none swap sw 0 0' >> /etc/fstab
swapon --show
free -h
```

## Nginx 代理超时

参考 [personal-blog-2c4g.conf](D:/develop/Personal-Blog-RAG-Backend-Project/script/ops/nginx/personal-blog-2c4g.conf) 中的超时和上传限制配置。

## 小机型组件预算

- Java 后端：`1.25GB`
- PostgreSQL：`512MB`
- Redis：`128MB`
- RocketMQ：`384MB`
- RustFS：`256MB`

## 常用排障命令

```bash
free -h
swapon --show
vmstat 1 5
top
tail -f /opt/personal-blog/logs/luoluo-admin.out
tail -f /var/log/nginx/error.log
ss -lntp
```
