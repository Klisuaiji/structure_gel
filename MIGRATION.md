# 1.21.1 迁移状态（Structure Gel API）

分支：`1.21.1`　目标：Minecraft 1.21.1 / NeoForge 21.1.x / Java 21

## 结论先说

这个分支把**能确定性完成的部分**做完了：工程骨架、Gradle 配置、`neoforge.mods.toml` 元数据、Java 21 toolchain、资源/数据包格式。**代码层（`src/main/java`）还没有做函数级迁移**，仍是 1.20.4 形态。

原因：本机无法访问 `maven.neoforged.net`（DNS 被解析到不可达的 IPv6 地址），拿不到 NeoGradle 与 NeoForge 依赖，因此**任何代码改动都无法编译验证**。在没有编译器反馈的情况下批量改 200 个文件，只会产出“看起来像 1.21.1、实际到处报错”的代码，反而比保留原样更难修。逐处待办见 [`docs/migration-inventory.md`](docs/migration-inventory.md)。

## 已完成的改动

| 项 | 1.20.4 | 1.21.1 | 说明 |
| --- | --- | --- | --- |
| 模组元数据文件 | `META-INF/mods.toml` | `META-INF/neoforge.mods.toml` | NeoForge 1.20.5+ 改名，必须移动 |
| loaderVersion | `[1,)` | `[4,)` | NeoForge 21.x 的 FML loader 版本 |
| NeoForge 依赖范围 | `[20.4.111-beta,)` | `[21.1,)` | |
| Minecraft 依赖范围 | `[1.20.4,1.21)` | `[1.21.1,1.21.2)` | |
| 模组版本 | `2.19.1` | `3.0.0` | 跨大版本迁移，语义版本上调 |
| Java toolchain | 17 | 21 | MC 1.20.5+ 要求 Java 21 |
| `pack_format` | 26 | 48（同时兼容 34） | 1.21.1 数据包 48 / 资源包 34 |
| 构建脚本 | 无 | `build.gradle` / `settings.gradle` / `gradle.properties` | 按 NeoForge MDK 结构编写 |

`mods.toml` 中原来写死的字段已改为 Gradle `expand` 占位符（`${mod_id}`、`${mod_version}` 等），构建时由 `gradle.properties` 注入，方便后续同时维护多个版本。

## 代码层必须处理的问题（按优先级）

完整逐处清单见 [`docs/migration-inventory.md`](docs/migration-inventory.md)，这里只列类别和数量。

| 类别 | 命中处数 | 1.21.1 的替代方案 |
| --- | --- | --- |
| `getTag()` / `getOrCreateTag()`（物品 NBT） | 10 个文件 / 22 处 | 1.20.5 起物品 NBT 被**数据组件**取代：改用 `stack.get(DataComponents.CUSTOM_DATA)`（`CustomData`）或自定义 `DataComponentType`；写入用 `stack.set(...)` |
| `ICapabilityProvider` / `LazyOptional` / `Capability` | 17 个文件 | NeoForge 1.20.5+ 移除 Capability 系统，改为 `AttachmentType` + `IAttachmentHolder`（`NeoForgeRegistries.ATTACHMENT_TYPES`） |
| `MobEffectInstance` 构造 | 1 处 | 1.20.5 起接收 `Holder<MobEffect>`，需 `MobEffects.XXX`（现在是 `Holder`）直接传入，`new MobEffectInstance(holder, ...)` |
| `Level.random` | 22 个文件 | 字段被移除，统一改为 `level.getRandom()`（返回 `RandomSource`） |
| `CreativeModeTab` 输出 | 1 处 | `accept` 改为 `Output.accept(...)`，注册方式改为 `DeferredItem` / `DeferredHolder<CreativeModeTab>` |
| `StructureType` 注册 | 4 处 | 1.21 注册仍走 `DeferredRegister`，但 `StructureType` 构造参数与 `Codec` 用法有调整，需比对 1.21.1 的 `StructureType` 源码 |
| `Registry` 取用 | 54 个文件 | 多数为 `registryAccess.registryOrThrow(Registries.X)`，1.21 仍可编译，但 `Registries` 中部分键被合并/改名（如 `Registries.STRUCTURE_TYPE`），需逐个核对 |

## 高风险项（不是改个名字就能解决）

1. **JS coremod 机制**：`src/main/resources/coremods/*.js` + `META-INF/coremods.json` 是 NeoForge 的 JS coremod 注入。NeoForge 在 1.20.5 之后已不再支持这套机制，注入逻辑必须改写为 Mixin。本分支保留了这些文件（不参与编译），但它们当前**不会生效**。
2. **访问转换器**：`accesstransformer.cfg` 里的目标可能是 1.20.4 的混淆/成员名，1.21.1 里成员被重命名或搬类后会静默失效，需要逐个验证（失效会在运行期抛 `IllegalAccessError`）。
3. **Mixin 目标**：`structure_gel.mixins.json` 指向的类/方法名在 1.21.1 有变动（尤其 `LevelChunk`、`StructureStart`、`ChunkGenerator` 相关），需要重新对照映射表。
4. **结构生成数据格式**：1.21 的世界生成 JSON（`data/` 下的 `worldgen/structure` 等）字段有变化，本仓库不含资源文件，需在补充资源时一并迁移。

## 建议的推进方式

1. 本地拉这个分支，用 NeoForge 1.21.1 MDK 对齐 `gradle.properties` 里的 `neo_version` 与插件版本；
2. 先 `gradle build` 拿第一批编译错误，按 [`docs/migration-inventory.md`](docs/migration-inventory.md) 的分类逐类清（NBT → 数据组件 → Capability → 其余）；
3. 卡住的地方把报错贴回来，我按报错继续改——有编译器反馈时，逐处迁移才是可靠的。
