/**
 * Copyright (C) 2015-2018 Expedia Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hotels.heat.core.handlers;

import java.util.Properties;

import com.hotels.heat.core.testcasedetails.TestCase;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.hotels.heat.core.log.Log;


/**
 * Unit Tests for {@link PropertyHandler}.
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest(PropertyHandler.class)
public class PropertyHandlerTest {


    @Mock
    private PlaceholderHandler placeholderHandler;

    @InjectMocks
    private PropertyHandler underTest;

    @BeforeMethod
    public void setUp() { }

    private void clearSysProperties() {
        System.clearProperty("defaultEnvironment");
        System.clearProperty("environment");
        System.clearProperty("webappName");
    }

    @Test
    public void testNotCorrectFlow() {
        //GIVEN
        System.setProperty("defaultEnvironment", "DEFAULT");
        System.setProperty("environment", "ENV_UNDER_TEST");
        System.setProperty("webappName", "WEBAPP_UNDER_TEST");


        underTest = new PropertyHandler("file", placeholderHandler, TestCase.getInstance());
        underTest.loadEnvironmentProperties();
        Assert.assertFalse(underTest.isLoaded());

    }

    @Test
    public void testPropertiesSetterAndGetter() {

        underTest = new PropertyHandler("file", placeholderHandler, TestCase.getInstance());
        Properties ps = new Properties();
        underTest.setProperties(ps);
        Assert.assertEquals(ps, underTest.getProperties());

    }

}
