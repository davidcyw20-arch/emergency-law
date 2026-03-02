# emergency-law

VS Code friendly setup for the Spring Boot API and Vite + Vue frontend.

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
