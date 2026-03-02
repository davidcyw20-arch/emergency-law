package com.yunxian.emergencylaw.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String role;      // USER / ADMIN
    private String region;
    private LocalDateTime createTime;
}
