package com.lms.cloudpan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lms.cloudpan.client.OssClient;
import com.lms.cloudpan.config.OssProperties;
import com.lms.cloudpan.constants.HttpCode;
import com.lms.cloudpan.entity.dao.File;
import com.lms.cloudpan.entity.dao.Folder;
import com.lms.cloudpan.entity.dao.User;
import com.lms.cloudpan.entity.dto.FolderDto;
import com.lms.cloudpan.entity.vo.FolderVo;
import com.lms.cloudpan.exception.BusinessException;
import com.lms.cloudpan.mapper.FileMapper;
import com.lms.cloudpan.mapper.FolderMapper;
import com.lms.cloudpan.mapper.UserMapper;
import com.lms.cloudpan.service.IFolderService;
import com.lms.cloudpan.utis.ZipUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@Slf4j
public class FolderServiceImpl extends ServiceImpl<FolderMapper,Folder> implements IFolderService {


    @Resource
    private FolderMapper folderMapper;



    @Resource
    private FileMapper fileMapper;


    @Resource
    private OssProperties ossProperties;

    @Resource
    private OssClient ossClient;

    @Resource
    private UserMapper userMapper;


    //获取文件夹树
    @Override
    public List<FolderDto> getUserFolder(String path,Integer userId) {

        Map<String,Object> queryMap=new HashMap<>();
        queryMap.put("userId",userId);
        if(!path.startsWith("/")){
            path="/"+path;
        }
        queryMap.put("folderName",path);
        Folder folder = folderMapper.selectOne(new QueryWrapper<Folder>().eq("user_id",userId)
                .eq("folder_name",path));
        if(folder==null){
            throw new BusinessException(HttpCode.PARAMS_ERROR,"获取用户当前路径的父文件夹失败");
        }
        List<Folder> subFolders = folderMapper.selectList(new QueryWrapper<Folder>().eq("user_id",userId)
                .eq("parent_folder",folder.getFolderId()));


        //获取全部的文件夹集合
        List<FolderDto> temp=new ArrayList<>();
        subFolders.stream().forEach(subFolder -> {
            FolderDto folderDto=new FolderDto();
            BeanUtils.copyProperties(subFolder,folderDto);
            temp.add(folderDto);
        });
        return temp;

//        //获取全部的顶级文件夹
//        //获取一级的分类
//        List<FolderDto> resultList= temp.stream()
//                .filter(folderDto -> folderDto.getParentFolder()==0)
//                .map(folderDto -> {
//                    folderDto.setChildernList(getChildrenList(folderDto,temp));
//                    return folderDto;
//                })
//                .collect(Collectors.toList());
//        return resultList;
    }


    @Override
    public Boolean insertFolder(FolderVo folderVo, Integer uid) {
        //先检查是否已存在文件
        String path=folderVo.getPath();
        if(!path.startsWith("/")){
            path="/"+path;
        }
        Folder folder = folderMapper.selectOne(new QueryWrapper<Folder>().eq("user_id",uid)
                .eq("folder_name",path));
        if(folder!=null){
            throw new BusinessException(HttpCode.PARAMS_ERROR,"文件夹已存在");
        }
        //查看父级文件夹是否存在
        Map<String,Object> map=new HashMap<>();
        map.put("userId",uid);
        map.put("folderName",folderVo.getParentPath());
        Folder parentFolder= folderMapper.selectOne(new QueryWrapper<Folder>().eq("user_id",uid)
                .eq("folder_name",folderVo.getParentPath()));
        if(parentFolder==null){
            throw new BusinessException(HttpCode.PARAMS_ERROR,"父级文件夹不存在");
        }
       return folderMapper.insert(Folder.builder()
               .userId(uid).folderName(parentFolder.getFolderName()+path)
               .parentFolder(parentFolder.getFolderId()).build())>0;
    }

    /**
     * 修改文件夹名
     * @param folderId
     * @param folderName
     * @param uid
     * @return
     */
    @Override
    public Boolean renameFolder(Integer folderId, String folderName, Integer uid) {
        //检查文件夹是否存在
        Folder folder = folderMapper.selectOne(new QueryWrapper<Folder>().eq("user_id",uid)
                .eq("folder_id",folderId));
        if(folder==null){
            throw new BusinessException(HttpCode.PARAMS_ERROR,"修改的文件夹不存在");
        }
        if(!folderName.startsWith("/")){
            folderName="/"+folderName;
        }
        return folderMapper.updateById(Folder.builder()
                .folderId(folderId).folderName(folderName).build())>0;
    }

    @Override
    @Transactional
    public Boolean deleteFolder(Integer folderId, User user) {
        Integer uid = user.getUserId();
        //查找文件夹是否存在
        Folder folder = folderMapper.selectOne(new QueryWrapper<Folder>().eq("user_id",uid)
                .eq("folder_id",folderId));
        if(folder==null){
            throw new BusinessException(HttpCode.PARAMS_ERROR,"要删除的文件夹不存在");
        }


        //查询用户所有的文件夹
        List<FolderDto> folderNode = getFolderNode(uid);

        if(folderNode!=null&&folderNode.size()>1){
            throw new BusinessException(HttpCode.PARAMS_ERROR,"用户只有一个根目录");
        }
        //获取子文件夹树
        FolderDto subFolders = getPath(folder.getFolderName(), folderNode.get(0));

        //获取用户的全部文件
        List<File> uesrFiles = fileMapper.selectList(new QueryWrapper<File>().eq("user_id",uid));
        //删除
        deleteSubFile(subFolders,uesrFiles);

        //修改用户配额
        userMapper.updateById(User.builder().userId(uid)
                .useQuota(user.getUseQuota()-folder.getSize()).build());
        return folderMapper.deleteById(folderId)>0;
    }

    private void deleteSubFile(FolderDto folderDto,List<File> files){
        // 将文件夹内的子文件夹和文件添加到 zip 中
        for (FolderDto subFolder : folderDto.getChildrenList()) {
            deleteSubFile( subFolder,files);
        }
        //删除文件夹（文件夹为虚拟路径，没有实际创建，直接删记录就好了）
        folderMapper.deleteById(folderDto.getFolderId());

        //获取该目录下的子文件
        List<File> subFiles=new ArrayList<>();
        files.stream().forEach(file -> {
            if(folderDto.getFolderId().equals(file.getFolderId())){
                subFiles.add(file);
            }
        });
        //删除其下的文件
        List<Integer> fileIds=subFiles.stream().map(File::getFileId).collect(Collectors.toList());
        Integer userId = subFiles.get(0).getUserId();
        fileMapper.delete(new QueryWrapper<File>().in("file_id"));

        //删除实际的文件

        List<String> urlList = subFiles.stream().map(File::getFileUrl).collect(Collectors.toList());
        String bucketName = "bucket_user_" + userId;
        urlList.stream().forEach(url->{
            String[] split = url.split(bucketName);
            ossClient.deleteObject(bucketName,split[1]);
        });

    }






    //下载文件夹（打包压缩包）
    @Override
    public String downloadFolder(String path, Integer uid)  {
          //首先查看文件夹是否存在
        Folder folder = folderMapper.selectOne(new QueryWrapper<Folder>().eq("user_id",uid)
                .eq("folder_name",path));
        if(folder==null){
            throw new BusinessException(HttpCode.PARAMS_ERROR,"要删除的文件夹不存在");
        }

        //查出用户的子文件夹
        List<FolderDto> folderNode = getFolderNode(uid);

        //获取目标的子文件夹

        if(folderNode!=null&&folderNode.size()>1){
            log.error("下载{}时发现多个根目录",path);
        }
        //获取目标文件及其子文件夹
        assert folderNode != null;
        FolderDto subFolders = getPath(path, folderNode.get(0));

        //获取目标用户所有的文件
        List<File> files = fileMapper.selectList(new QueryWrapper<File>().eq("user_id",uid));
        String zipUrl=null;
        try {
            zipUrl = ZipUtil.compressFilesInFolder(subFolders, files,ossProperties,uid);
        } catch (IOException e) {
          throw new BusinessException(HttpCode.OPERATION_ERROR,"压缩文件夹失败！");
        }

        return zipUrl;
    }




    @Override
    public List<FolderDto> getFolderList(Integer uid) {
        return getFolderNode(uid);
    }





    private List<FolderDto> getFolderNode(Integer uid){
        List<Folder> list1 = folderMapper.selectList(new QueryWrapper<Folder>().eq("user_id",uid));
        List<FolderDto> temp=new ArrayList<>();
        list1.stream().forEach(folder -> {
            FolderDto folderDto=new FolderDto();
            BeanUtils.copyProperties(folder,folderDto);
            temp.add(folderDto);
        });

                //获取全部的顶级文件夹
        //获取一级的分类
        List<FolderDto> resultList= temp.stream()
                .filter(folderDto -> folderDto.getParentFolder()==0)
                .map(folderDto -> {
                    folderDto.setChildrenList(getChildrenList(folderDto,temp));
                    return folderDto;
                })
                .collect(Collectors.toList());
        return resultList;
    }
    private List<FolderDto> getChildrenList(FolderDto cur,List<FolderDto> allList){
        List<FolderDto> res=allList.stream()
                .filter(categoryEntity -> categoryEntity.getParentFolder().equals(cur.getFolderId()))
                .map(folderDto -> {
                    folderDto.setChildrenList(getChildrenList(folderDto,allList));
                    return folderDto;
                }).collect(Collectors.toList());
        return res;
    }





    //多叉树遍历问题，每个用户只有一个根路径，每个路径都有若干个子路径
    public  FolderDto getPath(String path,FolderDto folderNode){
        if(folderNode==null)
            return null;
        if(folderNode.getFolderName().equals(path)){
            return folderNode;
        }
        if(folderNode.getChildrenList()==null)return null;
        //根据path获取对应的节点
        for (FolderDto node : folderNode.getChildrenList()) {
            FolderDto path1 = getPath(path, node);
            if(path1!=null)return path1;
        }
        return null;
    }
}
