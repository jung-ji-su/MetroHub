package com.metrohub.notification.user;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserLookupMapper {

    @Select("SELECT id FROM users WHERE email = #{email}")
    Long findIdByEmail(String email);
}
