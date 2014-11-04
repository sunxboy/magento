magento
=======

Magento integration with Apache OFBiz, compatible with OFBiz-13.07 and trunk

Steps to use magneto component with Apache OFBiz-13.07 and OFBiz trunk

1. Start Terminal and go to the home directory of Apache OFBiz
2. Checkout magento component in hot-deploy folder
3. Load data using command: ./ant load-demo
4. Start the OFBiz using command: ./ant run

Configuration of magento in OFBiz is also easy. This process involves two simple steps - 
1) Create SOAP/XML-RPC user and role at Magento Side.
2) Set information at OFBiz Side: https://ofbizdomain/magento/control/main
