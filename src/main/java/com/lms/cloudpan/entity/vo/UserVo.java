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
public class UserVo {



    private Integer userId;

    private String username;
    private String password;

    private String checkPassword;

    private String email;
    private  Integer isEnable;

    private Long useQuota;
    private Long quota;


}
