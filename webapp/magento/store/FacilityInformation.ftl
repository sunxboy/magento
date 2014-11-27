<section class="well well-sm">
  <form method="post" id="companyInfo" class="requireValidation" action="<@ofbizUrl><#if facility?has_content>updateFacilityInformation<#else>createFacilityInformation</#if></@ofbizUrl>">
    <input type="hidden" name="productStoreId" value="${(productStore.productStoreId)!}"/>
    <input type="hidden" name="partyId" value="${(productStore.payToPartyId)!}"/>
    <input type="hidden" name="facilityId" value="${(facility.facilityId)!}"/>
    <input type="hidden" name="facilityPostalContactMechId" value="${(warehousePostalAddress.contactMechId)!}"/>
    <input type="hidden" name="facilityTelecomContactMechId" value="${(warehouseTelecomNumber.contactMechId)!}"/>
    <div class="row">
      <div class="col-lg-5 col-md-5">
        <div class="form-group row">
          <div class="col-lg-8 col-md-8">
            <label for="facilityName">${uiLabelMap.MagentoFacilityName}</label>
            <input type="text" name="facilityName" id="facilityName"  class="required form-control" data-label="${uiLabelMap.MagentoFacilityName}" value="${(facility.facilityName)!}" maxLength="100">
          </div>
        </div>
        <div class="form-group row">
          <div class="col-lg-8 col-md-8">
            <label for="contactNumber">${uiLabelMap.MagentoPhone}</label>
            <input type="tel" name="contactNumber" id="contactNumber" class="form-control validate-phone" value="${(warehouseTelecomNumber.countryCode)!}${(warehouseTelecomNumber.areaCode)!}${(warehouseTelecomNumber.contactNumber)!}">
          </div>
        </div>
        <div class="form-group row">
          <div class="col-lg-8 col-md-8">
            <label for="address1">${uiLabelMap.CommonAddress1}</label>
            <input type="text" name="address1" id="address1" data-label="${uiLabelMap.CommonAddress1}" class="required form-control" value="${(warehousePostalAddress.address1)!}">
          </div>
        </div>
        <div class="form-group row">
          <div class="col-lg-8 col-md-8">
            <label for="address2">${uiLabelMap.CommonAddress2}</label>
            <input type="text" name="address2" id="address2" class="form-control" value="${(warehousePostalAddress.address2)!}">
          </div>
        </div>
      </div>
      <div class="col-lg-5 col-md-5">
        <div class="form-group row">
          <div class="col-lg-8 col-md-8">
            <label for="city">${uiLabelMap.CommonCity}</label>
            <input type="text" name="city" id="city" class="required form-control" data-label="${uiLabelMap.CommonCity}" value="${(warehousePostalAddress.city)!}">
          </div>
        </div>
        <div class="form-group row">
          <div class="col-lg-8 col-md-8">
            <label for="countryGeoId">${uiLabelMap.CommonCountry}</label>
            <select name="countryGeoId" id="countryGeoId" data-label="${uiLabelMap.CommonCountry}" data-dependent="#stateProvinceGeoId" class="form-control">
              <#list countryList as country>
                <option value='${country.geoId}' title='${country.geoId}' <#if country.geoId == (warehousePostalAddress.countryGeoId)?default("USA")> selected="selected" </#if>>${country.get("geoName")?default(country.geoId)}</option>
              </#list>
            </select>
          </div>
        </div>
        <div class="form-group row">
          <div class="col-lg-8 col-md-8">
            <label for="stateProvinceGeoId">${uiLabelMap.CommonStateProvince}</label>
            <select name="stateProvinceGeoId" id="stateProvinceGeoId" class="required form-control" data-label="${uiLabelMap.CommonStateProvince}">
              <#list countryList as country>
                <#assign stateAssocs = Static["org.ofbiz.common.CommonWorkers"].getAssociatedStateList(delegator,country.geoId)>
                  <#if stateAssocs?has_content>
                    <optgroup label="${country.geoId}">
                      <option value='' title=''>${uiLabelMap.CommonSelect}</option>
                      <#list stateAssocs as stateAssoc>
                        <option value='${stateAssoc.geoId}' title='${stateAssoc.geoId}' <#if stateAssoc.geoId == (warehousePostalAddress.stateProvinceGeoId)?if_exists> selected</#if>>${stateAssoc.geoName?default(stateAssoc.geoId)}</option>
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
            <input type="text" name="postalCode" id="postalCode" class="required form-control" data-label="${uiLabelMap.CommonZipPostalCode}" value="${(warehousePostalAddress.postalCode)!}" maxLength="60"/>
          </div>
        </div>
        <div class="row">
          <div class="col-lg-8 col-md-8">
            <button type="submit" class="btn btn-primary pull-left">
              ${uiLabelMap.CommonSave}
            </button>
          </div>
        </div> 
      </div>
    </div>
  </form>
</section>
