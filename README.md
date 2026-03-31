# Study Room Project

这是一个前后端放在同一个仓库里的项目：

- `study room frontend`：Vue + Vite 前端
- `study-room-backend`：Spring Boot 后端

## 后端本地配置

后端真正运行时读取的是：

- `study-room-backend/src/main/resources/application.properties`

这个文件包含本机数据库配置，所以不会上传到 GitHub。

仓库里提供了一个模板文件：

- `study-room-backend/src/main/resources/application.example.properties`

换电脑时可以先复制：

```bash
cd study-room-backend
cp src/main/resources/application.example.properties src/main/resources/application.properties
```

然后再按本机环境修改下面这些值：

- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`

## 本地启动

### 前端

```bash
cd "study room frontend"
npm install
npm run dev
```

### 后端

```bash
cd study-room-backend
chmod +x mvnw
./mvnw spring-boot:run
```
