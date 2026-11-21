> **重要法律声明**：本仓库为教育性质的反编译 + 移植研究产物，**绝非** Structure Gel API 的官方仓库。所有原始代码的知识产权归 Modding Legacy 及原作者所有。本仓库仅供个人学习与研究。移植部分保留所有权益（All Rights Reserved）。详见 [LICENSE](LICENSE)（含上游 GPL-3.0 全文）。

# Structure Gel API — 1.21.1 NeoForge 移植源码存档

> 这不是官方仓库。官方 Structure Gel API 由 Modding Legacy 开发并发布于 [GitLab](https://gitlab.com/modding-legacy/structure-gel-api)。本仓库保存从 1.20.4 版反编译源码迁移到 Minecraft 1.21.1 / NeoForge 的 API 源码，供 API 阅读、研究与复用参考。

## 定位

Structure Gel API 是一个面向结构生成 / 传送门 / 动态刷怪等功能的**前置 API 模组**，被 Blue Skies 等模组依赖。

本仓库作为**源码存档**提供：

| 项目 | 内容 |
| --- | --- |
| 上游版本 | 1.20.4 / 2.19.1（反编译自发布 jar） |
| 目标环境 | Minecraft 1.21.1 / NeoForge 21.1.234+ |
| Java | 21 |
| 源码数量 | 184 个 `.java` |
| 仓库性质 | API 源码存档（非独立可编译工程） |

## 与 blue_skies 仓库的关系

- 完整、可编译的模组工程（含内嵌的本 API）位于 [blue_skies 仓库](https://github.com/Klisuaiji/blue_skies)。
- 本仓库单独保存 `com.legacy.structure_gel` 包的 184 个源码，方便 API 阅读、比对与复用。
- 两个包之间存在**双向引用**（本 API 中有 15 处引用 `com.legacy.blue_skies`），因此本仓库的源码不保证独立编译通过——请以 blue_skies 仓库的完整工程为准。

## 目录结构

```
src/main/java/com/legacy/structure_gel/
├── api/            # 对外 API：方块、方块实体、生物群系、结构、传送门等
├── core/           # 核心实现与工具
├── mixins/         # Mixin 注入
└── StructureGel.java 等入口类
```

## 已知局限

- 源码由反编译还原，局部变量名、lambda / 内部类命名带有编译器痕迹；
- 泛型签名、注解参数、record/enum 结构与原始源码可能存在差异；
- 本仓库不含构建脚本与资源文件，需配合 blue_skies 仓库的完整工程使用。

## 版权声明 (Copyright)

所有 Structure Gel API 原始代码的版权归 Modding Legacy 及原作者（Silver_David、KingPhygieBoo、Jonathing）所有。上游 `mods.toml` 声明为 GPL-3.0，LICENSE 中保留其全文。移植工作 © 2026 Kulisaiji（保留所有权益）。

本仓库仅供个人学习与研究。若您是版权持有人并希望移除本仓库，请提交 Issue，将立即处理。
