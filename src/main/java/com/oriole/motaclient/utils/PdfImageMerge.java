package com.oriole.motaclient.utils;

import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 图片拼接处理类
 *
 * @author NeoSunJz
 * @version V1.0.1 Beta
 */
public class PdfImageMerge {

    /**
     * 此方法用于将广告图片拼接至PDF文件上
     *
     * @param imgPath 广告图片路径数组（绝对路径）
     * @param filePath 待拼合PDF文件路径（绝对路径）
     * @param savePath 拼合完成后新PDF存储路径
     * @param direction 页面打印方向（transverse,lengthwise）
     */
    public static void mergeADImage(List<String> imgPath, String filePath, String savePath ,String direction) throws Exception {
        PdfReader pdfReader = getPdfReader(readFile(filePath));
        PdfStamper stamper = new PdfStamper(pdfReader, new FileOutputStream(savePath));

        //获取文档总页数
        int TotalPages = pdfReader.getNumberOfPages();
        //获取广告总页数
        int TotalADs = imgPath.size();
        int NowAD = 0;
        for (int NowPage = 1; NowPage <= TotalPages; NowPage++) {
            //检查AD是否越界
            if (NowAD >= TotalADs) {
                //若越界则归零
                NowAD = 0;
            }
            PdfContentByte content = stamper.getOverContent(NowPage);
            BufferedImage img_buffered_top = ImageIO.read(new File(imgPath.get(NowAD++))); // 读取该图片
            //再次检查AD是否越界
            if (NowAD >= TotalADs) {
                //若越界则归零
                NowAD = 0;
            }
            BufferedImage img_buffered_bottom = ImageIO.read(new File(imgPath.get(NowAD++)));

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            Image img_top;
            Image img_bottom;
            Rectangle rectangle;
            float height;
            float width;
            switch (direction) {
                case "transverse":
                    //旋转图片
                    img_buffered_top=PdfSplicing.rotateCounterclockwise90(img_buffered_top);
                    img_buffered_bottom=PdfSplicing.rotateCounterclockwise90(img_buffered_bottom);

                    ImageIO.write(img_buffered_top, "png", pngOutputStream);
                    img_top = Image.getInstance(pngOutputStream.toByteArray());
                    pngOutputStream = new ByteArrayOutputStream();
                    ImageIO.write(img_buffered_bottom, "png", pngOutputStream);
                    img_bottom = Image.getInstance(pngOutputStream.toByteArray());

                    //设置缩放比例
                    img_top.scalePercent(32f);
                    img_bottom.scalePercent(32f);
                    //设置位置
                    rectangle = pdfReader.getPageSize(NowPage);
                    height = rectangle.getHeight();
                    width = rectangle.getWidth();
                    img_bottom.setAbsolutePosition(0, ((height-img_top.getScaledHeight())/2));
                    img_top.setAbsolutePosition(width -img_top.getScaledWidth(), ((height-img_top.getScaledHeight())/2));
                    break;
                case "lengthwise":
                    ImageIO.write(img_buffered_top, "png", pngOutputStream);
                    img_top = Image.getInstance(pngOutputStream.toByteArray());
                    pngOutputStream = new ByteArrayOutputStream();
                    ImageIO.write(img_buffered_bottom, "png", pngOutputStream);
                    img_bottom = Image.getInstance(pngOutputStream.toByteArray());
                    //设置缩放比例
                    img_top.scalePercent(32f);
                    img_bottom.scalePercent(32f);
                    //设置位置
                    rectangle = pdfReader.getPageSize(NowPage);
                    height = rectangle.getHeight();
                    img_top.setAbsolutePosition(0, height - img_top.getScaledHeight());
                    img_bottom.setAbsolutePosition(0, 0);
                    break;
                default:
                    throw new Exception();
            }
            content.addImage(img_top);
            content.addImage(img_bottom);
        }
        System.out.println("[MOTA Client] Merge AD completed.");
        stamper.close();
    }

    private static byte[] readFile(String filePath) {
        FileInputStream file = null;
        try {
            file = new FileInputStream(filePath);
            byte[] fileByte = new byte[file.available()];
            file.read(fileByte);

            return fileByte;
        } catch (Exception e) {
            System.out.println(String.format(
                    "[MOTA Client] fail to read file [%s], exception=%s", filePath, e.getMessage()));
        } finally {
            try {
                file.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    private static PdfReader getPdfReader(byte[] document) throws IOException {
        PdfReader pdfReader = new PdfReader(document);
        pdfReader.unethicalreading = true;

        return pdfReader;
    }
}
