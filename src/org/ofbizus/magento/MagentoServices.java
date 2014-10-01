package org.ofbizus.magento;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
import org.ofbiz.service.ServiceUtil;
import org.ofbizus.magento.MagentoHelper;

public class MagentoServices {
    public static final String module = MagentoClient.class.getName();

    // Import orders from magento
    public Map<String, Object> createPendingOrdersFromMagento(DispatchContext dctx, Map<String, ?> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Map<String, Object> serviceResp = null;
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        
        String magOrderId = (String) context.get("orderId");
        String statusId = (String) context.get("statusId");

        Timestamp fromDate = (Timestamp) context.get("fromDate");
        Timestamp thruDate = (Timestamp) context.get("thruDate");
        
        try {
        Map<String, Object> condMap = MagentoHelper.prepareSalesOrderCondition(magOrderId, "pending", fromDate, thruDate);
        MagentoClient magentoClient = new MagentoClient(dispatcher, delegator);
        Object[] responseMessage = magentoClient.getSalesOrderList(condMap);
        List<String> errorMessageList = new ArrayList<String>();
        GenericValue system = delegator.findOne("UserLogin", false, UtilMisc.toMap("userLoginId", "system"));
        for (Object orderInformation : responseMessage) {
            Map<String, Object> orderInfo = (Map<String, Object>)orderInformation;
            Object salesOrderInformation = magentoClient.getSalesOrderInfo((String)orderInfo.get("increment_id"));
            Map<String, Object> salesOrderInfo = (Map<String, Object>)salesOrderInformation;
            String externalId = (String) salesOrderInfo.get("increment_id");
            if (UtilValidate.isNotEmpty(externalId)) {
                // Check if order already imported
                GenericValue orderHeader = EntityUtil.getFirst(delegator.findByAnd("OrderHeader", UtilMisc.toMap("externalId", externalId, "salesChannelEnumId", "MAGENTO_SALE_CHANNEL", "orderTypeId", "SALES_ORDER"), null, false));
                String magentoOrderStatus = (String) salesOrderInfo.get("status");
                if (UtilValidate.isNotEmpty(orderHeader)) {
                    String orderStatusId = orderHeader.getString("statusId");
                    if (("canceled".equals(magentoOrderStatus)) && !("ORDER_CANCELLED".equals(orderStatusId))) {
                        Map<String, Object> cancelOrderFromMagentoCtx = new HashMap<String, Object>();
                        cancelOrderFromMagentoCtx.put("externalId", externalId);
                        cancelOrderFromMagentoCtx.put("orderStatus", magentoOrderStatus);
                        cancelOrderFromMagentoCtx.put("userLogin", system);
                        serviceResp = dispatcher.runSync("cancelOrderFromMagento", cancelOrderFromMagentoCtx);
                        if (ServiceUtil.isSuccess(serviceResp)) {
                        } else {
                            errorMessageList.add((String) ServiceUtil.getErrorMessage(serviceResp));
                        }
                    } else {
                        //Ignore if order already imported
                        continue;
                    }
                } else {
                    Map<String, Object> createOrderCtx = new HashMap<String, Object>();
                    createOrderCtx.put("orderInfo", salesOrderInfo);
                    createOrderCtx.put("userLogin", system);
                    serviceResp = dispatcher.runSync("createOrderFromMagento", createOrderCtx, 120, true);
                    if (ServiceUtil.isSuccess(serviceResp)) {
                        if ("canceled".equals(magentoOrderStatus)) {
                            Map<String, Object> cancelOrderCtx = new HashMap<String, Object>();
                            cancelOrderCtx.put("externalId", externalId);
                            cancelOrderCtx.put("orderStatus", magentoOrderStatus);
                            cancelOrderCtx.put("userLogin", system);
                            Map cancelOrderResp = dispatcher.runSync("cancelOrderFromMagento", cancelOrderCtx);
                            if (!ServiceUtil.isSuccess(cancelOrderResp)) {
                                errorMessageList.add((String) ServiceUtil.getErrorMessage(serviceResp));
                            }
                        }
                    } else {
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
        Map orderInfo = (Map)context.get("orderInfo");
        if (UtilValidate.isNotEmpty(context)) {
            try {
                String result = MagentoHelper.createOrder(orderInfo, locale, delegator, dispatcher);
                if (!result.equals("success")) {
                    response = ServiceUtil.returnError(result);
                }
            } catch (GeneralException ge) {
                Debug.logError(ge ,module);
            }
        }
        return response;
    }
    public static Map<String, Object> cancelOrderFromMagento(DispatchContext dctx, Map<String, ?> context) {
        Map<String, Object> response = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        if (context != null) {
            try {
                MagentoHelper.processStateChange(context, delegator, dispatcher);
            } catch (GeneralException e) {
                Debug.logError(e ,module);
            }
        }
        return response;
    }
    public static Map<String, Object> cancelOrderInMagento(DispatchContext dctx, Map<String, ?> context) {
        Map<String, Object> response = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String orderIncrementId = null;
        String orderId = (String) context.get("orderId");
        try {
            GenericValue orderHeader = delegator.findOne("OrderHeader", false, UtilMisc.toMap("orderId", orderId));
            if (UtilValidate.isNotEmpty(orderHeader) && UtilValidate.isNotEmpty(orderHeader.getString("externalId"))) {
                orderIncrementId = orderHeader.getString("externalId");
                MagentoClient magentoClient = new MagentoClient(dispatcher, delegator);
                boolean isCanceled = magentoClient.cancelSalesOrder(orderIncrementId);
                if (isCanceled) {
                    Debug.log("============Magento Order #"+ orderIncrementId+ " is cancelled successfully.==========================");
                }
            }
        } catch (GenericEntityException gee) {
            Debug.logError(gee, module);
        }
        return response;
    }
}