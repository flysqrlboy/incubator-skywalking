/*
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
 */

package org.apache.skywalking.apm.agent.core.conf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Strings;
import fm.lizhi.commons.config.config.ConfigBean;
import fm.lizhi.commons.config.event.ConfigFuture;
import fm.lizhi.commons.config.service.ConfigService;
import org.apache.skywalking.apm.agent.core.boot.AgentPackageNotFoundException;
import org.apache.skywalking.apm.agent.core.boot.AgentPackagePath;
import org.apache.skywalking.apm.agent.core.boot.ServiceManager;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.agent.core.sampling.SamplingService;
import org.apache.skywalking.apm.util.ConfigInitializer;
import org.apache.skywalking.apm.util.StringUtil;

/**
 * The <code>SnifferConfigInitializer</code> initializes all configs in several way.
 *
 * @author wusheng
 */
public class SnifferConfigInitializer {
    private static final ILog logger = LogManager.getLogger(SnifferConfigInitializer.class);
    private static String CONFIG_FILE_NAME = "/config/agent.config";
    private static String ENV_KEY_PREFIX = "skywalking.";
    private static String SKYWALKING_AGENT_CONFIG_KEY = "skywalking-agent";
    private static boolean IS_INIT_COMPLETED = false;
    
    private static SamplingService SAMPLING_SERVICE;

    /**
     * Try to locate `agent.config`, which should be in the /config dictionary of agent package. <br/>
     * 
     * Load order: <br/>
     * 
     * 1: `commonConfig.agent.focus_override_config` is `true` : <br/>
     * 
     * system properties > common config(config service) > application config(config service) > config file
     * 
     * 2: `commonConfig.agent.focus_override_config` is `false` or not set : <br/>
     * 
     * system properties > application config(config service) > common config(config service) > config file
     * 
     * <p>
     * Also try to override the config by system.env and system.properties. All the keys in these two places should
     * start with {@link #ENV_KEY_PREFIX}. e.g. in env `skywalking.agent.application_code=yourAppName` to override
     * `agent.application_code` in config file.
     * <p>
     * At the end, `agent.application_code` and `collector.servers` must be not blank.
     */
    public static void initialize() throws ConfigNotFoundException, AgentPackageNotFoundException {
        loadConfig();
        overrideConfig();
        
        logger.info("Skywalking agent config: {}", Config.print());

        if (StringUtil.isEmpty(Config.Agent.APPLICATION_CODE)) {
            throw new ExceptionInInitializerError("`agent.application_code` is missing.");
        }
        if (StringUtil.isEmpty(Config.Collector.SERVERS) && StringUtil.isEmpty(Config.Collector.DIRECT_SERVERS)) {
            throw new ExceptionInInitializerError("`collector.direct_servers` and `collector.servers` cannot be empty at the same time.");
        }

        IS_INIT_COMPLETED = true;
    }

    /**
     * load config : <br/>
     * 
     * Load from config service first.
     * 
     */
    private static void loadConfig() {
        try {
            loadConfigFromService();

        } catch (Exception e) {
            logger.error(e, "Failed to load config from config service, skywalking is going to load from file.");
            loadConfigFromFile();
        }
    }

    private static void overrideConfig() {
        try {
            overrideConfigBySystemEnv();
        } catch (Exception e) {
            logger.error(e, "Failed to read the system env.");
        }

        Config.Logging.FILE_NAME = Config.Agent.APPLICATION_CODE + ".log";
    }

    private static void loadConfigFromFile() {
        InputStreamReader configFileStream;
        try {
            configFileStream = loadConfigFromAgentFolder();
            Properties properties = new Properties();
            properties.load(configFileStream);
            ConfigInitializer.initialize(properties, Config.class);
        } catch (Exception e) {
            logger.error(e, "Failed to read the config file, skywalking is going to run in default config.");
        }
    }

    private static void loadConfigFromService() throws IllegalAccessException {
        logger.info("load config from service");
        
        boolean isProduct = "PRODUCT".equalsIgnoreCase(System.getProperty("conf.env"));
        String skyWalkingAgentConfigKey = System.getProperty("skyWalkingAgentConfigKey", SKYWALKING_AGENT_CONFIG_KEY);
        
        Properties properties = ConfigService.loadConfig(null, Properties.class,
            isProduct ? ConfigBean.PRODUCTION : ConfigBean.TEST_OFFICE, new ConfigFuture() {
                @Override public void configChange(String file, Map<String, String> configs) {
                    if (!Objects.equals(SKYWALKING_AGENT_CONFIG_KEY, file)) {
                        return;
                    }
                    
                    try {
                        SnifferConfigInitializer.configChange(configs);
                    } catch (Throwable e) {
                        logger.error("Failed to change skywalking agent config", e);
                    }
                }
            }, skyWalkingAgentConfigKey);

        properties = getConfigProperties(properties);
        ConfigInitializer.initialize(properties, Config.class);
    }

    private static void configChange(Map<String, String> configs) throws Throwable {
        Properties props = getConfigProperties(configs);
        if (props == null) {
            return;
        }

        logger.info("Skywalking agent config changed, new config: {}", configs);
        
        ConfigInitializer.initialize(props, Config.class);
        overrideConfig();
 
        logger.info("Skywalking agent config: {}", Config.print());

        // reboot sampling service
        if (SAMPLING_SERVICE == null) {
            SAMPLING_SERVICE = ServiceManager.INSTANCE.findService(SamplingService.class);
        }

        if (SAMPLING_SERVICE != null) {
            SAMPLING_SERVICE.boot();
        }
    }

    private static Properties getConfigProperties(Map<String, String> configMap) {
        Properties props = new Properties();
        props.putAll(configMap);
        
        return getConfigProperties(props);
    }

    /**
     * Loading order: <br/>
     *
     * 1: `commonConfig.agent.focus_override_config` is `true` : <br/>
     *
     * system properties > common config(config service) > application config(config service) > config file
     *
     * 2: `commonConfig.agent.focus_override_config` is `false` or not set : <br/>
     *
     * system properties > application config(config service) override common config(config service) > config file
     *
     * @param configProps
     * @return
     */
    private static Properties getConfigProperties(Properties configProps) {
        String commonConfigKey = "common";
        String appConfigKey = System.getProperty("app.name", "");
        String focusOverrideKey = "focus_override_config";
        
        Properties commonConfigProps = null;
        Properties appConfigProps = null;

        String commonConfig = configProps.getProperty(commonConfigKey);
        String appConfig = configProps.getProperty(appConfigKey);

        if (!Strings.isNullOrEmpty(commonConfig)) {
            commonConfigProps = JSON.parseObject(commonConfig, Properties.class);
        }

        if (!Strings.isNullOrEmpty(appConfig)) {
            appConfigProps = JSON.parseObject(appConfig, Properties.class);
        }
        
        if (commonConfigProps == null && appConfigProps == null) {
            return null;
        }

        if (commonConfigProps == null && appConfigProps != null) {
            return appConfigProps;
        }

        if (commonConfigProps != null && appConfigProps == null) {
            return commonConfigProps;
        }
        
        if (commonConfigProps != null
            && Objects.equals(Boolean.TRUE.toString(), commonConfigProps.get(focusOverrideKey))) {
            return commonConfigProps;
        } 
        
        // application config override common config 
        commonConfigProps.putAll(appConfigProps);
        
        return commonConfigProps;
    }

    public static boolean isInitCompleted() {
        return IS_INIT_COMPLETED;
    }

    /**
     * Override the config by system env. The env key must start with `skywalking`, the reuslt should be as same as in
     * `agent.config`
     * <p>
     * such as:
     * Env key of `agent.application_code` shoule be `skywalking.agent.application_code`
     *
     * @return the config file {@link InputStream}, or null if not needEnhance.
     */
    private static void overrideConfigBySystemEnv() throws IllegalAccessException {
        Properties properties = new Properties();
        Properties systemProperties = System.getProperties();
        Iterator<Map.Entry<Object, Object>> entryIterator = systemProperties.entrySet().iterator();
        while (entryIterator.hasNext()) {
            Map.Entry<Object, Object> prop = entryIterator.next();
            if (prop.getKey().toString().startsWith(ENV_KEY_PREFIX)) {
                String realKey = prop.getKey().toString().substring(ENV_KEY_PREFIX.length());
                properties.put(realKey, prop.getValue());
            }
        }

        if (!properties.isEmpty()) {
            ConfigInitializer.initialize(properties, Config.class);
        }
    }

    /**
     * Load the config file, where the agent jar is.
     *
     * @return the config file {@link InputStream}, or null if not needEnhance.
     */
    private static InputStreamReader loadConfigFromAgentFolder() throws AgentPackageNotFoundException, ConfigNotFoundException, ConfigReadFailedException {
        File configFile = new File(AgentPackagePath.getPath(), CONFIG_FILE_NAME);
        if (configFile.exists() && configFile.isFile()) {
            try {
                logger.info("Config file found in {}.", configFile);

                return new InputStreamReader(new FileInputStream(configFile), "UTF-8");
            } catch (FileNotFoundException e) {
                throw new ConfigNotFoundException("Fail to load agent.config", e);
            } catch (UnsupportedEncodingException e) {
                throw new ConfigReadFailedException("Fail to load agent.config", e);
            }
        }
        throw new ConfigNotFoundException("Fail to load agent config file.");
    }
}
