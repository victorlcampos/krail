/*
 * Copyright (C) 2013 David Sowerby
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.q3c.krail.i18n;

/**
 * @author David Sowerby 9 Feb 2013
 */
public class TestLabels_it extends TestLabels {

    @Override
    protected void loadMap(Class<Enum<?>> enumKeyClass) {
        put(TestLabelKey.Home, "it_Home");
        put(TestLabelKey.Yes, "it_Yes");
        put(TestLabelKey.No, "it_No");
        put(TestLabelKey.ViewA, "it_ViewA");
        put(TestLabelKey.ViewB, "it_ViewB");
        put(TestLabelKey.MoneyInOut, "it_MoneyInOut");
        put(TestLabelKey.Blank, "");

    }


}
