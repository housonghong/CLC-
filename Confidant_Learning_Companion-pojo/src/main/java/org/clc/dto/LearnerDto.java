package org.clc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @version 1.0
 * 用户dto
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title="用户请求类")
public class LearnerDto {
    //@Schema(title="用户ID",type="String")
    private String uid;//用户ID
    @Schema(title="邮箱",type="String",description = "请输入合法邮箱",required=true)
    @NotNull(message = "邮箱不能为空")
    @Pattern(regexp = "^[\\w.-]+@[\\w.-]+$",message = "请输入合法邮箱")
    private String email;//邮箱
    @Schema(title="密码",description = "5-16位非空字符",type="String",required=true)
    @NotNull(message = "密码不能为空")
    @Pattern(regexp = "^\\S{5,16}$",message = "密码需要5-16位非空字符")
    private String password;//密码
    @Schema(title="用户名",description = "5-16位非空字符", type="String")
    @Pattern(regexp = "^\\S{5,16}$",message = "用户名需要5-16位非空字符")
    private String username;//用户名
    @Schema(title="手机号",type="String")
    @Pattern(regexp = "^1[3-9]\\d{9}$",message = "请输入合法手机号")
    private String phone;//手机号
    @Schema(title="性别(0 不愿透露，1 男性，2 女性)",type="Integer")
    @Min(value = 0)
    @Max(value = 2)
    private Integer gender;//性别
    @Schema(title="年龄",type="Integer")
    @Min(value = 1,message = "年龄必须为正整数")
    private Integer age;//年龄
    @Schema(title="学历(0 不愿透露，1 小学，2 初中，3 高中，4 大学，5 硕士，6 博士)",type="Integer")
    @Min(value = 0)
    @Max(value = 6)
    private Integer degree;//学历
    @Schema(title="工作",type="Integer")
    private Integer job;//工作
    @Schema(title="个性签名",type="String")
    private String characters;//个性签名
    @Schema(title = "头像")
    private String image;
}
