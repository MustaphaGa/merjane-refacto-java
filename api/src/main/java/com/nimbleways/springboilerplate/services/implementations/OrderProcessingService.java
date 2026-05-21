package com.nimbleways.springboilerplate.services.implementations;

import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.OrderRepository;

@Service
public class OrderProcessingService {
    private final OrderRepository orderRepository;
    private final ProductService productService;

    public OrderProcessingService(OrderRepository orderRepository, ProductService productService) {
        this.orderRepository = orderRepository;
        this.productService = productService;
    }

    @Transactional
    public Long processOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(NoSuchElementException::new);

        for (Product product : order.getItems()) {
            productService.processOrderedProduct(product);
        }

        return order.getId();
    }
}
