package com.lms.cloudpan.controller;

import com.lms.cloudpan.entity.dao.User;
import com.lms.cloudpan.entity.dto.FileDto;
import com.lms.cloudpan.entity.vo.DownloadFileVo;
import com.lms.cloudpan.entity.vo.FileVo;
import com.lms.cloudpan.entity.vo.GetFileVo;
import com.lms.cloudpan.service.IFileService;
import com.lms.cloudpan.service.IUserService;
import com.lms.result.EnableResponseAdvice;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/file")
@Api(tags = "文件管理")
@EnableResponseAdvice
public class FileController {


    @Resource
    private IFileService fileService;
    @Resource
    private IUserService userService;

    /**
     * 上传文件到指定的路径下
     *
     * @param file
     * @param path
     * @param request
     * @return
     */
    @PostMapping(value = "/upload", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation("上传文件")
    public Boolean insertFile(@RequestBody MultipartFile file, @ApiParam("指定上传路径") @RequestParam("path") String path, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        FileVo fileVo = new FileVo();
        fileVo.setFile(file);
        fileVo.setFolderPath(path);
        return fileService.insertFile(fileVo, loginUser);
    }

    /**
     * 获取当前用户的指定路径下的全部文件
     *
     * @param getFileVo
     * @param request
     * @return
     */
    @PostMapping("/getFiles")
    @ApiOperation("获取当前路径下的全部文件")
    public List<FileDto> getFilesByPath(@RequestBody GetFileVo getFileVo, HttpServletRequest request) {
        Integer userId = userService.getLoginUser(request).getUserId();
        String path = getFileVo.getPath();
        //判断path是否以/或者\\开头
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        List<FileDto> userFileByPath = fileService.getUserFileByPath(path, userId);
        return userFileByPath;
    }

    /**
     * 重命名文件（没有修改实际存储磁盘的文件名）
     *
     * @param id
     * @param fileName
     * @return
     */
    @PostMapping("/rename/{id}/{name}")
    public Boolean renameFile(@PathVariable("id") Integer id, @PathVariable("name") String fileName, HttpServletRequest request) {
        Integer userId = userService.getLoginUser(request).getUserId();
        return fileService.renameFile(id, fileName, userId);
    }

    /**
     * 根据文件名模糊查询
     *
     * @param fileName
     * @return
     */
    @GetMapping("/search/{fileName}")
    public List<FileDto> searchFileByName(@PathVariable("fileName") String fileName, HttpServletRequest request) {
        Integer userId = userService.getLoginUser(request).getUserId();
        return fileService.searchFile(fileName, userId);
    }


    /**
     * 删除多个文件
     *
     * @param ids
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public Boolean deleteFiles(@RequestParam("ids") List<Integer> ids, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return fileService.deleteFiles(ids, loginUser);
    }


    //多选文件移动

    /**
     * 将多个文件移动到同一文件夹下
     *
     * @param fileIds
     * @param path
     * @param request
     * @return
     */
    @PostMapping("/move")
    public Boolean moveFiles(@RequestParam("ids") List<Integer> fileIds, @RequestParam("path") String path, HttpServletRequest request) {
        Integer userId = userService.getLoginUser(request).getUserId();
        return fileService.moveFiles(fileIds, path, userId);
    }


    @PostMapping("/download")
    public byte[] downloadFile(@RequestBody DownloadFileVo downloadFileVo) {
       return fileService.downloadFile(downloadFileVo);

    }
}
