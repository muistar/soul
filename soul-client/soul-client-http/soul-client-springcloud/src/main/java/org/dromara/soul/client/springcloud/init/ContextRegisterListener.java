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
 */

package org.dromara.soul.client.springcloud.init;

import lombok.extern.slf4j.Slf4j;
import org.dromara.soul.client.common.utils.OkHttpTools;
import org.dromara.soul.client.springcloud.config.SoulSpringCloudConfig;
import org.dromara.soul.client.springcloud.dto.SpringCloudRegisterDTO;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The type Context register listener.
 * @author tnnn
 */
@Slf4j
public class ContextRegisterListener implements ApplicationListener<ContextRefreshedEvent> {

    private volatile AtomicBoolean registered = new AtomicBoolean(false);

    private final String url;

    private final SoulSpringCloudConfig config;

    private final Environment env;

    /**
     * Instantiates a new Context register listener.
     *
     * @param config the soul spring cloud config
     * @param env    the env
     */
    public ContextRegisterListener(final SoulSpringCloudConfig config, final Environment env) {
        String contextPath = config.getContextPath();
        String adminUrl = config.getAdminUrl();
        String appName = env.getProperty("spring.application.name");
        if (contextPath == null || "".equals(contextPath)
                || adminUrl == null || "".equals(adminUrl)
                || appName == null || "".equals(appName)) {
            throw new RuntimeException("spring cloud param must config the contextPath, adminUrl and appName");
        }
        this.config = config;
        this.env = env;
        this.url = adminUrl + "/soul-client/springcloud-register";
    }

    @Override
    public void onApplicationEvent(final ContextRefreshedEvent contextRefreshedEvent) {
        if (!registered.compareAndSet(false, true)) {
            return;
        }
        if (config.isFull()) {
            post(buildJsonParams(config.getContextPath()));
        }
    }

    private void post(final String json) {
        try {
            String result = OkHttpTools.getInstance().post(url, json);
            if (Objects.equals(result, "success")) {
                log.info("http context register success :{} ", json);
            } else {
                log.error("http context register error :{} ", json);
            }
        } catch (IOException e) {
            log.error("cannot register soul admin param :{}", url + ":" + json);
        }
    }

    private String buildJsonParams(final String contextPath) {

        String appName = env.getProperty("spring.application.name");
        String path = contextPath + "/**";
        SpringCloudRegisterDTO registerDTO = SpringCloudRegisterDTO.builder()
                .context(contextPath)
                .appName(appName)
                .path(path)
                .rpcType("springCloud")
                .enabled(true)
                .ruleName(path)
                .build();
        return OkHttpTools.getInstance().getGosn().toJson(registerDTO);
    }
}
