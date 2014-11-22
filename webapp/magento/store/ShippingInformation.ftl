<div class="panel-group" id="accordion" role="tablist" aria-multiselectable="true">
  <#if carrierParties?has_content>
    <#list carrierParties as carrier>
      <div class="panel panel-default">
        <div class="panel-heading" role="tab" id="heading_${(carrier.partyId)!}">
          <h4 class="panel-title">
            <a data-toggle="collapse" data-parent="#accordion" href="#${(carrier.partyId)!}" aria-expanded="true" aria-controls="collapse_${(carrier.partyId)!}">
              ${(carrier.groupName)!}
            </a>
          </h4>
        </div>
        <div id="${(carrier.partyId)!}" class="panel-collapse collapse <#if (parameters.partyId)?has_content && (parameters.partyId) == (carrier.partyId)>in<#elseif !(parameters.partyId)?has_content && carrier_index == 0>in</#if>" role="tabpanel" aria-labelledby="heading_${(carrier.partyId)!}">
          <div class="panel-body">
            <div class="row">
              <div class="col-md-12">
                <div class="row">
                  <div class="col-md-12">
                    <ul class="list-unstyled list-inline pull-right">
                      <li><a class="btn btn-default" data-toggle-display="#shipGateway_${carrier.partyId}" data-hide=".shipMeth_${carrier.partyId}" data-toggle-effect="slide">Edit Configuration</a></li>
                      <li><a class="btn btn-default" data-toggle-display="#shipingMethod_${carrier.partyId}" data-hide=".shipGate_${carrier.partyId}" data-toggle-hide=".hideShipMeth_${carrier.partyId}" data-toggle-effect="slide">Add Shipping Method</a></li>
                    </ul>
                  </div>
                </div>
                <div id="shipGateway_${carrier.partyId}" class="shipGate_${carrier.partyId}" style="display:none;">
                  <#include "component://magento/webapp/magento/store/ShippingGatewayConfiguration.ftl"/>
                </div>
                <div id="shipingMethod_${carrier.partyId}" class="shipMeth_${carrier.partyId}" style="display:none;">
                  <form method="post" action="<@ofbizUrl>createRemoveProductStoreShipMeth</@ofbizUrl>" id="addShipingMethod_${carrier.partyId}" class="">
                    <input type="hidden" name="productStoreId" value="${(productStore.productStoreId)!}"/>
                    <input type="hidden" name="partyId" value="${(carrier.partyId)!}"/>
                    <input type="hidden" name="roleTypeId" value="CARRIER"/>
                    <input type="hidden" name="serviceName" value="${(shippingServiceNameMap[carrier.partyId])!}"/>
                    <div class="form-group row">
                      <div class="col-lg-3 col-md-3">
                        <label for="countryGeoId">Shipping Method</label>
                        <select name="shipmentMethodTypeId" multiple="multiple" class="form-control chosen-select">
                          <#if carrierAndShipmentMethod?has_content>
                            <#assign shipmentMethodList = carrierAndShipmentMethod[carrier.partyId]>
                            <#list shipmentMethodList as shipmentMethod>
                              <option value="${(shipmentMethod.shipmentMethodTypeId)!}"> ${(shipmentMethod.description)!}</option>
                            </#list>
                          </#if>
                        </select>
                      </div>
                    </div>
                    <div class="row">
                      <div class="col-lg-3 col-md-3">
                        <button type="submit" class="btn btn-primary" title="${uiLabelMap.CommonAdd}">${uiLabelMap.CommonAdd}</button>
                      </div>
                    </div>
                  </form>
                </div>
                <#if storeShipMethMap?has_content && (storeShipMethMap[carrier.partyId])?has_content>
                  <#assign storeShipMeth = storeShipMethMap[carrier.partyId]/>
                  <table class="table table-hover">
                    <thead>
                      <tr>
                        <th class="col-lg-5 col-md-5">${uiLabelMap.MagentoShippingMethod}</th>
                        <th class="col-lg-4 col-md-4">${uiLabelMap.MagentoAction}</th>
                      </tr>
                    </thead>
                    <tbody>
                      <#list storeShipMeth.keySet() as storeShipMethTypeId>
                        <#assign storeShippingMethodMap = storeShipMeth[storeShipMethTypeId]>
                        <tr>
                          <td>
                            ${(storeShippingMethodMap.description)!}
                          </td>
                          <td> 
                            <form method="post" action="<@ofbizUrl>createRemoveProductStoreShipMeth</@ofbizUrl>" id="removeShippingMethod_${(carrier.partyId)!}">
                              <input type="hidden" name="productStoreShipMethId" value="${(storeShippingMethodMap.productStoreShipMethId)!}"/> 
                              <input type="hidden" name="productStoreId" value="${(productStore.productStoreId)!}"/>
                              <input type="hidden" name="partyId" value="${(carrier.partyId)!}"/>
                              <input type="hidden" name="roleTypeId" value="CARRIER"/>
                              <input type="hidden" name="shipmentMethodTypeId" value="${storeShipMethTypeId!}"/>
                              <button type="submit" class="btn-link" title="${uiLabelMap.CommonRemove}">
                                <i class="fa fa-trash-o"></i>
                              </button>
                            </form>
                          </td>
                        </tr>
                      </#list>
                    </tbody>
                  </table>
                <#else>
                  <div class="hideShipMeth_${carrier.partyId} shipMeth_${carrier.partyId}">
                    There is no shipping method. To add click <a href="" data-toggle-display="#shipingMethod_${carrier.partyId}" data-toggle-hide=".hideShipMeth_${carrier.partyId}" data-toggle-effect="slide">here</a>.
                  </div>
                </#if>
              </div>
            </div>
          </div>
        </div>
      </div>
    </#list>
  </#if>
</div>
