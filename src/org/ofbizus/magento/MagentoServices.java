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

import javolution.util.FastMap;

import org.apache.xmlrpc.XmlRpcException;
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

public class MagentoServices {
    public static final String module = MagentoClient.class.getName();

    // Import orders from magento
    public Map<String, Object> importOrdersFromMagento(DispatchContext dctx, Map<String, ?> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        String magOrderId = (String) context.get("orderId");
        String statusId = (String) context.get("statusId");

        Timestamp fromDate = (Timestamp) context.get("fromDate");
        Timestamp thruDate = (Timestamp) context.get("thruDate");

        DateFormat df = new SimpleDateFormat("yyyy-mm-dd HH:mm:ss");
        Date from = (Date) fromDate;
        Date thru = (Date) thruDate;
        String createdFrom = null;
        String createdTo = null;
        if (UtilValidate.isNotEmpty(from)) {
            createdFrom = df.format(from);
        }
        if (UtilValidate.isNotEmpty(thru)) {
            createdTo = df.format(thru);
        }

        Map<String, Object> condMap = new HashMap<String, Object>();
        if (UtilValidate.isNotEmpty(magOrderId)) {
            Map<String, String> orderIdCondMap = UtilMisc.toMap("eq", magOrderId);
            condMap.put("increment_id", orderIdCondMap);
        }
        Map<String, String> statusCondMap = FastMap.newInstance();
        if (UtilValidate.isNotEmpty(statusId)) {
            statusCondMap = UtilMisc.toMap("eq", statusId);
        } else {
            statusCondMap = UtilMisc.toMap("eq", "pending");
        }
        condMap.put("status", statusCondMap);
        Map<String, String> createdDateCondMap = FastMap.newInstance();
        if (UtilValidate.isNotEmpty(createdFrom)) {
            createdDateCondMap = UtilMisc.toMap("from", createdFrom);
        }
        if (UtilValidate.isNotEmpty(createdTo)) {
            createdDateCondMap = UtilMisc.toMap("to", createdTo);
        }
        if (UtilValidate.isNotEmpty(createdFrom) || UtilValidate.isNotEmpty(createdTo)) {
            condMap.put("created_at", createdDateCondMap);
        }
        MagentoClient magentoClient = new MagentoClient(dispatcher, delegator);
        Object[] responseMessage = magentoClient.getSalesOrderList(condMap);

        for (Object orderInformation : responseMessage) {
            Map<String, Object> orderInfo = (Map<String, Object>)orderInformation;
            Object salesOrderInformation = magentoClient.getSalesOrderInfo((String)orderInfo.get("increment_id"));
            Map<String, Object> salesOrderInfo = (Map<String, Object>)salesOrderInformation;
            String externalId = (String) salesOrderInfo.get("externalId");
            if (UtilValidate.isNotEmpty(externalId)) {
                    // Check if order already imported.
            } else {
                //Create new order.
            }
        }
        return result;
    }
}