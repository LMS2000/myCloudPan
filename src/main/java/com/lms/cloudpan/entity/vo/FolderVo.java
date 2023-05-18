package com.lms.cloudpan.entity.vo;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Slf4j
public class FolderVo {
    private String path;
    private String  parentPath;

    private Integer folderId;

    private String newPath;


}
