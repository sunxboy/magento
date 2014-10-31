package org.ofbizus.magento;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import magento.AssociativeEntity;
import magento.CatalogInventoryStockItemEntity;
import magento.CatalogInventoryStockItemEntityArray;
import magento.ComplexFilter;
import magento.ComplexFilterArray;
import magento.DirectoryRegionEntity;
import magento.Filters;
import magento.SalesOrderAddressEntity;
import magento.SalesOrderEntity;
import magento.SalesOrderItemEntity;
import magento.SalesOrderItemEntityArray;

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
    public static String createOrder(SalesOrderEntity orderInformation, Locale locale, Delegator delegator, LocalDispatcher dispatcher) throws GeneralException {
        GenericValue magentoConfiguration = null;
        String productStoreId = null;
        String websiteId = null;
        String prodCatalogId = null;

        // get the magento order number
        String externalId = orderInformation.getIncrementId();

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
        String currencyUom = orderInformation.getOrderCurrencyCode();

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
        SalesOrderAddressEntity shippingAddress = orderInformation.getShippingAddress();
        SalesOrderAddressEntity billingAddress = orderInformation.getBillingAddress();
        
        MagentoClient magentoClient = new MagentoClient(dispatcher, delegator);
        List<DirectoryRegionEntity> shippingDirectoryRegionList = magentoClient.getDirectoryRegionList(shippingAddress.getCountryId());
        for (DirectoryRegionEntity region : shippingDirectoryRegionList) {
            if ((region.getRegionId()).equals(shippingAddress.getRegionId())) {
                shippingAddress.setRegion(region.getCode());
                break;
            }
        }

        List<DirectoryRegionEntity> billingDirectoryRegionList = magentoClient.getDirectoryRegionList(shippingAddress.getCountryId());
        for (DirectoryRegionEntity region : billingDirectoryRegionList) {
            if ((region.getRegionId()).equals(shippingAddress.getRegionId())) {
                billingAddress.setRegion(region.getCode());
                break;
            }
        }

        String[] partyInfo = setPartyInfo(orderInformation.getCustomerEmail(), shippingAddress, billingAddress, delegator, dispatcher);
        if (partyInfo == null || partyInfo.length != 3) {
            throw new GeneralException("Unable to parse/create party information, invalid number of parameters returned");
        }
        cart.setOrderPartyId(partyInfo[0]);
        cart.setPlacingCustomerPartyId(partyInfo[0]);
        cart.setShippingContactMechId(0, partyInfo[1]);
        // contact info
        if (UtilValidate.isNotEmpty(shippingAddress)) {
            if (UtilValidate.isNotEmpty(orderInformation.getCustomerEmail())) {
                String shippingEmail = orderInformation.getCustomerEmail();
                setContactInfo(cart, "PRIMARY_EMAIL", shippingEmail, delegator, dispatcher);
            }
            if (UtilValidate.isNotEmpty(shippingAddress.getTelephone())) {
                String shippingPhone = shippingAddress.getTelephone();
                setContactInfo(cart, "PHONE_SHIPPING", shippingPhone, delegator, dispatcher);
            }
        }
        if (UtilValidate.isNotEmpty(billingAddress)) {
            if(UtilValidate.isNotEmpty(orderInformation.getCustomerEmail())) {
                String billingEmail = orderInformation.getCustomerEmail();
                setContactInfo(cart, "BILLING_EMAIL", billingEmail, delegator, dispatcher);
            }
            if (UtilValidate.isNotEmpty(billingAddress.getTelephone())) {
                String billingPhone = billingAddress.getTelephone();
                setContactInfo(cart, "PHONE_BILLING", billingPhone, delegator, dispatcher);
            }
        }
        // set the order items
        SalesOrderItemEntityArray salesOrderItemEntityArray = orderInformation.getItems();
        List<SalesOrderItemEntity> orderItems = salesOrderItemEntityArray.getComplexObjectArray();
        HashMap<String, Object> productData = null;
        GenericValue magentoProduct = null;
        BigDecimal price = null;

        HashMap<String, SalesOrderItemEntity> items = new HashMap<String, SalesOrderItemEntity>();
        for (SalesOrderItemEntity orderItem : orderItems) {
            if ("configurable".equals(orderItem.getProductType())) {
                items.put(orderItem.getSku(), orderItem);
            }
            
        }

        for (SalesOrderItemEntity item : orderItems) {
            try {
                productData = new HashMap<String, Object>();
                productData.put("productTypeId", "FINISHED_GOOD");
                productData.put("internalName", item.getName());
                productData.put("productName", item.getName());
                productData.put("userLogin", system);
                String idValue = item.getProductId();

                // Handling Magento's Product Id.
                EntityCondition cond = EntityCondition.makeCondition(
                        EntityCondition.makeCondition("idValue", idValue),
                        EntityCondition.makeCondition("goodIdentificationTypeId", "MAGENTO_ID")
                        );
                List<GenericValue> goodIdentification = delegator.findList("GoodIdentification", cond, null, null, null, false);
                if ("bundle".equals(item.getProductType())) {
                    continue;
                } else if ("configurable".equals(item.getProductType())) {
                    price =  new BigDecimal(item.getPrice());
                    continue;
                } else if (!"simple".equals(item.getProductType())) {
                    price =  new BigDecimal(item.getPrice());
                }
                if (UtilValidate.isNotEmpty(goodIdentification) && "configurable".equals(item.getProductType())) {
                    continue;
                } else if (UtilValidate.isNotEmpty(goodIdentification)) {
                    magentoProduct = EntityUtil.getFirst(goodIdentification);
                    productData.put("productId", magentoProduct.get("productId"));
                    productData.put("price", price);
                    productData.put("quantity", item.getQtyOrdered());
                } else {
                    Integer inventoryCount = null;
                    CatalogInventoryStockItemEntityArray catalogInventoryStockItemEntityArray = magentoClient.getCatalogInventoryStockItemList(item.getSku());
                    List<CatalogInventoryStockItemEntity>stockItemList = catalogInventoryStockItemEntityArray.getComplexObjectArray();
                    CatalogInventoryStockItemEntity itemStock = stockItemList.get(0);
                    if (UtilValidate.isNotEmpty(itemStock.getQty()) && UtilValidate.isNotEmpty(item.getQtyOrdered())) {
                        Integer quantity = (Integer) ObjectType.simpleTypeConvert(item.getQtyOrdered(), "Integer", null, locale);
                        inventoryCount = (Integer) ObjectType.simpleTypeConvert(itemStock.getQty(), "Integer", null, locale);
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
                        productData.clear();
                        productData.put("productId", product.get("productId"));
                        productData.put("price" , price);
                        productData.put("productPriceTypeId", "DEFAULT_PRICE");
                        productData.put("productPricePurposeId", "PURCHASE");
                        productData.put("currencyUomId", orderInformation.getOrderCurrencyCode());
                        productData.put("productStoreGroupId" , "_NA_");
                        productData.put("fromDate" , UtilDateTime.nowTimestamp());
                        productData.put("userLogin", system);
                        dispatcher.runSync("createProductPrice",productData );
                        productData.put("productPriceTypeId", "LIST_PRICE");
                        dispatcher.runSync("createProductPrice",productData );
                        productData.put("quantity", new BigDecimal(item.getQtyOrdered()));

                        GenericValue goodIdentificationRecord = delegator.makeValue("GoodIdentification");
                        goodIdentificationRecord.set("goodIdentificationTypeId", "MAGENTO_ID");
                        goodIdentificationRecord.set("productId", product.get("productId"));
                        goodIdentificationRecord.set("idValue", item.getProductId());
                        delegator.createOrStore(goodIdentificationRecord);
                        if ("configurable".equals(item.getProductType())) {
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
                            serviceContext.put("quantityRejected", 0);
                            serviceContext.put("inventoryItemTypeId", "NON_SERIAL_INV_ITEM");
                            dispatcher.runSync("receiveInventoryProduct",serviceContext);
                            serviceContext.clear();
                        }
                    }
                }
                if ("simple".equals(item.getProductType())) {
                    SalesOrderItemEntity orderItem = items.get(item.getSku());
                    if (UtilValidate.isNotEmpty(orderItem)) {
                        productData.put("orderItemId", orderItem.getItemId());
                    } else {
                productData.put("orderItemId", item.getItemId());
                    }
                }

                addItem(cart, productData, prodCatalogId, 0, delegator, dispatcher);
            } catch (ItemNotFoundException e) {
                Debug.logError("Unable to obtain GoodIdentification entity value of the Magento id for product [" + orderInformation.getParentId() + "]: " + e.getMessage(), module);
            }
        }
        // handle the adjustments
        HashMap<String,String> adjustment = new HashMap<String, String>();
        adjustment.put("orderTaxAmount", orderInformation.getTaxAmount());
        adjustment.put("orderDiscountAmount", orderInformation.getDiscountAmount());
        adjustment.put("orderShippingAmount", orderInformation.getShippingAmount());
        if (UtilValidate.isNotEmpty(adjustment)) {
            addAdjustments(cart, adjustment, delegator);
            // ship group info
            if (UtilValidate.isNotEmpty(orderInformation.getShippingMethod())) {
                String ShippingMethod = orderInformation.getShippingMethod();
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
        String paymentMethod = EntityUtilProperties.getPropertyValue("Magento.properties", "magento.payment.method", "EXT_OFFLINE", delegator);
        // set the cart payment method
        cart.addPaymentAmount(paymentMethod, new BigDecimal(orderInformation.getGrandTotal()));
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

    public static String[] setPartyInfo(String emailAddress, SalesOrderAddressEntity shipAddr, SalesOrderAddressEntity billAddr, Delegator delegator, LocalDispatcher dispatcher) throws GeneralException {
        String shipCmId = null;
        String billCmId = null;
        String partyId = null;
        GenericValue system = delegator.findOne("UserLogin", false, UtilMisc.toMap("userLoginId", "system"));
        Map<String, Object> serviceCtx = new HashMap<String, Object>();
        // create new party
        if (partyId == null) {
            serviceCtx.put("firstName", shipAddr.getFirstname());
            serviceCtx.put("lastName", shipAddr.getLastname());
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

    public static String createPartyAddress(String partyId, SalesOrderAddressEntity addr, Delegator delegator, LocalDispatcher dispatcher) throws GeneralException {
        // check for zip+4
        String postalCode = addr.getPostcode();
        String postalCodeExt = null;
        if (postalCode.length() == 10 && postalCode.indexOf("-") != -1) {
            String[] strSplit = postalCode.split("-", 2);
            postalCode = strSplit[0];
            postalCodeExt = strSplit[1];
        }
        String toName = (addr.getFirstname()+" "+(String)addr.getLastname());
        GenericValue system = delegator.findOne("UserLogin", false, UtilMisc.toMap("userLoginId", "system"));
        // prepare the create address map
        Map<String, Object> addrMap = new HashMap<String, Object>();
        addrMap.put("partyId", partyId);
        addrMap.put("toName", toName);
        addrMap.put("address1", addr.getStreet());
        addrMap.put("city", addr.getCity());
        addrMap.put("stateProvinceGeoId", addr.getRegion());
        addrMap.put("countryGeoId", getCountryGeoId(addr.getCountryId(), delegator));
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
        if ("ORDER_CANCELLED".equals(info.get("orderStatus").toString().toUpperCase())) {
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
        cartItem.setExternalId((String)item.get("orderItemId"));
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
    public static String completeOrderInMagento (LocalDispatcher dispatcher, Delegator delegator, String orderId) {
        try {
            GenericValue orderHeader = delegator.findOne("OrderHeader", false, UtilMisc.toMap("orderId", orderId));
            if (UtilValidate.isNotEmpty(orderHeader)) {
                String orderIncrementId = orderHeader.getString("externalId");
                MagentoClient magentoClient = new MagentoClient(dispatcher, delegator);
                List<GenericValue> orderShipmentList = delegator.findList("OrderShipment", EntityCondition.makeCondition("orderId", orderId), null, null, null, false);
                if (UtilValidate.isNotEmpty(orderShipmentList)) {
                    List<String> shipGroupSeqIdList = EntityUtil.getFieldListFromEntityList(orderShipmentList, "shipGroupSeqId", true);
                    if (UtilValidate.isNotEmpty(shipGroupSeqIdList)) {
                        for (String shipGroupSeqId : shipGroupSeqIdList) {
                            String shipmentId = null;
                            Map<Integer, Double> orderItemQtyMap = new HashMap<Integer, Double>();
                            for (GenericValue orderShipment : orderShipmentList) {
                                if ((orderShipment.getString("shipGroupSeqId")).equals(shipGroupSeqId)) {
                                    if (UtilValidate.isEmpty(shipmentId)) {
                                        shipmentId = orderShipment.getString("shipmentId");
                                    }
                                    GenericValue orderItem = delegator.findOne("OrderItem", false, UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderShipment.getString("orderItemSeqId")));
                                    Integer externalId = (Integer) ObjectType.simpleTypeConvert(orderItem.get("externalId"), "Integer", null, null);
                                    orderItemQtyMap.put(externalId, orderShipment.getDouble("quantity"));
                                }
                            }
                            String shipmentIncrementId = magentoClient.createShipment(orderIncrementId, orderItemQtyMap);
                            if (UtilValidate.isNotEmpty(shipmentIncrementId)) {
                                List<GenericValue> shipmentPackageRouteSegList = delegator.findList("ShipmentPackageRouteSeg", EntityCondition.makeCondition("shipmentId", shipmentId), UtilMisc.toSet("shipmentRouteSegmentId", "trackingCode"), null, null, false);
                                if (UtilValidate.isNotEmpty(shipmentPackageRouteSegList)) {
                                    String carrierTitle = null;
                                    for (GenericValue shipmentPackageRouteSeg : shipmentPackageRouteSegList) {
                                        GenericValue shipmentRoutSegment = delegator.findOne("ShipmentRouteSegment", false, UtilMisc.toMap("shipmentId", shipmentId, "shipmentRouteSegmentId", shipmentPackageRouteSeg.getString("shipmentRouteSegmentId")));
                                        String trackingCode = shipmentPackageRouteSeg.getString("trackingCode");
                                        String carrierPartyId = shipmentRoutSegment.getString("carrierPartyId");
                                        if (UtilValidate.isEmpty(trackingCode)) {
                                            continue;
                                        }
                                        if (UtilValidate.isEmpty(carrierPartyId) || "_NA_".equals(carrierPartyId)) {
                                            carrierPartyId = "custom";
                                            carrierTitle = "Flat Rate";
                                        } else {
                                            GenericValue carrier = delegator.findOne("PartyGroup", false, UtilMisc.toMap("partyId", carrierPartyId));
                                            if (UtilValidate.isNotEmpty(carrier)) {
                                                carrierTitle = carrier.getString("groupName");
                                            }
                                        }
                                        int istrackingCodeAdded = magentoClient.addTrack(shipmentIncrementId, carrierPartyId, carrierTitle, trackingCode);
                                        if (1 == istrackingCodeAdded) {
                                            Debug.logInfo("============Tracking code is added successfully in Magento side for shipment # "+shipmentId+".==============================", module);
                                        }
                                    }
                                }

                                String invoiceIncrementId = magentoClient.createInvoice(orderIncrementId, orderItemQtyMap);
                                if (UtilValidate.isNotEmpty(invoiceIncrementId)) {
                                    Debug.log("============order #"+orderIncrementId+"=======invoiceIncrementId="+invoiceIncrementId+"==========================");
                                }
                            }
                        }
                    }
                }
            }
        } catch (GenericEntityException gee) {
            Debug.logError(gee.getMessage(), module);
            return null;
        } catch (GeneralException ge) {
            Debug.logError(ge.getMessage(), module);
            return null;
        }
        return "Success";
    }
    public static Filters prepareSalesOrderFilters(String magOrderId, String statusId, Timestamp fromDate, Timestamp thruDate) {
        DateFormat df = new SimpleDateFormat("yyyy-mm-dd HH:mm:ss");
        String createdFrom = null;
        String createdTo = null;

        Filters filters = new Filters();
        ComplexFilterArray complexFilterArray = new ComplexFilterArray();
        ComplexFilter complexFilter = new ComplexFilter();

        if (UtilValidate.isNotEmpty(fromDate)) {
            Date from = (Date) fromDate;
            createdFrom = df.format(from);
        }
        if (UtilValidate.isNotEmpty(thruDate)) {
            Date thru = (Date) thruDate;
            createdTo = df.format(thru);
        }

        AssociativeEntity statusCond = new AssociativeEntity();
        statusCond.setKey("eq");
        statusCond.setValue(statusId);
        complexFilter.setKey("status");
        complexFilter.setValue(statusCond);

        AssociativeEntity createdDateCond = new AssociativeEntity();
        if (UtilValidate.isNotEmpty(createdFrom)) {
            createdDateCond.setKey("from");
            createdDateCond.setValue(createdFrom);
        }
        if (UtilValidate.isNotEmpty(createdTo)) {
            createdDateCond.setKey("to");
            createdDateCond.setValue(createdTo);
        }
        if (UtilValidate.isNotEmpty(createdFrom) || UtilValidate.isNotEmpty(createdTo)) {
            complexFilter.setKey("created_at");
            complexFilter.setValue(createdDateCond);
        }

        if (UtilValidate.isNotEmpty(magOrderId)) {
            AssociativeEntity orderIncrementIdCond = new AssociativeEntity();
            orderIncrementIdCond.setKey("eq");
            orderIncrementIdCond.setValue(magOrderId);
            complexFilter.setKey("increment_id");
            complexFilter.setValue(orderIncrementIdCond);
        }
        complexFilterArray.getComplexObjectArray().add(complexFilter);
        filters.setComplexFilter(complexFilterArray);

        return filters;
    }
}