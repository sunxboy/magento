<section class="well well-sm">
  <form method="post" action="<@ofbizUrl>createUpdateMagentoConfiguration</@ofbizUrl>" class="form-vertical requireValidation">
    <#if magentoConfiguration?has_content>
      <input type="hidden" name="magentoConfigurationId" value="${(magentoConfiguration.magentoConfigurationId)!}">
    <#else>
      <input type="hidden" name="enumId" value="MAGENTO_SALE_CHANNEL">
    </#if>
    <div class="form-group row">
      <div class="col-lg-6 col-md-6">
        <label for="xmlRpcUserName">${uiLabelMap.MagentoSoapUserName}</label>
        <input type="text" id="xmlRpcUserName" name="xmlRpcUserName" data-label="${uiLabelMap.MagentoSoapUserName}" class="form-control required" value="${(magentoConfiguration.xmlRpcUserName)!}"/>
      </div>
    </div>
    <div class="form-group row">
      <div class="col-lg-6 col-md-6">
        <label for="password">${uiLabelMap.CommonPassword}</label>
        <input type="text" id="password" name="password" class="required form-control" data-label="${uiLabelMap.CommonPassword}" value="${(magentoConfiguration.password)!}"/>
      </div>
    </div>
    <div class="form-group row">
      <div class="col-lg-6 col-md-6">
        <label for="serverUrl">${uiLabelMap.MagentoMagentoConnectUrl}</label>
        <small class="text-muted">(eg. http://magentohost/soap/api/?wsdl)</small>
        <input type="text" id="serverUrl" name="serverUrl" class="required form-control" data-label="${uiLabelMap.MagentoMagentoWsdlLocationUrl}" value="${(magentoConfiguration.serverUrl)!}"/>
      </div>
    </div>
    <div class="row">
      <div class="col-lg-6 col-md-6">
        <button type="submit" class="btn btn-primary">${uiLabelMap.CommonSave}</button>
      </div>
    </div>
  </form>
</section>