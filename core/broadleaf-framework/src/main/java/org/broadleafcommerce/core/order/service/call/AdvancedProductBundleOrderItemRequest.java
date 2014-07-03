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
package org.broadleafcommerce.core.order.service.call;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.core.catalog.domain.Category;
import org.broadleafcommerce.core.catalog.domain.ProductBundle;
import org.broadleafcommerce.core.catalog.domain.Sku;
import org.broadleafcommerce.core.order.domain.Order;

public class AdvancedProductBundleOrderItemRequest {

    protected String name;
    protected Category category;
    protected Sku sku;
    protected Order order;
    protected int quantity;
    protected ProductBundle productBundle;
    private Map<String,String> itemAttributes = new HashMap<String,String>();
    protected Money salePriceOverride;
    protected Money retailPriceOverride;
    protected List<OrderItemRequestDTO> bundleOrderItems = new ArrayList<OrderItemRequestDTO>();


    public AdvancedProductBundleOrderItemRequest() {}
    
    public String getName() {
        return name;
    }

    public AdvancedProductBundleOrderItemRequest setName(String name) {
        this.name = name;
        return this;
    }

    public Category getCategory() {
        return category;
    }

    public AdvancedProductBundleOrderItemRequest setCategory(Category category) {
        this.category = category;
        return this;
    }
    
    public Sku getSku() {
        return sku;
    }

    public AdvancedProductBundleOrderItemRequest setSku(Sku sku) {
        this.sku = sku;
        return this;
    }
    
    public AdvancedProductBundleOrderItemRequest setOrder(Order order) {
        this.order = order;
        return this;
    }
    
    public Order getOrder() {
        return order;
    }

    public int getQuantity() {
        return quantity;
    }

    public AdvancedProductBundleOrderItemRequest setQuantity(int quantity) {
        this.quantity = quantity;
        return this;
    }

    public ProductBundle getProductBundle() {
        return productBundle;
    }

    public AdvancedProductBundleOrderItemRequest setProductBundle(ProductBundle productBundle) {
        this.productBundle = productBundle;
        return this;
    }

    public Map<String, String> getItemAttributes() {
        return itemAttributes;
    }

    public AdvancedProductBundleOrderItemRequest setItemAttributes(Map<String, String> itemAttributes) {
        this.itemAttributes = itemAttributes;
        return this;
    }

    public Money getSalePriceOverride() {
        return salePriceOverride;
    }

    public void setSalePriceOverride(Money salePriceOverride) {
        this.salePriceOverride = salePriceOverride;
    }

    public Money getRetailPriceOverride() {
        return retailPriceOverride;
    }

    public void setRetailPriceOverride(Money retailPriceOverride) {
        this.retailPriceOverride = retailPriceOverride;
    }

    public List<OrderItemRequestDTO> getBundleOrderItems() {
        return bundleOrderItems;
    }

    public void setBundleOrderItems(List<OrderItemRequestDTO> bundleOrderItems) {
        this.bundleOrderItems = bundleOrderItems;
    }
}
