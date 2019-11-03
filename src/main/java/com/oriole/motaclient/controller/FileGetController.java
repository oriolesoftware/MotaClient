package com.oriole.motaclient.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.oriole.motaclient.entity.FileEntity;
import com.oriole.motaclient.entity.MsgEntity;
import com.oriole.motaclient.utils.DiskSearchThread;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.oriole.motaclient.Constant.*;
import static com.oriole.motaclient.utils.CommonUtils.DownloadFromServer;

/**
 * 获得需要打印的文件。
 * 此类包含的三个方法将提供自服务器下载并保存文件、监听USB设备插入与从USB设备选择文件的功能
 * 控制端（VUE-MOTA）将根据情况调用这三个方法获得用户需要打印的文件
 *
 * @author NeoSunJz
 * @version V2.1.3 Beta
 */
@EnableAutoConfiguration
@RestController
public class FileGetController {

    /**
     * 从服务器下载文件
     * <p>
     * 此方法将从Url+DownloadFileUrl（资源获得链接，服务器）处，利用fileName参数获取用户上传的文件
     * 并将获得的文件以控制端randomCode为文件名存入DownloadFileSavePath处（具体路径参考{@link com.oriole.motaclient.Constant}）
     * 下载成功后向控制端发送成功返回，否则发送具体错误信息
     *
     * @param fileName   控制端从服务器获取的服务器暂存文件名
     * @param randomCode 控制端的单次任务标识串（随机字符串）
     * @return JSON(msgEntity)，成功将在msg部分返回文件实体类{@link FileEntity}的JSON串
     */
    @RequestMapping(value = "/download", produces = "application/json; charset=utf-8")
    public MsgEntity doDownload(@RequestParam String fileName, @RequestParam String randomCode) throws Exception {
        String url = Url + DownloadFileUrl + "fileName=" + fileName + "&location=PrintFileUploadTemp";
        String path = DownloadFileSavePath + randomCode + fileName.substring(fileName.lastIndexOf("."));
        File temp = DownloadFromServer(path, url);
        FileEntity fileEntity = new FileEntity(temp.getAbsolutePath().replace("\\", "\\\\"), fileName, "false", fileName.substring(fileName.lastIndexOf(".") + 1));
        return new MsgEntity("SUCCESS", "1", JSON.toJSONString(fileEntity));
    }

    /**
     * 监听USB驱动器是否连入设备
     * <p>
     * 此方法将开启{@link DiskSearchThread}线程，并从线程获得接入设备的diskPath
     * 如果获得，则向控制端发送diskPath，除非错误向控制端发送具体错误信息，否则不返回数据直到连接超时
     * <p>
     * 此方法无参数。
     *
     * @return JSON(msgEntity)，成功将在msg部分返回此USB设备的路径
     */
    @RequestMapping(value = "/listenUSBInsertion", produces = "application/json; charset=utf-8")
    public MsgEntity listenUSBInsertion() throws Exception {

        DiskSearchThread diskSearchThread = new DiskSearchThread();
        ExecutorService mExecutorService = Executors.newSingleThreadExecutor();
        Future<String> mFuture = mExecutorService.submit(diskSearchThread);

        String diskPath = mFuture.get();
        return new MsgEntity("SUCCESS", "1", diskPath);

    }

    /**
     * 获得文件信息列表
     * <p>
     * 此方法将首先判断路径是否为文件夹，若不是则立刻返回文件信息（文件实体类）
     * 若是文件夹，则对该路径下文件进行遍历，若当前遍历文件是文件夹，则将其文件信息加入ArrayList
     * 若不是，但是以DOC,DOCX,PPT,PPTX,ELS,ELSX,PNG,BMP,JPG为后缀的文件，将其文件信息加入ArrayList
     * 若遍历完毕，向控制端发送成功返回，否则发送具体错误信息
     *
     * @param path 欲获取文件信息的绝对路径
     * @return JSON(msgEntity)，成功将在msg部分返回文件实体类{@link FileEntity}的JSON数组
     */
    @RequestMapping(value = "/getFiles", produces = "application/json; charset=utf-8")
    public MsgEntity getFiles(@RequestParam String path) throws Exception {

        JSONArray fileInfoArray = new JSONArray();
        File file = new File(path);
        if (!file.isDirectory()) {
            FileEntity fileEntity = new FileEntity(file.getAbsolutePath(), file.getName(), "false", file.getName().substring(file.getName().lastIndexOf(".") + 1));
            fileInfoArray.add(fileEntity);
        } else{
            //建立数组存储该路径下的文件列表
            String[] fileList = file.list();
            for (String s : fileList) {
                //遍历读取文件，以readFile为临时存储变量
                File readFile = new File(path + "\\" + s);
                //判断取到的文件类型
                if (!readFile.isDirectory()) {
                    String fileType = readFile.getName().substring(readFile.getName().lastIndexOf(".") + 1);
                    if ((fileType.equals("doc") || fileType.equals("docx") || fileType.equals("xls")
                            || fileType.equals("xlsx") || fileType.equals("ppt") || fileType.equals("pptx")
                            || fileType.equals("pdf") || fileType.equals("png") || fileType.equals("jpg")
                            || fileType.equals("bmp"))&&(!readFile.getName().contains("~$"))) {
                        FileEntity fileEntity = new FileEntity(readFile.getAbsolutePath(), readFile.getName(), "false", fileType);
                        fileInfoArray.add(fileEntity);
                    }
                } else {
                    FileEntity fileEntity = new FileEntity(readFile.getAbsolutePath(), readFile.getName(), "true", "Directory");
                    fileInfoArray.add(fileEntity);
                }
            }
        }
        //成功则返回JSON数组，由控制端处理
        return new MsgEntity("SUCCESS", "1", fileInfoArray.toJSONString());
    }
}
