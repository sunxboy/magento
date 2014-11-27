<section class="well well-sm">
  <form method="post" id="companyInfo" class="requireValidation" action="<@ofbizUrl>createUpdateStoreInformation</@ofbizUrl>">
    <input type="hidden" name="partyId" value="${partyId!}"/>
    <input type="hidden" name="productStoreId" value="${(productStore.productStoreId)!}"/>
    <input type="hidden" name="postalContactMechId" value="${(companyMap.postalAddress.contactMechId)!}"/>
    <input type="hidden" name="emailContactMechId" value="${(companyMap.companyEmail.contactMechId)!}"/>
    <input type="hidden" name="telecomContactMechId" value="${(companyMap.telecomNumber.contactMechId)!}"/>
    <div class="row">
      <div class="col-lg-5 col-md-5">
        <div class="form-group row">
          <div class="col-lg-8 col-md-8">
            <label for="groupName">${uiLabelMap.MagentoCompanyName}</label>
            <input type="text" name="groupName" id="groupName" class="required form-control" data-label="${uiLabelMap.MagentoCompanyName}" value="${(companyMap.groupName)!}" maxLength="100">
          </div>
        </div>
        <div class="form-group row">
          <div class="col-lg-8 col-md-8">
            <label for="storeName">${uiLabelMap.MagentoStoreName}</label>
            <input type="text" name="storeName" id="storeName" class="required form-control" data-label="${uiLabelMap.MagentoStoreName}" value="${(productStore.storeName)!}" maxLength="100">
          </div>
        </div>
        <div class="form-group row">
          <div class="col-lg-8 col-md-8">
            <label for="contactNumber">${uiLabelMap.MagentoPhone}</label>
            <input type="text" name="contactNumber" id="contactNumber" class="form-control validate-phone" value="${(companyMap.telecomNumber.countryCode)!}${(companyMap.telecomNumber.areaCode)!}${(companyMap.telecomNumber.contactNumber)!}">
          </div>
        </div>
        <div class="form-group row">
          <div class="col-lg-8 col-md-8">
            <label class="control-label" for="infoString">${uiLabelMap.CommonEmail}</label>
            <input type="text" name="infoString" id="infoString" data-label="${uiLabelMap.CommonEmail}" class="required validate-email form-control" value="${(companyMap.companyEmail.emailAddress)!}">
          </div>
        </div>
        <div class="form-group row">
          <div class="col-lg-8 col-md-8">
            <label for="address1">${uiLabelMap.CommonAddress1}</label>
            <input type="text" name="address1" id="address1" data-label="${uiLabelMap.CommonAddress1}" class="required form-control" value="${(companyMap.postalAddress.address1)!}">
          </div>
        </div>
        <div class="form-group row">
          <div class="col-lg-8 col-md-8">
            <label for="address2">${uiLabelMap.CommonAddress2}</label>
            <input type="text" name="address2" id="address2" class="form-control" value="${(companyMap.postalAddress.address2)!}">
          </div>
        </div>
      </div>
      <div class="col-lg-5 col-md-5">
        <div class="form-group row">
          <div class="col-lg-8 col-md-8">
            <label for="city">${uiLabelMap.CommonCity}</label>
            <input type="text" name="city" id="city" class="required form-control" data-label="${uiLabelMap.CommonCity}" value="${(companyMap.postalAddress.city)!}">
          </div>
        </div>
        <div class="form-group row">
          <div class="col-lg-8 col-md-8">
            <label for="countryGeoId">${uiLabelMap.CommonCountry}</label>
            <select name="countryGeoId" id="countryGeoId" data-label="${uiLabelMap.CommonCountry}" data-dependent="#stateProvinceGeoId" class="form-control">
              <#list countryList as country>
                <option value='${country.geoId}' title='${country.geoId}' <#if country.geoId == (companyMap.postalAddress.countryGeoId)?default("USA")> selected="selected" </#if>>${country.get("geoName")?default(country.geoId)}</option>
              </#list>
            </select>
          </div>
        </div>
        <div class="form-group row">
          <div class="col-lg-8 col-md-8">
            <label for="stateProvinceGeoId">${uiLabelMap.CommonStateProvince}</label>
            <select name="stateProvinceGeoId" form="companyInfo" id="stateProvinceGeoId" class="required form-control" data-label="${uiLabelMap.CommonStateProvince}">
              <#list countryList as country>
                <#assign stateAssocs = Static["org.ofbiz.common.CommonWorkers"].getAssociatedStateList(delegator,country.geoId)>
                  <#if stateAssocs?has_content>
                    <optgroup label="${country.geoId}">
                      <option value='' title=''>${uiLabelMap.CommonSelect}</option>
                      <#list stateAssocs as stateAssoc>
                        <option value='${stateAssoc.geoId}' title='${stateAssoc.geoId}' <#if stateAssoc.geoId == (companyMap.postalAddress.stateProvinceGeoId)?if_exists> selected</#if>>${stateAssoc.geoName?default(stateAssoc.geoId)}</option>
                      </#list>
                    </optgroup>
                  <#else>
                    <optgroup label="${country.geoId}">
                      <option value='_NA_'>${uiLabelMap.CommonNA}</option>
                    </optgroup>
                  </#if>
              </#list>
            </select>
          </div>
        </div>
        <div class="form-group row">
          <div class="col-lg-8 col-md-8">
            <label for="">${uiLabelMap.CommonZipPostalCode}</label>
            <input type="text" name="postalCode" form="companyInfo" id="postalCode" class="required validate-usCanadaZip form-control" data-label="${uiLabelMap.CommonZipPostalCode}" value="${(companyMap.postalAddress.postalCode)!}" data-country-box="#countryGeoId" maxLength="60"/>
          </div>
        </div>
        <div class="row">
          <div class="col-lg-8 col-md-8">
            <button type="submit" class="btn pull-right btn-primary relative pull-left">
              ${uiLabelMap.CommonSave}
            </button>
          </div>
        </div> 
      </div>
    </div>
  </form>
</section>