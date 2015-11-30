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

package uk.q3c.krail.core.services;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;

import static com.google.inject.multibindings.Multibinder.newSetBinder;

/**
 * A helper base class which can be used where the developer wishes to dependencies between services using Guice.
 * Remember to invoke super.configure from the sub-class configure()
 * <p>
 * Created by David Sowerby on 13/11/15.
 */
public class AbstractServiceModule extends AbstractModule {
    private Multibinder<DependencyDefinition> dependencies;
    //other modules may add to this
    private MapBinder<Class<? extends Service>, Service> registeredServices;


    @Override
    protected void configure() {
        dependencies = newSetBinder(binder(), DependencyDefinition.class);
        //use TypeLiteral for one parameter have to use it for both
        TypeLiteral<Class<? extends Service>> serviceClassLiteral = new TypeLiteral<Class<? extends Service>>() {
        };
        TypeLiteral<Service> serviceLiteral = new TypeLiteral<Service>() {
        };
        registeredServices = MapBinder.newMapBinder(binder(), serviceClassLiteral, serviceLiteral);
    }

    /**
     * Define a dependency - identical in function to calling {@link ServicesGraph#addDependencyInstance(Class, Class,
     * Dependency.Type)}   Dependencies added here are injected into {@link ServicesGraph}
     */
    protected void addDependency(Class<? extends Service> dependant, Class<? extends Service> dependency, Dependency.Type type) {
        dependencies.addBinding()
                .toInstance(new DependencyDefinition(dependant, dependency, type));
    }

    protected void registerService(Class<? extends Service> serviceClass) {
        registeredServices.addBinding(serviceClass)
                .to(serviceClass);
    }
}
