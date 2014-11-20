<div class="row">
  <div class="col-lg-2 col-md-2">
    <#include "component://magento/webapp/magento/store/StoreNav.ftl"/>
  </div>
  <div class="col-lg-10 col-md-10">
    <#if step == "1">
      <#include "component://magento/webapp/magento/store/StoreConfiguration.ftl"/>
    <#elseif step="2">
      <#include "component://magento/webapp/magento/store/StoreInformation.ftl"/>
    <#elseif step="3">
      <#include "component://magento/webapp/magento/store/FacilityInformation.ftl"/>
    <#elseif step="4">
      <#include "component://magento/webapp/magento/store/ShippingInformation.ftl"/>
    </#if>
  </div>
</div>