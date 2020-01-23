/*-
 * ============LICENSE_START=======================================================
 * ONAP - SO
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 * Modifications Copyright (C) 2020 Nokia
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

package org.onap.so.bpmn.servicedecomposition.tasks;

import static com.shazam.shazamcrest.MatcherAssert.assertThat;
import static com.shazam.shazamcrest.matcher.Matchers.sameBeanAs;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.onap.aai.domain.yang.CloudRegion;
import org.onap.aai.domain.yang.Configuration;
import org.onap.aai.domain.yang.Configurations;
import org.onap.aai.domain.yang.GenericVnf;
import org.onap.aai.domain.yang.GenericVnfs;
import org.onap.aai.domain.yang.L3Network;
import org.onap.aai.domain.yang.L3Networks;
import org.onap.aai.domain.yang.ServiceInstance;
import org.onap.aai.domain.yang.ServiceInstances;
import org.onap.aai.domain.yang.VolumeGroup;
import org.onap.aai.domain.yang.VolumeGroups;
import org.onap.aai.domain.yang.VpnBinding;
import org.onap.so.bpmn.common.InjectionHelper;
import org.onap.so.bpmn.servicedecomposition.bbobjects.Customer;
import org.onap.so.bpmn.servicedecomposition.bbobjects.ServiceSubscription;
import org.onap.so.bpmn.servicedecomposition.entities.ExecuteBuildingBlock;
import org.onap.so.bpmn.servicedecomposition.tasks.exceptions.MultipleObjectsFoundException;
import org.onap.so.bpmn.servicedecomposition.tasks.exceptions.NoServiceInstanceFoundException;
import org.onap.so.client.aai.AAIObjectPlurals;
import org.onap.so.client.aai.AAIObjectType;
import org.onap.so.client.aai.AAIResourcesClient;
import org.onap.so.client.aai.entities.uri.AAIResourceUri;
import org.onap.so.client.aai.entities.uri.AAIUriFactory;
import org.onap.so.client.graphinventory.entities.uri.Depth;
import org.onap.so.db.catalog.beans.VnfcInstanceGroupCustomization;
import org.onap.so.db.catalog.beans.CollectionResourceInstanceGroupCustomization;
import org.onap.so.db.catalog.beans.NetworkCollectionResourceCustomization;
import org.onap.so.db.catalog.beans.CvnfcConfigurationCustomization;
import org.onap.so.db.catalog.beans.VfModuleCustomization;
import org.onap.so.db.catalog.beans.CollectionNetworkResourceCustomization;
import org.onap.so.db.catalog.beans.Service;
import org.onap.so.db.catalog.client.CatalogDbClient;
import org.onap.so.db.request.beans.InfraActiveRequests;
import org.onap.so.db.request.beans.RequestProcessingData;
import org.onap.so.db.request.client.RequestsDbClient;
import org.onap.so.serviceinstancebeans.CloudConfiguration;
import org.onap.so.serviceinstancebeans.ModelInfo;
import org.onap.so.serviceinstancebeans.RequestDetails;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(MockitoJUnitRunner.class)
public class BBInputSetupUtilsTest {
    protected ObjectMapper mapper = new ObjectMapper();
    private static final String RESOURCE_PATH = "src/test/resources/__files/ExecuteBuildingBlock/";

    @InjectMocks
    BBInputSetupUtils bbInputSetupUtils = new BBInputSetupUtils();

    @Mock
    protected CatalogDbClient MOCK_catalogDbClient;

    @Mock
    protected RequestsDbClient MOCK_requestsDbClient;

    @Mock
    protected AAIResourcesClient MOCK_aaiResourcesClient;

    @Mock
    protected InjectionHelper MOCK_injectionHelper;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setup() {
        doReturn(MOCK_aaiResourcesClient).when(MOCK_injectionHelper).getAaiClient();
    }

    @Test
    public void testGetCatalogServiceByModelUUID() throws JsonParseException, JsonMappingException, IOException {
        Service expected = mapper.readValue(new File(RESOURCE_PATH + "CatalogServiceExpected.json"), Service.class);

        RequestDetails requestDetails = new RequestDetails();
        ModelInfo modelInfo = new ModelInfo();
        modelInfo.setModelVersionId("modelUUID");
        requestDetails.setModelInfo(modelInfo);
        doReturn(expected).when(MOCK_catalogDbClient).getServiceByID("modelUUID");
        Service actual = bbInputSetupUtils.getCatalogServiceByModelUUID(modelInfo.getModelVersionId());

        assertThat(actual, sameBeanAs(expected));
    }

    @Test
    public void testGetCatalogServiceByModelVersionAndModelInvariantUUID()
            throws JsonParseException, JsonMappingException, IOException {
        String modelVersion = "modelVersion";
        String modelInvariantUUID = "modelInvariantUUID";
        Service expectedService =
                mapper.readValue(new File(RESOURCE_PATH + "CatalogServiceExpected.json"), Service.class);

        doReturn(expectedService).when(MOCK_catalogDbClient)
                .getServiceByModelVersionAndModelInvariantUUID(isA(String.class), isA(String.class));

        Service actualService = bbInputSetupUtils.getCatalogServiceByModelVersionAndModelInvariantUUID(modelVersion,
                modelInvariantUUID);

        assertThat(actualService, sameBeanAs(expectedService));
    }

    @Test
    public void testGetVnfcInstanceGroups() throws JsonParseException, JsonMappingException, IOException {
        VnfcInstanceGroupCustomization vnfc = mapper.readValue(
                new File(RESOURCE_PATH + "VnfcInstanceGroupCustomization.json"), VnfcInstanceGroupCustomization.class);
        String modelCustomizationUUID = "modelCustomizationUUID";

        doReturn(Arrays.asList(vnfc)).when(MOCK_catalogDbClient)
                .getVnfcInstanceGroupsByVnfResourceCust(isA(String.class));

        List<VnfcInstanceGroupCustomization> actualVnfcList =
                bbInputSetupUtils.getVnfcInstanceGroups(modelCustomizationUUID);

        assertThat(actualVnfcList, sameBeanAs(Arrays.asList(vnfc)));
    }

    @Test
    public void testGetRequestDetails() throws JsonParseException, JsonMappingException, IOException {
        InfraActiveRequests infraActiveRequest = mapper
                .readValue(new File(RESOURCE_PATH + "InfraActiveRequestExpected.json"), InfraActiveRequests.class);

        RequestDetails expected =
                mapper.readValue(new File(RESOURCE_PATH + "RequestDetailsExpected.json"), RequestDetails.class);
        String requestId = "requestId";
        doReturn(infraActiveRequest).when(MOCK_requestsDbClient).getInfraActiveRequestbyRequestId(requestId);
        RequestDetails actual = bbInputSetupUtils.getRequestDetails(requestId);

        assertThat(actual, sameBeanAs(expected));
    }

    @Test
    public void getRequestDetailsNullTest() throws IOException {
        RequestDetails requestDetails = bbInputSetupUtils.getRequestDetails("");

        assertNull(requestDetails);
    }

    @Test
    public void testGetCloudRegion() {
        CloudConfiguration cloudConfig = new CloudConfiguration();
        cloudConfig.setLcpCloudRegionId("lcpCloudRegionId");
        Optional<org.onap.aai.domain.yang.CloudRegion> expected =
                Optional.of(new org.onap.aai.domain.yang.CloudRegion());
        expected.get().setCloudOwner("cloudOwner");
        expected.get().setCloudRegionId("lcpCloudRegionId");
        doReturn(expected).when(MOCK_aaiResourcesClient).get(org.onap.aai.domain.yang.CloudRegion.class,
                AAIUriFactory.createResourceUri(AAIObjectType.CLOUD_REGION, cloudConfig.getCloudOwner(),
                        cloudConfig.getLcpCloudRegionId()).depth(Depth.TWO));

        AAIResourceUri expectedUri = AAIUriFactory.createResourceUri(AAIObjectType.CLOUD_REGION,
                cloudConfig.getCloudOwner(), cloudConfig.getLcpCloudRegionId()).depth(Depth.TWO);
        bbInputSetupUtils.getCloudRegion(cloudConfig);

        verify(MOCK_aaiResourcesClient, times(1)).get(CloudRegion.class, expectedUri);
    }

    @Test
    public void testGetCloudRegionExceptionTest() {

        CloudConfiguration cloudConfig = new CloudConfiguration();
        cloudConfig.setLcpCloudRegionId("lcpCloudRegionId");

        RequestDetails requestDetails = new RequestDetails();
        requestDetails.setCloudConfiguration(cloudConfig);

        doReturn(Optional.empty()).when(MOCK_aaiResourcesClient).get(isA(Class.class), isA(AAIResourceUri.class));

        CloudRegion cloudRegion = bbInputSetupUtils.getCloudRegion(cloudConfig);

        assertNull(cloudRegion);
    }

    @Test
    public void testGetCloudRegionEmptyId() {
        CloudConfiguration cloudConfig = new CloudConfiguration();
        cloudConfig.setLcpCloudRegionId("");

        RequestDetails requestDetails = new RequestDetails();
        requestDetails.setCloudConfiguration(cloudConfig);

        CloudRegion cloudRegion = bbInputSetupUtils.getCloudRegion(cloudConfig);

        assertNull(cloudRegion);
    }

    @Test
    public void testGetCloudRegionEmptyConfiguration() {
        RequestDetails requestDetails = new RequestDetails();

        CloudRegion cloudRegion = bbInputSetupUtils.getCloudRegion(requestDetails.getCloudConfiguration());

        assertNull(cloudRegion);
    }

    @Test
    public void testGetAAIInstanceGroup() {
        Optional<org.onap.aai.domain.yang.InstanceGroup> expected =
                Optional.of(new org.onap.aai.domain.yang.InstanceGroup());
        String instanceGroupId = "instanceGroupId";
        expected.get().setId(instanceGroupId);
        doReturn(expected).when(MOCK_aaiResourcesClient).get(org.onap.aai.domain.yang.InstanceGroup.class,
                AAIUriFactory.createResourceUri(AAIObjectType.INSTANCE_GROUP, instanceGroupId));
        AAIResourceUri expectedUri = AAIUriFactory.createResourceUri(AAIObjectType.INSTANCE_GROUP, instanceGroupId);

        bbInputSetupUtils.getAAIInstanceGroup(instanceGroupId);
        verify(MOCK_aaiResourcesClient, times(1)).get(org.onap.aai.domain.yang.InstanceGroup.class, expectedUri);
    }

    @Test
    public void testGetAAIInstanceGroupThrowNotFound() {
        String instanceGroupId = "instanceGroupId";
        doReturn(MOCK_aaiResourcesClient).when(MOCK_injectionHelper).getAaiClient();
        doReturn(Optional.empty()).when(MOCK_aaiResourcesClient).get(org.onap.aai.domain.yang.InstanceGroup.class,
                AAIUriFactory.createResourceUri(AAIObjectType.INSTANCE_GROUP, instanceGroupId));

        org.onap.aai.domain.yang.InstanceGroup actual = bbInputSetupUtils.getAAIInstanceGroup(instanceGroupId);

        assertNull(actual);
    }

    @Test
    public void testGetAAICustomer() {
        Optional<org.onap.aai.domain.yang.Customer> expected = Optional.of(new org.onap.aai.domain.yang.Customer());
        String globalSubscriberId = "globalSubscriberId";
        expected.get().setGlobalCustomerId(globalSubscriberId);
        doReturn(expected).when(MOCK_aaiResourcesClient).get(org.onap.aai.domain.yang.Customer.class,
                AAIUriFactory.createResourceUri(AAIObjectType.CUSTOMER, globalSubscriberId));
        AAIResourceUri expectedUri = AAIUriFactory.createResourceUri(AAIObjectType.CUSTOMER, globalSubscriberId);

        bbInputSetupUtils.getAAICustomer(globalSubscriberId);
        verify(MOCK_aaiResourcesClient, times(1)).get(org.onap.aai.domain.yang.Customer.class, expectedUri);
    }

    @Test
    public void testGetAAICustomerThrowNotFound() {
        String globalSubscriberId = "globalSubscriberId";
        doReturn(MOCK_aaiResourcesClient).when(MOCK_injectionHelper).getAaiClient();
        doReturn(Optional.empty()).when(MOCK_aaiResourcesClient).get(org.onap.aai.domain.yang.Customer.class,
                AAIUriFactory.createResourceUri(AAIObjectType.CUSTOMER, globalSubscriberId));

        org.onap.aai.domain.yang.Customer actual = bbInputSetupUtils.getAAICustomer(globalSubscriberId);

        assertNull(actual);
    }

    @Test
    public void testGetAAIServiceSubscription() {
        Optional<org.onap.aai.domain.yang.ServiceSubscription> expected =
                Optional.of(new org.onap.aai.domain.yang.ServiceSubscription());
        String globalSubscriberId = "globalSubscriberId";
        String subscriptionServiceType = "subscriptionServiceType";
        expected.get().setServiceType(subscriptionServiceType);
        doReturn(expected).when(MOCK_aaiResourcesClient).get(org.onap.aai.domain.yang.ServiceSubscription.class,
                AAIUriFactory.createResourceUri(AAIObjectType.SERVICE_SUBSCRIPTION, globalSubscriberId,
                        subscriptionServiceType));
        AAIResourceUri expectedUri = AAIUriFactory.createResourceUri(AAIObjectType.SERVICE_SUBSCRIPTION,
                globalSubscriberId, subscriptionServiceType);

        bbInputSetupUtils.getAAIServiceSubscription(globalSubscriberId, subscriptionServiceType);
        verify(MOCK_aaiResourcesClient, times(1)).get(org.onap.aai.domain.yang.ServiceSubscription.class, expectedUri);
    }

    @Test
    public void testGetAAIServiceSubscriptionErrors() {
        String globalSubId = null;
        String subServiceType = null;
        org.onap.aai.domain.yang.ServiceSubscription actual =
                bbInputSetupUtils.getAAIServiceSubscription(globalSubId, subServiceType);
        assertNull(actual);

        String globalSubId2 = "";
        String subServiceType2 = "";
        org.onap.aai.domain.yang.ServiceSubscription actual2 =
                bbInputSetupUtils.getAAIServiceSubscription(globalSubId2, subServiceType2);
        assertNull(actual2);

        String globalSubId3 = "";
        String subServiceType3 = null;
        org.onap.aai.domain.yang.ServiceSubscription actual3 =
                bbInputSetupUtils.getAAIServiceSubscription(globalSubId3, subServiceType3);
        assertNull(actual3);

        String globalSubId4 = null;
        String subServiceType4 = "";
        org.onap.aai.domain.yang.ServiceSubscription actual4 =
                bbInputSetupUtils.getAAIServiceSubscription(globalSubId4, subServiceType4);
        assertNull(actual4);
    }

    @Test
    public void testGetAAIServiceSubscriptionThrowNotFound() {
        String globalSubscriberId = "globalSubscriberId";
        String subscriptionServiceType = "subscriptionServiceType";
        doReturn(MOCK_aaiResourcesClient).when(MOCK_injectionHelper).getAaiClient();
        doReturn(Optional.empty()).when(MOCK_aaiResourcesClient).get(org.onap.aai.domain.yang.ServiceSubscription.class,
                AAIUriFactory.createResourceUri(AAIObjectType.SERVICE_SUBSCRIPTION, globalSubscriberId,
                        subscriptionServiceType));
        org.onap.aai.domain.yang.ServiceSubscription actual =
                bbInputSetupUtils.getAAIServiceSubscription(globalSubscriberId, subscriptionServiceType);
        assertNull(actual);
    }

    @Test
    public void testGetAAIServiceInstanceById() {
        String serviceInstanceId = "serviceInstanceId";

        ServiceInstance expectedServiceInstance = new ServiceInstance();

        doReturn(Optional.of(expectedServiceInstance)).when(MOCK_aaiResourcesClient).get(isA(Class.class),
                isA(AAIResourceUri.class));

        ServiceInstance actualServiceInstance = bbInputSetupUtils.getAAIServiceInstanceById(serviceInstanceId);

        assertThat(actualServiceInstance, sameBeanAs(expectedServiceInstance));
    }

    @Test
    public void testGetAAIServiceInstanceByIdThrowNotFound() {
        String serviceInstanceId = "serviceInstanceId";

        doReturn(Optional.empty()).when(MOCK_aaiResourcesClient).get(isA(Class.class), isA(AAIResourceUri.class));

        ServiceInstance actualServiceInstance = bbInputSetupUtils.getAAIServiceInstanceById(serviceInstanceId);

        assertNull(actualServiceInstance);
    }

    @Test
    public void testGetAAIServiceInstanceByIdAndCustomer() {
        String globalCustomerId = "globalCustomerId";
        String serviceType = "serviceType";
        String serviceInstanceId = "serviceInstanceId";
        ServiceInstance expected = new ServiceInstance();
        expected.setServiceInstanceId(serviceInstanceId);
        doReturn(Optional.of(expected)).when(MOCK_aaiResourcesClient).get(isA(Class.class), isA(AAIResourceUri.class));
        AAIResourceUri expectedUri = AAIUriFactory
                .createResourceUri(AAIObjectType.SERVICE_INSTANCE, globalCustomerId, serviceType, serviceInstanceId)
                .depth(Depth.TWO);
        this.bbInputSetupUtils.getAAIServiceInstanceByIdAndCustomer(globalCustomerId, serviceType, serviceInstanceId);

        verify(MOCK_aaiResourcesClient, times(1)).get(org.onap.aai.domain.yang.ServiceInstance.class, expectedUri);

    }

    @Test
    public void testGetAAIServiceInstanceByIdAndCustomerThrowNotFound() {
        String globalCustomerId = "globalCustomerId";
        String serviceType = "serviceType";
        String serviceInstanceId = "serviceInstanceId";

        doReturn(MOCK_aaiResourcesClient).when(MOCK_injectionHelper).getAaiClient();
        doReturn(Optional.empty()).when(MOCK_aaiResourcesClient).get(isA(Class.class), isA(AAIResourceUri.class));
        ServiceInstance actual = this.bbInputSetupUtils.getAAIServiceInstanceByIdAndCustomer(globalCustomerId,
                serviceType, serviceInstanceId);

        assertNull(actual);
    }

    @Test
    public void testGetAAIServiceInstanceByName() throws Exception {
        String serviceInstanceName = "serviceInstanceName";

        ServiceInstance expectedServiceInstance = new ServiceInstance();
        expectedServiceInstance.setServiceInstanceId("serviceInstanceId");

        ServiceSubscription serviceSubscription = new ServiceSubscription();
        serviceSubscription.setServiceType("serviceType");

        Customer customer = new Customer();
        customer.setGlobalCustomerId("globalCustomerId");
        customer.setServiceSubscription(serviceSubscription);

        ServiceInstances serviceInstances = new ServiceInstances();
        serviceInstances.getServiceInstance().add(expectedServiceInstance);

        doReturn(Optional.of(serviceInstances)).when(MOCK_aaiResourcesClient).get(isA(Class.class),
                isA(AAIResourceUri.class));
        AAIResourceUri expectedUri = AAIUriFactory
                .createResourceUri(AAIObjectPlurals.SERVICE_INSTANCE, customer.getGlobalCustomerId(),
                        customer.getServiceSubscription().getServiceType())
                .queryParam("service-instance-name", serviceInstanceName).depth(Depth.TWO);
        bbInputSetupUtils.getAAIServiceInstanceByName(serviceInstanceName, customer);

        verify(MOCK_aaiResourcesClient, times(1)).get(org.onap.aai.domain.yang.ServiceInstances.class, expectedUri);
    }

    @Test
    public void testGetAAIServiceInstanceByNameException() throws Exception {
        expectedException.expect(Exception.class);

        String serviceInstanceName = "serviceInstanceName";

        ServiceInstance serviceInstance = new ServiceInstance();
        serviceInstance.setServiceInstanceId("serviceInstanceId");

        ServiceSubscription serviceSubscription = new ServiceSubscription();
        serviceSubscription.setServiceType("serviceType");

        Customer customer = new Customer();
        customer.setGlobalCustomerId("globalCustomerId");
        customer.setServiceSubscription(serviceSubscription);

        ServiceInstances serviceInstances = new ServiceInstances();
        serviceInstances.getServiceInstance().add(serviceInstance);
        serviceInstances.getServiceInstance().add(serviceInstance);

        doReturn(Optional.of(serviceInstances)).when(MOCK_aaiResourcesClient).get(isA(Class.class),
                isA(AAIResourceUri.class));

        bbInputSetupUtils.getAAIServiceInstanceByName(serviceInstanceName, customer);
    }

    @Test
    public void testGetAAIServiceInstanceByNameNull() throws Exception {
        String serviceInstanceName = "serviceInstanceName";

        ServiceInstance serviceInstance = new ServiceInstance();
        serviceInstance.setServiceInstanceId("serviceInstanceId");

        ServiceSubscription serviceSubscription = new ServiceSubscription();
        serviceSubscription.setServiceType("serviceType");

        Customer customer = new Customer();
        customer.setGlobalCustomerId("globalCustomerId");
        customer.setServiceSubscription(serviceSubscription);

        ServiceInstances serviceInstances = new ServiceInstances();
        serviceInstances.getServiceInstance().add(serviceInstance);
        serviceInstances.getServiceInstance().add(serviceInstance);

        doReturn(Optional.empty()).when(MOCK_aaiResourcesClient).get(isA(Class.class), isA(AAIResourceUri.class));

        ServiceInstance actualServiceInstance =
                bbInputSetupUtils.getAAIServiceInstanceByName(serviceInstanceName, customer);

        assertNull(actualServiceInstance);
    }

    @Test
    public void testGetOptionalAAIServiceInstanceByNameException() throws Exception {
        expectedException.expect(MultipleObjectsFoundException.class);
        expectedException.expectMessage(containsString(
                "Multiple service instances found for customer-id: globalCustomerId, service-type: serviceType and service-instance-name: serviceInstanceId."));

        String globalCustomerId = "globalCustomerId";
        String serviceType = "serviceType";
        String serviceInstanceId = "serviceInstanceId";

        ServiceInstance serviceInstance = new ServiceInstance();
        serviceInstance.setServiceInstanceId("serviceInstanceId");
        serviceInstance.setServiceType(serviceType);
        serviceInstance.setServiceInstanceName("serviceInstanceName");

        ServiceInstances serviceInstances = new ServiceInstances();
        serviceInstances.getServiceInstance().add(serviceInstance);
        serviceInstances.getServiceInstance().add(serviceInstance);

        doReturn(Optional.of(serviceInstances)).when(MOCK_aaiResourcesClient).get(isA(Class.class),
                isA(AAIResourceUri.class));

        bbInputSetupUtils.getAAIServiceInstanceByName(globalCustomerId, serviceType, serviceInstanceId);
    }

    @Test
    public void testGetOptionalAAIServiceInstanceByNameNull() throws Exception {
        String globalCustomerId = "globalCustomerId";
        String serviceType = "serviceType";
        String serviceInstanceId = "serviceInstanceId";

        doReturn(Optional.empty()).when(MOCK_aaiResourcesClient).get(isA(Class.class), isA(AAIResourceUri.class));
        Optional<ServiceInstance> actual =
                this.bbInputSetupUtils.getAAIServiceInstanceByName(globalCustomerId, serviceType, serviceInstanceId);

        assertThat(actual, sameBeanAs(Optional.empty()));
    }

    @Test
    public void testGetCatalogInstanceGroup() throws JsonParseException, JsonMappingException, IOException {
        String modelUUID = "modelUUID";

        org.onap.so.db.catalog.beans.InstanceGroup expectedInstanceGroup = mapper.readValue(
                new File(RESOURCE_PATH + "InstanceGroup.json"), org.onap.so.db.catalog.beans.InstanceGroup.class);

        doReturn(expectedInstanceGroup).when(MOCK_catalogDbClient).getInstanceGroupByModelUUID(isA(String.class));

        org.onap.so.db.catalog.beans.InstanceGroup actualInstanceGroup =
                bbInputSetupUtils.getCatalogInstanceGroup(modelUUID);

        assertThat(actualInstanceGroup, sameBeanAs(expectedInstanceGroup));
    }

    @Test
    public void testGetCollectionResourceInstanceGroupCustomization() {
        String modelCustomizationUUID = "modelCustomizationUUID";

        CollectionResourceInstanceGroupCustomization expectedCollection =
                new CollectionResourceInstanceGroupCustomization();

        doReturn(Arrays.asList(expectedCollection)).when(MOCK_catalogDbClient)
                .getCollectionResourceInstanceGroupCustomizationByModelCustUUID(modelCustomizationUUID);

        List<CollectionResourceInstanceGroupCustomization> actualCollection =
                bbInputSetupUtils.getCollectionResourceInstanceGroupCustomization(modelCustomizationUUID);

        assertThat(actualCollection, sameBeanAs(Arrays.asList(expectedCollection)));
    }

    @Test
    public void testGetAAIGenericVnf() throws JsonParseException, JsonMappingException, IOException {
        String vnfId = "vnfId";

        GenericVnf expectedAaiVnf =
                mapper.readValue(new File(RESOURCE_PATH + "aaiGenericVnfInput.json"), GenericVnf.class);

        doReturn(Optional.of(expectedAaiVnf)).when(MOCK_aaiResourcesClient).get(isA(Class.class),
                isA(AAIResourceUri.class));
        AAIResourceUri expectedUri = AAIUriFactory.createResourceUri(AAIObjectType.GENERIC_VNF, vnfId).depth(Depth.ONE);
        GenericVnf actualAaiVnf = bbInputSetupUtils.getAAIGenericVnf(vnfId);

        assertThat(actualAaiVnf, sameBeanAs(expectedAaiVnf));

        verify(MOCK_aaiResourcesClient, times(1)).get(org.onap.aai.domain.yang.GenericVnf.class, expectedUri);
    }

    @Test
    public void testGetAAIConfiguration() throws JsonParseException, JsonMappingException, IOException {
        String configurationId = "configurationId";

        Configuration expectedAaiConfiguration =
                mapper.readValue(new File(RESOURCE_PATH + "ConfigurationInput.json"), Configuration.class);

        doReturn(Optional.of(expectedAaiConfiguration)).when(MOCK_aaiResourcesClient).get(isA(Class.class),
                isA(AAIResourceUri.class));
        AAIResourceUri expectedUri =
                AAIUriFactory.createResourceUri(AAIObjectType.CONFIGURATION, configurationId).depth(Depth.ONE);
        bbInputSetupUtils.getAAIConfiguration(configurationId);

        verify(MOCK_aaiResourcesClient, times(1)).get(org.onap.aai.domain.yang.Configuration.class, expectedUri);
    }

    @Test
    public void testGetAAIGenericVnfThrowNotFound() throws JsonParseException, JsonMappingException, IOException {
        String vnfId = "vnfId";

        doReturn(Optional.empty()).when(MOCK_aaiResourcesClient).get(isA(Class.class), isA(AAIResourceUri.class));

        GenericVnf actualAaiVnf = bbInputSetupUtils.getAAIGenericVnf(vnfId);

        assertNull(actualAaiVnf);
    }

    @Test
    public void testGetAAIResourceDepthOne() {
        String vnfId = "vnfId";
        AAIResourceUri aaiResourceUri = AAIUriFactory.createResourceUri(AAIObjectType.GENERIC_VNF, vnfId);
        AAIResourceUri expectedUri = aaiResourceUri.clone().depth(Depth.ONE);
        AAIResourceUri aaiResourceUriClone = aaiResourceUri.clone();
        bbInputSetupUtils.getAAIResourceDepthOne(aaiResourceUri);

        verify(MOCK_aaiResourcesClient, times(1)).get(expectedUri);
        assertEquals("Uri should not have changed", aaiResourceUriClone.build(), aaiResourceUri.build());
    }

    @Test
    public void testGetAAIResourceDepthTwo() {
        String vnfId = "vnfId";
        AAIResourceUri aaiResourceUri = AAIUriFactory.createResourceUri(AAIObjectType.GENERIC_VNF, vnfId);
        AAIResourceUri expectedUri = aaiResourceUri.clone().depth(Depth.TWO);
        AAIResourceUri aaiResourceUriClone = aaiResourceUri.clone();
        bbInputSetupUtils.getAAIResourceDepthTwo(aaiResourceUri);

        verify(MOCK_aaiResourcesClient, times(1)).get(expectedUri);
        assertEquals("Uri should not have changed", aaiResourceUriClone.build(), aaiResourceUri.build());
    }

    @Test
    public void getRelatedNetworkByNameFromServiceInstanceTest() throws Exception {
        Optional<L3Networks> expected = Optional.of(new L3Networks());
        L3Network network = new L3Network();
        network.setNetworkId("id123");
        network.setNetworkName("name123");
        expected.get().getL3Network().add(network);
        doReturn(expected).when(MOCK_aaiResourcesClient).get(eq(L3Networks.class), any(AAIResourceUri.class));
        Optional<L3Network> actual =
                this.bbInputSetupUtils.getRelatedNetworkByNameFromServiceInstance("id123", "name123");
        assertEquals(actual.get().getNetworkId(), expected.get().getL3Network().get(0).getNetworkId());
    }

    @Test
    public void getRelatedNetworkByNameFromServiceInstanceMultipleNetworksExceptionTest() throws Exception {
        expectedException.expect(MultipleObjectsFoundException.class);
        expectedException.expectMessage(containsString(
                "Multiple networks found for service-instance-id: serviceInstanceId and network-name: networkName."));

        String serviceInstanceId = "serviceInstanceId";
        String networkName = "networkName";

        L3Network network = new L3Network();
        network.setNetworkId("id123");
        network.setNetworkName("name123");

        L3Networks l3Networks = new L3Networks();
        l3Networks.getL3Network().add(network);
        l3Networks.getL3Network().add(network);
        Optional<L3Networks> optNetworks = Optional.of(l3Networks);

        doReturn(optNetworks).when(MOCK_aaiResourcesClient).get(eq(L3Networks.class), any(AAIResourceUri.class));

        bbInputSetupUtils.getRelatedNetworkByNameFromServiceInstance(serviceInstanceId, networkName);
    }

    @Test
    public void getRelatedNetworkByNameFromServiceInstanceNotFoundTest() throws Exception {
        String serviceInstanceId = "serviceInstanceId";
        String networkName = "networkName";

        doReturn(Optional.empty()).when(MOCK_aaiResourcesClient).get(eq(L3Networks.class), any(AAIResourceUri.class));

        Optional<L3Network> actualNetwork =
                bbInputSetupUtils.getRelatedNetworkByNameFromServiceInstance(serviceInstanceId, networkName);

        assertEquals(Optional.empty(), actualNetwork);
    }

    @Test
    public void getRelatedServiceInstanceFromInstanceGroupTest() throws Exception {
        Optional<ServiceInstances> expected = Optional.of(new ServiceInstances());
        ServiceInstance serviceInstance = new ServiceInstance();
        serviceInstance.setServiceInstanceId("serviceInstanceId");
        serviceInstance.setServiceInstanceName("serviceInstanceName");
        expected.get().getServiceInstance().add(serviceInstance);

        doReturn(expected).when(MOCK_aaiResourcesClient).get(eq(ServiceInstances.class), any(AAIResourceUri.class));
        Optional<ServiceInstance> actual = this.bbInputSetupUtils.getRelatedServiceInstanceFromInstanceGroup("ig-001");
        assertEquals(actual.get().getServiceInstanceId(),
                expected.get().getServiceInstance().get(0).getServiceInstanceId());
    }

    @Test
    public void getRelatedServiceInstanceFromInstanceGroupMultipleTest() throws Exception {
        expectedException.expect(MultipleObjectsFoundException.class);
        Optional<ServiceInstances> serviceInstances = Optional.of(new ServiceInstances());
        ServiceInstance si1 = Mockito.mock(ServiceInstance.class);
        ServiceInstance si2 = Mockito.mock(ServiceInstance.class);
        serviceInstances.get().getServiceInstance().add(si1);
        serviceInstances.get().getServiceInstance().add(si2);

        doReturn(serviceInstances).when(MOCK_aaiResourcesClient).get(eq(ServiceInstances.class),
                any(AAIResourceUri.class));
        this.bbInputSetupUtils.getRelatedServiceInstanceFromInstanceGroup("ig-001");
    }

    @Test
    public void getRelatedServiceInstanceFromInstanceGroupNotFoundTest() throws Exception {
        expectedException.expect(NoServiceInstanceFoundException.class);
        Optional<ServiceInstances> serviceInstances = Optional.of(new ServiceInstances());

        doReturn(serviceInstances).when(MOCK_aaiResourcesClient).get(eq(ServiceInstances.class),
                any(AAIResourceUri.class));
        this.bbInputSetupUtils.getRelatedServiceInstanceFromInstanceGroup("ig-001");
    }

    @Test
    public void getRelatedVnfByNameFromServiceInstanceTest() throws Exception {
        Optional<GenericVnfs> expected = Optional.of(new GenericVnfs());
        GenericVnf vnf = new GenericVnf();
        vnf.setVnfId("id123");
        vnf.setVnfName("name123");
        expected.get().getGenericVnf().add(vnf);
        doReturn(expected).when(MOCK_aaiResourcesClient).get(eq(GenericVnfs.class), any(AAIResourceUri.class));
        Optional<GenericVnf> actual = this.bbInputSetupUtils.getRelatedVnfByNameFromServiceInstance("id123", "name123");
        assertEquals(actual.get().getVnfId(), expected.get().getGenericVnf().get(0).getVnfId());
    }

    @Test
    public void getRelatedVnfByNameFromServiceInstanceMultipleVnfsExceptionTest() throws Exception {
        expectedException.expect(MultipleObjectsFoundException.class);
        expectedException.expectMessage(containsString(
                "Multiple vnfs found for service-instance-id: serviceInstanceId and vnf-name: vnfName."));

        String serviceInstanceId = "serviceInstanceId";
        String vnfName = "vnfName";

        GenericVnf vnf = new GenericVnf();
        vnf.setVnfId("id123");
        vnf.setVnfName("name123");

        GenericVnfs vnfs = new GenericVnfs();
        vnfs.getGenericVnf().add(vnf);
        vnfs.getGenericVnf().add(vnf);

        Optional<GenericVnfs> optVnfs = Optional.of(vnfs);

        doReturn(optVnfs).when(MOCK_aaiResourcesClient).get(eq(GenericVnfs.class), any(AAIResourceUri.class));

        bbInputSetupUtils.getRelatedVnfByNameFromServiceInstance(serviceInstanceId, vnfName);
    }

    @Test
    public void getRelatedVnfByNameFromServiceInstanceNotFoundTest() throws Exception {
        String serviceInstanceId = "serviceInstanceId";
        String vnfName = "vnfName";

        doReturn(Optional.empty()).when(MOCK_aaiResourcesClient).get(eq(GenericVnfs.class), any(AAIResourceUri.class));

        Optional<GenericVnf> actualVnf =
                this.bbInputSetupUtils.getRelatedVnfByNameFromServiceInstance(serviceInstanceId, vnfName);

        assertEquals(actualVnf, Optional.empty());
    }

    @Test
    public void getRelatedVolumeGroupByNameFromVnfTest() throws Exception {
        Optional<VolumeGroups> expected = Optional.of(new VolumeGroups());
        VolumeGroup volumeGroup = new VolumeGroup();
        volumeGroup.setVolumeGroupId("id123");
        volumeGroup.setVolumeGroupName("name123");
        expected.get().getVolumeGroup().add(volumeGroup);
        doReturn(expected).when(MOCK_aaiResourcesClient).get(eq(VolumeGroups.class), any(AAIResourceUri.class));
        Optional<VolumeGroup> actual = this.bbInputSetupUtils.getRelatedVolumeGroupByNameFromVnf("id123", "name123");
        assertEquals(actual.get().getVolumeGroupId(), expected.get().getVolumeGroup().get(0).getVolumeGroupId());
    }

    @Test
    public void getRelatedVolumeGroupByNameFromVnfMultipleVolumeGroupsExceptionTest() throws Exception {
        expectedException.expect(MultipleObjectsFoundException.class);
        expectedException.expectMessage(containsString(
                "Multiple volume-groups found for vnf-id: vnfId and volume-group-name: volumeGroupName."));


        String vnfId = "vnfId";
        String volumeGroupName = "volumeGroupName";

        VolumeGroup volumeGroup = new VolumeGroup();
        volumeGroup.setVolumeGroupId("id123");
        volumeGroup.setVolumeGroupName("name123");

        VolumeGroups volumeGroups = new VolumeGroups();
        volumeGroups.getVolumeGroup().add(volumeGroup);
        volumeGroups.getVolumeGroup().add(volumeGroup);
        Optional<VolumeGroups> optVolumeGroups = Optional.of(volumeGroups);

        doReturn(optVolumeGroups).when(MOCK_aaiResourcesClient).get(eq(VolumeGroups.class), any(AAIResourceUri.class));

        bbInputSetupUtils.getRelatedVolumeGroupByNameFromVnf(vnfId, volumeGroupName);
    }

    @Test
    public void getRelatedVolumeGroupByNameFromVnfNotFoundTest() throws Exception {
        String vnfId = "vnfId";
        String volumeGroupName = "volumeGroupName";

        doReturn(Optional.empty()).when(MOCK_aaiResourcesClient).get(eq(VolumeGroups.class), any(AAIResourceUri.class));

        Optional<VolumeGroup> actualVolumeGroup =
                bbInputSetupUtils.getRelatedVolumeGroupByNameFromVnf(vnfId, volumeGroupName);

        assertEquals(actualVolumeGroup, Optional.empty());
    }

    @Test
    public void getRelatedVolumeGroupByNameFromVfModuleTest() throws Exception {
        Optional<VolumeGroups> expected = Optional.of(new VolumeGroups());
        VolumeGroup volumeGroup = new VolumeGroup();
        volumeGroup.setVolumeGroupId("id123");
        volumeGroup.setVolumeGroupName("name123");
        expected.get().getVolumeGroup().add(volumeGroup);
        doReturn(expected).when(MOCK_aaiResourcesClient).get(eq(VolumeGroups.class), any(AAIResourceUri.class));
        Optional<VolumeGroup> actual =
                this.bbInputSetupUtils.getRelatedVolumeGroupByNameFromVfModule("id123", "id123", "name123");
        assertEquals(actual.get().getVolumeGroupId(), expected.get().getVolumeGroup().get(0).getVolumeGroupId());
    }

    @Test
    public void getRelatedVolumeGroupFromVfModuleMultipleVolumeGroupsExceptionTest() throws Exception {
        expectedException.expect(Exception.class);

        String vnfId = "vnfId";
        String volumeGroupId = "volumeGroupId";

        VolumeGroup volumeGroup = new VolumeGroup();
        volumeGroup.setVolumeGroupId("id123");
        volumeGroup.setVolumeGroupName("name123");

        VolumeGroups expectedVolumeGroup = new VolumeGroups();
        expectedVolumeGroup.getVolumeGroup().add(volumeGroup);
        expectedVolumeGroup.getVolumeGroup().add(volumeGroup);

        doReturn(expectedVolumeGroup).when(MOCK_aaiResourcesClient).get(eq(VolumeGroups.class),
                any(AAIResourceUri.class));

        bbInputSetupUtils.getRelatedVolumeGroupFromVfModule(vnfId, volumeGroupId);
    }

    @Test
    public void getRelatedVolumeGroupFromVfModuleNotFoundTest() throws Exception {
        String vnfId = "vnfId";
        String volumeGroupId = "volumeGroupId";

        doReturn(Optional.empty()).when(MOCK_aaiResourcesClient).get(eq(VolumeGroups.class), any(AAIResourceUri.class));

        Optional<VolumeGroup> actualVolumeGroup =
                bbInputSetupUtils.getRelatedVolumeGroupFromVfModule(vnfId, volumeGroupId);

        assertEquals(actualVolumeGroup, Optional.empty());
    }

    @Test
    public void getRelatedVolumeGroupFromVfModuleTest() throws Exception {
        Optional<VolumeGroups> expected = Optional.of(new VolumeGroups());
        VolumeGroup volumeGroup = new VolumeGroup();
        volumeGroup.setVolumeGroupId("id123");
        expected.get().getVolumeGroup().add(volumeGroup);
        doReturn(expected).when(MOCK_aaiResourcesClient).get(eq(VolumeGroups.class), any(AAIResourceUri.class));
        Optional<VolumeGroup> actual = this.bbInputSetupUtils.getRelatedVolumeGroupFromVfModule("id123", "id123");
        assertEquals(actual.get().getVolumeGroupId(), expected.get().getVolumeGroup().get(0).getVolumeGroupId());
    }

    @Test
    public void getRelatedVolumeGroupByNameFromVfModuleMultipleVolumeGroupsExceptionTest() throws Exception {
        expectedException.expect(MultipleObjectsFoundException.class);
        expectedException.expectMessage(containsString(
                "Multiple voulme-groups found for vnf-id: vnfId, vf-module-id: volumeGroupId and volume-group-name: volumeGroupName."));
        String vnfId = "vnfId";
        String volumeGroupId = "volumeGroupId";
        String volumeGroupName = "volumeGroupName";

        VolumeGroup volumeGroup = new VolumeGroup();
        volumeGroup.setVolumeGroupId("id123");
        volumeGroup.setVolumeGroupName("name123");

        VolumeGroups volumeGroups = new VolumeGroups();
        volumeGroups.getVolumeGroup().add(volumeGroup);
        volumeGroups.getVolumeGroup().add(volumeGroup);

        Optional<VolumeGroups> optVolumeGroups = Optional.of(volumeGroups);

        doReturn(optVolumeGroups).when(MOCK_aaiResourcesClient).get(eq(VolumeGroups.class), any(AAIResourceUri.class));

        bbInputSetupUtils.getRelatedVolumeGroupByNameFromVfModule(vnfId, volumeGroupId, volumeGroupName);
    }

    @Test
    public void getRelatedVolumeGroupByNameFromVfModuleNotFoundTest() throws Exception {
        String vnfId = "vnfId";
        String volumeGroupId = "volumeGroupId";
        String volumeGroupName = "volumeGroupName";

        doReturn(Optional.empty()).when(MOCK_aaiResourcesClient).get(eq(VolumeGroups.class), any(AAIResourceUri.class));

        Optional<VolumeGroup> actualVolumeGroup =
                bbInputSetupUtils.getRelatedVolumeGroupByNameFromVfModule(vnfId, volumeGroupId, volumeGroupName);

        assertEquals(actualVolumeGroup, Optional.empty());
    }


    @Test
    public void loadOriginalFlowExecutionPathTest() throws Exception {

        String requestId = "123";
        String flowsToExecuteString =
                "[{\"buildingBlock\":{\"mso-id\":\"2f9ddc4b-4dcf-4129-a35f-be1625ae0176\",\"bpmn-flow-name\":\"DeactivateFabricConfigurationBB\",\"key\":\"7adc7c29-21a3-40a2-b8b6-5d4ad08b68e9\",\"is-virtual-link\":false,\"virtual-link-key\":null},\"requestId\":\"9c944122-d161-4280-8594-48c06a9d96d5\",\"apiVersion\":\"7\",\"resourceId\":\"d1d35800-783d-42d3-82f6-d654c5054a6e\",\"requestAction\":\"deleteInstance\",\"vnfType\":\"\",\"aLaCarte\":true,\"homing\":false,\"workflowResourceIds\":{\"serviceInstanceId\":\"ff9dae72-05bb-4277-ad2b-1b082467c138\",\"vnfId\":\"84a29830-e533-4f20-a838-910c740bf24c\",\"networkId\":\"\",\"volumeGroupId\":\"\",\"vfModuleId\":\"d1d35800-783d-42d3-82f6-d654c5054a6e\",\"networkCollectionId\":null,\"configurationId\":\"10f8a3a3-91bf-4821-9515-c01b2864dff0\",\"instanceGroupId\":\"\"},\"requestDetails\":{\"modelInfo\":{\"modelCustomizationName\":\"McmrNcUpVnf20191..cr_mccm_fc_base..module-0\",\"modelInvariantId\":\"8028fcc0-96dc-427d-a4de-4536245943da\",\"modelType\":\"vfModule\",\"modelId\":\"00d15ebb-c80e-43c1-80f0-90c40dde70b0\",\"modelName\":\"McmrNcUpVnf20191..cr_mccm_fc_base..module-0\",\"modelVersion\":\"1\",\"modelCustomizationUuid\":\"7adc7c29-21a3-40a2-b8b6-5d4ad08b68e9\",\"modelVersionId\":\"00d15ebb-c80e-43c1-80f0-90c40dde70b0\",\"modelCustomizationId\":\"7adc7c29-21a3-40a2-b8b6-5d4ad08b68e9\",\"modelUuid\":\"00d15ebb-c80e-43c1-80f0-90c40dde70b0\",\"modelInvariantUuid\":\"8028fcc0-96dc-427d-a4de-4536245943da\",\"modelInstanceName\":\"McmrNcUpVnf20191..cr_mccm_fc_base..module-0\"},\"requestInfo\":{\"source\":\"VID\",\"suppressRollback\":false,\"requestorId\":\"pj8646\"},\"cloudConfiguration\":{\"tenantId\":\"e2a6af59d1cb43b2874e943bbbf8470a\",\"cloudOwner\":\"att-nc\",\"lcpCloudRegionId\":\"auk51b\"},\"requestParameters\":{\"testApi\":\"GR_API\"}},\"configurationResourceKeys\":{\"vfModuleCustomizationUUID\":\"7adc7c29-21a3-40a2-b8b6-5d4ad08b68e9\",\"vnfResourceCustomizationUUID\":\"a80f05b8-d651-44af-b999-8ed78fb4582f\",\"cvnfcCustomizationUUID\":\"69cce457-9ffd-4359-962b-0596a1e83ad1\",\"vnfcName\":\"zauk51bmcmr01mcm001\"}},{\"buildingBlock\":{\"mso-id\":\"1ca5584e-38a9-4c3f-a4b4-99b0d42089ba\",\"bpmn-flow-name\":\"UnassignFabricConfigurationBB\",\"key\":\"7adc7c29-21a3-40a2-b8b6-5d4ad08b68e9\",\"is-virtual-link\":false,\"virtual-link-key\":null},\"requestId\":\"9c944122-d161-4280-8594-48c06a9d96d5\",\"apiVersion\":\"7\",\"resourceId\":\"d1d35800-783d-42d3-82f6-d654c5054a6e\",\"requestAction\":\"deleteInstance\",\"vnfType\":\"\",\"aLaCarte\":true,\"homing\":false,\"workflowResourceIds\":{\"serviceInstanceId\":\"ff9dae72-05bb-4277-ad2b-1b082467c138\",\"vnfId\":\"84a29830-e533-4f20-a838-910c740bf24c\",\"networkId\":\"\",\"volumeGroupId\":\"\",\"vfModuleId\":\"d1d35800-783d-42d3-82f6-d654c5054a6e\",\"networkCollectionId\":null,\"configurationId\":\"10f8a3a3-91bf-4821-9515-c01b2864dff0\",\"instanceGroupId\":\"\"},\"requestDetails\":{\"modelInfo\":{\"modelCustomizationName\":\"McmrNcUpVnf20191..cr_mccm_fc_base..module-0\",\"modelInvariantId\":\"8028fcc0-96dc-427d-a4de-4536245943da\",\"modelType\":\"vfModule\",\"modelId\":\"00d15ebb-c80e-43c1-80f0-90c40dde70b0\",\"modelName\":\"McmrNcUpVnf20191..cr_mccm_fc_base..module-0\",\"modelVersion\":\"1\",\"modelCustomizationUuid\":\"7adc7c29-21a3-40a2-b8b6-5d4ad08b68e9\",\"modelVersionId\":\"00d15ebb-c80e-43c1-80f0-90c40dde70b0\",\"modelCustomizationId\":\"7adc7c29-21a3-40a2-b8b6-5d4ad08b68e9\",\"modelUuid\":\"00d15ebb-c80e-43c1-80f0-90c40dde70b0\",\"modelInvariantUuid\":\"8028fcc0-96dc-427d-a4de-4536245943da\",\"modelInstanceName\":\"McmrNcUpVnf20191..cr_mccm_fc_base..module-0\"},\"requestInfo\":{\"source\":\"VID\",\"suppressRollback\":false,\"requestorId\":\"pj8646\"},\"cloudConfiguration\":{\"tenantId\":\"e2a6af59d1cb43b2874e943bbbf8470a\",\"cloudOwner\":\"att-nc\",\"lcpCloudRegionId\":\"auk51b\"},\"requestParameters\":{\"testApi\":\"GR_API\"}},\"configurationResourceKeys\":{\"vfModuleCustomizationUUID\":\"7adc7c29-21a3-40a2-b8b6-5d4ad08b68e9\",\"vnfResourceCustomizationUUID\":\"a80f05b8-d651-44af-b999-8ed78fb4582f\",\"cvnfcCustomizationUUID\":\"69cce457-9ffd-4359-962b-0596a1e83ad1\",\"vnfcName\":\"zauk51bmcmr01mcm001\"}},{\"buildingBlock\":{\"mso-id\":\"68d16097-4810-477d-803b-8322106106ef\",\"bpmn-flow-name\":\"DeactivateVfModuleBB\",\"key\":\"7adc7c29-21a3-40a2-b8b6-5d4ad08b68e9\",\"is-virtual-link\":false,\"virtual-link-key\":null},\"requestId\":\"9c944122-d161-4280-8594-48c06a9d96d5\",\"apiVersion\":\"7\",\"resourceId\":\"d1d35800-783d-42d3-82f6-d654c5054a6e\",\"requestAction\":\"deleteInstance\",\"vnfType\":\"\",\"aLaCarte\":true,\"homing\":false,\"workflowResourceIds\":{\"serviceInstanceId\":\"ff9dae72-05bb-4277-ad2b-1b082467c138\",\"vnfId\":\"84a29830-e533-4f20-a838-910c740bf24c\",\"networkId\":\"\",\"volumeGroupId\":\"\",\"vfModuleId\":\"d1d35800-783d-42d3-82f6-d654c5054a6e\",\"networkCollectionId\":null,\"configurationId\":\"10f8a3a3-91bf-4821-9515-c01b2864dff0\",\"instanceGroupId\":\"\"},\"requestDetails\":{\"modelInfo\":{\"modelCustomizationName\":\"McmrNcUpVnf20191..cr_mccm_fc_base..module-0\",\"modelInvariantId\":\"8028fcc0-96dc-427d-a4de-4536245943da\",\"modelType\":\"vfModule\",\"modelId\":\"00d15ebb-c80e-43c1-80f0-90c40dde70b0\",\"modelName\":\"McmrNcUpVnf20191..cr_mccm_fc_base..module-0\",\"modelVersion\":\"1\",\"modelCustomizationUuid\":\"7adc7c29-21a3-40a2-b8b6-5d4ad08b68e9\",\"modelVersionId\":\"00d15ebb-c80e-43c1-80f0-90c40dde70b0\",\"modelCustomizationId\":\"7adc7c29-21a3-40a2-b8b6-5d4ad08b68e9\",\"modelUuid\":\"00d15ebb-c80e-43c1-80f0-90c40dde70b0\",\"modelInvariantUuid\":\"8028fcc0-96dc-427d-a4de-4536245943da\",\"modelInstanceName\":\"McmrNcUpVnf20191..cr_mccm_fc_base..module-0\"},\"requestInfo\":{\"source\":\"VID\",\"suppressRollback\":false,\"requestorId\":\"pj8646\"},\"cloudConfiguration\":{\"tenantId\":\"e2a6af59d1cb43b2874e943bbbf8470a\",\"cloudOwner\":\"att-nc\",\"lcpCloudRegionId\":\"auk51b\"},\"requestParameters\":{\"testApi\":\"GR_API\"}},\"configurationResourceKeys\":null},{\"buildingBlock\":{\"mso-id\":\"0b02eb09-bc23-4329-b19e-716dcca4e4a6\",\"bpmn-flow-name\":\"DeleteVfModuleBB\",\"key\":\"7adc7c29-21a3-40a2-b8b6-5d4ad08b68e9\",\"is-virtual-link\":false,\"virtual-link-key\":null},\"requestId\":\"9c944122-d161-4280-8594-48c06a9d96d5\",\"apiVersion\":\"7\",\"resourceId\":\"d1d35800-783d-42d3-82f6-d654c5054a6e\",\"requestAction\":\"deleteInstance\",\"vnfType\":\"\",\"aLaCarte\":true,\"homing\":false,\"workflowResourceIds\":{\"serviceInstanceId\":\"ff9dae72-05bb-4277-ad2b-1b082467c138\",\"vnfId\":\"84a29830-e533-4f20-a838-910c740bf24c\",\"networkId\":\"\",\"volumeGroupId\":\"\",\"vfModuleId\":\"d1d35800-783d-42d3-82f6-d654c5054a6e\",\"networkCollectionId\":null,\"configurationId\":\"10f8a3a3-91bf-4821-9515-c01b2864dff0\",\"instanceGroupId\":\"\"},\"requestDetails\":{\"modelInfo\":{\"modelCustomizationName\":\"McmrNcUpVnf20191..cr_mccm_fc_base..module-0\",\"modelInvariantId\":\"8028fcc0-96dc-427d-a4de-4536245943da\",\"modelType\":\"vfModule\",\"modelId\":\"00d15ebb-c80e-43c1-80f0-90c40dde70b0\",\"modelName\":\"McmrNcUpVnf20191..cr_mccm_fc_base..module-0\",\"modelVersion\":\"1\",\"modelCustomizationUuid\":\"7adc7c29-21a3-40a2-b8b6-5d4ad08b68e9\",\"modelVersionId\":\"00d15ebb-c80e-43c1-80f0-90c40dde70b0\",\"modelCustomizationId\":\"7adc7c29-21a3-40a2-b8b6-5d4ad08b68e9\",\"modelUuid\":\"00d15ebb-c80e-43c1-80f0-90c40dde70b0\",\"modelInvariantUuid\":\"8028fcc0-96dc-427d-a4de-4536245943da\",\"modelInstanceName\":\"McmrNcUpVnf20191..cr_mccm_fc_base..module-0\"},\"requestInfo\":{\"source\":\"VID\",\"suppressRollback\":false,\"requestorId\":\"pj8646\"},\"cloudConfiguration\":{\"tenantId\":\"e2a6af59d1cb43b2874e943bbbf8470a\",\"cloudOwner\":\"att-nc\",\"lcpCloudRegionId\":\"auk51b\"},\"requestParameters\":{\"testApi\":\"GR_API\"}},\"configurationResourceKeys\":null},{\"buildingBlock\":{\"mso-id\":\"bcf95d05-0782-44ff-920d-d5100525c275\",\"bpmn-flow-name\":\"UnassignVfModuleBB\",\"key\":\"7adc7c29-21a3-40a2-b8b6-5d4ad08b68e9\",\"is-virtual-link\":false,\"virtual-link-key\":null},\"requestId\":\"9c944122-d161-4280-8594-48c06a9d96d5\",\"apiVersion\":\"7\",\"resourceId\":\"d1d35800-783d-42d3-82f6-d654c5054a6e\",\"requestAction\":\"deleteInstance\",\"vnfType\":\"\",\"aLaCarte\":true,\"homing\":false,\"workflowResourceIds\":{\"serviceInstanceId\":\"ff9dae72-05bb-4277-ad2b-1b082467c138\",\"vnfId\":\"84a29830-e533-4f20-a838-910c740bf24c\",\"networkId\":\"\",\"volumeGroupId\":\"\",\"vfModuleId\":\"d1d35800-783d-42d3-82f6-d654c5054a6e\",\"networkCollectionId\":null,\"configurationId\":\"10f8a3a3-91bf-4821-9515-c01b2864dff0\",\"instanceGroupId\":\"\"},\"requestDetails\":{\"modelInfo\":{\"modelCustomizationName\":\"McmrNcUpVnf20191..cr_mccm_fc_base..module-0\",\"modelInvariantId\":\"8028fcc0-96dc-427d-a4de-4536245943da\",\"modelType\":\"vfModule\",\"modelId\":\"00d15ebb-c80e-43c1-80f0-90c40dde70b0\",\"modelName\":\"McmrNcUpVnf20191..cr_mccm_fc_base..module-0\",\"modelVersion\":\"1\",\"modelCustomizationUuid\":\"7adc7c29-21a3-40a2-b8b6-5d4ad08b68e9\",\"modelVersionId\":\"00d15ebb-c80e-43c1-80f0-90c40dde70b0\",\"modelCustomizationId\":\"7adc7c29-21a3-40a2-b8b6-5d4ad08b68e9\",\"modelUuid\":\"00d15ebb-c80e-43c1-80f0-90c40dde70b0\",\"modelInvariantUuid\":\"8028fcc0-96dc-427d-a4de-4536245943da\",\"modelInstanceName\":\"McmrNcUpVnf20191..cr_mccm_fc_base..module-0\"},\"requestInfo\":{\"source\":\"VID\",\"suppressRollback\":false,\"requestorId\":\"pj8646\"},\"cloudConfiguration\":{\"tenantId\":\"e2a6af59d1cb43b2874e943bbbf8470a\",\"cloudOwner\":\"att-nc\",\"lcpCloudRegionId\":\"auk51b\"},\"requestParameters\":{\"testApi\":\"GR_API\"}},\"configurationResourceKeys\":null}]";
        ObjectMapper om = new ObjectMapper();
        List<ExecuteBuildingBlock> expectedFlowsToExecute = null;
        try {
            ExecuteBuildingBlock[] asArray = om.readValue(flowsToExecuteString, ExecuteBuildingBlock[].class);
            expectedFlowsToExecute = Arrays.asList(asArray);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        InfraActiveRequests request = new InfraActiveRequests();
        request.setRequestId("requestId");
        request.setOriginalRequestId("originalRequestId");
        doReturn(request).when(MOCK_requestsDbClient).getInfraActiveRequestbyRequestId(anyString());
        RequestProcessingData rpd = new RequestProcessingData();
        rpd.setValue(flowsToExecuteString);
        doReturn(rpd).when(MOCK_requestsDbClient).getRequestProcessingDataBySoRequestIdAndName(anyString(),
                anyString());

        List<ExecuteBuildingBlock> flowsToExecute = bbInputSetupUtils.loadOriginalFlowExecutionPath(requestId);
        assertEquals(expectedFlowsToExecute.size(), flowsToExecute.size());
    }

    @Test
    public void getRelatedConfigurationByNameFromServiceInstanceExceptionTest() throws Exception {
        Configuration configuration = new Configuration();
        configuration.setConfigurationId("id123");

        Configurations configurations = new Configurations();
        configurations.getConfiguration().add(configuration);
        configurations.getConfiguration().add(configuration);

        Optional<Configurations> optConfigurations = Optional.of(configurations);

        doReturn(optConfigurations).when(MOCK_aaiResourcesClient).get(eq(Configurations.class),
                any(AAIResourceUri.class));

        expectedException.expect(MultipleObjectsFoundException.class);
        this.bbInputSetupUtils.getRelatedConfigurationByNameFromServiceInstance("id123", "name123");
    }

    @Test
    public void getRelatedConfigurationByNameFromServiceInstanceNotFoundTest() throws Exception {
        doReturn(Optional.empty()).when(MOCK_aaiResourcesClient).get(eq(Configurations.class),
                any(AAIResourceUri.class));
        Optional<Configuration> actualConfiguration =
                bbInputSetupUtils.getRelatedConfigurationByNameFromServiceInstance("id123", "name123");
        assertEquals(actualConfiguration, Optional.empty());
    }

    @Test
    public void getRelatedConfigurationByNameFromServiceInstanceTest() throws Exception {
        Optional<Configurations> expected = Optional.of(new Configurations());
        Configuration configuration = new Configuration();
        configuration.setConfigurationId("id123");
        expected.get().getConfiguration().add(configuration);
        doReturn(expected).when(MOCK_aaiResourcesClient).get(eq(Configurations.class), any(AAIResourceUri.class));
        Optional<Configuration> actual =
                this.bbInputSetupUtils.getRelatedConfigurationByNameFromServiceInstance("id123", "name123");
        assertEquals(actual.get().getConfigurationId(), expected.get().getConfiguration().get(0).getConfigurationId());
    }

    @Test
    public void existsAAIVfModuleGloballyByNameTest() throws Exception {
        AAIResourceUri expectedUri =
                AAIUriFactory.createNodesUri(AAIObjectPlurals.VF_MODULE).queryParam("vf-module-name", "testVfModule");
        bbInputSetupUtils.existsAAIVfModuleGloballyByName("testVfModule");
        verify(MOCK_aaiResourcesClient, times(1)).exists(expectedUri);
    }

    @Test
    public void existsAAIConfigurationGloballyByNameTest() throws Exception {
        AAIResourceUri expectedUri = AAIUriFactory.createResourceUri(AAIObjectPlurals.CONFIGURATION)
                .queryParam("configuration-name", "testConfig");
        bbInputSetupUtils.existsAAIConfigurationGloballyByName("testConfig");
        verify(MOCK_aaiResourcesClient, times(1)).exists(expectedUri);
    }

    @Test
    public void existsAAINetworksGloballyByNameTest() throws Exception {
        AAIResourceUri expectedUri =
                AAIUriFactory.createResourceUri(AAIObjectPlurals.L3_NETWORK).queryParam("network-name", "testNetwork");
        bbInputSetupUtils.existsAAINetworksGloballyByName("testNetwork");
        verify(MOCK_aaiResourcesClient, times(1)).exists(expectedUri);
    }

    @Test
    public void existsAAIVolumeGroupGloballyByNameTest() throws Exception {
        AAIResourceUri expectedUri = AAIUriFactory.createNodesUri(AAIObjectPlurals.VOLUME_GROUP)
                .queryParam("volume-group-name", "testVoumeGroup");
        bbInputSetupUtils.existsAAIVolumeGroupGloballyByName("testVoumeGroup");
        verify(MOCK_aaiResourcesClient, times(1)).exists(expectedUri);
    }

    @Test
    public void shouldChangeInfraActiveRequestVnfId() throws IOException {
        final String vnfId = "vnfId";
        InfraActiveRequests infraActiveRequests = loadExpectedInfraActiveRequest();

        bbInputSetupUtils.updateInfraActiveRequestVnfId(infraActiveRequests, vnfId);

        assertEquals(infraActiveRequests.getVnfId(), vnfId);
    }

    @Test
    public void shouldChangeInfraActiveRequestVfModuleId() throws IOException {
        final String vfModuleId = "vfModuleId";
        InfraActiveRequests infraActiveRequests = loadExpectedInfraActiveRequest();

        bbInputSetupUtils.updateInfraActiveRequestVfModuleId(infraActiveRequests, vfModuleId);

        assertEquals(infraActiveRequests.getVfModuleId(), vfModuleId);
    }

    @Test
    public void shouldChangeInfraActiveRequestVolumeGroupId() throws IOException {
        final String volumeGroupId = "volumeGroupId";
        InfraActiveRequests infraActiveRequests = loadExpectedInfraActiveRequest();

        bbInputSetupUtils.updateInfraActiveRequestVolumeGroupId(infraActiveRequests, volumeGroupId);

        assertEquals(infraActiveRequests.getVolumeGroupId(), volumeGroupId);
    }

    @Test
    public void shouldChangeInfraActiveRequestNetworkId() throws IOException {
        final String networkId = "activeRequestNetworkId";
        InfraActiveRequests infraActiveRequests = loadExpectedInfraActiveRequest();

        bbInputSetupUtils.updateInfraActiveRequestNetworkId(infraActiveRequests, networkId);

        assertEquals(infraActiveRequests.getNetworkId(), networkId);
    }

    @Test
    public void getAAIVpnBindingNullTest() {
        assertNull(bbInputSetupUtils.getAAIVpnBinding("vpnBindingId"));
    }

    @Test
    public void getAAIVolumeGroupNullTest() {
        VolumeGroup actualAaiVnf =
                bbInputSetupUtils.getAAIVolumeGroup("cloudOwnerId", "cloudRegionId", "volumeGroupId");
        assertNull(actualAaiVnf);
    }

    @Test
    public void getAAIVfModuleNullTest() {
        assertNull(bbInputSetupUtils.getAAIVfModule("vnfId", "vfModuleId"));
    }

    @Test
    public void getAAIL3NetworkNullTest() {
        assertNull(bbInputSetupUtils.getAAIL3Network("networkId"));
    }

    @Test
    public void getAICVpnBindingFromNetwork_noVpnBindingTest() throws IOException {
        L3Network l3Network =
                mapper.readValue(new File(RESOURCE_PATH + "aaiL3NetworkInputWithSubnets.json"), L3Network.class);

        Optional<VpnBinding> actual = bbInputSetupUtils.getAICVpnBindingFromNetwork(l3Network);
        assertEquals(actual, Optional.empty());
    }

    @Test
    public void getAAIServiceInstancesGloballyByName_noAAIResourceTest() {
        final String serviceInstanceName = "serviceInstanceName";

        doReturn(Optional.empty()).when(MOCK_aaiResourcesClient).get(isA(Class.class), isA(AAIResourceUri.class));
        ServiceInstances actualServiceInstances =
                bbInputSetupUtils.getAAIServiceInstancesGloballyByName(serviceInstanceName);

        assertNull(actualServiceInstances);
    }

    @Test
    public void getAAIVnfsGloballyByName_noAAIResourceTest() {
        final String vnfName = "vnfName";

        doReturn(Optional.empty()).when(MOCK_aaiResourcesClient).get(isA(Class.class), isA(AAIResourceUri.class));
        GenericVnfs actualGenericVnfs = bbInputSetupUtils.getAAIVnfsGloballyByName(vnfName);

        assertNull(actualGenericVnfs);
    }

    private InfraActiveRequests loadExpectedInfraActiveRequest() throws IOException {
        return mapper.readValue(new File(RESOURCE_PATH + "InfraActiveRequestExpected.json"), InfraActiveRequests.class);
    }
}
