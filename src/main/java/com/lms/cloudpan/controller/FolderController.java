package com.lms.cloudpan.controller;

import com.lms.cloudpan.entity.dao.User;
import com.lms.cloudpan.entity.dto.FolderDto;
import com.lms.cloudpan.entity.vo.DownloadFolderVo;
import com.lms.cloudpan.entity.vo.FolderVo;
import com.lms.cloudpan.entity.vo.GetFileVo;
import com.lms.cloudpan.service.IFolderService;
import com.lms.cloudpan.service.IUserService;
import com.lms.result.EnableResponseAdvice;
import com.lms.result.ResultData;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/folder")
@EnableResponseAdvice
@Api(tags = "文件夹管理")
public class FolderController {

    @Resource
    private IFolderService folderService;

    @Resource
    private IUserService userService;

//
//    /**
//     * 获取用户全部的文件夹树
//     * @param request
//     * @return
//     */
//    @GetMapping("/getUserDir")
//    public ResultData getUserDir(HttpServletRequest request){
//        Integer userId = userService.getLoginUser(request).getUserId();
//        List<FolderDto> userFolder = folderService.getUserFolder(userId);
//        System.out.println("用户的id为----------"+userId);
//        return ResultData.success(userFolder);
//    }

    @PostMapping("/getUserDir/v2")
    public ResultData getCurrentDir(@RequestBody GetFileVo getFileVo, HttpServletRequest request) {
        Integer userId = userService.getLoginUser(request).getUserId();
        String path = getFileVo.getPath();
        List<FolderDto> userFolder = folderService.getUserFolder(path, userId);
        return ResultData.success(userFolder);
    }

    /**
     * 新建文件夹
     *
     * @param folderVo
     * @param request
     * @return
     */
    @PostMapping("/createPath")
    public Boolean insertFolder(@RequestBody FolderVo folderVo, HttpServletRequest request) {
        Integer userId = userService.getLoginUser(request).getUserId();
        return folderService.insertFolder(folderVo, userId);
    }


    // 重命名文件夹
    //关于请求路径为/rename 后端接收不来前端的请求体
    /**
     * 重命名文件夹
     *
     * @param folderVo
     * @param request
     * @return
     */
    @PostMapping("/rename")
    public Boolean renameFolder(@RequestBody FolderVo folderVo, HttpServletRequest request) {
        Integer userId = userService.getLoginUser(request).getUserId();
        return folderService.renameFolder(folderVo.getFolderId(), folderVo.getNewPath(), userId);
    }


    //删除文件夹
    @PostMapping("/delete/{id}")
    public Boolean deleteFolder(@PathVariable("id") Integer folderId, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return folderService.deleteFolder(folderId, loginUser);
    }

//    /**
//     * 新建文件夹
//     *
//     * @param renameFolderVo
//     * @param request
//     * @return
//     */
//    @PostMapping("/rename/v2")
//    public ResultData insertFolder2(@RequestBody RenameFolderVo renameFolderVo, HttpServletRequest request) {
//        Integer userId = userService.getLoginUser(request).getUserId();
//        return ResultData.success(folderService.renameFolder(renameFolderVo.getFolderId(), renameFolderVo.getNewPath(), userId));
//    }

    @PostMapping("/download")
    public ResultData downloadFolder(DownloadFolderVo downloadFolderVo, HttpServletRequest request){
        Integer userId = userService.getLoginUser(request).getUserId();
        ResultData success = ResultData.success();
        success.put("data",folderService.downloadFolder(downloadFolderVo.getPath(),userId));
        return success;
    }

}
