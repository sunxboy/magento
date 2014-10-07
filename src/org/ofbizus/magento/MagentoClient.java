package org.ofbizus.magento;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.LocalDispatcher;

public class MagentoClient {
    public static final String module = MagentoClient.class.getName();
    private static String xmlRpcUrl;
    private static String xmlRpcUserName;
    private static String xmlRpcPassword;

    protected LocalDispatcher dispatcher;
    protected Delegator delegator;
    protected GenericValue system;

    public MagentoClient (LocalDispatcher dispatcher, Delegator delegator) {
        this.dispatcher = dispatcher;
        this.delegator = delegator;

        try {
            system = delegator.findOne("UserLogin", true, "userLoginId", "system");
            GenericValue magentoConfiguration = EntityUtil.getFirst((delegator.findList("MagentoConfiguration", null, null, null, null, false))); 
            if (UtilValidate.isNotEmpty(magentoConfiguration)) { 
                xmlRpcUrl = (String) magentoConfiguration.get("serverUrl");
                xmlRpcUserName = (String) magentoConfiguration.get("xmlRpcUserName");
                xmlRpcPassword = (String) magentoConfiguration.get("password");
            }
        } catch (GenericEntityException gee) {
            Debug.logError(gee, module);
            system = delegator.makeValue("UserLogin");
            system.set("userLoginId", "system");
            system.set("partyId", "admin");
            system.set("isSystem", "Y");
        }
    }

    public static XmlRpcClient getMagentoConnection() {
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        XmlRpcClient xmlrpc = new XmlRpcClient();
        // Server URL
        try {
            config.setServerURL(new URL(xmlRpcUrl));
            config.setEnabledForExtensions(true);
            // create an instance of XmlRpcClient
            xmlrpc.setConfig(config);
            xmlrpc.setTypeFactory(new NilParser(xmlrpc));
        } catch (Exception e) {
            e.printStackTrace();
            Debug.logError("Error in connection=====" + e.getMessage(), module);
        }
        return xmlrpc;
    }

    public static String getMagentoSession() {
        String responseMessage = null;
        try {
            XmlRpcClient xmlrpc = getMagentoConnection();
            List messageParams = new ArrayList();
            messageParams.add(xmlRpcUserName);
            messageParams.add(xmlRpcPassword);

            responseMessage = (String) xmlrpc.execute("login", messageParams);
            Debug.logInfo("The result details is ======== " + responseMessage, module);
        } catch (Exception e) {
            e.printStackTrace();
            Debug.logError("Error in getting magento session=====" + e.getMessage(), module);
            return "error";
        }
        return responseMessage;
    }

    // Fetches sales order List from magento
    public Object[] getSalesOrderList(Map<String, Object> filters) {
        Object[] result = null;
        try {
            XmlRpcClient xmlrpc = getMagentoConnection();
            String magentoSessionId = getMagentoSession();
            List params = new ArrayList();
            if (UtilValidate.isNotEmpty(filters)) {
                params.add(filters);
            }
            result = (Object[]) xmlrpc.execute("call", new Object[] { magentoSessionId, new String("sales_order.list"), params});
        } catch (XmlRpcException e) {
            e.printStackTrace();
            Debug.logError("Error in order import (XmlRpcException) " + e.getMessage(), module);
        }
        return result;
    }

    // Fetches sales order info from magento
    public Object getSalesOrderInfo(String orderIncrementId) {
        Object result = null;
        try {
            XmlRpcClient xmlrpc = getMagentoConnection();
            String magentoSessionId = getMagentoSession();
            Map<String, Object> condMap = new HashMap<String, Object>();
            Map<String, String> orderIncrementIdMap = UtilMisc.toMap("eq", orderIncrementId);
            condMap.put("orderIncrementId", orderIncrementIdMap);
            List params = new ArrayList();
            if (UtilValidate.isNotEmpty(condMap)) {
                params.add(condMap);
            }
            result = (Object) xmlrpc.execute("call", new Object[] { magentoSessionId, new String("sales_order.info"), params});
        } catch (XmlRpcException e) {
            e.printStackTrace();
            Debug.logError("Error in order import (XmlRpcException) " + e.getMessage(), module);
        }
        return result;
    }

    public Object[] getCatalogInventoryStockItemList (String sku) {
        Object[] result = null;
        try {
            XmlRpcClient xmlrpc = getMagentoConnection();
            String magentoSessionId = getMagentoSession();
            List params = new ArrayList();
            params.add(sku);
            result = (Object[]) xmlrpc.execute("call", new Object[] { magentoSessionId, new String("cataloginventory_stock_item.list"), params});
        } catch (XmlRpcException e) {
            e.printStackTrace();
            Debug.logError("Error in order import (XmlRpcException) " + e.getMessage(), module);
        }
        return result;
    }

    public Object[] getDirectoryRegionList(String countryGeoCode) {
        Object[] result = null;
        try {
            XmlRpcClient xmlrpc = getMagentoConnection();
            String magentoSessionId = getMagentoSession();
            List params = new ArrayList();
            params.add(countryGeoCode);
            result = (Object[]) xmlrpc.execute("call", new Object[] { magentoSessionId, new String("directory_region.list"), params});
        } catch (XmlRpcException e) {
            e.printStackTrace();
            Debug.logError("Error in order import (XmlRpcException) " + e.getMessage(), module);
        }
        return result;
    }
    public boolean cancelSalesOrder(String orderIncrementId) {
        boolean isCancelled = false;
        try {
            XmlRpcClient xmlrpc = getMagentoConnection();
            String magentoSessionId = getMagentoSession();
            List params = new ArrayList();
            params.add(orderIncrementId);
            isCancelled = (Boolean) xmlrpc.execute("call", new Object[] { magentoSessionId, new String("sales_order.cancel"), params});
        } catch (XmlRpcException e) {
            e.printStackTrace();
            Debug.logError("Error in order import (XmlRpcException) " + e.getMessage(), module);
        }
        return isCancelled;
    }
    public String createShipment(String orderIncrementId) {
        String shipmentIncrementId = null;
        try {
            XmlRpcClient xmlrpc = getMagentoConnection();
            String magentoSessionId = getMagentoSession();
            List params = new ArrayList();
            params.add(orderIncrementId);
            shipmentIncrementId = (String) xmlrpc.execute("call", new Object[] { magentoSessionId, new String("order_shipment.create"), params});
        } catch (XmlRpcException e) {
            e.printStackTrace();
            Debug.logError("Error in order import (XmlRpcException) " + e.getMessage(), module);
        }
        return shipmentIncrementId;
    }
    public String createInvoice(String orderIncrementId) {
        String invoiceIncrementId = null;
        try {
            XmlRpcClient xmlrpc = getMagentoConnection();
            String magentoSessionId = getMagentoSession();
            List params = new ArrayList();
            params.add(orderIncrementId);
            invoiceIncrementId = (String) xmlrpc.execute("call", new Object[] { magentoSessionId, new String("order_invoice.create"), params});

        } catch (XmlRpcException e) {
            e.printStackTrace();
            Debug.logError("Error in order import (XmlRpcException) " + e.getMessage(), module);
        }
        return invoiceIncrementId;
    }
}
