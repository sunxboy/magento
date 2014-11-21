<html>
  <#if session.getAttribute("userLogin")?has_content>
    <#if page.permission?has_content && page.action?has_content && !security.hasEntityPermission(page.permission, page.action, session)>
      <#assign hasPermission = false>
    <#elseif userHasPermission?has_content && !(userHasPermission?default('N') == 'Y')>
      <#assign hasPermission = false>
    </#if>
  </#if>
  <head>
    <title>${layoutSettings.companyName}<#if hasPermission?default(true)><#if (page.titleProperty)?has_content>: ${StringUtil.wrapString(uiLabelMap[page.titleProperty])}<#else>${(page.title)?if_exists}</#if></#if></title>
    <meta name="viewport" content="width=device-width, user-scalable=no"/>
    <#if webSiteFaviconContent?has_content>
      <link rel="shortcut icon" href="<@renderContentAsText contentId='${webSiteFaviconContent.getString("contentId")}'/>">
    </#if>
    <#list styleSheets as styleSheet>
      <link rel="stylesheet" href="${styleSheet}" type="text/css"/>
    </#list>
    <#list javaScripts as javaScript>
      <script type="text/javascript" src="${javaScript}" ></script>
    </#list>
  </head>
  <body>
    <div class="container">
      <nav class="navbar navbar-default" role="navigation">
        <div class="container-fluid">
          <div class="navbar-header">
            <a class="navbar-brand" href="#">
              Magento Store Setup
            </a>
          </div>
        </div>
      </nav>
      <div class="row">
        <div class="col-md-12">
          <div id="notification-messages">
            ${screens.render("component://magento/widget/magento/CommonScreens.xml#Messages")}
          </div>