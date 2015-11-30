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
import uk.q3c.krail.core.navigate.sitemap.SitemapService

import static uk.q3c.krail.core.services.Service.State.FAILED_TO_START

@UnitTestFor(ServiceStatus)
class ServiceStatusTest extends Specification {



    def "construct"() {
        given:

        ServiceStatus status = new ServiceStatus(SitemapService.class, FAILED_TO_START)

        expect:

        status.getServiceClass() == SitemapService.class
        status.getState() == FAILED_TO_START
    }
}
