<div class="panel panel-default">
  <div class="panel-heading">
    <span class="panel-title">${uiLabelMap.CommonForgotYourPassword}</span>
  </div>
  <div class="panel-body">
    <form method="post" action="<@ofbizUrl>forgotPassword</@ofbizUrl>" class="form-vertical requireValidation">
      <input type="hidden" name="roleTypeId" value="APPLICATION_USER">
      <div class="form-group row">
        <div class="col-lg-6 col-md-7 col-sm-8">
          <label for="forgot-password-username">${uiLabelMap.MagentoEmailAddress}</label>
          <input type="email" name="userName" class="required form-control" id="forgot-password-username" data-label="${uiLabelMap.HwmCommonEmailAddress}" value="<#if requestParameters.userName?has_content>${requestParameters.userName}<#elseif autoUserLogin?has_content>${autoUserLogin.userLoginId}</#if>"/>
        </div>
      </div>
      <div class="row">
        <div class="col-lg-6 col-md-7 col-sm-8">
          <button type="submit" name="EMAIL_PASSWORD" value="Y" class="btn btn-default">${uiLabelMap.CommonEmailPassword}</button>
        </div>
      </div>
    </form>
  </div>
</div>