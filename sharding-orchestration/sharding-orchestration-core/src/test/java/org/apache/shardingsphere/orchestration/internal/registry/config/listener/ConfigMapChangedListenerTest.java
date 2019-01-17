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

package org.apache.shardingsphere.orchestration.internal.registry.config.listener;

import org.apache.shardingsphere.orchestration.reg.api.RegistryCenter;
import org.apache.shardingsphere.orchestration.reg.listener.DataChangedEvent;
import org.apache.shardingsphere.orchestration.reg.listener.DataChangedEvent.ChangedType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public final class ConfigMapChangedListenerTest {
    
    private static final String CONFIG_MAP_YAML = "sharding-key1: sharding-value1";
    
    private ConfigMapChangedListener configMapChangedListener;
    
    @Mock
    private RegistryCenter regCenter;
    
    @Before
    public void setUp() {
        configMapChangedListener = new ConfigMapChangedListener("test", regCenter);
    }
    
    @Test
    public void assertCreateShardingOrchestrationEvent() {
        assertThat(configMapChangedListener.createShardingOrchestrationEvent(new DataChangedEvent("test", CONFIG_MAP_YAML, ChangedType.UPDATED)).getConfigMap().size(), is(1));
    }
}