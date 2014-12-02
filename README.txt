Magento integration with Apache OFBiz, compatible with OFBiz-13.07 and trunk

Steps to use magneto component with Apache OFBiz-13.07 and OFBiz trunk

   - Start Terminal and go to the home directory of Apache OFBiz
   - Checkout magento component in hot-deploy folder
   - Load data using command: ./ant load-demo
   - Start the OFBiz using command: ./ant start

Configuration of magento in OFBiz is also easy. This process involves two simple steps - 
 - Create SOAP/XML-RPC user and role at Magento Side. 
 - Set information at OFBiz Side: https://ofbizdomain/magento/
