package com.oriole.motaclient.utils;

import com.alibaba.fastjson.JSONArray;
import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfWriter;
import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.ComThread;
import com.jacob.com.Dispatch;
import com.jacob.com.Variant;

import java.io.File;
import java.io.FileOutputStream;

/**
 * PDF文件转换类
 * 核心工具类
 *
 * @author NeoSunJz
 * @version V1.0.1 Beta
 */
public class FileToPdf {
    private static final Integer WORD_TO_PDF_OPERAND = 17;
    private static final Integer PPT_TO_PDF_OPERAND = 32;
    private static final Integer EXCEL_TO_PDF_OPERAND = 0;
    public static final int width_A4 = 595;
    public static final int height_A4 = 842;

    /**
     * 此方法用于将WORD文档转换为PDF文件
     * 实现原理为JACOB调用WPS相关COM组件完成无损转换，请参阅WPS开发手册V9.0
     *
     * @param srcFilePath 文档路径（绝对路径）
     * @param pdfFilePath 转换后PDF文件保存路径（绝对路径）
     */
    public void doc2pdf(String srcFilePath, String pdfFilePath) throws Exception {
        ActiveXComponent app = null;
        Dispatch doc = null;
        try {
            ComThread.InitSTA();
            app = new ActiveXComponent("KWPS.Application");
            app.setProperty("Visible", false);
            Dispatch docs = app.getProperty("Documents").toDispatch();
            Object[] obj = new Object[]{
                    srcFilePath,
                    new Variant(false),
                    new Variant(false),//是否只读
                    new Variant(false),
                    new Variant("pwd")
            };
            doc = Dispatch.invoke(docs, "Open", Dispatch.Method, obj, new int[1]).toDispatch();
//          Dispatch.put(doc, "Compatibility", false);  //兼容性检查,为特定值false不正确
            Dispatch.put(doc, "RemovePersonalInformation", false);
            Dispatch.call(doc, "ExportAsFixedFormat", pdfFilePath, WORD_TO_PDF_OPERAND); // word保存为pdf格式宏，值为17

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (doc != null) {
                Dispatch.call(doc, "Close", false);
            }
            if (app != null) {
                app.invoke("Quit", 0);
            }
            ComThread.Release();
        }
    }

    /**
     * 此方法用于将PPT文档转换为PDF文件
     * 实现原理为JACOB调用WPS相关COM组件完成无损转换，请参阅WPS开发手册V9.0
     *
     * @param srcFilePath 文档路径（绝对路径）
     * @param pdfFilePath 转换后PDF文件保存路径（绝对路径）
     */
    public void ppt2pdf(String srcFilePath, String pdfFilePath) throws Exception {
        ActiveXComponent app = null;
        Dispatch ppt = null;
        try {
            ComThread.InitSTA();
            app = new ActiveXComponent("KWPP.Application");
            Dispatch ppts = app.getProperty("Presentations").toDispatch();

            /*
             * call
             * param 4: ReadOnly
             * param 5: Untitled指定文件是否有标题
             * param 6: WithWindow指定文件是否可见
             * */
            ppt = Dispatch.call(ppts, "Open", srcFilePath, true, true, false).toDispatch();
            Dispatch.call(ppt, "SaveAs", pdfFilePath, PPT_TO_PDF_OPERAND); // pptSaveAsPDF为特定值32

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (ppt != null) {
                Dispatch.call(ppt, "Close");
            }
            if (app != null) {
                app.invoke("Quit");
            }
            ComThread.Release();
        }
    }

    /**
     * 此方法用于将EXCEL文档转换为PDF文件
     * 实现原理为JACOB调用WPS相关COM组件完成无损转换，请参阅WPS开发手册V9.0
     *
     * @param srcFilePath 文档路径（绝对路径）
     * @param pdfFilePath 转换后PDF文件保存路径（绝对路径）
     */
    public void excel2Pdf(String srcFilePath, String pdfFilePath) throws Exception {
        ActiveXComponent ax = null;
        Dispatch excel = null;
        try {
            ComThread.InitSTA();
            ax = new ActiveXComponent("KET.Application");
            ax.setProperty("Visible", new Variant(false));
            ax.setProperty("AutomationSecurity", new Variant(3)); // 禁用宏
            Dispatch excels = ax.getProperty("Workbooks").toDispatch();

            Object[] obj = new Object[]{
                    srcFilePath,
                    new Variant(false),
                    new Variant(false)
            };
            excel = Dispatch.invoke(excels, "Open", Dispatch.Method, obj, new int[9]).toDispatch();

            // 转换格式
            Object[] obj2 = new Object[]{
                    new Variant(EXCEL_TO_PDF_OPERAND), // PDF格式=0
                    pdfFilePath,
                    new Variant(0)  //0=标准 (生成的PDF图片不会变模糊) ; 1=最小文件
            };
            Dispatch.invoke(excel, "ExportAsFixedFormat", Dispatch.Method, obj2, new int[1]);

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (excel != null) {
                Dispatch.call(excel, "Close", new Variant(false));
            }
            if (ax != null) {
                ax.invoke("Quit", new Variant[]{});
                ax = null;
            }
            ComThread.Release();
        }
    }

    /**
     * 此方法用于将图片文件转换为PDF文件
     *
     * @param srcFilePath 文档路径（绝对路径）
     * @param pdfFilePath 转换后PDF文件保存路径（绝对路径）
     */
    public void png2Pdf(JSONArray srcFilePath, String pdfFilePath) throws Exception {
        File file = new File(pdfFilePath);
        FileOutputStream out = null;

        Rectangle rect = new Rectangle(width_A4, height_A4);
        Document document = new Document(rect);
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));
        writer.setViewerPreferences(PdfWriter.PageModeUseOutlines);
        document.open();

        for (int index = 0; index < srcFilePath.size(); index++) {
            document.newPage();
            Image image = Image.getInstance((String)srcFilePath.get(index));
            if(image.getWidth()>width_A4||image.getHeight()>height_A4) {
                float imgAspectRatio = image.getWidth() / image.getHeight();
                if (width_A4/height_A4 < imgAspectRatio) {
                    image.scaleAbsolute(width_A4, width_A4/imgAspectRatio);
                }else{
                    image.scaleAbsolute(height_A4*imgAspectRatio, height_A4);
                }

            }
            image.setAbsolutePosition(0, 0);
            document.add(image);
        }
        document.close();
    }
}