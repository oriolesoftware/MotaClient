package com.oriole.motaclient.utils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * 一般性工具类
 *
 * @author NeoSunJz
 * @version V1.0.1 Beta
 */
public class CommonUtils {
    /**
     * 此方法用于获得URL请求的内容
     *
     * @param urlStr 请求URL路径
     * @return 读取到请求的字符串result
     */
    public static String getURLContent(String urlStr) {
        //请求的url
        URL url = null;
        //建立的http链接
        HttpURLConnection httpConn = null;
        //请求的输入流
        BufferedReader bufferedReader = null;
        //输入流的缓冲
        StringBuffer stringBuffer = new StringBuffer();
        try {
            url = new URL(urlStr);
            bufferedReader = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
            String str = null;
            //一行一行进行读入
            while ((str = bufferedReader.readLine()) != null) {
                stringBuffer.append(str);
            }
        } catch (Exception ex) {
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close(); //关闭流
                }
            } catch (IOException ex) {
            }
        }
        String result = stringBuffer.toString();
        return result;
    }

    /**
     * 此方法用于从URL获取文件
     *
     * @param downloadFileSavePath 下载文件的储存目录
     * @param downloadURL 获取文件的URL地址
     * @return 下载文件的File类型变量（用于提取文件信息或对文件进行操作）
     */
    public static File DownloadFromServer(String downloadFileSavePath, String downloadURL) throws Exception {
        File file = null;
        // 统一资源
        java.net.URL url = new URL(downloadURL);
        // 连接类的父类，抽象类
        URLConnection urlConnection = url.openConnection();
        // http的连接类
        HttpURLConnection httpURLConnection = (HttpURLConnection) urlConnection;
        // 设定请求的方法，默认是GET
        httpURLConnection.setRequestMethod("POST");
        // 设置字符编码
        httpURLConnection.setRequestProperty("Charset", "UTF-8");
        // 打开到此 URL 引用的资源的通信链接（如果尚未建立这样的连接）。
        httpURLConnection.connect();

        URLConnection con = url.openConnection();
        BufferedInputStream bin = new BufferedInputStream(httpURLConnection.getInputStream());

        file = new File(downloadFileSavePath);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        OutputStream out = new FileOutputStream(file);
        int size = 0;
        int len = 0;
        byte[] buf = new byte[1024];
        while ((size = bin.read(buf)) != -1) {
            len += size;
            out.write(buf, 0, size);
        }
        bin.close();
        out.close();
        return file;
    }

    /**
     * 此方法用于拷贝文件
     *
     * @param fileAbsolutePath 欲拷贝的文件路径（绝对路径）
     * @param fileCopyPath 拷贝目的路径（绝对路径）
     */
    public static void CopyFile(String fileAbsolutePath, String fileCopyPath) throws Exception {
        FileInputStream fis = new FileInputStream(new File(fileAbsolutePath));
        FileOutputStream fos = new FileOutputStream(new File(fileCopyPath));
        byte[] buf = new byte[1024];
        int i;
        while ((i = fis.read(buf)) != -1) {
            fos.write(buf, 0, i);
        }
        fis.close();
        fos.close();
    }
}
