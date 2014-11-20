import org.ofbiz.entity.condition.EntityConditionBuilder;
import org.ofbiz.entity.condition.EntityCondition;

exprBldr = new EntityConditionBuilder();
expr = exprBldr.AND() {
    EQUALS(roleTypeId: "CARRIER")
    NOT_EQUALS(partyId: '_NA_')
}
carrierParties = delegator.findList("PartyRoleAndPartyDetail", expr,
        null, ["groupName"], null, false);
if (carrierParties) {
    carrierAndShipmentMethod = [:];
    storeShipMethMap = [:];
    carrierParties.each { carrier ->
        expr = exprBldr.AND() {
            EQUALS(roleTypeId: "CARRIER");
            EQUALS(partyId: carrier.partyId);
        }
        carrierAndShipmentMethodList = delegator.findList("CarrierAndShipmentMethod", expr, null, null, null, false);
        if (carrierAndShipmentMethodList) {
            carrierAndShipmentMethod.(carrier.partyId) = carrierAndShipmentMethodList;
        }
        expr = exprBldr.AND() {
            EQUALS(productStoreId: productStore.productStoreId);
            EQUALS(partyId: carrier.partyId);
        }
        existingStoreShipMethList = delegator.findList("ProductStoreShipmentMeth", expr, null, ["includeGeoId", "shipmentMethodTypeId"], null, false);
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
    }
    context.carrierAndShipmentMethod = carrierAndShipmentMethod;
    context.carrierParties = carrierParties;
    context.storeShipMethMap = storeShipMethMap;
}