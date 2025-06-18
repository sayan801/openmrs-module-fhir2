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

import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

import lombok.AccessLevel;
import lombok.Setter;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Meta;
import org.openmrs.Visit;
import org.openmrs.module.fhir2.api.translators.VisitMetaSecurityTranslator;
import org.springframework.stereotype.Component;

@Component
@Setter(AccessLevel.PACKAGE)
public class VisitMetaSecurityTranslatorImpl implements VisitMetaSecurityTranslator<Visit> {
	
	@Override
	public Meta toFhirResource(@Nonnull Visit visit) {
		
		Meta result = new Meta();
		
		List<Coding> securityTags = new ArrayList<>();
		
		// set fhir meta.security from encounter- visit- attribute ==============================		
		
		visit.getAttributes().forEach(attr -> {
			String attrTypeName = attr.getAttributeType().getName();
			if ("security".equalsIgnoreCase(attrTypeName)) {
				Coding coding = new Coding().setSystem("http://example.org/security-tags").setCode(attr.getUuid())
				        .setDisplay(attrTypeName + ": " + attr.getValueReference());
				securityTags.add(coding);
			}
		});
		
		result.setSecurity(securityTags);
		return result;
		
	}
	
	@Override
	public Visit toOpenmrsType(@Nonnull Visit visit, @Nonnull List<Coding> codings) {
		// Example: Store security tags in encounter attributes or extensions (customized storage)
		// OpenMRS doesn't support meta.security natively, so you must store this in an extension or obs
		
		for (Coding coding : codings) {
			
			System.out.printf("Received security tag: system=%s, code=%s, display=%s%n", coding.getSystem(),
			    coding.getCode(), coding.getDisplay());
		}
		
		return visit;
		
	}
}
