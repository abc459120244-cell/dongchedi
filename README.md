# dongchedi · 车帝选车

> 基于 Spring Boot 的汽车筛选与推荐系统，类似懂车帝的软件形式。
> 本仓库同时是《Git 与远程仓库基础协作流程》实践作业的载体，包含完整的提交历史、分支结构与版本回滚演练。

## 一、项目简介

「车帝选车」是一个前后端一体、**多端协作**的汽车选车平台：买家端可以像逛懂车帝一样筛选车型、
得到可解释的推荐结果并在线预约；商家端可以管理自己的车源、处理买家订单；
所有数据都持久化在**数据库**中，买家下单后商家端通过 **SSE 实时收到订单消息**。

系统内置 63 款、覆盖 33 个品牌的主流车型数据（含 20 款性能车），开箱即可运行，**不依赖任何外部数据库**。

主要功能：

- **多条件筛选**：品牌、级别、车身形式、能源类型、用车场景、价格区间、座位数、纯电续航、百公里油耗、口碑评分、零百加速共 11 个维度，支持同一维度多选。
- **多种排序 + 分页**：销量优先、价格升降序、口碑评分、动力、续航、零百加速最快。
- **性能车**：20 款零百 5 秒内的性能车（钢炮、性能轿车、性能旅行车、后驱跑车、超跑、性能皮卡），卡片上有「性能车」徽标，首页提供「只看性能车」快捷入口。
- **智能推荐**：按预算、场景、能源偏好、优先级（综合 / 经济性 / 动力 / 空间 / 口碑）加权打分，输出推荐理由与得分构成。
- **车型对比**：勾选 2~4 款车生成横向对比表，自动高亮更优项。
- **市场榜单**：品牌销量排行、能源类型分布、价格区间分布、用车场景分布。
- **用户系统**：注册 / 登录 / 退出、基于服务端会话的登录态、普通用户与管理员两种角色。
- **收藏**：登录后收藏保存到账号（按用户隔离），未登录时临时保存在浏览器本地。
- **商家端（多端）**：独立入口 `/merchant.html`，支持车源新增 / 修改 / 上下架 / 删除，以及订单的确认、拒绝、完成。
- **实时消息（SSE）**：买家提交预约试驾 / 询价后，对应商家端**无需刷新**即收到提醒；商家处理订单后买家同样实时收到状态变更。
- **数据库持久化**：车型、用户、收藏、订单、消息全部落库（默认 H2 文件数据库，可一键切换 MySQL）。

## 二、技术栈

| 层次 | 选型 |
| --- | --- |
| 运行环境 | JDK 17+（开发验证环境为 JDK 24）、Maven 3.9+ |
| 应用框架 | Spring Boot 3.5.15、Spring Framework 6.2 |
| Web / 参数校验 | Spring MVC、Validation（Jakarta Bean Validation） |
| 序列化 | Jackson |
| 认证与密码 | `spring-security-crypto`（BCrypt）+ Session 登录拦截器 |
| 数据层 | Spring Data JPA + Hibernate，默认 H2 文件数据库（`./data/dongchedi.mv.db`），部署时切换 MySQL 8 |
| 实时通信 | Server-Sent Events（SSE），买家与商家双向推送订单与消息 |
| 前端 | 原生 HTML / CSS / JavaScript（无第三方依赖） |
| 测试 | JUnit 5 + AssertJ + MockMvc（58 个测试用例，含多端互动流程测试） |

## 三、快速开始

### 1. 运行

```bash
# 方式一：源码直接运行
mvn spring-boot:run

# 方式二：打包后运行
mvn clean package
java -jar target/dongchedi.jar
```

启动成功后访问 <http://localhost:8080>。

首次启动会自动建表，并把 `src/main/resources/data/cars.json` 里的 63 款车型导入数据库
（控制台会打印 `DataSeeder` 的导入日志），同时创建演示账号。

- 买家端：<http://localhost:8080/>（或 `/index.html`）
- 商家端：<http://localhost:8080/merchant.html>
- 数据库控制台（仅开发环境）：<http://localhost:8080/h2-console>，JDBC URL `jdbc:h2:file:./data/dongchedi`，用户名 `sa`，密码为空

> 若 8080 端口已被占用，可指定其他端口：`java -jar target/dongchedi.jar --server.port=18080`，
> 或者在 `src/main/resources/application.yml` 中修改 `server.port`。

### 2. 运行测试

```bash
mvn test
```

### 3. 健康检查

```bash
curl http://localhost:8080/api/system/health
```

### 4. 切换 MySQL / 部署到服务器

```bash
# 本地用 MySQL 运行
java -jar target/dongchedi.jar --spring.profiles.active=mysql \
  --DB_HOST=localhost --DB_NAME=dongchedi --DB_USER=root --DB_PASSWORD=你的密码
```

部署到云服务器（含 MySQL + Nginx + 外网访问）请参考 [docs/部署指南.md](docs/部署指南.md)，
仓库内已提供 `deploy/Dockerfile`、`deploy/docker-compose.yml`、`deploy/nginx.conf`、
`deploy/dongchedi.service` 与一键脚本 `deploy/deploy-ubuntu.sh`。

## 四、接口清单

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/system/health` | 服务健康检查与版本信息 |
| GET | `/api/cars` | 多条件筛选 + 排序 + 分页 |
| GET | `/api/cars/{id}` | 单个车型详情（不存在返回 404） |
| GET | `/api/cars/filters` | 筛选项（含每个选项的车型数量、价格范围） |
| GET | `/api/cars/compare?ids=1,3,9` | 车型对比（2~4 款） |
| POST | `/api/recommend` | 智能推荐（请求体见下） |
| GET | `/api/recommend` | 快速推荐（查询参数直接调用） |
| GET | `/api/recommend/options` | 推荐表单选项与常用预算档位 |
| GET | `/api/statistics/overview` | 市场概览统计 |
| GET | `/api/statistics/brand-ranking?limit=10` | 品牌销量排行榜 |
| GET | `/api/statistics/car-ranking?limit=10&scope=ALL` | 车型销量排行榜（scope：`ALL` / `NEW_ENERGY` / `TRADITIONAL` / `PERFORMANCE`，另支持 `brand`、`bodyType` 过滤） |
| GET | `/api/statistics/scene-distribution` | 用车场景分布 |
| POST | `/api/auth/register` | 注册并自动登录 |
| POST | `/api/auth/login` | 登录（写入服务端会话） |
| POST | `/api/auth/logout` | 退出登录 |
| GET | `/api/auth/me` | 当前登录用户（未登录返回 401） |
| GET | `/api/favorites` | 我的收藏，返回完整车型（需登录） |
| GET | `/api/favorites/ids` | 我的收藏 id 列表（需登录） |
| POST | `/api/favorites/{carId}` | 收藏车型（需登录） |
| DELETE | `/api/favorites/{carId}` | 取消收藏（需登录） |
| DELETE | `/api/favorites` | 清空收藏（需登录） |
| GET | `/api/admin/users` | 用户列表（需管理员角色） |
| POST | `/api/orders` | 买家提交预约试驾 / 询价 / 意向定金（需登录） |
| GET | `/api/orders/mine` | 我的订单（需登录） |
| GET | `/api/notifications` | 我的站内消息（需登录） |
| GET | `/api/notifications/unread-count` | 未读消息数（需登录） |
| POST | `/api/notifications/{id}/read`、`/read-all` | 标记已读（需登录） |
| GET | `/api/notifications/stream` | **SSE 实时消息通道**（订单、状态变更实时推送） |
| GET | `/api/merchant/summary` | 商家经营概览（需商家角色） |
| GET / POST | `/api/merchant/cars` | 我的车源列表 / 新增车源 |
| PUT / DELETE | `/api/merchant/cars/{id}` | 修改 / 删除车源 |
| POST | `/api/merchant/cars/{id}/status` | 上架 / 下架车源 |
| GET | `/api/merchant/orders` | 收到的订单（可按状态过滤） |
| POST | `/api/merchant/orders/{id}/status` | 处理订单（确认 / 拒绝 / 完成） |

更多参数与返回示例见 [docs/接口说明.md](docs/接口说明.md)。

## 五、用户登录系统

### 5.1 演示账号

| 账号 | 密码 | 角色 | 初始收藏 |
| --- | --- | --- | --- |
| `demo` | `demo123` | 普通用户 | 3 款（秦PLUS DM-i、Model Y、理想 L7） |
| `admin` | `admin123` | 管理员 | 无 |
| `dealer_byd` | `dealer123` | 商家（比亚迪 / 仰望 / 方程豹 / 腾势，8 款车源） | 无 |
| `dealer_ev` | `dealer123` | 商家（特斯拉 / 蔚来 / 小鹏 / 理想 / 小米 / 极氪 / 问界 / 零跑 / 智己） | 无 |
| `dealer_intl` | `dealer123` | 商家（大众 / 丰田 / 本田 / 宝马 / 奔驰 / 奥迪 / 保时捷等 20 个品牌） | 无 |

演示账号在 `application.yml` 的 `dongchedi.auth.seed-users` 中配置，
应用启动时会先做 BCrypt 哈希再写入内存用户仓库，仓库中不保存明文密码。

### 5.2 功能

- **注册**：用户名 4~20 位字母 / 数字 / 下划线，密码 6~32 位且必须同时包含字母和数字；注册成功即自动登录。
- **登录 / 退出**：基于服务端会话（`HttpSession`），Cookie 设置 `HttpOnly` + `SameSite=Lax`，30 分钟无操作自动过期。
- **角色权限**：`USER` 管理自己的收藏与订单；`MERCHANT` 管理自己名下的车源与订单；`ADMIN` 可管理全部车源并访问 `/api/admin/**`。
- **收藏**：登录后保存到账号（服务端按用户隔离）；未登录时可临时收藏到浏览器本地，并提示登录。
- **顶栏用户区**：显示昵称与角色、「我的收藏」入口与退出按钮。

### 5.3 安全设计

| 措施 | 说明 |
| --- | --- |
| 密码哈希 | BCrypt（`spring-security-crypto`），所有接口都不会返回 `passwordHash` |
| 防账号枚举 | 用户名不存在时同样执行一次哈希比对，错误提示统一为「用户名或密码错误」 |
| 防暴力破解 | 连续失败 5 次后按用户名锁定 5 分钟，可用 `dongchedi.auth.max-failed-attempts` 调整 |
| 防会话固定 | 登录成功后销毁旧 Session 并重建，再写入登录用户 id |
| 防越权访问 | 拦截器统一校验登录态与角色；收藏与订单只使用会话中的用户 id，无法通过参数访问他人数据 |
| 参数校验 | `@Valid` + 服务层二次校验，错误统一返回 400 与字段级说明 |
| 错误信息隔离 | 兜底异常只写日志，对外统一返回「服务器内部错误」，不泄露实现细节 |

### 5.4 说明

用户、收藏、订单、消息全部保存在数据库中（默认 H2 文件数据库，部署时切换 MySQL），
**重启应用数据不会丢失**；需要清空演示数据时删除 `data/` 目录即可。

## 六、多端互动与数据库设计

### 6.1 多端形态

| 端 | 入口 | 主要能力 |
| --- | --- | --- |
| 买家端 | `/index.html` | 多条件筛选、智能推荐、车型对比、收藏、**预约试驾 / 询价**、我的订单与消息 |
| 商家端 | `/merchant.html` | 经营概览、**车源增删改与上下架**、**订单处理**、站内消息 |
| 管理视角 | 复用商家端（用 `admin` 登录） | 查看全部车源与全部订单 |

两套界面共用同一个后端服务与数据库，PC 与手机浏览器都做了响应式适配。

### 6.2 互动闭环（对应验收标准「买家下单，卖家收到订单消息」）

```
买家点击「预约」→ POST /api/orders            订单写入 car_order 表（状态 PENDING）
                     ↓ 同时
               notification 表写入商家消息 + SSE 推送给商家端
                     ↓
商家端订单列表自动刷新 + 顶部弹出「收到新的订单」
                     ↓
商家点击「处理」→ POST /api/merchant/orders/{id}/status
                     ↓ 更新订单状态并写商家回复
               notification 表写入买家消息 + SSE 推送给买家端
                     ↓
买家端实时收到「订单状态更新：已确认」
```

即使商家端当时没有打开页面，消息也已经落库，下次打开商家端即可在「消息」里看到未读记录。

### 6.3 数据库表结构

| 表 | 说明 | 关键字段 |
| --- | --- | --- |
| `car` | 车型 / 车源 | `id`、`name`、`brand`、`price`、`monthly_sales`、`dealer_id`（车源归属商家）、`status`（在售 / 已下架） |
| `user_account` | 用户 | `id`、`username`（唯一）、`role`（USER / MERCHANT / ADMIN）、`password_hash`（BCrypt） |
| `favorite` | 收藏 | `user_id` + `car_id` 唯一约束 |
| `car_order` | 订单 | `id`、`car_id`、`dealer_id`、`buyer_id`、`contact_phone`、`status`、`dealer_reply` |
| `notification` | 站内消息 | `id`、`user_id`、`type`、`title`、`content`、`read_flag` |

查看数据库内容：

```bash
# MySQL（Docker 部署时）
docker compose -f deploy/docker-compose.yml exec mysql \
  mysql -uroot -pdongchedi123 dongchedi -e "select id,name,brand,price,status from car limit 5;"
docker compose -f deploy/docker-compose.yml exec mysql \
  mysql -uroot -pdongchedi123 dongchedi -e "select id,car_name,buyer_name,status from car_order;"

# 本地 H2 文件数据库：直接打开 /h2-console 即可（见快速开始）
```

## 七、推荐算法说明

推荐采用「内容特征匹配 + 市场热度 + 用户口碑」的加权混合模型，整体分三步：

1. **候选集过滤**：剔除超出预算 15% 以上、或不符合能源偏好的车型。
2. **多维度打分**：8 个维度先各自归一化到 `0~1`。

| 维度 | 说明 |
| --- | --- |
| 预算匹配 | 落在预算区间内得分最高，低于预算下限说明更省钱，同样给较高分 |
| 场景匹配 | 命中用户选择的用车场景（家用代步、长途自驾、性能驾控等） |
| 能源偏好 | 燃油 / 油混 / 新能源偏好匹配 |
| 用户口碑 | 用户评分（满分 5 分） |
| 市场热度 | 月销量对数归一化 |
| 空间表现 | 座位数比例 + 轴距归一化 |
| 能耗经济 | 纯电看续航，燃油 / 混动看百公里油耗 |
| 动力性能 | 最大功率归一化（60%）+ 零百加速归一化（40%） |

3. **按优先级加权求和**：5 套权重方案（综合、经济性、动力、空间、口碑），权重之和为 100，最终得分区间 `0~100`。

结果中会返回 `reasons`（推荐理由）与 `details`（各维度得分明细），保证推荐结论可解释、可追溯。

## 八、项目结构

```
dongchedi
├── deploy                    # 部署产物：Dockerfile、docker-compose、Nginx、systemd、一键脚本
├── docs                      # 文档：部署指南、开发与提交指南、接口说明、Git 流程记录、验收报告
├── scripts                   # 仓库同步校验脚本、真实车型照片抓取脚本
├── src/main/java/com/dongchedi/car
│   ├── controller            # CarController / RecommendController / StatisticsController / SystemController
│   │                         # AuthController / FavoriteController / OrderController
│   │                         # NotificationController（SSE）/ MerchantController / AdminController
│   ├── config                # 认证配置（AuthProperties / AuthConfig / WebMvcConfig / 数据初始化）
│   ├── dto                   # 请求与响应对象
│   ├── entity                # JPA 实体：CarEntity / UserEntity / FavoriteEntity / OrderEntity / NotificationEntity
│   ├── exception             # 业务异常与全局异常处理
│   ├── model                 # 领域模型与枚举
│   ├── repository            # 数据访问接口 + JPA 实现（jpa 子包为 Spring Data 仓库）
│   ├── security              # 登录 / 商家 / 管理员拦截器
│   ├── service               # 筛选、推荐、统计、订单、消息与实时推送
│   └── support               # 通用工具
├── src/main/resources
│   ├── application.yml       # 默认 H2 文件数据库
│   ├── application-mysql.yml # MySQL 配置（部署时使用）
│   ├── data/cars.json        # 63 款车型种子数据（含 20 款零百 5 秒内的性能车）
│   └── static                # 前端页面：买家端 index.html、商家端 merchant.html
└── src/test/java             # 58 个单元测试与集成测试（含多端互动流程测试）
```

## 九、Git 协作流程

本仓库完整走了一遍「远程仓库创建 → 克隆到本地 → 分模块开发提交 → 版本回滚 → 推送与校验」的流程。

### 9.1 分支策略

- `main`：可运行的主分支，只接受功能分支合并。
- `feature/*`：每个功能模块一个分支，开发完成后用 `git merge --no-ff` 合并，保留分支合并记录。
  - `feature/car-query`：车型多条件筛选
  - `feature/recommend-engine`：智能推荐引擎
  - `feature/statistics-ranking`：统计与榜单
  - `feature/frontend-ui`：前端页面与联调
  - `feature/user-auth`：用户登录系统与收藏打通
  - `feature/richer-cars`：车型库扩充与性能车维度
- `practice/rollback-demo`：专门用于演示 `git reset --hard` 回滚的练习分支。

### 9.2 提交规范

提交信息采用 `类型: 摘要` 的约定式格式，正文用 `-` 列出要点：

| 类型 | 含义 |
| --- | --- |
| `chore` | 构建、配置、脚手架 |
| `feat` | 新功能 |
| `fix` | 缺陷修复 |
| `perf` | 性能相关改动 |
| `docs` | 文档 |
| `merge` | 分支合并 |

### 9.3 版本回滚演练

作业要求的「故意引入错误提交再回滚」在本仓库中真实发生过，见提交 `perf: 为车型筛选结果增加排序缓存`：

- 该提交加入了只以「排序方式」为 key 的排序缓存，没有区分筛选条件，第二次请求会复用第一次的结果；
- 新增的 `CarSortCacheBugTest` 立刻暴露了缺陷（期望命中 35 款，实际只有 10 款），`mvn test` 失败；
- 随后用 `git revert` 生成反向提交回滚到正确状态，`mvn test` 重新变为 32 项全绿；
- 另外在 `practice/rollback-demo` 分支上用 `git reset --hard HEAD~1` 演示了硬回滚，并用 `git reflog` 验证被丢弃的提交仍可找回。

完整命令与真实输出见 [docs/Git协作流程记录.md](docs/Git协作流程记录.md)。

### 9.4 同步校验

推送到远程仓库后，可用脚本一键校验本地与远程是否完全一致（文件、提交历史、分支结构）：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/verify-sync.ps1
```

### 9.5 联调阶段发现并修复的真实缺陷

除了作业要求「故意引入」的错误提交，前后端联调过程中还修掉了两个真实缺陷，均通过 `fix:` 提交留痕：

| 缺陷 | 现象 | 原因 | 修复 |
| --- | --- | --- | --- |
| 缺失静态资源被包装成 500 | 访问不存在的静态文件返回 500 而不是 404 | 兜底 `@ExceptionHandler(Exception.class)` 拦截了 Spring 的 `NoResourceFoundException` | 单独处理 `NoResourceFoundException`，返回 404 并补充测试 |
| 空白弹窗常驻遮挡页面 | 页面一加载就出现空白的「车型对比」弹窗和底部空浮条，挡住中间车型卡片 | 作者样式 `.modal { display: grid }`、`.compare-bar { display: flex }` 与浏览器默认的 `[hidden] { display: none }` 优先级相同但更靠后，把隐藏效果覆盖了 | 显式声明 `[hidden] { display: none !important; }`；同时给对比弹窗加「加载中 / 加载失败」提示，关闭时清空内容 |
| 顶栏登录按钮不显示 | 未登录时右上角没有「登录 / 注册」入口 | `applyFavoritesChange()` 只在已登录分支刷新用户区 | 改为无论是否登录都渲染用户区，并在初始化时先渲染一次未登录状态 |
| 错误请求体返回 500 | 登录接口收到非法 JSON 时提示「服务器内部错误」 | 兜底异常处理拦截了 `HttpMessageNotReadableException` | 单独处理并返回 400「请求体格式错误，请提交合法的 JSON」 |
| 中窄窗口顶栏换行 | 窗口宽度约 850px 时导航与「登录 / 注册」被挤成两行 | 搜索框固定 `min-width: 320px`，加上用户区后总宽超出容器 | 新增 1100px 断点收紧间距、让搜索框可伸缩，并给按钮加 `white-space: nowrap` |

## 十、验收标准对照

### 10.1 课程项目要求（前后端数据库 / 多端 / 部署）

| 老师要求 | 完成情况 | 证据 / 验证方式 |
| --- | --- | --- |
| （1）前后端 + 数据库 | ✅ Spring Boot 后端 + JPA/Hibernate 持久化，5 张业务表（`car` / `user_account` / `favorite` / `car_order` / `notification`）；默认 H2 文件库，部署切 MySQL | 商家端新增/改价/下架/删除车源，买家端列表实时变化；`docker compose exec mysql mysql -e "select ... from car"` 可查库 |
| （2）多端系统项目 | ✅ 买家端 `/index.html` + 商家端 `/merchant.html` + 管理视角，共用后端与数据库 | 两个入口分别登录 `demo` 与 `dealer_byd` |
| （3）远程仓库要有代码提交记录 | ✅ 本项目为**一人一组独立完成**：main 分支包含数十次按功能拆分的提交、多次 `feature/*` 分支 `--no-ff` 合并，以及一次真实的 bug 回滚演练，全部由本人提交 | 仓库提交历史 / 提交日历 / `git shortlog -sne`；提交规范见 [docs/开发与提交指南.md](docs/开发与提交指南.md) |
| （4）部署到服务器 | ✅ 提供 Dockerfile / docker-compose（MySQL+Nginx）/ systemd / 一键脚本与完整文档 | [docs/部署指南.md](docs/部署指南.md) |

### 10.2 验收标准

| 验收标准 | 完成情况 | 验证方式 |
| --- | --- | --- |
| （1）前端增删改数据 → 后端业务处理 → 数据库变化 | ✅ 商家端新增 / 修改 / 上下架 / 删除车源直接写 `car` 表；买家端下单写 `car_order` 表 | 操作后查询数据库，或重启应用后数据仍在 |
| （2）多端互动（买家下单，卖家收到订单消息） | ✅ 买家提交预约试驾 → 订单落库并归属车源商家 → 商家端 SSE **无需刷新**收到提醒；商家处理后买家同样实时收到消息 | 商家端与买家端各开一个浏览器（或手机+电脑）演示 |
| （3）远程仓库有代码提交记录（一人一组） | ✅ 提交历史完整连续：按功能拆分提交 + 功能分支合并 + 一次真实的 bug 回滚演练 | GitHub/Gitee 仓库的提交历史、提交日历与 Contributors |
| （4）项目可通过不同网络访问 | ✅ 提供服务器部署方案（Docker Compose + Nginx），并给出内网穿透备选 | 手机流量访问服务器公网地址 |

### 10.3 与原 Git 作业要求对照（同一仓库，兼顾两门作业）

| 验收标准 | 完成情况 | 对应证据 |
| --- | --- | --- |
| 远程仓库包含完整项目文件与清晰提交历史 | main 分支包含全部功能提交与合并记录 | `docs/Git协作流程记录.md` |
| 本地展示完整版本控制流程：初始提交、功能开发提交、版本回滚 | 初始提交 + 多个功能分支提交 + `git revert` 与 `git reset --hard` 两种回滚演练 | `docs/Git协作流程记录.md` |
| 验证本地与远程内容完全同步 | `scripts/verify-sync.ps1` 逐项比对哈希、文件清单、提交历史与分支 | `docs/验收报告.md` |
| 体现克隆 → 本地开发 → 版本控制 → 推送的协作模式 | 远程仓库创建并克隆 → 分支开发 → 提交推送 → 合并 → 校验 | `docs/Git协作流程记录.md` 全篇 |

## 十一、说明

- 车型数据为教学演示用途整理的公开信息，价格与销量均为示例值，不代表真实市场数据。
- **车型配图分两层**：
  1. 默认图片是按车身形式（轿车 / SUV / MPV / 轿跑 / 跑车 / 皮卡 / 旅行车 / 两厢车）绘制的 SVG 矢量示意图，
     位于 `src/main/resources/static/img/cars/`，版权归本项目所有，可随仓库公开分发；
  2. 需要真实车型照片时，执行下面的脚本按车型名抓取照片到 `static/img/cars/photos/`：

     ```powershell
     powershell -ExecutionPolicy Bypass -File scripts/fetch-car-photos.ps1
     ```

     照片目录已在 `.gitignore` 中忽略，**只保存在本地、不会提交到公开仓库**，
     版权归原图站所有，仅供本地个人学习演示使用；抓取时会生成 `photos-credits.json`
     记录每张照片的来源站点与原始链接。页面会自动优先使用照片，照片缺失或加载失败时回退到矢量示意图。
- 本项目由一人独立完成，仓库全部提交的作者统一为 `abc459120244-cell <abc459120244@gmail.com>`。
  在其他机器上继续开发时，记得先配置成同一身份，提交才会统计到你的 GitHub 账号下：

```bash
git config user.name "你的名字"
git config user.email "你的邮箱"
```
