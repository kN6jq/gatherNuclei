package org.jiu.utils;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * YAML 配置工具类：读写运行目录下的 gatherConfig.yaml，并把配置同步到 Utils 全局变量。
 * 对外提供配置解析、单项修改与默认配置生成。
 */
public class YamlUtils {
    /**
     * 读取运行目录下的 gatherConfig.yaml，文件不存在时先生成默认配置；
     * 读取失败仅打印堆栈并返回 null。
     */
    private static Map<String, Map<String, String>> readConfigYaml() {
        String currentPath = System.getProperty("user.dir");
        String yamlFilePath = currentPath + File.separator + "gatherConfig.yaml";
        File file = new File(yamlFilePath);
        if (!file.exists()) {
            generateYaml();
        }

        Map<String, Map<String, String>> yamlMap = null;
        try (InputStream inputStream = new FileInputStream(yamlFilePath)) {
            Yaml yaml = new Yaml();
            yamlMap = yaml.loadAs(inputStream, LinkedHashMap.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return yamlMap;
    }

    /**
     * 解析配置文件并写入 Utils 各全局配置项；读取失败仅打印错误，不抛异常。
     */
    public static void parseConfig() {
        Map<String, Map<String, String>> data = readConfigYaml();
        if (data == null) {
            System.err.println("Failed to read YAML configuration file");
            return;
        }

        Map<String, String> nucleiConfig = data.get("nuclei");
        if (nucleiConfig != null) {
            Utils.templatePath = nucleiConfig.getOrDefault("nucleipath", "");
            Utils.templateArg = nucleiConfig.getOrDefault("nucleiarg", "");
        }

        Map<String, String> fofaConfig = data.get("fofa");
        if (fofaConfig != null) {
            Utils.fofaUrl = fofaConfig.getOrDefault("fofaurl", "");
            Utils.fofaKey = fofaConfig.getOrDefault("fofakey", "");
            Utils.fofaTimeRange = org.jiu.core.FofaCore.normalizeTimeRange(fofaConfig.get("fofatimerange"));
        }

        Map<String, String> hunterConfig = data.get("hunter");
        if (hunterConfig != null) {
            Utils.hunterUrl = hunterConfig.getOrDefault("hunterurl", "");
            Utils.hunterKey = hunterConfig.getOrDefault("hunterkey", "");
        }

        Map<String, String> zoneConfig = data.get("zone");
        if (zoneConfig != null) {
            Utils.zoneUrl = zoneConfig.getOrDefault("zoneurl", "");
            Utils.zoneKey = zoneConfig.getOrDefault("zonekey", "");
        }

        Map<String, String> daydaymapConfig = data.get("daydaymap");
        if (daydaymapConfig != null) {
            Utils.daydaymapUrl = daydaymapConfig.getOrDefault("daydaymapurl", "");
            Utils.daydaymapKey = daydaymapConfig.getOrDefault("daydaymapkey", "");
        }
    }

    /**
     * 更新指定配置项并整体重写 gatherConfig.yaml；type 未识别时不做任何处理，
     * 读取或写入失败仅打印错误。
     *
     * @param type 配置键名，如 fofaurl、nucleipath
     */
    public static void modifyYaml(String type, String value) {
        Map<String, Map<String, String>> data = readConfigYaml();
        if (data == null) {
            System.err.println("Failed to read YAML configuration file");
            return;
        }

        // 配置键 -> 所属 section 的映射
        Map<String, String> configMap = new LinkedHashMap<>();
        configMap.put("nucleipath", "nuclei");
        configMap.put("nucleiarg", "nuclei");
        configMap.put("fofaurl", "fofa");
        configMap.put("fofakey", "fofa");
        configMap.put("fofatimerange", "fofa");
        configMap.put("hunterurl", "hunter");
        configMap.put("hunterkey", "hunter");
        configMap.put("zoneurl", "zone");
        configMap.put("zonekey", "zone");
        configMap.put("daydaymapurl", "daydaymap");
        configMap.put("daydaymapkey", "daydaymap");

        String section = configMap.get(type);
        if (section != null) {
            if (!data.containsKey(section)) {
                data.put(section, new LinkedHashMap<>());
            }
            data.get(section).put(type, value);

            // 重建数据结构：只保留 configMap 定义的已知字段，丢弃文件中的未知内容
            LinkedHashMap<String, Map<String, String>> newData = new LinkedHashMap<>();

            for (Map.Entry<String, String> entry : configMap.entrySet()) {
                String configType = entry.getKey();
                String sectionName = entry.getValue();

                if (!newData.containsKey(sectionName)) {
                    newData.put(sectionName, new LinkedHashMap<>());
                }

                // 如果原数据中有这个字段，则保留；缺失的补空值，保证配置结构完整
                if (data.containsKey(sectionName) && data.get(sectionName).containsKey(configType)) {
                    newData.get(sectionName).put(configType, data.get(sectionName).get(configType));
                } else {
                    newData.get(sectionName).put(configType, "");
                }
            }

            String currentPath = System.getProperty("user.dir");
            String yamlFilePath = currentPath + File.separator + "gatherConfig.yaml";

            try (FileWriter fileWriter = new FileWriter(yamlFilePath)) {
                DumperOptions dumperOptions = new DumperOptions();
                dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
                Yaml yaml = new Yaml(dumperOptions);
                yaml.dump(newData, fileWriter);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 在运行目录生成含全部配置项（值为空串）的 gatherConfig.yaml；文件已存在时不覆盖。
     */
    public static void generateYaml() {
        String currentPath = System.getProperty("user.dir");
        String yamlFilePath = currentPath + File.separator + "gatherConfig.yaml";
        File file = new File(yamlFilePath);

        if (!file.exists()) {
            try (FileWriter fileWriter = new FileWriter(yamlFilePath)) {
                DumperOptions dumperOptions = new DumperOptions();
                dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
                Yaml yaml = new Yaml(dumperOptions);

                LinkedHashMap<String, Map<String, String>> data = new LinkedHashMap<>();

                data.put("nuclei", createConfigSection("nucleipath", "nucleiarg"));
                data.put("fofa", createConfigSection("fofaurl", "fofakey", "fofatimerange"));
                data.put("hunter", createConfigSection("hunterurl", "hunterkey"));
                data.put("zone", createConfigSection("zoneurl", "zonekey"));
                data.put("daydaymap", createConfigSection("daydaymapurl", "daydaymapkey"));

                yaml.dump(data, fileWriter);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 创建值全为空串的配置 section。
     */
    private static Map<String, String> createConfigSection(String... keys) {
        Map<String, String> section = new LinkedHashMap<>();
        for (String key : keys) {
            section.put(key, "");
        }
        return section;
    }
}