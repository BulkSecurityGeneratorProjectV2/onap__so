/*-
 * ============LICENSE_START=======================================================
 * ONAP - SO
 * ================================================================================
 * Copyright (C) 2017 - 2018 AT&T Intellectual Property. All rights reserved.
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

package org.onap.so.client.dmaapproperties;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.onap.so.BaseTest;
import org.springframework.beans.factory.annotation.Autowired;

public class GlobalDmaapPublisherTest extends BaseTest{

	@Autowired
	private GlobalDmaapPublisher globalDmaapPublisher;
	
	@Test
	public void testGetters() {
		assertEquals("dmaapUsername", globalDmaapPublisher.getUserName());
		assertEquals("dmaapPassword", globalDmaapPublisher.getPassword());
		assertEquals("com.att.mso.asyncStatusUpdate", globalDmaapPublisher.getTopic());
		assertEquals("http://localhost:" + wireMockPort, globalDmaapPublisher.getHost().get());
	}
}
