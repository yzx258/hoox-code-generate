/**
 * Copyright (c) 2018 人人开源 All rights reserved.
 *
 * https://www.renren.io
 *
 * 版权所有，侵权必究！
 */

package io.renren.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.renren.dao.GeneratorDao;
import io.renren.utils.GenUtils;
import io.renren.utils.PageUtils;
import io.renren.utils.Query;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;

/**
 * 代码生成器
 * 
 * @author ： Mark sunlightcs@gmail.com
 */
@Service
public class SysGeneratorService {
	@Autowired
	private GeneratorDao generatorDao;

	/**
	 * 查询表信息
	 * @param query
	 * @return
	 */
	public PageUtils queryList(Query query) {
		Page<?> page = PageHelper.startPage(query.getPage(), query.getLimit());
		List<Map<String, Object>> list = generatorDao.queryList(query);

		if (CollectionUtils.isNotEmpty(list)) {
			for (Map<String, Object> map : list) {
				// 默认模块名
				map.put("moduleName", GenUtils.getConfig().getString("moduleName"));
				// 默认请求路径
				String tableName = map.get("tableName").toString();
				map.put("restPath", GenUtils.getRestPath(tableName));
			}
		}
		return new PageUtils(list, (int)page.getTotal(), query.getLimit(), query.getPage());
	}

	/**
	 * 根据表名查询表信息
	 * @param tableName
	 * @return
	 */
	public Map<String, String> queryTable(String tableName) {
		return generatorDao.queryTable(tableName);
	}

	/**
	 * 根据表名查询字段信息
	 * @param tableName
	 * @return
	 */
	public List<Map<String, String>> queryColumns(String tableName) {
		return generatorDao.queryColumns(tableName);
	}

	/**
	 * 根据生成配置生成代码
	 * @param generatorConfigs
	 * @return
	 */
	public byte[] generatorCode(JSONArray generatorConfigs) {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ZipOutputStream zip = new ZipOutputStream(outputStream);
		for (int i = 0; i < generatorConfigs.size(); i ++) {
			JSONObject generatorConfig = generatorConfigs.getJSONObject(i);
			String tableName = generatorConfig.getString("tableName");
			//查询表信息
			Map<String, String> table = queryTable(tableName);
			//查询列信息
			List<Map<String, String>> columns = queryColumns(tableName);
			//生成代码
			GenUtils.generatorCode(generatorConfig, table, columns, zip);
			if (i == generatorConfigs.size() - 1) {
				GenUtils.generatorCode("custemplate/README.vm", generatorConfig, table, columns, zip);
			}
		}
		IOUtils.closeQuietly(zip);
		return outputStream.toByteArray();
	}
}
