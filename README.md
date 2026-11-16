<div align="center">
  <h1 style="font-size: 28px; margin: 10px 0 0 0;">Spline Roads</h1>
  <p>Spline-based road building mod for Minecraft.<br>Unofficial 1.21.1 NeoForge port of Spline Roads 2.1.0.</p>
</div>

<p align="center">
  <a href="https://github.com/BlueStarrySky233/Spline-Roads-1.21.1-NeoForge/graphs/contributors">
    <img alt="GitHub Contributors" src="https://img.shields.io/github/contributors/BlueStarrySky233/Spline-Roads-1.21.1-NeoForge" />
  </a>
  <a href="https://github.com/BlueStarrySky233/Spline-Roads-1.21.1-NeoForge/issues">
    <img alt="Issues" src="https://img.shields.io/github/issues/BlueStarrySky233/Spline-Roads-1.21.1-NeoForge?color=0088ff" />
  </a>
  <a href="https://github.com/BlueStarrySky233/Spline-Roads-1.21.1-NeoForge/pulls">
    <img alt="GitHub pull requests" src="https://img.shields.io/github/issues-pr/BlueStarrySky233/Spline-Roads-1.21.1-NeoForge?color=0088ff" />
  </a>
  <br/>
    <img alt="Stars" src="https://img.shields.io/github/stars/BlueStarrySky233/Spline-Roads-1.21.1-NeoForge" />
    <img alt="Forks" src="https://img.shields.io/github/forks/BlueStarrySky233/Spline-Roads-1.21.1-NeoForge" />
</p>

## 简介
Spline Roads 是 Minecraft 的道路建造模组，提供样条道路、路口、匝道、立交、桥隧、标志与信号灯，以及可选的 MTR 铁路路基联动。

本分支是 **1.20.1 Forge 版向 Minecraft 1.21.1 / NeoForge 的移植**。

## 免责声明

**这是一个非官方（unofficial）移植版本，尚未经过充分测试。**

- 移植工作集中在 API 迁移与渲染管线适配，**没有覆盖全部功能组合的回归测试**：路口、立交、隧道、收费站、标牌、信号灯、多人联机等场景均可能存在未知缺陷。
- 1.20.1 版的部分机制在当前版本被替换或暂时禁用（见"与 1.20.1 版本的差异"），行为与原版**不完全一致**。
- 使用前请**备份存档**。模组会写入世界数据（放置方块、保存道路记录），出现问题可能导致世界损坏或道路数据异常，且**不提供数据修复保证**。
- 在多人服务器上使用前，请先在单人测试世界验证；服务端与客户端必须使用同一版本，版本不一致可能导致连接失败或数据错乱。
- 本模组按「现状」提供，**不附带任何明示或暗示的担保**。使用本模组所产生的一切后果（包括但不限于存档损坏、服务器故障、数据丢失）由使用者自行承担，移植者与原作者均不承担责任。
- 若发现缺陷，欢迎提交 issue；但**不承诺修复时限**。

## 支持的版本

| 项目 | 版本 |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.264（21.1.x 任意小版本均可） |
| Java | 21 |
| 模组版本 | 1.21.1-2.1.0-1 |

版本号格式为 `Minecraft版本-所移植的SR版本-移植修订号`。`1.21.1-2.1.0-1` 表示：面向 Minecraft 1.21.1、移植自 1.20.1 分支的 Spline Roads 2.1.0、移植侧第 1 个修订。修订号只在移植侧修 bug 时递增，不改变所对照的上游版本。

## 安装

使用 Minecraft 1.21.1、NeoForge 21.1.x 和 Java 21。把 Releases 中的 `splineroads-1.21.1-2.1.0-1.jar` 放入 `mods` 目录。

联机时，客户端和服务端必须使用同一版本。MTR 为可选依赖：安装 MTR 4.1.x 后即可使用铁路路基联动，未安装或版本不符时相关功能安全降级。

## 构建

需要 **JDK 21**，并让 Gradle 用它（`JAVA_HOME` 指向 JDK 21）。Gradle 8.8 在 JDK 25 上会因 Groovy 无法解析新版 class 文件而以 `Unsupported class file major version 69` 失败。

铁路适配层直接引用 MTR 的类型，所以编译期始终需要 MTR 4.1.x 的 jar（与 1.20.1 版相同）。解析顺序：

- 优先使用 `run/mods/` 下的 MTR jar（离线可用）
- 没有时按 `gradle.properties` 的 `mtr_version` 从 Modrinth 下载，也可用 `-Pmtr_version=<版本>` 覆盖

想让构建完全不联网，把 MTR jar 放进 `run/mods/` 并加 `--offline` 即可。

```bash
# Windows
set JAVA_HOME=C:\Program Files\Java\jdk-21
gradlew build

# Linux / macOS
export JAVA_HOME=/path/to/jdk-21
./gradlew build
```

首次构建需要联网，Gradle 会下载 NeoForge 21.1.264 与编译所需依赖；之后可加 `--offline` 复用缓存。

产物位于 `build/libs/splineroads-1.21.1-2.1.0-1.jar`。

开发时可直接运行 `gradlew runClient`。

## 功能

- 样条道路：车道数、宽度、绿化带、护栏、路缘、人行道与非机动车道
- 道路网络：节点、端点、路口（含 Y 型、环岛）、匝道、立交、隧道与桥梁
- 道路标线、导向箭头、标牌、门架、路灯与收费站设施
- 手动规划、俯视预览、实景预览与分段微调
- 世界内实时编辑：端点微调、删除、分流、路基高亮
- `/sr` 系列命令与性能浮层（`/sr perf`）

## 与 1.20.1 版本的差异

移植到 1.21.1 后，以下部分与 1.20.1 版行为不同：

1. **地形注入后端暂不可用。** 1.20.1 版通过 JS coremod 把路面注入区块编译流程，以获得光影包的地形材质；NeoForge 不再提供该机制，需要改写为 Mixin。目前始终使用 VBO 后端，画面与 1.20.1 的 VBO 模式一致。可用 `/sr sf` 查看当前表面模式。
2. **MTR 仅支持 4.1.x。** 适配层针对 MTR 4.1.0 的 `org.mtr.core` / `org.mtr.block` 结构编写，版本不在 `[4.1.0-beta.1,4.2.0)` 时铁路功能会降级并提示原因。MTR 的四个注入点通过 Mixin 实现（`splineroads.mixins.json`），MTR 缺席时整组 Mixin 自动跳过。
3. **配置文件不再支持外部热重载。** 1.20.1 版监听配置文件并重新派发 `IConfigEvent`，游戏外编辑会即时生效；NeoForge 21.1 移除了 `IConfigEvent` 与 `ModConfig#getConfigData()`，因此游戏内修改仍然即时生效，游戏外编辑需重启后应用。

## 已知问题

- **路口接缝闪烁。** 同时开启「绿色非机动车道」与「外侧马路牙子」时，路口附近可能有一条边缘随视角闪动。这是两块共面几何在深度上互有胜负造成的纯视觉问题，不影响数据与碰撞；上游 2.1.0 的人行道接触面几何已移植，若仍有残留将在后续修订处理。
- **建造预览的轮廓闪烁（上游同样存在）。** 预览是贴地半透明覆盖层，与地表深度几乎相等，逐像素比较会在移动中跳变，轮廓边缘因此有轻微闪动。已在原版 1.20.1 上验证同样出现，属于上游缺陷而非移植引入；若更在意轮廓稳定，可用 `/sr previewdepth off` 让预览不参与深度测试（代价是预览会透过地形显示）。

## 移植修订记录

**1.21.1-2.1.0-1**（跟进上游 Spline Roads 2.1.0）
- 上游新增**路口点创建器**（`junction_point_creator`）：可直接在道路断面上创建路口点
- 上游新增**连续道路**体系（拟合 / 提交 / 回收），路口与立交的端点可跨道路连续续建
- 上游新增立交预览的端点确认（仅接受落在原拟合域内的固定端点）
- 上游新增人行道与整格方块的接触面几何（`SidewalkBlockContact` 及客户端模型/四边面）
- 规划地图：端点模式（Ctrl＋滚轮切换，含 HUD 提示）、双击主路选择一对端点、地表缓存秒数设置与关闭后短时保留地表
- 同步上游对道路记录、序列化格式的调整；存档格式升到 73，并移植了"读不出的存档不会被当成新世界"的保护；回滚地形时对栅栏/楼梯等邻居连接属性宽容重建；网络协议号随之上调
- 移植侧：Fabric 渲染桥在 NeoForge 上以空实现保留接缝（无 Fabric 渲染器可挂载）
- 移植侧：**MTR 铁路联动适配 MTR 4.1.x**，用 4 个 Mixin 取代 1.20.1 的 JS coremod；4.1.0 移除了 `positionsToRail`/`railIdMap`，改为按维度自建邻接表并按 `Data.sync()` 失效
- 修复了红绿灯显示错误的 bug
- 修复建造预览的渲染姿态：分割器预览、路基高亮、附属点与车道点选择框原来把世界姿态与相机平移各应用了两次，导致线条横穿画面；现按上游写法统一为 `getPoseStack()` 加 `translate(-camera)`
- 修复只选一条主路（2 个端点）创建辅路时的 `ArrayIndexOutOfBoundsException`：连续范围的几何中心需要两条主路，节点不足时改为按真实端点校验而非越界计算
- 修复规划地图「工具 2」不响应端点模式：原来仍按 Alt/Ctrl 快捷键分派，导致路口点创建器无法从地图内触发；现按端点模式（道路端点 / 路口点 / 附属点 / 出入点）分派
- 恢复规划设置三个滑块的悬停说明（上游 `SettingSlider.hint()`），并修正地表缓存滑块的文案

**1.21.1-2.0-2**（第 2 个移植修订）
- 修复标牌立即模式几何被二次旋转，龙门架/支架不再跟随相机
- 修复规划地图删除端点后选中状态与地图数据未刷新，导致后续点击全部提示"端点已删除"
- 修复 1.21.1 `BufferBuilder.build()` 返回 null 时上传崩溃（光影下按 P 打开规划界面）
- 修复大界面缩放下文字编辑器「确认」按钮被挤出屏幕，改为按可用高度压缩行距
- 新增 `/sr previewdepth on|off` 建造预览深度测试开关
- 道路各层加入亚厘米级高差，减少与地表及彼此之间的深度冲突（2.1.0-1 已撤销：该做法反而把路面抬到建造预览之上，实测未能消除接缝闪烁）

**1.21.1-2.0**（首个移植修订）
- 完成 Forge 1.20.1 → NeoForge 1.21.1 的构建、API、渲染管线迁移

## 移植

- 原始模组：[Spline Roads](https://github.com/ShiraAya/Spline-Roads-Release) 2.1.0，作者 **ShiraAya**
- 1.21.1 NeoForge 移植：**BlueStarrySky**
- 实现方式：**AI 辅助实现，经人工审校与实机测试**（与上游 1.20.1 版一致，原作者亦在元数据中标明 AI 参与）
- 移植改动同样遵循原版的[非商业许可](LICENSE)；两个版权声明并存，详见[移植声明](NOTICE.md)

## 许可

Spline Roads 原创内容采用[自定义非商业许可](LICENSE)。随附资源和构建工具的许可见[第三方声明](THIRD_PARTY_NOTICES.md)。
