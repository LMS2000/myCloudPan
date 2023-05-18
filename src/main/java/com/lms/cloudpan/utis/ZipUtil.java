package com.lms.cloudpan.utis;

import com.lms.cloudpan.config.OssProperties;
import com.lms.cloudpan.entity.dao.File;
import com.lms.cloudpan.entity.dto.FolderDto;
import org.springframework.util.StreamUtils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipUtil {

    /**
     * 在本地创建临时压缩文件夹，然后返回下载地址
     * @param folderDto
     * @param files
     * @param ossProperties
     * @param uid
     * @return
     * @throws IOException
     */
    public static String compressFilesInFolder(FolderDto folderDto, List<File> files,OssProperties ossProperties,Integer uid) throws IOException {

        // 声明压缩文件名，以及临时文件存储路径
        String folderName = folderDto.getFolderName();

        // /root/sad
        int lastIndex =folderName.lastIndexOf("/");

        String zipName = folderName.substring(lastIndex+1)+".zip";
        String tmpPath = ossProperties.getRootPath()+"/temp/"+uid+"/"+zipName;

        java.io.File tempDir=new java.io.File(tmpPath);
        if (!tempDir.getParentFile().exists()) {
            tempDir.getParentFile().mkdirs();
        }
        // 压缩文件
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tmpPath))) {
            // 将文件夹内的所有子文件夹和文件打包压缩到 zip 文件中
            addFolderToZip("", folderDto, zos,files,ossProperties);
        }catch (Exception e){
            e.printStackTrace();
        }

//        // 将临时文件读取到字节数组中
//        byte[] bytes =  Files.readAllBytes(Paths.get(tmpPath));
//
//        // 对byte数组进行Base64编码
//        byte[] encodedBytes = Base64.getEncoder().encode(bytes);
        String zipUrl=ossProperties.getEndpoint()+"/static/temp/"+uid+"/"+zipName;
        return zipUrl;
    }

    // 将文件夹和其内部的所有子文件夹和文件打包压缩到 zip 中
    private static void addFolderToZip(String parentFolderName, FolderDto folder, ZipOutputStream zos,List<File> files,OssProperties ossProperties) throws IOException {

        // 将文件夹本身添加到 zip 中

        String folderName =folder.getFolderName();
        int lastIndex =folderName.lastIndexOf("/");
        if("".equals(parentFolderName)){
            folderName= folderName.substring(lastIndex+1);
        }else{
            folderName= parentFolderName+folderName.substring(lastIndex+1);
        }

        ZipEntry folderEntry = new ZipEntry(folderName + "/");
        zos.putNextEntry(folderEntry);
        zos.closeEntry();

        // 将文件夹内的子文件夹和文件添加到 zip 中
        for (FolderDto subFolder : folder.getChildrenList()) {
            addFolderToZip(folderName + "/", subFolder, zos,files,ossProperties);
        }
        //获取该目录下的子文件
        List<File> subFiles=new ArrayList<>();
        files.stream().forEach(file -> {
            if(folder.getFolderId().equals(file.getFolderId())){
                subFiles.add(file);
            }
        });

        //将文件添加到zip中
        for (File file : subFiles) {
            addFileToZip(folderName + "/", file, zos,ossProperties);
        }
    }

    // 将文件添加到 zip 中
    private static void addFileToZip(String parentFolderName, File file, ZipOutputStream zos,OssProperties ossProperties) throws IOException {
        String fileName = parentFolderName + file.getFileName();
        ZipEntry fileEntry = new ZipEntry(fileName);
        zos.putNextEntry(fileEntry);
        // http://localhost:8080/pan/static/bucket_user_4/root/test.png
        String fileUrl = file.getFileUrl();
        String[] split = fileUrl.split("static");
        String realPath=ossProperties.getRootPath()+split[1];
        try (InputStream is = new FileInputStream(realPath)) {
            StreamUtils.copy(is, zos);
        }
        zos.closeEntry();
    }
}
