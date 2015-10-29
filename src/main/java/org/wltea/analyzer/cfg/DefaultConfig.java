/**
 * IK 中文分词  版本 5.0
 * IK Analyzer release 5.0
 * 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * 源代码由林良益(linliangyi2005@gmail.com)提供
 * 版权声明 2012，乌龙茶工作室
 * provided by Linliangyi and copyright 2012 by Oolong studio
 * 
 * 
 */
package org.wltea.analyzer.cfg;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.InvalidPropertiesFormatException;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jolbox.bonecp.BoneCPDataSource;

/**
 * Configuration 默认实现. 支持由数据加载词典.
 * 
 * Note: 修复之前IK中单例模式的bug.
 * 
 * @author Jingyi Yu.
 * @author Liangyi Lin.
 * @version 1.0 2015-10-29.
 */
public class DefaultConfig implements Configuration{

	private static final Logger logger = LoggerFactory.getLogger(DefaultConfig.class);
	
	private static Configuration singleton = new DefaultConfig();
	
	/*
	 * 分词器默认字典路径 
	 */
	private static final String DIC_MAIN = "main";
	private static final String DIC_QUANTIFIER = "quantifier";

	/*
	 * 分词器配置文件路径
	 */	
	private static final String DICT_CONF = "IKxAnalyzer.cfg.xml";
	//配置属性——扩展字典
	private static final String EXT_DICT = "ext_dict";
	//配置属性——扩展停止词典
	private static final String EXT_STOP = "ext_stopwords";
	
	/**
	 * 数据库配置文件
	 */
	private static final String DB_CONFIG = "dbconfig.properties";
	
	private PropertiesConfiguration dbConf;
	
	private DataSource dataSource;
	
	private Properties dictProps;
	
	/*
	 * 是否使用smart方式分词
	 */
	private boolean useSmart;
	
	/**
	 * 返回单例
	 * @return Configuration单例
	 */
	public static Configuration getInstance(){
		return singleton;
	}
	
	/*
	 * 初始化配置文件
	 */
	private DefaultConfig(){		
		
		reloadDbConf();
		reloadDataSource();
		reloadDictConf();
	}

	/**
	 * 获取扩展字典配置路径
	 * @return List<String> 相对类加载器的路径
	 */
	public List<String> getExtDictionarys(){
		
		List<String> extDictFiles = new ArrayList<String>(2);
		String extDictCfg = dictProps.getProperty(EXT_DICT);
		if(extDictCfg != null){
			
			//使用;分割多个扩展字典配置
			String[] filePaths = extDictCfg.split(";");
			
			if(filePaths != null){
				for(String filePath : filePaths){
					if(filePath != null && !"".equals(filePath.trim())){
						extDictFiles.add(filePath.trim());
					}
				}
			}
		}
		
		return extDictFiles;		
	}


	/**
	 * 获取扩展停止词典配置路径
	 * @return List<String> 相对类加载器的路径
	 */
	public List<String> getExtStopWordDictionarys(){
		
		List<String> extStopWordDictFiles = new ArrayList<String>(2);
		String extStopWordDictCfg = dictProps.getProperty(EXT_STOP);
		if(extStopWordDictCfg != null){
			
			//使用;分割多个扩展字典配置
			String[] filePaths = extStopWordDictCfg.split(";");
			
			if(filePaths != null){
				for(String filePath : filePaths){
					if(filePath != null && !"".equals(filePath.trim())){
						extStopWordDictFiles.add(filePath.trim());
					}
				}
			}
		}
		
		return extStopWordDictFiles;		
	}
	
	private void reloadDictConf() {
		
		dictProps = new Properties();
		
		InputStream input = this.getClass().getClassLoader().getResourceAsStream(DICT_CONF);
		if(input != null){
			try {
				dictProps.loadFromXML(input);
			} catch (InvalidPropertiesFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}

	private void reloadDbConf() {
		
		if (dbConf == null) {
			dbConf = new PropertiesConfiguration();
		} else {
			dbConf.clear();
		}
		
		try {
			InputStream in = this.getClass().getClassLoader()
					.getResourceAsStream(DB_CONFIG);
			dbConf.load(in);
		} catch (ConfigurationException e) {
			logger.error("加载数据库配置失败，异常：{}", e);
		}
		
		Iterator<String> iter = dbConf.getKeys();
		while (iter.hasNext()) {
			String key = iter.next();
			Object value = dbConf.getProperty(key);
			logger.info("已获取数据库配置： {} = {} ", key, value);
		}
		
		logger.info("已重新加载数据库配置");
	}
	
	private void reloadDataSource() {
		
		try {
			BoneCPDataSource ds = new BoneCPDataSource();
			
			String driver = dbConf.getString("db.driver");
			ds.setDriverClass(String.format("org.%s.Driver", driver));
			
			String host = dbConf.getString("db.host");
			String port = dbConf.getString("db.port");
			String user = dbConf.getString("db.user");
			String password = dbConf.getString("db.password");
			String dbname = dbConf.getString("db.dbname");
			
			ds.setJdbcUrl(String.format("jdbc:%s://%s:%s/%s", driver, host, port, dbname));
			ds.setUser(user);
			ds.setPassword(password);
			
			ds.setMaxConnectionsPerPartition(Integer.valueOf(dbConf
					.getString("db.maxconns")));
			ds.setMinConnectionsPerPartition(Integer.valueOf(dbConf
					.getString("db.minconns")));
			ds.setPartitionCount(Integer.valueOf(dbConf
					.getString("db.partitions")));
			
			dataSource = ds;
			
		} catch (Exception e) {
			logger.error("初始化数据源连接池异常：{}", e);
		}
		
	}
	
	/**
	 * 返回useSmart标志位
	 * useSmart =true ，分词器使用智能切分策略， =false则使用细粒度切分
	 * @return useSmart
	 */
	public boolean useSmart() {
		return useSmart;
	}

	/**
	 * 设置useSmart标志位
	 * useSmart =true ，分词器使用智能切分策略， =false则使用细粒度切分
	 * @param useSmart
	 */
	public void setUseSmart(boolean useSmart) {
		this.useSmart = useSmart;
	}	
	
	/**
	 * 获取主词典路径
	 * 
	 * @return String 主词典路径
	 */
	public String getMainDictionary(){
		return DIC_MAIN;
	}

	/**
	 * 获取量词词典路径
	 * @return String 量词词典路径
	 */
	public String getQuantifierDicionary(){
		return DIC_QUANTIFIER;
	}
	
	public DataSource getDataSource() {
		return dataSource;
	}
	
}
