package com.oriole.motaclient.utils;


import java.io.File;
import java.util.Vector;
import java.util.concurrent.Callable;

/**
 * 搜索文件系统的盘符(监听设备插入)
 *
 * @author liuyazhuang（特别感谢）
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
                return fileVector.get(0).getAbsolutePath();
            } else {
                for (int i = roots.length - 1; i >= 0; i--) {
                    sign = false;
                    for (int j = tempFiles.length - 1; j >= 0; j--) {
                        if (tempFiles[j].equals(roots[i])) {
                            sign = true; }
                    }
                    /** 如果前后比较的盘符不相同，表明U盘被拔出 */
                    if (!sign) {
                        System.out.println("[MOTA Client] DISK LISTEN : A device Quit:" + roots[i].toString());
                        fileVector.removeAllElements();
                    }
                }
                roots = File.listRoots();
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}