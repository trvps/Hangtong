package com.htdz.db.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.htdz.def.dbmodel.UserToken;

public interface UserTokenMapper {

	@Select("select * from UserToken where name=#{0}")
	public UserToken select(String name);

	@Select("select * from UserToken")
	public List<UserToken> selectAll();

	@Insert("insert into UserToken(name, token, versions) values(#{name}, #{token}, #{versions})")
	@Options(useGeneratedKeys = true)
	public int add(UserToken userToken);

	@Update("update UserToken set token=#{token} where name=#{name}")
	public int modify(UserToken userToken);
}
