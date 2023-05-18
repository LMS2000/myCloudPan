package com.lms.cloudpan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lms.cloudpan.entity.dao.File;
import com.lms.cloudpan.entity.dao.Folder;
import com.lms.cloudpan.entity.dao.User;
import com.lms.cloudpan.entity.dto.FolderDto;
import com.lms.cloudpan.entity.vo.FolderVo;

import java.util.List;

public interface IFolderService  extends IService<Folder> {

    List<FolderDto> getUserFolder(String path, Integer userId);

    Boolean insertFolder(FolderVo folderVo, Integer uid);

    Boolean renameFolder(Integer folderId, String folderName, Integer uid);

    Boolean  deleteFolder(Integer folderId, User user);

   String downloadFolder(String path, Integer uid);

    List<FolderDto> getFolderList(Integer uid);
}
