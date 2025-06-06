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
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import com.google.common.collect.Sets;
import org.exparity.hamcrest.date.DateMatchers;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.StringType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.BaseOpenmrsData;
import org.openmrs.PatientIdentifier;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.PersonName;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.FhirGlobalPropertyService;
import org.openmrs.module.fhir2.api.dao.FhirPersonDao;
import org.openmrs.module.fhir2.api.translators.BirthDateTranslator;
import org.openmrs.module.fhir2.api.translators.GenderTranslator;
import org.openmrs.module.fhir2.api.translators.PatientIdentifierTranslator;
import org.openmrs.module.fhir2.api.translators.PersonAddressTranslator;
import org.openmrs.module.fhir2.api.translators.PersonAttributeTranslator;
import org.openmrs.module.fhir2.api.translators.PersonNameTranslator;
import org.openmrs.module.fhir2.api.translators.TelecomTranslator;

@RunWith(MockitoJUnitRunner.class)
public class PatientTranslatorImplTest {
	
	private static final String PATIENT_UUID = "123456-abcdef-123456";
	
	private static final String PATIENT_IDENTIFIER_UUID = "654321-fedcba-654321";
	
	private static final String PATIENT_NAME_UUID = "1fdb5469-57c7-435f-b009-3dcceb23b0a2";
	
	private static final String PATIENT_GIVEN_NAME = "Jean Claude";
	
	private static final String PATIENT_FAMILY_NAME = "van Damme";
	
	private static final String ADDRESS_UUID = "135791-acegik-135791";
	
	private static final String ADDRESS_CITY = "Maputo";
	
	private static final String PERSON_ATTRIBUTE_TYPE_NAME = "Contact";
	
	private static final String PERSON_ATTRIBUTE_TYPE_UUID = "14d4f066-15f5-102d-96e4-000c29c2a5d7";
	
	private static final String PERSON_ATTRIBUTE_UUID = "12o3et5kl3-2e323-23g23-232h3y343s";
	
	private static final String PERSON_ATTRIBUTE_VALUE = "254723723456";
	
	private static final Date PATIENT_DEATH_DATE = Date.from(Instant.ofEpochSecond(872986980L));
	
	@Mock
	private PatientIdentifierTranslator identifierTranslator;
	
	@Mock
	private PersonNameTranslator nameTranslator;
	
	@Mock
	private GenderTranslator genderTranslator;
	
	@Mock
	private PersonAddressTranslator addressTranslator;
	
	@Mock
	private FhirPersonDao fhirPersonDao;
	
	@Mock
	private TelecomTranslator<BaseOpenmrsData> telecomTranslator;
	
	@Mock
	private FhirGlobalPropertyService globalPropertyService;
	
	@Mock
	private PersonAttributeTranslator personAttributeTranslator;
	
	private BirthDateTranslator birthDateTranslator = new BirthDateTranslatorImpl();
	
	private PatientTranslatorImpl patientTranslator;
	
	@Before
	public void setup() {
		patientTranslator = new PatientTranslatorImpl();
		patientTranslator.setIdentifierTranslator(identifierTranslator);
		patientTranslator.setNameTranslator(nameTranslator);
		patientTranslator.setGenderTranslator(genderTranslator);
		patientTranslator.setAddressTranslator(addressTranslator);
		patientTranslator.setTelecomTranslator(telecomTranslator);
		patientTranslator.setFhirPersonDao(fhirPersonDao);
		patientTranslator.setGlobalPropertyService(globalPropertyService);
		patientTranslator.setBirthDateTranslator(birthDateTranslator);
		patientTranslator.setPersonAttributeTranslator(personAttributeTranslator);
	}
	
	@Test
	public void shouldTranslateOpenmrsPatientToFhirPatient() {
		org.openmrs.Patient patient = new org.openmrs.Patient();
		Patient result = patientTranslator.toFhirResource(patient);
		assertThat(result, notNullValue());
	}
	
	@Test
	public void shouldTranslateOpenmrsPatientWithPersonAttributeToFhirPatient() {
		org.openmrs.Patient patient = new org.openmrs.Patient();
		PersonAttributeType personAttributeType = new PersonAttributeType();
		personAttributeType.setFormat("java.lang.String");
		
		PersonAttribute personAttribute = new PersonAttribute();
		personAttribute.setUuid(PERSON_ATTRIBUTE_UUID);
		personAttribute.setValue("RANDOM_VALUE");
		personAttribute.setAttributeType(personAttributeType);
		patient.addAttribute(personAttribute);
		
		when(personAttributeTranslator.toFhirResource(personAttribute)).thenReturn(new Extension());
		
		Patient result = patientTranslator.toFhirResource(patient);
		assertThat(result, notNullValue());
		assertThat(result.getExtension(), notNullValue());
		assertThat(result.getExtension(), hasSize(1));
	}
	
	@Test
	public void shouldTranslateOpenmrsPatientWithInvalidPersonAttributeToFhirPatient() {
		org.openmrs.Patient patient = new org.openmrs.Patient();
		PersonAttributeType personAttributeType = new PersonAttributeType();
		personAttributeType.setFormat("Unsupported_type");
		
		PersonAttribute personAttribute = new PersonAttribute();
		personAttribute.setUuid(PERSON_ATTRIBUTE_UUID);
		personAttribute.setValue("RANDOM_VALUE");
		personAttribute.setAttributeType(personAttributeType);
		
		patient.addAttribute(personAttribute);
		
		when(personAttributeTranslator.toFhirResource(personAttribute)).thenReturn(null);
		
		Patient result = patientTranslator.toFhirResource(patient);
		assertThat(result, notNullValue());
		assertThat(result.getExtension(), notNullValue());
		assertThat(result.getExtension(), hasSize(0));
	}
	
	@Test
	public void shouldTranslatePatientUuidToFhirIdType() {
		org.openmrs.Patient patient = new org.openmrs.Patient();
		patient.setUuid(PATIENT_UUID);
		
		Patient result = patientTranslator.toFhirResource(patient);
		assertThat(result.getId(), equalTo(PATIENT_UUID));
	}
	
	@Test
	public void shouldTranslateAlivePatientToAlive() {
		org.openmrs.Patient patient = new org.openmrs.Patient();
		patient.setDead(false);
		
		Patient result = patientTranslator.toFhirResource(patient);
		assertThat(result.getDeceased(), notNullValue());
		assertThat(result.getDeceasedBooleanType(), notNullValue());
		assertThat(result.getDeceasedBooleanType().booleanValue(), is(false));
	}
	
	@Test
	public void shouldTranslateDeadPatientWithoutDeathDateToDead() {
		org.openmrs.Patient patient = new org.openmrs.Patient();
		patient.setDead(true);
		
		Patient result = patientTranslator.toFhirResource(patient);
		assertThat(result.getDeceased(), notNullValue());
		assertThat(result.getDeceasedBooleanType(), notNullValue());
		assertThat(result.getDeceasedBooleanType().booleanValue(), is(true));
	}
	
	@Test
	public void shouldTranslateDeadPatientWithDeathDateToPatientWithDeathDate() {
		org.openmrs.Patient patient = new org.openmrs.Patient();
		patient.setDead(true);
		patient.setDeathDate(PATIENT_DEATH_DATE);
		
		Patient result = patientTranslator.toFhirResource(patient);
		assertThat(result.getDeceased(), notNullValue());
		assertThat(result.getDeceasedDateTimeType(), notNullValue());
		assertThat(result.getDeceasedDateTimeType().getValue(), equalTo(PATIENT_DEATH_DATE));
	}
	
	@Test
	public void shouldTranslatePatientIdentifierToFhirIdentifier() {
		Identifier id = new Identifier();
		id.setValue(PATIENT_IDENTIFIER_UUID);
		when(identifierTranslator.toFhirResource(argThat(hasProperty("uuid", equalTo(PATIENT_IDENTIFIER_UUID)))))
		        .thenReturn(id);
		
		org.openmrs.Patient patient = new org.openmrs.Patient();
		PatientIdentifier patientIdentifier = new PatientIdentifier();
		patientIdentifier.setUuid(PATIENT_IDENTIFIER_UUID);
		patient.addIdentifier(patientIdentifier);
		
		Patient result = patientTranslator.toFhirResource(patient);
		assertThat(result.getIdentifier(), not(empty()));
		assertThat(result.getIdentifier(), hasItem(hasProperty("value", equalTo(PATIENT_IDENTIFIER_UUID))));
	}
	
	@Test
	public void shouldTranslateOpenmrsPatientNameToFhirPatientName() {
		HumanName humanName = new HumanName();
		humanName.addGiven(PATIENT_GIVEN_NAME);
		humanName.setFamily(PATIENT_FAMILY_NAME);
		when(nameTranslator.toFhirResource(argThat(allOf(hasProperty("givenName", equalTo(PATIENT_GIVEN_NAME)),
		    hasProperty("familyName", equalTo(PATIENT_FAMILY_NAME)))))).thenReturn(humanName);
		
		org.openmrs.Patient patient = new org.openmrs.Patient();
		PersonName name = new PersonName();
		name.setGivenName(PATIENT_GIVEN_NAME);
		name.setFamilyName(PATIENT_FAMILY_NAME);
		patient.setNames(Sets.newHashSet(name));
		
		Patient result = patientTranslator.toFhirResource(patient);
		assertThat(result.getName(), not(empty()));
		assertThat(result.getName().get(0), notNullValue());
		assertThat(result.getName().get(0).getGivenAsSingleString(), equalTo(PATIENT_GIVEN_NAME));
		assertThat(result.getName().get(0).getFamily(), equalTo(PATIENT_FAMILY_NAME));
	}
	
	@Test
	public void shouldTranslateOpenMRSPatientGenderToFhirGender() {
		when(genderTranslator.toFhirResource(argThat(equalTo("M")))).thenReturn(Enumerations.AdministrativeGender.MALE);
		
		org.openmrs.Patient patient = new org.openmrs.Patient();
		patient.setGender("M");
		
		Patient result = patientTranslator.toFhirResource(patient);
		assertThat(result.getGender(), equalTo(Enumerations.AdministrativeGender.MALE));
	}
	
	@Test
	public void shouldTranslateOpenMRSAddressToFhirAddress() {
		Address address = new Address();
		address.setId(ADDRESS_UUID);
		address.setCity(ADDRESS_CITY);
		when(addressTranslator.toFhirResource(argThat(hasProperty("uuid", equalTo(ADDRESS_UUID))))).thenReturn(address);
		
		org.openmrs.Patient patient = new org.openmrs.Patient();
		PersonAddress personAddress = new PersonAddress();
		personAddress.setUuid(ADDRESS_UUID);
		personAddress.setCityVillage(ADDRESS_CITY);
		patient.addAddress(personAddress);
		
		Patient result = patientTranslator.toFhirResource(patient);
		assertThat(result.getAddress(), not(empty()));
		assertThat(result.getAddress(), hasItem(hasProperty("id", equalTo(ADDRESS_UUID))));
		assertThat(result.getAddress(), hasItem(hasProperty("city", equalTo(ADDRESS_CITY))));
	}
	
	@Test
	public void shouldTranslateFhirPatientToOpenmrsPatient() {
		Patient patient = new Patient();
		org.openmrs.Patient result = patientTranslator.toOpenmrsType(patient);
		assertThat(result, notNullValue());
	}
	
	@Test
	public void shouldTranslateFhirPatientWithPersonAttributeExtensionToOpenmrsPatient() {
		Patient patient = new Patient();
		Extension personAttributeTypeExtension = new Extension();
		personAttributeTypeExtension.setUrl(FhirConstants.OPENMRS_FHIR_EXT_PERSON_ATTRIBUTE_TYPE);
		personAttributeTypeExtension.setValue(new StringType("ATTRIBUTE_TYPE_NAME"));
		
		Extension personAttributeValueExtension = new Extension();
		personAttributeValueExtension.setUrl(FhirConstants.OPENMRS_FHIR_EXT_PERSON_ATTRIBUTE_VALUE);
		personAttributeValueExtension.setValue(new StringType("STRING_ATTRIBUTE_VALUE"));
		
		Extension extension = new Extension();
		extension.setUrl(FhirConstants.OPENMRS_FHIR_EXT_PERSON_ATTRIBUTE);
		extension.addExtension(personAttributeTypeExtension);
		extension.addExtension(personAttributeValueExtension);
		
		patient.addExtension(extension);
		
		PersonAttributeType personAttributeType = new PersonAttributeType();
		personAttributeType.setFormat("java.lang.String");
		
		PersonAttribute personAttribute = new PersonAttribute();
		personAttribute.setUuid(PERSON_ATTRIBUTE_UUID);
		personAttribute.setValue("RANDOM_VALUE");
		personAttribute.setAttributeType(personAttributeType);
		
		when(personAttributeTranslator.toOpenmrsType(extension)).thenReturn(personAttribute);
		
		org.openmrs.Patient result = patientTranslator.toOpenmrsType(patient);
		
		assertThat(result, notNullValue());
		assertThat(result.getAttributes(), notNullValue());
		assertThat(result.getAttributes(), hasSize(1));
	}
	
	@Test
	public void shouldTranslateFhirPatientWithInvalidPersonAttributeExtensionToOpenmrsPatient() {
		Patient patient = new Patient();
		
		Extension extension = new Extension();
		extension.setUrl(FhirConstants.OPENMRS_FHIR_EXT_PERSON_ATTRIBUTE);
		
		patient.addExtension(extension);
		
		when(personAttributeTranslator.toOpenmrsType(extension)).thenReturn(null);
		
		org.openmrs.Patient result = patientTranslator.toOpenmrsType(patient);
		
		assertThat(result, notNullValue());
		assertThat(result.getAttributes(), notNullValue());
		assertThat(result.getAttributes(), hasSize(0));
	}
	
	@Test
	public void shouldTranslateFhirPatientIdToUuid() {
		Patient patient = new Patient();
		patient.setId(PATIENT_UUID);
		
		org.openmrs.Patient result = patientTranslator.toOpenmrsType(patient);
		assertThat(result.getUuid(), equalTo(PATIENT_UUID));
	}
	
	@Test
	public void shouldTranslateAliveFhirPatientToAlive() {
		Patient patient = new Patient();
		patient.setDeceased(new BooleanType(false));
		
		assertThat(patientTranslator.toOpenmrsType(patient).getDead(), is(false));
	}
	
	@Test
	public void shouldTranslateDeceasedPatientToDeceased() {
		Patient patient = new Patient();
		patient.setDeceased(new BooleanType(true));
		
		assertThat(patientTranslator.toOpenmrsType(patient).getDead(), is(true));
	}
	
	@Test
	public void shouldTranslateDeathDateToDeceasedPatient() {
		Patient patient = new Patient();
		patient.setDeceased(new DateTimeType(PATIENT_DEATH_DATE));
		
		org.openmrs.Patient result = patientTranslator.toOpenmrsType(patient);
		assertThat(result.getDead(), is(true));
		assertThat(result.getDeathDate(), equalTo(PATIENT_DEATH_DATE));
	}
	
	@Test
	public void shouldTranslateFhirIdentifierToPatientIdentifier() {
		PatientIdentifier patientIdentifier = new PatientIdentifier();
		patientIdentifier.setUuid(PATIENT_IDENTIFIER_UUID);
		when(identifierTranslator.toOpenmrsType(argThat(hasProperty("value", equalTo(PATIENT_IDENTIFIER_UUID)))))
		        .thenReturn(patientIdentifier);
		
		Patient patient = new Patient();
		Identifier id = new Identifier();
		id.setValue(PATIENT_IDENTIFIER_UUID);
		patient.addIdentifier(id);
		
		org.openmrs.Patient result = patientTranslator.toOpenmrsType(patient);
		assertThat(result.getPatientIdentifier(), notNullValue());
		assertThat(result.getPatientIdentifier(), hasProperty("uuid", equalTo(PATIENT_IDENTIFIER_UUID)));
	}
	
	@Test
	public void shouldTranslateFhirPatientNameToOpenmrsPatientName() {
		PersonName personName = new PersonName();
		personName.setGivenName(PATIENT_GIVEN_NAME);
		personName.setFamilyName(PATIENT_FAMILY_NAME);
		when(nameTranslator.toOpenmrsType(any(), any())).thenReturn(personName);
		
		Patient patient = new Patient();
		HumanName name = new HumanName();
		name.addGiven(PATIENT_GIVEN_NAME);
		name.setFamily(PATIENT_FAMILY_NAME);
		patient.addName(name);
		
		org.openmrs.Patient result = patientTranslator.toOpenmrsType(patient);
		assertThat(result.getGivenName(), equalTo(PATIENT_GIVEN_NAME));
		assertThat(result.getFamilyName(), equalTo(PATIENT_FAMILY_NAME));
	}
	
	@Test
	public void shouldTranslateFhirPatientNameToExistingOpenmrsPatientName() {
		PersonName personName = new PersonName();
		personName.setUuid(PATIENT_NAME_UUID);
		personName.setGivenName(PATIENT_GIVEN_NAME);
		personName.setFamilyName(PATIENT_FAMILY_NAME);
		when(nameTranslator.toOpenmrsType(any(), any())).thenReturn(personName);
		
		Patient patient = new Patient();
		HumanName name = new HumanName();
		name.setId(PATIENT_NAME_UUID);
		name.addGiven(PATIENT_GIVEN_NAME);
		name.setFamily(PATIENT_FAMILY_NAME);
		patient.addName(name);
		
		org.openmrs.Patient result = patientTranslator.toOpenmrsType(patient);
		assertThat(result.getGivenName(), equalTo(PATIENT_GIVEN_NAME));
		assertThat(result.getFamilyName(), equalTo(PATIENT_FAMILY_NAME));
		assertThat(result.getPersonName().getUuid(), equalTo(PATIENT_NAME_UUID));
	}
	
	@Test
	public void shouldTranslateFhirPatientGenderToOpenmrsGender() {
		when(genderTranslator.toOpenmrsType(Enumerations.AdministrativeGender.FEMALE)).thenReturn("F");
		
		Patient patient = new Patient();
		patient.setGender(Enumerations.AdministrativeGender.FEMALE);
		
		org.openmrs.Patient result = patientTranslator.toOpenmrsType(patient);
		assertThat(result.getGender(), equalTo("F"));
	}
	
	@Test
	public void shouldTranslateFhirAddressToOpenMRSAddress() {
		PersonAddress personAddress = new PersonAddress();
		personAddress.setUuid(ADDRESS_UUID);
		personAddress.setCityVillage(ADDRESS_CITY);
		when(addressTranslator.toOpenmrsType(argThat(hasProperty("id", equalTo(ADDRESS_UUID))))).thenReturn(personAddress);
		
		Patient patient = new Patient();
		Address address = new Address();
		address.setId(ADDRESS_UUID);
		address.setCity(ADDRESS_CITY);
		patient.addAddress(address);
		
		org.openmrs.Patient result = patientTranslator.toOpenmrsType(patient);
		assertThat(result.getAddresses(), not(empty()));
		assertThat(result.getAddresses(), hasItem(hasProperty("uuid", equalTo(ADDRESS_UUID))));
		assertThat(result.getAddresses(), hasItem(hasProperty("cityVillage", equalTo(ADDRESS_CITY))));
	}
	
	@Test
	public void shouldReturnPatientContactPointGivenOpenMrsPatient() {
		PersonAttributeType attributeType = new PersonAttributeType();
		attributeType.setName(PERSON_ATTRIBUTE_TYPE_NAME);
		attributeType.setUuid(PERSON_ATTRIBUTE_TYPE_UUID);
		PersonAttribute personAttribute = new PersonAttribute();
		personAttribute.setUuid(PERSON_ATTRIBUTE_UUID);
		personAttribute.setValue(PERSON_ATTRIBUTE_VALUE);
		personAttribute.setAttributeType(attributeType);
		
		org.openmrs.Patient patient = new org.openmrs.Patient();
		patient.addAttribute(personAttribute);
		
		List<ContactPoint> contactPoints = patientTranslator.getPatientContactDetails(patient);
		assertThat(contactPoints, notNullValue());
	}
	
	@Test
	public void shouldTranslateOpenMrsDateChangedToLastUpdatedDate() {
		org.openmrs.Patient patient = new org.openmrs.Patient();
		patient.setDateChanged(new Date());
		
		Patient result = patientTranslator.toFhirResource(patient);
		assertThat(result, notNullValue());
		assertThat(result.getMeta().getLastUpdated(), DateMatchers.sameDay(new Date()));
	}
	
	@Test
	public void shouldTranslateOpenMrsDateChangedToVersionId() {
		org.openmrs.Patient patient = new org.openmrs.Patient();
		patient.setDateChanged(new Date());
		
		Patient result = patientTranslator.toFhirResource(patient);
		
		assertThat(result, notNullValue());
		assertThat(result.getMeta().getVersionId(), notNullValue());
	}
	
	@Test
	public void shouldTranslateToOpenMRSBirthDate() {
		org.hl7.fhir.r4.model.Patient openMRSPatient = new org.hl7.fhir.r4.model.Patient();
		Date date = new Date();
		DateType dateType = new DateType();
		
		// for TemporalPrecisionEnum.DAY
		dateType.setValue(date, TemporalPrecisionEnum.DAY);
		openMRSPatient.setBirthDateElement(dateType);
		
		org.openmrs.Patient result = patientTranslator.toOpenmrsType(openMRSPatient);
		
		assertThat(result, notNullValue());
		assertThat(result.getBirthdateEstimated(), equalTo(false));
		assertThat(result.getBirthdate(), equalTo(date));
		
		// for TemporalPrecisionEnum.Month
		dateType.setValue(date, TemporalPrecisionEnum.MONTH);
		openMRSPatient.setBirthDateElement(dateType);
		
		result = patientTranslator.toOpenmrsType(openMRSPatient);
		
		assertThat(result, notNullValue());
		assertThat(result.getBirthdateEstimated(), equalTo(true));
		assertThat(result.getBirthdate(), equalTo(date));
		
		// for TemporalPrecisionEnum.YEAR
		dateType.setValue(date, TemporalPrecisionEnum.YEAR);
		openMRSPatient.setBirthDateElement(dateType);
		
		result = patientTranslator.toOpenmrsType(openMRSPatient);
		
		assertThat(result, notNullValue());
		assertThat(result.getBirthdateEstimated(), equalTo(true));
		assertThat(result.getBirthdate(), equalTo(date));
	}
	
	@Test
	public void shouldTranslateToFhirBirthDate() {
		org.openmrs.Patient patient = new org.openmrs.Patient();
		Calendar calendar = Calendar.getInstance();
		DateType dateType = new DateType();
		
		// when birthdate more than 5 year
		calendar.set(2000, Calendar.AUGUST, 12);
		patient.setBirthdateEstimated(true);
		patient.setBirthdate(calendar.getTime());
		
		Patient result = patientTranslator.toFhirResource(patient);
		
		assertThat(result, notNullValue());
		assertThat(result.getBirthDateElement().getPrecision(), equalTo(TemporalPrecisionEnum.YEAR));
		assertThat(result.getBirthDateElement().getYear(), equalTo(2000));
		
		//when birthDate less then 5 year
		Date date = new Date();
		patient.setBirthdate(date);
		dateType.setValue(date, TemporalPrecisionEnum.MONTH);
		
		result = patientTranslator.toFhirResource(patient);
		
		assertThat(result, notNullValue());
		assertThat(result.getBirthDateElement().getPrecision(), equalTo(TemporalPrecisionEnum.MONTH));
		assertThat(result.getBirthDateElement().getYear(), equalTo(dateType.getYear()));
		assertThat(result.getBirthDateElement().getMonth(), equalTo(dateType.getMonth()));
	}
}
