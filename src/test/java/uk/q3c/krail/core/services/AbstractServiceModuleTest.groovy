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

package uk.q3c.krail.core.services

import com.google.inject.*
import spock.lang.Specification
import uk.q3c.krail.UnitTestFor
import uk.q3c.krail.core.config.ApplicationConfigurationService
import uk.q3c.krail.core.navigate.sitemap.SitemapService

@UnitTestFor(AbstractServiceModule)
//@UseModules([])
class AbstractServiceModuleTest extends Specification {

    Class<? extends Service> serviceKeyA = SitemapService.class
    Class<? extends Service> serviceKeyB = ApplicationConfigurationService.class
    Class<? extends Service> serviceKeyC = TestService.class


    class TestServiceModule extends AbstractServiceModule {
        @Override
        protected void configure() {
            super.configure()
            addDependency(serviceKeyA, serviceKeyB, Dependency.Type.REQUIRED_ONLY_AT_START)
            addDependency(serviceKeyA, serviceKeyC, Dependency.Type.OPTIONAL)
        }
    }


    def ""() {
        given:

        TypeLiteral<Set<DependencyDefinition>> lit = new TypeLiteral<Set<DependencyDefinition>>() {}
        Key key = Key.get(lit)

        when:
        Injector injector = Guice.createInjector(new TestServiceModule())

        then:

        injector.getBinding(key) != null

        Binding<Set<DependencyDefinition>> x = injector.getBinding(key)
        Set<DependencyDefinition> s = injector.getProvider(key).get()

        s.size() == 2

    }
}
