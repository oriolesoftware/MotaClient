package com.oriole.motaclient.utils;


import com.alibaba.fastjson.JSONArray;

import java.io.File;
import java.util.Vector;
import java.util.concurrent.Callable;

/**
 * 搜索文件系统的盘符(监听设备插入)
 *
 * @author 孙家正
 * @version V1.0.1 Beta
 */
public class DiskSearchThread implements Callable<String> {

    /** root 现有文件系统的盘符 */
    private File[] roots = File.listRoots();
    /** fileVector 为了遍历U盘内文件 */
    private Vector<File> fileVector = new Vector<File>();
    volatile boolean sign = false;

    public DiskSearchThread() {
    }

    @Override
    public String call() throws Exception{
        while (true) {
            File[] tempFiles = File.listRoots();

            fileVector.removeAllElements();
            /** 检测到了有U盘插入 */
            if (tempFiles.length > roots.length) {
                for (int i = tempFiles.length - 1; i >= 0; i--) {
                    sign = false;
                    for (int j = roots.length - 1; j >= 0; j--) {
                        /** 如果前后比较的盘符相同 */
                        if (tempFiles[i].equals(roots[j])) {
                            sign = true; }
                    }
                    /** 如果前后比较的盘符不相同，将不相同的盘符写入向量，并做进一步处理 */
                    if (!sign) {
                        System.out.println("[MOTA Client] DISK LISTEN : A device Insert:" + tempFiles[i].toString());
                        fileVector.add(tempFiles[i]);
                    }

                }
                roots = File.listRoots();
                JSONArray pathArray =new JSONArray();
                for (int i = 0;i<fileVector.size();i++) {
                    pathArray.add(fileVector.get(i).getAbsolutePath());
                }
                return pathArray.toJSONString();
            }else{
                roots = File.listRoots();
            }
            Thread.sleep(1000);
        }
    }
}