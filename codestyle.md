# 后端代码规范（calculator-backend）

> 本规范参考并遵循 **[Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)**。
> 凡本文档未特别说明之处，一律以 Google Java Style Guide 为准。
> 补充约定部分参考阿里巴巴《Java 开发手册（黄山版）》。

## 1. 规范来源

- 主要来源：Google Java Style Guide — <https://google.github.io/styleguide/javaguide.html>
- 补充来源：阿里巴巴 Java 开发手册（黄山版）
- 构建期强制：Maven Compiler 编译级别 Java 17，源文件统一 UTF-8

## 2. 文件与编码

- 所有源文件使用 **UTF-8** 编码，换行符统一为 LF。
- 一个源文件只包含一个顶层 public 类，文件名与类名一致。
- 文件结构顺序：许可证/版权（如有） → package → import → 类定义。
- import 不使用通配符 `*`；按 `java` / `javax` / 第三方 / 本项目分组，组间空一行。

## 3. 命名

| 元素 | 规则 | 示例 |
| --- | --- | --- |
| 包名 | 全小写，反向域名 | `com.fzu.calculator.service` |
| 类 / 接口 / 枚举 | UpperCamelCase | `ExpressionParser`、`ErrorCode` |
| 方法 | lowerCamelCase，动词开头 | `calculate`、`deleteById` |
| 变量 / 参数 | lowerCamelCase | `rawExpression`、`divisionScale` |
| 常量 | UPPER_SNAKE_CASE，`static final` | `MAX_PAGE_SIZE`、`MAX_MAGNITUDE` |
| 数据库表 / 字段 | 小写下划线 | `calculation_history`、`created_at` |

禁止使用拼音命名、无意义缩写（如 `a1`、`tmp2`）和单字母变量（循环下标 `i` 除外）。

## 4. 格式

- 缩进 4 个空格，禁止使用 Tab。
- 单行长度不超过 100 字符。
- 左大括号不换行（K&R 风格）。
- 二元运算符两侧、逗号后、`if`/`while`/`for` 关键字与括号之间各留一个空格。
- 方法之间空一行；逻辑相关的语句块之间空一行。

## 5. 类与分层约定

- **Controller**：只做参数绑定与响应组装，不写业务逻辑，不直接访问 Repository。
- **Service**：承载业务规则，负责事务边界（`@Transactional`）。
- **Repository**：只负责数据访问，不写业务判断。
- **Entity**：只描述表结构，不掺入业务方法。
- **DTO**：统一使用 Java `record`，不可变；实体不直接暴露给前端。
- **Calculator 包**：纯计算逻辑，不依赖 Spring，保证可以被单元测试直接实例化。

依赖方向固定为 `Controller → Service → Repository`，禁止反向依赖。

## 6. 注释

- 所有 `public` 类、方法必须有 Javadoc，说明"做什么"和必要的"为什么"。
- 复杂算法（如递归下降解析）必须在类注释中写明文法。
- 行内注释解释 **为什么这么做**，不复述代码本身。
- 禁止提交被注释掉的死代码。

## 7. 异常与日志

- 可预期的业务错误统一抛 `BusinessException` + `ErrorCode`，由 `GlobalExceptionHandler` 转换响应。
- 禁止吞异常（空 `catch`）；禁止用异常做流程控制之外的事。
- 禁止 `System.out.println` 输出调试信息，统一使用 SLF4J。
- 日志不打印密码、完整连接串等敏感信息。

## 8. 安全与质量红线

- **禁止**使用 `eval`、`ScriptEngine`、`Nashorn` 等把用户输入当作程序执行的手段解析表达式。
- 所有来自前端的输入都必须经过校验（长度、格式、范围）。
- 数据库访问一律使用 JPA / 参数绑定，禁止字符串拼接 SQL。
- 大于 0 的数字运算使用 `BigDecimal`，禁止用 `double` 处理金额或需要精确结果的运算。

## 9. 提交前自检

```bash
mvn -B test          # 单元测试必须全部通过
mvn -B clean package # 打包必须成功
```

- [ ] 无编译警告与未使用 import
- [ ] 新增公共方法已补 Javadoc
- [ ] 没有把数据库密码等敏感配置提交到仓库
