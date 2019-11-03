package com.oriole.motaclient.utils;

import com.alibaba.fastjson.JSONWriter;

import java.io.*;

/**
 * JSON文件读取处理类
 *
 * @author NeoSunJz
 * @version V1.0.1 Beta
 */
public class JSONFileIO {

    /**
     * 此方法用于读取JSON文件
     *
     * @param path JSON文件路径（绝对路径）
     * @return 将返回此文件中的字符串（JSON串）
     */
    public static String ReadFile(String path) throws FileNotFoundException {
        BufferedReader reader = null;
        String finalStr = "";
        try {
            FileInputStream fileInputStream = new FileInputStream(path);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, "UTF-8");
            reader = new BufferedReader(inputStreamReader);
            String tempString = null;
            while ((tempString = reader.readLine()) != null) {
                finalStr += tempString;
            }
            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw e;
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally
        {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return finalStr;
    }

    /**
     * 此方法用于写入JSON文件
     *
     * @param savePath JSON文件保存路径（绝对路径，不含文件名）
     * @param savePath JSON文件名
     */
    public static void WriteFile(Object json,String savePath,String fileName) throws IOException {
            File checkFolderExists = new File(savePath);
            if (!checkFolderExists.getAbsoluteFile().exists()) {
                checkFolderExists.getAbsoluteFile().mkdirs();
            }

            JSONWriter writer = new JSONWriter(new FileWriter(savePath + fileName));
            writer.writeObject(json);
            writer.close();
    }
}
