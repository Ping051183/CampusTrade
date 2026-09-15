package com.campustrade.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campustrade.entity.Comment;
import com.campustrade.exception.BusinessException;
import com.campustrade.exception.ErrorCode;
import com.campustrade.mapper.CommentMapper;
import com.campustrade.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 评论Service实现类
 */
@Service
@RequiredArgsConstructor
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    private final CommentMapper commentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addComment(Comment comment) {
        commentMapper.insert(comment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }
        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_YOURS);
        }
        commentMapper.deleteById(commentId);
    }
}
