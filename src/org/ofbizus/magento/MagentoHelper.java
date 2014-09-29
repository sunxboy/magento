package org.ofbizus.magento;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityFunction;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.entity.util.EntityUtilProperties;
import org.ofbiz.order.order.OrderChangeHelper;
import org.ofbiz.order.shoppingcart.CheckOutHelper;
import org.ofbiz.order.shoppingcart.ItemNotFoundException;
import org.ofbiz.order.shoppingcart.ShoppingCart;
import org.ofbiz.order.shoppingcart.ShoppingCartItem;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

public class MagentoHelper {
    public static final String SALES_CHANNEL = "MAGENTO_SALE_CHANNEL";
    public static final String ORDER_TYPE = "SALES_ORDER";

    public static final int SHIPPING_ADDRESS = 10;
    public static final int BILLING_ADDRESS = 50;

    public static final String module = MagentoHelper.class.getName();
    @SuppressWarnings("unchecked")
    public static String createOrder(Map<String, ?> orderInformation, Locale locale, Delegator delegator, LocalDispatcher dispatcher) throws GeneralException {
        GenericValue magentoConfiguration = null;
        String productStoreId = null;
        String websiteId = null;
        String prodCatalogId = null;
        for (Map.Entry<String, ?> entry: orderInformation.entrySet()) {
            Object cKey = entry.getKey();
            Object value = entry.getValue();

            System.out.println("----Sent by Xml Rpc SVC-CONTEXT: " + cKey + " => " + value);
        }
        
        // get the magento order number
        String externalId = (String) orderInformation.get("increment_id");

        // check and make sure if order with externalId already exist
        List<GenericValue> existingOrder = delegator.findList("OrderHeader", EntityCondition.makeCondition("externalId", externalId), null, null, null, false);
        if (UtilValidate.isNotEmpty(existingOrder) && existingOrder.size() > 0) {
            //throw new GeneralException("Ofbiz order #" + externalId + " already exists.");
            Debug.logWarning("Ofbiz order #" + externalId + " already exists.", module);
            return "Ofbiz order #" + externalId + " already exists.";
        }
        GenericValue system = delegator.findOne("UserLogin", false, UtilMisc.toMap("userLoginId", "system"));
        magentoConfiguration = EntityUtil.getFirst(delegator.findList("MagentoConfiguration", EntityCondition.makeCondition("enumId", EntityOperator.EQUALS, "MAGENTO_SALE_CHANNEL"), null, null, null, false));
        if (UtilValidate.isNotEmpty(magentoConfiguration)) {
            productStoreId = magentoConfiguration.getString("productStoreId");
        }
        String currencyUom = (String) orderInformation.get("order_currency_code");

     // Initialize the shopping cart
        ShoppingCart cart = new ShoppingCart(delegator, productStoreId, websiteId, locale, currencyUom);
        cart.setUserLogin(system, dispatcher);
        cart.setOrderType(ORDER_TYPE);
        cart.setChannelType(SALES_CHANNEL);
        //cart.setOrderDate(UtilDateTime.toTimestamp(info.getTimestamp().()));
        cart.setExternalId(externalId);

        Debug.logInfo("Created shopping cart for Magento order: ", module);
        Debug.logInfo("-- WebSite : " + websiteId, module);
        Debug.logInfo("-- Product Store : " + productStoreId, module);
        Debug.logInfo("-- Locale : " + locale.toString(), module);
        Debug.logInfo("-- Magento Order # : " + externalId, module);

        
        // set the customer information

        Map<String, Object> shippingAddress = (Map<String, Object>) orderInformation.get("shipping_address");
        Map<String, Object> billingAddress = (Map<String, Object>) orderInformation.get("billing_address");

        MagentoClient magentoClient = new MagentoClient(dispatcher, delegator);
        Object[] directoryRegionList = magentoClient.getDirectoryRegionList((String)shippingAddress.get("country_id"));
        for (Object directoryRegion : directoryRegionList) {
            Map<String, Object> region = (Map<String, Object>)directoryRegion;
            if (((String)region.get("region_id")).equals(shippingAddress.get("region_id"))) {
                shippingAddress.put("region_code", (String)region.get("code"));
                billingAddress.put("region_code", (String)region.get("code"));
                break;
            }
        }

        String[] partyInfo = setPartyInfo(orderInformation.get("customer_email").toString(), shippingAddress, billingAddress, delegator, dispatcher);
        if (partyInfo == null || partyInfo.length != 3) {
            throw new GeneralException("Unable to parse/create party information, invalid number of parameters returned");
        }
        cart.setOrderPartyId(partyInfo[0]);
        cart.setPlacingCustomerPartyId(partyInfo[0]);
        cart.setShippingContactMechId(0, partyInfo[1]);
        // contact info
        if (UtilValidate.isNotEmpty(shippingAddress)) {
            String shippingEmail = (String) shippingAddress.get("email");
            if (UtilValidate.isNotEmpty(shippingEmail)) {
                setContactInfo(cart, "PRIMARY_EMAIL", shippingEmail, delegator, dispatcher);
            }
            String shippingPhone = shippingAddress.get("telephone").toString();
            if (UtilValidate.isNotEmpty(shippingPhone)) {
                setContactInfo(cart, "PHONE_SHIPPING", shippingPhone, delegator, dispatcher);
            }
        }
        if (UtilValidate.isNotEmpty(billingAddress)) {
            String billingEmail = billingAddress.get("email").toString();
            if (UtilValidate.isNotEmpty(billingEmail)) {
                setContactInfo(cart, "BILLING_EMAIL", billingEmail, delegator, dispatcher);
            }
            String billingPhone = billingAddress.get("telephone").toString();
            if (UtilValidate.isNotEmpty(billingPhone)) {
                setContactInfo(cart, "PHONE_BILLING", billingPhone, delegator, dispatcher);
            }
        }
        // set the order items
        //List<Map<String,?>> orderItems =  (List<Map<String,?>>) orderInformation.get("orderItems");
        Object[] orderItems =  (Object[]) orderInformation.get("items");
        HashMap<String, Object> items = new HashMap<String, Object>();
        for (Object orderItem : orderItems) {
            Map<String,?> item = (Map<String,?>) orderItem;
            items.put((String)item.get("item_id"), item);
        }
        HashMap<String, Object> productData = null;
        GenericValue magentoProduct = null;
        BigDecimal price = null;
        for (Object orderItem : orderItems) {
            Map<String,?> item = (Map<String,?>) orderItem;
            try {
                productData = new HashMap<String, Object>();
                productData.put("productTypeId", "FINISHED_GOOD");
                productData.put("internalName", item.get("name"));
                productData.put("productName", item.get("name"));
                productData.put("description", item.get("shortDescription"));
                productData.put("longDescription", item.get("description"));
                productData.put("userLogin", system);
                String idValue = (String) item.get("product_id");

                // Handling Magento's Product Id.
                EntityCondition cond = EntityCondition.makeCondition(
                        EntityCondition.makeCondition("idValue", idValue),
                        EntityCondition.makeCondition("goodIdentificationTypeId", "MAGENTO_ID")
                        );
                List<GenericValue> goodIdentification = delegator.findList("GoodIdentification", cond, null, null, null, false);
                Map<String, Object> parentItem = new HashMap<String, Object>();
                if ("bundle".equals(item.get("product_type"))) {
                    continue;
                }
                if ("simple".equals(item.get("product_type")) && UtilValidate.isNotEmpty(item.get("parent_item_id"))) {
                    parentItem = (Map<String, Object>) items.get(item.get("parent_item_id"));
                    // Check if simple product is child of any configurable product, if yes then create it as a variant product.
                    if (UtilValidate.isNotEmpty(parentItem) && "configurable".equals(parentItem.get("product_type"))) {
                        price = new BigDecimal(parentItem.get("price").toString());
                        productData.put("isVariant", "Y");
                    } else {
                        price =  new BigDecimal(item.get("price").toString());
                    }
                } else if ("configurable".equals(item.get("product_type"))) {
                    // We have considered Magento Configurable products as Virtual Product in our system.
                    price =  new BigDecimal(item.get("price").toString());
                    productData.put("isVirtual", "Y");
                } else {
                    price =  new BigDecimal(item.get("price").toString());
                }
                if (UtilValidate.isNotEmpty(goodIdentification) && "configurable".equals(item.get("product_type"))) {
                    continue;
                } else if (UtilValidate.isNotEmpty(goodIdentification)) {
                    magentoProduct = EntityUtil.getFirst(goodIdentification);
                    productData.put("productId", magentoProduct.get("productId"));
                    productData.put("price", price);
                    productData.put("quantity", item.get("qty_ordered").toString());
                } else {
                    Integer inventoryCount = null;
                    Object[] catalogInventoryStockItemList = magentoClient.getCatalogInventoryStockItemList((String)item.get("sku"));
                    Map<String, Object> itemStock = (Map<String, Object>)catalogInventoryStockItemList[0];
                    if (UtilValidate.isNotEmpty(itemStock.get("qty")) && UtilValidate.isNotEmpty(item.get("qty_ordered"))) {
                        Integer quantity = (Integer) ObjectType.simpleTypeConvert(item.get("qty_ordered"), "Integer", null, locale);
                        inventoryCount = (Integer) ObjectType.simpleTypeConvert(itemStock.get("qty"), "Integer", null, locale);
                        inventoryCount = inventoryCount + quantity;
                        //Here we need to add quantity of the product in inventory count because the value of it that we get from magento
                        //is already reduced and after full fulfilling order in ofbiz it reduces again. Hence we have to make inventory count in ofbiz equal
                        //to initial value of it in magento.
                    }

                    //TODO: Need to get information, to check, whether the order is back order or not.
                    /*
                    if (UtilValidate.isNotEmpty(item.get("isBackOrder"))) {
                        Integer isBackOrder = (Integer) ObjectType.simpleTypeConvert(item.get("isBackOrder"), "Integer", null, locale);
                        if(isBackOrder != 0) {
                            productData.put("requireInventory", "N");
                        }
                    }*/
                    Map<String, Object> product = dispatcher.runSync("createProduct", productData);
                    if (ServiceUtil.isSuccess(product)) {
                        if (UtilValidate.isNotEmpty(parentItem) && "configurable".equals(parentItem.get("product_type"))) {
                            EntityCondition condition = EntityCondition.makeCondition(
                                    EntityCondition.makeCondition("idValue", parentItem.get("product_id")),
                                    EntityCondition.makeCondition("goodIdentificationTypeId", "MAGENTO_ID")
                                    );
                            GenericValue virtualGoodIdentification = EntityUtil.getFirst(delegator.findList("GoodIdentification", condition, null, null, null, false));
                            if (UtilValidate.isNotEmpty(virtualGoodIdentification)) {
                                Map<String, Object> productAssocCtx = new HashMap<String, Object>();
                                productAssocCtx.put("productId", virtualGoodIdentification.getString("productId"));
                                productAssocCtx.put("productIdTo", product.get("productId"));
                                productAssocCtx.put("productAssocTypeId", "PRODUCT_VARIANT");
                                productAssocCtx.put("fromDate", UtilDateTime.nowTimestamp());
                                productAssocCtx.put("userLogin", system);
                                dispatcher.runSync("createProductAssoc", productAssocCtx);
                                //TODO : Need to find a way to get ProductFeatures from Magento and provide its support as well.
                            }
                        }
                        productData.clear();
                        productData.put("productId", product.get("productId"));
                        productData.put("price" , price);
                        productData.put("productPriceTypeId", "DEFAULT_PRICE");
                        productData.put("productPricePurposeId", "PURCHASE");
                        productData.put("currencyUomId", orderInformation.get("order_currency_code"));
                        productData.put("productStoreGroupId" , "_NA_");
                        productData.put("fromDate" , UtilDateTime.nowTimestamp());
                        productData.put("userLogin", system);
                        dispatcher.runSync("createProductPrice",productData );
                        productData.put("productPriceTypeId", "LIST_PRICE");
                        dispatcher.runSync("createProductPrice",productData );
                        productData.put("quantity", new BigDecimal(item.get("qty_ordered").toString()));

                        GenericValue goodIdentificationRecord = delegator.makeValue("GoodIdentification");
                        goodIdentificationRecord.set("goodIdentificationTypeId", "MAGENTO_ID");
                        goodIdentificationRecord.set("productId", product.get("productId"));
                        goodIdentificationRecord.set("idValue", item.get("product_id"));
                        delegator.createOrStore(goodIdentificationRecord);
                        if ("configurable".equals(item.get("product_type"))) {
                            continue;
                        }
                        if(inventoryCount > 0) {
                            String facilityId = (delegator.findOne("ProductStore", UtilMisc.toMap("productStoreId", productStoreId), true)).getString("inventoryFacilityId");
                            HashMap<String, Object> serviceContext = new HashMap<String, Object>();
                            serviceContext.put("userLogin", system);
                            serviceContext.put("facilityId", facilityId);
                            serviceContext.put("productId", product.get("productId"));

                            cond = EntityCondition.makeCondition(
                                    EntityCondition.makeCondition("productId", product.get("productId")),
                                    EntityCondition.makeCondition("facilityId", facilityId)
                                    );
                            GenericValue productFacilityLocation = EntityUtil.getFirst(delegator.findList("ProductFacilityLocation", cond, null, null, null, false));
                            if (UtilValidate.isEmpty(productFacilityLocation)) {
                                GenericValue facilityLocation = EntityUtil.getFirst(delegator.findList("FacilityLocation", EntityCondition.makeCondition("facilityId", facilityId), null, null, null, false));
                                if (UtilValidate.isNotEmpty(facilityLocation)) {
                                    serviceContext.put("locationSeqId", facilityLocation.getString("locationSeqId"));
                                    dispatcher.runSync("createProductFacilityLocation", serviceContext);
                                }
                            } else {
                                serviceContext.put("locationSeqId", productFacilityLocation.getString("locationSeqId"));
                            }
                            serviceContext.put("quantityAccepted", inventoryCount);
                            serviceContext.put("inventoryItemTypeId", "NON_SERIAL_INV_ITEM");
                            dispatcher.runSync("receiveInventoryProductFromMagento",serviceContext);
                            serviceContext.clear();
                        }
                    }
                }
                addItem(cart, productData, prodCatalogId, 0, delegator, dispatcher);
            } catch (ItemNotFoundException e) {
                Debug.logError("Unable to obtain GoodIdentification entity value of the Magento id for product [" + orderInformation.get("id") + "]: " + e.getMessage(), module);
            }
        }
        // handle the adjustments
        HashMap<String,String> adjustment = new HashMap<String, String>();
        adjustment.put("orderTaxAmount", (String) orderInformation.get("tax_amount"));
        adjustment.put("orderDiscountAmount", (String) orderInformation.get("discount_amount"));
        adjustment.put("orderShippingAmount", (String) orderInformation.get("shipping_amount"));
        if (UtilValidate.isNotEmpty(adjustment)){
            addAdjustments(cart, adjustment, delegator);
            // ship group info
            if (UtilValidate.isNotEmpty(orderInformation.get("shipping_method"))) {
                String ShippingMethod = orderInformation.get("shipping_method").toString();
                String carrierPartyId = ShippingMethod.substring(0, ShippingMethod.indexOf("_")).toUpperCase();
                if("FLATRATE".equalsIgnoreCase(carrierPartyId)) {
                    carrierPartyId = "_NA_";
                }

                String shipmentMethodTypeId = null;
                String carrierServiceCode = ShippingMethod.replace("_", "").toUpperCase();
                EntityCondition condition = EntityCondition.makeCondition(
                        EntityCondition.makeCondition("roleTypeId", "CARRIER"),
                        EntityCondition.makeCondition("partyId", carrierPartyId),
                        EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("carrierServiceCode"), EntityOperator.EQUALS ,carrierServiceCode)
                        );
                GenericValue carrierShipmentMethod = EntityUtil.getFirst(delegator.findList("CarrierShipmentMethod", condition, UtilMisc.toSet("shipmentMethodTypeId"), null, null, false));
                if (UtilValidate.isNotEmpty(carrierShipmentMethod)) {
                    shipmentMethodTypeId = carrierShipmentMethod.getString("shipmentMethodTypeId");
                } else if (UtilValidate.isEmpty(shipmentMethodTypeId)) {
                    String magShipmentMethodTypeId = ShippingMethod.substring(ShippingMethod.indexOf("_") + 1);
                    //shipmentMethodName = "Ground";
                    Debug.logInfo("Magento ShipmentMethodTypeId :"+magShipmentMethodTypeId, module);
                    shipmentMethodTypeId = EntityUtilProperties.getPropertyValue("Magento", magShipmentMethodTypeId, delegator);
                    if (UtilValidate.isEmpty(shipmentMethodTypeId)) {
                        shipmentMethodTypeId = magShipmentMethodTypeId;
                    }
                }
                Debug.logInfo("Setting ShipmentMethodTypeId to order:"+shipmentMethodTypeId, module);
                addShipInfo(cart, UtilMisc.toMap("carrierPartyId" , carrierPartyId, "shipmentMethodTypeId", shipmentMethodTypeId), partyInfo[1]);
            }
        }
        String paymentMethod = EntityUtilProperties.getPropertyValue("MagentoConfig.properties", "magento.payment.method", "EXT_OFFLINE", delegator);
        // set the cart payment method
        cart.addPaymentAmount(paymentMethod, new BigDecimal((String) orderInformation.get("grand_total")));
        // validate the payment methods
        CheckOutHelper coh = new CheckOutHelper(dispatcher, delegator, cart);
        Map validateResp = coh.validatePaymentMethods();
        if (!ServiceUtil.isSuccess(validateResp)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(validateResp));
        }
        // create the order & process payments
        Map createResp = coh.createOrder(system);
        if (!ServiceUtil.isSuccess(createResp)) {
            return (String) ServiceUtil.getErrorMessage(createResp);
        }
        return "success";
    }

    public static String[] setPartyInfo(String emailAddress, Map<String, ?> shipAddr, Map<String, ?> billAddr, Delegator delegator, LocalDispatcher dispatcher) throws GeneralException {
        String shipCmId = null;
        String billCmId = null;
        String partyId = null;
        GenericValue system = delegator.findOne("UserLogin", false, UtilMisc.toMap("userLoginId", "system"));
        Map<String, Object> serviceCtx = new HashMap<String, Object>();
        // create new party
        if (partyId == null) {
            serviceCtx.put("firstName", shipAddr.get("firstname"));
            serviceCtx.put("lastName", shipAddr.get("lastname"));
            serviceCtx.put("userLogin", system);
            Map<String, Object> personResp = dispatcher.runSync("createPerson", serviceCtx);
            if (!ServiceUtil.isSuccess(personResp)) {
                throw new GeneralException("Unable to create new customer account: " + ServiceUtil.getErrorMessage(personResp));
            }
            partyId = (String) personResp.get("partyId");
            Debug.logInfo("New party created : " + partyId, module);
        }

        serviceCtx.clear();
        serviceCtx.put("partyId", partyId);
        serviceCtx.put("roleTypeId", "CUSTOMER");
        serviceCtx.put("userLogin", system);
        dispatcher.runSync("createPartyRole", serviceCtx);

        if (UtilValidate.isNotEmpty(shipAddr)) {
            shipCmId = createPartyAddress(partyId, shipAddr, delegator, dispatcher);
            addPurposeToAddress(partyId, shipCmId, SHIPPING_ADDRESS, delegator, dispatcher);
        }
        if (UtilValidate.isNotEmpty(billAddr)) {
            billCmId = createPartyAddress(partyId, billAddr, delegator, dispatcher);
            addPurposeToAddress(partyId, billCmId, BILLING_ADDRESS, delegator, dispatcher);
        }
        return new String[] { partyId, shipCmId, billCmId };
    }

    public static String createPartyAddress(String partyId, Map<String,?> addr, Delegator delegator, LocalDispatcher dispatcher) throws GeneralException {
        // check for zip+4
        String postalCode = addr.get("postcode").toString();
        String postalCodeExt = null;
        if (postalCode.length() == 10 && postalCode.indexOf("-") != -1) {
            String[] strSplit = postalCode.split("-", 2);
            postalCode = strSplit[0];
            postalCodeExt = strSplit[1];
        }
        String toName = ((String)addr.get("firstname"))+" "+((String)addr.get("lastname"));
        GenericValue system = delegator.findOne("UserLogin", false, UtilMisc.toMap("userLoginId", "system"));
        // prepare the create address map
        Map<String, Object> addrMap = new HashMap<String, Object>();
        addrMap.put("partyId", partyId);
        addrMap.put("toName", toName);
        addrMap.put("address1", addr.get("street"));
        addrMap.put("city", addr.get("city"));
        addrMap.put("stateProvinceGeoId", addr.get("region_code").toString());
        addrMap.put("countryGeoId", getCountryGeoId(addr.get("country_id").toString(), delegator));
        addrMap.put("postalCode", postalCode);
        addrMap.put("postalCodeExt", postalCodeExt);
        addrMap.put("allowSolicitation", "Y");
        addrMap.put("contactMechPurposeTypeId", "GENERAL_LOCATION");
        addrMap.put("userLogin", system); // run as the system user
        
        // invoke the create address service
        Map<String, Object> addrResp = dispatcher.runSync("createPartyPostalAddress", addrMap);
        if (ServiceUtil.isError(addrResp)) {
            throw new GeneralException("Unable to create new customer address record: " +
                    ServiceUtil.getErrorMessage(addrResp));
        }
        String contactMechId = (String) addrResp.get("contactMechId");
        
        Debug.logInfo("Created new address for partyId [" + partyId + "] :" + contactMechId, module);
        return contactMechId;
    }

    public static void addPurposeToAddress(String partyId, String contactMechId, int addrType, Delegator delegator, LocalDispatcher dispatcher) throws GeneralException {
        // convert the int to a purpose type ID
        String contactMechPurposeTypeId = getAddressType(addrType);
        GenericValue system = delegator.findOne("UserLogin", false, UtilMisc.toMap("userLoginId", "system"));
        
        
        // check to make sure the purpose doesn't already exist
        EntityCondition condition = EntityCondition.makeCondition(
                EntityCondition.makeCondition("partyId", partyId),
                EntityCondition.makeCondition("contactMechId", contactMechId),
                EntityCondition.makeCondition("contactMechPurposeTypeId", contactMechPurposeTypeId)
                );
        
        List<GenericValue> values = delegator.findList("PartyContactMechPurpose", condition, null, null, null, false);
        if (values == null || values.size() == 0) {
            Map<String, Object> addPurposeMap = new HashMap<String, Object>();
            addPurposeMap.put("contactMechId", contactMechId);
            addPurposeMap.put("partyId", partyId);     
            addPurposeMap.put("contactMechPurposeTypeId", contactMechPurposeTypeId);
            addPurposeMap.put("userLogin", system);
            
            Map<String, Object> addPurposeResp = dispatcher.runSync("createPartyContactMechPurpose", addPurposeMap);
            if (addPurposeResp != null && ServiceUtil.isError(addPurposeResp)) {
                throw new GeneralException(ServiceUtil.getErrorMessage(addPurposeResp));
            }
        }
    }

    public static String getAddressType(int addrType) {
        String contactMechPurposeTypeId = "GENERAL_LOCATION";
        switch(addrType) {
            case SHIPPING_ADDRESS:
                contactMechPurposeTypeId = "SHIPPING_LOCATION";
                break;
            case BILLING_ADDRESS:
                contactMechPurposeTypeId = "BILLING_LOCATION";
                break;
        }
        return contactMechPurposeTypeId;
    }
    
    public static void setContactInfo(ShoppingCart cart, String contactMechPurposeTypeId, String infoString, Delegator delegator, LocalDispatcher dispatcher) throws GeneralException {
        Map<String, Object> lookupMap = new HashMap<String, Object>();
        String cmId = null;
        String entityName = "PartyAndContactMech";
        GenericValue cmLookup = null;
        GenericValue  system = delegator.findOne("UserLogin", false, UtilMisc.toMap("userLoginId", "system")); 
        if (contactMechPurposeTypeId.startsWith("PHONE_")) {
            lookupMap.put("partyId", cart.getOrderPartyId());
            lookupMap.put("contactNumber", infoString);
            entityName = "PartyAndTelecomNumber";
        } else if (contactMechPurposeTypeId.endsWith("_EMAIL")) {
            lookupMap.put("partyId", cart.getOrderPartyId());
            lookupMap.put("infoString", infoString);
        } else {
            throw new GeneralException("Invalid contact mech type");
        }
        EntityCondition cond = EntityCondition.makeCondition(
                EntityCondition.makeCondition(lookupMap),
                EntityCondition.makeConditionDate("fromDate", "thruDate")
                );
        try {
            //cmLookup = delegator.findByAnd(entityName, lookupMap, UtilMisc.toList("-fromDate"));
            //cmLookup = EntityUtil.filterByDate(cmLookup);
            cmLookup = EntityUtil.getFirst(delegator.findList(entityName, cond, null, null, null, false));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            throw e;
        }

        if (UtilValidate.isNotEmpty(cmLookup)) {
                cmId = cmLookup.getString("contactMechId");
        } else {
            // create it
            lookupMap.put("contactMechPurposeTypeId", contactMechPurposeTypeId);
            lookupMap.put("userLogin", system);
            Map<String, Object> createResp = null;
            if (contactMechPurposeTypeId.startsWith("PHONE_")) {
                try {
                    createResp = dispatcher.runSync("createPartyTelecomNumber", lookupMap);
                } catch (GeneralException e) {
                    Debug.logError(e, module);
                    throw e;
                }
            } else if (contactMechPurposeTypeId.endsWith("_EMAIL")) {
                lookupMap.put("emailAddress", lookupMap.get("infoString"));
                lookupMap.put("allowSolicitation", "Y");
                try {
                    createResp = dispatcher.runSync("createPartyEmailAddress", lookupMap);
                } catch (GeneralException e) {
                    Debug.logError(e, module);
                    throw e;
                }
            }
            if (createResp == null || ServiceUtil.isError(createResp)) {
                throw new GeneralException("Unable to create the request contact mech");
            }

            // get the created ID
            cmId = (String) createResp.get("contactMechId");
        }
        if (cmId != null) {
            cart.addContactMech(contactMechPurposeTypeId, cmId);
        }
    }

    public static void addAdjustments(ShoppingCart cart, Map<String,?> adjustment, Delegator delegator) {
        // handle shipping
        BigDecimal shipAmount = new BigDecimal(adjustment.get("orderShippingAmount").toString());
        GenericValue shipAdj = delegator.makeValue("OrderAdjustment", new HashMap<String, Object>());
        shipAdj.set("orderAdjustmentTypeId", "SHIPPING_CHARGES");
        shipAdj.set("amount", shipAmount);
        cart.addAdjustment(shipAdj);

        // handle tax
        BigDecimal taxAmount = new BigDecimal(adjustment.get("orderTaxAmount").toString());
        GenericValue taxAdj = delegator.makeValue("OrderAdjustment", new HashMap<String, Object>());
        taxAdj.set("orderAdjustmentTypeId", "SALES_TAX");
        taxAdj.set("amount", taxAmount);
        cart.addAdjustment(taxAdj);

        // handle DISCOUNT
        BigDecimal discountAmount = new BigDecimal(adjustment.get("orderDiscountAmount").toString());
        GenericValue discountAdj = delegator.makeValue("OrderAdjustment", new HashMap<String, Object>());
        discountAdj.set("orderAdjustmentTypeId", "DISCOUNT_ADJUSTMENT");
        discountAdj.set("amount", discountAmount);
        cart.addAdjustment(discountAdj);
    }

    public static String getCountryGeoId(String geoCode, Delegator delegator) {
        if (UtilValidate.isNotEmpty(geoCode) && geoCode.length() == 3) {
            return geoCode;
        }
        GenericValue geo = null;
        try {
            EntityCondition condition = EntityCondition.makeCondition(
                    EntityCondition.makeCondition("geoCode", geoCode),
                    EntityCondition.makeCondition("geoTypeId", "COUNTRY")
                    );
            geo = EntityUtil.getFirst(delegator.findList("Geo", condition, null, null, null, false));
            if (UtilValidate.isNotEmpty(geo)) {
                return geo.getString("geoId");
            } else {
                return "_NA_";
            }
            
        } catch (GenericEntityException gee) {
            Debug.logError(gee, module);
        }
        return null;
    }

    public static void addShipInfo(ShoppingCart cart, Map<String , ?> shipping, String shipContactMechId) {
        String shipmentMethodTypeId = (String) shipping.get("shipmentMethodTypeId");
        String carrierPartyId = (String)shipping.get("carrierPartyId");
        Boolean maySplit = Boolean.FALSE; 
        cart.setShipmentMethodTypeId(0, shipmentMethodTypeId);
        cart.setCarrierPartyId(0, carrierPartyId);
        cart.setMaySplit(0, maySplit);
        cart.setShippingContactMechId(0, shipContactMechId);
    }
    public static void processStateChange(Map<String, ?> info, Delegator delegator, LocalDispatcher dispatcher) throws GeneralException {
        String externalId = info.get("externalId").toString();
        GenericValue order = null;
        GenericValue system = delegator.findOne("UserLogin", false, UtilMisc.toMap("userLoginId", "system"));
        if ("CANCELED".equals(info.get("orderStatus").toString().toUpperCase())) {
            try {
                EntityCondition cond = EntityCondition.makeCondition(
                        EntityCondition.makeCondition("externalId", externalId),
                        EntityCondition.makeCondition("salesChannelEnumId", SALES_CHANNEL)
                        );
                order = EntityUtil.getFirst(delegator.findList("OrderHeader", cond, null, null, null, false));
            } catch (GenericEntityException gee) {
                Debug.logError(gee, module);
            }
            if (UtilValidate.isNotEmpty(order)) {
             // cancel the order
                if (!"ORDER_CANCELLED".equals(order.getString("syncStatusId"))) {
                    dispatcher.runSync("updateOrderHeader", UtilMisc.toMap("orderId", order.getString("orderId"), "syncStatusId", "ORDER_CANCELLED", "userLogin", system));
                }
                if (!"ORDER_CANCELLED".equals(order.getString("statusId"))) {
                    OrderChangeHelper.cancelOrder(dispatcher, system, order.getString("orderId"));
                }
            }
        }
    }
    public static void addItem(ShoppingCart cart, Map<String,?> item, String prodCatalogId, int groupIdx, Delegator delegator, LocalDispatcher dispatcher) throws GeneralException {
        String productId = item.get("productId").toString();
        BigDecimal qty = new BigDecimal(item.get("quantity").toString());
        BigDecimal price = new BigDecimal(item.get("price").toString());
        price = price.setScale(ShoppingCart.scale, ShoppingCart.rounding);
        
        HashMap<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("shipGroup", groupIdx);
        int idx = cart.addItemToEnd(productId, null, qty, null, null, attrs, prodCatalogId, null, dispatcher, Boolean.FALSE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE);
        ShoppingCartItem cartItem = cart.findCartItem(idx);
        cartItem.setQuantity(qty, dispatcher, cart, true, false);
        // locate the price verify it matches the expected price
        BigDecimal cartPrice = cartItem.getBasePrice();
        cartPrice = cartPrice.setScale(ShoppingCart.scale, ShoppingCart.rounding);
        if (price.doubleValue() != cartPrice.doubleValue()) {
            // does not match; honor the price but hold the order for manual review
            cartItem.setIsModifiedPrice(true);
            cartItem.setBasePrice(price);
            cart.setHoldOrder(true);
        }
        // assign the item to its ship group
        cart.setItemShipGroupQty(cartItem, qty, groupIdx);
    }

}