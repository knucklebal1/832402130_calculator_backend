# calculator-backend —— 前后端分离计算器系统（后端）

基于 **Spring Boot 3 + MySQL** 的计算器后端服务。前端只负责输入表达式与展示结果，
表达式的**校验、解析、计算、历史记录持久化**全部在本服务中完成。

配套前端仓库：`calculator-frontend`（见博客中的仓库链接）

---

## 一、技术栈

| 项目 | 版本 / 说明 |
| --- | --- |
| 语言 | Java 17 |
| 框架 | Spring Boot 3.2.0（Spring Web / Validation / Data JPA） |
| 数据库 | MySQL 8.0 |
| 构建 | Maven 3.8+ |
| 测试 | JUnit 5（44 个用例） |

**核心计算不使用任何 `eval` / `ScriptEngine`**，而是自研的词法分析器 + 递归下降解析器，
全程使用 `BigDecimal` 保证精度。文法如下：

```
expression := term (('+' | '-') term)*
term       := factor (('*' | '/') factor)*
factor     := ('+' | '-') factor | postfix
postfix    := primary ('%')*
primary    := number | '(' expression ')'
number     := digits ['.' digits]
```

其中 `%` 是**后缀百分号**，`x%` 等于 `x / 100`（用小数点移位实现，结果精确）：
`50% = 0.5`、`200*10% = 20`、`(1+2)% = 0.03`。

一元正负号由 `factor` 这一层处理，因此 `-5`、`3*-2`、`-(2+3)*2` 都是合法表达式。

---

## 二、运行环境

- JDK 17 及以上（`java -version` 应为 17+）
- Maven 3.8 及以上
- MySQL 8.0 及以上（本地或远程均可）

---

## 三、快速开始

### 1. 初始化数据库

```bash
mysql -u root -p --default-character-set=utf8mb4 < src/main/resources/db/schema.sql
```

脚本会创建 `calculator_db` 数据库和 `calculation_history` 表。
应用默认 `ddl-auto=update`，即使跳过这一步，首次启动也会自动建表。

> Windows 下建议带上 `--default-character-set=utf8mb4`（脚本内也已加 `SET NAMES utf8mb4`），
> 否则控制台默认的 GBK 编码会让建表语句里的中文注释变成乱码。

### 2. 配置数据库连接

修改 `src/main/resources/application.yml`，或（推荐）用环境变量覆盖，避免把密码提交到仓库：

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | 服务端口 |
| `DB_URL` | `jdbc:mysql://localhost:3306/calculator_db?...` | JDBC 连接串 |
| `DB_USER` | `root` | 数据库用户名 |
| `DB_PASSWORD` | `123456` | 数据库密码 |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://127.0.0.1:5173` | 允许跨域的前端地址 |

Windows PowerShell 示例：

```powershell
$env:DB_PASSWORD="你的密码"
```

Linux / macOS 示例：

```bash
export DB_PASSWORD='你的密码'
```

### 3. 启动服务

```bash
# 开发模式
mvn spring-boot:run

# 或打包后运行
mvn clean package -DskipTests
java -jar target/calculator-backend.jar
```

启动成功后服务地址为 <http://localhost:8080>。

### 4. 运行测试

```bash
mvn -B test
```

---

## 四、API 说明

所有接口以 `/api` 为前缀，请求与响应均为 JSON。
错误响应统一为 `{"success":false,"code":"...","message":"..."}`。

### 4.1 计算

`POST /api/calculate`

```json
{ "expression": "(1+2)*3" }
```

成功响应 `200`：

```json
{
  "success": true,
  "id": 4,
  "expression": "(1+2)*3",
  "result": "9",
  "createdAt": "2026-10-05 10:22:00"
}
```

> `result` 以**字符串**返回，避免 JavaScript 处理大数时丢失精度。

失败响应 `400`：

```json
{ "success": false, "code": "DIVIDE_BY_ZERO", "message": "Division by zero" }
```

### 4.2 查询历史

`GET /api/history?page=1&size=20&keyword=1%2B2`

| 参数 | 默认值 | 说明 |
| --- | --- | --- |
| `page` | 1 | 页码，从 1 开始 |
| `size` | 20 | 每页条数，最大 100 |
| `keyword` | 空 | 表达式关键字，模糊匹配 |

```json
{
  "success": true,
  "total": 3,
  "page": 1,
  "size": 20,
  "pages": 1,
  "list": [
    { "id": 3, "expression": "(2+3)*4", "result": "20", "createdAt": "2026-10-05 10:22:00" }
  ]
}
```

### 4.3 删除单条历史

`DELETE /api/history/{id}` → 成功返回 `204 No Content`；
记录不存在返回 `404 {"success":false,"code":"HISTORY_NOT_FOUND","message":"..."}`。

### 4.4 清空历史（加分项）

`DELETE /api/history` → `200 {"success":true,"deleted":3}`

### 4.5 计算统计（加分项）

`GET /api/history/stats`

```json
{
  "success": true,
  "total": 12,
  "topOperator": "+",
  "topOperatorCount": 7,
  "averageResult": "15.250000",
  "latestAt": "2026-10-05 10:22:00"
}
```

### 4.6 错误码

| 错误码 | HTTP | 触发场景 |
| --- | --- | --- |
| `EXPRESSION_EMPTY` | 400 | 表达式为空 |
| `EXPRESSION_TOO_LONG` | 400 | 表达式超过 200 字符 |
| `UNSUPPORTED_CHARACTER` | 400 | 含非法字符，如 `1+abc` |
| `INVALID_EXPRESSION` | 400 | 语法错误，如 `1+`、`(1+2`、`(1+2)3` |
| `DIVIDE_BY_ZERO` | 400 | 除数为 0，如 `10/0`、`5/(3-3)` |
| `NUMBER_OUT_OF_RANGE` | 400 | 结果绝对值 ≥ 1E28 |
| `INVALID_REQUEST` | 400 | 请求体不是合法 JSON |
| `HISTORY_NOT_FOUND` | 404 | 删除不存在的记录 |
| `INTERNAL_ERROR` | 500 | 服务端未预期异常 |

---

## 五、目录结构

```
calculator-backend/
├── src/main/java/com/fzu/calculator/
│   ├── CalculatorApplication.java
│   ├── calculator/        # 词法分析、递归下降解析、BigDecimal 工具（纯逻辑，不依赖 Spring）
│   ├── config/            # 业务参数、跨域配置
│   ├── controller/        # REST 接口层
│   ├── exception/         # 错误码、业务异常、全局异常处理
│   ├── model/
│   │   ├── dto/           # 请求 / 响应对象（record）
│   │   └── entity/        # JPA 实体
│   ├── repository/        # 数据访问层
│   ├── service/           # 业务逻辑层
│   └── util/              # 通用工具
├── src/main/resources/
│   ├── application.yml
│   └── db/schema.sql      # 数据库初始化脚本
├── src/test/java/         # JUnit 单元测试
├── codestyle.md
└── README.md
```

---

## 六、数据库设计

```sql
CREATE TABLE calculation_history (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  expression  VARCHAR(255)    NOT NULL                COMMENT '计算表达式',
  result      DECIMAL(38,10)  NOT NULL                COMMENT '计算结果',
  created_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '计算时间',
  PRIMARY KEY (id),
  KEY idx_history_created_at (created_at DESC),
  KEY idx_history_expression (expression(64))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
```

**为什么 `result` 用 `DECIMAL` 而不是 `DOUBLE`？**
`DOUBLE` 是二进制浮点数，`0.1 + 0.2` 会得到 `0.30000000000000004`。
计算器必须精确，因此后端全程使用 `BigDecimal`，数据库使用 `DECIMAL(38,10)`。

---

## 七、与前端对接

1. 启动本服务（默认 8080 端口）。
2. 前端在 `.env.development` 中配置 `VITE_API_BASE_URL=http://localhost:8080`，并通过
   Vite 代理或后端 CORS 打通跨域。
3. 前端调用 `POST /api/calculate` 发送表达式，展示后端返回的 `result`。
4. 历史记录通过 `GET /api/history` 从数据库读取，`DELETE /api/history/{id}` 删除。

**自检方法**：停止本服务后，前端仍能进行界面交互，但无法得到任何新的计算结果——
这说明核心计算确实在后端完成，符合作业对"前后端分离"的要求。

---

## 八、部署

```bash
# 1. 打包
mvn clean package -DskipTests

# 2. 在服务器上以环境变量方式运行
export DB_URL='jdbc:mysql://127.0.0.1:3306/calculator_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true'
export DB_USER='calculator'
export DB_PASSWORD='********'
java -jar target/calculator-backend.jar
```

生产环境建议用 Nginx 把前端静态资源和 `/api` 反向代理到同一域名下，
此时前后端同源，可以不再配置 CORS。
