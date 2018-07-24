package com.htdz.db.sqlprovider;

public class SqlProvider {
	public String executeSQL(String sql) {
		return sql;
	}

	public String executeSQLHandleNull(String sql) {
		return sql.replace("'null'", "null");
	}

}
