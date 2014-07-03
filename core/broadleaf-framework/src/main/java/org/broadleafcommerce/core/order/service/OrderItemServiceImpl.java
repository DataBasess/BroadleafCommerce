/*
 * #%L
 * BroadleafCommerce Framework
 * %%
 * Copyright (C) 2009 - 2013 Broadleaf Commerce
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.broadleafcommerce.core.order.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Resource;

import org.broadleafcommerce.core.catalog.domain.Category;
import org.broadleafcommerce.core.catalog.domain.Product;
import org.broadleafcommerce.core.catalog.domain.ProductBundle;
import org.broadleafcommerce.core.catalog.domain.ProductOption;
import org.broadleafcommerce.core.catalog.domain.ProductOptionValue;
import org.broadleafcommerce.core.catalog.domain.Sku;
import org.broadleafcommerce.core.catalog.domain.SkuBundleItem;
import org.broadleafcommerce.core.catalog.service.dynamic.DynamicSkuPrices;
import org.broadleafcommerce.core.catalog.service.dynamic.DynamicSkuPricingService;
import org.broadleafcommerce.core.order.dao.OrderItemDao;
import org.broadleafcommerce.core.order.domain.BundleOrderItem;
import org.broadleafcommerce.core.order.domain.DiscreteOrderItem;
import org.broadleafcommerce.core.order.domain.DiscreteOrderItemFeePrice;
import org.broadleafcommerce.core.order.domain.GiftWrapOrderItem;
import org.broadleafcommerce.core.order.domain.OrderItem;
import org.broadleafcommerce.core.order.domain.OrderItemAttribute;
import org.broadleafcommerce.core.order.domain.OrderItemAttributeImpl;
import org.broadleafcommerce.core.order.domain.PersonalMessage;
import org.broadleafcommerce.core.order.service.call.AbstractOrderItemRequest;
import org.broadleafcommerce.core.order.service.call.AdvancedProductBundleOrderItemRequest;
import org.broadleafcommerce.core.order.service.call.BundleOrderItemRequest;
import org.broadleafcommerce.core.order.service.call.DiscreteOrderItemRequest;
import org.broadleafcommerce.core.order.service.call.GiftWrapOrderItemRequest;
import org.broadleafcommerce.core.order.service.call.NonDiscreteOrderItemRequestDTO;
import org.broadleafcommerce.core.order.service.call.OrderItemRequest;
import org.broadleafcommerce.core.order.service.call.OrderItemRequestDTO;
import org.broadleafcommerce.core.order.service.call.ProductBundleOrderItemRequest;
import org.broadleafcommerce.core.order.service.type.OrderItemType;
import org.springframework.stereotype.Service;

@Service("blOrderItemService")
public class OrderItemServiceImpl implements OrderItemService {

    @Resource(name="blOrderItemDao")
    protected OrderItemDao orderItemDao;

    @Resource(name="blDynamicSkuPricingService" )
    protected DynamicSkuPricingService dynamicSkuPricingService;

    @Override
    public OrderItem readOrderItemById(final Long orderItemId) {
        return orderItemDao.readOrderItemById(orderItemId);
    }

    @Override
    public OrderItem saveOrderItem(final OrderItem orderItem) {
        return orderItemDao.saveOrderItem(orderItem);
    }
    
    @Override
    public void delete(final OrderItem item) {
        orderItemDao.delete(item);
    }
    
    @Override
    public PersonalMessage createPersonalMessage() {
        return orderItemDao.createPersonalMessage();
    }
    
    protected void populateDiscreteOrderItem(DiscreteOrderItem item, AbstractOrderItemRequest itemRequest) {
        item.setSku(itemRequest.getSku());
        item.setQuantity(itemRequest.getQuantity());
        item.setCategory(itemRequest.getCategory());
        item.setProduct(itemRequest.getProduct());
        item.setOrder(itemRequest.getOrder());
        Map<String, String> attributes = itemRequest.getItemAttributes();
        populateProductOptionAttributes(item, attributes);
    }

    protected void populateProductOptionAttributes(OrderItem item, Map<String, String> attributes) {
        if (attributes != null && attributes.size() > 0) {
            Map<String, OrderItemAttribute> orderItemAttributes = item.getOrderItemAttributes();
            if (item.getOrderItemAttributes() == null) {
                orderItemAttributes = new HashMap<String, OrderItemAttribute>();
                item.setOrderItemAttributes(orderItemAttributes);
            }
            for (String key : attributes.keySet()) {
                String value = attributes.get(key);
                OrderItemAttribute attribute = new OrderItemAttributeImpl();
                attribute.setName(key);
                attribute.setValue(value);
                attribute.setOrderItem(item);
                orderItemAttributes.put(key, attribute);
            }
        }
    }
    
    @Override
    public OrderItem createOrderItem(final OrderItemRequest itemRequest) {
        final OrderItem item = orderItemDao.create(OrderItemType.BASIC);
        item.setName(itemRequest.getItemName());
        item.setQuantity(itemRequest.getQuantity());
        item.setOrder(itemRequest.getOrder());
        
        if (itemRequest.getSalePriceOverride() != null) {
            item.setSalePriceOverride(Boolean.TRUE);
            item.setSalePrice(itemRequest.getSalePriceOverride());
        }

        if (itemRequest.getRetailPriceOverride() != null) {
            item.setRetailPriceOverride(Boolean.TRUE);
            item.setRetailPrice(itemRequest.getRetailPriceOverride());
        }
        
        return item;
    }

    @Override
    public OrderItem updateDiscreteOrderItem(OrderItem item, final DiscreteOrderItemRequest itemRequest) {
        List<ProductOption> productOptions = null;
        if (item instanceof DiscreteOrderItem) {
            productOptions = ((DiscreteOrderItem) item).getProduct().getProductOptions();
        } else if (item instanceof BundleOrderItem) {
            productOptions = ((BundleOrderItem) item).getProduct().getProductOptions();
        }
        List<String> removeKeys = new ArrayList<String>();
        if (productOptions != null && itemRequest.getItemAttributes() != null) {
            for (String name : itemRequest.getItemAttributes().keySet()) {
                //we do not let them update all product options. 
                //Only allow them to update those options that can have validation to take place at later time
                //if  option.getProductOptionValidationType()  is null then it might change the sku, so we dont allow those
                for (ProductOption option : productOptions) {
                    if (option.getAttributeName().equals(name) && option.getProductOptionValidationStrategyType() == null) {

                        removeKeys.add(name);
                        break;
                    }
                }
            }
        }
        for (String name : removeKeys) {
            itemRequest.getItemAttributes().remove(name);
        }
        populateProductOptionAttributes(item, itemRequest.getItemAttributes());
        return item;
    }

    @Override
    public DiscreteOrderItem createDiscreteOrderItem(final DiscreteOrderItemRequest itemRequest) {
        final DiscreteOrderItem item = (DiscreteOrderItem) orderItemDao.create(OrderItemType.DISCRETE);
        populateDiscreteOrderItem(item, itemRequest);
        
        item.setBundleOrderItem(itemRequest.getBundleOrderItem());
        item.setBaseSalePrice(itemRequest.getSalePriceOverride()==null?itemRequest.getSku().getSalePrice():itemRequest.getSalePriceOverride());
        item.setBaseRetailPrice(itemRequest.getSku().getRetailPrice());
        item.setDiscreteOrderItemFeePrices(itemRequest.getDiscreteOrderItemFeePrices());

        if (itemRequest.getSalePriceOverride() != null) {
            item.setSalePriceOverride(Boolean.TRUE);
            item.setSalePrice(itemRequest.getSalePriceOverride());
            item.setBaseSalePrice(itemRequest.getSalePriceOverride());
        }

        if (itemRequest.getRetailPriceOverride() != null) {
            item.setRetailPriceOverride(Boolean.TRUE);
            item.setRetailPrice(itemRequest.getRetailPriceOverride());
            item.setBaseRetailPrice(itemRequest.getRetailPriceOverride());
        }

        for (DiscreteOrderItemFeePrice feePrice : item.getDiscreteOrderItemFeePrices()) {
            feePrice.setDiscreteOrderItem(item);
        }
        
        item.setPersonalMessage(itemRequest.getPersonalMessage());

        return item;
    }

    public DiscreteOrderItem createDiscreteOrderItem(final AbstractOrderItemRequest itemRequest) {
        final DiscreteOrderItem item = (DiscreteOrderItem) orderItemDao.create(OrderItemType.DISCRETE);
        populateDiscreteOrderItem(item, itemRequest);
        item.setBaseSalePrice(itemRequest.getSku().getSalePrice());
        item.setBaseRetailPrice(itemRequest.getSku().getRetailPrice());
        // item.updatePrices();
        item.updateSaleAndRetailPrices();

        item.assignFinalPrice();
        item.setPersonalMessage(itemRequest.getPersonalMessage());

        return item;
    }
    
    @Override
    public DiscreteOrderItem createDynamicPriceDiscreteOrderItem(final DiscreteOrderItemRequest itemRequest, @SuppressWarnings("rawtypes") HashMap skuPricingConsiderations) {
        final DiscreteOrderItem item = (DiscreteOrderItem) orderItemDao.create(OrderItemType.EXTERNALLY_PRICED);
        populateDiscreteOrderItem(item, itemRequest);

        DynamicSkuPrices prices = dynamicSkuPricingService.getSkuPrices(itemRequest.getSku(), skuPricingConsiderations);
        item.setBundleOrderItem(itemRequest.getBundleOrderItem());
        item.setBaseRetailPrice(prices.getRetailPrice());
        item.setBaseSalePrice(prices.getSalePrice());
        item.setSalePrice(prices.getSalePrice());
        item.setRetailPrice(prices.getRetailPrice());

        if (itemRequest.getSalePriceOverride() != null) {
            item.setSalePriceOverride(Boolean.TRUE);
            item.setSalePrice(itemRequest.getSalePriceOverride());
            item.setBaseSalePrice(itemRequest.getSalePriceOverride());
        }

        if (itemRequest.getRetailPriceOverride() != null) {
            item.setRetailPriceOverride(Boolean.TRUE);
            item.setRetailPrice(itemRequest.getRetailPriceOverride());
            item.setBaseRetailPrice(itemRequest.getRetailPriceOverride());
        }

        item.setDiscreteOrderItemFeePrices(itemRequest.getDiscreteOrderItemFeePrices());
        for (DiscreteOrderItemFeePrice fee : itemRequest.getDiscreteOrderItemFeePrices()) {
            item.setSalePrice(item.getSalePrice().add(fee.getAmount()));
            item.setRetailPrice(item.getRetailPrice().add(fee.getAmount()));
        }

        item.setPersonalMessage(itemRequest.getPersonalMessage());

        return item;
    }

    @Override
    public GiftWrapOrderItem createGiftWrapOrderItem(final GiftWrapOrderItemRequest itemRequest) {
        final GiftWrapOrderItem item = (GiftWrapOrderItem) orderItemDao.create(OrderItemType.GIFTWRAP);
        item.setSku(itemRequest.getSku());
        item.setOrder(itemRequest.getOrder());
        item.setBundleOrderItem(itemRequest.getBundleOrderItem());
        item.setQuantity(itemRequest.getQuantity());
        item.setCategory(itemRequest.getCategory());
        item.setProduct(itemRequest.getProduct());
        item.setBaseSalePrice(itemRequest.getSku().getSalePrice());
        item.setBaseRetailPrice(itemRequest.getSku().getRetailPrice());
        item.setDiscreteOrderItemFeePrices(itemRequest.getDiscreteOrderItemFeePrices());

        if (itemRequest.getSalePriceOverride() != null) {
            item.setSalePriceOverride(Boolean.TRUE);
            item.setSalePrice(itemRequest.getSalePriceOverride());
            item.setBaseSalePrice(itemRequest.getSalePriceOverride());
        }

        if (itemRequest.getRetailPriceOverride() != null) {
            item.setRetailPriceOverride(Boolean.TRUE);
            item.setRetailPrice(itemRequest.getRetailPriceOverride());
            item.setBaseRetailPrice(itemRequest.getRetailPriceOverride());
        }

        //item.updatePrices();
        item.updateSaleAndRetailPrices();
        item.assignFinalPrice();
        item.getWrappedItems().addAll(itemRequest.getWrappedItems());
        for (OrderItem orderItem : item.getWrappedItems()) {
            orderItem.setGiftWrapOrderItem(item);
        }

        return item;
    }

    @Override
    public BundleOrderItem createBundleOrderItem(final BundleOrderItemRequest itemRequest) {
        final BundleOrderItem item = (BundleOrderItem) orderItemDao.create(OrderItemType.BUNDLE);
        item.setQuantity(itemRequest.getQuantity());
        item.setCategory(itemRequest.getCategory());
        item.setName(itemRequest.getName());
        item.setBundleOrderItemFeePrices(itemRequest.getBundleOrderItemFeePrices());
        item.setOrder(itemRequest.getOrder());

        if (itemRequest.getSalePriceOverride() != null) {
            item.setSalePriceOverride(Boolean.TRUE);
            item.setSalePrice(itemRequest.getSalePriceOverride());
            item.setBaseSalePrice(itemRequest.getSalePriceOverride());
        }

        if (itemRequest.getRetailPriceOverride() != null) {
            item.setRetailPriceOverride(Boolean.TRUE);
            item.setRetailPrice(itemRequest.getRetailPriceOverride());
            item.setBaseRetailPrice(itemRequest.getRetailPriceOverride());
        }

        for (DiscreteOrderItemRequest discreteItemRequest : itemRequest.getDiscreteOrderItems()) {
            discreteItemRequest.setBundleOrderItem(item);
            DiscreteOrderItem discreteOrderItem;
            if (discreteItemRequest instanceof GiftWrapOrderItemRequest) {
                discreteOrderItem = createGiftWrapOrderItem((GiftWrapOrderItemRequest) discreteItemRequest);
            } else {
                discreteOrderItem = createDiscreteOrderItem(discreteItemRequest);
            }
            item.getDiscreteOrderItems().add(discreteOrderItem);
        }

        return item;
    }
    
    @Override
    public BundleOrderItem createBundleOrderItem(final ProductBundleOrderItemRequest itemRequest, boolean saveItem) {
        ProductBundle productBundle = itemRequest.getProductBundle();
        BundleOrderItem bundleOrderItem = (BundleOrderItem) orderItemDao.create(OrderItemType.BUNDLE);
        bundleOrderItem.setQuantity(itemRequest.getQuantity());
        bundleOrderItem.setCategory(itemRequest.getCategory());
        bundleOrderItem.setSku(itemRequest.getSku());
        bundleOrderItem.setName(itemRequest.getName());
        bundleOrderItem.setProductBundle(productBundle);
        bundleOrderItem.setOrder(itemRequest.getOrder());

        if (itemRequest.getSalePriceOverride() != null) {
            bundleOrderItem.setSalePriceOverride(Boolean.TRUE);
            bundleOrderItem.setSalePrice(itemRequest.getSalePriceOverride());
            bundleOrderItem.setBaseSalePrice(itemRequest.getSalePriceOverride());
        }

        if (itemRequest.getRetailPriceOverride() != null) {
            bundleOrderItem.setRetailPriceOverride(Boolean.TRUE);
            bundleOrderItem.setRetailPrice(itemRequest.getRetailPriceOverride());
            bundleOrderItem.setBaseRetailPrice(itemRequest.getRetailPriceOverride());
        }

        for (SkuBundleItem skuBundleItem : productBundle.getSkuBundleItems()) {
            Product bundleProduct = skuBundleItem.getBundle();
            Sku bundleSku = skuBundleItem.getSku();

            Category bundleCategory = null;
            if (itemRequest.getCategory() != null) {
                bundleCategory = itemRequest.getCategory();
            } 
    
            if (bundleCategory == null && bundleProduct != null) {
                bundleCategory = bundleProduct.getDefaultCategory();
            }

            DiscreteOrderItemRequest bundleItemRequest = new DiscreteOrderItemRequest();
            bundleItemRequest.setCategory(bundleCategory);
            bundleItemRequest.setProduct(bundleProduct);
            bundleItemRequest.setQuantity(skuBundleItem.getQuantity());
            bundleItemRequest.setSku(bundleSku);
            bundleItemRequest.setItemAttributes(itemRequest.getItemAttributes());
            bundleItemRequest.setSalePriceOverride(skuBundleItem.getSalePrice());
            bundleItemRequest.setBundleOrderItem(bundleOrderItem);
            
            DiscreteOrderItem bundleDiscreteItem = createDiscreteOrderItem(bundleItemRequest);
            bundleDiscreteItem.setSkuBundleItem(skuBundleItem);
            bundleOrderItem.getDiscreteOrderItems().add(bundleDiscreteItem);
        }
        
        if (saveItem) {
            bundleOrderItem = (BundleOrderItem) saveOrderItem(bundleOrderItem);
        }

        return bundleOrderItem;
    }

    @Override
    public BundleOrderItem createBundleOrderItem(final ProductBundleOrderItemRequest itemRequest) {
        return createBundleOrderItem(itemRequest, true);
    }
    
    @Override
    public OrderItemRequestDTO buildOrderItemRequestDTOFromOrderItem(OrderItem item) {
        OrderItemRequestDTO orderItemRequest; 
        if (item instanceof DiscreteOrderItem) {
            DiscreteOrderItem doi = (DiscreteOrderItem) item;
            orderItemRequest = new OrderItemRequestDTO();
            orderItemRequest.setQuantity(doi.getQuantity());
            
            if (doi.getCategory() != null) {
                orderItemRequest.setCategoryId(doi.getCategory().getId());
            }
            
            if (doi.getProduct() != null) {
                orderItemRequest.setProductId(doi.getProduct().getId());
            }
            
            if (doi.getSku() != null) {
                orderItemRequest.setSkuId(doi.getSku().getId());
            }
            
            if (doi.getOrderItemAttributes() != null) {
                for (Entry<String, OrderItemAttribute> entry : item.getOrderItemAttributes().entrySet()) {
                    orderItemRequest.getItemAttributes().put(entry.getKey(), entry.getValue().getValue());
                }
            }
        } else {
            orderItemRequest = new NonDiscreteOrderItemRequestDTO();
            NonDiscreteOrderItemRequestDTO ndr = (NonDiscreteOrderItemRequestDTO) orderItemRequest;
            
            ndr.setItemName(item.getName());
            ndr.setQuantity(item.getQuantity());
            ndr.setOverrideRetailPrice(item.getRetailPrice());
            ndr.setOverrideSalePrice(item.getSalePrice());
        }
        
        return orderItemRequest;
    }

    @Override
    public OrderItem createBundleOrderItem(AdvancedProductBundleOrderItemRequest itemRequest, boolean saveItem) {
        ProductBundle productBundle = itemRequest.getProductBundle();
        BundleOrderItem bundleOrderItem = (BundleOrderItem) orderItemDao.create(OrderItemType.BUNDLE);
        bundleOrderItem.setQuantity(itemRequest.getQuantity());
        bundleOrderItem.setCategory(itemRequest.getCategory());
        bundleOrderItem.setSku(itemRequest.getSku());
        bundleOrderItem.setName(itemRequest.getName());
        bundleOrderItem.setProductBundle(productBundle);
        bundleOrderItem.setOrder(itemRequest.getOrder());

        if (itemRequest.getSalePriceOverride() != null) {
            bundleOrderItem.setSalePriceOverride(Boolean.TRUE);
            bundleOrderItem.setSalePrice(itemRequest.getSalePriceOverride());
            bundleOrderItem.setBaseSalePrice(itemRequest.getSalePriceOverride());
        }

        if (itemRequest.getRetailPriceOverride() != null) {
            bundleOrderItem.setRetailPriceOverride(Boolean.TRUE);
            bundleOrderItem.setRetailPrice(itemRequest.getRetailPriceOverride());
            bundleOrderItem.setBaseRetailPrice(itemRequest.getRetailPriceOverride());
        }

        replaceChildBundleItemsWithOptionsWithMatchingSkus(itemRequest);

        for (SkuBundleItem skuBundleItem : productBundle.getSkuBundleItems()) {

            Product bundleProduct = skuBundleItem.getBundle();
            Sku bundleSku = skuBundleItem.getSku();



            Category bundleCategory = null;
            if (itemRequest.getCategory() != null) {
                bundleCategory = itemRequest.getCategory();
            }

            if (bundleCategory == null && bundleProduct != null) {
                bundleCategory = bundleProduct.getDefaultCategory();
            }

            DiscreteOrderItemRequest bundleItemRequest = new DiscreteOrderItemRequest();
            bundleItemRequest.setCategory(bundleCategory);
            bundleItemRequest.setProduct(bundleProduct);
            bundleItemRequest.setQuantity(skuBundleItem.getQuantity());
            bundleItemRequest.setSku(bundleSku);
            bundleItemRequest.setItemAttributes(itemRequest.getItemAttributes());
            bundleItemRequest.setSalePriceOverride(skuBundleItem.getSalePrice());
            bundleItemRequest.setBundleOrderItem(bundleOrderItem);

            DiscreteOrderItem bundleDiscreteItem = createDiscreteOrderItem(bundleItemRequest);
            bundleDiscreteItem.setSkuBundleItem(skuBundleItem);
            bundleOrderItem.getDiscreteOrderItems().add(bundleDiscreteItem);
        }

        if (saveItem) {
            bundleOrderItem = (BundleOrderItem) saveOrderItem(bundleOrderItem);
        }

        return bundleOrderItem;
    }

    private void replaceChildBundleItemsWithOptionsWithMatchingSkus(AdvancedProductBundleOrderItemRequest itemRequest) {
        for (OrderItemRequestDTO item : itemRequest.getBundleOrderItems()) {
            Map<String, String> itemAttributes = item.getItemAttributes();
            // Loop through the Skus on the product bundle and replace them with a matching Sku
            for (SkuBundleItem bundledSku : itemRequest.getProductBundle().getSkuBundleItems()) {
                // TODO Check if this is an item that is eligible for sku replacement
                // TODO The flag will need to be added to framework instead of extending SkuBundleItem
                List<String> attributesFound = new ArrayList<String>();
                if (bundledSku.getSku().getProduct().getId().equals(item.getProductId())) {
                    // Loop through all the available Skus
                    List<Sku> availableSkus = bundledSku.getSku().getProduct().getAllSkus();
                    for (Sku sku : availableSkus) {
                        // Loop through all the ProductOptions on the Sku
                        for (ProductOptionValue optionValue : sku.getProductOptionValuesCollection()) {
                            String skuAttributeName = optionValue.getProductOption().getAttributeName();
                            String skuAttributeValue = optionValue.getAttributeValue();
                            // Loop through the itemAttributes passed in to find matches against the Sku
                            for (Map.Entry<String, String> requestAttribute : itemAttributes.entrySet()) {
                                String requestAttributeName = requestAttribute.getKey();
                                String requestAttributeValue = requestAttribute.getValue();
                                // If attribute name matches, proceed to check the value
                                if (skuAttributeName.equals(requestAttributeName)) {
                                    // If the value matches, add the ProductOption to the list of matching attributes
                                    if (skuAttributeValue.equals(requestAttributeValue)) {
                                        attributesFound.add(skuAttributeName);
                                    }
                                }
                            }   // -- End itemAttribute Loop
                        }   // -- End ProductOptions on Sku loop
                        // Set sku if it matched on all attributes otherwise clear the matches found for the next sku
                        if (itemAttributes.size() == attributesFound.size()) {
                            bundledSku.setSku(sku);
                            // A match has been found, clear the list
                            attributesFound.clear();
                        } else {
                            attributesFound.clear();
                        }
                    }   // -- End Sku loop
                }
            }   // -- End loop on the bundledSkus which are eligible for Sku matching based on ProductOptions
        }   // -- End loop on bundles items that were passed in with ProductOptions selected on the front end
        // Clear out the child items; All items were replaced by a match otherwise a it maintained the original Sku
        itemRequest.getBundleOrderItems().clear();
    }

}
