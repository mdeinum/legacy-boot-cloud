package com.apress.prospringmvc.bookstore.domain.support;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.stereotype.Component;

import com.apress.prospringmvc.bookstore.domain.Book;
import com.apress.prospringmvc.bookstore.domain.Account;
import com.apress.prospringmvc.bookstore.domain.Order;
import com.apress.prospringmvc.bookstore.domain.OrderDetail;

/**
 * Builds {@link Order} domain objects
 * 
 * @author Marten Deinum
 * @author Koen Serneels
 * 
 */
@Component
public class OrderBuilder extends EntityBuilder<Order> {

    private List<OrderDetail> orderDetails;

    @Override
    void initProduct() {
        this.entity = new Order();
        this.orderDetails = new ArrayList<OrderDetail>();
    }

    public OrderBuilder addBook(Book book, int quantity) {
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setQuantity(quantity);

        orderDetail.setBook(book);
        this.orderDetails.add(orderDetail);
        return this;
    }

    public OrderBuilder addBooks(Map<Book, Integer> map) {
        for (Entry<Book, Integer> entry : map.entrySet()) {
            addBook(entry.getKey(), entry.getValue());
        }
        return this;
    }

    public OrderBuilder deliveryDate(Date date) {
        this.entity.setDeliveryDate(date);
        return this;
    }

    public OrderBuilder orderDate(Date date) {
        this.entity.setOrderDate(date);
        return this;
    }

    public OrderBuilder account(Account account) {
        this.entity.setAccount(account);
        return this;
    }

    @Override
    Order assembleProduct() {
        for (OrderDetail orderDetail : this.orderDetails) {
            this.entity.addOrderDetail(orderDetail);
        }
        return this.entity;
    }
}