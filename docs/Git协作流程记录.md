# Git 与远程仓库基础协作流程 · 实践记录

本文档记录本次实践作业中真实执行的命令与输出（哈希值均来自本仓库，可直接用 `git log` 核对）。

## 一、环境准备

```bash
git --version        # git version 2.53.0.windows.3
java -version        # openjdk 24.0.2
mvn -v               # Apache Maven 3.9.15
```

配置仓库级身份（避免污染全局配置，也方便多人协作时区分提交作者）：

```bash
git config user.name  "dongchedi-dev"
git config user.email "dongchedi-dev@example.com"
git config core.autocrlf false      # 与 .gitattributes 配合，统一使用 LF
git config core.quotepath false     # 让中文文件名正常显示
git config i18n.commitEncoding utf-8
git config i18n.logOutputEncoding utf-8
```

## 二、创建远程仓库并克隆到本地

在 Gitee / GitHub 上创建公开仓库时的关键设置：

1. 仓库名称与项目名保持一致：`dongchedi`；
2. 可见性选择 **公开（Public）**；
3. 初始化时**勾选「添加 README 文件」**，这样远程仓库会自带一个初始提交。

平台上的初始提交内容就是本仓库的 `Initial commit`（`3989748`），克隆到本地后的标准命令是：

```bash
git clone https://gitee.com/<你的用户名>/dongchedi.git
cd dongchedi
git log --oneline
# 3989748 Initial commit
```

> 说明：本次实践是在已经初始化好的本地目录中完成的，本地仓库与远程仓库的关联通过下面的命令建立，
> 效果与「先建远程仓库再克隆」完全一致：
>
> ```bash
> git init -b main
> git remote add origin https://gitee.com/<你的用户名>/dongchedi.git
> git branch --set-upstream-to=origin/main main
> ```

## 三、本地开发与提交历史

开发按功能模块拆分，每个模块一个 `feature/*` 分支，完成后用 `git merge --no-ff` 合并回 `main`，
保留清晰的分支合并记录。

| 序号 | 提交 | 信息 | 说明 |
| --- | --- | --- | --- |
| 1 | `3989748` | `Initial commit` | 远程仓库创建时自动生成的 README |
| 2 | `abe9aa5` | `chore: 初始化 Spring Boot 项目骨架与基础配置` | pom、启动类、配置、.gitignore |
| 3 | `3e56599` | `feat: 新增汽车领域模型与内存数据仓库` | 枚举、`Car` 模型、仓库、35 款车型数据集 |
| 4 | `c451d05` | `feat: 实现汽车多条件筛选查询接口` | 分支 `feature/car-query` |
| 5 | `b9987ab` | `merge: 合并车型筛选查询功能到 main` | `--no-ff` 合并 |
| 6 | `abc93cc` | `feat: 实现基于多维度加权的智能推荐引擎` | 分支 `feature/recommend-engine` |
| 7 | `73aa572` | `merge: 合并智能推荐引擎到 main` | `--no-ff` 合并 |
| 8 | `6a77172` | `feat: 新增市场统计与品牌销量排行榜` | 分支 `feature/statistics-ranking` |
| 9 | `b89ece6` | `merge: 合并统计与排行模块到 main` | `--no-ff` 合并 |
| 10 | `92831d7` | `feat: 新增懂车帝风格前端页面与前后端联调` | 分支 `feature/frontend-ui` |
| 11 | `1c1f2aa` | `merge: 合并前端页面到 main` | `--no-ff` 合并 |
| 12 | `7da31d6` | `perf: 为车型筛选结果增加排序缓存` | **故意引入的错误提交** |
| 13 | `41a99ac` | `Revert "perf: 为车型筛选结果增加排序缓存"` | **回滚提交** |

分支结构（`git log --all --graph --decorate --oneline` 的真实输出）：

```
* 83b72c4 (practice/rollback-demo) practice: 添加版本回滚练习说明
* 41a99ac (HEAD -> main) Revert "perf: 为车型筛选结果增加排序缓存"
* 7da31d6 perf: 为车型筛选结果增加排序缓存
*   1c1f2aa merge: 合并前端页面到 main
|\
| * 92831d7 (feature/frontend-ui) feat: 新增懂车帝风格前端页面与前后端联调
|/
*   b89ece6 merge: 合并统计与排行模块到 main
|\
| * 6a77172 (feature/statistics-ranking) feat: 新增市场统计与品牌销量排行榜
|/
*   73aa572 merge: 合并智能推荐引擎到 main
|\
| * abc93cc (feature/recommend-engine) feat: 实现基于多维度加权的智能推荐引擎
|/
*   b9987ab merge: 合并车型筛选查询功能到 main
|\
| * c451d05 (feature/car-query) feat: 实现汽车多条件筛选查询接口
|/
* 3e56599 feat: 新增汽车领域模型与内存数据仓库
* abe9aa5 chore: 初始化 Spring Boot 项目骨架与基础配置
* 3989748 Initial commit
```

常用命令：

```bash
git checkout -b feature/car-query         # 新建功能分支
git add -A
git commit -m "feat: 实现汽车多条件筛选查询接口" -m "- 新增 CarQuery 查询对象" -m "- 新增 15 个测试用例"
git checkout main
git merge --no-ff feature/car-query -m "merge: 合并车型筛选查询功能到 main"
```

## 四、推送与拉取

```bash
git push -u origin main
git push origin --all          # 一并推送全部分支（含 4 个 feature 分支与练习分支）
git push origin --tags         # 如有标签

git fetch --prune              # 拉取远程最新引用并清理已删除的远端分支
git pull --ff-only             # 以快进方式拉取，避免无意中产生合并提交
```

## 五、版本回滚演练（一）：`git revert`

### 5.1 故意引入错误提交

提交 `7da31d6` 给筛选服务加了一个「排序结果缓存」：缓存 key 只用了排序方式，
没有带上筛选条件与分页，于是第二次请求会命中第一次请求留下的结果。

```java
perf: 为车型筛选结果增加排序缓存
```

新增的复现用例 `CarSortCacheBugTest#shouldNotReuseCacheAcrossDifferentFilters` 立即失败：

```
[ERROR] Tests run: 33, Failures: 1, Errors: 0, Skipped: 0
[ERROR] com.dongchedi.car.service.CarSortCacheBugTest.shouldNotReuseCacheAcrossDifferentFilters
expected: 35L
[INFO] BUILD FAILURE
```

失败原因很明确：先请求 `energyType=BEV&sort=PRICE_ASC`（10 款车）后，
再请求不带筛选条件的 `sort=PRICE_ASC`，返回的仍然是那 10 款车，而不是全部 35 款。

### 5.2 用 `git revert` 回滚到正确状态

```bash
git revert --no-edit 7da31d6
# [main 41a99ac] Revert "perf: 为车型筛选结果增加排序缓存"
#  2 files changed, 3 insertions(+), 57 deletions(-)
#  delete mode 100644 src/test/java/com/dongchedi/car/service/CarSortCacheBugTest.java
```

回滚后重新运行测试：

```bash
mvn test
# Tests run: 32, Failures: 0, Errors: 0, Skipped: 0
# BUILD SUCCESS
```

`revert` 的特点：**不删除历史**，而是新增一个反向提交，因此即使错误提交已经推送到远程，
其他协作者也能安全地拉到这个回滚。

## 六、版本回滚演练（二）：`git reset --hard`

对于**尚未推送**的本地提交，可以直接移动分支指针：

```bash
git checkout -b practice/rollback-demo

# 提交 1
git add -A && git commit -m "practice: 添加版本回滚练习说明"
# [practice/rollback-demo 83b72c4] practice: 添加版本回滚练习说明

# 提交 2（故意写错的内容）
git add -A && git commit -m "practice: 追加一行错误内容（将被回滚）"
# [practice/rollback-demo 2673a77] practice: 追加一行错误内容（将被回滚）

git log --oneline -3
# 2673a77 practice: 追加一行错误内容（将被回滚）
# 83b72c4 practice: 添加版本回滚练习说明
# 41a99ac Revert "perf: 为车型筛选结果增加排序缓存"

# 回到上一个正确状态
git reset --hard HEAD~1
# HEAD is now at 83b72c4 practice: 添加版本回滚练习说明

git log --oneline -2
# 83b72c4 practice: 添加版本回滚练习说明
# 41a99ac Revert "perf: 为车型筛选结果增加排序缓存"
```

被丢弃的提交并没有立刻消失，可以通过 `git reflog` 找回：

```
83b72c4 HEAD@{0}: reset: moving to HEAD~1
2673a77 HEAD@{1}: commit: practice: 追加一行错误内容（将被回滚）
83b72c4 HEAD@{2}: commit: practice: 添加版本回滚练习说明
41a99ac HEAD@{3}: checkout: moving from main to practice/rollback-demo
41a99ac HEAD@{4}: revert: Revert "perf: 为车型筛选结果增加排序缓存"
7da31d6 HEAD@{5}: commit: perf: 为车型筛选结果增加排序缓存
```

如需找回：

```bash
git reset --hard 2673a77      # 直接回到被丢弃的提交
# 或者只拣选其中的改动
git cherry-pick 2673a77
```

### 两种回滚方式对比

| 命令 | 是否改写历史 | 适用场景 |
| --- | --- | --- |
| `git revert <commit>` | 否，新增反向提交 | 已经推送到远程、可能被他人拉取的提交 |
| `git reset --hard <commit>` | 是，移动分支指针 | 只在本地、尚未推送的提交 |

## 七、同步校验

推送完成后，运行校验脚本逐项比对本仓库与远程仓库：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/verify-sync.ps1
```

脚本会检查：

1. 本地 `main` 与 `origin/main` 的提交哈希是否一致；
2. 工作区是否有未提交的改动；
3. 是否存在未推送 / 未拉取的提交；
4. `git ls-tree` 输出的文件清单是否完全一致（无文件遗漏）；
5. 提交历史（`git log --format=%H`）是否逐条一致；
6. 本地分支与远程分支列表是否一致。

也可以手动核对：

```bash
git rev-parse HEAD
git rev-parse origin/main
git ls-remote origin refs/heads/main
git diff --stat HEAD origin/main
git log --oneline HEAD..origin/main      # 应为空
git log --oneline origin/main..HEAD      # 应为空
```

## 八、常见问题

| 问题 | 解决办法 |
| --- | --- |
| 推送被拒绝：`non-fast-forward` | 先 `git pull --rebase` 变基到最新远程提交，再 `git push` |
| 提交信息写错了 | 尚未推送用 `git commit --amend`；已推送只能 `git revert` 或经协商后强推 |
| 想撤销 `git add` | `git restore --staged <file>` |
| 想丢弃工作区改动 | `git restore <file>`（危险操作，请先确认） |
| 中文文件名显示为 `\344\270\255` | `git config core.quotepath false` |
| 换行符导致整文件 diff | 配置 `.gitattributes` 并 `git config core.autocrlf false` |
