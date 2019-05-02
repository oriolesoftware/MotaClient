package com.oriole.motaclient.utils;

import com.alibaba.fastjson.JSON;
import com.itextpdf.text.*;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfWriter;
import com.oriole.motaclient.entity.MsgEntity;
import com.oriole.motaclient.websocket.PreviewLoadingWSServer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;

import static com.oriole.motaclient.Constant.PdfPagePicSavePath;
import static com.oriole.motaclient.Constant.PrintFileSavePath;

/**
 * 一张多页处理类
 * 注意，此类是专用类，不具备泛化可能
 *
 * @author NeoSunJz
 * @version V1.0.1 Beta
 */
public class PdfSplicing {

    public static final int width_A4 = 595;
    public static final int height_A4 = 842;
    private float marginLeft = 40;
    private float marginRight = 40;
    private float marginTop = 30;
    private float marginBottom = 30;
    private float colMargin = 0;
    private float rowMargin = 0;
    private float autoRowMargin = 0;
    private float autoColMargin = 0;

    /**
     * 带参构造方法
     *
     * @param marginLeft 页面左边距
     * @param marginRight 页面右边距
     * @param marginTop 页面上边距
     * @param marginBottom 页面下边距
     * @param colMargin 页面列距
     * @param rowMargin 页面行距
     */
    public PdfSplicing(float marginLeft, float marginRight, float marginTop, float marginBottom, float colMargin, float rowMargin) {
        this.marginLeft = marginLeft;
        this.marginRight = marginRight;
        this.marginTop = marginTop;
        this.marginBottom = marginBottom;
        this.colMargin = colMargin;
        this.rowMargin = rowMargin;
    }

    /**
     * 无参构造方法
     */
    public PdfSplicing() {

    }

    /**
     * 此方法用于拼合一张多页的PDF文件
     *
     *
     * @param fileName 控制端的单次任务标识串（文件名）
     * @param pageCount 页面总数
     * @param col 一张多页列数
     * @param row 一张多页行数
     * @param pageOrder 页面顺序 （Z,anti_Z,N,anti_N）
     * @param direction 页面方向 （transverse,lengthwise）
     */
    public int splicingPdfPage(String fileName, int pageCount, int col, int row, String direction, String pageOrder) throws Exception {
        File file = new File(PdfPagePicSavePath+fileName+".pdf");
        FileOutputStream out = null;
        //建立并打开文档
        Document document = new Document(PageSize.A4, marginLeft, marginRight, marginTop, marginBottom);
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));
        writer.setViewerPreferences(PdfWriter.PageModeUseOutlines);
        document.open();
        //获取待拼合图片byte数组
        ArrayList<byte[]> pageList = imgProcessing(fileName, pageCount, direction);

        //获取图片的纵横比
        ByteArrayInputStream in = new ByteArrayInputStream(pageList.get(0));
        BufferedImage image = ImageIO.read(in);
        image.getWidth();
        image.getHeight();
        float realAspecRatio = (float)image.getWidth() / (float)image.getHeight();

        //页面数总计
        int countPage = pageList.size();
        //拼合后页面数总计
        int countPrintPage = countPage / (row * col);
        //防止转换失去精度问题
        if((float)countPrintPage < (float)countPage / (float)(row * col)){
            countPrintPage++;
        }
        //当前处理页面
        int nowPage = 0;

        //获取页的纵横比
        float pageWidth = ((width_A4 - (marginLeft + marginRight)) - (col - 1) * colMargin) / col;
        float pageHeight = ((height_A4 - (marginTop + marginBottom)) - (row - 1) * rowMargin) / row;
        float pageAspecRatio = pageWidth / pageHeight;


        if (pageAspecRatio < realAspecRatio) {
            //在高度上被拉伸了，因此需重新定义高度
            pageHeight = pageWidth / realAspecRatio;
            autoRowMargin = (height_A4 - (marginTop + marginBottom + (row * pageHeight))) / (row + 1);
        }

        if (pageAspecRatio > realAspecRatio) {
            //在宽度上被拉伸了，因此需重新定义宽度
            pageWidth = pageWidth * realAspecRatio;
            autoColMargin = (width_A4 - (marginLeft + marginRight + (col * pageWidth))) / (col + 1);
        }

        //根据排列方式选择处理
        switch (pageOrder) {
            case "Z":
                for (int nowPrintPage = 0; nowPrintPage < countPrintPage + 1; nowPrintPage++) {
                    document.newPage();
                    //将A4纸沿长边自上而下定义为ROW发展方向，沿短边自左至右定义为COL发展方向
                    for (int nowRow = 1; nowRow < row + 1 && nowPage < countPage; nowRow++) {
                        for (int nowCol = 1; nowCol < col + 1 && nowPage < countPage; nowCol++) {
                            float x;
                            float y;
                            Image img = Image.getInstance(pageList.get(nowPage));
                            y = height_A4 - (marginTop + (nowRow) * pageHeight + rowMargin * (nowRow - 1) + autoRowMargin * nowRow);
                            x = marginLeft + (nowCol - 1) * pageWidth + colMargin * (nowCol - 1) + autoColMargin * nowCol;
                            img.setAbsolutePosition(x, y);
                            img.scaleAbsolute(pageWidth, pageHeight);
                            document.add(img);
                            nowPage++;
                        }
                    }
                }
                break;
            case "anti_Z":
                for (int nowPrintPage = 0; nowPrintPage < countPrintPage + 1; nowPrintPage++) {
                    document.newPage();
                    //将A4纸沿长边自上而下定义为ROW发展方向，沿短边自左至右定义为COL发展方向
                    for (int nowRow = 1; nowRow < row + 1 && nowPage < countPage; nowRow++) {
                        for (int nowCol = col; nowCol > 0 && nowPage < countPage; nowCol--) {
                            float x;
                            float y;
                            Image img = Image.getInstance(pageList.get(nowPage));
                            y = height_A4 - (marginTop + (nowRow) * pageHeight + rowMargin * (nowRow - 1) + autoRowMargin * nowRow);
                            x = marginLeft + (nowCol - 1) * pageWidth + colMargin * (nowCol - 1) + autoColMargin * nowCol;
                            img.setAbsolutePosition(x, y);
                            img.scaleAbsolute(pageWidth, pageHeight);
                            document.add(img);
                            nowPage++;
                        }
                    }
                }
                break;
            case "anti_N":
                for (int nowPrintPage = 0; nowPrintPage < countPrintPage + 1; nowPrintPage++) {
                    document.newPage();
                    //将A4纸沿长边自上而下定义为ROW发展方向，沿短边自左至右定义为COL发展方向
                    for (int nowCol = 1; nowCol < col + 1 && nowPage < countPage; nowCol++) {
                        for (int nowRow = 1; nowRow < row + 1 && nowPage < countPage; nowRow++) {
                            float x;
                            float y;
                            Image img = Image.getInstance(pageList.get(nowPage));
                            y = height_A4 - (marginTop + (nowRow) * pageHeight + rowMargin * (nowRow - 1) + autoRowMargin * nowRow);
                            x = marginLeft + (nowCol - 1) * pageWidth + colMargin * (nowCol - 1) + autoColMargin * nowCol;
                            img.setAbsolutePosition(x, y);
                            img.scaleAbsolute(pageWidth, pageHeight);
                            document.add(img);
                            nowPage++;
                        }
                    }
                }
                break;
            case "N":
                for (int nowPrintPage = 0; nowPrintPage < countPrintPage + 1; nowPrintPage++) {
                    document.newPage();
                    //将A4纸沿长边自上而下定义为ROW发展方向，沿短边自左至右定义为COL发展方向
                    for (int nowCol = col; nowCol > 0 && nowPage < countPage; nowCol--) {
                        for (int nowRow = 1; nowRow < row + 1 && nowPage < countPage; nowRow++) {
                            float x;
                            float y;
                            Image img = Image.getInstance(pageList.get(nowPage));
                            y = height_A4 - (marginTop + (nowRow) * pageHeight + rowMargin * (nowRow - 1) + autoRowMargin * nowRow);
                            x = marginLeft + (nowCol - 1) * pageWidth + colMargin * (nowCol - 1) + autoColMargin * nowCol;
                            img.setAbsolutePosition(x, y);
                            img.scaleAbsolute(pageWidth, pageHeight);
                            document.add(img);
                            nowPage++;
                        }
                    }
                }
                break;
        }
        document.close();
        return countPrintPage;
    }

    /**
     * 此方法用于将BufferedImage类型的图片逆时针旋转90度
     *
     * @param bufferedImage BufferedImage图片矩阵
     */
    public static BufferedImage rotateCounterclockwise90(BufferedImage bufferedImage) {
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        BufferedImage newBufferedImage = new BufferedImage(height, width, bufferedImage.getType());
        for (int i = 0; i < width; i++)
            for (int j = 0; j < height; j++)
                newBufferedImage.setRGB(height - j - 1, i, bufferedImage.getRGB(i, j));
        return newBufferedImage;
    }

    /**
     * 此方法用于将图片预处理并返回处理完毕的图像byte数组的ArrayList
     *
     * @param fileName 待处理PDF文件名（文件需位于PdfPagePicSavePath内）
     * @param pageCount 页面总数
     * @param direction 页面方向 （transverse,lengthwise）
     */
    private static ArrayList<byte[]> imgProcessing(String fileName, int pageCount, String direction) throws Exception {
        ArrayList<byte[]> imageList = new ArrayList<>();
        switch (direction) {
            case "lengthwise":
                for (int page = 0; page < pageCount; page++) {
                    FileInputStream inputStream = new FileInputStream(PdfPagePicSavePath + fileName + "_" + page + ".png");
                    int i = inputStream.available();
                    byte[] imgTemp = new byte[i];
                    inputStream.read(imgTemp);
                    inputStream.close();
                    imageList.add(page, imgTemp);
                }
                break;
            case "transverse":
                for (int page = 0; page < pageCount; page++) {
                    FileInputStream inputStream = new FileInputStream(PdfPagePicSavePath + fileName + "_" + page + ".png");
                    BufferedImage image = ImageIO.read(inputStream);
                    image=rotateCounterclockwise90(image);
                    ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
                    ImageIO.write(image,"png",pngOutputStream);
                    imageList.add(page, pngOutputStream.toByteArray());
                }
                break;
        }
        return imageList;
    }
}
