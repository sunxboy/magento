<#if parameters.carrierPartyId?has_content>
  <div class="row">
    <div class="col-md-12">
      <a class="btn btn-default pull-right" href="<@ofbizUrl>ShippingInformation?partyId=${(parameters.carrierPartyId)!}</@ofbizUrl>"><i class="fa fa-times"></i></a>
    </div>
  </div>
  <form method="post" action="<@ofbizUrl>createUpdateShipmentGatewayConfig</@ofbizUrl>" class="form-vertical requireValidation">
    <input type="hidden" name="shipmentGatewayConfigId" value="${(shipmentGatewayConfiguration.shipmentGatewayConfigId)!}">
    <input type="hidden" name="carrierPartyId" value="${(parameters.carrierPartyId)!}">
    <div class="form-group row">
      <div class="col-lg-6 col-md-6">
        <label for="accessUserId">${uiLabelMap.MagentoAccessUserKey}</label>
        <input type="text" id="accessUserId" name="accessUserId" data-label="${uiLabelMap.MagentoAccessUserKey}" class="form-control required" value="${(shipmentGatewayConfiguration.accessUserId)!}"/>
      </div>
    </div>
    <div class="form-group row">
      <div class="col-lg-6 col-md-6">
        <label for="accessPassword">${uiLabelMap.MagentoAccessUserPassword}</label>
        <input type="text" id="accessPassword" name="accessPassword" class="required form-control" data-label="${uiLabelMap.MagentoAccessUserPassword}" value="${(shipmentGatewayConfiguration.accessPassword)!}"/>
      </div>
    </div>
    <div class="form-group row">
      <div class="col-lg-6 col-md-6">
        <label for="connectUrl">${uiLabelMap.MagentoConnectUrl}</label>
        <input type="text" id="connectUrl" name="connectUrl" class="required form-control" data-label="${uiLabelMap.MagentoConnectUrl}" value="${(shipmentGatewayConfiguration.connectUrl)!}"/>
      </div>
    </div>
    <div class="row">
      <div class="col-lg-6 col-md-6">
        <button type="submit" class="btn btn-primary">${uiLabelMap.CommonSave}</button>
      </div>
    </div>
  </form>
</#if>