import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** 把 Mojang ProGuard 映射应用到混淆 jar，输出 Mojang 官方名（mojmap）jar。 */
public class Remap {

    static final Map<String, String> classMap = new HashMap<>();      // obfOwner -> namedOwner
    static final Map<String, String> methodMap = new HashMap<>();     // obfOwner|name|desc -> newName
    static final Map<String, String> fieldMap = new HashMap<>();      // obfOwner|name|desc -> newName
    static final Map<String, Set<String>> looseM = new HashMap<>();    // obfOwner|name -> set(newName)
    static final Map<String, Set<String>> looseF = new HashMap<>();

    static final Map<String, String> PRIM = new HashMap<>();

    static {
        PRIM.put("void", "V"); PRIM.put("boolean", "Z"); PRIM.put("byte", "B"); PRIM.put("char", "C");
        PRIM.put("short", "S"); PRIM.put("int", "I"); PRIM.put("long", "J"); PRIM.put("float", "F");
        PRIM.put("double", "D");
    }

    static String desc(String t) {
        t = t.trim();
        if (t.endsWith("[]")) return "[" + desc(t.substring(0, t.length() - 2));
        String p = PRIM.get(t);
        if (p != null) return p;
        return "L" + t.replace('.', '/') + ";";
    }

    public static void main(String[] args) throws Exception {
        String mapFile = args[0], inJar = args[1], outJar = args[2];
        parseMappings(Files.readAllLines(Paths.get(mapFile), StandardCharsets.UTF_8));
        System.out.println("classes=" + classMap.size() + " methods=" + methodMap.size() + " fields=" + fieldMap.size());

        Remapper remapper = new Remapper() {
            @Override public String map(String internalName) {
                String n = classMap.get(internalName);
                return n != null ? n : internalName;
            }
            @Override public String mapMethodName(String owner, String name, String d) {
                String k = owner + '|' + name + '|' + d;
                String n = methodMap.get(k);
                if (n != null) return n;
                Set<String> s = looseM.get(owner + '|' + name);
                if (s != null && s.size() == 1) return s.iterator().next();
                return name;
            }
            @Override public String mapFieldName(String owner, String name, String d) {
                String k = owner + '|' + name + '|' + d;
                String n = fieldMap.get(k);
                if (n != null) return n;
                Set<String> s = looseF.get(owner + '|' + name);
                if (s != null && s.size() == 1) return s.iterator().next();
                return name;
            }
        };

        int done = 0, failed = 0;
        try (ZipFile zin = new ZipFile(inJar);
             ZipOutputStream zout = new ZipOutputStream(Files.newOutputStream(Paths.get(outJar)))) {
            Enumeration<? extends ZipEntry> en = zin.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                byte[] data = zin.getInputStream(e).readAllBytes();
                if (!e.getName().endsWith(".class") || e.getName().startsWith("META-INF/")) {
                    zout.putNextEntry(new ZipEntry(e.getName()));
                    zout.write(data);
                    zout.closeEntry();
                    continue;
                }
                try {
                    ClassReader cr = new ClassReader(data);
                    ClassWriter cw = new ClassWriter(0);
                    cr.accept(new ClassRemapper(cw, remapper), 0);
                    byte[] out = cw.toByteArray();
                    String newName = cr.getClassName();
                    String nn = classMap.getOrDefault(newName, newName);
                    zout.putNextEntry(new ZipEntry(nn + ".class"));
                    zout.write(out);
                    zout.closeEntry();
                    done++;
                } catch (Throwable t) {
                    failed++;
                    if (failed < 6) System.out.println("  skip " + e.getName() + " : " + t);
                }
            }
        }
        System.out.println("remapped=" + done + " failed=" + failed);
    }

    static void parseMappings(List<String> lines) {
        String cur = null;
        for (String raw : lines) {
            if (raw.startsWith("#") || raw.trim().isEmpty()) continue;
            String ln = raw;
            if (!ln.startsWith(" ") && !ln.startsWith("\t")) {          // 类行: named -> obf:
                int arrow = ln.lastIndexOf(" -> ");
                if (arrow < 0) continue;
                String named = ln.substring(0, arrow).trim();
                String obf = ln.substring(arrow + 4).trim();
                if (obf.endsWith(":")) obf = obf.substring(0, obf.length() - 1);
                cur = obf.replace('.', '/');
                classMap.put(cur, named.replace('.', '/'));
                continue;
            }
            if (cur == null) continue;
            String t = ln.trim();
            int arrow = t.lastIndexOf(" -> ");
            if (arrow < 0) continue;
            String left = t.substring(0, arrow).trim();
            String obfName = t.substring(arrow + 4).trim();
            if (left.startsWith("<")) continue;                            // 行号信息残留
            int paren = left.indexOf('(');
            if (paren >= 0) {                                             // 方法
                int sp = left.indexOf(' ');
                String ret = left.substring(0, sp).trim();
                String name = left.substring(sp + 1, paren).trim();
                String params = left.substring(paren + 1, left.lastIndexOf(')'));
                StringBuilder d = new StringBuilder("(");
                if (!params.trim().isEmpty())
                    for (String p : params.split(",")) d.append(desc(p));
                d.append(')').append(desc(ret));
                methodMap.put(cur + '|' + obfName + '|' + d, name);
                looseM.computeIfAbsent(cur + '|' + obfName, k -> new HashSet<>()).add(name);
            } else {                                                      // 字段
                int sp = left.lastIndexOf(' ');
                if (sp < 0) continue;
                String type = left.substring(0, sp).trim();
                String name = left.substring(sp + 1).trim();
                fieldMap.put(cur + '|' + obfName + '|' + desc(type), name);
                looseF.computeIfAbsent(cur + '|' + obfName, k -> new HashSet<>()).add(name);
            }
        }
    }
}
