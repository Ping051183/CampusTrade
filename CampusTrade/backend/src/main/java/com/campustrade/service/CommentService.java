package com.campustrade.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campustrade.entity.Comment;

/**
 * 评论Service接口
 */
public interface CommentService extends IService<Comment> {

    /**
     * 添加评论
     */
    void addComment(Comment comment);

    /**
     * 删除评论（只有评论作者可以删除）
     */
    void deleteComment(Long commentId, Long userId);
}
