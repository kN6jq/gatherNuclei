package org.jiu.utils;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class YamlUtils {
    // 读取当前目录下的config yaml文件
    private static Map<String, Map<String, String>> readConfigYaml() {
        String currentPath = System.getProperty("user.dir");
        String yamlFilePath = currentPath + File.separator + "gatherConfig.yaml";
        // 判断文件是否存在,不存在调用generateYaml方法生成yaml文件
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

    // 解析yaml文件
    public static void parseConfig() {
        Map<String, Map<String, String>> data = readConfigYaml();
        if (data == null) {
            System.err.println("Failed to read YAML configuration file");
            return;
        }

        // 获取nuclei配置
        Map<String, String> nucleiConfig = data.get("nuclei");
        if (nucleiConfig != null) {
            Utils.templatePath = nucleiConfig.getOrDefault("nucleipath", "");
            Utils.templateArg = nucleiConfig.getOrDefault("nucleiarg", "");
        }

        // 获取fofa配置
        Map<String, String> fofaConfig = data.get("fofa");
        if (fofaConfig != null) {
            Utils.fofaUrl = fofaConfig.getOrDefault("fofaurl", "");
            Utils.fofaKey = fofaConfig.getOrDefault("fofakey", "");
        }

        // 获取hunter配置
        Map<String, String> hunterConfig = data.get("hunter");
        if (hunterConfig != null) {
            Utils.hunterUrl = hunterConfig.getOrDefault("hunterurl", "");
            Utils.hunterKey = hunterConfig.getOrDefault("hunterkey", "");
        }

        // 获取zone配置
        Map<String, String> zoneConfig = data.get("zone");
        if (zoneConfig != null) {
            Utils.zoneUrl = zoneConfig.getOrDefault("zoneurl", "");
            Utils.zoneKey = zoneConfig.getOrDefault("zonekey", "");
        }

        // 获取daydaymap配置
        Map<String, String> daydaymapConfig = data.get("daydaymap");
        if (daydaymapConfig != null) {
            Utils.daydaymapUrl = daydaymapConfig.getOrDefault("daydaymapurl", "");
            Utils.daydaymapKey = daydaymapConfig.getOrDefault("daydaymapkey", "");
        }
    }

    // 修改yaml文件
    public static void modifyYaml(String type, String value) {
        Map<String, Map<String, String>> data = readConfigYaml();
        if (data == null) {
            System.err.println("Failed to read YAML configuration file");
            return;
        }

        // 使用Map来存储配置类型和对应的section
        Map<String, String> configMap = new LinkedHashMap<>();
        configMap.put("nucleipath", "nuclei");
        configMap.put("nucleiarg", "nuclei");
        configMap.put("fofaurl", "fofa");
        configMap.put("fofakey", "fofa");
        configMap.put("hunterurl", "hunter");
        configMap.put("hunterkey", "hunter");
        configMap.put("zoneurl", "zone");
        configMap.put("zonekey", "zone");
        configMap.put("daydaymapurl", "daydaymap");
        configMap.put("daydaymapkey", "daydaymap");

        String section = configMap.get(type);
        if (section != null) {
            // 确保section存在
            if (!data.containsKey(section)) {
                data.put(section, new LinkedHashMap<>());
            }
            data.get(section).put(type, value);

            // 创建新的数据结构，只包含configMap中定义的字段
            LinkedHashMap<String, Map<String, String>> newData = new LinkedHashMap<>();

            // 遍历configMap，重新组织数据
            for (Map.Entry<String, String> entry : configMap.entrySet()) {
                String configType = entry.getKey();
                String sectionName = entry.getValue();

                if (!newData.containsKey(sectionName)) {
                    newData.put(sectionName, new LinkedHashMap<>());
                }

                // 如果原数据中有这个字段，则保留
                if (data.containsKey(sectionName) && data.get(sectionName).containsKey(configType)) {
                    newData.get(sectionName).put(configType, data.get(sectionName).get(configType));
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

    // 在当前目录下生成yaml文件
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

                // 初始化各个配置section
                data.put("nuclei", createConfigSection("nucleipath", "nucleiarg"));
                data.put("fofa", createConfigSection("fofaurl", "fofakey"));
                data.put("hunter", createConfigSection("hunterurl", "hunterkey"));
                data.put("zone", createConfigSection("zoneurl", "zonekey"));
                data.put("daydaymap", createConfigSection("daydaymapurl", "daydaymapkey"));

                yaml.dump(data, fileWriter);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // 辅助方法：创建配置section
    private static Map<String, String> createConfigSection(String... keys) {
        Map<String, String> section = new LinkedHashMap<>();
        for (String key : keys) {
            section.put(key, "");
        }
        return section;
    }
}