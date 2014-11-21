<#-- display the error messages -->
<#if errorNotificationMessageList?has_content>
  <#list errorNotificationMessageList as errorMsg>
    <!--TODO: alert-error is renamed to alert-danger class in bootstrap 3.0
     so alert-error class will be removed once all components will be migrated to bootstrap 3.0-->
    <div class="alert alert-block alert-error alert-danger fade in">
      <button type="button" class="close" data-dismiss="alert"><i class="fa fa-times"></i> </button>
      ${StringUtil.wrapString(errorMsg)?replace("<br/>", "")}
    </div>
  </#list>
</#if>
<#-- display the event messages -->
<#if eventNotificationMessageList?has_content>
  <#list eventNotificationMessageList as eventMsg>
    <div class="alert alert-block alert-success fade in">
      <button type="button" class="close" data-dismiss="alert"><i class="fa fa-times"></i></button>
      ${StringUtil.wrapString(eventMsg)?replace("<br/>", "")}
    </div>
  </#list>
</#if>