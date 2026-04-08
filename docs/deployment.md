# 部署指南

## 1. 环境要求

### 后端环境

| 软件 | 版本要求 |
|------|----------|
| JDK | 17+ |
| Maven | 3.8+ |
| MySQL/MariaDB | 10.11+ |
| Python | 3.8+ |

### 前端环境

| 软件 | 版本要求 |
|------|----------|
| Node.js | 18+ |
| npm | 9+ |

## 2. 数据库配置

### 2.1 创建数据库

```sql
CREATE DATABASE quant_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2.2 创建用户

```sql
CREATE USER 'quant'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON quant_db.* TO 'quant'@'localhost';
FLUSH PRIVILEGES;
```

## 3. 后端部署

### 3.1 安装Python依赖

```bash
pip install akshare baostock
```

### 3.2 配置应用

编辑 `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/quant_db
    username: quant
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false

server:
  port: 8080

# Python脚本路径
python:
  script-path: scripts/data_collector.py
```

### 3.3 编译打包

```bash
cd /home/gp/quant-system
mvn clean package -DskipTests
```

### 3.4 运行

```bash
java -jar target/quant-system-1.0.0.jar
```

或使用Maven运行：

```bash
mvn spring-boot:run
```

## 4. 前端部署

### 4.1 安装依赖

```bash
cd /home/gp/quant-system/frontend
npm install
```

### 4.2 开发模式运行

```bash
npm run dev
```

访问 http://localhost:3000

### 4.3 生产构建

```bash
npm run build
```

构建产物在 `dist/` 目录。

### 4.4 Nginx部署

创建Nginx配置 `/etc/nginx/conf.d/quant-system.conf`:

```nginx
server {
    listen 80;
    server_name your-domain.com;

    location / {
        root /path/to/quant-system/frontend/dist;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## 5. Docker部署

### 5.1 创建Dockerfile

后端 `Dockerfile`:

```dockerfile
FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY target/quant-system-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 5.2 Docker Compose

创建 `docker-compose.yml`:

```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: quant_db
      MYSQL_USER: quant
      MYSQL_PASSWORD: quant123
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql

  backend:
    build: .
    ports:
      - "8080:8080"
    depends_on:
      - mysql
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/quant_db
      SPRING_DATASOURCE_USERNAME: quant
      SPRING_DATASOURCE_PASSWORD: quant123

  frontend:
    image: nginx:alpine
    ports:
      - "80:80"
    volumes:
      - ./frontend/dist:/usr/share/nginx/html
      - ./nginx.conf:/etc/nginx/conf.d/default.conf
    depends_on:
      - backend

volumes:
  mysql_data:
```

### 5.3 启动

```bash
docker-compose up -d
```

## 6. 定时任务配置

### 6.1 数据采集定时任务

使用Cron表达式配置定时采集：

```java
@Scheduled(cron = "0 30 15 * * 1-5")  // 每个交易日15:30执行
public void scheduledCollection() {
    // 执行数据采集
}
```

### 6.2 信号生成定时任务

```java
@Scheduled(cron = "0 0 16 * * 1-5")  // 每个交易日16:00执行
public void scheduledSignalGeneration() {
    // 生成信号
}
```

## 7. 监控与日志

### 7.1 日志配置

`logback-spring.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/quant-system.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/quant-system.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

### 7.2 健康检查

访问 `/actuator/health` 检查服务状态。

## 8. 故障排查

### 8.1 数据采集失败

1. 检查Python环境：`python3 --version`
2. 检查依赖：`pip list | grep akshare`
3. 检查网络连接
4. 查看日志：`tail -f logs/quant-system.log`

### 8.2 数据库连接失败

1. 检查MySQL服务：`systemctl status mysql`
2. 检查连接配置
3. 检查防火墙

### 8.3 前端无法访问后端

1. 检查后端服务是否运行
2. 检查CORS配置
3. 检查代理配置