package com.oriole.motaclient.utils;

import com.alibaba.fastjson.JSONObject;
import com.oriole.motaclient.entity.BusinessException;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 一般性工具类
 *
 * @author NeoSunJz
 * @version V1.0.1 Beta
 */
public class CommonUtils {
    /**
     * 此方法用于通过GET方法获得URL请求的内容
     *
     * @param urlStr 请求URL路径
     * @param parameter 请求参数
     * @return 读取到请求的字符串result
     */
    public static String getResponse(String urlStr,JSONObject parameter) throws Exception {
        HttpClient client = new DefaultHttpClient();
        String parameterStr="";
        for(String str:parameter.keySet()){
            parameterStr+=str + "=" +parameter.get(str)+"&";
        }
        HttpGet get = new HttpGet(urlStr+parameterStr);
        String result = "";
            // 发送请求
            HttpResponse httpResponse = client.execute(get);
            // 获取响应输入流
            InputStream inStream = httpResponse.getEntity().getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inStream, "utf-8"));
            StringBuilder strber = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null)
                strber.append(line + "\n");
            inStream.close();
            result = strber.toString();
        return result;
    }

    /**
     * 此方法用于通过Post方法获得URL请求的内容
     *
     * @param urlStr 请求URL路径
     * @param data 提交的相关数据（JSONObject）
     * @return 读取到请求的字符串result
     */
    public static String postResponse(String urlStr, JSONObject data) throws Exception{
        HttpClient client = new DefaultHttpClient();
        HttpPost post = new HttpPost(urlStr);
        post.setHeader("Content-Type", "application/json");
        String result = "";
            StringEntity s = new StringEntity(data.toString(), "utf-8");
            s.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE,"application/json"));
            post.setEntity(s);
            // 发送请求
            HttpResponse httpResponse = client.execute(post);
            // 获取响应输入流
            InputStream inStream = httpResponse.getEntity().getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inStream, "utf-8"));
            StringBuilder strber = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null)
                strber.append(line + "\n");
            inStream.close();
            result = strber.toString();
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
    public static void copyFile(String fileAbsolutePath, String fileCopyPath) throws Exception {
        FileInputStream fis = new FileInputStream(new File(fileAbsolutePath));
        File outPutFile=new File(fileCopyPath);
        File outPutPath=new File(outPutFile.getParent());
        if (!outPutPath.exists()) {
            outPutPath.mkdirs();
        }
        FileOutputStream fos = new FileOutputStream(outPutFile);
        byte[] buf = new byte[1024];
        int i;
        while ((i = fis.read(buf)) != -1) {
            fos.write(buf, 0, i);
        }
        fis.close();
        fos.close();
    }

    /**
     * 此方法用于删除单个文件
     *
     * @param fileName
     *            要删除的文件的文件名
     * @return 单个文件删除成功返回true，否则返回false
     */
    public static boolean deleteFile(String fileName) throws Exception {
        File file = new File(fileName);
        // 如果文件路径所对应的文件存在，并且是一个文件，则直接删除
        if (file.exists() && file.isFile()) {
            if (file.delete()) {
                System.out.println("[MOTA Client] File "+fileName+" deleted successfully");
                return true;
            } else {
                System.out.println("[MOTA Client] File "+ fileName +" deleted failed");
                return false;
            }
        } else {
            System.out.println("[MOTA Client] File " + fileName + "is not exist");
            return false;
        }
    }


    /**
     * 此方法用于将16进制字符串转换为String
     *
     * @param hex 待转换的HEX
     * @return 对应字符串
     */
    public static String hexToString(String hex)throws Exception {
        /*兼容带有\x的十六进制串*/
        hex = hex.replace("\\x", "");
        char[] data = hex.toCharArray();
        int len = data.length;
        if ((len & 0x01) != 0) {
            throw new BusinessException("-1","The number of characters should be even!");
        }
        byte[] out = new byte[len >> 1];
        for (int i = 0, j = 0; j < len; i++) {
            int f = toDigit(data[j], j) << 4;
            j++;
            f |= toDigit(data[j], j);
            j++;
            out[i] = (byte) (f & 0xFF);
        }
        return new String(out);
    }

    private static int toDigit(char ch, int index) {
        int digit = Character.digit(ch, 16);
        if (digit == -1) {
            throw new RuntimeException("Illegal hexadecimal character " + ch + " at index " + index);
        }
        return digit;
    }


    /**
      文件压缩
     */
    public static void zipFiles(ArrayList<String> filePathList, String zipFilePath)throws Exception {
        byte[] buf = new byte[1024];
        ZipOutputStream out = null;
        try {
            out = new ZipOutputStream(new FileOutputStream(zipFilePath));
            for (int i = 0; i < filePathList.size(); i++) {
                File file=new File(filePathList.get(i));
                FileInputStream in = new FileInputStream(file);
                out.putNextEntry(new ZipEntry(file.getName()));
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.closeEntry();
                in.close();
            }
            out.close();
        } catch (IOException e) {
            throw e;
        } finally {
            try {
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
