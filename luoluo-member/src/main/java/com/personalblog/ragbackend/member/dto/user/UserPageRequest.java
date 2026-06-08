package com.personalblog.ragbackend.member.dto.user;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 用户分页请求对象
 */
public class UserPageRequest extends Page<Object> {
    private String keyword;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
