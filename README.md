# Structure Gel API — 1.20.4 (2.19.1) 反编译源码

<p align="center">Decompiled Java sources of <b>Structure Gel API</b> for Minecraft 1.20.4 / NeoForge</p>

> **这不是官方源码仓库。** 本仓库是对已发布构建产物 `structure_gel-1.20.4-2.19.1.jar` 的反编译存档，用于学习、逆向研究与移植参考。

## 项目信息

| 项目 | 内容 |
| --- | --- |
| 模组名称 | Structure Gel API |
| 模组 ID | `structure_gel` |
| 作者 | Silver_David, KingPhygieBoo, Jonathing |
| 官方仓库 | https://gitlab.com/modding-legacy/structure-gel-api （Modding Legacy） |
| 官方站点 | https://moddinglegacy.com/ |
| 许可证 | **GPL-3.0**（原始 `mods.toml` 中的声明，全文见 [LICENSE](LICENSE)） |
| 用途 | 面向前置/结构生成的开发者 API 模组，被 Blue Skies 等模组依赖 |

`mods.toml` 声明的是 GPL-3.0，本存档在保留原作者署名与许可证的前提下提供，遵循 GPL-3.0 的源码可得性要求。

## 存档对象

| 字段 | 值 |
| --- | --- |
| 文件名 | `structure_gel-1.20.4-2.19.1.jar` |
| 大小 | 1,045,828 字节 |
| SHA-256 | `f14f4fc91f0de784e2de1ff2162602923d137f92b431f9d2c37aada92af56f4c` |
| 目标环境 | Minecraft 1.20.4 / NeoForge `[20.4.111-beta,)` |

## 反编译方法

- 运行时：Temurin JDK 17.0.21
- 反编译器：Vineflower 1.11.1（Fernflower 的维护分支）

```bash
unzip -q structure_gel-1.20.4-2.19.1.jar '*.class' -d classes
java -jar vineflower.jar -dgs=1 classes out
```

结果：376 个 `.class`（含内部类）→ **208 个 `.java`** 文件。

## 目录结构

```
src/main/java/com/legacy/structure_gel/
├── api/            # 对外 API：方块、方块实体、生物群系、结构、传送门等
├── core/           # 核心实现与工具
├── mixins/         # Mixin 注入
└── StructureGel.java 等入口类
src/main/resources/
├── META-INF/mods.toml                   # 模组元数据（保留原样）
├── META-INF/accesstransformer.cfg       # 访问转换器
├── structure_gel.mixins.json            # Mixin 配置
├── pack.mcmeta
└── coremods/*.js                        # jar 内原始的 JS coremod（非反编译产物）
```

## 如何编译

原 jar 不包含构建脚本，因此本仓库只提供 `src`。需要自行准备 NeoForge 1.20.4 的 MDK：

1. 用 NeoForge 1.20.4 MDK（NeoGradle 7.x + Java 17）初始化工程；
2. 把本仓库的 `src/main/java`、`src/main/resources` 覆盖进去；
3. 依赖 `net.neoforged:neoforge:20.4.111-beta` 以上。

## 已知局限

- 局部变量名、lambda / 内部类命名带编译器痕迹，泛型签名与注解参数可能与原码存在差异；
- 资源文件（`assets/`、`data/`、结构模板）**未**包含在本仓库；
- ASM/coremod 注入点与 mixin 的配合关系需自行核对运行行为。

## 致谢

设计与实现属于 Structure Gel 原作者及 Modding Legacy 团队。
