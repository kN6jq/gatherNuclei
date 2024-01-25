package org.jiu.core;

import cn.hutool.json.JSONObject;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.*;

public class TemplatesCore {
    public static final LinkedList<LinkedHashMap<String, String>> templates = new LinkedList<>(); // 存储模板信息

    public static int getAllTemplatesFromPath(String path) {
        // office template
//        if (Files.exists(Path.of(TemplatesPanel.defaultNucleiTemplatesPath))) {
//            walkFiles(TemplatesPanel.defaultNucleiTemplatesPath);
//        }

        // custom template
        if (Files.exists(Paths.get(path))) {
            try {
                walkFiles(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return templates.size();
    }

    private static void walkFiles(String path) throws IOException {
        Files.walkFileTree(Paths.get(path), new SimpleFileVisitor<Path>() {
            // 访问文件时触发
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (file.toString().endsWith(".yaml")) {
                    try {
                        templates.add(getTemplateInfoFromPath(file.toString()));
                    } catch (Exception e) {
                        // 处理异常
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            // 访问目录时触发
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static LinkedHashMap<String, String> getTemplateInfoFromPath(String path) {
        LinkedHashMap<String, String> templateInfo = new LinkedHashMap<>();
        Map map = getMapFromYaml(path);
        if (map != null) {
            JSONObject jsonObject = new JSONObject(map);
            JSONObject info = jsonObject.getJSONObject("info");

            templateInfo.put("path", path);
            templateInfo.put("id", jsonObject.getStr("id") == null ? "空" : jsonObject.getStr("id"));
            templateInfo.put("name", info.getStr("name"));
            templateInfo.put("severity", info.getStr("severity"));
            templateInfo.put("author", info.getStr("author"));
            templateInfo.put("description", info.getStr("description"));
            templateInfo.put("reference", info.getStr("reference"));
            templateInfo.put("tags", info.getStr("tags"));
        } else {
            templateInfo.put("path", path);
            templateInfo.put("id", "空");
            templateInfo.put("name", "空");
            templateInfo.put("severity", "空");
            templateInfo.put("author", "空");
            templateInfo.put("description", "空");
            templateInfo.put("reference", "空");
            templateInfo.put("tags", "空");
        }
        return templateInfo;
    }

    public static LinkedHashMap getMapFromYaml(String path) {
        if (!Files.exists(Paths.get(path))) {
            try {
                Files.createFile(Paths.get(path));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        LinkedHashMap yamlMap;
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(path);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        // 调基础工具类的方法
        Yaml yaml = new Yaml();
        yamlMap = yaml.loadAs(inputStream, LinkedHashMap.class);
        return yamlMap;
    }

    // 获取选中的模板
    public static void getSelectedTemplateMap() {
        ArrayList<String> template = new ArrayList<>();
        for (LinkedHashMap<String, String> templateInfo : templates) {
            template.add(templateInfo.get("path"));
        }
    }

    // 生成配置文件
    public static String generateNucleiConfigFile(String templateName, LinkedList<String> templates) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String savePath = "";
        if (templateName.isEmpty()) {
            savePath = "nuclei-config-" + format.format(System.currentTimeMillis()) + ".yaml";
        } else {
            savePath = templateName + "-nuclei-config-" + format.format(System.currentTimeMillis()) + ".yaml";
        }

        // 创建DumperOptions实例并设置排序选项
        DumperOptions options = new DumperOptions();
        options.setPrettyFlow(true);
        options.setIndentWithIndicator(true);
        options.setIndent(2);
        options.setIndicatorIndent(2);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);

        Map<String, Object> data = new LinkedHashMap<>();
        List<String> arrayData = new ArrayList<>();
        for (String template : templates) {
            arrayData.add(template);
        }
        data.put("templates", arrayData);

        // 将Map对象转换为YAML格式并写入文件
        try {
            FileWriter writer = new FileWriter(savePath);
            yaml.dump(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 获取文件的绝对路径
        try {
            savePath = new File(savePath).getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return savePath;
    }
}
