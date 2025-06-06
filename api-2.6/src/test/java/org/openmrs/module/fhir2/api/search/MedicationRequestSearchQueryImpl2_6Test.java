/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.fhir2.api.search;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import java.util.HashSet;
import java.util.List;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.DrugOrder;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.TestFhirSpringConfiguration;
import org.openmrs.module.fhir2.api.dao.FhirMedicationRequestDao;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.openmrs.module.fhir2.api.translators.MedicationRequestTranslator;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = TestFhirSpringConfiguration.class, inheritLocations = false)
public class MedicationRequestSearchQueryImpl2_6Test extends BaseModuleContextSensitiveTest {
	
	public static final String PATIENT_UUID = "da7f524f-27ce-4bb2-86d6-6d1d05312bd5"; // patient 2 in test dataset
	
	public static final String MEDICATION_REQUEST_UUID = "dfca4077-493c-496b-8312-856ee5d1cc26"; // order 2 in the test database;
	
	@Autowired
	private MedicationRequestTranslator translator;
	
	@Autowired
	private FhirMedicationRequestDao dao;
	
	@Autowired
	private SearchQueryInclude<MedicationRequest> searchQueryInclude;
	
	@Autowired
	private SearchQuery<DrugOrder, MedicationRequest, FhirMedicationRequestDao, MedicationRequestTranslator, SearchQueryInclude<MedicationRequest>> searchQuery;
	
	@Before
	public void setup() {
		executeDataSet("org/openmrs/api/include/MedicationDispenseServiceTest-initialData.xml");
		updateSearchIndex();
	}
	
	private IBundleProvider search(SearchParameterMap theParams) {
		return searchQuery.getQueryResults(theParams, dao, translator, searchQueryInclude);
	}
	
	private List<IBaseResource> get(IBundleProvider results) {
		return results.getResources(0, 9);
	}
	
	@Test
	public void searchForMedicationRequest_shouldReturnMedicationRequestByPatientUuidAndRevIncludeAssociatedMedicationDispense() {
		ReferenceAndListParam patientReference = new ReferenceAndListParam()
		        .addAnd(new ReferenceOrListParam().add(new ReferenceParam().setValue(PATIENT_UUID)));
		
		HashSet<Include> revIncludes = new HashSet<>();
		revIncludes.add(new Include("MedicationDispense:prescription", true));
		
		SearchParameterMap theParams = new SearchParameterMap()
		        .addParameter(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER, patientReference)
		        .addParameter(FhirConstants.REVERSE_INCLUDE_SEARCH_HANDLER, revIncludes);
		
		IBundleProvider results = search(theParams);
		
		assertThat(results, notNullValue());
		
		List<IBaseResource> resultList = get(results);
		
		assertThat(resultList, not(empty()));
		assertThat(resultList, hasSize(7));
		assertThat(resultList, hasItem(hasProperty("id", equalTo(MEDICATION_REQUEST_UUID)))); // order 2
		assertThat(resultList, hasItem(hasProperty("id", equalTo("2662e6c2-697b-11e3-bd76-0800271c1b75")))); // order 222
		assertThat(resultList, hasItem(hasProperty("id", equalTo("e3d621f0-a4d5-47d1-a4e1-5ace3f66d43a")))); // order 3
		assertThat(resultList, hasItem(hasProperty("id", equalTo("047b7424-6f33-4357-823c-420f316bb039")))); // order 4
		assertThat(resultList, hasItem(hasProperty("id", equalTo("9c21e407-697b-11e3-bd76-0800271c1b75")))); // order 444
		assertThat(resultList, hasItem(hasProperty("id", equalTo("0c96f25c-4949-4f72-9931-d808fbc226db")))); // order 5
		assertThat(resultList, hasItem(hasProperty("id", equalTo("b75c5c9e-b66c-11ec-8065-0242ac110002")))); // medication dispense 1 from Medication dispense dataset (see above)
		// note that discontinue orders, #22 and #44 are *not* included, see: https://issues.openmrs.org/browse/FM2-532
	}
	
}
