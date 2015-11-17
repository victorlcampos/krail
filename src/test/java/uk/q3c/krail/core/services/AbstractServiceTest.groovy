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

import com.google.inject.Inject
import net.engio.mbassy.bus.common.PubSubSupport
import spock.lang.Specification
import uk.q3c.krail.core.eventbus.BusMessage
import uk.q3c.krail.core.eventbus.GlobalBusProvider
import uk.q3c.krail.i18n.I18NKey
import uk.q3c.krail.i18n.LabelKey
import uk.q3c.krail.i18n.Translate

import static uk.q3c.krail.core.services.Service.State.*

/**
 * Created by David Sowerby on 08/11/15.
 *
 */
//@UseModules([])
class AbstractServiceTest extends Specification {

    def translate = Mock(Translate)
    GlobalBusProvider globalBusProvider = Mock(GlobalBusProvider)
    PubSubSupport<BusMessage> eventBus = Mock(PubSubSupport)

    TestService service;

    def servicesController = Mock(ServicesController)

    def setup() {
        globalBusProvider.getGlobalBus() >> eventBus
        service = new TestService(translate, servicesController, globalBusProvider)
        service.setThrowStartException(false)
        service.setThrowStopException(false)
    }

    def "name translated"() {
        given:
        translate.from(LabelKey.Authorisation) >> "Authorisation"
        expect:
        service.getNameKey().equals(LabelKey.Authorisation)
        service.getName().equals("Authorisation")
    }


    def "description key translated"() {
        given:
        translate.from(LabelKey.Authorisation) >> "Authorisation"
        service.setDescriptionKey(LabelKey.Authorisation)
        expect:
        service.getDescriptionKey().equals(LabelKey.Authorisation)
        service.getDescription().equals("Authorisation")
    }


    def "setDescriptionKey null is accepted"() {
        when:
        service.setDescriptionKey(null)

        then:

        service.getDescriptionKey().equals(null)
    }

    def "missing description key returns empty String"() {

        expect:
        service.getDescription().equals("")
    }

    def "start sets state to STARTING, then START if dependencies start ok"() {
        given:

        translate.from(LabelKey.Authorisation) >> "Authorisation"

        when:

        service.start()

        then:
        1 * eventBus.publish({ ServiceBusMessage m -> m.toState.equals(STARTING) })
        1 * servicesController.startDependenciesFor(service) >> true
        1 * eventBus.publish({ ServiceBusMessage m -> m.toState.equals(STARTED) })
        service.getState().equals(STARTED)

    }

    def "start sets state to STARTING, then FAILED_TO_START if dependencies do not start ok"() {
        given:

        translate.from(LabelKey.Authorisation) >> "Authorisation"

        when:

        service.start()

        then:
        1 * eventBus.publish({ ServiceBusMessage m -> m.toState.equals(STARTING) })
        1 * servicesController.startDependenciesFor(service) >> false
        1 * eventBus.publish({ ServiceBusMessage m -> m.toState.equals(DEPENDENCY_FAILED) })
        service.getState().equals(DEPENDENCY_FAILED)
    }

    def "doStart() throws an exception, state is FAILED_TO_START"() {
        given:


        service.throwStartException = true
        translate.from(LabelKey.Authorisation) >> "Authorisation"

        when:

        service.start()

        then:
        1 * eventBus.publish({ ServiceBusMessage m -> m.toState.equals(STARTING) })
        1 * servicesController.startDependenciesFor(service) >> true
        1 * eventBus.publish({ ServiceBusMessage m -> m.toState.equals(FAILED_TO_START) })
        service.getState().equals(FAILED_TO_START)
    }

    def "stop sets state to STOPPED"() {
        given:

        service.throwStopException = false
        translate.from(LabelKey.Authorisation) >> "Authorisation"

        when:

        def status = service.stop()

        then:

        1 * eventBus.publish({ ServiceBusMessage m -> m.toState.equals(STOPPING) })
        1 * servicesController.stopDependantsOf(service, false)
        1 * eventBus.publish({ ServiceBusMessage m -> m.toState.equals(STOPPED) })
        service.getState().equals(STOPPED)
        status.getServiceKey().equals(service.getServiceKey())
        status.getState().equals(STOPPED)
    }

    def "doStop() throws an exception, sets state to FAILED_TO_STOP"() {
        given:

        service.throwStopException = true
        translate.from(LabelKey.Authorisation) >> "Authorisation"

        when:

        def status = service.stop()

        then:

        1 * eventBus.publish({ ServiceBusMessage m -> m.toState.equals(STOPPING) })
        1 * servicesController.stopDependantsOf(service, false)
        1 * eventBus.publish({ ServiceBusMessage m -> m.toState.equals(FAILED_TO_STOP) })
        service.getState().equals(FAILED_TO_STOP)
        status.getServiceKey().equals(service.getServiceKey())
        status.getState().equals(FAILED_TO_STOP)
    }

    def "stop with dependency failed sets state to DEPENDENCY_FAILED"() {
        given:

        service.throwStopException = false
        translate.from(LabelKey.Authorisation) >> "Authorisation"

        when:

        def status = service.stop(DEPENDENCY_FAILED)

        then:

        1 * eventBus.publish({ ServiceBusMessage m -> m.toState.equals(STOPPING) })
        1 * servicesController.stopDependantsOf(service, false)
        1 * eventBus.publish({ ServiceBusMessage m -> m.toState.equals(DEPENDENCY_FAILED) })
        service.getState().equals(DEPENDENCY_FAILED)
        status.getServiceKey().equals(service.getServiceKey())
        status.getState().equals(DEPENDENCY_FAILED)
    }

    def "stop with dependency stopped sets state to DEPENDENCY_STOPPED"() {
        given:

        service.throwStopException = false
        translate.from(LabelKey.Authorisation) >> "Authorisation"

        when:

        def status = service.stop(DEPENDENCY_STOPPED)

        then:

        1 * eventBus.publish({ ServiceBusMessage m -> m.toState.equals(STOPPING) })
        1 * servicesController.stopDependantsOf(service, false)
        1 * eventBus.publish({ ServiceBusMessage m -> m.toState.equals(DEPENDENCY_STOPPED) })
        service.getState().equals(DEPENDENCY_STOPPED)
        status.getServiceKey().equals(service.getServiceKey())
        status.getState().equals(DEPENDENCY_STOPPED)
    }

    def "stop with FAILED sets state to FAILED"() {
        given:

        service.throwStopException = false
        translate.from(LabelKey.Authorisation) >> "Authorisation"

        when:

        def status = service.stop(FAILED)

        then:

        1 * eventBus.publish({ ServiceBusMessage m -> m.toState.equals(STOPPING) })
        1 * servicesController.stopDependantsOf(service, false)
        1 * eventBus.publish({ ServiceBusMessage m -> m.toState.equals(FAILED) })
        service.getState().equals(FAILED)
        status.getServiceKey().equals(service.getServiceKey())
        status.getState().equals(FAILED)
    }


    def "fail() is same as stop() with FAILED parameter"() {
        given:

        service.throwStopException = false
        translate.from(LabelKey.Authorisation) >> "Authorisation"

        when:

        def status = service.fail()

        then:

        1 * eventBus.publish({ ServiceBusMessage m -> m.toState.equals(STOPPING) })
        1 * servicesController.stopDependantsOf(service, false)
        1 * eventBus.publish({ ServiceBusMessage m -> m.toState.equals(FAILED) })
        service.getState().equals(FAILED)
        status.getServiceKey().equals(service.getServiceKey())
        status.getState().equals(FAILED)
    }

    def "fail during fail() is sets state to FAILED_TO_STOP"() {
        given:

        service.throwStopException = true
        translate.from(LabelKey.Authorisation) >> "Authorisation"

        when:

        def status = service.fail()

        then:

        1 * eventBus.publish({ ServiceBusMessage m -> m.toState.equals(STOPPING) })
        1 * servicesController.stopDependantsOf(service, false)
        1 * eventBus.publish({ ServiceBusMessage m -> m.toState.equals(FAILED_TO_STOP) })
        service.getState().equals(FAILED_TO_STOP)
        status.getServiceKey().equals(service.getServiceKey())
        status.getState().equals(FAILED_TO_STOP)
    }


    def "stop() with invalid parameter throws IllegalArgumentException"() {
        given:

        service.throwStopException = false
        translate.from(LabelKey.Authorisation) >> "Authorisation"

        when:

        def status = service.stop(STARTED)

        then:

        thrown IllegalArgumentException
    }

    def "start() when already started should exit"() {

        given:


        when:

        service.start()
        service.start()

        then:
        1 * servicesController.startDependenciesFor(service) >> true

    }

    def "stop() when already stopped should exit"() {

        given:


        when:

        service.stop()
        service.stop()

        then:
        1 * servicesController.stopDependantsOf(service, false)
        0 * servicesController.stopDependantsOf(service, true)

    }

    def "fail() when already stopped should exit"() {

        given:


        when:

        service.stop()
        service.fail()

        then:
        1 * servicesController.stopDependantsOf(service, false)
        0 * servicesController.stopDependantsOf(service, true)

    }


    def "get and setInstance()"() {

        given:


        when:

        service.setInstance(33)

        then:
        service.getInstance() == 33

    }


    static class TestService extends AbstractService implements Service {

        boolean throwStartException = false;
        boolean throwStopException = false;

        @Inject
        protected TestService(Translate translate, ServicesController servicesController, GlobalBusProvider globalBusProvider) {
            super(translate, servicesController, globalBusProvider);
        }

        @Override
        public void doStart() {
            if (throwStartException) {
                throw new RuntimeException("Test Exception thrown")
            }
        }

        @Override
        public void doStop() {
            if (throwStopException) {
                throw new RuntimeException("Test Exception thrown")
            }
        }

        @Override
        I18NKey getNameKey() {
            return LabelKey.Authorisation
        }
    }
}
