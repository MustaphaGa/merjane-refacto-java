package com.nimbleways.springboilerplate.services.implementations;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.utils.Annotations.UnitTest;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@UnitTest
public class MyUnitTests {

    @Mock
    private NotificationService notificationService;
    @Mock
    private ProductRepository productRepository;
    @InjectMocks 
    private ProductService productService;

    @Test
    public void normalProductInStockShouldDecreaseAvailableStock() {
        Product product = new Product(null, 15, 3, "NORMAL", "Coixial Cable", null, null, null);

        productService.processOrderedProduct(product);

        assertEquals(2, product.getAvailable());
        verify(productRepository).save(product);
        verify(notificationService, never()).sendDelayNotification(Mockito.anyInt(), Mockito.anyString());
    }

    @Test
    public void normalProductOutOfStockShouldNotifyDelayWhenLeadTimeIsAvailable() {
        Product product = new Product(null, 15, 0, "NORMAL", "RJ45 Cable", null, null, null);

        productService.processOrderedProduct(product);

        assertEquals(0, product.getAvailable());
        assertEquals(15, product.getLeadTime());
        verify(productRepository).save(product);
        verify(notificationService).sendDelayNotification(product.getLeadTime(), product.getName());
    }

    @Test
    public void normalProductOutOfStockShouldDoNothingWhenLeadTimeIsUnavailable() {
        Product product = new Product(null, 0, 0, "NORMAL", "HDMI Cable", null, null, null);

        productService.processOrderedProduct(product);

        assertEquals(0, product.getAvailable());
        verify(productRepository, never()).save(product);
        verify(notificationService, never()).sendDelayNotification(Mockito.anyInt(), Mockito.anyString());
    }

    @Test
    public void seasonalProductInSeasonAndInStockShouldDecreaseAvailableStock() {
        Product product = new Product(null, 15, 30, "SEASONAL", "Watermelon", null,
                LocalDate.now().minusDays(2), LocalDate.now().plusDays(58));

        productService.processOrderedProduct(product);

        assertEquals(29, product.getAvailable());
        verify(productRepository).save(product);
        verify(notificationService, never()).sendOutOfStockNotification(product.getName());
    }

    @Test
    public void seasonalProductShouldNotifyDelayWhenRestockFitsCurrentSeason() {
        Product product = new Product(null, 15, 0, "SEASONAL", "Strawberries", null,
                LocalDate.now().minusDays(2), LocalDate.now().plusDays(58));

        productService.processOrderedProduct(product);

        assertEquals(0, product.getAvailable());
        verify(productRepository).save(product);
        verify(notificationService).sendDelayNotification(product.getLeadTime(), product.getName());
    }

    @Test
    public void seasonalProductShouldBecomeUnavailableWhenRestockExceedsSeasonEnd() {
        Product product = new Product(null, 90, 0, "SEASONAL", "Cherries", null,
                LocalDate.now().minusDays(2), LocalDate.now().plusDays(10));

        productService.processOrderedProduct(product);

        assertEquals(0, product.getAvailable());
        verify(productRepository).save(product);
        verify(notificationService).sendOutOfStockNotification(product.getName());
    }

    @Test
    public void seasonalProductBeforeSeasonShouldNotifyOutOfStock() {
        Product product = new Product(null, 15, 30, "SEASONAL", "Grapes", null,
                LocalDate.now().plusDays(180), LocalDate.now().plusDays(240));

        productService.processOrderedProduct(product);

        assertEquals(30, product.getAvailable());
        verify(productRepository).save(product);
        verify(notificationService).sendOutOfStockNotification(product.getName());
    }

    @Test
    public void expirableProductInStockAndNotExpiredShouldDecreaseAvailableStock() {
        Product product = new Product(null, 15, 30, "EXPIRABLE", "Butter",
                LocalDate.now().plusDays(26), null, null);

        productService.processOrderedProduct(product);

        assertEquals(29, product.getAvailable());
        verify(productRepository).save(product);
        verify(notificationService, never()).sendExpirationNotification(Mockito.anyString(), Mockito.any());
    }

    @Test
    public void expirableProductPastExpiryDateShouldBecomeUnavailable() {
        Product product = new Product(null, 90, 6, "EXPIRABLE", "Milk",
                LocalDate.now().minusDays(2), null, null);

        productService.processOrderedProduct(product);

        assertEquals(0, product.getAvailable());
        verify(productRepository).save(product);
        verify(notificationService).sendExpirationNotification(product.getName(), product.getExpiryDate());
    }
}
