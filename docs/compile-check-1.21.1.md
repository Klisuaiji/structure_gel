# 1.21.1 编译探测报告（Structure Gel）

执行时间：2026-11-19　环境：本机 Linux，Temurin JDK 21.0.13，无访问 `maven.neoforged.net` 的能力。

## 为什么做这件事

要判断“1.20.4 源码迁到 1.21.1 到底有多少真实破坏点”，必须有编译器反馈。官方构建链（NeoGradle + NeoForm）在这台机器上拿不到，所以我绕开 Gradle，手工拼了一个**能跑 javac 的近似编译环境**，用真实的 1.21.1 类去撞源码。

## 环境是怎么拼出来的（可复现）

1. **Minecraft 1.21.1 客户端 jar**：从 `piston-meta.mojang.com` 取（这个域名可达），得到混淆 jar（8269 个类）。
2. **重映射成 Mojang 官方名**：ART 的 ProGuard 解析方向不对，改用 `tools/compile-check/Remap.java`（自写 ASM 重映射器），把 `client.txt`（ProGuard 映射）应用成 mojmap jar。结果为 `net/minecraft/world/item/ItemStack.class` 等 8269 个正常类。
3. **NeoForge 本体**：`neoforge-21.1.265-universal.jar`（此版本类已是 Mojang 名，可直接当 API 参照；但**不含 `net/minecraft` 类**）。
4. **Minecraft 依赖库**：从版本 JSON 的 `libraries` 段全部拉取（97 个 jar，72 MB）。
5. **编译期缺件的临时桩**：`net.neoforged.api.distmarker.Dist`、`net.neoforged.fml.loading.FMLEnvironment`、`net.neoforged.fml.util.ObfuscationReflectionHelper`（这三个在 FML 里，本机拿不到）；Mixin 用 Fabric 镜像的 `sponge-mixin 0.16.4`，MixinExtras 用 Central 的 `0.5.5`。
6. `javac -proc:none` 编译 `src/main/java`（208 个文件）。

## 结果

| 指标 | 数值 |
| --- | --- |
| 源文件 | 208 |
| 报错文件 | 170（仅 38 个文件零报错） |
| 错误总数 | **2747** |

高频缺失符号（`symbol:` 计数）：

| 符号 | 次数 | 归类 |
| --- | --- | --- |
| `literal(String)` / `translatable(String)` | 143 / 59 | 反编译残留：接收者类型丢失，源码里其实是 `Component.literal` |
| `getX()` / `getY()` / `z()` / `location()` | 71 / 57 / 16 / 49 | 反编译残留：`BlockPos`/`Vec3` 接收者被还原成弱类型 |
| `PlayPayloadContext` | 41 | 1.20.5+ 网络 API 改名（`IPayloadContext`） |
| `OnlyIn` / `SubscribeEvent` | 36 / 21 | 缺编译期依赖（FML 注解、`net.neoforged:bus`） |
| `BootstapContext` | 34 | 反编译残留：拼写/泛型恢复错误 |
| `vertex(Matrix4f, float, float, float)` | 24 | 1.20.5+ 渲染管线重写 |
| `setBlock(BlockPos, BlockState, int)` | 20 | 1.21 签名变化 |
| `relative(Direction)` | 17 | 反编译残留（`Direction` 接收者类型丢失） |
| `getTag()` | 18 | **真实**：物品 NBT 已被数据组件取代 |
| `getData()` | 16 | 真实/半真实：NeoForge 的 `AttachmentHolder#getData` 存在，但 vanilla jar 里没有 |

## 结论

1. **2747 个错误不能全部算到版本迁移头上**。粗分三类：
   - 反编译残留（约六成）：Vineflower 从字节码恢复出的弱类型接收者、`instanceof var`、错误的泛型参数。这类问题**在 1.20.4 上也一样存在**——也就是说，这份反编译源码本来就无法编译，跟迁不迁移无关。
   - 缺失编译期依赖（约两成）：FML、bus、coremods、JarJar 这些只有 neoforged 仓库才有的包。
   - 真实版本差异（约两成）：数据组件、网络 API、渲染管线、`Level.random`、签名变更。
2. **顺序必须倒过来**：先让 1.20.4 那份源码能编译，再谈往 1.21.1 迁。否则迁移的每一步都踩在反编译噪声上。
3. 顺带验证到两条确定的 1.21.1 事实（用于后续改代码）：`Level` 有 `getRandom()`、没有 `random` 字段；`ItemStack` 实现 `DataComponentHolder`，没有 `getOrCreateTag()`。

## 要真正编译，还差这些

只用 `universal.jar` 不够：NeoForge 对 Minecraft 类的**二进制补丁**（例如 `ItemStack` 上的附件接口、`Level` 上的扩展方法）只在 NeoForm 产出的 patched jar 里。缺的就是这一个：

```
net.neoforged:neoform:1.21.1-20240808.144430
```

拿到它（或直接给 MDK 跑过一次 `gradlew build` 之后的 Gradle 缓存），就能把上面“缺失依赖”和“补丁成员”两类噪声消掉，剩下的错误才是真正需要改的迁移点。
