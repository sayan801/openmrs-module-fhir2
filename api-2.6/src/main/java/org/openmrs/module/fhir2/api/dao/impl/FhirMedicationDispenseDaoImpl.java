/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.fhir2.api.dao.impl;

import javax.annotation.Nonnull;

import java.util.Optional;

import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.openmrs.MedicationDispense;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.dao.FhirMedicationDispenseDao;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.springframework.stereotype.Component;

@Component
public class FhirMedicationDispenseDaoImpl extends BaseFhirDao<MedicationDispense> implements FhirMedicationDispenseDao<MedicationDispense> {
	
	@Override
	protected void setupSearchParams(Criteria criteria, SearchParameterMap theParams) {
		theParams.getParameters().forEach(entry -> {
			switch (entry.getKey()) {
				case FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER:
					entry.getValue()
					        .forEach(param -> handlePatientReference(criteria, (ReferenceAndListParam) param.getParam()));
					break;
				case FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER:
					entry.getValue()
					        .forEach(e -> handleEncounterReference(criteria, (ReferenceAndListParam) e.getParam(), "e"));
					break;
				case FhirConstants.MEDICATION_REQUEST_REFERENCE_SEARCH_HANDLER:
					entry.getValue()
					        .forEach(e -> handleMedicationRequestReference("drugOrder", (ReferenceAndListParam) e.getParam())
					                .ifPresent(c -> createAlias(criteria, "drugOrder", "drugOrder").add(c)));
					break;
				case FhirConstants.COMMON_SEARCH_HANDLER:
					handleCommonSearchParameters(entry.getValue()).ifPresent(criteria::add);
					break;
			}
		});
	}
	
	@Override
	protected Optional<Criterion> handleLastUpdated(DateRangeParam param) {
		return super.handleLastUpdatedImmutable(param);
	}
	
	@Override
	protected String paramToProp(@Nonnull String param) {
		return super.paramToProp(param);
	}
}
