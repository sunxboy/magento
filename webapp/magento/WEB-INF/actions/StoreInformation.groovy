import org.ofbiz.party.party.PartyHelper;
import org.ofbiz.party.party.PartyWorker;

if (productStore) {
    partyId = productStore.payToPartyId;
if (partyId) {
    companyMap = [:];
    groupName = PartyHelper.getPartyName(delegator, partyId, false);
    companyMap.groupName = groupName;

    /*Get postal address of company*/
    postalAddress = PartyWorker.findPartyLatestPostalAddress(partyId, delegator);
    companyMap.postalAddress = postalAddress;

    /*Get email address of company*/
    companyEmail = dispatcher.runSync("getPartyEmail", ['partyId': partyId, 'userLogin': parameters.userLogin]);
    companyMap.companyEmail = companyEmail;

    /*Get contact number of company*/
    telecomNumber = PartyWorker.findPartyLatestTelecomNumber(partyId, delegator);
    if(telecomNumber) {
        companyMap.telecomNumber= telecomNumber;
    }
    context.companyMap = companyMap;
    context.partyId = partyId;
}
}