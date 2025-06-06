/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.fhir2.providers.r4;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.NumberParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.MethodNotAllowedException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import lombok.AccessLevel;
import lombok.Getter;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Patient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.FhirTestConstants;
import org.openmrs.module.fhir2.api.FhirObservationService;
import org.openmrs.module.fhir2.api.search.param.ObservationSearchParams;
import org.openmrs.module.fhir2.providers.BaseFhirProvenanceResourceTest;

@RunWith(MockitoJUnitRunner.class)
public class ObservationFhirResourceProviderTest extends BaseFhirProvenanceResourceTest<Observation> {
	
	private static final String PATIENT_UUID = "5946f880-b197-400b-9caa-a3c661d23041";
	
	private static final String OBSERVATION_UUID = "1223h34-34nj3-34nj34-34nj";
	
	private static final String WRONG_OBSERVATION_UUID = "hj243h34-cb4vsd-34xxx34-ope4jj";
	
	private static final int START_INDEX = 0;
	
	private static final int END_INDEX = 10;
	
	private static final String CIEL_DIASTOLIC_BP = "5086";
	
	private static final String LOINC_SYSTOLIC_BP = "8480-6";
	
	@Mock
	private FhirObservationService observationService;
	
	@Getter(AccessLevel.PUBLIC)
	private ObservationFhirResourceProvider resourceProvider;
	
	private Observation observation;
	
	@Captor
	private ArgumentCaptor<ObservationSearchParams> searchParamsCaptor;
	
	@Before
	public void setup() {
		resourceProvider = new ObservationFhirResourceProvider();
		resourceProvider.setObservationService(observationService);
	}
	
	@Before
	public void initObservation() {
		observation = new Observation();
		observation.setId(OBSERVATION_UUID);
		observation.setStatus(Observation.ObservationStatus.UNKNOWN);
		setProvenanceResources(observation);
	}
	
	private List<IBaseResource> get(IBundleProvider results) {
		return results.getResources(START_INDEX, END_INDEX);
	}
	
	@Test
	public void getResourceType_shouldReturnResourceType() {
		assertThat(resourceProvider.getResourceType(), equalTo(Observation.class));
		assertThat(resourceProvider.getResourceType().getName(), equalTo(Observation.class.getName()));
	}
	
	@Test
	public void getObservationByUuid_shouldReturnMatchingObservation() {
		when(observationService.get(OBSERVATION_UUID)).thenReturn(observation);
		IdType id = new IdType();
		id.setValue(OBSERVATION_UUID);
		
		Observation result = resourceProvider.getObservationById(id);
		
		assertThat(result, notNullValue());
		assertThat(result.getId(), notNullValue());
		assertThat(result.getId(), equalTo(OBSERVATION_UUID));
	}
	
	@Test(expected = ResourceNotFoundException.class)
	public void getObservationWithWrongUuid_shouldThrowResourceNotFoundException() {
		IdType id = new IdType();
		id.setValue(WRONG_OBSERVATION_UUID);
		
		resourceProvider.getObservationById(id);
	}
	
	@Test
	public void searchObservations_shouldReturnMatchingObservations() {
		when(observationService.searchForObservations(any()))
		        .thenReturn(new MockIBundleProvider<>(Collections.singletonList(observation), 10, 1));
		
		TokenAndListParam code = new TokenAndListParam();
		TokenParam codingToken = new TokenParam();
		codingToken.setValue("1000");
		code.addAnd(codingToken);
		
		IBundleProvider results = resourceProvider.searchObservations(null, null, null, null, null, null, null, null, code,
		    null, null, null, null, null, null, null);
		
		List<IBaseResource> resultList = get(results);
		
		assertThat(results, notNullValue());
		assertThat(resultList, hasSize(equalTo(1)));
		assertThat(resultList.get(0), notNullValue());
		assertThat(resultList.get(0).fhirType(), equalTo(FhirConstants.OBSERVATION));
		assertThat(resultList.get(0).getIdElement().getIdPart(), equalTo(OBSERVATION_UUID));
	}
	
	@Test
	public void searchObservations_shouldReturnMatchingObservationsWhenPatientParamIsSpecified() {
		when(observationService.searchForObservations(any()))
		        .thenReturn(new MockIBundleProvider<>(Collections.singletonList(observation), 10, 1));
		
		ReferenceAndListParam patientParam = new ReferenceAndListParam();
		patientParam.addValue(new ReferenceOrListParam().add(new ReferenceParam().setChain(Patient.SP_NAME)));
		
		IBundleProvider results = resourceProvider.searchObservations(null, null, null, null, null, null, null, null, null,
		    null, null, null, null, patientParam, null, null);
		
		List<IBaseResource> resultList = get(results);
		
		assertThat(results, notNullValue());
		assertThat(resultList, hasSize(equalTo(1)));
		assertThat(resultList.get(0), notNullValue());
		assertThat(resultList.get(0).fhirType(), equalTo(FhirConstants.OBSERVATION));
		assertThat(resultList.get(0).getIdElement().getIdPart(), equalTo(OBSERVATION_UUID));
	}
	
	@Test
	public void searchObservations_shouldReturnMatchingObservationsWhenEncounterParamIsSpecified() {
		when(observationService.searchForObservations(any()))
		        .thenReturn(new MockIBundleProvider<>(Collections.singletonList(observation), 10, 1));
		
		ReferenceAndListParam encounterParam = new ReferenceAndListParam();
		encounterParam.addValue(new ReferenceOrListParam().add(new ReferenceParam().setChain(Encounter.SP_TYPE)));
		
		IBundleProvider results = resourceProvider.searchObservations(encounterParam, null, null, null, null, null, null,
		    null, null, null, null, null, null, null, null, null);
		
		List<IBaseResource> resultList = get(results);
		
		assertThat(results, notNullValue());
		assertThat(resultList, hasSize(equalTo(1)));
		assertThat(resultList.get(0), notNullValue());
		assertThat(resultList.get(0).fhirType(), equalTo(FhirConstants.OBSERVATION));
		assertThat(resultList.get(0).getIdElement().getIdPart(), equalTo(OBSERVATION_UUID));
	}
	
	@Test
	public void searchObservations_shouldAddRelatedResourcesWhenIncluded() {
		HashSet<Include> includes = new HashSet<>();
		includes.add(new Include("Observation:patient"));
		
		when(observationService.searchForObservations(any()))
		        .thenReturn(new MockIBundleProvider<>(Arrays.asList(observation, new Patient()), 10, 1));
		
		IBundleProvider results = resourceProvider.searchObservations(null, null, null, null, null, null, null, null, null,
		    null, null, null, null, null, includes, null);
		
		List<IBaseResource> resultList = get(results);
		
		assertThat(results, notNullValue());
		assertThat(resultList, hasSize(equalTo(2)));
		assertThat(resultList.get(0), notNullValue());
		assertThat(resultList.get(0).fhirType(), equalTo(FhirConstants.OBSERVATION));
		assertThat(resultList.get(1).fhirType(), equalTo(FhirConstants.PATIENT));
		assertThat(resultList.get(0).getIdElement().getIdPart(), equalTo(OBSERVATION_UUID));
	}
	
	@Test
	public void searchObservations_shouldNotAddRelatedResourcesForEmptyInclude() {
		HashSet<Include> includes = new HashSet<>();
		
		when(observationService.searchForObservations(any()))
		        .thenReturn(new MockIBundleProvider<>(Collections.singletonList(observation), 10, 1));
		
		IBundleProvider results = resourceProvider.searchObservations(null, null, null, null, null, null, null, null, null,
		    null, null, null, null, null, includes, null);
		
		List<IBaseResource> resultList = get(results);
		
		assertThat(results, notNullValue());
		assertThat(resultList, hasSize(equalTo(1)));
		assertThat(resultList.get(0), notNullValue());
		assertThat(resultList.get(0).fhirType(), equalTo(FhirConstants.OBSERVATION));
		assertThat(resultList.get(0).getIdElement().getIdPart(), equalTo(OBSERVATION_UUID));
	}
	
	@Test
	public void searchObservations_shouldAddRelatedResourcesWhenReverseIncluded() {
		HashSet<Include> includes = new HashSet<>();
		includes.add(new Include("DiagnosticReport:result"));
		
		when(observationService.searchForObservations(any()))
		        .thenReturn(new MockIBundleProvider<>(Arrays.asList(observation, new DiagnosticReport()), 10, 1));
		
		IBundleProvider results = resourceProvider.searchObservations(null, null, null, null, null, null, null, null, null,
		    null, null, null, null, null, includes, null);
		
		List<IBaseResource> resultList = get(results);
		
		assertThat(results, notNullValue());
		assertThat(resultList, hasSize(equalTo(2)));
		assertThat(resultList.get(0), notNullValue());
		assertThat(resultList.get(0).fhirType(), equalTo(FhirConstants.OBSERVATION));
		assertThat(resultList.get(1).fhirType(), equalTo(FhirConstants.DIAGNOSTIC_REPORT));
		assertThat(resultList.get(0).getIdElement().getIdPart(), equalTo(OBSERVATION_UUID));
	}
	
	@Test
	public void searchObservations_shouldNotAddRelatedResourcesForEmptyReverseInclude() {
		HashSet<Include> includes = new HashSet<>();
		
		when(observationService.searchForObservations(any()))
		        .thenReturn(new MockIBundleProvider<>(Collections.singletonList(observation), 10, 1));
		
		IBundleProvider results = resourceProvider.searchObservations(null, null, null, null, null, null, null, null, null,
		    null, null, null, null, null, includes, null);
		
		verify(observationService).searchForObservations(searchParamsCaptor.capture());
		List<IBaseResource> resultList = get(results);
		
		assertThat(results, notNullValue());
		assertThat(resultList, hasSize(equalTo(1)));
		assertThat(resultList.get(0), notNullValue());
		assertThat(resultList.get(0).fhirType(), equalTo(FhirConstants.OBSERVATION));
		assertThat(resultList.get(0).getIdElement().getIdPart(), equalTo(OBSERVATION_UUID));
		
		Set<Include> includesParam = searchParamsCaptor.getValue().getIncludes();
		assertThat(includesParam, nullValue());
	}
	
	@Test
	public void createObservation_shouldCreateNewObservation() {
		when(observationService.create(observation)).thenReturn(observation);
		
		MethodOutcome result = resourceProvider.createObservationResource(observation);
		
		assertThat(result, notNullValue());
		assertThat(result.getCreated(), is(true));
		assertThat(result.getResource(), equalTo(observation));
	}
	
	@Test
	public void updateObservation_shouldUpdateObservation() {
		when(observationService.update(OBSERVATION_UUID, observation)).thenReturn(observation);
		
		MethodOutcome result = resourceProvider.updateObservation(new IdType().setValue(OBSERVATION_UUID), observation);
		
		assertThat(result, notNullValue());
		assertThat(result.getResource(), equalTo(observation));
	}
	
	@Test(expected = InvalidRequestException.class)
	public void updateObservation_shouldThrowInvalidRequestExceptionForUuidMismatch() {
		when(observationService.update(WRONG_OBSERVATION_UUID, observation)).thenThrow(InvalidRequestException.class);
		
		resourceProvider.updateObservation(new IdType().setValue(WRONG_OBSERVATION_UUID), observation);
	}
	
	@Test(expected = InvalidRequestException.class)
	public void updateObservation_shouldThrowInvalidRequestExceptionForMissingId() {
		Observation noObservation = new Observation();
		
		when(observationService.update(WRONG_OBSERVATION_UUID, noObservation)).thenThrow(InvalidRequestException.class);
		
		resourceProvider.updateObservation(new IdType().setValue(WRONG_OBSERVATION_UUID), noObservation);
	}
	
	@Test(expected = MethodNotAllowedException.class)
	public void updateObservation_shouldThrowMethodNotAllowedIfDoesNotExist() {
		observation.setId(WRONG_OBSERVATION_UUID);
		
		when(observationService.update(WRONG_OBSERVATION_UUID, observation)).thenThrow(MethodNotAllowedException.class);
		
		resourceProvider.updateObservation(new IdType().setValue(WRONG_OBSERVATION_UUID), observation);
	}
	
	@Test
	public void deleteObservation_shouldDeleteObservation() {
		OperationOutcome result = resourceProvider.deleteObservationResource(new IdType().setValue(OBSERVATION_UUID));
		
		assertThat(result, notNullValue());
		assertThat(result.getIssue(), notNullValue());
		assertThat(result.getIssueFirstRep().getSeverity(), equalTo(OperationOutcome.IssueSeverity.INFORMATION));
		assertThat(result.getIssueFirstRep().getDetails().getCodingFirstRep().getCode(), equalTo("MSG_DELETED"));
	}
	
	@Test
	public void getLastnObservations_shouldReturnRecentNObservations() {
		NumberParam max = new NumberParam(2);
		ReferenceAndListParam referenceParam = new ReferenceAndListParam();
		ReferenceParam patient = new ReferenceParam();
		
		patient.setValue(PATIENT_UUID);
		
		referenceParam.addValue(new ReferenceOrListParam().add(patient));
		
		TokenAndListParam categories = new TokenAndListParam().addAnd(new TokenParam().setValue("laboratory"));
		
		TokenAndListParam code = new TokenAndListParam().addAnd(
		    new TokenParam().setSystem(FhirTestConstants.LOINC_SYSTEM_URL).setValue(LOINC_SYSTOLIC_BP),
		    new TokenParam().setSystem(FhirTestConstants.CIEL_SYSTEM_URN).setValue(CIEL_DIASTOLIC_BP));
		
		ObservationSearchParams searchParams = new ObservationSearchParams();
		searchParams.setPatient(referenceParam);
		searchParams.setCategory(categories);
		searchParams.setCode(code);
		
		when(observationService.getLastnObservations(max, searchParams))
		        .thenReturn(new MockIBundleProvider<>(Collections.singletonList(observation), 10, 1));
		
		IBundleProvider results = resourceProvider.getLastnObservations(max, referenceParam, null, categories, code);
		
		List<IBaseResource> resultList = get(results);
		
		assertThat(results, notNullValue());
		assertThat(resultList, hasSize(equalTo(1)));
		assertThat(resultList.get(0), notNullValue());
		assertThat(resultList.get(0).fhirType(), equalTo(FhirConstants.OBSERVATION));
		assertThat(resultList.get(0).getIdElement().getIdPart(), equalTo(OBSERVATION_UUID));
		
	}
	
	@Test
	public void getLastn_shouldReturnFirstRecentObservationsWhenMaxIsMissing() {
		ReferenceAndListParam referenceParam = new ReferenceAndListParam();
		ReferenceParam patient = new ReferenceParam();
		
		patient.setValue(PATIENT_UUID);
		
		referenceParam.addValue(new ReferenceOrListParam().add(patient));
		
		TokenAndListParam categories = new TokenAndListParam().addAnd(new TokenParam().setValue("laboratory"));
		
		TokenAndListParam code = new TokenAndListParam().addAnd(
		    new TokenParam().setSystem(FhirTestConstants.LOINC_SYSTEM_URL).setValue(LOINC_SYSTOLIC_BP),
		    new TokenParam().setSystem(FhirTestConstants.CIEL_SYSTEM_URN).setValue(CIEL_DIASTOLIC_BP));
		
		ObservationSearchParams searchParams = new ObservationSearchParams();
		searchParams.setPatient(referenceParam);
		searchParams.setCategory(categories);
		searchParams.setCode(code);
		
		when(observationService.getLastnObservations(null, searchParams))
		        .thenReturn(new MockIBundleProvider<>(Collections.singletonList(observation), 10, 1));
		
		IBundleProvider results = resourceProvider.getLastnObservations(null, referenceParam, null, categories, code);
		
		List<IBaseResource> resultList = get(results);
		
		assertThat(results, notNullValue());
		assertThat(resultList, hasSize(equalTo(1)));
		assertThat(resultList.get(0), notNullValue());
		assertThat(resultList.get(0).fhirType(), equalTo(FhirConstants.OBSERVATION));
		assertThat(resultList.get(0).getIdElement().getIdPart(), equalTo(OBSERVATION_UUID));
	}
	
	@Test
	public void getLastnObservations_shouldReturnRecentNObservationsWhenPatientReferenceIsPassedInPatientParameter() {
		NumberParam max = new NumberParam(2);
		ReferenceAndListParam referenceParam = new ReferenceAndListParam();
		ReferenceParam patient = new ReferenceParam();
		
		patient.setValue(PATIENT_UUID);
		
		referenceParam.addValue(new ReferenceOrListParam().add(patient));
		
		TokenAndListParam categories = new TokenAndListParam().addAnd(new TokenParam().setValue("laboratory"));
		
		TokenAndListParam code = new TokenAndListParam().addAnd(
		    new TokenParam().setSystem(FhirTestConstants.LOINC_SYSTEM_URL).setValue(LOINC_SYSTOLIC_BP),
		    new TokenParam().setSystem(FhirTestConstants.CIEL_SYSTEM_URN).setValue(CIEL_DIASTOLIC_BP));
		
		ObservationSearchParams searchParams = new ObservationSearchParams();
		searchParams.setPatient(referenceParam);
		searchParams.setCategory(categories);
		searchParams.setCode(code);
		
		when(observationService.getLastnObservations(max, searchParams))
		        .thenReturn(new MockIBundleProvider<>(Collections.singletonList(observation), 10, 1));
		
		IBundleProvider results = resourceProvider.getLastnObservations(max, null, referenceParam, categories, code);
		
		List<IBaseResource> resultList = get(results);
		
		assertThat(results, notNullValue());
		assertThat(resultList, hasSize(equalTo(1)));
		assertThat(resultList.get(0), notNullValue());
		assertThat(resultList.get(0).fhirType(), equalTo(FhirConstants.OBSERVATION));
		assertThat(resultList.get(0).getIdElement().getIdPart(), equalTo(OBSERVATION_UUID));
	}
	
	@Test
	public void getLastnObservations_shouldReturnRecentNObservationsWhenPatientReferenceIsNotGiven() {
		NumberParam max = new NumberParam(2);
		
		TokenAndListParam categories = new TokenAndListParam().addAnd(new TokenParam().setValue("laboratory"));
		
		TokenAndListParam code = new TokenAndListParam().addAnd(
		    new TokenParam().setSystem(FhirTestConstants.LOINC_SYSTEM_URL).setValue(LOINC_SYSTOLIC_BP),
		    new TokenParam().setSystem(FhirTestConstants.CIEL_SYSTEM_URN).setValue(CIEL_DIASTOLIC_BP));
		
		ObservationSearchParams searchParams = new ObservationSearchParams();
		searchParams.setCategory(categories);
		searchParams.setCode(code);
		
		when(observationService.getLastnObservations(max, searchParams))
		        .thenReturn(new MockIBundleProvider<>(Collections.singletonList(observation), 10, 1));
		
		IBundleProvider results = resourceProvider.getLastnObservations(max, null, null, categories, code);
		
		List<IBaseResource> resultList = get(results);
		
		assertThat(results, notNullValue());
		assertThat(resultList, hasSize(equalTo(1)));
		assertThat(resultList.get(0), notNullValue());
		assertThat(resultList.get(0).fhirType(), equalTo(FhirConstants.OBSERVATION));
		assertThat(resultList.get(0).getIdElement().getIdPart(), equalTo(OBSERVATION_UUID));
	}
	
	@Test
	public void getLastnObservations_shouldReturnRecentNObservationsWhenNoCategoryIsSpecified() {
		NumberParam max = new NumberParam(2);
		ReferenceAndListParam referenceParam = new ReferenceAndListParam();
		ReferenceParam patient = new ReferenceParam();
		
		patient.setValue(PATIENT_UUID);
		
		referenceParam.addValue(new ReferenceOrListParam().add(patient));
		
		TokenAndListParam code = new TokenAndListParam().addAnd(
		    new TokenParam().setSystem(FhirTestConstants.LOINC_SYSTEM_URL).setValue(LOINC_SYSTOLIC_BP),
		    new TokenParam().setSystem(FhirTestConstants.CIEL_SYSTEM_URN).setValue(CIEL_DIASTOLIC_BP));
		
		ObservationSearchParams searchParams = new ObservationSearchParams();
		searchParams.setPatient(referenceParam);
		searchParams.setCode(code);
		
		when(observationService.getLastnObservations(max, searchParams))
		        .thenReturn(new MockIBundleProvider<>(Collections.singletonList(observation), 10, 1));
		
		IBundleProvider results = resourceProvider.getLastnObservations(max, referenceParam, null, null, code);
		
		List<IBaseResource> resultList = get(results);
		
		assertThat(results, notNullValue());
		assertThat(resultList, hasSize(equalTo(1)));
		assertThat(resultList.get(0), notNullValue());
		assertThat(resultList.get(0).fhirType(), equalTo(FhirConstants.OBSERVATION));
		assertThat(resultList.get(0).getIdElement().getIdPart(), equalTo(OBSERVATION_UUID));
	}
	
	@Test
	public void getLastnObservations_shouldReturnRecentNObservationsWhenNoCodeIsSpecified() {
		NumberParam max = new NumberParam(2);
		ReferenceAndListParam referenceParam = new ReferenceAndListParam();
		ReferenceParam patient = new ReferenceParam();
		
		patient.setValue(PATIENT_UUID);
		
		referenceParam.addValue(new ReferenceOrListParam().add(patient));
		
		TokenAndListParam categories = new TokenAndListParam().addAnd(new TokenParam().setValue("laboratory"));
		
		ObservationSearchParams searchParams = new ObservationSearchParams();
		searchParams.setPatient(referenceParam);
		searchParams.setCategory(categories);
		
		when(observationService.getLastnObservations(max, searchParams))
		        .thenReturn(new MockIBundleProvider<>(Collections.singletonList(observation), 10, 1));
		
		IBundleProvider results = resourceProvider.getLastnObservations(max, referenceParam, null, categories, null);
		
		List<IBaseResource> resultList = get(results);
		
		assertThat(results, notNullValue());
		assertThat(resultList, hasSize(equalTo(1)));
		assertThat(resultList.get(0), notNullValue());
		assertThat(resultList.get(0).fhirType(), equalTo(FhirConstants.OBSERVATION));
		assertThat(resultList.get(0).getIdElement().getIdPart(), equalTo(OBSERVATION_UUID));
	}
	
	@Test
	public void getLastnObservations_shouldReturnRecentNObservationsWhenBothCodeAndCategoryIsNotSpecified() {
		NumberParam max = new NumberParam(2);
		ReferenceAndListParam referenceParam = new ReferenceAndListParam();
		ReferenceParam patient = new ReferenceParam();
		
		patient.setValue(PATIENT_UUID);
		
		referenceParam.addValue(new ReferenceOrListParam().add(patient));
		
		ObservationSearchParams searchParams = new ObservationSearchParams();
		searchParams.setPatient(referenceParam);
		
		when(observationService.getLastnObservations(max, searchParams))
		        .thenReturn(new MockIBundleProvider<>(Collections.singletonList(observation), 10, 1));
		
		IBundleProvider results = resourceProvider.getLastnObservations(max, referenceParam, null, null, null);
		
		List<IBaseResource> resultList = get(results);
		
		assertThat(results, notNullValue());
		assertThat(resultList, hasSize(equalTo(1)));
		assertThat(resultList.get(0), notNullValue());
		assertThat(resultList.get(0).fhirType(), equalTo(FhirConstants.OBSERVATION));
		assertThat(resultList.get(0).getIdElement().getIdPart(), equalTo(OBSERVATION_UUID));
	}
	
	@Test
	public void getLastnEncounters_shouldReturnRecentNEncountersObservations() {
		NumberParam max = new NumberParam(2);
		ReferenceAndListParam referenceParam = new ReferenceAndListParam();
		ReferenceParam patient = new ReferenceParam();
		
		patient.setValue(PATIENT_UUID);
		
		referenceParam.addValue(new ReferenceOrListParam().add(patient));
		
		TokenAndListParam categories = new TokenAndListParam().addAnd(new TokenParam().setValue("laboratory"));
		
		TokenAndListParam code = new TokenAndListParam().addAnd(
		    new TokenParam().setSystem(FhirTestConstants.LOINC_SYSTEM_URL).setValue(LOINC_SYSTOLIC_BP),
		    new TokenParam().setSystem(FhirTestConstants.CIEL_SYSTEM_URN).setValue(CIEL_DIASTOLIC_BP));
		
		ObservationSearchParams searchParams = new ObservationSearchParams();
		searchParams.setPatient(referenceParam);
		searchParams.setCategory(categories);
		searchParams.setCode(code);
		
		when(observationService.getLastnEncountersObservations(max, searchParams))
		        .thenReturn(new MockIBundleProvider<>(Collections.singletonList(observation), 10, 1));
		
		IBundleProvider results = resourceProvider.getLastnEncountersObservations(max, referenceParam, null, categories,
		    code);
		
		List<IBaseResource> resultList = get(results);
		
		assertThat(results, notNullValue());
		assertThat(resultList, hasSize(equalTo(1)));
		assertThat(resultList.get(0), notNullValue());
		assertThat(resultList.get(0).fhirType(), equalTo(FhirConstants.OBSERVATION));
		assertThat(resultList.get(0).getIdElement().getIdPart(), equalTo(OBSERVATION_UUID));
		
	}
	
	@Test
	public void getLastnEncounters_shouldReturnFirstRecentEncountersObservationsWhenMaxIsMissing() {
		
		ReferenceAndListParam referenceParam = new ReferenceAndListParam();
		ReferenceParam patient = new ReferenceParam();
		
		patient.setValue(PATIENT_UUID);
		
		referenceParam.addValue(new ReferenceOrListParam().add(patient));
		
		TokenAndListParam categories = new TokenAndListParam().addAnd(new TokenParam().setValue("laboratory"));
		
		TokenAndListParam code = new TokenAndListParam().addAnd(
		    new TokenParam().setSystem(FhirTestConstants.LOINC_SYSTEM_URL).setValue(LOINC_SYSTOLIC_BP),
		    new TokenParam().setSystem(FhirTestConstants.CIEL_SYSTEM_URN).setValue(CIEL_DIASTOLIC_BP));
		
		ObservationSearchParams searchParams = new ObservationSearchParams();
		searchParams.setPatient(referenceParam);
		searchParams.setCategory(categories);
		searchParams.setCode(code);
		
		when(observationService.getLastnEncountersObservations(null, searchParams))
		        .thenReturn(new MockIBundleProvider<>(Collections.singletonList(observation), 10, 1));
		
		IBundleProvider results = resourceProvider.getLastnEncountersObservations(null, referenceParam, null, categories,
		    code);
		
		List<IBaseResource> resultList = get(results);
		
		assertThat(results, notNullValue());
		assertThat(resultList, hasSize(equalTo(1)));
		assertThat(resultList.get(0), notNullValue());
		assertThat(resultList.get(0).fhirType(), equalTo(FhirConstants.OBSERVATION));
		assertThat(resultList.get(0).getIdElement().getIdPart(), equalTo(OBSERVATION_UUID));
	}
	
	@Test
	public void getLastnEncountersObservations_shouldReturnRecentNEncountersObservationsWhenPatientReferenceIsPassedInPatientParameter() {
		NumberParam max = new NumberParam(2);
		ReferenceAndListParam referenceParam = new ReferenceAndListParam();
		ReferenceParam patient = new ReferenceParam();
		
		patient.setValue(PATIENT_UUID);
		
		referenceParam.addValue(new ReferenceOrListParam().add(patient));
		
		TokenAndListParam categories = new TokenAndListParam().addAnd(new TokenParam().setValue("laboratory"));
		
		TokenAndListParam code = new TokenAndListParam().addAnd(
		    new TokenParam().setSystem(FhirTestConstants.LOINC_SYSTEM_URL).setValue(LOINC_SYSTOLIC_BP),
		    new TokenParam().setSystem(FhirTestConstants.CIEL_SYSTEM_URN).setValue(CIEL_DIASTOLIC_BP));
		
		ObservationSearchParams searchParams = new ObservationSearchParams();
		searchParams.setPatient(referenceParam);
		searchParams.setCategory(categories);
		searchParams.setCode(code);
		
		when(observationService.getLastnEncountersObservations(max, searchParams))
		        .thenReturn(new MockIBundleProvider<>(Collections.singletonList(observation), 10, 1));
		
		IBundleProvider results = resourceProvider.getLastnEncountersObservations(max, null, referenceParam, categories,
		    code);
		
		List<IBaseResource> resultList = get(results);
		
		assertThat(results, notNullValue());
		assertThat(resultList, hasSize(equalTo(1)));
		assertThat(resultList.get(0), notNullValue());
		assertThat(resultList.get(0).fhirType(), equalTo(FhirConstants.OBSERVATION));
		assertThat(resultList.get(0).getIdElement().getIdPart(), equalTo(OBSERVATION_UUID));
	}
	
	@Test
	public void getLastnEncountersObservations_shouldReturnRecentNEncountersObservationsWhenPatientReferenceIsNotGiven() {
		NumberParam max = new NumberParam(2);
		
		TokenAndListParam categories = new TokenAndListParam().addAnd(new TokenParam().setValue("laboratory"));
		
		TokenAndListParam code = new TokenAndListParam().addAnd(
		    new TokenParam().setSystem(FhirTestConstants.LOINC_SYSTEM_URL).setValue(LOINC_SYSTOLIC_BP),
		    new TokenParam().setSystem(FhirTestConstants.CIEL_SYSTEM_URN).setValue(CIEL_DIASTOLIC_BP));
		
		ObservationSearchParams searchParams = new ObservationSearchParams();
		searchParams.setCategory(categories);
		searchParams.setCode(code);
		
		when(observationService.getLastnEncountersObservations(max, searchParams))
		        .thenReturn(new MockIBundleProvider<>(Collections.singletonList(observation), 10, 1));
		
		IBundleProvider results = resourceProvider.getLastnEncountersObservations(max, null, null, categories, code);
		
		List<IBaseResource> resultList = get(results);
		
		assertThat(results, notNullValue());
		assertThat(resultList, hasSize(equalTo(1)));
		assertThat(resultList.get(0), notNullValue());
		assertThat(resultList.get(0).fhirType(), equalTo(FhirConstants.OBSERVATION));
		assertThat(resultList.get(0).getIdElement().getIdPart(), equalTo(OBSERVATION_UUID));
	}
	
	@Test
	public void getLastnEncountersObservations_shouldReturnRecentNEncountersObservationsWhenNoCategoryIsSpecified() {
		NumberParam max = new NumberParam(2);
		ReferenceAndListParam referenceParam = new ReferenceAndListParam();
		ReferenceParam patient = new ReferenceParam();
		
		patient.setValue(PATIENT_UUID);
		
		referenceParam.addValue(new ReferenceOrListParam().add(patient));
		
		TokenAndListParam code = new TokenAndListParam().addAnd(
		    new TokenParam().setSystem(FhirTestConstants.LOINC_SYSTEM_URL).setValue(LOINC_SYSTOLIC_BP),
		    new TokenParam().setSystem(FhirTestConstants.CIEL_SYSTEM_URN).setValue(CIEL_DIASTOLIC_BP));
		
		ObservationSearchParams searchParams = new ObservationSearchParams();
		searchParams.setPatient(referenceParam);
		searchParams.setCode(code);
		
		when(observationService.getLastnEncountersObservations(max, searchParams))
		        .thenReturn(new MockIBundleProvider<>(Collections.singletonList(observation), 10, 1));
		
		IBundleProvider results = resourceProvider.getLastnEncountersObservations(max, referenceParam, null, null, code);
		
		List<IBaseResource> resultList = get(results);
		
		assertThat(results, notNullValue());
		assertThat(resultList, hasSize(equalTo(1)));
		assertThat(resultList.get(0), notNullValue());
		assertThat(resultList.get(0).fhirType(), equalTo(FhirConstants.OBSERVATION));
		assertThat(resultList.get(0).getIdElement().getIdPart(), equalTo(OBSERVATION_UUID));
	}
	
	@Test
	public void getLastnEncountersObservations_shouldReturnRecentNEncountersObservationsWhenNoCodeIsSpecified() {
		NumberParam max = new NumberParam(2);
		ReferenceAndListParam referenceParam = new ReferenceAndListParam();
		ReferenceParam patient = new ReferenceParam();
		
		patient.setValue(PATIENT_UUID);
		
		referenceParam.addValue(new ReferenceOrListParam().add(patient));
		
		TokenAndListParam categories = new TokenAndListParam().addAnd(new TokenParam().setValue("laboratory"));
		
		ObservationSearchParams searchParams = new ObservationSearchParams();
		searchParams.setPatient(referenceParam);
		searchParams.setCategory(categories);
		
		when(observationService.getLastnEncountersObservations(max, searchParams))
		        .thenReturn(new MockIBundleProvider<>(Collections.singletonList(observation), 10, 1));
		
		IBundleProvider results = resourceProvider.getLastnEncountersObservations(max, referenceParam, null, categories,
		    null);
		
		List<IBaseResource> resultList = get(results);
		
		assertThat(results, notNullValue());
		assertThat(resultList, hasSize(equalTo(1)));
		assertThat(resultList.get(0), notNullValue());
		assertThat(resultList.get(0).fhirType(), equalTo(FhirConstants.OBSERVATION));
		assertThat(resultList.get(0).getIdElement().getIdPart(), equalTo(OBSERVATION_UUID));
	}
	
	@Test
	public void getLastnEncountersObservations_shouldReturnRecentNEncountersObservationsWhenBothCodeAndCategoryIsNotSpecified() {
		NumberParam max = new NumberParam(2);
		ReferenceAndListParam referenceParam = new ReferenceAndListParam();
		ReferenceParam patient = new ReferenceParam();
		
		patient.setValue(PATIENT_UUID);
		
		referenceParam.addValue(new ReferenceOrListParam().add(patient));
		
		ObservationSearchParams searchParams = new ObservationSearchParams();
		searchParams.setPatient(referenceParam);
		
		when(observationService.getLastnEncountersObservations(max, searchParams))
		        .thenReturn(new MockIBundleProvider<>(Collections.singletonList(observation), 10, 1));
		
		IBundleProvider results = resourceProvider.getLastnEncountersObservations(max, referenceParam, null, null, null);
		
		List<IBaseResource> resultList = get(results);
		
		assertThat(results, notNullValue());
		assertThat(resultList, hasSize(equalTo(1)));
		assertThat(resultList.get(0), notNullValue());
		assertThat(resultList.get(0).fhirType(), equalTo(FhirConstants.OBSERVATION));
		assertThat(resultList.get(0).getIdElement().getIdPart(), equalTo(OBSERVATION_UUID));
	}
}
