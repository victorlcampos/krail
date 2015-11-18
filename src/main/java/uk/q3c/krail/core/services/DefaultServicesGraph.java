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
import com.google.inject.Singleton;
import edu.uci.ics.jung.graph.DelegateForest;
import edu.uci.ics.jung.graph.DirectedOrderedSparseMultigraph;
import edu.uci.ics.jung.graph.Forest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.q3c.util.CycleDetectedException;

import javax.annotation.Nonnull;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation for {@link ServicesGraph}
 * <p>
 * Created by David Sowerby on 24/10/15.
 */
@Singleton
public class DefaultServicesGraph implements ServicesGraph {
    private static Logger log = LoggerFactory.getLogger(DefaultServicesGraph.class);
    private Forest<ServiceKey, ServiceEdge> graph;
    private Map<ServiceKey, Service> keyMap;
    private Forest<Class<? extends Service>, ServiceEdge> classGraph;
    private Forest<Service, ServiceEdge> instanceGraph;
    @Inject
    public DefaultServicesGraph(Set<DependencyDefinition> configuredDependencies) {
        classGraph = new DelegateForest<>(new DirectedOrderedSparseMultigraph<>());
        instanceGraph = new DelegateForest<>(new DirectedOrderedSparseMultigraph<>());
        keyMap = new HashMap<>();
        processConfiguredDependencies(configuredDependencies);
    }

    /**
     * reads dependency definitions from set provided via Guice and creates dependencies
     *
     * @param configuredDependencies
     *         dependency definitions provided via Guice
     */
    private void processConfiguredDependencies(Set<DependencyDefinition> configuredDependencies) {
        configuredDependencies.forEach(this::createDependency);
    }

    @Override
    public void alwaysDependsOn(@Nonnull Class<? extends Service> dependant, @Nonnull Class<? extends Service> dependency) {
        createDependency(dependant, dependency, Dependency.Type.ALWAYS_REQUIRED);
    }

    private void createDependency(Class<? extends Service> dependant, Class<? extends Service> dependency, Dependency.Type type) {
        checkNotNull(dependant, dependency);
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
     * @param parentNode
     *         dependant
     * @param childNode
     *         dependency
     *
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
        createDependency(dependant, dependency, Dependency.Type.REQUIRED_ONLY_AT_START);
    }

    @Override
    public void optionallyUses(@Nonnull Class<? extends Service> dependant, @Nonnull Class<? extends Service> dependency) {
        createDependency(dependant, dependency, Dependency.Type.OPTIONAL);
    }

    @Override
    public void addDependency(@Nonnull Class<? extends Service> dependant, @Nonnull Class<? extends Service> dependency, Dependency.Type type) {
        createDependency(dependant, dependency, type);
    }


    // ----------------------------------- find dependencies -------------------------------------

    @Override
    @Nonnull
    public List<Class<? extends Service>> findDependenciesOnlyRequiredAtStartFor(@Nonnull Class<? extends Service> dependant) {
        checkNotNull(dependant);
        return buildDependencies(dependant, Selection.REQUIRED_AT_START);
    }

    private List<Class<? extends Service>> buildDependencies(Class<? extends Service> dependant, Selection selection) {
        if (!classGraph.containsVertex(dependant)) {
            addService(dependant);
        }
        Collection<ServiceEdge> edges = classGraph.getOutEdges(dependant);
        List<Class<? extends Service>> selectedDependencies = new ArrayList<>();

        for (ServiceEdge edge : edges) {
            if (edgeSelected(edge, selection)) {
                selectedDependencies.add(classGraph.getOpposite(dependant, edge));
            }
        }
        return selectedDependencies;
    }

    @Override
    public void addService(@Nonnull Class<? extends Service> service) {
        checkNotNull(service);
        classGraph.addVertex(service);
    }

    //------------------------------------- find dependants --------------------------------------------

    private boolean edgeSelected(ServiceEdge edge, Selection selection) {
        switch (selection) {
            case REQUIRED_AT_START:
                return edge.requiredOnlyAtStart();
            case ALWAYS_REQUIRED:
                return edge.alwaysRequired();
            case OPTIONAL:
                return edge.optional();

            default:
                return false;
        }

    }

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
    //--------------------------------------------------------------------------------------

    @Override
    @Nonnull
    public List<Class<? extends Service>> findDependantsAlwaysRequiringDependency(@Nonnull Class<? extends Service> dependency) {
        checkNotNull(dependency);
        return buildDependants(dependency, Selection.ALWAYS_REQUIRED);
    }

    private List<Class<? extends Service>> buildDependants(Class<? extends Service> dependency, Selection selection) {
        if (!classGraph.containsVertex(dependency)) {
            addService(dependency);
        }
        Collection<ServiceEdge> edges = classGraph.getInEdges(dependency);
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
    @Nonnull
    public List<Service> servicesForKeys(@Nonnull List<Class<? extends Service>> serviceKeys) {
        checkNotNull(serviceKeys);
        List<Service> services = new ArrayList<>();
        List<Class<? extends Service>> keysWithoutService = new ArrayList<>();
        for (Class<? extends Service> serviceKey : serviceKeys) {
            Service service = keyMap.get(serviceKey);
            if (service != null) {
                services.add(service);
            } else {
                keysWithoutService.add(serviceKey);
            }

        }
        if (keysWithoutService.isEmpty()) {
            return services;
        }

        //some hopefully helpful error message construction
        StringBuilder buf = new StringBuilder("Service has not been registered for ");
        buf.append(keysWithoutService.size());
        buf.append(" ServiceKeys:\n");
        for (Class<? extends Service> key : keysWithoutService) {
            buf.append(key.toString());
            buf.append("\n");
        }
        log.error(buf.toString());
        throw new ServiceKeyException(buf.toString());
    }

    @Override
    public void registerService(@Nonnull Service service) {
        checkNotNull(service);
        keyMap.put(service.getServiceKey(), service);
    }

    @Override
    public boolean isRegistered(Service service) {
        return keyMap.containsKey(service.getServiceKey());
    }

    @Override
    public boolean isRegistered(Class<? extends Service> serviceKey) {
        return graph.getVertices()
                .contains(serviceKey);
    }

    @Override
    public ImmutableList<Service> registeredServices() {
        return ImmutableList.copyOf(keyMap.values());
    }

    private enum Selection {REQUIRED_AT_START, ALWAYS_REQUIRED, OPTIONAL}
}


