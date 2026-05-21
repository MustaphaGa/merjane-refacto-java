package com.nimbleways.springboilerplate.services.implementations;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.ProductRepository;

@Service
@Transactional
public class ProductService {
    private static final String NORMAL_TYPE = "NORMAL";
    private static final String SEASONAL_TYPE = "SEASONAL";
    private static final String EXPIRABLE_TYPE = "EXPIRABLE";

    private final ProductRepository productRepository;
    private final NotificationService notificationService;

    public ProductService(ProductRepository productRepository, NotificationService notificationService) {
        this.productRepository = productRepository;
        this.notificationService = notificationService;
    }

    public void processOrderedProduct(Product product) {
        if (NORMAL_TYPE.equals(product.getType())) {
            handleNormalProduct(product);
        } else if (SEASONAL_TYPE.equals(product.getType())) {
            processSeasonalProduct(product);
        } else if (EXPIRABLE_TYPE.equals(product.getType())) {
            processExpirableProduct(product);
        }
    }

    public void notifyDelay(int leadTime, Product p) {
        p.setLeadTime(leadTime);
        productRepository.save(p);
        notificationService.sendDelayNotification(leadTime, p.getName());
    }

    public void handleSeasonalProduct(Product p) {
        LocalDate today = LocalDate.now();
        if (today.plusDays(p.getLeadTime()).isAfter(p.getSeasonEndDate())) {
            notificationService.sendOutOfStockNotification(p.getName());
            p.setAvailable(0);
            productRepository.save(p);
        } else if (p.getSeasonStartDate().isAfter(today)) {
            notificationService.sendOutOfStockNotification(p.getName());
            productRepository.save(p);
        } else {
            notifyDelay(p.getLeadTime(), p);
        }
    }

    public void handleExpiredProduct(Product p) {
        if (p.getAvailable() > 0 && p.getExpiryDate().isAfter(LocalDate.now())) {
            decrementAvailableStock(p);
        } else {
            notificationService.sendExpirationNotification(p.getName(), p.getExpiryDate());
            p.setAvailable(0);
            productRepository.save(p);
        }
    }

    private void handleNormalProduct(Product product) {
        if (product.getAvailable() > 0) {
            decrementAvailableStock(product);
        } else if (product.getLeadTime() > 0) {
            notifyDelay(product.getLeadTime(), product);
        }
    }

    private void processSeasonalProduct(Product product) {
        LocalDate today = LocalDate.now();
        if (isInSeason(product, today) && product.getAvailable() > 0) {
            decrementAvailableStock(product);
        } else {
            handleSeasonalProduct(product);
        }
    }

    private void processExpirableProduct(Product product) {
        if (product.getAvailable() > 0 && product.getExpiryDate().isAfter(LocalDate.now())) {
            decrementAvailableStock(product);
        } else {
            handleExpiredProduct(product);
        }
    }

    private boolean isInSeason(Product product, LocalDate date) {
        return date.isAfter(product.getSeasonStartDate()) && date.isBefore(product.getSeasonEndDate());
    }

    private void decrementAvailableStock(Product product) {
        product.setAvailable(product.getAvailable() - 1);
        productRepository.save(product);
    }
}
