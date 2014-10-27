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
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.ofbizus.magento.MagentoHelper;

public class MagentoServices {
    public static final String module = MagentoClient.class.getName();

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
            List<String> errorMessageList = new ArrayList<String>();
            GenericValue system = delegator.findOne("UserLogin", false, UtilMisc.toMap("userLoginId", "system"));
            for (SalesOrderListEntity salesOrder : salesOrderList) {
                SalesOrderEntity salesOrderInfo = magentoClient.getSalesOrderInfo(salesOrder.getIncrementId());
                String externalId = salesOrderInfo.getIncrementId();
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
                    } else {
                        Map<String, Object> createOrderCtx = new HashMap<String, Object>();
                        createOrderCtx.put("orderInfo", salesOrderInfo);
                        createOrderCtx.put("userLogin", system);
                        serviceResp = dispatcher.runSync("createOrderFromMagento", createOrderCtx, 120, true);
                        if (!ServiceUtil.isSuccess(serviceResp)) {
                            errorMessageList.add((String) ServiceUtil.getErrorMessage(serviceResp));
                        }
                        MagentoHelper.processStateChange(cancelOrderInfo, delegator, dispatcher);
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
        String orderIncrementId = null;
        String orderId = (String) context.get("orderId");
        try {
            GenericValue orderHeader = delegator.findOne("OrderHeader", false, UtilMisc.toMap("orderId", orderId));
            if (UtilValidate.isNotEmpty(orderHeader) && !"ORDER_COMPLETED".equals(orderHeader.getString("syncStatusId")) && UtilValidate.isNotEmpty(orderHeader.getString("externalId"))) {
                orderIncrementId = orderHeader.getString("externalId");
                MagentoClient magentoClient = new MagentoClient(dispatcher, delegator);
                String shipmentIncrementId = magentoClient.createShipment(orderIncrementId);
                if (UtilValidate.isNotEmpty(shipmentIncrementId)) {
                    Debug.log("============order #"+orderIncrementId+"=======shipmentIncrementId="+shipmentIncrementId+"==========================");
                }
                String invoiceIncrementId = magentoClient.createInvoice(orderIncrementId);
                if (UtilValidate.isNotEmpty(invoiceIncrementId)) {
                    Debug.log("============order #"+orderIncrementId+"=======invoiceIncrementId="+invoiceIncrementId+"==========================");
                }
            }
        } catch (GenericEntityException gee) {
            Debug.logError(gee.getMessage(), module);
            return ServiceUtil.returnError(gee.getMessage());
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

}