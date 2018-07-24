package com.htdz.liteguardian.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import com.alibaba.druid.pool.DruidDataSource;

/**
 * dbutils常用模板，使用log4jdbc监控sql执行，需要用到dao的地方用getinstace即可
 * 说明：这里完全可以封装成一个完全静态的类，所有方法都用public static
 * 
 * @author chensheng
 */
public class DaoStatic {

	private static DataSource dataSource = null;// 数据源，在静态快中实例化一个log4jdbc的数据源
	private static DaoStatic instance = null;// 单例模式的实例
	private final QueryRunner queryRunner = new QueryRunner(dataSource);// dbutils的查询对象

	/**
	 * 静态快，先实例化一个druid的数据库连接池，在实例化一个log4jdbcdatasource
	 */
	static {
		/*
		 * Properties props = new Properties(); DruidDataSource dds = new
		 * DruidDataSource(); try {
		 * props.load(DaoStatic.class.getResourceAsStream("/jdbc.properties"));
		 * } catch (IOException e) { e.printStackTrace(); }
		 * 
		 * dds.setUrl(props.getProperty("url"));
		 * dds.setUsername(props.getProperty("username"));
		 * dds.setPassword(props.getProperty("password"));
		 */

		dataSource = new DruidDataSource();
	}

	/**
	 * 私有化的构造
	 */
	private DaoStatic() {
		super();
	}

	/**
	 * 单例方法
	 */
	public static DaoStatic getInstance() {
		if (instance == null) {
			synchronized (DaoStatic.class) {
				if (instance == null)
					instance = new DaoStatic();
			}
		}
		return instance;
	}

	/**
	 * 执行sql语句
	 * 
	 * @param sql
	 *            sql语句
	 * @return 受影响的行数
	 */
	public int update(String sql) throws SQLException {
		return update(sql, null);
	}

	/**
	 * 执行sql语句 <code> 
	 * executeUpdate("update user set username = 'kitty' where username = ?", "hello kitty"); 
	 * </code>
	 * 
	 * @param sql
	 *            sql语句
	 * @param param
	 *            参数
	 * @return 受影响的行数
	 */
	public int update(String sql, Object param) throws SQLException {
		return update(sql, new Object[] { param });
	}

	/**
	 * 执行sql语句
	 * 
	 * @param sql
	 *            sql语句
	 * @param params
	 *            参数数组
	 * @return 受影响的行数
	 * @throws SQLException
	 */
	public int update(String sql, Object[] params) throws SQLException {
		// queryRunner = new QueryRunner(dataSource);
		int affectedRows = 0;
		try {
			if (params == null) {
				affectedRows = queryRunner.update(sql);
			} else {
				affectedRows = queryRunner.update(sql, params);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			throw e;
		}
		return affectedRows;
	}

	/**
	 * 执行批量sql语句
	 * 
	 * @param sql
	 *            sql语句
	 * @param params
	 *            二维参数数组
	 * @return 受影响的行数的数组
	 */
	public int[] batchUpdate(String sql, Object[][] params) {
		// queryRunner = new QueryRunner(dataSource);
		int[] affectedRows = new int[0];
		try {
			affectedRows = queryRunner.batch(sql, params);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return affectedRows;
	}

	/**
	 * 执行查询，将每行的结果保存到一个Map对象中，然后将所有Map对象保存到List中
	 * 
	 * @param sql
	 *            sql语句
	 * @return 查询结果
	 */
	public List<Map<String, Object>> find(String sql) {
		return find(sql, null);
	}

	/**
	 * 执行查询，将每行的结果保存到一个Map对象中，然后将所有Map对象保存到List中
	 * 
	 * @param sql
	 *            sql语句
	 * @param param
	 *            参数
	 * @return 查询结果
	 */
	public List<Map<String, Object>> find(String sql, Object param) {
		return find(sql, new Object[] { param });
	}

	/**
	 * 执行查询，将每行的结果保存到一个Map对象中，然后将所有Map对象保存到List中
	 * 
	 * @param sql
	 *            sql语句
	 * @param params
	 *            参数数组
	 * @return 查询结果
	 */
	public List<Map<String, Object>> find(String sql, Object[] params) {
		// queryRunner = new QueryRunner(dataSource);
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		try {
			if (params == null) {
				list = queryRunner.query(sql, new MapListHandler());
			} else {
				list = queryRunner.query(sql, new MapListHandler(), params);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return list;
	}

	/**
	 * 求count
	 * 
	 * @param sql
	 *            sql语句
	 * @param params
	 *            参数
	 * @return
	 */
	public long count(String sql, Object[] params) {
		// count(Type)返回的是Long类型,无奈
		Long retVal = 0L;
		try {
			sql = "select count(*) as count "
					+ sql.substring(sql.indexOf("from"));
			Map<String, Object> first = findFirst(sql, params);
			retVal = Long.valueOf(first.get("count").toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return retVal;
	}

	/**
	 * 执行查询，将每行的结果保存到Bean中，然后将所有Bean保存到List中
	 * 
	 * @param entityClass
	 *            类名
	 * @param sql
	 *            sql语句
	 * @return 查询结果
	 * @throws SQLException
	 */
	public <T> List<T> find(Class<T> entityClass, String sql)
			throws SQLException {
		return find(entityClass, sql, null);
	}

	/**
	 * 执行查询，将每行的结果保存到Bean中，然后将所有Bean保存到List中
	 * 
	 * @param entityClass
	 *            类名
	 * @param sql
	 *            sql语句
	 * @param param
	 *            参数
	 * @return 查询结果
	 * @throws SQLException
	 */
	public <T> List<T> find(Class<T> entityClass, String sql, Object param)
			throws SQLException {
		return find(entityClass, sql, new Object[] { param });
	}

	/**
	 * 查询出结果集中的第一条记录，并封装成对象
	 * 
	 * @param entityClass
	 *            类名
	 * @param sql
	 *            sql语句
	 * @return 对象
	 */
	public <T> T findFirst(Class<T> entityClass, String sql) {
		return findFirst(entityClass, sql, null);
	}

	/**
	 * 查询出结果集中的第一条记录，并封装成对象
	 * 
	 * @param entityClass
	 *            类名
	 * @param sql
	 *            sql语句
	 * @param param
	 *            参数
	 * @return 对象
	 */
	public <T> T findFirst(Class<T> entityClass, String sql, Object param) {
		return findFirst(entityClass, sql, new Object[] { param });
	}

	/**
	 * 查询出结果集中的第一条记录，并封装成Map对象
	 * 
	 * @param sql
	 *            sql语句
	 * @return 封装为Map的对象
	 */
	public Map<String, Object> findFirst(String sql) {
		return findFirst(sql, null);
	}

	/**
	 * 查询出结果集中的第一条记录，并封装成Map对象
	 * 
	 * @param sql
	 *            sql语句
	 * @param param
	 *            参数
	 * @return 封装为Map的对象
	 */
	public Map<String, Object> findFirst(String sql, Object param) {
		return findFirst(sql, new Object[] { param });
	}

	/**
	 * 查询出结果集中的第一条记录，并封装成Map对象
	 * 
	 * @param sql
	 *            sql语句
	 * @param params
	 *            参数数组
	 * @return 封装为Map的对象
	 */
	public Map<String, Object> findFirst(String sql, Object[] params) {
		// queryRunner = new QueryRunner(dataSource);
		Map<String, Object> map = null;
		try {
			if (params == null) {
				map = queryRunner.query(sql, new MapHandler());
			} else {
				map = queryRunner.query(sql, new MapHandler(), params);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return map;
	}

	/**
	 * 查询某一条记录，并将指定列的数据转换为Object
	 * 
	 * @param sql
	 *            sql语句
	 * @param columnName
	 *            列名
	 * @return 结果对象
	 */
	public Object findBy(String sql, String columnName) {
		return findBy(sql, columnName, null);
	}

	/**
	 * 查询某一条记录，并将指定列的数据转换为Object
	 * 
	 * @param sql
	 *            sql语句
	 * @param columnName
	 *            列名
	 * @param param
	 *            参数
	 * @return 结果对象
	 */
	public Object findBy(String sql, String columnName, Object param) {
		return findBy(sql, columnName, new Object[] { param });
	}

	/**
	 * 使用ScalarHandler处理单行记录，只返回结果集第一行中的指定字段，如未指定字段，则返回第一个字段！
	 * 
	 * @param sql
	 *            sql语句
	 * @param columnName
	 *            列名
	 * @param params
	 *            参数数组
	 * @return 结果对象
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Object findBy(String sql, String columnName, Object[] params) {
		// queryRunner = new QueryRunner(dataSource);
		Object object = null;
		try {
			if (params == null) {
				object = queryRunner.query(sql, new ScalarHandler(columnName));
			} else {
				object = queryRunner.query(sql, new ScalarHandler(columnName),
						params);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return object;
	}

	/**
	 * 查询某一条记录，并将指定列的数据转换为Object
	 * 
	 * @param sql
	 *            sql语句
	 * @param columnIndex
	 *            列索引
	 * @return 结果对象
	 */
	public Object findBy(String sql, int columnIndex) {
		return findBy(sql, columnIndex, null);
	}

	/**
	 * 查询某一条记录，并将指定列的数据转换为Object
	 * 
	 * @param sql
	 *            sql语句
	 * @param columnIndex
	 *            列索引
	 * @param param
	 *            参数
	 * @return 结果对象
	 */
	public Object findBy(String sql, int columnIndex, Object param) {
		return findBy(sql, columnIndex, new Object[] { param });
	}

	/**
	 * 查询某一条记录，并将指定列的数据转换为Object
	 * 
	 * @param sql
	 *            sql语句
	 * @param columnIndex
	 *            列索引
	 * @param params
	 *            参数数组
	 * @return 结果对象
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Object findBy(String sql, int columnIndex, Object[] params) {
		// queryRunner = new QueryRunner(dataSource);
		Object object = null;
		try {
			if (params == null) {
				object = queryRunner.query(sql, new ScalarHandler(columnIndex));
			} else {
				object = queryRunner.query(sql, new ScalarHandler(columnIndex),
						params);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return object;
	}

	/**
	 * 添加某一条记录，返回自增长的键值
	 * 
	 * @param sql
	 *            sql语句
	 * @param paramList
	 *            参数数组
	 * @return 第一列的值
	 */
	public long updateGetID(String sql, List<Object> paramList)
			throws SQLException {
		Connection conn = dataSource.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			if (paramList != null)
				for (int i = 0; i < paramList.size(); i++) {
					ps.setObject(i + 1, paramList.get(i));
				}
			ps.executeUpdate();
			rs = ps.getGeneratedKeys();
			return rs.next() ? rs.getLong(1) : -1;
		} catch (SQLException e) {
			throw new SQLException(e);
		} finally {
			DbUtils.closeQuietly(rs);
			DbUtils.closeQuietly(ps);
			sql = null;
		}
	}

	// main方法
	public static void main(String[] args) {
		DaoStatic dao = DaoStatic.getInstance();
		String sql = "INSERT INTO `TClockTime`(`cid`,`diabolo`)VALUES (1,'09:21')";
		try {
			dao.update(sql);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}