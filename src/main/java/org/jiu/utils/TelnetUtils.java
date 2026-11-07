package org.jiu.utils;


import java.io.*;

/**
 * Telnet 连通性测试工具，对外提供 telnet(ip, port) 测试入口。
 */
public class TelnetUtils {
    /**
     * 对指定 IP 和端口做 telnet 测试：在运行目录生成 ip_port.bat 并执行，
     * 再弹出独立 cmd 窗口展示 telnet 交互，后台延迟 5 秒删除临时脚本。
     *
     * @throws IOException 弹出 cmd 窗口失败时抛出
     */
    public static void telnet(String ip, String port) throws IOException {
        String cmd = "cmd /c telnet " + ip + " " + port;
        String batPath = System.getProperty("user.dir") + File.separator + ip + "_" + port + ".bat";
        File file = new File(batPath);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(cmd);
            fileWriter.flush();
            fileWriter.close();
            Process process = Runtime.getRuntime().exec(batPath);
            InputStream inputStream = process.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line = null;
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line + "\n");
            }
            bufferedReader.close();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 用 start 另开可见窗口，让用户看到 telnet 交互界面
        Runtime.getRuntime().exec("cmd /c start " + batPath);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000 * 5);
                    file.delete();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();


    }
}
