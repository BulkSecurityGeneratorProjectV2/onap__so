/*-
 * ============LICENSE_START=======================================================
 * ONAP - SO
 * ================================================================================
 * Copyright (C) 2017 - 2019 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.onap.so.apihandler.filters;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import java.io.IOException;
import javax.ws.rs.container.ContainerRequestContext;
import org.apache.http.HttpStatus;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.MockitoRule;
import org.onap.logging.ref.slf4j.ONAPLogConstants;
import org.onap.so.db.request.beans.InfraActiveRequests;
import org.onap.so.db.request.client.RequestsDbClient;
import org.slf4j.MDC;

@RunWith(MockitoJUnitRunner.class)
public class RequestIdFilterTest {

    @Mock
    ContainerRequestContext mockContext;

    @Mock
    protected RequestsDbClient requestsDbClient;

    @InjectMocks
    @Spy
    RequestIdFilter requestIdFilter;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Test
    public void filterTest() throws IOException {

        String requestId = "32807a28-1a14-4b88-b7b3-2950918aa769";
        MDC.put(ONAPLogConstants.MDCs.REQUEST_ID, requestId);

        // ExpectedRecord InfraActiveRequests
        InfraActiveRequests infraActiveRequests = new InfraActiveRequests();
        infraActiveRequests.setRequestStatus("FAILED");
        infraActiveRequests.setProgress(100L);
        infraActiveRequests.setLastModifiedBy("APIH");
        infraActiveRequests.setRequestScope("network");
        infraActiveRequests.setRequestAction("deleteInstance");
        infraActiveRequests.setRequestId("32807a28-1a14-4b88-b7b3-2950918aa769");

        doReturn(infraActiveRequests).when(requestsDbClient).getInfraActiveRequestbyRequestId(requestId);

        requestIdFilter.filter(mockContext);

        Mockito.verify(requestIdFilter, Mockito.times(1)).filter(mockContext);
        assertEquals(MDC.get(ONAPLogConstants.MDCs.RESPONSE_CODE), String.valueOf(HttpStatus.SC_BAD_REQUEST));

    }
}
