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

import com.google.common.collect.ImmutableList;
import uk.q3c.util.CycleDetectedException;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Provides a model of dependencies between Services.  Dependencies, of types specified by {@link Dependency.Type}  are
 * defined at {@link Service} class level, but are also held at {@link Service} instance level.  This enables, for example, a dependant
 * to be stopped automatically if one of its dependencies stops. <br><br>
 * Dependencies can be declared either using @{@link Dependency} annotations or by Guice configuration. (
 * ServicesModule} or at runtime through the {@link ServicesGraphRuntimeUserInterface}<br> <br> Note that when a call is
 * made to any method, when a {@link ServiceKey} parameter does not exist in the graph, that {@link ServiceKey} is added
 * to the graph automatically.   For example, a call to {@link #findOptionalDependencies} with a ServiceKey which has
 * not yet been added to the graph, will add the key, and return an empty list.
 * <p>
 * Throws a {@link CycleDetectedException} If a dependency is created which causes a loop (Service A depends on B which
 * depends on A)
 * <p>
 * /** Implementation works in conjunction with {@link AbstractService} to ensure that dependencies between services are
 * managed appropriately.  The dependencies are defined by configuring {@link ServicesGraph}.  {@link AbstractService}
 * delegates calls to start() and stop() to this interface, as should any Service implementation which continues to use
 * this interface
 * <p>
 * Created by David Sowerby on 24/10/15.
 */

public interface ServicesGraph {


    /**
     * The {@code dependant} service always depends on {@code dependency}.  Thus:<ol><li>if {@code dependency} does not
     * start,{@code dependant} cannot start</li><li>if {@code dependency} stops or fails after {@code dependant} has
     * started,{@code dependency} stops or fails </li></ol>
     *
     * @param dependant  the Service which depends on {@code dependency}.  Will be added to the graph if not already
     *                   added.
     * @param dependency the Service on which {@code dependant} depends.  Will be added to the graph if not already
     *                   added.
     * @throws CycleDetectedException if a loop is created by forming this dependency
     */
    void alwaysDependsOn(@Nonnull Class<? extends Service> dependant, @Nonnull Class<? extends Service> dependency);

    /**
     * The {@code dependant} service depends on {@code dependency}, but only in order to start - for example, {@code
     * dependency} may just provide some configuration data in order for {@code dependant} to start.  Thus:<ol><li>if
     * {@code dependency} does not start,{@code dependant} cannot start</li><li>if {@code dependency} stops or fails
     * after {@code dependant} has started,{@code dependency} will continue</li></ol>
     *
     * @param dependant  the Service which depends on {@code dependency}.  Will be added to the graph if not already
     *                   added.
     * @param dependency the Service on which {@code dependant} depends.  Will be added to the graph if not already
     *                   added.
     * @throws CycleDetectedException if a loop is created by forming this dependency
     */
    void requiresOnlyAtStart(@Nonnull Class<? extends Service> dependant, @Nonnull Class<? extends Service> dependency);

    /**
     * The {@code dependant} service will attempt to use {@code dependency} if it is available, but will start and
     * continue to run without it.  Thus:<ol><li>if {@code dependency} does not start,{@code dependant} will still
     * start.  Note, however, that {@code dependant} will wait until {@code dependency} has either started or failed
     * before commencing its own start process.</li><li>if {@code dependency} stops or fails after {@code dependant} has
     * started,{@code dependency} will continue</li></ol>
     *
     * @param dependant  the Service which depends on {@code dependency}. Will be added to the graph if not already
     *                   added.
     * @param dependency the Service on which {@code dependant} depends.  Will be added to the graph if not already
     *                   added.
     * @throws CycleDetectedException if a loop is created by forming this dependency
     */
    void optionallyUses(@Nonnull Class<? extends Service> dependant, @Nonnull Class<? extends Service> dependency);

    /**
     * Equivalent to {@link #optionallyUses}, {@link #requiresOnlyAtStart} or {@link #alwaysDependsOn} depending on the
     * value of {@code type}
     *
     * @param dependant  the Service which depends on {@code dependency}. Will be added to the graph if not already
     *                   added.
     * @param dependency the Service on which {@code dependant} depends.  Will be added to the graph if not already
     *                   added.
     * @param type       the type of {@link Dependency)
     * @throws CycleDetectedException if a loop is created by forming this dependency
     */
    void addDependencyInstance(@Nonnull Class<? extends Service> dependant, @Nonnull Class<? extends Service> dependency, Dependency.Type type);


    /**
     * Returns a list of all services which are required to be started only to start the {@code dependant} - once the
     * dependant has started, they are no longer required.  To obtain a complete set of dependencies needed to start a
     * dependant, combine the results of this method and {@link #findDependenciesAlwaysRequiredFor}
     *
     * @param dependant the Service to identify the dependencies for
     * @return a list of all service which must be started before the {@code dependant} can start
     */
    @Nonnull
    List<Class<? extends Service>> findDependenciesOnlyRequiredAtStartFor(@Nonnull Class<? extends Service> dependant);

    /**
     * Returns a list of all services which are optionally started before the {@code dependant} starts.  Note that the
     * {@code dependant} still wiats for optional dependencies to either start or fail, before starting itself.  This is
     * to enable the {@code dependant} to use the state of optional services in its start up logic.
     *
     * @param dependant the Service to identify the dependencies for
     * @return a list of all services which are required to be started only to start the {@code dependant}
     */
    @Nonnull
    List<Class<? extends Service>> findOptionalDependencies(@Nonnull Class<? extends Service> dependant);


    /**
     * Returns a list of all services which either must be must always be running for {@code dependant} to continue
     * running.
     *
     * @param dependant the Service to identify the dependencies for
     * @return a list of all services which either must be must always be running for {@code dependant} to continue
     * running.
     */
    @Nonnull
    List<Class<? extends Service>> findDependenciesAlwaysRequiredFor(@Nonnull Class<? extends Service> dependant);

    /**
     * Returns a list of all services which have declared that {@code dependency} must be running in order for them to
     * continue running.
     */
    @Nonnull
    List<Class<? extends Service>> findDependantsAlwaysRequiringDependency(@Nonnull Class<? extends Service> dependency);

    /**
     * Returns a list of all services which have declared that they use {@code dependency} as an optional dependency
     */
    @Nonnull
    List<Class<? extends Service>> findDependantsOptionallyUsingDependency(@Nonnull Class<? extends Service> dependency);

    /**
     * Returns a list of all dependants which have declared that {@code dependency} is only required in order to start
     * the dependant.  The result therefore does not include those returned by {@link
     * #findDependantsAlwaysRequiringDependency)}
     */
    @Nonnull
    List<Class<? extends Service>> findDependantsRequiringDependencyOnlyToStart(@Nonnull Class<? extends Service> dependency);

//    /**
//     * Returns a list of {@link Service} instances corresponding to the provided list of {@link ServiceKey}s.  Throws a {@link ServiceKeyException} if
// there is
//     * no
//     * Service mapped to any {@link ServiceKey}, but always returns all the mappings that are available.
//     *
//     * @param serviceKeys
//     *         the keys to look up
//     *
//     * @return Returns a list of {@link Service} of all instances which correspond to an entry in the provided list of {@link ServiceKey}s.
//     *
//     * @throws ServiceKeyException
//     *         if there is no Service mapped to any {@link ServiceKey}
//     */
//    @Nonnull
//    List<Service> servicesForKeys(@Nonnull List<Class<? extends Service>> serviceKeys);

    /**
     * Registers a service (instance), usually as it is constructed.
     *
     * @param service the Service instance to register
     */
    void registerServiceInstance(@Nonnull Service service);

    /**
     * Registers a service class
     *
     * @param serviceClass the Service Class to register
     */

    void registerService(@Nonnull Class<? extends Service> serviceClass);

    /**
     * Returns true if {@code Service} is registered
     *
     * @param service the service to check for
     */
    boolean isRegistered(Service service);


    /**
     * Returns true if the {@code serviceKey} is registered.  There may however not be a Service associated with the key
     * yet.
     *
     * @param serviceKey the Service Class to look for
     * @return Returns true if the {@code serviceKey} is registered.  There may however not be a Service associated with
     * the key yet.
     */
    boolean isRegistered(Class<? extends Service> serviceKey);

    /**
     * returns an immutable list of currently registered services
     *
     * @return an immutable list of currently registered services
     */
    ImmutableList<Service> registeredServiceInstances();


    /**
     * It is unlikely that a developer would need to call this method directly, as generally the find**** methods are
     * more relevant.  This method returns 0 or more dependencies as Service instances for the {@code dependant}.
     *
     * @param dependant the Service for which dependencies are required.
     * @return instances of services which {@code dependant} depends on
     */
    List<Service> getDependencyInstances(Service dependant);


    /**
     * Starts any dependencies which are required in order to start {@code service}.  These are a combination of {@link
     * ServicesGraph#alwaysDependsOn (ServiceKey, ServiceKey)} and {@link ServicesGraph#requiresOnlyAtStart(ServiceKey,
     * ServiceKey)}.
     *
     * @param dependant the service to start the dependencies for
     * @return true if all required dependencies attain a state of {@link Service.State#STARTED}, false if any
     * dependency fails to do so
     */
    boolean startDependenciesFor(Service dependant);

    /**
     * Stops all dependants which have declared that {@code dependency} must be running in order for them to continue
     * running (see {@link ServicesGraph#alwaysDependsOn(ServiceKey, ServiceKey)}).
     *
     * @param dependency       the dependency which requires its dependants to be stopped
     * @param dependencyFailed if true, the dependency has called this method because it failed, if false, the
     *                         dependency has been stopped
     */
    void stopDependantsOf(Service dependency, boolean dependencyFailed);

    /**
     * Stops all services.  Usually only used during shutdown
     */
    void stopAllServices();
}

