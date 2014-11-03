package org.ofbizus.magento;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import magento.ArrayOfString;
import magento.CatalogInventoryStockItemEntityArray;
import magento.CatalogInventoryStockItemListRequestParam;
import magento.CatalogInventoryStockItemListResponseParam;
import magento.CatalogInventoryStockItemUpdateEntity;
import magento.CatalogInventoryStockItemUpdateRequestParam;
import magento.CatalogInventoryStockItemUpdateResponseParam;
import magento.DirectoryRegionEntity;
import magento.DirectoryRegionEntityArray;
import magento.DirectoryRegionListRequestParam;
import magento.DirectoryRegionListResponseParam;
import magento.Filters;
import magento.LoginParam;
import magento.LoginResponseParam;
import magento.MageApiModelServerWsiHandlerPortType;
import magento.MagentoService;
import magento.OrderItemIdQty;
import magento.OrderItemIdQtyArray;
import magento.SalesOrderCancelRequestParam;
import magento.SalesOrderCancelResponseParam;
import magento.SalesOrderEntity;
import magento.SalesOrderInfoRequestParam;
import magento.SalesOrderInfoResponseParam;
import magento.SalesOrderInvoiceCreateRequestParam;
import magento.SalesOrderInvoiceCreateResponseParam;
import magento.SalesOrderListEntity;
import magento.SalesOrderListEntityArray;
import magento.SalesOrderListRequestParam;
import magento.SalesOrderListResponseParam;
import magento.SalesOrderShipmentAddTrackRequestParam;
import magento.SalesOrderShipmentAddTrackResponseParam;
import magento.SalesOrderShipmentCreateRequestParam;
import magento.SalesOrderShipmentCreateResponseParam;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.LocalDispatcher;

public class MagentoClient {
    public static final String module = MagentoClient.class.getName();
    private static String soapUserName;
    private static String soapPassword;
    private static String magentoServiceWsdlLocation;

    protected LocalDispatcher dispatcher;
    protected Delegator delegator;
    protected GenericValue system;

    public MagentoClient (LocalDispatcher dispatcher, Delegator delegator) {
        this.dispatcher = dispatcher;
        this.delegator = delegator;

        try {
            system = delegator.findOne("UserLogin", true, "userLoginId", "system");
            GenericValue magentoConfiguration = EntityUtil.getFirst((delegator.findList("MagentoConfiguration", null, null, null, null, false))); 
            if (UtilValidate.isNotEmpty(magentoConfiguration)) { 
                soapUserName = (String) magentoConfiguration.get("xmlRpcUserName");
                soapPassword = (String) magentoConfiguration.get("password");
                magentoServiceWsdlLocation = magentoConfiguration.getString("serverUrl");
            }
        } catch (GenericEntityException gee) {
            Debug.logError(gee, module);
            system = delegator.makeValue("UserLogin");
            system.set("userLoginId", "system");
            system.set("partyId", "admin");
            system.set("isSystem", "Y");
        }
    }

    public static MageApiModelServerWsiHandlerPortType getPort() {
        URL url = null;
        MageApiModelServerWsiHandlerPortType port = null;
        try {
            url = new URL(magentoServiceWsdlLocation);
            QName serviceName = new QName("urn:Magento", "MagentoService");

            MagentoService mage = new MagentoService(url, serviceName);
            port = mage.getMageApiModelServerWsiHandlerPort();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            Debug.logError(e.getMessage(), module);
        } 
        return port;
    }
    public static String getMagentoSession() {
        String session = null;
        try {
            LoginParam loginParams = new LoginParam();
            loginParams.setUsername(soapUserName);
            loginParams.setApiKey(soapPassword);
            MageApiModelServerWsiHandlerPortType port = getPort();
            LoginResponseParam loginResponseParam = port.login(loginParams);

            session = loginResponseParam.getResult();
            Debug.logInfo("===========Got Magento session  with sessionId:" +session, module);
        } catch (Exception e) {
            e.printStackTrace();
            Debug.logError("===========Error in getting magento session=====" + e.getMessage(), module);
            return "error";
        }
        return session;
    }

    // Fetches sales order List from magento
    public List<SalesOrderListEntity> getSalesOrderList(Filters filters) {
        List<SalesOrderListEntity> salesOrderList = new ArrayList<SalesOrderListEntity>();

        String magentoSessionId = getMagentoSession();
        SalesOrderListRequestParam salesOrderListRequestParam = new SalesOrderListRequestParam();
        salesOrderListRequestParam.setSessionId(magentoSessionId);
        salesOrderListRequestParam.setFilters(filters);
        MageApiModelServerWsiHandlerPortType port = getPort();
        SalesOrderListResponseParam salesOrderListResponseParam = port.salesOrderList(salesOrderListRequestParam);
        SalesOrderListEntityArray salesOrderListEntityArray = salesOrderListResponseParam.getResult();
        salesOrderList = salesOrderListEntityArray.getComplexObjectArray();
        return salesOrderList;
    }

    // Fetches sales order info from magento
    public SalesOrderEntity getSalesOrderInfo(String orderIncrementId) {
        if (UtilValidate.isEmpty(orderIncrementId)) {
            Debug.logInfo("Empty orderIncrementId.", module);
            return null;
        }
        String magentoSessionId = getMagentoSession();
        SalesOrderInfoRequestParam salesOrderInfoRequestParam = new SalesOrderInfoRequestParam();
        salesOrderInfoRequestParam.setSessionId(magentoSessionId);
        salesOrderInfoRequestParam.setOrderIncrementId(orderIncrementId);

        MageApiModelServerWsiHandlerPortType port = getPort();
        SalesOrderInfoResponseParam salesOrderListResponseParam = port.salesOrderInfo(salesOrderInfoRequestParam);
        SalesOrderEntity salesOrder = salesOrderListResponseParam.getResult();
        return salesOrder;
    }

    public CatalogInventoryStockItemEntityArray getCatalogInventoryStockItemList (String sku) {
        if (UtilValidate.isEmpty(sku)) {
            Debug.logInfo("Empty product's sku.", module);
            return null;
        }
        String magentoSessionId = getMagentoSession();
        CatalogInventoryStockItemListRequestParam catalogInventoryStockItemListRequestParam = new CatalogInventoryStockItemListRequestParam();
        catalogInventoryStockItemListRequestParam.setSessionId(magentoSessionId);
        ArrayOfString productIds = new ArrayOfString();
        productIds.getComplexObjectArray().add(sku);
        catalogInventoryStockItemListRequestParam.setProductIds(productIds);
        MageApiModelServerWsiHandlerPortType port = getPort();
        CatalogInventoryStockItemListResponseParam catalogInventoryStockItemListResponseParam = port.catalogInventoryStockItemList(catalogInventoryStockItemListRequestParam);
        CatalogInventoryStockItemEntityArray catalogInventoryStockItemEntityArray = catalogInventoryStockItemListResponseParam.getResult();
        return catalogInventoryStockItemEntityArray;
    }

    public List<DirectoryRegionEntity> getDirectoryRegionList(String countryGeoCode) {
        if (UtilValidate.isEmpty(countryGeoCode)) {
            Debug.logInfo("Empty countryGeoCode.", module);
            return null;
        }
        List<DirectoryRegionEntity> directoryRegionList = new ArrayList<DirectoryRegionEntity>();
        String magentoSessionId = getMagentoSession();
        DirectoryRegionListRequestParam directoryRegionListRequestParam = new DirectoryRegionListRequestParam();
        directoryRegionListRequestParam.setSessionId(magentoSessionId);
        directoryRegionListRequestParam.setCountry(countryGeoCode);

        MageApiModelServerWsiHandlerPortType port = getPort();
        DirectoryRegionListResponseParam directoryRegionListResponseParam = port.directoryRegionList(directoryRegionListRequestParam);
        DirectoryRegionEntityArray directorRegionEntityArray = directoryRegionListResponseParam.getResult();
        directoryRegionList = directorRegionEntityArray.getComplexObjectArray();
        return directoryRegionList;
    }
    public int cancelSalesOrder(String orderIncrementId) {
        if (UtilValidate.isEmpty(orderIncrementId)) {
            Debug.logInfo("Empty orderIncrementId.", module);
            return 0;
        }
        int isCancelled = 0;
        String magentoSessionId = getMagentoSession();
        SalesOrderCancelRequestParam salesOrderCancelRequestParam = new SalesOrderCancelRequestParam();
        salesOrderCancelRequestParam.setSessionId(magentoSessionId);
        salesOrderCancelRequestParam.setOrderIncrementId(orderIncrementId);

        MageApiModelServerWsiHandlerPortType port = getPort();
        SalesOrderCancelResponseParam salesOrderCancelResponseParam = port.salesOrderCancel(salesOrderCancelRequestParam);
        isCancelled = salesOrderCancelResponseParam.getResult();

        return isCancelled;
    }
    public String createShipment(String orderIncrementId, Map<Integer, Double> orderItemQtyMap) {
        if (UtilValidate.isEmpty(orderIncrementId)) {
            Debug.logInfo("Empty orderIncrementId. Not going to create shipment.", module);
            return null;
        }
        String shipmentIncrementId = null;
        String magentoSessionId = getMagentoSession();
        SalesOrderShipmentCreateRequestParam salesOrderShipmentCreateRequestParam = new SalesOrderShipmentCreateRequestParam();

        OrderItemIdQtyArray orderItemIdQtyArray = new OrderItemIdQtyArray();
        if (UtilValidate.isNotEmpty(orderItemQtyMap)) {
            for (int orderItemId : orderItemQtyMap.keySet()) {
                OrderItemIdQty orderItemIdQty = new OrderItemIdQty();
                orderItemIdQty.setOrderItemId(orderItemId);
                orderItemIdQty.setQty(orderItemQtyMap.get(orderItemId));
                orderItemIdQtyArray.getComplexObjectArray().add(orderItemIdQty);
            }
        }

        salesOrderShipmentCreateRequestParam.setSessionId(magentoSessionId);
        salesOrderShipmentCreateRequestParam.setOrderIncrementId(orderIncrementId);
        salesOrderShipmentCreateRequestParam.setEmail(1);
        salesOrderShipmentCreateRequestParam.setItemsQty(orderItemIdQtyArray);

        MageApiModelServerWsiHandlerPortType port = getPort();
        SalesOrderShipmentCreateResponseParam salesOrderShipmentCreateResponseParam = port.salesOrderShipmentCreate(salesOrderShipmentCreateRequestParam);
        shipmentIncrementId = salesOrderShipmentCreateResponseParam.getResult();
        return shipmentIncrementId;
    }
    public int addTrack(String shipmentIncrementId, String carrierPartyId, String carrierTitle, String trackNumber) {
        if (UtilValidate.isEmpty(shipmentIncrementId) || UtilValidate.isEmpty(carrierPartyId) || UtilValidate.isEmpty(carrierTitle) || UtilValidate.isEmpty(trackNumber)) {
            Debug.logInfo("Not getting complete information while going to add tracking code.", module);
            Debug.logInfo("shipmentIncrementId = "+shipmentIncrementId, module);
            Debug.logInfo("carrierPartyId = "+carrierPartyId, module);
            Debug.logInfo("carrierTitle = "+carrierTitle, module);
            Debug.logInfo("trackNumber = "+trackNumber, module);
            return 0;
        }
        int isTrackingCodeAdded = 0;
        String magentoSessionId = getMagentoSession();
        SalesOrderShipmentAddTrackRequestParam requestParam = new SalesOrderShipmentAddTrackRequestParam();
        requestParam.setSessionId(magentoSessionId);
        requestParam.setShipmentIncrementId(shipmentIncrementId);
        requestParam.setCarrier(carrierPartyId);
        requestParam.setTitle(carrierTitle);
        requestParam.setTrackNumber(trackNumber);

        MageApiModelServerWsiHandlerPortType port = getPort();
        SalesOrderShipmentAddTrackResponseParam responseParam = port.salesOrderShipmentAddTrack(requestParam);
        isTrackingCodeAdded = responseParam.getResult();
        return isTrackingCodeAdded;
    }
    public String createInvoice(String orderIncrementId, Map<Integer, Double> orderItemQtyMap) {
        if (UtilValidate.isEmpty(orderIncrementId)) {
            Debug.logInfo("Empty orderIncrementId. Not going to create invoice.", module);
            return null;
        }
        String invoiceIncrementId = null;
        String magentoSessionId = getMagentoSession();
        OrderItemIdQtyArray orderItemIdQtyArray = new OrderItemIdQtyArray();
        if (UtilValidate.isNotEmpty(orderItemQtyMap)) {
            for (int orderItemId : orderItemQtyMap.keySet()) {
                OrderItemIdQty orderItemIdQty = new OrderItemIdQty();
                orderItemIdQty.setOrderItemId(orderItemId);
                orderItemIdQty.setQty(orderItemQtyMap.get(orderItemId));
                orderItemIdQtyArray.getComplexObjectArray().add(orderItemIdQty);
            }
        }

        SalesOrderInvoiceCreateRequestParam salesOrderInvoiceCreateRequestParam = new SalesOrderInvoiceCreateRequestParam();
        salesOrderInvoiceCreateRequestParam.setSessionId(magentoSessionId);
        salesOrderInvoiceCreateRequestParam.setInvoiceIncrementId(orderIncrementId);
        salesOrderInvoiceCreateRequestParam.setEmail("true");
        salesOrderInvoiceCreateRequestParam.setItemsQty(orderItemIdQtyArray);

        MageApiModelServerWsiHandlerPortType port = getPort();
        SalesOrderInvoiceCreateResponseParam salesOrderInvoiceCreateResponseParam = port.salesOrderInvoiceCreate(salesOrderInvoiceCreateRequestParam);
        invoiceIncrementId = salesOrderInvoiceCreateResponseParam.getResult();
        return invoiceIncrementId;
    }
    public int catalogInventoryStockItemUpdate (String productId, String inventoryCount) {
        if (UtilValidate.isEmpty(productId) || UtilValidate.isEmpty(inventoryCount)) {
            Debug.logInfo("Not getting complete information while going to update catalog inventory stock.", module);
            Debug.logInfo("productId = "+productId+" inventoryCount = "+inventoryCount, module);
        }
        int isStockItemUpdated = 0;
        String magentoSessionId = getMagentoSession();
        CatalogInventoryStockItemUpdateRequestParam requestParam = new CatalogInventoryStockItemUpdateRequestParam();
        requestParam.setSessionId(magentoSessionId);
        requestParam.setProductId(productId);

        CatalogInventoryStockItemUpdateEntity catalogInventoryStockItemUpdateEntity = new CatalogInventoryStockItemUpdateEntity();
        catalogInventoryStockItemUpdateEntity.setQty(inventoryCount);
        requestParam.setData(catalogInventoryStockItemUpdateEntity);

        MageApiModelServerWsiHandlerPortType port = getPort();
        CatalogInventoryStockItemUpdateResponseParam responseParam = port.catalogInventoryStockItemUpdate(requestParam);
        isStockItemUpdated = responseParam.getResult();
        return isStockItemUpdated;
    }
}