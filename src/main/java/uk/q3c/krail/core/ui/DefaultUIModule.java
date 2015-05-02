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

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import com.vaadin.server.WebBrowser;
import com.vaadin.ui.UI;
import uk.q3c.krail.i18n.I18NKey;
import uk.q3c.krail.i18n.LabelKey;

public class DefaultUIModule extends AbstractModule {

    @Override
    protected void configure() {
        bindApplicationTitle();
        MapBinder<String, UI> mapbinder = MapBinder.newMapBinder(binder(), String.class, UI.class);

        bind(WebBrowser.class).toProvider(BrowserProvider.class);

        bindUIProvider();
        addUIBindings(mapbinder);


    }

    private void bindApplicationTitle() {
        ApplicationTitle title = new ApplicationTitle(applicationTitleKey());
        bind(ApplicationTitle.class).toInstance(title);
    }

    /**
     * override this method to provide the I18Nkey which defines your application title (which appears in your browser
     * tab)
     */
    protected I18NKey applicationTitleKey() {
        return LabelKey.Krail;
    }


    /**
     * Override to bind your ScopedUIProvider implementation
     */
    protected void bindUIProvider() {
        bind(ScopedUIProvider.class).to(DefaultApplicationUIProvider.class);
    }


    /**
     * Override with your UI bindings
     *
     * @param mapbinder
     */
    protected void addUIBindings(MapBinder<String, UI> mapbinder) {
        mapbinder.addBinding(DefaultApplicationUI.class.getName())
                 .to(DefaultApplicationUI.class);
    }

}