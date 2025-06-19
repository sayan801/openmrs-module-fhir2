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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Meta;
import org.openmrs.Visit;
import org.openmrs.VisitAttribute;
import org.openmrs.VisitAttributeType;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir2.api.translators.VisitMetaSecurityTranslator;
import org.springframework.stereotype.Component;

import lombok.AccessLevel;
import lombok.Setter;

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
				        .setDisplay(attrTypeName + ": " + "Restricted");
				securityTags.add(coding);
			}
		});
		
		result.setSecurity(securityTags);
		return result;
		
	}
	
	@Override
	public Visit toOpenmrsType(@Nonnull Visit visit, @Nonnull List<Coding> codings) {
		if (visit == null || codings == null) {
			return null;
		}
		
		for (Coding coding : codings) {
			String display = coding.getDisplay();
			if (display != null && display.toLowerCase().contains("security")) {
				// Create a new VisitAttribute
				VisitAttribute attribute = new VisitAttribute();
				
				
				
				// all visit attribute types
				List<VisitAttributeType> attributeTypes = Context.getVisitService().getAllVisitAttributeTypes();
				
				// Then filter with security
				VisitAttributeType attrType = attributeTypes.stream().filter(type -> "security".equals(type.getName()))
				        .findFirst().orElse(null);
				attribute.setAttributeType(attrType);
				
				// Set UUID from coding.code
				attribute.setUuid(coding.getCode());
				
				// Set value from display (assumes "security: "Restricted"")
				String[] parts = display.split(":", 2);
				if (parts.length == 2) {
					String value = parts[1].trim();
					attribute.setValueReferenceInternal("Restricted");//value
				}
				
				// Add the attribute to the visit
				visit.addAttribute(attribute);
				
				System.out.printf("Added visit attribute: uuid=%s, display=%s%n", coding.getCode(), display);
			}
		}
		
		return visit;
	}
	
}
