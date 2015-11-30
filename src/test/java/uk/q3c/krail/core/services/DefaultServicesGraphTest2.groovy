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
import spock.lang.Specification
import uk.q3c.krail.UnitTestFor
import uk.q3c.krail.i18n.Translate
/**
 *
 * Created by david on 27/10/15.
 */
@UnitTestFor(DefaultServicesGraph)
class DefaultServicesGraphTest2 extends Specification {


    ServicesGraph graph
    def translate = Mock(Translate)
    ServicesGraph servicesGraph

    Service sA = new MockServiceA()
    Service sB = new MockServiceB()
    Service sC = new MockServiceC()
    Service sD = new MockServiceD()


    def setup() {
        Set<DependencyDefinition> dependencyDefinitions = new HashSet<>()
        servicesGraph = new DefaultServicesGraph(dependencyDefinitions)
        translate.from(_, _) >> "translated key"
        graph = new DefaultServicesGraph(servicesGraph, translate)


        servicesGraph.registerService(sA.getClass())
        servicesGraph.registerService(sB.getClass())
        servicesGraph.registerService(sC.getClass())
        servicesGraph.registerService(sD.getClass())


    }

    def "no dependencies, 'startDependenciesFor' returns true"() {
        given:

        servicesGraph.addService(sA.getClass() as Class<? extends Service>)

        when:

        boolean result = graph.startDependenciesFor(sA)

        then:

        result

    }


    def "alwaysDependsOn dependencies 'start()' is called, 'startDependenciesFor' returns true"() {
        given:


        servicesGraph.alwaysDependsOn(sA.getClass(), sB.getClass())
        servicesGraph.alwaysDependsOn(sA.getClass(), sC.getClass())

        when:

        boolean result = graph.startDependenciesFor(sA)

        then: //dependencies started

        result
        sB.isStarted()
        sC.isStarted()

        sB.getCallsToStart() == 1;
        sC.getCallsToStart() == 1;

    }

    def "alwaysDependsOn, dependsOnAtStart and optional dependencies 'start()' is called, 'startDependenciesFor' returns true"() {
        given:


        servicesGraph.alwaysDependsOn(sA.getClass(), sB.getClass())
        servicesGraph.requiresOnlyAtStart(sA.getClass(), sC.getClass())
        servicesGraph.optionallyUses(sA.getClass(), sD.getClass())

        when:

        boolean result = graph.startDependenciesFor(sA)

        then: //dependencies started

        result
        sB.isStarted()
        sC.isStarted()
        sD.isStarted()

        sB.getCallsToStart() == 1;
        sC.getCallsToStart() == 1;
        sD.getCallsToStart() == 1;

    }

    def "alwaysDependsOn, dependsOnAtStart and optional dependencies 'start()' is called, alwaysDependsOn fails, 'startDependenciesFor' returns false"() {
        given:

        sB.failToStart(true)
        servicesGraph.alwaysDependsOn(sA.getClass(), sB.getClass())
        servicesGraph.requiresOnlyAtStart(sA.getClass(), sC.getClass())
        servicesGraph.optionallyUses(sA.getClass(), sD.getClass())

        when:

        boolean result = graph.startDependenciesFor(sA)

        then: //dependencies started

        !result
        !sB.isStarted()
        sC.isStarted()
        sD.isStarted()

        sB.getCallsToStart() == 1;
        sC.getCallsToStart() == 1;
        sD.getCallsToStart() == 1;

    }

    def "alwaysDependsOn, dependsOnAtStart and optional dependencies 'start()' is called, dependsOnAtStart fails, 'startDependenciesFor' returns false"() {
        given:

        sC.failToStart(true)
        servicesGraph.alwaysDependsOn(sA.getClass(), sB.getClass())
        servicesGraph.requiresOnlyAtStart(sA.getClass(), sC.getClass())
        servicesGraph.optionallyUses(sA.getClass(), sD.getClass())

        when:

        boolean result = graph.startDependenciesFor(sA)

        then: //dependencies started

        !result
        sB.isStarted()
        !sC.isStarted()
        sD.isStarted()

        sB.getCallsToStart() == 1;
        sC.getCallsToStart() == 1;
        sD.getCallsToStart() == 1;

    }

    def "alwaysDependsOn, dependsOnAtStart and optional dependencies 'start()' is called, optionallyUses fails, 'startDependenciesFor' returns true"() {
        given:

        sD.failToStart(true)
        servicesGraph.alwaysDependsOn(sA.getClass(), sB.getClass())
        servicesGraph.requiresOnlyAtStart(sA.getClass(), sC.getClass())
        servicesGraph.optionallyUses(sA.getClass(), sD.getClass())

        when:

        boolean result = graph.startDependenciesFor(sA)

        then: //dependencies started

        result
        sB.isStarted()
        sC.isStarted()
        !sD.isStarted()

        sB.getCallsToStart() == 1;
        sC.getCallsToStart() == 1;
        sD.getCallsToStart() == 1;

    }


    def "dependency stops, dependants with 'alwaysRequired' also stop, optional and 'requiredAtStart' do not"() {
        given:

        servicesGraph.alwaysDependsOn(sA.getClass(), sB.getClass())
        servicesGraph.requiresOnlyAtStart(sC.getClass(), sB.getClass())
        servicesGraph.optionallyUses(sD.getClass(), sB.getClass())

        sA.start()
        sB.start()
        sC.start()
        sD.start()


        when:

        boolean result = graph.stopDependantsOf(sB, false)

        then: //dependencies started

        sA.isStopped()
        sA.getState().equals(Service.State.DEPENDENCY_STOPPED)
        sC.isStarted()
        sD.isStarted()

        sA.getCallsToStop() == 1;
        sC.getCallsToStop() == 0;
        sD.getCallsToStop() == 0;
    }

    def "dependency fails, dependants with 'alwaysRequired' also fail, optional and 'requiredAtStart' do not"() {
        given:

        servicesGraph.alwaysDependsOn(sA.getClass(), sB.getClass())
        servicesGraph.requiresOnlyAtStart(sC.getClass(), sB.getClass())
        servicesGraph.optionallyUses(sD.getClass(), sB.getClass())

        sA.start()
        sB.start()
        sC.start()
        sD.start()


        when:

        boolean result = graph.stopDependantsOf(sB, true)

        then: //dependencies started

        sA.isStopped()
        sA.getState().equals(Service.State.DEPENDENCY_FAILED)
        sC.isStarted()
        sD.isStarted()

        sA.getCallsToStop() == 1;
        sC.getCallsToStop() == 0;
        sD.getCallsToStop() == 0;
    }

    def "stop all services"() {
        given:

        servicesGraph.alwaysDependsOn(sA.getClass(), sB.getClass())
        servicesGraph.requiresOnlyAtStart(sC.getClass(), sB.getClass())
        servicesGraph.optionallyUses(sD.getClass(), sB.getClass())

        sA.start()
        sB.start()
        sC.start()
        sD.start()

        when:

        graph.stopAllServices()

        then:

        sA.isStopped()
        sB.isStopped()
        sC.isStopped()
        sD.isStopped()
    }
}