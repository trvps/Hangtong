package com.htdz.db.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;

import com.htdz.db.sqlprovider.SqlProvider;

public interface DaoMapper {

	@SelectProvider(type = SqlProvider.class, method = "executeSQL")
	public List<Map<String, Object>> selectList(String sql);

	@SelectProvider(type = SqlProvider.class, method = "executeSQL")
	public Map<String, Object> select(String sql);

	@InsertProvider(type = SqlProvider.class, method = "executeSQLHandleNull")
	@Options(useGeneratedKeys = true)
	public int add(String sql);

	@UpdateProvider(type = SqlProvider.class, method = "executeSQLHandleNull")
	public int modify(String sql);

	@SelectProvider(type = SqlProvider.class, method = "executeSQL")
	public int count(String sql);

	@SelectProvider(type = SqlProvider.class, method = "executeSQL")
	public Object findBy(String sql);
}
