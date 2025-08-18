/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.fhir2.api.translators.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.ArrayList;
import java.util.Collections;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Meta;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Visit;
import org.openmrs.VisitAttribute;
import org.openmrs.VisitAttributeType;
import org.openmrs.api.VisitService;

@RunWith(MockitoJUnitRunner.class)
public class VisitMetaSecurityTranslatorImplTest {
	
	@Mock
	private VisitService visitService;
	
	@InjectMocks
	private VisitMetaSecurityTranslatorImpl translator;
	
	@Before
	public void setup() {
		// Additional setup if needed
	}
	
	@Test
	public void toFhirResource_shouldConvertSecurityAttributeToMetaSecurity() {
		// Given
		String securityDisplay = "restricted";
		String expectedCode = "R"; // Based on your getSecurityCodeFromDisplay method
		
		Visit visit = new Visit();
		VisitAttributeType attrType = new VisitAttributeType();
		attrType.setName("security");
		
		VisitAttribute attr = new VisitAttribute();
		attr.setAttributeType(attrType);
		attr.setValue(securityDisplay);
		attr.setUuid("sec-attr-uuid");
		
		visit.setAttributes(Collections.singleton(attr));
		
		// When
		Meta result = translator.toFhirResource(visit);
		
		// Then
		assertThat(result, notNullValue());
		assertThat(result.getSecurity(), hasSize(1));
		Coding coding = result.getSecurityFirstRep();
		assertThat(coding.getSystem(), equalTo("http://terminology.hl7.org/CodeSystem/v3-Confidentiality"));
		assertThat(coding.getCode(), equalTo(expectedCode));
		assertThat(coding.getDisplay(), equalTo(securityDisplay));
	}
	
	@Test
	public void toFhirResource_shouldHandleColonSeparatedValues() {
		// Given
		String securityValue = "confidentiality:moderate";
		String expectedDisplay = "moderate";
		String expectedCode = "M";
		
		Visit visit = new Visit();
		VisitAttributeType attrType = new VisitAttributeType();
		attrType.setName("security");
		
		VisitAttribute attr = new VisitAttribute();
		attr.setAttributeType(attrType);
		attr.setValue(securityValue);
		
		visit.setAttributes(Collections.singleton(attr));
		
		// When
		Meta result = translator.toFhirResource(visit);
		
		// Then
		assertThat(result, notNullValue());
		assertThat(result.getSecurity(), hasSize(1));
		Coding coding = result.getSecurityFirstRep();
		assertThat(coding.getCode(), equalTo(expectedCode));
		assertThat(coding.getDisplay(), equalTo(expectedDisplay));
	}
	
	@Test
	public void toFhirResource_shouldHandleNullValueWithDefault() {
		// Given
		Visit visit = new Visit();
		VisitAttributeType attrType = new VisitAttributeType();
		attrType.setName("security");
		
		VisitAttribute attr = new VisitAttribute();
		attr.setAttributeType(attrType);
		attr.setValue(null); // null value
		
		visit.setAttributes(Collections.singleton(attr));
		
		// When
		Meta result = translator.toFhirResource(visit);
		
		// Then
		assertThat(result, notNullValue());
		assertThat(result.getSecurity(), hasSize(1));
		Coding coding = result.getSecurityFirstRep();
		assertThat(coding.getCode(), equalTo("R")); // Default for "Restricted"
		assertThat(coding.getDisplay(), equalTo("Restricted")); // Default display
	}
	
	@Test
	public void toFhirResource_shouldReturnEmptyMetaWhenNoSecurityAttributesPresent() {
		// Given
		Visit visit = new Visit();
		
		// Add non-security attribute
		VisitAttributeType nonSecurityType = new VisitAttributeType();
		nonSecurityType.setName("location");
		
		VisitAttribute nonSecurityAttr = new VisitAttribute();
		nonSecurityAttr.setAttributeType(nonSecurityType);
		nonSecurityAttr.setValue("ward-a");
		
		visit.setAttributes(Collections.singleton(nonSecurityAttr));
		
		// When
		Meta result = translator.toFhirResource(visit);
		
		// Then
		assertThat(result, notNullValue());
		assertThat(result.getSecurity(), is(empty()));
	}
	
	@Test
	public void toFhirResource_shouldTestAllSecurityCodes() {
		Visit visit = new Visit();
		VisitAttributeType attrType = new VisitAttributeType();
		attrType.setName("security");
		
		// Test different security levels
		String[][] testCases = { { "low", "L" }, { "moderate", "M" }, { "normal", "N" }, { "restricted", "R" },
		        { "unrestricted", "U" }, { "very restricted", "V" }, { "unknown", "R" } // Default case
		};
		
		for (String[] testCase : testCases) {
			String display = testCase[0];
			String expectedCode = testCase[1];
			
			VisitAttribute attr = new VisitAttribute();
			attr.setAttributeType(attrType);
			attr.setValue(display);
			
			visit.setAttributes(Collections.singleton(attr));
			
			Meta result = translator.toFhirResource(visit);
			
			assertThat("Failed for display: " + display, result.getSecurity(), hasSize(1));
			assertThat("Failed for display: " + display, result.getSecurityFirstRep().getCode(), equalTo(expectedCode));
			assertThat("Failed for display: " + display, result.getSecurityFirstRep().getDisplay(), equalTo(display));
		}
	}
	
	// NOTE: The following tests are commented out because they require static mocking of Context.getVisitService()
	// To run these tests, you need either PowerMock (see the other version) or Mockito 3.4.0+ with mockito-inline
	
	/*
	@Test
	public void toOpenmrsType_shouldConvertMetaSecurityToVisitAttribute() {
		// This test requires mocking Context.getVisitService() which needs PowerMock or newer Mockito
	}
	
	@Test
	public void toOpenmrsType_shouldIgnoreNonSecurityCodings() {
		// This test requires mocking Context.getVisitService() which needs PowerMock or newer Mockito
	}
	*/
	
	@Test
	public void toOpenmrsType_shouldHandleNullParameters() {
		// Test null visit
		Visit result1 = translator.toOpenmrsType(null, Collections.emptyList());
		assertThat(result1, is(equalTo(null)));
		
		// Test null codings
		Visit visit = new Visit();
		Visit result2 = translator.toOpenmrsType(visit, null);
		assertThat(result2, is(equalTo(null)));
	}
	
	@Test
	public void toOpenmrsType_shouldReturnVisitUnchangedWhenCodingsIsEmpty() {
		// Given
		Visit visit = new Visit();
		
		// When
		Visit result = translator.toOpenmrsType(visit, new ArrayList<>());
		
		// Then
		assertThat(result, is(visit));
		assertThat(result.getAttributes(), is(empty()));
	}
}
