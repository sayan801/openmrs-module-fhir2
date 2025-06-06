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

import static lombok.AccessLevel.PROTECTED;
import static org.openmrs.module.fhir2.FhirConstants.UCUM_SYSTEM_URI;

import javax.annotation.Nonnull;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import org.fhir.ucum.UcumEssenceService;
import org.fhir.ucum.UcumService;
import org.hl7.fhir.r4.model.Coding;
import org.openmrs.Concept;
import org.openmrs.ConceptNumeric;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Coding Translator implementation mapping an Observation NumericConcept to/from a FHIR
 * {@link Coding}. We prioritize a UCUM coding system and use the FHIR UCUM parsing library to
 * determine whether the NumericConcept conforms to the UCUM standard. If UCUM validation fails we
 * fall back to the concept UUID and a null system.
 */
@Component
public class ObservationQuantityCodingTranslatorImpl extends BaseCodingTranslator {
	
	@Getter(PROTECTED)
	@Setter(value = PROTECTED, onMethod_ = @Autowired)
	private List<UcumEssenceService> ucumServices;
	
	/**
	 * @param concept the openMRS {@link Concept} to translate
	 * @return UCUM Coding object if Concept provides valid UCUM unit, null if no ucum or uuid system
	 *         found
	 */
	@Override
	public Coding toFhirResource(@Nonnull Concept concept) {
		if (concept == null || !(concept instanceof ConceptNumeric)) {
			return null;
		}
		
		ConceptNumeric conceptNumeric = (ConceptNumeric) concept;
		Coding coding = null;
		
		for (UcumService ucumService : ucumServices) {
			String code = conceptNumeric.getUnits();
			if (code != null && ucumService.validate(code) == null) {
				coding = new Coding(UCUM_SYSTEM_URI, code, ucumService.getCommonDisplay(code));
				break;
			}
		}
		
		// attempt to fall back to the concept uuid with a null coding concept
		if (coding == null) {
			coding = getCodingForSystem(getConceptTranslator().toFhirResource(concept), null);
		}
		
		return coding;
		
	}
}
