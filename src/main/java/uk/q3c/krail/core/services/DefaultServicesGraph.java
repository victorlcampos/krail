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
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import edu.uci.ics.jung.graph.DelegateForest;
import edu.uci.ics.jung.graph.DirectedOrderedSparseMultigraph;
import edu.uci.ics.jung.graph.Forest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.q3c.krail.i18n.Translate;
import uk.q3c.util.CycleDetectedException;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static uk.q3c.krail.core.services.Dependency.Type;
import static uk.q3c.krail.core.services.Service.State.*;

/**
 * Default implementation for {@link ServicesGraph}
 * <p>
 * Created by David Sowerby on 24/10/15.
 */
@Singleton
public class DefaultServicesGraph implements ServicesGraph {
    private static Logger log = LoggerFactory.getLogger(DefaultServicesGraph.class);
    private Forest<Class<? extends Service>, ServiceEdge> classGraph;
    private Forest<Service, ServiceEdge> instanceGraph;
    private Map<Class<? extends Service>, Provider<Service>> serviceMap;
    private Translate translate;

    @Inject
    public DefaultServicesGraph(Set<DependencyDefinition> configuredDependencies, Map<Class<? extends Service>, Provider<Service>> serviceMap, Translate
            translate) {
        this.serviceMap = serviceMap;
        this.translate = translate;
        classGraph = new DelegateForest<>(new DirectedOrderedSparseMultigraph<>());
        instanceGraph = new DelegateForest<>(new DirectedOrderedSparseMultigraph<>());
        processConfiguredDependencies(configuredDependencies);
    }

    /**
     * reads dependency definitions from set provided via Guice and creates dependencies
     *
     * @param configuredDependencies dependency definitions provided via Guice
     */
    private void processConfiguredDependencies(Set<DependencyDefinition> configuredDependencies) {
        configuredDependencies.forEach(this::createDependency);
    }

    @Override
    public void alwaysDependsOn(@Nonnull Class<? extends Service> dependant, @Nonnull Class<? extends Service> dependency) {
        createDependency(dependant, dependency, Type.ALWAYS_REQUIRED);
    }

    private void createDependency(Class<? extends Service> dependant, Class<? extends Service> dependency, Type type) {
        checkNotNull(dependant);
        checkNotNull(dependency);
        checkNotNull(type);
        classGraph.addVertex(dependant);
        classGraph.addVertex(dependency);
        ServiceEdge edge = new ServiceEdge(type);
        classGraph.addEdge(edge, dependant, dependency);
        if (detectCycle(dependant, dependency)) {
            throw new CycleDetectedException("Creating dependency from " + dependant + " to " + dependency + " has caused a loop");
        }
    }

    /**
     * Checks the proposed connection between parent and child nodes, and returns true if a cycle would be created by
     * adding the child to the parent, or false if not
     *
     * @param parentNode dependant
     * @param childNode  dependency
     * @return true if a cycle detected
     */
    protected boolean detectCycle(Class<? extends Service> parentNode, Class<? extends Service> childNode) {
        if (parentNode.equals(childNode)) {
            return true;
        }
        Stack<Class<? extends Service>> stack = new Stack<>();
        stack.push(parentNode);
        while (!stack.isEmpty()) {
            Class<? extends Service> node = stack.pop();
            Collection<Class<? extends Service>> predecessors = classGraph.getPredecessors(node);
            if (predecessors != null) {
                for (Class<? extends Service> pred : predecessors) {
                    if (pred == childNode) {
                        return true;
                    }
                }
                stack.addAll(predecessors);
            }
        }
        return false;

    }

    private void createDependency(DependencyDefinition dependencyDefinition) {
        createDependency(dependencyDefinition.getDependant(), dependencyDefinition.getDependency(), dependencyDefinition.getType());
    }

    @Override
    public void requiresOnlyAtStart(@Nonnull Class<? extends Service> dependant, @Nonnull Class<? extends Service> dependency) {
        createDependency(dependant, dependency, Type.REQUIRED_ONLY_AT_START);
    }

    @Override
    public void optionallyUses(@Nonnull Class<? extends Service> dependant, @Nonnull Class<? extends Service> dependency) {
        createDependency(dependant, dependency, Type.OPTIONAL);
    }

    @Override
    public void addDependencyInstance(@Nonnull Class<? extends Service> dependant, @Nonnull Class<? extends Service> dependency, Type type) {
        createDependency(dependant, dependency, type);
    }

    @Override
    @Nonnull
    public List<Class<? extends Service>> findDependenciesOnlyRequiredAtStartFor(@Nonnull Class<? extends Service> dependant) {
        checkNotNull(dependant);
        return buildDependencies(dependant, Selection.REQUIRED_AT_START);
    }


    // ----------------------------------- find dependencies -------------------------------------

    private List<Class<? extends Service>> buildDependencies(Class<? extends Service> dependant, Selection selection) {
        if (!classGraph.containsVertex(dependant)) {
            registerService(dependant);
        }
        Collection<ServiceEdge> edges = getDependenciesEdges(dependant);
        List<Class<? extends Service>> selectedDependencies = new ArrayList<>();

        for (ServiceEdge edge : edges) {
            if (edgeSelected(edge, selection)) {
                selectedDependencies.add(classGraph.getOpposite(dependant, edge));
            }
        }
        return selectedDependencies;
    }

    private boolean edgeSelected(ServiceEdge edge, Selection selection) {
        switch (selection) {
            case REQUIRED_AT_START:
                return edge.requiredOnlyAtStart();
            case ALWAYS_REQUIRED:
                return edge.alwaysRequired();
            case OPTIONAL:
                return edge.optional();
            case ALL:
                return true;

            default:
                return false;
        }

    }


    //------------------------------------- find dependants --------------------------------------------

    @Override
    @Nonnull
    public List<Class<? extends Service>> findOptionalDependencies(@Nonnull Class<? extends Service> dependant) {
        checkNotNull(dependant);
        return buildDependencies(dependant, Selection.OPTIONAL);
    }

    @Override
    @Nonnull
    public List<Class<? extends Service>> findDependenciesAlwaysRequiredFor(@Nonnull Class<? extends Service> dependant) {
        checkNotNull(dependant);
        return buildDependencies(dependant, Selection.ALWAYS_REQUIRED);
    }

    @Override
    @Nonnull
    public List<Class<? extends Service>> findDependantsAlwaysRequiringDependency(@Nonnull Class<? extends Service> dependency) {
        checkNotNull(dependency);
        return buildDependants(dependency, Selection.ALWAYS_REQUIRED);
    }
    //--------------------------------------------------------------------------------------

    private List<Class<? extends Service>> buildDependants(Class<? extends Service> dependency, Selection selection) {
        if (!classGraph.containsVertex(dependency)) {
            registerService(dependency);
        }
        Collection<ServiceEdge> edges = getDependantsEdges(dependency);
        List<Class<? extends Service>> selectedDependencies = new ArrayList<>();

        for (ServiceEdge edge : edges) {
            if (edgeSelected(edge, selection)) {
                selectedDependencies.add(classGraph.getOpposite(dependency, edge));
            }
        }
        return selectedDependencies;
    }

    @Override
    @Nonnull
    public List<Class<? extends Service>> findDependantsOptionallyUsingDependency(@Nonnull Class<? extends Service> dependency) {
        checkNotNull(dependency);
        return buildDependants(dependency, Selection.OPTIONAL);
    }

    @Override
    @Nonnull
    public List<Class<? extends Service>> findDependantsRequiringDependencyOnlyToStart(@Nonnull Class<? extends Service> dependency) {
        checkNotNull(dependency);
        return buildDependants(dependency, Selection.REQUIRED_AT_START);
    }

    @Override
    public void registerServiceInstance(@Nonnull Service service) {
        checkNotNull(service);
        instanceGraph.addVertex(service);
    }

    @Override
    public void registerService(@Nonnull Class<? extends Service> serviceClass) {
        checkNotNull(serviceClass);
        classGraph.addVertex(serviceClass);
    }

    @Override
    public boolean isRegistered(@Nonnull Service service) {
        checkNotNull(service);
        return instanceGraph.containsVertex(service);
    }

    @Override
    public boolean isRegistered(Class<? extends Service> serviceKey) {
        return classGraph.getVertices()
                .contains(serviceKey);
    }

    @Override
    public ImmutableList<Service> registeredServiceInstances() {
        return ImmutableList.copyOf(instanceGraph.getVertices());
    }

    @Override
    public List<Service> getDependencyInstances(@Nonnull Service dependant) {
        checkNotNull(dependant);
        throw new RuntimeException("Not yet implemented");
    }

    /**
     * Maps a dependant to a dependency, at instance level (as opposed to class level)
     *
     * @param dependant  the Service instance which depends on {@code dependency}.
     * @param dependency the Service on which {@code dependant} depends.
     * @param type       a ServiceEdge representing the {@link Type} of dependency
     */
    protected void addDependencyInstance(@Nonnull Service dependant, @Nonnull Service dependency, ServiceEdge type) {
        checkNotNull(dependant);
        checkNotNull(dependency);
        instanceGraph.addVertex(dependant);
        instanceGraph.addVertex(dependency);
        ServiceEdge edge = new ServiceEdge(type);
        instanceGraph.addEdge(edge, dependant, dependency);
//does not perform a cycle check, that's already been done at class level

    }


    public List<Class<? extends Service>> findAllDependencies(Class<? extends Service> dependant) {
        //collect all the optional & required dependencies (they will be started in parallel, so order is irrelevant)
        final List<Class<? extends Service>> dependencyServiceClasses = findOptionalDependencies(dependant);

        // we need dependencies which are always required, and those only required at start
        List<Class<? extends Service>> requiredServiceClasses = findDependenciesOnlyRequiredAtStartFor(dependant);
        requiredServiceClasses.addAll(findDependenciesAlwaysRequiredFor(dependant));
        dependencyServiceClasses.addAll(requiredServiceClasses);
        return dependencyServiceClasses;
    }

    @Override
    public boolean startDependenciesFor(Service dependant) {
        checkNotNull(dependant);
        List<Class<? extends Service>> dependencyServiceClasses = findAllDependencies(dependant.getClass());


        //instantiate each dependency that is missing.  These should be lightweight and therefore quick to create
        //scope may vary

        List<Service> servicesToStart = new ArrayList<>();
        List<Service> existingDependencyInstances = getDependencyInstances(dependant);
        for (Class<? extends Service> dependencyServiceClass : dependencyServiceClasses) {

            Service dependency = null;

            // if there is already an instance, re-use it
            for (Service existingInstance : existingDependencyInstances) {
                if (dependencyServiceClass.isAssignableFrom(existingInstance.getClass())) {
                    dependency = existingInstance;
                    break;
                }
            }

            // if there is no instance, get one
            if (dependency == null) {
                Provider<Service> serviceProvider = serviceMap.get(dependencyServiceClass);
                if (serviceProvider == null) {
                    throw new ServiceRegistrationException("Service class must be registered");
                }
                dependency = serviceProvider.get();
                ServiceEdge dependencyType = getDependencyType(dependant.getClass(), dependency.getClass());
                addDependencyInstance(dependant, dependency, dependencyType);
            }
            //whether new or existing instance, add it to the list for starting
            servicesToStart.add(dependency);
        }


        //Call service.start() for all dependencies in parallel
        ExecutorService executor = createStartExecutor(servicesToStart.size());
        List<Future<ServiceStatus>> futures = invokeServicesStateChange(executor, servicesToStart, StateChange.START);


        //Get result from each Future (get() will block until result returned)
        //Optional results are ignored, otherwise only start dependant if all dependencies started successfully
        boolean allRequiredDependenciesStarted = true;
        for (Future<ServiceStatus> future : futures) {
            try {
                ServiceStatus result = future.get();
                if (!findOptionalDependencies(dependant.getClass()).contains(result.getServiceClass())) {
                    if (result.getState() != STARTED) {
                        allRequiredDependenciesStarted = false;
                    }
                }
            } catch (InterruptedException e) {
                log.error("ServicesGraph thread interrupted while waiting for dependency to start");

            } catch (ExecutionException e) {
                log.error("Error occurred in thread spawned to start a service", e);
            }
        }


        closeExecutor(executor, "startDependenciesFor", dependant);


        return allRequiredDependenciesStarted;
    }

    protected List<Future<ServiceStatus>> invokeServicesStateChange(ExecutorService executor, List<Service> services, StateChange stateChange) {

        List<Future<ServiceStatus>> futures = new ArrayList<>();

        //Submit each to executor, and hold the Future for it
        for (Service dependency : services) {
            Callable<ServiceStatus> task = null;

            switch (stateChange) {
                case START:
                    task = dependency::start;
                    break;
                case STOP:
                    task = dependency::stop;
                    break;
                case FAIL:
                    task = dependency::fail;
                    break;
            }

            Future<ServiceStatus> future = executor.submit(task);
            futures.add(future);
        }
        return futures;
    }

    public ServiceEdge getDependencyType(@Nonnull Class<? extends Service> dependant, @Nonnull Class<? extends Service> dependency) {
        checkNotNull(dependant);
        checkNotNull(dependency);

        List<Class<? extends Service>> selectedDependencies = new ArrayList<>();
        List<Class<? extends Service>> dependencies = buildDependencies(dependant, Selection.ALL);
        if (!dependencies.contains(dependency)) {
            return null;
        }


        throw new RuntimeException("Not yet implemented");
    }

    private Collection<ServiceEdge> getDependenciesEdges(@Nonnull Class<? extends Service> dependant) {
        return classGraph.getOutEdges(dependant);
    }

    private Collection<ServiceEdge> getDependantsEdges(@Nonnull Class<? extends Service> dependency) {
        return classGraph.getInEdges(dependency);
    }

    /**
     * Stops the @{code executor} with appropriate timeouts and logging
     *
     * @param executor   the Executor to be shut down.
     * @param caller     the method making the call, purely for logging
     * @param instigator the Service which caused the call to be made, purley for logging
     */
    protected void closeExecutor(ExecutorService executor, String caller, Service instigator) {
        try {
            log.info("Closing Executor call via {}, initiated by {}", caller, translate.from(instigator.getNameKey(), Locale.UK));
            log.debug("attempt to shutdown executor");
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Thread interrupted while shutting down Executor");
        } finally {
            if (!executor.isTerminated()) {
                log.error("forcing shutdown");
            }
            executor.shutdownNow();
            log.info("Services Executor shutdown finished");
        }
    }

    /**
     * Provides a ExecutorService for use with {@link #startDependenciesFor(Service)}.  If you need to use a different thread pool configuration override
     * this method in a sub-class, and bind that sub-class to {@link ServicesGraph} in {@link ServicesModule}.
     *
     * @param servicesToStart the number of services which will be started using this ThreadPool
     * @return ExecutorService with ThreadPool set
     */
    @SuppressWarnings("UnusedParameters")
    @Nonnull
    protected ExecutorService createStartExecutor(int servicesToStart) {
        return Executors.newWorkStealingPool();
    }

    /**
     * Uses the #instanceGraph to identify dependants of {@code dependency}, and stops or fails the dependants as appropriate
     *
     * @param dependency       the dependency which requires its dependants to be stopped
     * @param dependencyFailed if true, the dependency failed, otherwise the dependency stopped
     */
    @Override
    public void stopDependantsOf(@Nonnull Service dependency, boolean dependencyFailed) {
        checkNotNull(dependency);
        final List<Class<? extends Service>> serviceKeys = findDependantsAlwaysRequiringDependency(dependency.getClass());
        List<Service> services = lookupServiceKeys(serviceKeys);
        ExecutorService executor = createStopExecutor(services.size());
        List<Future<ServiceStatus>> futures = new ArrayList<>();

        //Submit each to executor, and hold the Future for it
        for (Service dependant : services) {
            Callable<ServiceStatus> task = () -> {
                if (dependencyFailed) {
                    return dependant.stop(DEPENDENCY_FAILED);
                } else {
                    return dependant.stop(DEPENDENCY_STOPPED);
                }
            };
            Future<ServiceStatus> future = executor.submit(task);
            futures.add(future);
        }

        //wait until all threads complete
        for (Future<ServiceStatus> future : futures) {
            try {
                // just block until task complete
                future.get();
            } catch (InterruptedException e) {
                log.error("ServicesGraph thread interrupted while waiting for dependency to start");

            } catch (ExecutionException e) {
                log.error("Error occurred in thread spawned to start a service", e);
            }
        }
        closeExecutor(executor, "StopDependantsOf", dependency);
    }

    /**
     * Provides a ExecutorService for use with {@link #stopDependantsOf}.  If you need to use a different thread pool configuration override this method in a
     * sub-class, and bind that sub-class to {@link ServicesGraph} in {@link ServicesModule}.
     *
     * @param servicesToStop the number of services which will be started using this ThreadPool
     * @return ExecutorService with ThreadPool set
     */
    @SuppressWarnings("UnusedParameters")
    @Nonnull
    protected ExecutorService createStopExecutor(int servicesToStop) {
        return Executors.newWorkStealingPool();
    }

    @Override
    public void stopAllServices() {
        log.info("Stopping all services");
        registeredServiceInstances()
                .forEach(Service::stop);
    }

    private enum Selection {REQUIRED_AT_START, ALWAYS_REQUIRED, OPTIONAL, ALL}

    private enum StateChange {START, STOP, FAIL}
}


