/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.fhir2.api.dao;

import javax.annotation.Nonnull;

import java.util.Collection;
import java.util.List;

import org.openmrs.Provider;
import org.openmrs.ProviderAttribute;
import org.openmrs.annotation.Authorized;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.openmrs.util.PrivilegeConstants;

public interface FhirPractitionerDao extends FhirDao<Provider> {
	
	@Override
	@Authorized(PrivilegeConstants.GET_PROVIDERS)
	Provider get(@Nonnull String uuid);
	
	@Override
	@Authorized(PrivilegeConstants.GET_PROVIDERS)
	List<Provider> get(@Nonnull Collection<String> uuids);
	
	@Authorized(PrivilegeConstants.GET_PROVIDERS)
	List<ProviderAttribute> getActiveAttributesByPractitionerAndAttributeTypeUuid(@Nonnull Provider provider,
	        @Nonnull String providerAttributeTypeUuid);
	
	@Override
	@Authorized(PrivilegeConstants.GET_PROVIDERS)
	List<Provider> getSearchResults(@Nonnull SearchParameterMap theParams);
	
	@Override
	@Authorized(PrivilegeConstants.GET_PROVIDERS)
	int getSearchResultsCount(@Nonnull SearchParameterMap theParams);
	
	@Override
	@Authorized({ PrivilegeConstants.MANAGE_PROVIDERS })
	Provider createOrUpdate(@Nonnull Provider newEntry);
	
	@Override
	@Authorized(PrivilegeConstants.MANAGE_PROVIDERS)
	Provider delete(@Nonnull String uuid);
}
