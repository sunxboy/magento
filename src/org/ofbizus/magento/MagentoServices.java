package org.ofbizus.magento;

import java.sql.Timestamp;
import java.util.ArrayList;
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