/**
 * Copyright (c) 2014 Baidu, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.baidu.rigel.biplatform.cache.store.service;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;

import com.hazelcast.core.Message;

/**
 *Description:
 * @author david.wang
 *
 */
public class HazelcastNoticePortTest {
    
    @Test
    public void testOnMessageWithNullMessage () {
        try {
            HazelcastNoticePort port = new HazelcastNoticePort ();
            ApplicationContext ctx = Mockito.mock (ApplicationContext.class);
            port.setApplicationContext (ctx);
            port.onMessage (null);
        } catch (BeansException e) {
            Assert.fail ();
        }
    }
    
    @Test
    public void testOnMessageWithNullContext () {
        try {
            HazelcastNoticePort port = new HazelcastNoticePort ();
            ApplicationContext ctx = Mockito.mock (ApplicationContext.class);
            port.setApplicationContext (ctx);
            ApplicationEvent messageObject = Mockito.mock (ApplicationEvent.class);
            Message<Object> m = new Message<> ("test", messageObject, System.currentTimeMillis (), null);
            port.onMessage (m);
        } catch (Exception e) {
            Assert.fail ();
        }
    }
    
    @Test
    public void  testOnMessage () {
        try {
            HazelcastNoticePort port = new HazelcastNoticePort ();
            ApplicationEvent messageObject = Mockito.mock (ApplicationEvent.class);
            Message<Object> m = new Message<> ("test", messageObject, System.currentTimeMillis (), null);
            port.onMessage (m);
        } catch (Exception e) {
            Assert.fail ();
        }
    }
}
