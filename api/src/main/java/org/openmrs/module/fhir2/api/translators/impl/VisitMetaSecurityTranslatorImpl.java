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
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir2.api.translators.VisitMetaSecurityTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.AccessLevel;
import lombok.Setter;

@Component
@Setter(AccessLevel.PACKAGE)
public class VisitMetaSecurityTranslatorImpl implements VisitMetaSecurityTranslator<Visit> {
	
	public VisitService visitService;
	
	@Autowired
	public VisitMetaSecurityTranslatorImpl(VisitService visitService) {
		this.visitService = visitService;
	}
	
	@Override
	public Meta toFhirResource(@Nonnull Visit visit) {
		
		if (visit == null) {
			// Return appropriate default or handle gracefully
			return new Meta();
		}
		
		Meta result = new Meta();
		
		List<Coding> securityTags = new ArrayList<>();
		
		// set fhir meta.security from visit- attribute ==============================		
		
		if (visit.getAttributes() != null) {
			visit.getAttributes().forEach(attr -> {
				if (attr != null && attr.getAttributeType() != null) { // Add null checks
					String attrTypeName = attr.getAttributeType().getName();
					
					if (attrTypeName != null && "security".equalsIgnoreCase(attrTypeName)) 
					{
						Object value = attr.getValue();
						String displayValue = value != null ? value.toString() : "Restricted";
						String display = "";
						if (displayValue.contains(":"))
						 {
							String[] parts = displayValue.split(":", 2);
							if (parts.length == 2) 
							{
								display = parts[1].trim();
							} 
							else 
							{
								display = displayValue.trim();
							}
						} 
						else 
						{
							display = displayValue.trim();
						}
						String code = getSecurityCodeFromDisplay(display);
						
						Coding coding = new Coding().setSystem("http://terminology.hl7.org/CodeSystem/v3-Confidentiality")
						        .setCode(code).setDisplay(display);
						securityTags.add(coding);
					}
				}
			});
		}
		
		result.setSecurity(securityTags);
		return result;
		
	}
	
	@Override
	public Visit toOpenmrsType(@Nonnull Visit visit, @Nonnull List<Coding> codings) {
		if (visit == null || codings == null || codings.isEmpty()) {
			return visit; // Return the original visit instead of null
		}
		
		for (Coding coding : codings) {
			if (coding == null)
				continue; // Skip null codings
				
			String display = coding.getDisplay();
			
			if (display != null && display.toLowerCase().contains("security")) {
				// Get all visit attribute types
				List<VisitAttributeType> attributeTypes = Context.getVisitService().getAllVisitAttributeTypes();
				
				if (attributeTypes == null) {
					continue; // Skip if no attribute types available
				}
				
				// Find the security attribute type
				VisitAttributeType attrType = attributeTypes.stream()
				        .filter(type -> type != null && "security".equals(type.getName())).findFirst().orElse(null);
				
				// Only proceed if we found a valid security attribute type
				if (attrType != null) {
					// Create a new VisitAttribute
					VisitAttribute attribute = new VisitAttribute();
					
					attribute.setAttributeType(attrType);
					attribute.setUuid(coding.getCode());
					attribute.setValue(display);
					attribute.setValueReferenceInternal(display);
					
					// Add the attribute to the visit
					visit.addAttribute(attribute);
					
					try {
						Context.getVisitService().saveVisit(visit);
						System.out.printf("Added visit attribute: uuid=%s, display=%s%n", coding.getCode(), display);
					}
					catch (Exception e) {
						System.err.printf("Failed to save visit with security attribute: %s%n", e.getMessage());
					}
				} else {
					System.err.println("Security VisitAttributeType not found. Cannot add security attribute.");
				}
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
