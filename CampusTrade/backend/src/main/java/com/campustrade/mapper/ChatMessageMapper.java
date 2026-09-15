package com.campustrade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campustrade.entity.ChatMessage;
import com.campustrade.vo.ConversationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 聊天消息Mapper接口
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    /**
     * 获取用户的会话列表
     */
    List<ConversationVO> selectConversations(@Param("userId") Long userId);

    /**
     * 获取两个用户之间关于某个商品的未读消息数
     */
    Integer selectUnreadCount(@Param("receiverId") Long receiverId,
                              @Param("senderId") Long senderId,
                              @Param("goodsId") Long goodsId);

    /**
     * 获取用户总未读消息数
     */
    Integer selectTotalUnreadCount(@Param("userId") Long userId);

    /**
     * 标记消息为已读
     */
    void markAsRead(@Param("receiverId") Long receiverId,
                    @Param("senderId") Long senderId,
                    @Param("goodsId") Long goodsId);
}
