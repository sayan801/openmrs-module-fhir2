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
		
		// set fhir meta.security from visit- attribute ==============================		
		
		visit.getAttributes().forEach(attr -> {
			String attrTypeName = attr.getAttributeType().getName();
			if ("security".equalsIgnoreCase(attrTypeName)) {
				Object value = attr.getValue();
				String displayValue = value != null ? value.toString() : "Restricted";
				String display = "";
				if (displayValue.contains(":")) {
					String[] parts = displayValue.split(":", 2);
					if (parts.length == 2) {
						display = parts[1].trim();
					} else {
						display = displayValue;
					}
				}
				String code = getSecurityCodeFromDisplay(display);
				
				Coding coding = new Coding().setSystem("http://terminology.hl7.org/CodeSystem/v3-Confidentiality")
				        .setCode(code).setDisplay(display);
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
				attribute.setValue(display);
				attribute.setValueReferenceInternal(display);
				
				// Extract dynamic value from display
				// String[] parts = display.split(":", 2);
				// if (parts.length == 2) 
				// {
				// 	String value = parts[1].trim();
				
				// 	// Set both value and valueReferenceInternal dynamically
				// 	attribute.setValue(value);
				// 	attribute.setValueReferenceInternal(value);
				// }
				//  else 
				//  {
				// 	// fallback if display is malformed
				// 	attribute.setValue("Restricted");
				// 	attribute.setValueReferenceInternal("Restricted");
				// }
				
				// Add the attribute to the visit
				visit.addAttribute(attribute);
				Context.getVisitService().saveVisit(visit);
				
				System.out.printf("Added visit attribute: uuid=%s, display=%s%n", coding.getCode(), display);
			}
		}
		
		return visit;
	}
	
	private String getSecurityCodeFromDisplay(String display) {
		if (display == null)
			return "U";
		switch (display.toLowerCase()) {
			case "low":
				return "L";
			case "moderate":
				return "M";
			case "normal":
				return "N";
			case "restricted":
				return "R";
			case "unrestricted":
				return "U";
			case "very restricted":
				return "V";
			default:
				return "R";
		}
	}
	
}
