package com.oriole.motaclient.controller;

import com.alibaba.fastjson.JSONArray;
import com.oriole.motaclient.entity.BusinessException;
import com.oriole.motaclient.entity.MsgEntity;
import com.oriole.motaclient.utils.CommonUtils;
import com.oriole.motaclient.utils.FileToPdf;
import com.oriole.motaclient.utils.ScannerTWAIN;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.io.File;
import java.util.ArrayList;

import static com.oriole.motaclient.Constant.*;
import static com.oriole.motaclient.utils.CommonUtils.zipFiles;

/**
 * 控制扫描仪并对扫描的图片进行进一步处理
 * 此类包含一系列方法将提供文件扫描、扫描获得的系列图片集上传、拷贝等功能
 * 控制端（VUE-MOTA）将根据情况调用若干方法完成最终扫描操作
 * 值得注意的是，RunningScanner方法是必须调用的（需已经安装驱动程序）
 *
 * @author NeoSunJz
 * @version V2.1.3 Beta
 */
@EnableAutoConfiguration
@RestController
public class ScannerController {

    /**
     * 扫描设备初始化
     *
     * @param randomCode   控制端的单次任务标识串（随机字符串）
     * @param serialNumber 当前图片的序列号
     * @return JSON(msgEntity)，成功后msg部分不返回有处理必要的内容
     * @throws Exception
     */
    @RequestMapping(value = "/runningScanner")
    public MsgEntity RunningScanner(@RequestParam String randomCode, @RequestParam String serialNumber) throws Exception {
        Boolean isFinish = new ScannerTWAIN().getScan(randomCode + "_" + serialNumber);
        if (isFinish) {
            return new MsgEntity("SUCCESS", "1", "");
        } else {
            throw new BusinessException("-1", "Scanner init error!");
        }
    }

    /**
     * 复印图像预处理
     * <p>
     * 此方法将根据扫描文件的数量与唯一标识符，调用相关的处理方法将这些文件转换为保真的PDF文件
     * 程序将使用{@link FileToPdf}类png2pdf方法生成PDF
     * 最终保存至PrintFileSavePath下（具体路径参考{@link com.oriole.motaclient.Constant}）
     * 预处理成功后向控制端发送成功返回，否则发送具体错误信息
     *
     * @param totalPage  页面总数
     * @param randomCode 控制端的单次任务标识串（随机字符串）
     * @return JSON(msgEntity)，成功后msg部分不返回有处理必要的内容
     * @throws Exception
     */
    @RequestMapping(value = "/copyPreProcessing", produces = "application/json; charset=utf-8")
    public MsgEntity preProcessing(@RequestParam String totalPage, @RequestParam String randomCode) throws Exception {
        //创建FileToPdf工具类
        FileToPdf fileToPdf = new FileToPdf();

        //检查目标文件夹是否存在
        File checkFolderExists = new File(PrintFileSavePath);
        if (!checkFolderExists.getAbsoluteFile().exists()) {
            checkFolderExists.getAbsoluteFile().mkdirs();
        }

        //获得图片集所有文件名字符串的Array
        JSONArray fileAbsolutePathArray = new JSONArray();
        for (int i = 1; i <= Integer.valueOf(totalPage); i++) {
            fileAbsolutePathArray.add(ScannerFileSavePath + randomCode + "_" + i + ".png");
        }
        //转换图片集为PDF文件
        fileToPdf.png2Pdf(fileAbsolutePathArray, PrintFileSavePath + randomCode + ".pdf");
        return new MsgEntity("SUCCESS", "1", "[MOTA Client] Successful file pre processing");

    }

    /**
     * 拷贝图片至USB存储设备指定目录
     *
     * @param totalPage    页面总数
     * @param randomCode   控制端的单次任务标识串（随机字符串）
     * @param fileCopyPath 指定的文件拷贝目录
     * @return JSON(msgEntity)，成功后msg部分不返回有处理必要的内容
     * @throws Exception
     */
    @RequestMapping(value = "/copyFileToUSB", produces = "application/json; charset=utf-8")
    public MsgEntity copyFileToUSB(@RequestParam String totalPage, @RequestParam String randomCode, @RequestParam String fileCopyPath) throws Exception {
        for (int i = 1; i <= Integer.valueOf(totalPage); i++) {
            CommonUtils.copyFile(ScannerFileSavePath + randomCode + "_" + i + ".png", fileCopyPath + randomCode + "_" + i + ".png");
        }
        return new MsgEntity("SUCCESS", "1", "[MOTA Client] Successful file pre processing");
    }

    /**
     * 上传图片至服务器并获取二维码图片
     *
     * @param totalPage  页面总数
     * @param randomCode 控制端的单次任务标识串（随机字符串）
     * @return String 获取到的二维码图片的BASE64编码
     * @throws Exception
     */
    @RequestMapping(value = "/uploadCopyFileToServer", produces = "application/json; charset=utf-8")
    public String uploadCopyFileToServer(@RequestParam String totalPage, @RequestParam String randomCode) throws Exception {
        //获得图片集所有文件名字符串的Array
        ArrayList<String> copyFilePath = new ArrayList<>();
        for (int i = 1; i <= Integer.valueOf(totalPage); i++) {
            copyFilePath.add(ScannerFileSavePath + randomCode + "_" + i + ".png");
        }
        //将所有图片打包为ZIP
        zipFiles(copyFilePath, ScannerFileSavePath + randomCode + ".zip");

        //上传图片
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            HttpPost httpPost = new HttpPost(Url + UploadFileUrl + "randomCode=" + randomCode + "&deviceID=" + DeviceID);

            RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(200000).setSocketTimeout(200000).build();
            httpPost.setConfig(requestConfig);

            FileBody fileBody = new FileBody(new File(ScannerFileSavePath + randomCode + ".zip"));
            httpPost.setEntity(MultipartEntityBuilder.create().addPart("uploadFile", fileBody).build());

            //获得服务器返回的二维码
            CloseableHttpResponse response = httpclient.execute(httpPost);
            try {
                HttpEntity resEntity = response.getEntity();
                if (resEntity != null) {
                    return EntityUtils.toString(response.getEntity());
                } else {
                    throw new BusinessException("-1", "Upload scan image error!");
                }
            } finally {
                response.close();
            }

        } finally {
            httpclient.close();
        }
    }
}

