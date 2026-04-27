# Code Analyzer

基于 Spoon 框架的代码分析工具，支持 Java 项目和 SQL 文件分析，可提取代码结构、类、方法、依赖关系等信息，并生成 Neo4j 可导入的知识图谱数据。

## 功能特性

### Java 项目分析
- **代码结构分析**：分析 Java 项目的包、类、接口、枚举、注解等结构
- **方法分析**：提取方法签名、参数、返回类型、异常等信息
- **字段分析**：提取类字段的类型、修饰符等信息
- **调用关系分析**：分析方法之间的调用关系
- **依赖关系分析**：分析类之间的依赖关系

### SQL 文件分析
- **表结构分析**：提取表名、列名、数据类型、约束等
- **视图分析**：提取视图定义和引用的表
- **外键关系**：提取表之间的外键关系
- **支持多种 SQL 方言**：MySQL、Oracle、Generic

### 通用功能
- **多格式导出**：支持导出为 Cypher 脚本、CSV、JSON 格式
- **Neo4j 直接导入**：支持直接导入到 Neo4j 数据库
- **Markdown 导出**：支持导出为 Markdown 格式供 graphify 技能使用

## 知识图谱节点类型

### Java 节点类型

| 节点类型 | 说明 |
|---------|------|
| Package | Java 包 |
| Class | Java 类 |
| Interface | Java 接口 |
| Enum | Java 枚举 |
| Annotation | Java 注解 |
| Method | Java 方法 |
| Constructor | Java 构造函数 |
| Field | Java 字段 |

### SQL 节点类型

| 节点类型 | 说明 |
|---------|------|
| Database | 数据库 |
| Schema | 模式 |
| Table | 表 |
| Column | 列 |
| View | 视图 |
| Index | 索引 |

## 知识图谱关系类型

### Java 关系类型

| 关系类型 | 说明 |
|---------|------|
| BELONGS_TO | 类/接口属于某个包 |
| SUBPACKAGE_OF | 子包关系 |
| EXTENDS | 类继承关系 |
| IMPLEMENTS | 接口实现关系 |
| DECLARES | 类声明方法 |
| HAS_CONSTRUCTOR | 类拥有构造函数 |
| HAS_FIELD | 类拥有字段 |
| HAS_PARAMETER | 方法拥有参数 |
| RETURNS | 方法返回类型 |
| THROWS | 方法抛出异常 |
| HAS_TYPE | 字段类型 |
| CALLS | 方法调用关系 |
| DEPENDS_ON | 类型依赖关系 |

### SQL 关系类型

| 关系类型 | 说明 |
|---------|------|
| HAS_SCHEMA | 数据库包含模式 |
| HAS_TABLE | 模式包含表 |
| HAS_COLUMN | 表包含列 |
| HAS_VIEW | 模式包含视图 |
| HAS_INDEX | 表有索引 |
| FOREIGN_KEY | 外键关系 |
| REFERENCES | 视图引用表 |

## 构建项目

```bash
cd spoon-test
mvn clean package
```

## 使用方法

### Java 项目分析

```bash
# 分析 Java 项目
java -jar target/spoon-test-1.0-SNAPSHOT.jar -p /path/to/java/project -o ./output

# 只输出 Cypher 脚本
java -jar target/spoon-test-1.0-SNAPSHOT.jar -p /path/to/project -f cypher

# 只输出 JSON 格式
java -jar target/spoon-test-1.0-SNAPSHOT.jar -p /path/to/project -f json

# 分析后直接导入 Neo4j
java -jar target/spoon-test-1.0-SNAPSHOT.jar -p /path/to/project --import-neo4j --neo4j-password yourpassword
```

### SQL 文件分析

```bash
# 分析单个 SQL 文件
java -jar target/spoon-test-1.0-SNAPSHOT.jar sql -p /path/to/schema.sql -d mysql -o ./output

# 分析 SQL 文件目录
java -jar target/spoon-test-1.0-SNAPSHOT.jar sql -p /path/to/sql/files/ -d oracle -o ./output

# 指定 SQL 方言 (mysql, oracle, generic)
java -jar target/spoon-test-1.0-SNAPSHOT.jar sql -p schema.sql -d mysql -f json

# 分析后直接导入 Neo4j
java -jar target/spoon-test-1.0-SNAPSHOT.jar sql -p schema.sql -d mysql --import-neo4j --neo4j-password yourpassword
```

### 命令行参数

#### Java 分析参数

| 参数 | 说明 | 默认值 |
|-----|------|-------|
| `-p, --path` | 要分析的 Java 项目路径 | - |
| `-o, --output` | 输出目录 | `./output` |
| `-f, --format` | 输出格式：cypher, csv, json, all | `all` |
| `-v, --verbose` | 详细输出模式 | false |
| `--import-neo4j` | 直接导入到 Neo4j | false |
| `--neo4j-uri` | Neo4j URI | `bolt://localhost:7687` |
| `--neo4j-user` | Neo4j 用户名 | `neo4j` |
| `--neo4j-password` | Neo4j 密码 | - |

#### SQL 分析参数

| 参数 | 说明 | 默认值 |
|-----|------|-------|
| `-p, --path` | SQL 文件或目录路径 | - |
| `-d, --dialect` | SQL 方言：mysql, oracle, generic | `generic` |
| `-o, --output` | 输出目录 | `./output` |
| `-f, --format` | 输出格式：cypher, csv, json, all | `all` |
| `-v, --verbose` | 详细输出模式 | false |
| `--import-neo4j` | 直接导入到 Neo4j | false |
| `--neo4j-uri` | Neo4j URI | `bolt://localhost:7687` |
| `--neo4j-user` | Neo4j 用户名 | `neo4j` |
| `--neo4j-password` | Neo4j 密码 | - |

## 导入 Neo4j

### 使用 Cypher 脚本

```bash
# 在 Neo4j Browser 中执行生成的 cypher 文件
:load /path/to/output/neo4j_import.cypher
```

### 使用 CSV 导入

```bash
# 使用 neo4j-admin import 工具
neo4j-admin database import full --nodes=nodes.csv --relationships=relationships.csv neo4j
```

### 使用 JSON 数据

可以通过 Neo4j 的 APOC 库或编写程序导入 JSON 数据。

## 输出示例

### 分析摘要

```
========================================
Java Project Analyzer (Spoon-based)
========================================
Project path: /path/to/project
Output directory: ./output
Output format: all

Analyzing project...
Analysis completed in 1234 ms

=== Analysis Summary ===
Project: my-project
Total nodes: 150
Total relationships: 320

Nodes by type:
  - Package: 5
  - Class: 20
  - Interface: 8
  - Method: 100
  - Field: 17

Relationships by type:
  - BELONGS_TO: 28
  - DECLARES: 100
  - CALLS: 150
  - DEPENDS_ON: 42
```

## 技术栈

- **Spoon** - Java 源代码分析框架
- **Gson** - JSON 处理
- **Picocli** - 命令行参数解析
- **JUnit 5** - 单元测试

## 许可证

MIT License
# spoon-neo4j
