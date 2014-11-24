<ul class="nav nav-pills nav-stacked">
  <li <#if step == "1"> class="active" </#if>>
    <a href="<@ofbizUrl>StoreConfiguration</@ofbizUrl>"><i class="fa fa-cog"></i> ${uiLabelMap.CommonConfiguration}</a>
  </li>
  <li <#if step == "2"> class="active" </#if>>
    <a href="<@ofbizUrl>StoreInformation</@ofbizUrl>"><i class="fa fa-cog"></i> ${uiLabelMap.MagentoStore}</a>
  </li>
  <li <#if step == "3"> class="active" </#if>>
    <a href="<@ofbizUrl>FacilityInformation</@ofbizUrl>" <#if !productStore?has_content>class="disabled"</#if>><i class="fa fa-cog"></i> ${uiLabelMap.Facility}</a>
  </li>
  <li <#if step == "4"> class="active" </#if>>
    <a href="<@ofbizUrl>ShippingInformation</@ofbizUrl>" <#if !productStore?has_content>class="disabled"</#if>><i class="fa fa-cog"></i> ${uiLabelMap.CommonShipping}</a>
  </li>
</ul>
