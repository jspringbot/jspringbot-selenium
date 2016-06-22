/*
 * Copyright (c) 2012. JSpringBot. All Rights Reserved.
 *
 * See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The JSpringBot licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jspringbot.keyword.selenium.web;

import org.jspringbot.KeywordInfo;
import org.jspringbot.keyword.selenium.AbstractSeleniumKeyword;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@KeywordInfo(
        name = "Select From List",
        parameters = {"locator", "*items"},
        description = "classpath:desc/SelectFromList.txt"
)
public class SelectFromList extends AbstractSeleniumKeyword {

    @Override
    public Object execute(Object[] params) {

        List<String> list = new ArrayList<String>();
        for(int  i = 1; i < params.length; i++) {
            list.add(String.valueOf(params[i]));
        }

        helper.selectFromList(String.valueOf(params[0]), list);

        return null;
    }
}
