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
