# 1.21.1 编译探测报告（Structure Gel）

执行时间：2026-11-19　环境：本机 Linux，Temurin JDK 21.0.13，无访问 `maven.neoforged.net` 的能力。

## 为什么做这件事

要判断“1.20.4 源码迁到 1.21.1 到底有多少真实破坏点”，必须有编译器反馈。官方构建链（NeoGradle + NeoForm）在这台机器上拿不到，所以我绕开 Gradle，手工拼了一个**能跑 javac 的近似编译环境**，用真实的 1.21.1 类去撞源码。

## 环境是怎么拼出来的（可复现）

1. **Minecraft 1.21.1 客户端 jar**：从 `piston-meta.mojang.com` 取（该域名可达），得到混淆 jar（8269 个类）。
2. **重映射成 Mojang 官方名**：ART 的 ProGuard 解析方向相反（输出成 `a.class`），改用 `tools/compile-check/Remap.java`（自写 ASM 重映射器）。过程中踩到两个必须记住的点：
   - 映射表里的**类型是官方名**，字节码里是混淆名，所以做成员查表前必须先把 `desc` 走一遍 `mapMethodDesc`，否则所有带对象参数的成员都对不上（第一版就是这样漏掉了 `Component.literal`）；
   - ProGuard 成员行带 `225:225:` 这种**行号前缀**，必须剥掉，否则返回类型会被污染成 `L225:net/...`。
3. **NeoForge 本体**：`neoforge-21.1.265-universal.jar`（类已是官方名，可直接当 API 参照；但**不含 `net/minecraft` 类**，也不含对 MC 的二进制补丁）。
4. **Minecraft 依赖库**：从版本 JSON 的 `libraries` 全量拉取（97 个 jar，72 MB）。
5. **编译期缺件的临时桩**：`Dist`、`FMLEnvironment`、`ObfuscationReflectionHelper`（在 FML 里，本机拿不到）；Mixin 用 Fabric 镜像的 `sponge-mixin 0.16.4`，MixinExtras 用 Central 的 `0.5.5`。
6. `javac -proc:none` 编译 `src/main/java`（208 个文件）。

## 结果

| 指标 | 修正前 | 修正后 |
| --- | --- | --- |
| 错误总数 | 2747 | **731** |
| 报错文件 | 170 / 208 | **119 / 208** |

首版 2747 里有约 2000 个是我自己重映射器漏映射造成的假错，修掉后才剩下真正可分析的部分。

高频错误（修正后）：

| 错误 | 次数 | 性质 |
| --- | --- | --- |
| `PlayPayloadContext` 不存在 | 41 | **真**：1.20.5+ 拆成 `ClientPayloadContext` / `ServerPayloadContext` |
| `OnlyIn` 不存在 | 36 | 噪声：FML 注解，本机缺 jar |
| `BootstapContext` 不存在 | 34 | **反编译残留**：拼写错误，源码本意是 `BootstrapContext` |
| `vertex(Matrix4f, float, float, float)` | 24 | **真**：渲染管线/`GuiGraphics` 变更 |
| `SubscribeEvent` / `IEventBus` / `net.neoforged.bus.api` | 21 / 9 / 16 | 噪声：缺 `net.neoforged:bus` |
| `ResourceLocation(String,String)` 私有 | 23 + 11 | **真**：1.21 必须走 `ResourceLocation.fromNamespaceAndPath` |
| `getTag()` / `getOrCreateTag()` | 19 / 15 | **真**：物品 NBT 已由数据组件取代 |
| `getData()` | 16 | 一半真：附件 API 在补丁 class 里，本机不可见 |
| `isValidResourceLocation(String)` | 12 | **真**：已移除 |
| `KeyMapping(String, KeyConflictContext, ...)` | 6 | **真**：构造签名变更 |
| `TabVisibility` protected | 5 | **真**：可见性变更 |
| `saveWithoutMetadata` | 5 | **真**：签名变更 |
| `portalTime` 字段 | 6 | **真**：字段移除 |
| `TickEvent` 包不存在 | 6 | **真**：包路径迁移 |

## 结论

1. **731 个错误里，噪声约 90 个**（缺 FML/bus jar、补丁成员不可见），**反编译残留约 40 个**（`BootstapContext` 这类），其余 **约 600 个是真实的 1.21.1 版本差异**。这个比例说明：结构是能迁的，但必须逐处改，不能靠正则批量替换。
2. **顺序要倒过来**：反编译残留（`BootstapContext`、`instanceof var`）在 1.20.4 上同样编译不过。理想是先让 1.20.4 基线能编译，再迁移；否则每次编译都在跟两类问题混战。
3. 顺带实证到 1.21.1 的两条事实（后面改代码直接可用）：`Level` 有 `getRandom()`、无 `random` 字段；`ItemStack` 实现 `DataComponentHolder`、无 `getOrCreateTag()`。

## 要真正做到“编译通过”，还差什么

`universal.jar` 不含 NeoForge 对 Minecraft 的**二进制补丁**（例如 `ItemStack` 上的附件接口、`Level` 上的扩展方法），这些只在 NeoForm 产出的 patched jar 里。缺的是：

```
net.neoforged:neoform:1.21.1-20240808.144430
```

拿到它（或者 MDK 跑过一次 `gradlew build` 之后的 Gradle 缓存），就能消掉“缺 jar”和“补丁成员”这两类噪声，剩下的错误才是真正需要逐处改的迁移点。
