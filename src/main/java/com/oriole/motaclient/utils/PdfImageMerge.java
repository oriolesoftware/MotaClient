package com.oriole.motaclient.utils;

import com.alibaba.fastjson.JSONObject;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import javafx.util.Pair;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.oriole.motaclient.Constant.PrintADCountSavePath;
import static com.oriole.motaclient.Constant.PrintConfigSavePath;

/**
 * 图片拼接处理类
 *
 * @author NeoSunJz
 * @version V1.0.1 Beta
 */
public class PdfImageMerge {

    private int NowMarginAD = 0;
    private int totalPages =0;
    //广告插入统计
    private JSONObject printADCountList = new JSONObject();

    /**
     * 此方法用于将广告图片拼接至PDF文件上
     *
     * @param marginADPath 页边距广告图片路径数组（绝对路径）
     * @param randomCode
     * @param filePath     待拼合PDF文件路径（绝对路径）
     * @param savePath     拼合完成后新PDF存储路径
     * @param direction    页面打印方向（transverse,lengthwise）
     */
    public void mergeADImage(ArrayList<Pair<Integer, String>> marginADPath,ArrayList<Pair<Integer, String>> wholePageADPath, String randomCode, String filePath, String savePath, String direction) throws Exception {
        PdfReader pdfReader = getPdfReader(readFile(filePath));
        PdfStamper stamper = new PdfStamper(pdfReader, new FileOutputStream(savePath));

        //获取文档总页数
        totalPages = pdfReader.getNumberOfPages();

        for (int NowPage = 1; NowPage <= totalPages; NowPage++) {
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            Rectangle rectangle = pdfReader.getPageSize(NowPage);
            float height = rectangle.getHeight();
            float width = rectangle.getWidth();

            if(NowPage%6==0){
                stamper.insertPage(NowPage,pdfReader.getPageSizeWithRotation(NowPage));
                BufferedImage img_buffered = getADPic(wholePageADPath);

                ImageIO.write(img_buffered, "png", pngOutputStream);
                Image img = Image.getInstance(pngOutputStream.toByteArray());
                img.setAbsolutePosition(0, 0);
                img.scaleAbsolute(width,height);

                stamper.getOverContent(NowPage).addImage(img);
                totalPages++;

            }else {

                PdfContentByte content = stamper.getOverContent(NowPage);

                BufferedImage img_buffered_top = getADPic(marginADPath);
                BufferedImage img_buffered_bottom = getADPic(marginADPath);

                //旋转图片
                if (direction.equals("transverse")) {
                    img_buffered_top = PdfSplicing.rotateCounterclockwise90(img_buffered_top);
                    img_buffered_bottom = PdfSplicing.rotateCounterclockwise90(img_buffered_bottom);
                }

                ImageIO.write(img_buffered_top, "png", pngOutputStream);
                Image img_top = Image.getInstance(pngOutputStream.toByteArray());
                pngOutputStream = new ByteArrayOutputStream();
                ImageIO.write(img_buffered_bottom, "png", pngOutputStream);
                Image img_bottom = Image.getInstance(pngOutputStream.toByteArray());

                //设置缩放比例
                img_top.scalePercent(32f);
                img_bottom.scalePercent(32f);
                //设置位置
                if (direction.equals("transverse")) {
                    img_bottom.setAbsolutePosition(0, ((height - img_top.getScaledHeight()) / 2));
                    img_top.setAbsolutePosition(width - img_top.getScaledWidth(), ((height - img_top.getScaledHeight()) / 2));
                } else {
                    img_top.setAbsolutePosition(0, height - img_top.getScaledHeight());
                    img_bottom.setAbsolutePosition(0, 0);
                }

                content.addImage(img_top);
                content.addImage(img_bottom);
            }
        }
        System.out.println("[MOTA Client] Merge AD completed.");
        JSONFileIO.WriteFile(printADCountList, PrintADCountSavePath, randomCode + ".json");
        stamper.close();
    }

    private BufferedImage getADPic(ArrayList<Pair<Integer, String>> marginADPath) throws Exception{
        //从广告图片数组中循环取出单张图片
        Pair<Integer, String> tempMarginAD = marginADPath.get(NowMarginAD++ % marginADPath.size());
        //获得广告本次印量
        Integer printADCount = (Integer) printADCountList.getOrDefault(tempMarginAD.getKey(), 0);
        //更新印量
        printADCountList.put(tempMarginAD.getKey().toString(), printADCount + 1);
        //返回广告图片BufferedImage
        return ImageIO.read(new File(tempMarginAD.getValue()));
    }

    private byte[] readFile(String filePath) throws Exception {
        FileInputStream file = null;
        try {
            file = new FileInputStream(filePath);
            byte[] fileByte = new byte[file.available()];
            file.read(fileByte);

            return fileByte;
        } finally {
            file.close();
        }
    }

    private static PdfReader getPdfReader(byte[] document) throws Exception {
        PdfReader pdfReader = new PdfReader(document);
        pdfReader.unethicalreading = true;
        return pdfReader;
    }
}
