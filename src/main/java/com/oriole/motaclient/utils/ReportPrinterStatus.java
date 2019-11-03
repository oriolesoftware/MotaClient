package com.oriole.motaclient.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.FileNotFoundException;

import static com.oriole.motaclient.Constant.*;
import static com.oriole.motaclient.utils.CommonUtils.getResponse;
import static com.oriole.motaclient.utils.CommonUtils.postResponse;

public class ReportPrinterStatus {
    public static Boolean reportPrintingProcess(String deviceID,String randomCode, JSONObject data) throws Exception{
        String finalUrl=Url + "device/reportPrintingProcess?";
        data.put("deviceID",deviceID);
        data.put("randomCode",randomCode);
        String result=getResponse(finalUrl,data);
        JSONObject jsonObject = JSONObject.parseObject(result);
        if (jsonObject.get("state").equals("SUCCESS")) {
            return true;
        }else{
            return false;
        }
    }

    public static Boolean reportPrintingError(String deviceID, String randomCode, String errCode, String errMsg) throws Exception{
        JSONObject data=new JSONObject();
        data.put("deviceID",deviceID);
        data.put("randomCode",randomCode);
        data.put("errCode",errCode);
        data.put("errMsg",errMsg);
        String result=getResponse(Url + "device/reportPrintingError?",data);
        JSONObject jsonObject = JSONObject.parseObject(result);
        if (jsonObject.get("state").equals("SUCCESS")) {
            return true;
        }else{
            return false;
        }
    }

    public static Boolean reportADPrintingResult(String randomCode) throws Exception{
        JSONObject data=new JSONObject();
        try {
            String dataStr=JSONFileIO.ReadFile(PrintADCountSavePath+randomCode+".json");
            data.put("adPrintingList",dataStr);
            String result=postResponse(Url + "advertisement/adPrintRun?", data);
            JSONObject jsonObject = JSONObject.parseObject(result);
            if (jsonObject.get("state").equals("SUCCESS")) {
                return true;
            }else{
                return false;
            }
        }catch (FileNotFoundException e){
            //若文件无法找到则为用户付费打印，无需做广告记录
            return true;
        }
    }
}
