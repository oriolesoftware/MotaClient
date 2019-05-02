package com.oriole.motaclient.entity;

/**
 * 文件信息实体类
 *
 * @author NeoSunJz
 * @version V1.0.1 Beta
 */
public class FileEntity {
    private String filePath;
    private String fileName;
    private String isDirectory;
    private String fileType;

    /**
     * @param filePath 文件绝对路径
     * @param fileName 文件名（含扩展名）
     * @param isDirectory 文件是否为文件夹标志
     * @param fileType 文件真实类型
     */
    public FileEntity(String filePath, String fileName, String isDirectory, String fileType) {
        this.filePath = filePath;
        this.fileName = fileName;
        this.isDirectory = isDirectory;
        this.fileType = fileType;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getIsDirectory() {
        return isDirectory;
    }

    public void setIsDirectory(String isDirectory) {
        this.isDirectory = isDirectory;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
}
