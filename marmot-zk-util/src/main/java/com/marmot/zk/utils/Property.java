package com.marmot.zk.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;

public class Property {
	
	private final byte[] lock = new byte[0];

    private ConcurrentMap<String, PropertiesConfiguration> configMap = new ConcurrentHashMap<String, PropertiesConfiguration>();

    private String dir;
    private String filename;
    private boolean isChange = true;

    protected void setDir(String dir) {
        this.dir = dir;
    }

    protected void setFilename(String filename) {
        this.filename = filename;
    }

    protected void setChange(boolean isChange) {
        this.isChange = isChange;
    }

    private PropertiesConfiguration loadConfig(String configFile) {
        PropertiesConfiguration config = configMap.get(configFile);
        if (config == null) {
            synchronized (lock) {
                try {
                    config = new PropertiesConfiguration();
                    config.setListDelimiter((char) 0);
                    if (isChange) {
                        // 可以自动更新
                        config.load(new File(configFile));
                        config.setReloadingStrategy(new FileChangedReloadingStrategy());
                    } else {
                        config.load(new FileInputStream(configFile));
                    }
                    configMap.put(configFile, config);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return config;
    }

    private PropertiesConfiguration loadConfig(ClassLoader classLoader, String configFile) {
        PropertiesConfiguration config = configMap.get(configFile);
        if (config == null) {
            synchronized (lock) {
                try {
                    InputStream inputStream = classLoader.getResourceAsStream(configFile);
                    config = new PropertiesConfiguration();
                    config.setListDelimiter((char) 0);
                    config.load(inputStream);
                    configMap.put(configFile, config);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return config;
    }

    private String path(String configFile) {
        return dir + configFile;
    }

    /**
     * 默认classes下面的config.properties
     * 
     * @param key
     * @return
     */
    public String get(String key) {
        return get(filename, key);
    }

    /**
     * classes下面指定的configFile
     * 
     * @param configFile 配置文件名
     * @param key
     * @return
     */
    public String get(String configFile, String key) {
        PropertiesConfiguration config = loadConfig(path(configFile));
        try {
            return config.getString(key);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 指定jar包中的configFile
     * 
     * @param classLoader 指定jar包的类加载器
     * @param configFile 配置文件名
     * @param key
     * @return
     */
    public String get(ClassLoader classLoader, String configFile, String key) {
        PropertiesConfiguration config = loadConfig(classLoader, configFile);
        try {
            return config.getString(key);
        } catch (Exception e) {
            return null;
        }
    }

    public String getString(String key, String defaultValue) {
        return getString(filename, key, defaultValue);
    }

    public String getString(String configFile, String key, String defaultValue) {
        PropertiesConfiguration config = loadConfig(path(configFile));
        return config.getString(key, defaultValue);
    }

    public String getString(ClassLoader classLoader, String configFile, String key, String defaultValue) {
        PropertiesConfiguration config = loadConfig(classLoader, configFile);
        return config.getString(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        return getInt(filename, key, defaultValue);
    }

    public int getInt(String configFile, String key, int defaultValue) {
        PropertiesConfiguration config = loadConfig(path(configFile));
        try {
            return config.getInt(key);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public int getInt(ClassLoader classLoader, String configFile, String key, int defaultValue) {
        PropertiesConfiguration config = loadConfig(classLoader, configFile);
        try {
            return config.getInt(key);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public float getFloat(String key, float defaultValue) {
        return getFloat(filename, key, defaultValue);
    }

    public float getFloat(String configFile, String key, float defaultValue) {
        PropertiesConfiguration config = loadConfig(path(configFile));
        try {
            return config.getFloat(key);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public float getFloat(ClassLoader classLoader, String configFile, String key, float defaultValue) {
        PropertiesConfiguration config = loadConfig(classLoader, configFile);
        try {
            return config.getFloat(key);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public long getLong(String key, long defaultValue) {
        return getLong(filename, key, defaultValue);
    }

    public long getLong(String configFile, String key, long defaultValue) {
        PropertiesConfiguration config = loadConfig(path(configFile));
        try {
            return config.getLong(key);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public long getLong(ClassLoader classLoader, String configFile, String key, long defaultValue) {
        PropertiesConfiguration config = loadConfig(classLoader, configFile);
        try {
            return config.getLong(key);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return getBoolean(filename, key, defaultValue);
    }

    public boolean getBoolean(String configFile, String key, boolean defaultValue) {
        PropertiesConfiguration config = loadConfig(path(configFile));
        try {
            return config.getBoolean(key);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public boolean getBoolean(ClassLoader classLoader, String configFile, String key, boolean defaultValue) {
        PropertiesConfiguration config = loadConfig(classLoader, configFile);
        try {
            return config.getBoolean(key);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public List<?> getList(String key) {
        return getList(filename, key);
    }

    public List<?> getList(String configFile, String key) {
        PropertiesConfiguration config = loadConfig(path(configFile));
        try {
            return config.getList(key);
        } catch (Exception e) {
            return null;
        }
    }

    public List<?> getList(ClassLoader classLoader, String configFile, String key) {
        PropertiesConfiguration config = loadConfig(classLoader, configFile);
        try {
            return config.getList(key);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * {@inheritDoc} This implementation returns keys that either match the
     * prefix or start with the prefix followed by a dot ('.'). So the call
     * {@code getKeys("db");} will find the keys {@code db}, {@code db.user}, or
     * {@code db.password}, but not the key {@code dbdriver}.
     */
    public Iterator<String> getKeys(String prefix) {
        return getKeys(filename, prefix);
    }

    public Iterator<String> getKeys(String configFile, String prefix) {
        PropertiesConfiguration config = loadConfig(path(configFile));
        try {
            return config.getKeys(prefix);
        } catch (Exception e) {
            return null;
        }
    }

    public Iterator<String> getKeys(ClassLoader classLoader, String configFile, String prefix) {
        PropertiesConfiguration config = loadConfig(classLoader, configFile);
        try {
            return config.getKeys(prefix);
        } catch (Exception e) {
            return null;
        }
    }

}
