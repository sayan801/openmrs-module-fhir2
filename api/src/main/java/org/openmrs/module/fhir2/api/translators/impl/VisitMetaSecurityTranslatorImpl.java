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
		if (visit == null || codings == null)
		 {
			return null;
		}
		
		for (Coding coding : codings)
		 {
			String display = coding.getDisplay();
			if (display != null && display.toLowerCase().contains("security")) 
			{
				
				// The display value you want to store
				String displayValue = "Restricted";
				coding.setDisplay(displayValue);
				
				// Store the display value directly as the attribute value
				String valueToStore = displayValue;
				
				// Find "security" attribute type
				VisitAttributeType attrType = Context.getVisitService().getAllVisitAttributeTypes().stream()
				        .filter(type -> "security".equalsIgnoreCase(type.getName())).findFirst().orElse(null);
				
				if (attrType == null) 
				{
					System.out.println(" VisitAttributeType 'security' not found.");
					continue;
				}
				
				// Find existing attribute
				VisitAttribute existingAttr = visit.getActiveAttributes().stream()
				        .filter(attr -> "security".equalsIgnoreCase(attr.getAttributeType().getName())).findFirst()
				        .orElse(null);
				
				if (existingAttr != null) 
				{
					if (!displayValue.equals(existingAttr.getValue())) 
					{
						// Void the old one
						existingAttr.setVoided(true);
						existingAttr.setVoidReason("Updated by FHIR translator");
						
						// Add new one
						VisitAttribute newAttr = new VisitAttribute();
						newAttr.setAttributeType(attrType);
						newAttr.setValue(valueToStore);
						visit.addAttribute(newAttr);
						System.out.println("Voided old attribute and added new one with display value: " + valueToStore);
					} 
					else 
					{
						System.out.println("Attribute already has display value '" + displayValue + "'. Skipping.");
					}
				} 
				else
				{
					// No existing one — add new
					VisitAttribute newAttr = new VisitAttribute();
					newAttr.setAttributeType(attrType);
					newAttr.setValue(valueToStore);
					visit.addAttribute(newAttr);
					System.out.println("Added new 'security' attribute with display value: " + valueToStore);
				}
			}
		}
		
		// Save visit after processing all codings
		Context.getVisitService().saveVisit(visit);
		
		return visit;
	}
	
}
