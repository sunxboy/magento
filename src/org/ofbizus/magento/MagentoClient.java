package org.ofbizus.magento;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
            }
        } catch (GenericEntityException gee) {
            Debug.logError(gee, module);
            system = delegator.makeValue("UserLogin");
            system.set("userLoginId", "system");
            system.set("partyId", "admin");
            system.set("isSystem", "Y");
        }
    }

    public static String getMagentoSession() {
        String session = null;
        try {
            LoginParam loginParams = new LoginParam();
            loginParams.setUsername(soapUserName);
            loginParams.setApiKey(soapPassword);
            MagentoService mage = new MagentoService();
            MageApiModelServerWsiHandlerPortType port = mage.getMageApiModelServerWsiHandlerPort();
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
        MagentoService mage = new MagentoService();
        MageApiModelServerWsiHandlerPortType port = mage.getMageApiModelServerWsiHandlerPort();
        SalesOrderListResponseParam salesOrderListResponseParam = port.salesOrderList(salesOrderListRequestParam);
        SalesOrderListEntityArray salesOrderListEntityArray = salesOrderListResponseParam.getResult();
        salesOrderList = salesOrderListEntityArray.getComplexObjectArray();
        return salesOrderList;
    }

    // Fetches sales order info from magento
    public SalesOrderEntity getSalesOrderInfo(String orderIncrementId) {
        String magentoSessionId = getMagentoSession();
        SalesOrderInfoRequestParam salesOrderInfoRequestParam = new SalesOrderInfoRequestParam();
        salesOrderInfoRequestParam.setSessionId(magentoSessionId);
        salesOrderInfoRequestParam.setOrderIncrementId(orderIncrementId);

        MagentoService mage = new MagentoService();
        MageApiModelServerWsiHandlerPortType port = mage.getMageApiModelServerWsiHandlerPort();
        SalesOrderInfoResponseParam salesOrderListResponseParam = port.salesOrderInfo(salesOrderInfoRequestParam);
        SalesOrderEntity salesOrder = salesOrderListResponseParam.getResult();
        return salesOrder;
    }

    public CatalogInventoryStockItemEntityArray getCatalogInventoryStockItemList (String sku) {
        String magentoSessionId = getMagentoSession();
        MagentoService mage = new MagentoService();
        CatalogInventoryStockItemListRequestParam catalogInventoryStockItemListRequestParam = new CatalogInventoryStockItemListRequestParam();
        catalogInventoryStockItemListRequestParam.setSessionId(magentoSessionId);
        ArrayOfString productIds = new ArrayOfString();
        productIds.getComplexObjectArray().add(sku);
        catalogInventoryStockItemListRequestParam.setProductIds(productIds);
        MageApiModelServerWsiHandlerPortType port = mage.getMageApiModelServerWsiHandlerPort();
        CatalogInventoryStockItemListResponseParam catalogInventoryStockItemListResponseParam = port.catalogInventoryStockItemList(catalogInventoryStockItemListRequestParam);
        CatalogInventoryStockItemEntityArray catalogInventoryStockItemEntityArray = catalogInventoryStockItemListResponseParam.getResult();
        return catalogInventoryStockItemEntityArray;
    }

    public List<DirectoryRegionEntity> getDirectoryRegionList(String countryGeoCode) {
        List<DirectoryRegionEntity> directoryRegionList = new ArrayList<DirectoryRegionEntity>();
        String magentoSessionId = getMagentoSession();
        DirectoryRegionListRequestParam directoryRegionListRequestParam = new DirectoryRegionListRequestParam();
        directoryRegionListRequestParam.setSessionId(magentoSessionId);
        directoryRegionListRequestParam.setCountry(countryGeoCode);

        MagentoService mage = new MagentoService();
        MageApiModelServerWsiHandlerPortType port = mage.getMageApiModelServerWsiHandlerPort();
        DirectoryRegionListResponseParam directoryRegionListResponseParam = port.directoryRegionList(directoryRegionListRequestParam);
        DirectoryRegionEntityArray directorRegionEntityArray = directoryRegionListResponseParam.getResult();
        directoryRegionList = directorRegionEntityArray.getComplexObjectArray();
        return directoryRegionList;
    }
    public int cancelSalesOrder(String orderIncrementId) {
        int isCancelled = 0;
        String magentoSessionId = getMagentoSession();
        SalesOrderCancelRequestParam salesOrderCancelRequestParam = new SalesOrderCancelRequestParam();
        salesOrderCancelRequestParam.setSessionId(magentoSessionId);
        salesOrderCancelRequestParam.setOrderIncrementId(orderIncrementId);

        MagentoService mage = new MagentoService();
        MageApiModelServerWsiHandlerPortType port = mage.getMageApiModelServerWsiHandlerPort();
        SalesOrderCancelResponseParam salesOrderCancelResponseParam = port.salesOrderCancel(salesOrderCancelRequestParam);
        isCancelled = salesOrderCancelResponseParam.getResult();

        return isCancelled;
    }
    public String createShipment(String orderIncrementId, Map<Integer, Double> orderItemQtyMap) {
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

        MagentoService mage = new MagentoService();
        MageApiModelServerWsiHandlerPortType port = mage.getMageApiModelServerWsiHandlerPort();
        SalesOrderShipmentCreateResponseParam salesOrderShipmentCreateResponseParam = port.salesOrderShipmentCreate(salesOrderShipmentCreateRequestParam);
        shipmentIncrementId = salesOrderShipmentCreateResponseParam.getResult();
        return shipmentIncrementId;
    }
    public int addTrack(String shipmentIncrementId, String carrierPartyId, String carrierTitle, String trackNumber) {
        int isTrackingCodeAdded = 0;
        String magentoSessionId = getMagentoSession();
        SalesOrderShipmentAddTrackRequestParam requestParam = new SalesOrderShipmentAddTrackRequestParam();
        requestParam.setSessionId(magentoSessionId);
        requestParam.setShipmentIncrementId(shipmentIncrementId);
        requestParam.setCarrier(carrierPartyId);
        requestParam.setTitle(carrierTitle);
        requestParam.setTrackNumber(trackNumber);

        MagentoService mage = new MagentoService();
        MageApiModelServerWsiHandlerPortType port = mage.getMageApiModelServerWsiHandlerPort();
        SalesOrderShipmentAddTrackResponseParam responseParam = port.salesOrderShipmentAddTrack(requestParam);
        isTrackingCodeAdded = responseParam.getResult();
        return isTrackingCodeAdded;
    }
    public String createInvoice(String orderIncrementId, Map<Integer, Double> orderItemQtyMap) {
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

        MagentoService mage = new MagentoService();
        MageApiModelServerWsiHandlerPortType port = mage.getMageApiModelServerWsiHandlerPort();
        SalesOrderInvoiceCreateResponseParam salesOrderInvoiceCreateResponseParam = port.salesOrderInvoiceCreate(salesOrderInvoiceCreateRequestParam);
        invoiceIncrementId = salesOrderInvoiceCreateResponseParam.getResult();
        return invoiceIncrementId;
    }
    public int catalogInventoryStockItemUpdate (String productId, String inventoryCount) {
        int isStockItemUpdated = 0;
        String magentoSessionId = getMagentoSession();
        CatalogInventoryStockItemUpdateRequestParam requestParam = new CatalogInventoryStockItemUpdateRequestParam();
        requestParam.setSessionId(magentoSessionId);
        requestParam.setProductId(productId);

        CatalogInventoryStockItemUpdateEntity catalogInventoryStockItemUpdateEntity = new CatalogInventoryStockItemUpdateEntity();
        catalogInventoryStockItemUpdateEntity.setQty(inventoryCount);
        requestParam.setData(catalogInventoryStockItemUpdateEntity);

        MagentoService mage = new MagentoService();
        MageApiModelServerWsiHandlerPortType port = mage.getMageApiModelServerWsiHandlerPort();
        CatalogInventoryStockItemUpdateResponseParam responseParam = port.catalogInventoryStockItemUpdate(requestParam);
        isStockItemUpdated = responseParam.getResult();
        return isStockItemUpdated;
    }
}
