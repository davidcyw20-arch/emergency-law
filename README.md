# emergency-law

VS Code friendly setup for the Spring Boot API and Vite + Vue frontend.

## 项目说明书

- 完整说明书见：`docs/系统说明书.md`

## 详细环境配置与启动方式（推荐按本节执行）

本项目是 **前后端分离**：
- 后端：Spring Boot（默认 `8080`）
- 前端：Vite + Vue（默认 `5173`）

---

### 1. 环境准备

#### 1.1 必备软件版本
- JDK：`1.8+`（推荐 8/11/17 其一，团队内保持一致）
- Maven：`3.8+`
- Node.js：`18+`
- npm：`9+`
- MySQL：`8.x`（5.7 通常也可，但推荐 8.x）

可用以下命令检查：

```bash
java -version
mvn -v
node -v
npm -v
```

#### 1.2 端口约定
- 后端：`8080`
- 前端：`5173`
- MySQL：`3306`

请确保上述端口未被占用。

---

### 2. 数据库配置

编辑后端配置文件：`src/main/resources/application.yml`

当前关键项：
- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`

示例（按你本地环境替换）：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/emergency_law?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
    username: law_user
    password: 123456
    driver-class-name: com.mysql.cj.jdbc.Driver
```

#### 2.1 数据库初始化建议
1. 先创建数据库：`emergency_law`
2. 确认业务账号（如 `law_user`）对该库有读写权限
3. 若是远程数据库，需放通服务器到 DB 的网络访问（安全组 / 防火墙）

#### 2.2 一键连通性检查（可选但推荐）

```bash
bash scripts/check-db-connection.sh
```

---

### 3. 后端启动（Spring Boot）

> **必须在仓库根目录（含 `pom.xml`）执行**。

#### 3.1 安装依赖并测试

```bash
mvn clean test
```

#### 3.2 启动后端

```bash
mvn spring-boot:run
```

启动成功后可验证：

```text
http://localhost:8080/health
```

看到 `OK` / `DB Connected` 说明后端和数据库链路正常。

---

### 4. 前端启动（Vite）

> 在 `emergency-law-web` 目录执行。

#### 4.1 安装依赖

```bash
cd emergency-law-web
npm install
```

#### 4.2 启动开发环境

```bash
npm run dev -- --host
```

启动后访问：

```text
http://localhost:5173
```

说明：前端通过 Vite 代理访问后端 `/api`，所以 **后端 8080 必须先启动**，否则会出现 `ECONNREFUSED / proxy error / Network Error`。

---

### 5. 本地视频上传/播放相关配置（你当前场景重点）

#### 5.1 上传限制
后端已在 `application.yml` 配置：
- `spring.servlet.multipart.max-file-size: 200MB`
- `spring.servlet.multipart.max-request-size: 220MB`

如仍上传失败，可按实际情况继续增大。

#### 5.2 上传目录与静态访问
- 本地视频上传后保存在：`data/uploads/...`
- 通过后端静态资源映射访问：`/uploads/**`

因此，用户端播放上传视频时需要后端在线并可访问 `http://<后端地址>:8080/uploads/...`。

#### 5.3 常见问题排查
1. 管理端上传 `Network Error`：先确认后端 8080 已启动
2. 上传成功但用户端不播放：检查浏览器网络面板中视频 URL 是否可直接访问
3. `PacketTooBigException`：不要把视频内容存数据库，使用“先上传文件再保存 URL”的方式（当前实现即此方式）

---

### 6. 推荐启动顺序（避免 90% 本地问题）

1. 启动 MySQL 并确认库、账号可用
2. 在仓库根目录启动后端（`mvn spring-boot:run`）
3. 访问 `http://localhost:8080/health` 确认后端健康
4. 启动前端（`cd emergency-law-web && npm run dev -- --host`）
5. 打开 `http://localhost:5173`

---

### 7. 打包构建（可选）

#### 7.1 前端打包

```bash
cd emergency-law-web
npm run build
```

#### 7.2 后端打包

```bash
cd ..
mvn clean package -DskipTests
```

---

### 8. Windows 用户提示
- 不要在 `emergency-law-web` 目录执行 `mvn spring-boot:run`（该目录没有后端 `pom.xml`）
- 若 PowerShell 执行脚本受限，可改用 Git Bash 或先调整执行策略
- 路径中有空格时，命令参数建议加引号

## Quick start
- Requirements: Java 8+, Maven, Node.js 18+ with npm.
- Open `emergency-law.code-workspace` in VS Code (File → Open Workspace).
- Install recommended extensions if prompted (Java pack, Spring Boot dashboard, Volar).

### Tasks (Terminal → Run Task)
- `frontend: install` (run once): installs web dependencies in `emergency-law-web`.
- `backend: run (Spring Boot)`: starts the API on http://localhost:8080.
- `frontend: dev`: starts Vite on http://localhost:5173 (background).
- `backend: tests` / `frontend: build`: optional checks/builds.

### Debug/Run (Run and Debug panel)
- `Launch Backend (Spring Boot)`: debug the API (`com.yunxian.emergencylaw.EmergencyLawApplication`).
- `Frontend in Chrome (Vite)`: launches Chrome pointing at the dev server (auto-runs `frontend: dev`).
- `Full Stack: backend + frontend`: starts both debug sessions together.

Notes:
- The workspace-level `.vscode` folder holds shared tasks, launch configs, and extension recommendations.
- Adjust DB credentials and `SPRING_PROFILES_ACTIVE` in the launch config if you use non-default environments.


## 登录提示“密码错误”，但怀疑是“连不上数据”的排查
当登录页一直提示“密码错误”时，不一定是账号密码问题，也可能是后端连不上数据库导致认证流程失败。可按下面的“先快后细”流程处理。

### 1) 先做 30 秒快速判断
- 访问健康检查：`http://localhost:8080/health`
  - 返回 `OK ✅ DB Connected: ...`：后端与数据库已连通，优先检查账号状态/登录逻辑。
  - 无法访问或报 5xx：优先排查后端启动、数据库连接、网络连通。

### 2) 后端与数据库连通性检查（必做）
1. 核对配置：`src/main/resources/application.yml` 中 `spring.datasource.url/username/password` 是否正确。
2. 确认数据库服务在线（例如 MySQL 默认端口 `3306`）。
3. 从后端机器测试端口可达：
   - Linux: `nc -vz <db_host> 3306`
   - Windows PowerShell: `Test-NetConnection <db_host> -Port 3306`
4. 数据库不在本机时，检查安全组/防火墙/路由/DNS 解析。

### 一键检查（本仓库）
可直接运行：

```bash
bash scripts/check-db-connection.sh
```

该脚本会读取 `src/main/resources/application.yml` 的 MySQL URL 并测试端口连通性。

### 3) 日志定位：根据报错快速归类
- `Communications link failure` / `Connection timed out`：网络不通或被防火墙拦截。
- `Connection refused`：目标端口未监听（数据库服务未启动或端口写错）。
- `Unknown host`：DNS 或主机名配置错误。
- `Access denied for user`：用户名/密码错误，或账号无权限。

### 4) 排除前端干扰（建议）
- 用 `test.http` 或 Postman 直接调用登录接口。
- 若接口直连也失败，问题在后端/数据库链路；若接口成功而前端失败，再查前端请求参数或拦截器。

### 5) 常见“看起来像密码错”的根因
- 部署环境变量覆盖了 `application.yml`（例如生产机注入了旧密码）。
- 数据库白名单未放行当前服务器 IP。
- 新环境未同步数据库账号权限（只有本地权限，没有远程权限）。
- 使用了错误 profile（如 `prod` 配置连到了不可达数据库）。

> 建议：将 `/health` 作为部署后第一检查项；先确认“后端 + DB 已连通”，再分析登录业务逻辑。


## 常见启动错误：`No plugin found for prefix 'spring-boot'`
如果你在 **`emergency-law-web`** 目录执行了：

```bash
mvn spring-boot:run
```

会报这个错，因为前端目录是 Vite 项目（`package.json`），**没有后端 `pom.xml`**。

### 正确启动方式
1. 回到后端项目根目录（与 `pom.xml` 同级）：
   ```bash
   cd ..
   mvn spring-boot:run
   ```
2. 或直接在仓库根目录执行：
   ```bash
   mvn spring-boot:run
   ```
3. 前端仍在 `emergency-law-web` 目录启动：
   ```bash
   npm run dev -- --host
   ```

### 若仍提示插件找不到（镜像仓库问题）
你的日志里使用的是 `http://maven.aliyun.com/...`。建议改为 HTTPS 新地址或 Maven Central：

- 阿里云（推荐）：`https://maven.aliyun.com/repository/public`
- Maven Central：`https://repo.maven.apache.org/maven2`

> 先确保“命令执行目录正确”，再排查镜像配置。
