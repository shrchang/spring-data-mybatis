package org.springframework.data.mybatis.repository.support;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Convenient super class for MyBatis SqlSession data access objects. It gives you access to the template which can then
 * be used to execute SQL methods.
 *
 * @author Jarvis Song
 */
public abstract class SqlSessionRepositorySupport {

	public static final char DOT = '.';
	private final SqlSessionTemplate sqlSession;

	protected SqlSessionRepositorySupport(SqlSessionTemplate sqlSessionTemplate) {
		Assert.notNull(sqlSessionTemplate, "SqlSessionTemplate must not be null!");
		this.sqlSession = sqlSessionTemplate;
	}

	public SqlSessionTemplate getSqlSession() {
		return sqlSession;
	}

	/**
	 * Sub class can override this method.
	 *
	 * @return Namespace
	 */
	protected abstract String getNamespace();

	/**
	 * get the mapper statement include namespace.
	 *
	 * @param partStatement partStatement
	 * @return Statement
	 */
	protected String getStatement(String partStatement) {
		return getNamespace() + DOT + partStatement;
	}

	/**
	 * select one query.
	 *
	 * @param statement statement
	 * @param <T> entity class
	 * @return result
	 */
	protected <T> T selectOne(String statement) {
		return getSqlSession().selectOne(getStatement(statement));
	}

	protected <T> T selectOne(String statement, Object parameter) {
		return getSqlSession().selectOne(getStatement(statement), parameter);
	}

	protected <T> List<T> selectList(String statement) {
		return getSqlSession().selectList(getStatement(statement));
	}

	protected <T> List<T> selectList(String statement, Object parameter) {
		return getSqlSession().selectList(getStatement(statement), parameter);
	}

	protected int insert(String statement) {
		return getSqlSession().insert(getStatement(statement));
	}

	protected int insert(String statement, Object parameter) {
		return getSqlSession().insert(getStatement(statement), parameter);
	}

	protected int update(String statement) {
		return getSqlSession().update(getStatement(statement));
	}

	protected int update(String statement, Object parameter) {
		return getSqlSession().update(getStatement(statement), parameter);
	}

	protected int delete(String statement) {
		return getSqlSession().delete(getStatement(statement));
	}

	protected int delete(String statement, Object parameter) {
		return getSqlSession().delete(getStatement(statement), parameter);
	}

	/**
	 * Calculate total mount.
	 *
	 * @return if return -1 means can not judge ,need count from database.
	 */
	protected <X> long calculateTotal(Pageable pager, List<X> result) {
		if (pager.hasPrevious()) {
			if (CollectionUtils.isEmpty(result)) {
				return -1;
			}
			if (result.size() == pager.getPageSize()) {
				return -1;
			}
			return (pager.getPageNumber() - 1) * pager.getPageSize() + result.size();
		}
		if (result.size() < pager.getPageSize()) {
			return result.size();
		}
		return -1;
	}

	protected <X, Y, T extends Page<X>> T findByPager(Class<T> resultType, Pageable pager, String selectStatement,
			String countStatement, Y condition, Map<String, Object> otherParams) {
		Map<String, Object> params = new HashMap<String, Object>();
		// params.put("pager", pager);
		params.put("offset", pager.getOffset());
		params.put("pageSize", pager.getPageSize());
		params.put("offsetEnd", pager.getOffset() + pager.getPageSize());
		if (condition instanceof Sort) {
			params.put("_sorts", condition);
		} else {
			params.put("_sorts", pager.getSort());
		}
		params.put("_condition", condition);

		if (!CollectionUtils.isEmpty(otherParams)) {
			params.putAll(otherParams);
		}
		List<X> result = selectList(selectStatement, params);

		long total = calculateTotal(pager, result);
		if (total < 0) {
			total = selectOne(countStatement, params);
		}

		try {
			Constructor<T> constructor = resultType.getConstructor(List.class, Pageable.class, long.class);
			return constructor.newInstance(result, pager, total);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected <X, Y> Page<X> findByPager(Pageable pager, String selectStatement, String countStatement, Y condition,
			Map<String, Object> otherParams) {
		return findByPager(pager, selectStatement, countStatement, condition, otherParams, new String[0]);
	}

	protected <X, Y> Page<X> findByPager(Pageable pager, String selectStatement, String countStatement, Y condition,
			Map<String, Object> otherParams, String... columns) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("offset", pager.getOffset());
		params.put("pageSize", pager.getPageSize());
		params.put("offsetEnd", pager.getOffset() + pager.getPageSize());
		if (condition instanceof Sort && ((Sort) condition).isSorted()) {
			params.put("_sorts", condition);
		} else if (null != pager && null != pager.getSort() && pager.getSort().isSorted()) {
			params.put("_sorts", pager.getSort());
		}
		params.put("_condition", condition);
		// if (null != columns) {
		// params.put("_specifiedFields", columns);
		// }
		if (!CollectionUtils.isEmpty(otherParams)) {
			params.putAll(otherParams);
		}
		List<X> result = selectList(selectStatement, params);

		long total = calculateTotal(pager, result);
		if (total < 0) {
			total = selectOne(countStatement, params);
		}

		return new PageImpl<X>(result, pager, total);
	}

	protected <X> Page<X> findByPager(Pageable pager, String selectStatement, String countStatement) {
		return this.findByPager(pager, selectStatement, countStatement, null);
	}

	protected <X, Y> Page<X> findByPager(Pageable pager, String selectStatement, String countStatement, Y condition) {
		return this.findByPager(pager, selectStatement, countStatement, condition, (Map<String, Object>) null);
	}

	protected <X, Y> Page<X> findByPager(Pageable pager, String selectStatement, String countStatement, Y condition,
			String... columns) {
		return this.findByPager(pager, selectStatement, countStatement, condition, null, columns);
	}

}
