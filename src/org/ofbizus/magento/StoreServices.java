package org.ofbizus.magento;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.party.party.PartyWorker;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;

public class StoreServices {
    public static final String module = MagentoServices.class.getName();
    public static final String resource = "MagentoUiLabels";

    public static Map<String, Object> createUpdateCompany(DispatchContext dctx, Map<String, Object>context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Map<String, Object> result = new HashMap<String, Object>();
        Map<String, Object> serviceCtx = new HashMap<String, Object>();
        Map<String, String> contactNumberMap = new HashMap<String, String>();
        Locale locale = (Locale) context.get("locale");
        String partyId = (String) context.get("partyId");
        String postalContactMechId = (String) context.get("postalContactMechId");
        String emailContactMechId = (String) context.get("emailContactMechId");
        String telecomContactMechId = (String) context.get("telecomContactMechId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        try {
            // Create Company
            serviceCtx.put("groupName", (String) context.get("groupName"));
            serviceCtx.put("userLogin", userLogin);
            if (UtilValidate.isNotEmpty(partyId)) {
                serviceCtx.put("partyId", partyId);
                result = dispatcher.runSync("updatePartyGroup", serviceCtx);
                if (!ServiceUtil.isSuccess(result)) {
                    Debug.logError(ServiceUtil.getErrorMessage(result), module);
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                }
            } else {
                result = dispatcher.runSync("createPartyGroup", serviceCtx);
                if (!ServiceUtil.isSuccess(result)) {
                    Debug.logError(ServiceUtil.getErrorMessage(result), module);
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                }
                partyId = (String) result.get("partyId");

                serviceCtx.clear();
                //Create Company roles
                List<String> companyRoles = new ArrayList<String>();
                companyRoles.add("BILL_FROM_VENDOR");
                companyRoles.add("BILL_TO_CUSTOMER");
                companyRoles.add("INTERNAL_ORGANIZATIO");
                companyRoles.add("PARENT_ORGANIZATION");
                serviceCtx.put("userLogin", userLogin);
                serviceCtx.put("partyId", partyId);
                for (String companyRole : companyRoles) {
                    serviceCtx.put("roleTypeId", companyRole);
                    result = dispatcher.runSync("createPartyRole", serviceCtx);
                    if(!ServiceUtil.isSuccess(result)) {
                        Debug.logError(ServiceUtil.getErrorMessage(result), module);
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                    }
                }
            }

            serviceCtx.clear();
            result.clear();
            if (UtilValidate.isNotEmpty(postalContactMechId)) {
                serviceCtx = dctx.getModelService("updatePartyPostalAddress").makeValid(context, ModelService.IN_PARAM);
                serviceCtx.put("partyId", partyId);
                serviceCtx.put("contactMechId", postalContactMechId);
                result = dispatcher.runSync("updatePartyPostalAddress", serviceCtx);
                if (!ServiceUtil.isSuccess(result)) {
                    Debug.logError(ServiceUtil.getErrorMessage(result), module);
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                }
            } else {
                // Create Company Postal Address.
                serviceCtx = dctx.getModelService("createPartyPostalAddress").makeValid(context, ModelService.IN_PARAM);
                serviceCtx.put("partyId", partyId);
                result = dispatcher.runSync("createPartyPostalAddress", serviceCtx);
                if (!ServiceUtil.isSuccess(result)) {
                    Debug.logError(ServiceUtil.getErrorMessage(result), module);
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                }
                postalContactMechId = (String) result.get("contactMechId");
    
                //Create postal address purposes
                List<String> postalContactMechPurposes = new ArrayList<String>();
                postalContactMechPurposes.add("BILLING_LOCATION");
                postalContactMechPurposes.add("GENERAL_LOCATION");
                postalContactMechPurposes.add("PAYMENT_LOCATION");
    
                serviceCtx.clear();
                serviceCtx.put("partyId", partyId);
                serviceCtx.put("contactMechId", postalContactMechId);
                serviceCtx.put("userLogin", userLogin);
                for(String postalContactMechPurpose : postalContactMechPurposes) {
                    serviceCtx.put("contactMechPurposeTypeId", postalContactMechPurpose);
                    result = dispatcher.runSync("createPartyContactMechPurpose", serviceCtx);
                    if(!ServiceUtil.isSuccess(result)) {
                        Debug.logError(ServiceUtil.getErrorMessage(result), module);
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                    }
                }
            }

            result.clear();
            serviceCtx.clear();
            if (UtilValidate.isNotEmpty(context.get("infoString"))) {
                // Create Company Email Address.
                serviceCtx.put("partyId", partyId);
                serviceCtx.put("emailAddress", (String) context.get("infoString"));
                serviceCtx.put("userLogin", userLogin);
                if (UtilValidate.isNotEmpty(emailContactMechId)) {
                    serviceCtx.put("contactMechId", emailContactMechId);
                    result = dispatcher.runSync("updatePartyEmailAddress", serviceCtx);
                    if (!ServiceUtil.isSuccess(result)) {
                        Debug.logError(ServiceUtil.getErrorMessage(result), module);
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                    }
                } else  {
                    result = dispatcher.runSync("createPartyEmailAddress", serviceCtx);
                    if (!ServiceUtil.isSuccess(result)) {
                        Debug.logError(ServiceUtil.getErrorMessage(result), module);
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                    }
                    emailContactMechId = (String) result.get("contactMechId");

                  //Create email purposes 
                    serviceCtx.clear();
                    serviceCtx.put("partyId", partyId);
                    serviceCtx.put("userLogin", userLogin);
                    serviceCtx.put("contactMechId", emailContactMechId);
                    serviceCtx.put("contactMechPurposeTypeId", "PRIMARY_EMAIL");
                    result = dispatcher.runSync("createPartyContactMechPurpose",serviceCtx);
                    if (!ServiceUtil.isSuccess(result)) {
                        Debug.logError(ServiceUtil.getErrorMessage(result), module);
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                    }
                }
            }

            serviceCtx.clear();
            if (UtilValidate.isNotEmpty(context.get("contactNumber"))) {
                contactNumberMap = MagentoHelper.getMapForContactNumber((String) context.get("contactNumber"));
                serviceCtx = dctx.getModelService("createPartyTelecomNumber").makeValid(contactNumberMap, ModelService.IN_PARAM);
                serviceCtx.put("partyId", partyId);
                serviceCtx.put("userLogin", userLogin);
                if (UtilValidate.isNotEmpty(telecomContactMechId)) {
                    serviceCtx.put("contactMechId", telecomContactMechId);
                    result = dispatcher.runSync("updatePartyTelecomNumber", serviceCtx);
                } else {
                    serviceCtx.put("contactMechPurposeTypeId", "PRIMARY_PHONE");
                    result = dispatcher.runSync("createPartyTelecomNumber", serviceCtx);
                }
                if (!ServiceUtil.isSuccess(result)) {
                    Debug.logError(ServiceUtil.getErrorMessage(result), module);
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                }
            } else if (UtilValidate.isNotEmpty(telecomContactMechId)) {
                //Remove previous contact number if any
                serviceCtx.clear();
                serviceCtx.put("userLogin", userLogin);
                serviceCtx.put("partyId", partyId);
                serviceCtx.put("contactMechId", telecomContactMechId);
                result = dispatcher.runSync("updatePartyTelecomNumber", serviceCtx);
                if (ServiceUtil.isError(result)) {
                    Debug.logError(ServiceUtil.getErrorMessage(result), module);
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                }
            }
            result = ServiceUtil.returnSuccess(UtilProperties.getMessage(resource, "MagentoCompanyIsCreatedSuccessfully", locale));
            result.put("partyId", partyId);
        } catch (GenericServiceException e) {
            Debug.logError(e.getMessage(), module);
            ServiceUtil.returnError(e.getMessage());
        }
        return result;
    }

    public static Map<String, Object> createUpdateProductStore(DispatchContext dctx, Map<String, Object>context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> result = new HashMap<String, Object>();
        Map<String, Object> serviceCtx = new HashMap<String, Object>();
        Locale locale = (Locale) context.get("locale");
        String partyId = (String) context.get("partyId");
        String productStoreId = (String) context.get("productStoreId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        try {
            if (UtilValidate.isNotEmpty(productStoreId)) {
                GenericValue productStore = delegator.findOne("ProductStore", false, UtilMisc.toMap("productStoreId", productStoreId));
                if (UtilValidate.isNotEmpty(productStore)) {
                    serviceCtx = dctx.getModelService("updateProductStore").makeValid(productStore, ModelService.IN_PARAM);
                    serviceCtx.put("storeName", (String) context.get("storeName"));
                    serviceCtx.put("userLogin", userLogin);
                    result = dispatcher.runSync("updateProductStore", serviceCtx);
                    if (!ServiceUtil.isSuccess(result)) {
                        Debug.logError(ServiceUtil.getErrorMessage(result), module);
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                    }
                    result = ServiceUtil.returnSuccess(UtilProperties.getMessage(resource, "MagentoStoreIsUpdatedSuccessfully", locale));
                    result.put("partyId", partyId);
                    result.put("productStoreId", productStoreId);
                }
            } else {
                serviceCtx = dctx.getModelService("createProductStore").makeValid(context, ModelService.IN_PARAM);

                //Add basic setting values for product store
                serviceCtx.put("isDemoStore", "N");
                serviceCtx.put("requireInventory", "N");
                serviceCtx.put("isImmediatelyFulfilled", "N");
                serviceCtx.put("prodSearchExcludeVariants", "Y");
                serviceCtx.put("shipIfCaptureFails", "N");
                serviceCtx.put("retryFailedAuths", "Y");
                serviceCtx.put("explodeOrderItems", "N");
                serviceCtx.put("checkGcBalance", "Y");
                serviceCtx.put("autoApproveInvoice", "Y");
                serviceCtx.put("autoApproveOrder", "Y");
                serviceCtx.put("autoApproveReviews", "N");
                serviceCtx.put("allowPassword", "Y");
                serviceCtx.put("usePrimaryEmailUsername", "Y");
                serviceCtx.put("manualAuthIsCapture", "N");
                serviceCtx.put("requireCustomerRole", "Y");
                serviceCtx.put("daysToCancelNonPay", Long.valueOf("0"));
                serviceCtx.put("storeCreditAccountEnumId", "FIN_ACCOUNT");
                serviceCtx.put("defaultSalesChannelEnumId", "WEB_SALES_CHANNEL");
                serviceCtx.put("reqReturnInventoryReceive", "Y");
                serviceCtx.put("headerApprovedStatus", "ORDER_APPROVED");
                serviceCtx.put("itemApprovedStatus", "ITEM_APPROVED");
                serviceCtx.put("digitalItemApprovedStatus", "ITEM_APPROVED");
                serviceCtx.put("headerDeclinedStatus", "ORDER_REJECTED");
                serviceCtx.put("itemDeclinedStatus", "ITEM_REJECTED");
                serviceCtx.put("headerCancelStatus", "ORDER_CANCELLED");
                serviceCtx.put("itemCancelStatus", "ITEM_CANCELLED");

                serviceCtx.put("autoSaveCart", "N");
                serviceCtx.put("showCheckoutGiftOptions", "N");
                serviceCtx.put("prorateShipping", "N");
                serviceCtx.put("prorateTaxes", "N");
                serviceCtx.put("checkInventory", "Y");
                serviceCtx.put("balanceResOnOrderCreation", "N");
                serviceCtx.put("payToPartyId", partyId);
                serviceCtx.put("defaultCurrencyUomId", "USD");
                result = dispatcher.runSync("createProductStore", serviceCtx);
                if (!ServiceUtil.isSuccess(result)) {
                    Debug.logError(ServiceUtil.getErrorMessage(result), module);
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                }
                productStoreId = (String) result.get("productStoreId");
                if (UtilValidate.isNotEmpty(productStoreId)) {
                    GenericValue magentoConfiguration = EntityUtil.getFirst(delegator.findList("MagentoConfiguration", EntityCondition.makeCondition("enumId", "MAGENTO_SALE_CHANNEL"), null, null, null, false));
                    serviceCtx = dctx.getModelService("createUpdateMagentoConfiguration").makeValid(magentoConfiguration, ModelService.IN_PARAM);
                    serviceCtx.put("enumId", "MAGENTO_SALE_CHANNEL");
                    serviceCtx.put("productStoreId", productStoreId);
                    serviceCtx.put("userLogin", userLogin);
                    result = dispatcher.runSync("createUpdateMagentoConfiguration", serviceCtx);
                    if (!ServiceUtil.isSuccess(result)) {
                        Debug.logError(ServiceUtil.getErrorMessage(result), module);
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                    }
                }
                result = ServiceUtil.returnSuccess(UtilProperties.getMessage(resource, "MagentoStoreIsCreatedSuccessfully", locale));
                result.put("partyId", partyId);
                result.put("productStoreId", productStoreId);
            }
        } catch (GenericServiceException e) {
             Debug.logError(e.getMessage(), module);
        } catch (GenericEntityException gee) {
            Debug.logError(gee.getMessage(), module);
        }
        return result;
    }
    public static Map<String, Object> createUpdateStoreInformation(DispatchContext dctx, Map<String, Object>context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Map<String, Object> result = new HashMap<String, Object>();
        Map<String, Object> serviceCtx = new HashMap<String, Object>();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        try {
            serviceCtx = dctx.getModelService("createUpdateCompany").makeValid(context, ModelService.IN_PARAM);
            result = dispatcher.runSync("createUpdateCompany", serviceCtx);
            if (!ServiceUtil.isSuccess(result)) {
                Debug.logError(ServiceUtil.getErrorMessage(result), module);
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
            }
            String partyId = (String) result.get("partyId");
            if (UtilValidate.isNotEmpty(partyId)) {
                serviceCtx = dctx.getModelService("createUpdateProductStore").makeValid(context, ModelService.IN_PARAM);
                serviceCtx.put("partyId", partyId);
                result = dispatcher.runSync("createUpdateProductStore", serviceCtx);
                if (!ServiceUtil.isSuccess(result)) {
                    Debug.logError(ServiceUtil.getErrorMessage(result), module);
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                }
                serviceCtx.clear();
                serviceCtx.put("userLogin", userLogin);
                serviceCtx.put("partyId", partyId);
                result = dispatcher.runSync("setupDefaultGeneralLedger", serviceCtx);
                if (!ServiceUtil.isSuccess(result)) {
                    Debug.logError(ServiceUtil.getErrorMessage(result), module);
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                }
            }
        } catch (GenericServiceException gse) {
            Debug.logInfo(gse.getMessage(), module);
            ServiceUtil.returnError(gse.getMessage());
        }
        return ServiceUtil.returnSuccess(UtilProperties.getMessage(resource, "MagentoStoreInformationIsUpdatedSuccessfully", locale));
    }
    public static Map<String, Object> createWarehouse (DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> result = new HashMap<String, Object>();
        Map<String, Object> serviceCtx = new HashMap<String, Object>();
        Map<String, String> contactNumberMap = new HashMap<String, String>();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String partyId = (String) context.get("partyId");
        String productStoreId = (String) context.get("productStoreId");
        String facilityId = null;

        try {
            serviceCtx = dctx.getModelService("createFacility").makeValid(context, ModelService.IN_PARAM);
            serviceCtx.put("facilityTypeId", "WAREHOUSE");
            serviceCtx.put("ownerPartyId", partyId);
            result = dispatcher.runSync("createFacility", serviceCtx);
            if (!ServiceUtil.isSuccess(result)) {
                Debug.logError(ServiceUtil.getErrorMessage(result), module);
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
            }
            facilityId = (String) result.get("facilityId");
            serviceCtx.clear();
            result.clear();

            serviceCtx.put("facilityId", facilityId);
            serviceCtx.put("locationSeqId", "TLTLTLLL05");
            serviceCtx.put("locationTypeEnumId", "FLT_PICKLOC");
            serviceCtx.put("userLogin", userLogin);
            result = dispatcher.runSync("createFacilityLocation", serviceCtx);
            if (!ServiceUtil.isSuccess(result)) {
                Debug.logError(ServiceUtil.getErrorMessage(result), module);
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
            }

            serviceCtx.put("productStoreId", productStoreId);
            serviceCtx.put("inventoryFacilityId", facilityId);
            serviceCtx.put("userLogin", userLogin);
            if (UtilValidate.isEmpty(context.get("checkInventory"))) {
                serviceCtx.put("checkInventory", "Y");
            } else {
                serviceCtx.put("checkInventory", context.get("checkInventory"));
            }
            if (UtilValidate.isEmpty(context.get("balanceResOnOrderCreation"))) {
                serviceCtx.put("balanceResOnOrderCreation", "N");
            } else {
                serviceCtx.put("balanceResOnOrderCreation", context.get("balanceResOnOrderCreation"));
            }
            serviceCtx.put("reserveOrderEnumId", context.get("reserveOrderEnumId"));
            result = dispatcher.runSync("updateProductStore", serviceCtx);
            if (!ServiceUtil.isSuccess(result)) {
                Debug.logError(ServiceUtil.getErrorMessage(result), module);
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
            }
            serviceCtx.clear();
            result.clear();

            serviceCtx.put("productStoreId", productStoreId);
            serviceCtx.put("facilityId", facilityId);
            serviceCtx.put("userLogin", userLogin);
            serviceCtx.put("fromDate", UtilDateTime.nowTimestamp());
            result = dispatcher.runSync("createProductStoreFacility", serviceCtx);
            if (!ServiceUtil.isSuccess(result)) {
                Debug.logError(ServiceUtil.getErrorMessage(result), module);
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
            }
            serviceCtx.clear();
            result.clear();

            if (UtilValidate.isNotEmpty(context.get("isWarehoueAddressSameAsCompanyAddress"))) {
                GenericValue postalAddress = PartyWorker.findPartyLatestPostalAddress(partyId, delegator);
                serviceCtx = dctx.getModelService("createFacilityPostalAddress").makeValid(UtilMisc.toMap(postalAddress), ModelService.IN_PARAM);
                serviceCtx.remove("contactMechId");
            } else {
                serviceCtx = dctx.getModelService("createFacilityPostalAddress").makeValid(context, ModelService.IN_PARAM);
            }

            serviceCtx.put("userLogin", userLogin);
            serviceCtx.put("facilityId", facilityId);
            serviceCtx.put("toName", (String) context.get("facilityName"));
            result = dispatcher.runSync("createFacilityPostalAddress", serviceCtx);
            if (!ServiceUtil.isSuccess(result)) {
                Debug.logError(ServiceUtil.getErrorMessage(result), module);
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
            }
            String postalContactMechId = (String) result.get("contactMechId");
            serviceCtx.clear();
            result.clear();

            //Create Facility Postal Contact Mech Purpose.
            serviceCtx.put("contactMechId", postalContactMechId);
            serviceCtx.put("facilityId", facilityId);
            serviceCtx.put("userLogin", userLogin);

            List<String> postalContactMechPurpose = new ArrayList<String>();
            postalContactMechPurpose.add("SHIP_ORIG_LOCATION");
            postalContactMechPurpose.add("SHIPPING_LOCATION");
            for(String contactMechPurpose : postalContactMechPurpose) {
                serviceCtx.put("contactMechPurposeTypeId", contactMechPurpose);
                result = dispatcher.runSync("createFacilityContactMechPurpose", serviceCtx);
                if(!ServiceUtil.isSuccess(result)) {
                    Debug.logError(ServiceUtil.getErrorMessage(result), module);
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                }
            }
            serviceCtx.clear();

            if (UtilValidate.isNotEmpty(context.get("contactNumber"))) {
                //Create Facility Telecom Number.
                contactNumberMap = MagentoHelper.getMapForContactNumber((String) context.get("contactNumber"));
                serviceCtx = dctx.getModelService("createFacilityTelecomNumber").makeValid(contactNumberMap, ModelService.IN_PARAM);
                serviceCtx.put("facilityId", facilityId);
                serviceCtx.put("userLogin", userLogin);
                serviceCtx.put("contactMechPurposeTypeId", "PRIMARY_PHONE");
                result = dispatcher.runSync("createFacilityTelecomNumber", serviceCtx);
                if(!ServiceUtil.isSuccess(result)) {
                    Debug.logError(ServiceUtil.getErrorMessage(result), module);
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                }
                String telecomContactMechId = (String) result.get("contactMechId");
                serviceCtx.clear();
                contactNumberMap.clear();

                //Create telecom contact mech purposes
                serviceCtx.put("userLogin", userLogin);
                serviceCtx.put("facilityId", facilityId);
                serviceCtx.put("contactMechId", telecomContactMechId);
                List<String> telecomContactMechPurpose = new ArrayList<String>();
                telecomContactMechPurpose.add("PHONE_SHIPPING");
                telecomContactMechPurpose.add("PHONE_SHIP_ORIG");
                for(String contactMechPurpose : telecomContactMechPurpose) {
                    serviceCtx.put("contactMechPurposeTypeId", contactMechPurpose);
                    result = dispatcher.runSync("createFacilityContactMechPurpose", serviceCtx);
                    if(!ServiceUtil.isSuccess(result)) {
                        Debug.logError(ServiceUtil.getErrorMessage(result), module);
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                    }
                }
            }
            serviceCtx.clear();

            result = ServiceUtil.returnSuccess(UtilProperties.getMessage(resource, "MagentoFacilityInformationIsAddedSuccessfully", locale));
            result.put("facilityId", facilityId);
            result.put("partyId", partyId);
            result.put("productStoreId", productStoreId);
        
        } catch (GenericServiceException e) {
            Debug.logError(e.getMessage(), module);
        }
        return result;
    }

    public static Map<String, Object> updateWarehouse (DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Map<String, Object> result = new HashMap<String, Object>();
        Map<String, Object> serviceCtx = new HashMap<String, Object>();
        Map<String, String> contactNumberMap = new HashMap<String, String>();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String partyId = (String) context.get("partyId");
        String productStoreId = (String) context.get("productStoreId");
        String facilityId = (String) context.get("facilityId");
        String postalContactMechId = (String) context.get("facilityPostalContactMechId");
        String telecomContactMechId = (String) context.get("facilityTelecomContactMechId");

        try {
            serviceCtx = dctx.getModelService("updateFacility").makeValid(context, ModelService.IN_PARAM);
            serviceCtx.put("ownerPartyId", partyId);
            result = dispatcher.runSync("updateFacility", serviceCtx);
            if (!ServiceUtil.isSuccess(result)) {
                Debug.logError(ServiceUtil.getErrorMessage(result), module);
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
            }
            serviceCtx.clear();
            result.clear();
            if (UtilValidate.isNotEmpty(postalContactMechId)) {
                serviceCtx = dctx.getModelService("updateFacilityPostalAddress").makeValid(context, ModelService.IN_PARAM);
                serviceCtx.put("contactMechId", postalContactMechId);
                serviceCtx.put("toName", (String) context.get("facilityName"));
                serviceCtx.put("userLogin", userLogin);
                result = dispatcher.runSync("updateFacilityPostalAddress", serviceCtx);
                if (!ServiceUtil.isSuccess(result)) {
                    Debug.logError(ServiceUtil.getErrorMessage(result), module);
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                }
            }
            serviceCtx.clear();
            result.clear();
            if (UtilValidate.isNotEmpty(context.get("contactNumber"))) {
                //Create Facility Telecom Number.
                contactNumberMap = MagentoHelper.getMapForContactNumber((String) context.get("contactNumber"));
                serviceCtx = dctx.getModelService("createFacilityTelecomNumber").makeValid(contactNumberMap, ModelService.IN_PARAM);
                serviceCtx.put("facilityId", facilityId);
                serviceCtx.put("userLogin", userLogin);
                if (UtilValidate.isNotEmpty(telecomContactMechId)) {
                    serviceCtx.put("contactMechId", telecomContactMechId);
                    result = dispatcher.runSync("updateFacilityTelecomNumber", serviceCtx);
                } else {
                    result = dispatcher.runSync("createFacilityTelecomNumber", serviceCtx);
                }
                if (!ServiceUtil.isSuccess(result)) {
                    Debug.logError(ServiceUtil.getErrorMessage(result), module);
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                }
                serviceCtx.clear();
                contactNumberMap.clear();
            } else if (UtilValidate.isNotEmpty(telecomContactMechId)) {
                //Remove previous contact number if any
                serviceCtx.put("facilityId", facilityId);
                serviceCtx.put("contactMechId", telecomContactMechId);
                serviceCtx.put("userLogin", userLogin);
                result = dispatcher.runSync("deleteFacilityContactMech", serviceCtx);
                if (!ServiceUtil.isSuccess(result)) {
                    Debug.logError(ServiceUtil.getErrorMessage(result), module);
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                }
            }
            result = ServiceUtil.returnSuccess(UtilProperties.getMessage(resource, "MagentoFacilityInformationIsUpdatedSuccessfully", locale));
            result.put("facilityId", facilityId);
            result.put("partyId", partyId);
            result.put("productStoreId", productStoreId);
        } catch (GenericServiceException gse) {
            Debug.logError(gse.getMessage(), module);
        }
        return result;
    }
    public static Map<String, Object> createRemoveProductStoreShipMeth (DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Map<String, Object> result = new HashMap<String, Object>();
        Map<String, Object> serviceCtx = new HashMap<String, Object>();
        String productStoreShipMethId = (String) context.get("productStoreShipMethId");
        Locale locale = (Locale) context.get("locale");
        List<String> shipmentMethodTypeIdList = UtilGenerics.toList(context.get("shipmentMethodTypeId"));
        String successMessage = null;
        try {
            if (UtilValidate.isNotEmpty(productStoreShipMethId)) {
                serviceCtx = dctx.getModelService("removeProductStoreShipMeth").makeValid(context, ModelService.IN_PARAM);
                dispatcher.runSync("removeProductStoreShipMeth", serviceCtx);
                successMessage = UtilProperties.getMessage(resource, "MagentoShippingMethodIsRemovedSuccessfully", locale);
            } else {
                if (UtilValidate.isNotEmpty(shipmentMethodTypeIdList)) {
                    for (String shipmentMethodTypeId : shipmentMethodTypeIdList) {
                        serviceCtx = dctx.getModelService("createProductStoreShipMeth").makeValid(context, ModelService.IN_PARAM);
                        serviceCtx.put("shipmentMethodTypeId", shipmentMethodTypeId);
                        result = dispatcher.runSync("createProductStoreShipMeth", serviceCtx);
                    }
                    successMessage = UtilProperties.getMessage(resource, "MagentoShippingMethodsAreAddedSuccessfully", locale);
                }
                
            }
            if (!ServiceUtil.isSuccess(result)) {
                Debug.logError(ServiceUtil.getErrorMessage(result), module);
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
            }
        } catch (GenericServiceException gse) {
            Debug.logInfo(gse.getMessage(), module);
            ServiceUtil.returnError(gse.getMessage());
        }
        return ServiceUtil.returnSuccess(successMessage);
    }
    public static Map<String, Object> createUpdateShipmentGatewayConfig (DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Map<String, Object> result = new HashMap<String, Object>();
        Map<String, Object> serviceCtx = new HashMap<String, Object>();
        String shipmentGatewayConfigId = (String) context.get("shipmentGatewayConfigId");
        String carrierPartyId = (String) context.get("carrierPartyId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        String serviceName = null;
        String shipmentGatewayConfTypeId = null;
        try {
            if (UtilValidate.isNotEmpty(shipmentGatewayConfigId)) {
                if (UtilValidate.isNotEmpty(carrierPartyId)) {
                    if ("DHL".equalsIgnoreCase(carrierPartyId)) {
                        serviceName = "updateShipmentGatewayConfigDhl";
                    } else if ("FEDEX".equalsIgnoreCase(carrierPartyId)) {
                        serviceName = "updateShipmentGatewayConfigFedex";
                    } else if ("UPS".equalsIgnoreCase(carrierPartyId)) {
                        serviceName = "updateShipmentGatewayConfigUps";
                    } else if ("USPS".equalsIgnoreCase(carrierPartyId)) {
                        serviceName = "updateShipmentGatewayConfigUsps";
                    }
                    serviceCtx = dctx.getModelService(serviceName).makeValid(context, ModelService.IN_PARAM);
                    result = dispatcher.runSync(serviceName, serviceCtx);
                }
            } else {
                if (UtilValidate.isNotEmpty(carrierPartyId)) {
                    if ("DHL".equalsIgnoreCase(carrierPartyId)) {
                        shipmentGatewayConfTypeId = "DHL";
                        serviceName = "createShipmentGatewayConfigDhl";
                    } else if ("FEDEX".equalsIgnoreCase(carrierPartyId)) {
                        shipmentGatewayConfTypeId = "FEDEX";
                        serviceName = "createShipmentGatewayConfigFedex";
                    } else if ("UPS".equalsIgnoreCase(carrierPartyId)) {
                        shipmentGatewayConfTypeId = "UPS";
                        serviceName = "createShipmentGatewayConfigUps";
                    } else if ("USPS".equalsIgnoreCase(carrierPartyId)) {
                        shipmentGatewayConfTypeId = "USPS";
                        serviceName = "createShipmentGatewayConfigUsps";
                    }
                    serviceCtx.put("shipmentGatewayConfigId", shipmentGatewayConfigId);
                    serviceCtx.put("shipmentGatewayConfTypeId", shipmentGatewayConfTypeId);
                    serviceCtx.put("description", carrierPartyId);
                    serviceCtx.put("userLogin", userLogin);
                    result = dispatcher.runSync("createShipmentGatewayConfig", serviceCtx);

                    if (!ServiceUtil.isSuccess(result)) {
                        Debug.logError(ServiceUtil.getErrorMessage(result), module);
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                    }
                    shipmentGatewayConfigId = (String) result.get("shipmentGatewayConfigId");
                    if (UtilValidate.isNotEmpty(shipmentGatewayConfigId)) {
                        serviceCtx.clear();
                        serviceCtx = dctx.getModelService(serviceName).makeValid(context, ModelService.IN_PARAM);
                        serviceCtx.put("shipmentGatewayConfigId", shipmentGatewayConfigId);
                        result = dispatcher.runSync(serviceName, serviceCtx);
                    }
                }
            }
            if (!ServiceUtil.isSuccess(result)) {
                Debug.logError(ServiceUtil.getErrorMessage(result), module);
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
            }
        } catch (GenericServiceException gse) {
            Debug.logInfo(gse.getMessage(), module);
            return ServiceUtil.returnError(gse.getMessage());
        }
        return ServiceUtil.returnSuccess(UtilProperties.getMessage(resource, "MagentoShippingGatewayConfigurationIsUpdatedSuccessfully", locale));
    }
    public static Map<String, Object> setupDefaultGeneralLedger (DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> result = new HashMap<String, Object>();
        Map<String, Object> serviceCtx = new HashMap<String, Object>();
        String partyId = (String) context.get("partyId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        try {
            if (UtilValidate.isNotEmpty(partyId)) {
                EntityCondition cond = EntityCondition.makeCondition (
                        EntityCondition.makeCondition("organizationPartyId", partyId),
                        EntityCondition.makeCondition("glJournalName", "Suspense transactions")
                        );
                List<GenericValue> glJournalList = delegator.findList("GlJournal", cond, null, null, null, false);
                if (UtilValidate.isEmpty(glJournalList)) {
                    serviceCtx.put("glJournalName", "Suspense transactions");
                    serviceCtx.put("organizationPartyId", partyId);
                    serviceCtx.put("userLogin", userLogin);
                    result = dispatcher.runSync("createGlJournal", serviceCtx);
                    if (!ServiceUtil.isSuccess(result)) {
                        Debug.logError(ServiceUtil.getErrorMessage(result), module);
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                    }
                }

                GenericValue partyAcctgPreference = delegator.findOne("PartyAcctgPreference", false, UtilMisc.toMap("partyId", partyId));
                if (UtilValidate.isEmpty(partyAcctgPreference)) {
                    serviceCtx.clear();
                    serviceCtx.put("partyId", partyId);
                    serviceCtx.put("taxFormId", "US_IRS_1120");
                    serviceCtx.put("cogsMethodId", "COGS_AVG_COST");
                    serviceCtx.put("invoiceSequenceEnumId", "INVSQ_ENF_SEQ");
                    serviceCtx.put("invoiceIdPrefix", "CI");
                    serviceCtx.put("quoteSequenceEnumId", "INVSQ_ENF_SEQ");
                    serviceCtx.put("quoteIdPrefix", "CQ");
                    serviceCtx.put("orderSequenceEnumId", "INVSQ_ENF_SEQ");
                    serviceCtx.put("orderIdPrefix", "CO");
                    serviceCtx.put("orderIdPrefix", "CO");
                    serviceCtx.put("baseCurrencyUomId", "USD");
                    serviceCtx.put("userLogin", userLogin);
                    result = dispatcher.runSync("createPartyAcctgPreference", serviceCtx);
                    if (!ServiceUtil.isSuccess(result)) {
                        Debug.logError(ServiceUtil.getErrorMessage(result), module);
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                    }
                }

                URL outputPath = MagentoHelper.getTempDataFileUrlToImport(delegator, partyId);
                if (UtilValidate.isNotEmpty(outputPath)) {
                    result = dispatcher.runSync("parseEntityXmlFile", UtilMisc.toMap("url", outputPath, "userLogin", userLogin));
                    if (!ServiceUtil.isSuccess(result)) {
                        Debug.logError(ServiceUtil.getErrorMessage(result), module);
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                    }
                }
            }
        } catch (GenericServiceException gse) {
            Debug.logInfo(gse.getMessage(), module);
            return ServiceUtil.returnError(gse.getMessage());
        } catch (GenericEntityException gee) {
            Debug.logInfo(gee.getMessage(), module);
            return ServiceUtil.returnError(gee.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }
}