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
        Map<String, Map<String, String>> yamlMap;
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(yamlFilePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Yaml yaml = new Yaml();
        yamlMap = yaml.loadAs(inputStream, LinkedHashMap.class);
        return yamlMap;
    }

    // 解析yaml文件
    public static void parseConfig() {
        Map<String, Map<String, String>> data = readConfigYaml();
        if (data != null) {
            // 获取值
            Utils.templatePath = data.get("nuclei").get("nucleipath");
            Utils.fofaUrl = data.get("fofa").get("fofaurl");
            Utils.fofaEmail = data.get("fofa").get("fofaemail");
            Utils.fofaKey = data.get("fofa").get("fofakey");
            Utils.hunterUrl = data.get("hunter").get("hunterurl");
            Utils.hunterKey = data.get("hunter").get("hunterkey");
        }
    }

    // 修改yaml文件
    public static void modifyYaml(String type, String value) {
        Map<String, Map<String, String>> data = readConfigYaml();
        if (data != null) {
            // 获取值
            if ("nucleipath".equals(type)) {
                data.get("nuclei").put("nucleipath", value);
            } else if ("fofaurl".equals(type)) {
                data.get("fofa").put("fofaurl", value);
            } else if ("fofaemail".equals(type)) {
                data.get("fofa").put("fofaemail", value);
            } else if ("fofakey".equals(type)) {
                data.get("fofa").put("fofakey", value);
            } else if ("hunterurl".equals(type)) {
                data.get("hunter").put("hunterurl", value);
            } else if ("hunterkey".equals(type)) {
                data.get("hunter").put("hunterkey", value);
            }
            String currentPath = System.getProperty("user.dir");
            String yamlFilePath = currentPath + File.separator + "gatherConfig.yaml";
            try {
                FileWriter fileWriter = new FileWriter(yamlFilePath);
                DumperOptions dumperOptions = new DumperOptions();
                dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
                Yaml yaml = new Yaml(dumperOptions);
                yaml.dump(data, fileWriter);
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
            try {
                file.createNewFile();
                FileWriter fileWriter = new FileWriter(yamlFilePath);
                DumperOptions dumperOptions = new DumperOptions();
                dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
                Yaml yaml = new Yaml(dumperOptions);
                LinkedHashMap<String, Object> data = new LinkedHashMap<>();
                LinkedHashMap<String, Object> nuclei = new LinkedHashMap<>();
                LinkedHashMap<String, Object> fofa = new LinkedHashMap<>();
                LinkedHashMap<String, Object> hunter = new LinkedHashMap<>();
                nuclei.put("nucleipath", "");
                fofa.put("fofaurl", "");
                fofa.put("fofaemail", "");
                fofa.put("fofakey", "");
                hunter.put("hunterurl", "");
                hunter.put("hunterkey", "");
                data.put("nuclei", nuclei);
                data.put("fofa", fofa);
                data.put("hunter", hunter);
                yaml.dump(data, fileWriter);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
