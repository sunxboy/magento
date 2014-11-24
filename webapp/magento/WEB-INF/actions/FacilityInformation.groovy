import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionBuilder;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.party.party.PartyWorker;
import org.ofbiz.entity.util.EntityUtil;


if (productStore) {
    facilityId = productStore.inventoryFacilityId;
    partyId = productStore.payToPartyId;
    if (facilityId) {
        EntityCondition condition = EntityCondition.makeCondition(
                EntityCondition.makeCondition("facilityId", facilityId),
                EntityCondition.makeConditionDate("fromDate", "thruDate")
        );
        facilityContactMechList = delegator.findList("FacilityContactMech", condition, null, null, null, false);
        if (facilityContactMechList) {
            contactMechList = delegator.findList("ContactMech", EntityCondition.makeCondition("contactMechId", EntityOperator.IN, facilityContactMechList.contactMechId), null, null, null, false);
            if (contactMechList) {
                /*Get postal address of facility*/
                postalAddressContactMech = EntityUtil.getFirst(EntityUtil.filterByCondition(contactMechList, EntityCondition.makeCondition("contactMechTypeId", EntityOperator.EQUALS, "POSTAL_ADDRESS")));
                if (postalAddressContactMech) {
                    postalAddress = delegator.findOne("PostalAddress", [contactMechId: postalAddressContactMech.contactMechId], false);
                    if (postalAddress) {
                        context.warehousePostalAddress = postalAddress;
                    }
                }
                /*Get contact number of company*/
                telecomNumberContactMech = EntityUtil.getFirst(EntityUtil.filterByCondition(contactMechList, EntityCondition.makeCondition("contactMechTypeId", EntityOperator.EQUALS, "TELECOM_NUMBER")));
                if (telecomNumberContactMech) {
                    telecomNumber = delegator.findOne("TelecomNumber", [contactMechId: telecomNumberContactMech.contactMechId], false);
                    if (telecomNumber) {
                        context.warehouseTelecomNumber= telecomNumber;
                    }
                }
            }
        }
        /*Get facility values*/
        facility = delegator.findOne("Facility", [facilityId: facilityId], false);
        context.facility = facility;
    }
}