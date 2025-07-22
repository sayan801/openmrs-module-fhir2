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
import org.openmrs.VisitAttribute;
import org.openmrs.VisitAttributeType;
import org.openmrs.api.context.Context;
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
				Object value = attr.getValue();
				String displayValue = value != null ? value.toString() : "Restricted";
				Coding coding = new Coding().setSystem("http://terminology.hl7.org/CodeSystem/v3-Confidentiality")
				        .setCode(attr.getUuid()).setDisplay(attrTypeName + ": " + displayValue);
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
				attribute.setUuid(coding.getCode());
				
				// Extract dynamic value from display
				String[] parts = display.split(":", 2);
				if (parts.length == 2) {
					String value = parts[1].trim();
					
					// Set both value and valueReferenceInternal dynamically
					attribute.setValue(value);
					attribute.setValueReferenceInternal(value);
				} else {
					// fallback if display is malformed
					attribute.setValue("Unknown");
					attribute.setValueReferenceInternal("Unknown");
				}
				
				// Add the attribute to the visit
				visit.addAttribute(attribute);
				Context.getVisitService().saveVisit(visit);
				
				System.out.printf("Added visit attribute: uuid=%s, display=%s%n", coding.getCode(), display);
			}
		}
		
		return visit;
	}
	
}
