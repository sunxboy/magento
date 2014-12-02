package org.ofbiz.magento;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
        } catch (Exception e) {
            Debug.logError("Error in improting pending orders from Magento. Error Message: " +e.getMessage(), module);
            e.printStackTrace();
            return ServiceUtil.returnError("Error in improting pending orders from Magento. Error Message:" +e.getMessage());
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
        } catch (Exception e) {
            Debug.logError("Error in improting cancelled orders from Magento. Error Message: " +e.getMessage(), module);
            e.printStackTrace();
            return ServiceUtil.returnError("Error in improting cancelled orders from Magento. Error Message:" +e.getMessage());
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
        }  catch (Exception e) {
            Debug.logError("Error in cancelling order in Magento. Error Message: " +e.getMessage(), module);
            e.printStackTrace();
            return ServiceUtil.returnError("Error in cancelling order in Magento. Error Message:" +e.getMessage());
        }
        return response;
    }
    public static Map<String, Object> holdOrderInMagento(DispatchContext dctx, Map<String, ?> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String orderIncrementId = null;
        String orderId = (String) context.get("orderId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        try {
            GenericValue orderHeader = delegator.findOne("OrderHeader", false, UtilMisc.toMap("orderId", orderId));
            if (UtilValidate.isNotEmpty(orderHeader) && !"ORDER_HOLD".equals(orderHeader.getString("syncStatusId")) && UtilValidate.isNotEmpty(orderHeader.getString("externalId"))) {
                orderIncrementId = orderHeader.getString("externalId");
                MagentoClient magentoClient = new MagentoClient(dispatcher, delegator);
                int isMarkedHold = magentoClient.holdSalesOrder(orderIncrementId);
                if (UtilValidate.isNotEmpty(isMarkedHold) && isMarkedHold == 1) {
                    Debug.log("Magento Order #"+ orderIncrementId+ " is marked hold successfully.");
                    result = dispatcher.runSync("updateOrderHeader", UtilMisc.toMap("orderId", orderId, "syncStatusId", "ORDER_HOLD", "userLogin", userLogin));
                    if (!ServiceUtil.isSuccess(result)) {
                        Debug.logError(ServiceUtil.getErrorMessage(result), module);
                        return ServiceUtil.returnError((ServiceUtil.getErrorMessage(result)));
                    }
                }
            }
        } catch (GenericEntityException gee) {
            Debug.logError(gee.getMessage(), module);
            return ServiceUtil.returnError(gee.getMessage());
        }  catch (Exception e) {
            Debug.logError("Error in marking order status hold in Magento. Error Message: " +e.getMessage(), module);
            e.printStackTrace();
            return ServiceUtil.returnError("Error in marking order status hold in Magento. Error Message:" +e.getMessage());
        }
        return result;
    }
    public static Map<String, Object> unholdOrderInMagento(DispatchContext dctx, Map<String, ?> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String orderIncrementId = null;
        String orderId = (String) context.get("orderId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        try {
            GenericValue orderHeader = delegator.findOne("OrderHeader", false, UtilMisc.toMap("orderId", orderId));
            if (UtilValidate.isNotEmpty(orderHeader) && "ORDER_HOLD".equals(orderHeader.getString("syncStatusId")) && UtilValidate.isNotEmpty(orderHeader.getString("externalId"))) {
                orderIncrementId = orderHeader.getString("externalId");
                MagentoClient magentoClient = new MagentoClient(dispatcher, delegator);
                int isunholded = magentoClient.unholdSalesOrder(orderIncrementId);
                if (UtilValidate.isNotEmpty(isunholded) && isunholded == 1) {
                    Debug.log("Magento Order #"+ orderIncrementId+ " is unholded successfully.");
                    result = dispatcher.runSync("updateOrderHeader", UtilMisc.toMap("orderId", orderId, "syncStatusId", "ORDER_APPROVED", "userLogin", userLogin));
                    if (!ServiceUtil.isSuccess(result)) {
                        Debug.logError(ServiceUtil.getErrorMessage(result), module);
                        return ServiceUtil.returnError((ServiceUtil.getErrorMessage(result)));
                    }
                }
            }
        } catch (GenericEntityException gee) {
            Debug.logError(gee.getMessage(), module);
            return ServiceUtil.returnError(gee.getMessage());
        }  catch (Exception e) {
            Debug.logError("Error in unholding order in Magento. Error Message: " +e.getMessage(), module);
            e.printStackTrace();
            return ServiceUtil.returnError("Error in unholding order in Magento. Error Message:" +e.getMessage());
        }
        return result;
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
        } catch (Exception e) {
            Debug.logError("Error in completing order in Magento. Error Message: " +e.getMessage(), module);
            e.printStackTrace();
            return ServiceUtil.returnError("Error in completing order in Magento. Error Message:" +e.getMessage());
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
        } catch (GenericServiceException gse) {
            Debug.logError("Getting error while configuring magento"+gse.getMessage() ,module);
            return ServiceUtil.returnError("Getting error while configuring magento "+gse.getMessage());
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
            Debug.logError("Getting error while updating inventory count in magento. Error Message: "+e.getMessage() ,module);
            e.printStackTrace();
            return ServiceUtil.returnError("Getting error while updating inventory count in magento. Error Message: "+e.getMessage());
        }
        return response;
    }
    public static Map<String, Object> checkOrderStatusInMagento (DispatchContext dctx, Map<String, ?> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String orderId = (String) context.get("orderId");
        String statusId = null;
        String message = null;
        try {
            if (UtilValidate.isNotEmpty(orderId)) {
                GenericValue system = delegator.findOne("UserLogin", false, UtilMisc.toMap("userLoginId", "system"));
                GenericValue orderHeader = delegator.findOne("OrderHeader", false, UtilMisc.toMap("orderId", orderId));
                if (UtilValidate.isNotEmpty(orderHeader) && "MAGENTO_SALE_CHANNEL".equals(orderHeader.getString("salesChannelEnumId")) && UtilValidate.isNotEmpty(orderHeader.getString("externalId"))) {
                    String orderIncrementId = orderHeader.getString("externalId");
                    MagentoClient magentoClient = new MagentoClient(dispatcher, delegator);
                    SalesOrderEntity salesOrder = magentoClient.getSalesOrderInfo(orderIncrementId);
                    if ("canceled".equalsIgnoreCase(salesOrder.getStatus())) {
                        if (!"ORDER_CANCELLED".equals(orderHeader.getString("syncStatusId"))) {
                            statusId = "ORDER_CANCELLED";
                            message = "The order with magento orderId #"+orderIncrementId+" is cancelled in Magento. So cancelling the order.";
                        }
                    } else if ("holded".equalsIgnoreCase(salesOrder.getStatus())) {
                        statusId = "ORDER_HOLD";
                        message = "The order with magento orderId #"+orderIncrementId+" is on hold in Magento. So holding the order.";
                    }
                    if (UtilValidate.isNotEmpty(statusId)) {
                        dispatcher.runSync("updateOrderHeader", UtilMisc.toMap("orderId", orderId, "syncStatusId", statusId, "userLogin", system), 0, true);
                        Map <String, Object> serviceCtx = new HashMap<String, Object>();
                        serviceCtx.put("orderId", orderId);
                        serviceCtx.put("statusId", statusId);
                        serviceCtx.put("setItemStatus", "Y");
                        serviceCtx.put("userLogin", system);
                        dispatcher.runSync("changeOrderStatus", serviceCtx, 0, true);

                        Debug.logError("The order with magento orderId #"+orderIncrementId+" is marked "+statusId+ ".", module);
                        result = ServiceUtil.returnError(message);
                    }
                }
            }
        } catch (GenericEntityException gee) {
            Debug.logError(gee.getMessage(), module);
            return ServiceUtil.returnError(gee.getMessage());
        } catch (GenericServiceException gse) {
            Debug.logError(gse.getMessage(), module);
            return ServiceUtil.returnError(gse.getMessage());
        } catch (Exception e) {
            Debug.logError("Error while checking order status in Magento. Error Message: " +e.getMessage(), module);
            e.printStackTrace();
            return ServiceUtil.returnError("Error while checking order status in Magento. Error Message: " +e.getMessage());
        }
        return result;
    }
    public static Map<String, Object> magentoIntegrationConciliation (DispatchContext dctx, Map<String, ?> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        try {
            List<Map<String, Object>> varianceList = MagentoHelper.getVariance(dispatcher, delegator);
            if (UtilValidate.isNotEmpty(varianceList)) {
                MagentoHelper.createMagentoIntegrationConciliationCSV(varianceList);
                GenericValue magentoConfiguration = EntityUtil.getFirst(delegator.findList("MagentoConfiguration", EntityCondition.makeCondition("enumId", EntityOperator.EQUALS, "MAGENTO_SALE_CHANNEL"), null, null, null, false));
                if (UtilValidate.isNotEmpty(magentoConfiguration) && UtilValidate.isNotEmpty(magentoConfiguration.getString("productStoreId"))) {
                    GenericValue productStore = delegator.findOne("ProductStore", false, UtilMisc.toMap("productStoreId", magentoConfiguration.getString("productStoreId")));
                    if (UtilValidate.isNotEmpty(productStore) && UtilValidate.isNotEmpty(productStore.getString("payToPartyId"))) {
                        result = dispatcher.runSync("getPartyEmail", UtilMisc.toMap("partyId", productStore.get("payToPartyId"), "userLogin", userLogin));
                        if (!ServiceUtil.isSuccess(result)) {
                            Debug.logError(ServiceUtil.getErrorMessage(result), module);
                            return ServiceUtil.returnError((ServiceUtil.getErrorMessage(result)));
                        }
                    }
                }

                Map<String, Object> serviceCtx = new HashMap<String, Object>();
                serviceCtx.put("userLogin", userLogin);
                serviceCtx.put("sendTo", (String) result.get("emailAddress"));
                serviceCtx.put("sendFrom", (String) result.get("emailAddress"));
                serviceCtx.put("subject", "Problem in Magento integration");

                result = dispatcher.runSync("sendMagentoIntegrationConciliationMail", serviceCtx);
                if (!ServiceUtil.isSuccess(result)) {
                    Debug.logError(ServiceUtil.getErrorMessage(result), module);
                    return ServiceUtil.returnError((ServiceUtil.getErrorMessage(result)));
                }
                result.clear();
            } else {
                Debug.logInfo("Sales order synchronization process is consistent.", module);
            }
        } catch (GenericServiceException gse) {
            Debug.logInfo(gse.getMessage(), module);
        } catch (GenericEntityException gee) {
            Debug.logInfo(gee.getMessage(), module);
        }
        result = ServiceUtil.returnSuccess();
        return result;
    }
    public static Map<String, Object> sendMagentoIntegrationConciliationMail (DispatchContext dctx, Map<String, ?> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String sendTo = (String) context.get("sendTo");
        String sendFrom = (String) context.get("sendFrom");
        String subject = (String) context.get("subject");
        try {
            Map<String, Object> sendMailContext = new HashMap<String, Object>();
            sendMailContext.put("sendTo", sendTo);
            sendMailContext.put("sendFrom", sendFrom);
            sendMailContext.put("subject", subject);
            sendMailContext.put("userLogin", userLogin);
            String messageText = "Magento integration process is inconsistent. PFA for more detail.";
            String attachmentName = "SalesOrderConciliation.csv";
            List<Map<String, Object>> bodyParts = new ArrayList<Map<String,Object>>();

            bodyParts.add(UtilMisc.<String, Object>toMap("content", messageText, "type", "text/plain"));

            File fileOut = new File(System.getProperty("ofbiz.home")+"/runtime/magento/MagentoIntegrationConciliation.csv");
            FileInputStream fis = new FileInputStream(fileOut);
            int c;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while ((c=fis.read()) > -1) {
                baos.write(c);
            }
            fis.close();
            baos.close();
            if (UtilValidate.isNotEmpty(baos.toByteArray())) {
                bodyParts.add(UtilMisc.<String, Object>toMap("content", baos.toByteArray(), "type", "text/csv", "filename", attachmentName));
            }
            sendMailContext.put("bodyParts", bodyParts);

            result = dispatcher.runSync("sendMailMultiPart", sendMailContext);
            if (!ServiceUtil.isSuccess(result)) {
                Debug.logError(ServiceUtil.getErrorMessage(result), module);
                return ServiceUtil.returnError((ServiceUtil.getErrorMessage(result)));
            }
        } catch (GenericServiceException gse) {
            Debug.logInfo(gse.getMessage(), module);
            return ServiceUtil.returnError(gse.getMessage());
        } catch (IOException e) {
            Debug.logInfo(e.getMessage(), module);
            e.printStackTrace();
        }
        result = ServiceUtil.returnSuccess();
        return result;
    }
}