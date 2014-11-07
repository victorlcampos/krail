/*
 * Copyright (c) 2014 David Sowerby
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for
 * the specific language governing permissions and limitations under the License.
 */

package uk.q3c.krail.base.view.component;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.mycila.testing.junit.MycilaJunitRunner;
import com.mycila.testing.plugin.guice.GuiceContext;
import com.mycila.testing.plugin.guice.ModuleProvider;
import fixture.MockCurrentLocale;
import fixture.ReferenceUserSitemap;
import fixture.TestI18NModule;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.q3c.krail.base.guice.vsscope.VaadinSessionScopeModule;
import uk.q3c.krail.base.navigate.Navigator;
import uk.q3c.krail.base.navigate.StrictURIFragmentHandler;
import uk.q3c.krail.base.navigate.URIFragmentHandler;
import uk.q3c.krail.base.navigate.sitemap.UserSitemap;
import uk.q3c.krail.base.navigate.sitemap.comparator.DefaultUserSitemapSorters;
import uk.q3c.krail.base.user.opt.DefaultUserOption;
import uk.q3c.krail.base.user.opt.DefaultUserOptionStore;
import uk.q3c.krail.base.user.opt.UserOption;
import uk.q3c.krail.base.user.opt.UserOptionStore;
import uk.q3c.krail.i18n.CurrentLocale;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MycilaJunitRunner.class)
@GuiceContext({VaadinSessionScopeModule.class, TestI18NModule.class})
public class DefaultUserNavigationTreeBuilderTest {
    CurrentLocale currentLocale = new MockCurrentLocale();

    DefaultUserNavigationTreeBuilder builder;

    @Inject
    ReferenceUserSitemap userSitemap;

    @Inject
    DefaultUserSitemapSorters sorters;
    @Inject
    DefaultUserOption userOption;
    @Mock
    Navigator navigator;
    private DefaultUserNavigationTree userNavigationTree;

    @Before
    public void setUp() throws Exception {
        builder = new DefaultUserNavigationTreeBuilder(userSitemap);
        userNavigationTree = new DefaultUserNavigationTree(userSitemap, navigator, userOption, builder, sorters);
    }

    @Test
    public void construct() {
        // given
        // when

        // then
        assertThat(builder.getUserNavigationTree()).isEqualTo(userNavigationTree);
    }

    @ModuleProvider
    protected AbstractModule module() {
        return new AbstractModule() {

            @Override
            protected void configure() {
                bind(URIFragmentHandler.class).to(StrictURIFragmentHandler.class);
                bind(UserOption.class).to(DefaultUserOption.class);
                bind(UserOptionStore.class).to(DefaultUserOptionStore.class);
            }

            @Provides
            protected UserSitemap sitemapProvider() {
                return userSitemap;
            }

        };
    }

}