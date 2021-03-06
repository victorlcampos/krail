/*
 * Copyright (c) 2015. David Sowerby
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package uk.q3c.krail.core.ui;

import com.vaadin.ui.AbstractField;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.DateField;
import com.vaadin.ui.TextField;

import java.util.Date;

/**
 * Created by David Sowerby on 28/05/15.
 */
public class DefaultDataTypeToUI implements DataTypeToUI {

    @Override
    public AbstractField componentFor(Object dataObject) {
        return componentFor(dataObject.getClass());
    }

    @Override
    public AbstractField componentFor(Class<?> dataType) {
        if (dataType.equals(Date.class)) {
            return new DateField();
        }
        if (dataType.equals(boolean.class) || dataType.equals(Boolean.class)) {
            return new CheckBox();
        }
        return new TextField();
    }

    @Override
    public <T> T zeroValue(Class<T> dataType) {
        if (dataType.equals(Date.class)) {
            return (T) new Date();
        }
        if (dataType.equals(boolean.class) || dataType.equals(Boolean.class)) {
            return (T) new Boolean(false);
        }
        if (dataType.equals(Integer.class) || dataType.equals(int.class)) {
            return (T) new Integer(0);
        }
        return null;
    }
}
