package com.oriole.motaclient.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.oriole.motaclient.utils.ADFileManagement;
import com.oriole.motaclient.utils.CommonUtils;
import com.oriole.motaclient.utils.FileToPdf;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static com.oriole.motaclient.Constant.*;

/**
 * 用于将前端请求重定向至前端，为前端提供某些资源或永久设置参数，或接收前端关于本系统的指令
 *
 * @author NeoSunJz
 * @version V2.1.3 Beta
 */
@Controller
public class InitController {
    /**
     * 将controllerView的请求全部转发至VUE处理
     */
    @RequestMapping("/controllerView/**")
    public String index() {
        return "index_prod";
    }

    /**
     * 前端获取设备ID
     */
    @RequestMapping("/getDeviceID")
    @ResponseBody
    public String getDeviceID(){
        return DeviceID;
    }

    /**
     * 前端获取服务器Url
     */
    @RequestMapping("/getServerUrl")
    @ResponseBody
    public String getServerUrl(){
        return Url;
    }

    /**
     * 前端获取设备密码
     */
    @RequestMapping("/getPassword")
    @ResponseBody
    public String getPassword(){
        return Password;
    }

    /**
     * 前端指令退出本打印系统
     */
    @RequestMapping("/exit")
    public void exit() throws Exception{
        Runtime.getRuntime().exec("taskkill /im chrome.exe /f");
        System.exit(10001);
    }
    /**
     * 前端指令退出本打印系统
     */
    @RequestMapping("/exitAndShutdown")
    public void exitAndShutdown() throws Exception{
        Runtime.getRuntime().exec("shutdown -s -t 60");
        System.exit(10001);
    }
    /**
     * 前端指令退出本打印系统
     */
    @RequestMapping("/exitAndRebooting")
    public void exitAndRebooting() throws Exception{
        Runtime.getRuntime().exec("shutdown -r -t 60");
        System.exit(10001);
    }

    /**
     * 前端获取屏幕广告图片
     */
    @RequestMapping(value = "/getControlScreenADImgList")
    @ResponseBody
    public String getControlScreenADImgList() throws IOException {
        ADFileManagement adFileManagement = ADFileManagement.getInstance();
        JSONArray jsonArray = adFileManagement.getControlScreenImg();
        return jsonArray.toJSONString();
    }

    @RequestMapping(value = "/getControlScreenADImg", produces = MediaType.IMAGE_JPEG_VALUE)
    @ResponseBody
    public byte[] getControlScreenADImg(@RequestParam String picName) throws IOException {
        File file=new File(DownloadADSavePath + picName);
        FileInputStream inputStream = new FileInputStream(file);
        byte[] bytes = new byte[inputStream.available()];
        inputStream.read(bytes, 0, inputStream.available());
        return bytes;
    }
}