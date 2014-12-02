import org.ofbiz.entity.util.EntityUtil;
if (parameters.carrierPartyId) {
    carrierPartyId = parameters.carrierPartyId;
    if ("DHL".equalsIgnoreCase(carrierPartyId)) {
        shipmentGatewayConfiguration = EntityUtil.getFirst(delegator.findList("ShipmentGatewayDhl", null, null, null, null, false));
    } else if ("FEDEX".equalsIgnoreCase(carrierPartyId)) {
        shipmentGatewayFedex = EntityUtil.getFirst(delegator.findList("ShipmentGatewayFedex", null, null, null, null, false));
        if (shipmentGatewayFedex) {
            shipmentGatewayConfiguration = [:];
            shipmentGatewayConfiguration.shipmentGatewayConfigId = shipmentGatewayFedex.shipmentGatewayConfigId;
            shipmentGatewayConfiguration.accessUserId = shipmentGatewayFedex.accessUserKey;
            shipmentGatewayConfiguration.accessPassword = shipmentGatewayFedex.accessUserPwd;
            shipmentGatewayConfiguration.connectUrl = shipmentGatewayFedex.connectUrl;
        }
    } else if ("UPS".equalsIgnoreCase(carrierPartyId)) {
        shipmentGatewayConfiguration = EntityUtil.getFirst(delegator.findList("ShipmentGatewayUps", null, null, null, null, false));
    } else if ("USPS".equalsIgnoreCase(carrierPartyId)) {
        shipmentGatewayConfiguration = EntityUtil.getFirst(delegator.findList("ShipmentGatewayUsps", null, null, null, null, false));
    }
    context.shipmentGatewayConfiguration = shipmentGatewayConfiguration;
}