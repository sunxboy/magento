<section class="well well-sm" id="storeInfo">
  <#if productStore?has_content>
    <#include "component://magento/webapp/magento/store/ProductStoreAndCompany.ftl"/>
  <#elseif productStoreList?has_content>
<div class="row">
      <div class="col-lg-12 col-md-12">
        <a href="" class="btn btn-default pull-right" data-ajax-update="#storeInfo" data-update-url="<@ofbizUrl>CreateNewProductStoreAndCompany</@ofbizUrl>" >${uiLabelMap.MagentoCreateNewProductStore}</a>
      </div>
    </div>
    <form method="post" action="<@ofbizUrl>addMagentoProductStore</@ofbizUrl>" class="form-vertical requireValidation">
      <#if magentoConfiguration?has_content>
        <input type="hidden" name="magentoConfigurationId" value="${(magentoConfiguration.magentoConfigurationId)!}">
      <#else>
        <input type="hidden" name="enumId" value="MAGENTO_SALE_CHANNEL">
      </#if>
      <div class="form-group row">
        <div class="col-lg-3 col-md-3">
          <label for="productStoreId">${uiLabelMap.MagentoProductStore}</label>
          <select name="productStoreId" id="productStoreId" class="required form-control" data-label="${uiLabelMap.MagentoProductStore}">
            <option value=''>${uiLabelMap.CommonSelect}</option>
            <#list productStoreList as productStore>
              <option value='${(productStore.productStoreId)!}'>${(productStore.storeName)!}</option>
            </#list>
          </select>
        </div>
      </div>
      <div class="row">
        <div class="col-lg-3 col-md-3">
          <button type="submit" class="btn btn-primary">
            ${uiLabelMap.CommonAdd}
          </button>
        </div>
      </div>
    </form>
  </#if>
</section>