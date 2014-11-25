import org.ofbiz.entity.util.EntityUtil;
if (parameters.carrierPartyId) {
    carrierPartyId = parameters.carrierPartyId;
    if ("DHL".equalsIgnoreCase(carrierPartyId)) {
        shipmentGatewayConfiguration = EntityUtil.getFirst(delegator.findList("ShipmentGatewayDhl", null, null, null, null, false));
    } else if ("FEDEX".equalsIgnoreCase(carrierPartyId)) {
        shipmentGatewayConfiguration = EntityUtil.getFirst(delegator.findList("ShipmentGatewayFedex", null, null, null, null, false));
    } else if ("UPS".equalsIgnoreCase(carrierPartyId)) {
        shipmentGatewayConfiguration = EntityUtil.getFirst(delegator.findList("ShipmentGatewayUps", null, null, null, null, false));
    } else if ("USPS".equalsIgnoreCase(carrierPartyId)) {
        shipmentGatewayConfiguration = EntityUtil.getFirst(delegator.findList("ShipmentGatewayUsps", null, null, null, null, false));
    }
    context.shipmentGatewayConfiguration = shipmentGatewayConfiguration;
}