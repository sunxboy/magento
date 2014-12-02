import org.ofbiz.entity.condition.EntityConditionBuilder;
import org.ofbiz.entity.condition.EntityCondition;

if (productStore) {
    exprBldr = new EntityConditionBuilder();
    expr = exprBldr.AND() {
        EQUALS(roleTypeId: "CARRIER")
        IN(partyId: ["DHL", "FEDEX", "UPS", "USPS"])
    }
    carrierParties = delegator.findList("PartyRoleAndPartyDetail", expr,
            null, ["groupName"], null, false);
    if (carrierParties) {
        carrierAndShipmentMethod = [:];
        storeShipMethMap = [:];
        shippingServiceNameMap = [:];
        carrierParties.each { carrier ->
            expr = exprBldr.AND() {
                EQUALS(productStoreId: productStore.productStoreId);
                EQUALS(partyId: carrier.partyId);
            }
            existingStoreShipMethList = delegator.findList("ProductStoreShipmentMeth", expr, null, ["includeGeoId", "shipmentMethodTypeId"], null, false);
            expr = exprBldr.AND() {
                EQUALS(roleTypeId: "CARRIER");
                EQUALS(partyId: carrier.partyId);
                if (existingStoreShipMethList) {
                    NOT_IN(shipmentMethodTypeId : existingStoreShipMethList.shipmentMethodTypeId);
                }
            }
            carrierAndShipmentMethodList = delegator.findList("CarrierAndShipmentMethod", expr, null, null, null, false);
            if (carrierAndShipmentMethodList) {
                carrierAndShipmentMethod.(carrier.partyId) = carrierAndShipmentMethodList;
            }

            storeShipMethList = [];
            existingStoreShipMethMap = [:];
            existingStoreShipMethList.each { existingStoreShipMeth ->
                storeShipingMethMap = [:];
                shipmentMethodType = delegator.findOne("ShipmentMethodType", false, [shipmentMethodTypeId : existingStoreShipMeth.shipmentMethodTypeId]);
                if (shipmentMethodType) {
                    storeShipingMethMap.description = shipmentMethodType.description;
                    storeShipingMethMap.productStoreShipMethId = existingStoreShipMeth.productStoreShipMethId;
                    existingStoreShipMethMap.(shipmentMethodType.shipmentMethodTypeId) = storeShipingMethMap;
                    storeShipMethMap.(carrier.partyId) = existingStoreShipMethMap;
                }
            }
            if ("DHL".equalsIgnoreCase(carrier.partyId)) {
                shippingServiceNameMap.(carrier.partyId) = "";
            } else if ("UPS".equalsIgnoreCase(carrier.partyId)) {
                shippingServiceNameMap.(carrier.partyId) = "";
            } else if ("USPS".equalsIgnoreCase(carrier.partyId)) {
                shippingServiceNameMap.(carrier.partyId) = "uspsRateInquire";
            } else if ("FEDEX".equalsIgnoreCase(carrier.partyId)) {
                shippingServiceNameMap.(carrier.partyId) = "";
            } 
        }
        context.shippingServiceNameMap = shippingServiceNameMap;
        context.carrierAndShipmentMethod = carrierAndShipmentMethod;
        context.carrierParties = carrierParties;
        context.storeShipMethMap = storeShipMethMap;
    }
}