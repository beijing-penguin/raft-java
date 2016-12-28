package org.dc.penguin.core.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class ConfigManager {

	private static final Log LOG = LogFactory.getLog(ConfigManager.class);

	private static ConfigManager configManger = new ConfigManager();

	private static Map<String, Properties> proMap = new HashMap<String, Properties>();

	private ConfigManager() {
	}

	public static ConfigManager getInstance() {
		return configManger;
	}

	/**
	 * 
	 * @param profileName
	 *            配置文件名（不包括后缀）
	 * @param key
	 *            属性key值
	 * @return
	 */
	public String get(String profileName, String key) {
		if (StringUtils.isBlank(key) || StringUtils.isBlank(profileName) ) {
			throw new IllegalArgumentException("key is null");
		}
		Properties properties = proMap.get(profileName);
		if(properties!=null){
			return properties.getProperty(key);
		}else{
			return null;
		}
	}

	/**
	 * 
	 * @param profileName
	 *            配置文件名（不包括后缀）
	 * @param key
	 *            属性key值
	 * @return
	 */
	public int getInt(String profileName, String key) {
		String intValue = this.get(profileName, key);
		return Integer.parseInt(intValue);
	}
	/**
	 * 
	 * @param profileName
	 *            配置文件名（不包括后缀）
	 * @param key
	 *            属性key值
	 * @return
	 */
	public boolean getBoolean(String profileName, String key) {
		String value = this.get(profileName, key);
		return Boolean.parseBoolean(value);
	}
	public List<String> readLines(String proFileName) {
		InputStream resource = null;
		InputStreamReader is = null;
		BufferedReader reader = null;
		try {
			ClassLoader currentCL = getBaseClassLoader();
			for (;;) {
				if (currentCL != null) {
					resource = currentCL.getResourceAsStream(proFileName);
				} else {
					resource = ClassLoader.getSystemResourceAsStream(proFileName);
					break;
				}
				if (null != resource) {
					break;
				} else {
					currentCL = currentCL.getParent();
				}
			}
			is = new InputStreamReader(resource,"UTF-8");
			reader = new BufferedReader(is);
			List<String> list = new ArrayList<String>();
			String tempString = null;
			while ((tempString = reader.readLine()) != null) {
				list.add(tempString);
			}
			return list;
		} catch (Exception e) {
			LOG.error("Error reading " + proFileName + " in readLines() " ,e);
		} finally {
			try {
				if (reader != null){
					reader.close();
				}
			} catch (Exception e) {
				LOG.error("" ,e);
			}
			try {
				if (is != null){
					is.close();
				}
			} catch (Exception e) {
				LOG.error("" ,e);
			}
			try {
				if (resource != null){
					resource.close();
				}
			} catch (Exception e) {
				LOG.error("" ,e);
			}
		}
		return null;
	}
	public synchronized Properties loadProps(String proFileName) throws Exception {
		InputStream resource = null;
		try {
			ClassLoader currentCL = getBaseClassLoader();
			if (currentCL != null) {
				resource = currentCL.getResourceAsStream(proFileName);
			} else {
				resource = ClassLoader.getSystemResourceAsStream(proFileName);
			}
			Properties properties = new Properties();
			LOG.info("init config "+proFileName);
			properties.load(new InputStreamReader(resource, "UTF-8"));
			/*if(proMap.containsKey(proFileName)){
				throw new Exception("the file"+proFileName+" have repeat");
			}*/
			proMap.put(proFileName, properties);
			LOG.info("Successfully  proFileName=" + proFileName + " load Properties:" + properties);
			return properties;
		} catch (Exception e) {
			throw e;
		} finally {
			try {
				if (resource != null)
					resource.close();
			} catch (Exception e) {
				LOG.error("" ,e);
			}
		}
	}
	/*public Properties loadProperties(String proFileName) throws Exception {
		InputStream resource = null;
		try {
			ClassLoader currentCL = getBaseClassLoader();
			if (currentCL != null) {
				resource = currentCL.getResourceAsStream(proFileName);
			} else {
				resource = ClassLoader.getSystemResourceAsStream(proFileName);
			}
			Properties properties = new Properties();
			properties.load(new InputStreamReader(resource, "UTF-8"));
			return properties;
		} catch (Exception e) {
			throw e;
		} finally {
			try {
				if (resource != null)
					resource.close();
			} catch (Exception e) {
				LOG.error("" ,e);
			}
		}
	}*/
	public synchronized void clearConfig(){
		proMap.clear();
	}

	private static ClassLoader getBaseClassLoader() {
		ClassLoader thisClassLoader = ConfigManager.class
				.getClassLoader();
		ClassLoader contextClassLoader = Thread.currentThread()
				.getContextClassLoader();
		ClassLoader baseClassLoader = getLowestClassLoader(contextClassLoader,
				thisClassLoader);
		return baseClassLoader;
	}
	private static ClassLoader getLowestClassLoader(ClassLoader c1,ClassLoader c2) {
		if (c1 == null)
			return c2;

		if (c2 == null)
			return c1;

		ClassLoader current;

		current = c1;
		while (current != null) {
			if (current == c2)
				return c1;
			current = current.getParent();
		}

		current = c2;
		while (current != null) {
			if (current == c1)
				return c2;
			current = current.getParent();
		}

		return null;
	}
}
