# dongchedi · 车帝选车

> 基于 Spring Boot 的汽车筛选与推荐系统，类似懂车帝的软件形式。
> 本仓库同时是《Git 与远程仓库基础协作流程》实践作业的载体，包含完整的提交历史、分支结构与版本回滚演练。

## 一、项目简介

「车帝选车」是一个前后端一体的汽车选车系统，用户既能像逛懂车帝一样按条件筛选车型，
也可以填写预算与用车需求，让系统给出**可解释的推荐结果**（推荐理由 + 各维度得分明细）。

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

## 二、技术栈

| 层次 | 选型 |
| --- | --- |
| 运行环境 | JDK 17+（开发验证环境为 JDK 24）、Maven 3.9+ |
| 应用框架 | Spring Boot 3.5.15、Spring Framework 6.2 |
| Web / 参数校验 | Spring MVC、Validation（Jakarta Bean Validation） |
| 序列化 | Jackson |
| 认证与密码 | `spring-security-crypto`（BCrypt）+ Session 登录拦截器 |
| 数据层 | JSON 种子数据集 + 内存仓库（`CarRepository` 接口可平滑替换为 JPA / MyBatis 实现） |
| 前端 | 原生 HTML / CSS / JavaScript（无第三方依赖） |
| 测试 | JUnit 5 + AssertJ + MockMvc（52 个测试用例） |

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

更多参数与返回示例见 [docs/接口说明.md](docs/接口说明.md)。

## 五、用户登录系统

### 5.1 演示账号

| 账号 | 密码 | 角色 | 初始收藏 |
| --- | --- | --- | --- |
| `demo` | `demo123` | 普通用户 | 3 款（秦PLUS DM-i、Model Y、理想 L7） |
| `admin` | `admin123` | 管理员 | 无 |

演示账号在 `application.yml` 的 `dongchedi.auth.seed-users` 中配置，
应用启动时会先做 BCrypt 哈希再写入内存用户仓库，仓库中不保存明文密码。

### 5.2 功能

- **注册**：用户名 4~20 位字母 / 数字 / 下划线，密码 6~32 位且必须同时包含字母和数字；注册成功即自动登录。
- **登录 / 退出**：基于服务端会话（`HttpSession`），Cookie 设置 `HttpOnly` + `SameSite=Lax`，30 分钟无操作自动过期。
- **角色权限**：`USER` 管理自己的收藏；`ADMIN` 额外可访问 `/api/admin/**` 后台接口。
- **收藏**：登录后保存到账号（服务端按用户隔离）；未登录时可临时收藏到浏览器本地，并提示登录。
- **顶栏用户区**：显示昵称与角色、「我的收藏」入口与退出按钮。

### 5.3 安全设计

| 措施 | 说明 |
| --- | --- |
| 密码哈希 | BCrypt（`spring-security-crypto`），所有接口都不会返回 `passwordHash` |
| 防账号枚举 | 用户名不存在时同样执行一次哈希比对，错误提示统一为「用户名或密码错误」 |
| 防暴力破解 | 连续失败 5 次后按用户名锁定 5 分钟，可用 `dongchedi.auth.max-failed-attempts` 调整 |
| 防会话固定 | 登录成功后销毁旧 Session 并重建，再写入登录用户 id |
| 防越权访问 | 拦截器统一校验登录态与角色；收藏接口只使用会话中的用户 id，无法通过参数访问他人数据 |
| 参数校验 | `@Valid` + 服务层二次校验，错误统一返回 400 与字段级说明 |
| 错误信息隔离 | 兜底异常只写日志，对外统一返回「服务器内部错误」，不泄露实现细节 |

### 5.4 说明

用户与收藏数据保存在内存中，**应用重启后会重置**。
`UserRepository`、`FavoriteRepository` 已抽象为接口，替换为 JPA / MyBatis 实现即可持久化。

## 六、推荐算法说明

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

## 七、项目结构

```
dongchedi
├── docs                      # 文档：Git 协作流程记录、接口说明、验收报告
├── scripts                   # 仓库同步校验脚本、真实车型照片抓取脚本
├── src/main/java/com/dongchedi/car
│   ├── controller            # CarController / RecommendController / StatisticsController / SystemController
│   │                         # AuthController / FavoriteController / AdminController
│   ├── config                # 认证配置（AuthProperties / AuthConfig / WebMvcConfig / 数据初始化）
│   ├── dto                   # 请求与响应对象
│   ├── exception             # 业务异常与全局异常处理
│   ├── model                 # 领域模型与枚举
│   ├── repository            # 数据访问接口与内存实现
│   ├── security              # 登录 / 管理员拦截器
│   ├── service               # 筛选、推荐、统计业务逻辑
│   └── support               # 通用工具
├── src/main/resources
│   ├── application.yml
│   ├── data/cars.json        # 63 款车型种子数据（含 20 款零百 5 秒内的性能车）
│   └── static                # 前端页面（index.html / css / js）
└── src/test/java             # 52 个单元测试与集成测试
```

## 八、Git 协作流程

本仓库完整走了一遍「远程仓库创建 → 克隆到本地 → 分模块开发提交 → 版本回滚 → 推送与校验」的流程。

### 8.1 分支策略

- `main`：可运行的主分支，只接受功能分支合并。
- `feature/*`：每个功能模块一个分支，开发完成后用 `git merge --no-ff` 合并，保留分支合并记录。
  - `feature/car-query`：车型多条件筛选
  - `feature/recommend-engine`：智能推荐引擎
  - `feature/statistics-ranking`：统计与榜单
  - `feature/frontend-ui`：前端页面与联调
  - `feature/user-auth`：用户登录系统与收藏打通
  - `feature/richer-cars`：车型库扩充与性能车维度
- `practice/rollback-demo`：专门用于演示 `git reset --hard` 回滚的练习分支。

### 8.2 提交规范

提交信息采用 `类型: 摘要` 的约定式格式，正文用 `-` 列出要点：

| 类型 | 含义 |
| --- | --- |
| `chore` | 构建、配置、脚手架 |
| `feat` | 新功能 |
| `fix` | 缺陷修复 |
| `perf` | 性能相关改动 |
| `docs` | 文档 |
| `merge` | 分支合并 |

### 8.3 版本回滚演练

作业要求的「故意引入错误提交再回滚」在本仓库中真实发生过，见提交 `perf: 为车型筛选结果增加排序缓存`：

- 该提交加入了只以「排序方式」为 key 的排序缓存，没有区分筛选条件，第二次请求会复用第一次的结果；
- 新增的 `CarSortCacheBugTest` 立刻暴露了缺陷（期望命中 35 款，实际只有 10 款），`mvn test` 失败；
- 随后用 `git revert` 生成反向提交回滚到正确状态，`mvn test` 重新变为 32 项全绿；
- 另外在 `practice/rollback-demo` 分支上用 `git reset --hard HEAD~1` 演示了硬回滚，并用 `git reflog` 验证被丢弃的提交仍可找回。

完整命令与真实输出见 [docs/Git协作流程记录.md](docs/Git协作流程记录.md)。

### 8.4 同步校验

推送到远程仓库后，可用脚本一键校验本地与远程是否完全一致（文件、提交历史、分支结构）：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/verify-sync.ps1
```

### 8.5 联调阶段发现并修复的真实缺陷

除了作业要求「故意引入」的错误提交，前后端联调过程中还修掉了两个真实缺陷，均通过 `fix:` 提交留痕：

| 缺陷 | 现象 | 原因 | 修复 |
| --- | --- | --- | --- |
| 缺失静态资源被包装成 500 | 访问不存在的静态文件返回 500 而不是 404 | 兜底 `@ExceptionHandler(Exception.class)` 拦截了 Spring 的 `NoResourceFoundException` | 单独处理 `NoResourceFoundException`，返回 404 并补充测试 |
| 空白弹窗常驻遮挡页面 | 页面一加载就出现空白的「车型对比」弹窗和底部空浮条，挡住中间车型卡片 | 作者样式 `.modal { display: grid }`、`.compare-bar { display: flex }` 与浏览器默认的 `[hidden] { display: none }` 优先级相同但更靠后，把隐藏效果覆盖了 | 显式声明 `[hidden] { display: none !important; }`；同时给对比弹窗加「加载中 / 加载失败」提示，关闭时清空内容 |
| 顶栏登录按钮不显示 | 未登录时右上角没有「登录 / 注册」入口 | `applyFavoritesChange()` 只在已登录分支刷新用户区 | 改为无论是否登录都渲染用户区，并在初始化时先渲染一次未登录状态 |
| 错误请求体返回 500 | 登录接口收到非法 JSON 时提示「服务器内部错误」 | 兜底异常处理拦截了 `HttpMessageNotReadableException` | 单独处理并返回 400「请求体格式错误，请提交合法的 JSON」 |
| 中窄窗口顶栏换行 | 窗口宽度约 850px 时导航与「登录 / 注册」被挤成两行 | 搜索框固定 `min-width: 320px`，加上用户区后总宽超出容器 | 新增 1100px 断点收紧间距、让搜索框可伸缩，并给按钮加 `white-space: nowrap` |

## 九、验收标准对照

| 验收标准 | 完成情况 | 对应证据 |
| --- | --- | --- |
| （1）远程仓库包含完整项目文件与清晰提交历史，至少 3 次有意义的提交 | main 分支 13 个提交，其中功能提交 5 个、合并提交 4 个 | 见 Git 协作流程记录中的提交清单 |
| （2）本地展示完整版本控制流程：初始提交、功能开发提交、版本回滚 | 初始提交 `Initial commit`；4 个功能分支开发提交；`git revert` 与 `git reset --hard` 两种回滚演练 | `docs/Git协作流程记录.md` |
| （3）验证本地与远程内容完全同步（文件、分支、提交历史） | `scripts/verify-sync.ps1` 逐项比对 HEAD 哈希、文件清单、提交历史与分支列表 | `docs/验收报告.md` |
| （4）体现从克隆、本地开发、版本控制到推送的标准协作模式 | 远程仓库创建并克隆 → 分支开发 → 提交推送 → 合并 → 校验 | `docs/Git协作流程记录.md` 全篇 |

## 十、说明

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
- 仓库中的提交作者信息为 `dongchedi-dev <dongchedi-dev@example.com>`（本机未配置全局 Git 身份时的占位值），
  如需换成自己的身份，可执行：

```bash
git config user.name "你的名字"
git config user.email "你的邮箱"
```
