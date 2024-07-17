package org.clc.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @version 1.0
 * 评论实体类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title="评论实体类")
public class Comment {
    @Schema(title="数据库id主键")
    private int id;
    @Schema(title="评论ID")
    private Integer cId;//评论ID
    @Schema(title="用户ID")
    private String uid;//用户ID
    @Schema(title="帖子postId")
    private String postId;//帖子postId
    @Schema(title="详情")
    private String content;//详情
    @Schema(title="创建时间")
    private LocalDateTime createTime;//创建时间
}
