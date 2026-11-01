package com.campustrade.cache;

/**
 * 缓存 key 统一管理
 */
public interface CacheKeys {

    /** 商品详情：goods:detail:{goodsId} */
    String GOODS_DETAIL = "goods:detail:";

    /** 分类列表（全量） */
    String CATEGORY_LIST = "category:list";

    /** 用户未读消息数：chat:unread:{userId} */
    String CHAT_UNREAD = "chat:unread:";
}