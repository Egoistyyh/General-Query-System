# General-Query-System
### 一、技术选型
- 开发语言：Java
- 后端框架：Spring Boot
- 页面模板：Thymeleaf
- 数据库：H2 内存数据库
- 数据访问：JdbcTemplate
- 语音识别：浏览器 Web Speech API

### 二、系统结构

系统采用前后端协作方式：
- 前端页面负责语音录入、条件编辑和结果展示
- 后端接口负责表结构读取、文本解析、WHERE 构造和 SQL 查询

主要模块如下：
- `HomeController`：返回系统首页
- `AuthController`：提供管理员登录和退出登录
- `QueryApiController`：提供查询相关接口
- `AdminAuthService`：校验管理员账号和密码
- `AdminLoginInterceptor`：拦截未登录访问，保护查询页面和接口
- `TableMetadataService`：读取表名、列名和数据类型
- `VoiceTextParserService`：分析语音文本，抽取查询条件
- `QueryBuilderService`：构造 WHERE 子句并执行参数化 SQL

### 三、登录与访问控制设计
本系统增加了管理员登录模块

用户访问首页 `/` 前需要先登录；登录成功后，系统把管理员账号写入 Session，并允许访问查询页面和 `/api/**` 查询接口

未登录访问首页会跳转到 `/login`，未登录访问接口会返回 401

默认管理员账号配置在 `application.properties` 中：
- app.admin.username=admin
- app.admin.password=admin123

### 四、本地运行项目
power shell进入项目文件后

.\mvnw.cmd spring-boot:run

运行后可访问：
- 系统首页：http://localhost:8081
- H2 数据库控制台：http://localhost:8081/h2-console

首次访问会进入登录页，默认管理员账号如下：
- 账号：admin
- 密码：admin123
