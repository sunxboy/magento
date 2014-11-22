<div class="panel panel-default">
  <div class="panel-heading">
    <span class="panel-title">${uiLabelMap.CommonRegistered}</span>
  </div>
  <div class="panel-body">
    <form method="post" action="<@ofbizUrl>login</@ofbizUrl>" class="form-vertical requireValidation">
      <#-- assuming that javascript will always be enabled -->
      <input type="hidden" name="JavaScriptEnabled" value="Y"/>
      <div class="form-group row">
        <div class="col-lg-6 col-md-7 col-sm-8">
          <label for="username">${uiLabelMap.MagentoEmailAddress}</label>
          <input type="text" name="USERNAME" id="username" value="" class="form-control required" data-label="${uiLabelMap.MagentoEmailAddress}" <#if username == "">autofocus</#if>/>
        </div>
      </div>
      <div class="form-group row">
        <div class="col-lg-6 col-md-7 col-sm-8">
          <label for="password">${uiLabelMap.CommonPassword}</label>
          <input type="password" name="PASSWORD" id="password" class="form-control required" data-label="${uiLabelMap.CommonPassword}" <#if username != "">autofocus</#if>/>
        </div>
      </div>
      <div class="row">
        <div class="col-lg-6 col-md-7 col-sm-8">
          <button type="submit" class="btn btn-primary">${uiLabelMap.CommonLogin}</button>
        </div>
      </div>
    </form>
  </div>
</div>