package com.htdz.db.mapper;


import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.htdz.def.dbmodel.ChatToken;


public interface ChatTokenMapper {
	
	@Select("select * from chat_token where name=#{0}")
	public ChatToken select(String name);
	
	@Select("select * from chat_token")
	public List<ChatToken> selectAll();
	
	@Insert("insert into chat_token(name, token) values(#{name}, #{token})")
	@Options(useGeneratedKeys = true)
	public int add(ChatToken chatToken);
	
	@Update("update chat_token set name=#{name}, age=#{age} where id=#{id}")
	public int modify(ChatToken chatToken);
}

