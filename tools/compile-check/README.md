# compile-check

用于在**没有 NeoGradle 构建链**的机器上，用一个近似类路径跑 `javac`，把 1.20.4 源码与 Minecraft 1.21.1 的真实类撞一遍，拿到错误清单。

## 组件

- `Remap.java`：自写的 ASM 重映射器。把 Mojang ProGuard 映射（`client.txt`）应用到混淆客户端 jar，输出 Mojang 官方名（mojmap）jar。ART 的 ProGuard 解析方向相反，故未使用。

## 用法

```bash
# 1. 取 1.21.1 客户端 jar 与映射（来自版本 JSON 的 downloads.client / downloads.client_mappings）
curl -O https://piston-data.mojang.com/v1/objects/30c73b1c5da787909b2f73340419fdf13b9def88/client.jar
curl -O https://piston-data.mojang.com/v1/objects/2244b6f072256667bcd9a73df124d6c58de77992/client.txt

# 2. 编译并运行重映射器（需要 asm / asm-commons / asm-tree）
javac -cp "asm-9.6.jar:asm-commons-9.6.jar:asm-tree-9.6.jar" -d remapout Remap.java
java -cp "remapout:asm-9.6.jar:asm-commons-9.6.jar:asm-tree-9.6.jar" Remap client.txt client.jar client-mojmap.jar

# 3. 编译模组源码（JDK 21；libs/ 放 MC 依赖库、NeoForge universal、Mixin、MixinExtras）
javac -proc:none -nowarn -Xmaxerrs 10000 -d out \
  -cp "client-mojmap.jar:neoforge-<ver>-universal.jar:stubout:libs/*" \
  $(find src/main/java -name '*.java')
```

## 已知局限

- `universal.jar` 不含 `net/minecraft` 类，也不含 NeoForge 对 Minecraft 的**二进制补丁**，因此补丁成员（附件接口、扩展方法）会报“找不到符号”，属误报。
- `Dist` / `FMLEnvironment` / `ObfuscationReflectionHelper` 需自建桩类（见 `docs/compile-check-1.21.1.md`）。
- 反编译源码本身存在大量弱类型残留（`instanceof var`、通用 `Object` 接收者），这类错误与版本无关。
