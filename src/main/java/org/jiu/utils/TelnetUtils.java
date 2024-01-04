package org.jiu.utils;


import java.io.*;

public class TelnetUtils {
    // 实现对传入的ip和端口进行telnet测试,生成一个cmd命令,并执行
    public static void telnet(String ip, String port) throws IOException {
        String cmd = "cmd /c telnet " + ip + " " + port;
        // 保存到当前目录的以ip和端口命名的bat文件中
        String batPath = System.getProperty("user.dir") + File.separator + ip + "_" + port + ".bat";
        File file = new File(batPath);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            // 将cmd命令写入bat文件
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(cmd);
            fileWriter.flush();
            fileWriter.close();
            // 执行bat文件
            Process process = Runtime.getRuntime().exec(batPath);
            // 获取执行结果
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

        // 执行bat
        Runtime.getRuntime().exec("cmd /c start " + batPath);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000 * 5);
                    // 删除bat文件
                    file.delete();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();


    }
}
