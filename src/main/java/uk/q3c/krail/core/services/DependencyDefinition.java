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

import javax.annotation.concurrent.Immutable;

/**
 * Used to define dependencies between {@link Service}s in the {@link ServicesModule}
 * <p>
 * Created by david on 11/11/15.
 */
@Immutable
public class DependencyDefinition {

    private final Class<? extends Service> dependant;
    private final Class<? extends Service> dependency;
    private final Dependency.Type type;

    public DependencyDefinition(Class<? extends Service> dependant, Class<? extends Service> dependency, Dependency.Type type) {
        this.dependant = dependant;
        this.dependency = dependency;
        this.type = type;
    }

    public Dependency.Type getType() {
        return type;
    }

    public Class<? extends Service> getDependency() {
        return dependency;
    }

    public Class<? extends Service> getDependant() {
        return dependant;
    }
}
