package com.lms.cloudpan.service.impl;


import cn.hutool.core.io.FileTypeUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lms.cloudpan.client.OssClient;
import com.lms.cloudpan.config.OssProperties;
import com.lms.cloudpan.constants.HttpCode;
import com.lms.cloudpan.entity.dao.File;
import com.lms.cloudpan.entity.dao.Folder;
import com.lms.cloudpan.entity.dao.User;
import com.lms.cloudpan.entity.dto.FileDto;
import com.lms.cloudpan.entity.vo.DownloadFileVo;
import com.lms.cloudpan.entity.vo.FileVo;
import com.lms.cloudpan.exception.BusinessException;
import com.lms.cloudpan.mapper.FileMapper;
import com.lms.cloudpan.mapper.FolderMapper;
import com.lms.cloudpan.mapper.UserMapper;
import com.lms.cloudpan.service.IFileService;
import com.lms.cloudpan.utis.FileUtil;
import com.lms.cloudpan.utis.MybatisUtils;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class FileServiceImpl extends ServiceImpl<FileMapper, File> implements IFileService {


    @Resource
    private FileMapper fileMapper;


    @Resource
    private FolderMapper folderMapper;

    @Resource
    private OssProperties ossProperties;

    @Resource
    private UserMapper userMapper;

    @Resource
    private OssClient ossClient;

    //上传文件不能大于1G
    private static final Long MAX_FILE_SIZE = 1024L * 1024L * 1024L;

    private static final String STATIC_REQUEST_PREFIX = "static";

    @Override
    public List<FileDto> getUserFileByPath(String path, Integer uid) {
        //先根据path 和uid查找folder_id
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        //一般folder_name 和uid获取的都是唯一的一条记录
        Folder folder = folderMapper.selectOne(new QueryWrapper<Folder>().eq("user_id", uid)
                .eq("folder_name", path));

        if (folder == null) {
            throw new BusinessException(HttpCode.PARAMS_ERROR, "想要获取的文件夹不存在");
        }
        //根据folder_id 和uid 得到满足条件的文件列表
        List<File> files = fileMapper.selectList(new QueryWrapper<File>().eq("folder_id", folder.getFolderId())
                .eq("user_id", uid));

        //mapstruct映射转换
        List<FileDto> result = new ArrayList<>();
        files.forEach(file -> {
            FileDto fileDto = new FileDto();
            BeanUtils.copyProperties(file, fileDto);
            result.add(fileDto);
        });
        return result;
    }

    /**
     * 上传文件
     * 先判断保存的路径是否存在，然后判断文件是否合法（包括文件大小，是否超过用户的配额）
     * 根据上传问文件名生成唯一的路径，然后上传文件，通过返回用户可以访问的url
     * 然后记录文件信息，更新用户配额信息
     *
     * @param fileVo
     * @param user
     * @return
     */
    @Override
    @Transactional
    public boolean insertFile(FileVo fileVo, User user) {
        Integer uid = user.getUserId();
        //先校验文件的路径是否存在
        Folder folder = validPath(fileVo.getFolderPath(), uid);

        //
        MultipartFile file = fileVo.getFile();

        validFile(file, user);
        String bucketName = "bucket_user_" + uid;
        String fileName = file.getOriginalFilename() == null ? file.getName() : file.getOriginalFilename();
        //这里的fileName其实是实际的存储路径
        String filePath;
        try {
            String randomPath =
                    FileUtil.generatorFileName(fileName);
            String datePath = new DateTime().toString("yyyy/MM/dd");

            filePath = StringUtils.isEmpty(datePath) ? randomPath : datePath + "/" + randomPath;

//            log.info("正在执行上传文件，文件为{}",fileName);
            //判断
            ossClient.putObject(bucketName, filePath, file.getInputStream());

        } catch (Exception e) {
//            log.error("文件上传失败，{}",e);
            throw new BusinessException(HttpCode.OPERATION_ERROR, "文件上传失败");
        }

        //拼接访问文件url
        String fileUrl = FileUtil.getFileUrl(ossProperties.getEndpoint(), STATIC_REQUEST_PREFIX, bucketName, filePath);

        //开始记录文件
        File saveFile = new File();
        saveFile.setFileName(fileName);
        saveFile.setFileUrl(fileUrl);
        saveFile.setSize(file.getSize());
        saveFile.setFolderId(folder.getFolderId());
        saveFile.setUserId(uid);

        //获取文件类型
        String type = null;
        String fileString = FileUtil.pathMerge(ossProperties.getRootPath(), bucketName, filePath);
        type = FileTypeUtil.getType(cn.hutool.core.io.FileUtil.file(fileString));

        saveFile.setFileType(type);


        boolean saveFileStatus = this.save(saveFile);

        //修改用户的可用容量
        int updateUserStatus = userMapper.updateById(User.builder()
                .userId(uid).useQuota(file.getSize() + user.getUseQuota()).build());

        //修改文件夹容量
        int updateFolderStatus = folderMapper.updateById(Folder.builder()
                .folderId(folder.getFolderId()).size(folder.getSize() + file.getSize()).build());


        return saveFileStatus && updateUserStatus > 0 && updateFolderStatus > 0;
    }

    @Override
    public boolean renameFile(Integer id, String newName, Integer uid) {
        //先检查是否存在
        File file = fileMapper.selectOne(new QueryWrapper<File>().eq("file_id", id).eq("user_id", uid));
        if (file == null) {
            throw new BusinessException(HttpCode.PARAMS_ERROR, "文件不存在");
        }
        //修改文件名

        return fileMapper.updateById(File.builder().fileId(id).fileName(newName + "." + file.getFileType()).build()) > 0;
    }

    @Override
    public List<FileDto> searchFile(String fileName, Integer uid) {

        List<File> files = fileMapper
                .selectList(new QueryWrapper<File>().eq("user_id", uid).like("file_name", fileName));
        List<FileDto> result = new ArrayList<>();
        files.forEach(file -> {
            FileDto fileDto = new FileDto();
            BeanUtils.copyProperties(file, fileDto);
            result.add(fileDto);
        });
        return result;
    }

    /**
     * 删除多个文件(这里前端都是同一路径下的，所有文件都是一个路径下）
     *
     * @param ids
     * @param user
     * @return
     */
    @Override
    public boolean deleteFiles(List<Integer> ids, User user) {
        Integer uid = user.getUserId();
        //先查找全部的文件
        List<File> filesByIds = fileMapper.selectList(null);
        String bucketName = "bucket_user_" + user.getUserId();

        Integer folderId = filesByIds.get(0).getFolderId();

        Folder folder = folderMapper
                .selectOne(new QueryWrapper<Folder>().eq("folder_id", folderId));
        AtomicLong filesSize = new AtomicLong();
        filesByIds.forEach(file -> {
            filesSize.addAndGet(file.getSize());
        });
        //修改文件夹大小
        folderMapper
                .updateById(Folder.builder().folderId(folderId).size(folder.getSize() - filesSize.get()).build());

        Long useQuota = user.getUseQuota();
        try {
            for (File file : filesByIds) {
                String fileUrl = file.getFileUrl();
                //http://localhost:8080/pan/static/bucket_user_4/root/test.png
                String[] split = fileUrl.split(bucketName);
                ossClient.deleteObject(bucketName, split[1]);
                //删除后修改用户配额
                Map<String, Object> map = new HashMap<>();
                map.put("userId", uid);
                map.put("useQuota", useQuota - file.getSize());
                userMapper.updateById(User.builder().userId(uid).useQuota(useQuota - file.getSize()).build());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException(HttpCode.OPERATION_ERROR, "文件删除失败");
        }
        return fileMapper.deleteBatchIds(ids) > 0;
    }

    /**
     * 将多个文件移到指定文件夹，逻辑移动（本来还需修改文件夹大小，但是前端没实现就不做了）
     *
     * @param ids
     * @param path
     * @param uid
     * @return
     */
    @Override
    public Boolean moveFiles(List<Integer> ids, String path, Integer uid) {
        //首先检查路径是否存在
        //同样是先判断多级文件夹
        int lastIndex = path.lastIndexOf("/");
        path = path.substring(lastIndex);
        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("folderName", "/" + path);
        Folder folder = folderMapper.selectOne(new QueryWrapper<Folder>().eq("folderName", "/" + path));
        if (folder == null) {
            throw new BusinessException(HttpCode.PARAMS_ERROR, "移动到的文件夹不存在");
        }
        //  修改文件信息
        return fileMapper.update(File.builder().folderId(folder.getFolderId()).build(), new QueryWrapper<File>()
                .in("file_id", ids)) > 0;

    }

    @Override
    public byte[] downloadFile(DownloadFileVo downloadFileVo) {
        String url = downloadFileVo.getUrl();
        String[] split = url.split(STATIC_REQUEST_PREFIX);
        // http://localhost:8080/pan/static/bucket_user_4/root/test.png
        // http://localhost:8080/pan/static/temp/4/asdasaads.zip
        String realPath = ossProperties.getRootPath() + split[1];
        byte[] bytes = null;
        try {
            bytes = Files.readAllBytes(Paths.get(realPath));
        } catch (IOException e) {
            throw new BusinessException(HttpCode.OPERATION_ERROR, "下载文件失败");
        }
        return bytes;
    }


    //校验文件是否合法
    private void validFile(MultipartFile file, User user) {
        long size = file.getSize();
//        String fileSuffix = cn.hutool.core.io.FileUtil.getSuffix(file.getOriginalFilename());
        if (size > MAX_FILE_SIZE) {
            throw new BusinessException(HttpCode.PARAMS_ERROR, "文件大小不能超过 1G");
        }
        //判断用户上传这个文件的时候会不会满配额
        Long quota = user.getQuota();
        Long useQuota = user.getUseQuota();
        Long afterQuota = useQuota + file.getSize();
        if (afterQuota >= quota) {
            throw new BusinessException(HttpCode.PARAMS_ERROR, "已超过用户当前配额");
        }
    }

    private Folder validPath(String path, Integer uid) {
        //如果path是一个多级文件夹，获取最后一级的文件夹
        //如，/root/demo/hello
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        Folder folder = folderMapper.selectOne(new QueryWrapper<Folder>().eq("user_id", uid)
                .eq("folder_name", path));


        if (folder == null) {
            throw new BusinessException(HttpCode.PARAMS_ERROR, "要保存到的路径不存在");
        }
        return folder;
    }
}
