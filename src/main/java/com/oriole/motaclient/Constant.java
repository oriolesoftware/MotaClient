package com.oriole.motaclient;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.oriole.motaclient.utils.JSONFileIO;

import java.io.FileNotFoundException;

/**
 * 固定变量
 *
 * @author NeoSunJz
 * @version V1.0.1 Beta
 */
public class Constant {
    public final static String Url;
    public final static String DownloadFileUrl;
    public final static String UploadFileUrl;
    public final static String UpdateADUrl;

    public final static String DeviceID;
    public final static String Password;
    public final static String PrintADCountSavePath;
    public final static String ScannerFileSavePath;
    public final static String PrintFileSavePath;
    public final static String PrintConfigSavePath;
    public final static String DownloadFileSavePath;
    public final static String DownloadADSavePath;
    public final static String PdfPagePicSavePath;

    public final static String PrinterName;

    public final static String WiresharkUSBPcapConfig;
    public final static String ChromeConfig;
    public final static String WiresharkPathConfig;

    public final static Integer defaultMarginImgCount;
    public final static Integer defaultWholePageImgCount;
    public final static Integer defaultControlScreenImgCount;
    public final static Integer defaultADScreenImgOrVideoCount;


    static {
        JSONObject config = null;
        try {
            config = JSONObject.parseObject(JSONFileIO.ReadFile("D:\\temp\\config.json"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        Url = config.getString("Url");
        DownloadFileUrl = config.getString("DownloadFileUrl");
        UploadFileUrl = config.getString("UploadFileUrl");
        UpdateADUrl = config.getString("UpdateADUrl");

        DeviceID = config.getString("DeviceID");
        Password = config.getString("Password");
        PrintADCountSavePath = config.getString("PrintADCountSavePath");
        ScannerFileSavePath = config.getString("ScannerFileSavePath");
        PrintFileSavePath = config.getString("PrintFileSavePath");
        PrintConfigSavePath = config.getString("PrintConfigSavePath");
        DownloadFileSavePath = config.getString("DownloadFileSavePath");
        DownloadADSavePath = config.getString("DownloadADSavePath");
        PdfPagePicSavePath = config.getString("PdfPagePicSavePath");

        PrinterName = config.getString("PrinterName");

        WiresharkUSBPcapConfig = config.getString("WiresharkUSBPcapConfig");
        ChromeConfig = config.getString("ChromeConfig");
        WiresharkPathConfig = config.getString("WiresharkPathConfig");

        defaultMarginImgCount= config.getInteger("defaultMarginImgCount");
        defaultWholePageImgCount= config.getInteger("defaultWholePageImgCount");
        defaultControlScreenImgCount= config.getInteger("defaultControlScreenImgCount");
        defaultADScreenImgOrVideoCount= config.getInteger("defaultADScreenImgOrVideoCount");
    }
}
