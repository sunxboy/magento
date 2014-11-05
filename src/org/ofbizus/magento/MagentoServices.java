package org.ofbizus.magento;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import magento.Filters;
import magento.SalesOrderEntity;
import magento.SalesOrderListEntity;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;

public class MagentoServices {
    public static final String module = MagentoServices.class.getName();

    // Import orders from magento
    public Map<String, Object> importPendingOrdersFromMagento(DispatchContext dctx, Map<String, ?> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Map<String, Object> serviceResp = null;
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        
        String magOrderId = (String) context.get("orderId");
        Timestamp fromDate = (Timestamp) context.get("fromDate");
        Timestamp thruDate = (Timestamp) context.get("thruDate");

        try {
            Filters filters = MagentoHelper.prepareSalesOrderFilters(magOrderId, "pending", fromDate, thruDate);

            MagentoClient magentoClient = new MagentoClient(dispatcher, delegator);

            List<SalesOrderListEntity> salesOrderList = magentoClient.getSalesOrderList(filters);
            List<String> errorMessageList = new ArrayList<String>();
            GenericValue system = delegator.findOne("UserLogin", false, UtilMisc.toMap("userLoginId", "system"));
            for (SalesOrderListEntity salesOrder : salesOrderList) {
                SalesOrderEntity salesOrderInfo = magentoClient.getSalesOrderInfo(salesOrder.getIncrementId());
                String externalId = (String) salesOrderInfo.getIncrementId();
                if (UtilValidate.isNotEmpty(externalId)) {
                    // Check if order already imported
                    GenericValue orderHeader = EntityUtil.getFirst(delegator.findByAnd("OrderHeader", UtilMisc.toMap("externalId", externalId, "salesChannelEnumId", "MAGENTO_SALE_CHANNEL", "orderTypeId", "SALES_ORDER"), null, false));
                    if (UtilValidate.isNotEmpty(orderHeader)) {
                        continue;
                    } else {
                        Map<String, Object> createOrderCtx = new HashMap<String, Object>();
                        createOrderCtx.put("orderInfo", salesOrderInfo);
                        createOrderCtx.put("userLogin", system);
                        serviceResp = dispatcher.runSync("createOrderFromMagento", createOrderCtx, 120, true);
                        if (!ServiceUtil.isSuccess(serviceResp)) {
                            errorMessageList.add((String) ServiceUtil.getErrorMessage(serviceResp));
                        }
                    }
                }
            }
        } catch (GenericEntityException gee) {
            gee.printStackTrace();
            Debug.logError("Error in order import (GenericEntityException) "+ gee.getMessage(), module);
        } catch (GenericServiceException gse) {
            gse.printStackTrace();
            Debug.logError("Error in order import (GenericServiceException) "+gse.getMessage(), module);
        }
        return result;
    }
    public static Map<String, Object> createOrderFromMagento(DispatchContext dctx, Map<String, ?> context) {
        Map<String, Object> response = ServiceUtil.returnSuccess();
        Locale locale = (Locale) context.get("locale");
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        SalesOrderEntity orderInfo = (SalesOrderEntity)context.get("orderInfo");
        if (UtilValidate.isNotEmpty(context)) {
            try {
                String result = MagentoHelper.createOrder(orderInfo, locale, delegator, dispatcher);
                if (!result.equals("success")) {
                    response = ServiceUtil.returnError(result);
                }
            } catch (GeneralException ge) {
                Debug.logError(ge.getMessage() ,module);
            }
        }
        return response;
    }
    public static Map<String, Object> importCancelledOrdersFromMagento(DispatchContext dctx, Map<String, ?> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Map<String, Object> serviceResp = null;

        String magOrderId = (String) context.get("orderId");
        Timestamp fromDate = (Timestamp) context.get("fromDate");
        Timestamp thruDate = (Timestamp) context.get("thruDate");

        try {
            Filters filters = MagentoHelper.prepareSalesOrderFilters(magOrderId, "canceled", fromDate, thruDate);
            MagentoClient magentoClient = new MagentoClient(dispatcher, delegator);
            List<SalesOrderListEntity> salesOrderList = magentoClient.getSalesOrderList(filters);
            GenericValue system = delegator.findOne("UserLogin", false, UtilMisc.toMap("userLoginId", "system"));
            for (SalesOrderListEntity salesOrder : salesOrderList) {
                String externalId = salesOrder.getIncrementId();
                if (UtilValidate.isNotEmpty(externalId)) {
                 // Check if order already imported
                    Map<String, Object> cancelOrderInfo = new HashMap<String, Object>();
                    cancelOrderInfo.put("externalId", externalId);
                    cancelOrderInfo.put("orderStatus", "ORDER_CANCELLED");
                    cancelOrderInfo.put("userLogin", system);
                    GenericValue orderHeader = EntityUtil.getFirst(delegator.findByAnd("OrderHeader", UtilMisc.toMap("externalId", externalId, "salesChannelEnumId", "MAGENTO_SALE_CHANNEL", "orderTypeId", "SALES_ORDER"), null, false));
                    if (UtilValidate.isNotEmpty(orderHeader)) {
                        if("ORDER_CANCELLED".equals(orderHeader.get("statusId"))) {
                            continue;
                        } else {
                            MagentoHelper.processStateChange(cancelOrderInfo, delegator, dispatcher);
                        }
                    }
                }
            }
        } catch (GenericEntityException gee) {
            gee.printStackTrace();
            Debug.logError("Error in order import (GenericEntityException) "+ gee.getMessage(), module);
        } catch (GenericServiceException gse) {
            gse.printStackTrace();
            Debug.logError("Error in order import (GenericServiceException) "+gse.getMessage(), module);
        } catch (GeneralException ge) {
            ge.printStackTrace();
            Debug.logError("Error in order import (GeneralException)", ge.getMessage(), module);
        }
        return result;
    }
    public static Map<String, Object> cancelOrderInMagento(DispatchContext dctx, Map<String, ?> context) {
        Map<String, Object> response = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String orderIncrementId = null;
        String orderId = (String) context.get("orderId");
        try {
            GenericValue orderHeader = delegator.findOne("OrderHeader", false, UtilMisc.toMap("orderId", orderId));
            if (UtilValidate.isNotEmpty(orderHeader) && !"ORDER_CANCELLED".equals(orderHeader.getString("syncStatusId")) && UtilValidate.isNotEmpty(orderHeader.getString("externalId"))) {
                orderIncrementId = orderHeader.getString("externalId");
                MagentoClient magentoClient = new MagentoClient(dispatcher, delegator);
                int isCanceled = magentoClient.cancelSalesOrder(orderIncrementId);
                if (UtilValidate.isNotEmpty(isCanceled) && isCanceled == 1) {
                    Debug.log("============Magento Order #"+ orderIncrementId+ " is cancelled successfully.==========================");
                }
            }
        } catch (GenericEntityException gee) {
            Debug.logError(gee.getMessage(), module);
            return ServiceUtil.returnError(gee.getMessage());
        }
        return response;
    }
    public static Map<String, Object> completeOrderInMagento(DispatchContext dctx, Map<String, ?> context) {
        Map<String, Object> response = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String orderId = (String) context.get("orderId");
        try {
            GenericValue system = delegator.findOne("UserLogin", false, UtilMisc.toMap("userLoginId", "system"));
            if (UtilValidate.isNotEmpty(orderId)) {
                GenericValue orderHeader = delegator.findOne("OrderHeader", false, UtilMisc.toMap("orderId", orderId));
                if (UtilValidate.isEmpty(orderHeader) || !"MAGENTO_SALE_CHANNEL".equals(orderHeader.getString("salesChannelEnumId"))) {
                    Debug.logInfo("Not a Magento order, doing nothing with orderId #"+ orderId, module);
                    return response;
                } else if ("ORDER_COMPLETED".equals(orderHeader.getString("statusId")) && "ORDER_COMPLETED".equals(orderHeader.getString("syncStatusId"))){
                    Debug.logInfo("Order with order Id # "+orderId+" is already marked as completed in Magento.", module);
                    return response;
                } else {
                    String resp = MagentoHelper.completeOrderInMagento(dispatcher, delegator, orderId);
                    if (UtilValidate.isNotEmpty(resp)) {
                        dispatcher.runSync("updateOrderHeader", UtilMisc.toMap("orderId", orderId, "syncStatusId", "ORDER_COMPLETED", "userLogin", system));
                        Debug.logInfo("Order with orderId # "+orderId+" is successfully marked as completed in Magento.", module);
                    }
                }
            }
        } catch (GenericEntityException gee) {
            Debug.logError(gee.getMessage(), module);
            return ServiceUtil.returnError(gee.getMessage());
        } catch (GenericServiceException gse) {
            Debug.logError(gse.getMessage(), module);
            return ServiceUtil.returnError(gse.getMessage());
        }
        return response;
    }

    public static Map<String, Object> createUpdateMagentoConfiguration(DispatchContext dctx, Map<String, ?> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String magentoConfigurationId= (String) context.get("magentoConfigurationId");
        Map<String, Object> serviceCtx = new HashMap<String, Object>();
        Map<String, Object> serviceResult = new HashMap<String, Object>();

        try {
            if(UtilValidate.isEmpty(magentoConfigurationId)) {
                serviceCtx = dctx.getModelService("createMagentoConfiguration").makeValid(context, ModelService.IN_PARAM);
                serviceResult = dispatcher.runSync("createMagentoConfiguration", serviceCtx);
                if(!ServiceUtil.isSuccess(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
            } else {
                serviceCtx = dctx.getModelService("updateMagentoConfiguration").makeValid(context, ModelService.IN_PARAM);
                serviceResult = dispatcher.runSync("updateMagentoConfiguration", serviceCtx);
                if(!ServiceUtil.isSuccess(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
            }
        } catch (GenericServiceException e) {
            Debug.logError("Getting error while configuring magento"+e.getMessage() ,module);
            return ServiceUtil.returnError("Getting error while configuring magento "+e.getMessage());
        }
        return ServiceUtil.returnSuccess("Configuration has been done successfully.");
    }
    public static Map<String, Object> updateInventoryCountInMagento(DispatchContext dctx, Map<String, ?> context) {
        Map<String, Object> response = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String productId = (String) context.get("productId");
        GenericValue goodIdentification = null;
        String inventoryFacilityId = null;
        int isStockItemUpdated = 0;
        try {
            if (UtilValidate.isNotEmpty(productId)) {
                // Handling Magento's Product Id.
                EntityCondition cond = EntityCondition.makeCondition(
                        EntityCondition.makeCondition("productId", productId),
                        EntityCondition.makeCondition("goodIdentificationTypeId", "MAGENTO_ID")
                        );
                List<GenericValue> goodIdentifications = delegator.findList("GoodIdentification", cond, null, null, null, false);
                if (UtilValidate.isEmpty(goodIdentifications)) {
                    // nothing to do
                    Debug.logInfo("Not a magento product, doing nothing "+productId, module);
                    return response;
                }
                goodIdentification = EntityUtil.getFirst(goodIdentifications);
                GenericValue system = delegator.findOne("UserLogin", false, UtilMisc.toMap("userLoginId", "system"));
                GenericValue magentoConfiguration = EntityUtil.getFirst(delegator.findList("MagentoConfiguration", EntityCondition.makeCondition("enumId", EntityOperator.EQUALS, "MAGENTO_SALE_CHANNEL"), null, null, null, false));
                if (UtilValidate.isNotEmpty(magentoConfiguration) && UtilValidate.isNotEmpty(magentoConfiguration.getString("productStoreId"))) {
                    GenericValue productStore = delegator.findOne("ProductStore", false, UtilMisc.toMap("productStoreId", magentoConfiguration.getString("productStoreId")));
                    inventoryFacilityId = productStore.getString("inventoryFacilityId");
                }
                Map<String, Object> serviceContext = new HashMap<String, Object>();
                serviceContext.put("productId", productId);
                serviceContext.put("facilityId", inventoryFacilityId);
                serviceContext.put("userLogin", system);

                Map<String, Object> serviceResult = dispatcher.runSync("getInventoryAvailableByFacility", serviceContext);
                if (ServiceUtil.isSuccess(serviceResult)) {
                    // Call magento api method for updating inventory count order
                    String inventoryCount = (String) ObjectType.simpleTypeConvert(serviceResult.get("availableToPromiseTotal"), "String", null, null);
                    MagentoClient magentoClient = new MagentoClient(dispatcher, delegator);
                    isStockItemUpdated = magentoClient.catalogInventoryStockItemUpdate(goodIdentification.getString("idValue"), inventoryCount);
                    if (isStockItemUpdated == 0) {
                        Debug.logInfo("Getting error while updating inventory of product with id: "+goodIdentification.getString("idValue")+" in magento.", module);
                    } else {
                        Debug.logInfo("Inventory count of product with id: "+goodIdentification.getString("idValue")+" has been updated succesfully in magento.", module);
                    }
                }
            }
        } catch (GenericEntityException gee) {
            Debug.logError("Getting error while updating inventory count in magento "+gee.getMessage() ,module);
        } catch (Exception e) {
            Debug.logError("Getting error while updating inventory count in magento "+e.getMessage() ,module);
        }
        return response;
    }
}