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
import static org.openmrs.module.fhir2.api.translators.impl.ReferenceHandlingTranslator.createObservationReference;
import static org.openmrs.module.fhir2.api.translators.impl.ReferenceHandlingTranslator.getReferenceId;
import static org.openmrs.module.fhir2.api.translators.impl.ReferenceHandlingTranslator.getReferenceType;

import javax.annotation.Nonnull;

import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.Reference;
import org.openmrs.Obs;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.dao.FhirObservationDao;
import org.openmrs.module.fhir2.api.translators.ObservationReferenceTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ObservationReferenceTranslatorImpl implements ObservationReferenceTranslator {
	
	@Getter(PROTECTED)
	@Setter(value = PROTECTED, onMethod_ = @Autowired)
	private FhirObservationDao observationDao;
	
	@Override
	public Reference toFhirResource(@Nonnull Obs obs) {
		if (obs == null || obs.getVoided()) {
			return null;
		}
		
		return createObservationReference(obs);
	}
	
	@Override
	public Obs toOpenmrsType(@Nonnull Reference obsReference) {
		if (obsReference == null || !obsReference.hasReference()) {
			return null;
		}
		
		if (getReferenceType(obsReference).map(ref -> !ref.equals(FhirConstants.OBSERVATION)).orElse(true)) {
			throw new IllegalArgumentException(
			        "Reference must be to an Observation not a " + getReferenceType(obsReference).orElse(""));
		}
		
		return getReferenceId(obsReference).map(uuid -> observationDao.get(uuid)).orElse(null);
	}
}
