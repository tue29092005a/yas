package com.yas.order.specification;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yas.order.model.Order;
import com.yas.order.model.OrderItem;
import com.yas.order.model.enumeration.OrderStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

class OrderSpecificationAdditionalTest {

    private CriteriaBuilder criteriaBuilder;
    private Root<Order> root;
    private CriteriaQuery<?> query;
    private Root<OrderItem> orderItemRoot;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        criteriaBuilder = mock(CriteriaBuilder.class);
        root = mock(Root.class);
        query = mock(CriteriaQuery.class);
        orderItemRoot = mock(Root.class);
    }

    @Test
    void testFindMyOrders() {
        Specification<Order> spec = OrderSpecification.findMyOrders("user1", "product1", OrderStatus.COMPLETED);
        
        when(root.get(anyString())).thenReturn(mock(Path.class));
        when(criteriaBuilder.equal(any(), any())).thenReturn(mock(Predicate.class));
        
        Subquery<Long> subqueryMock = mock(Subquery.class);
        when(query.subquery(Long.class)).thenReturn(subqueryMock);
        when(subqueryMock.from(OrderItem.class)).thenReturn(orderItemRoot);
        when(criteriaBuilder.like(any(), anyString())).thenReturn(mock(Predicate.class));
        when(subqueryMock.select(any())).thenReturn(subqueryMock);
        when(subqueryMock.where(any(Predicate.class))).thenReturn(subqueryMock);
        
        CriteriaBuilder.In inMock = mock(CriteriaBuilder.In.class);
        when(criteriaBuilder.in(any())).thenReturn(inMock);
        when(inMock.value(any())).thenReturn(mock(CriteriaBuilder.In.class));
        
        when(criteriaBuilder.and(any(), any(), any())).thenReturn(mock(Predicate.class));

        Predicate predicate = spec.toPredicate(root, query, criteriaBuilder);
        assertNotNull(predicate);
    }

    @Test
    void testFindOrderByWithMulCriteria() {
        Specification<Order> spec = OrderSpecification.findOrderByWithMulCriteria(
                List.of(OrderStatus.COMPLETED), "123", "Vietnam", "test@test.com", "product1", 
                ZonedDateTime.now().minusDays(1), ZonedDateTime.now());
        
        when(query.getResultType()).thenReturn((Class) Object.class);
        when(root.get(anyString())).thenReturn(mock(Path.class));
        when(criteriaBuilder.like(any(), anyString())).thenReturn(mock(Predicate.class));
        
        Path mockPath = mock(Path.class);
        when(root.get("billingAddressId")).thenReturn(mockPath);
        when(mockPath.get(anyString())).thenReturn(mock(Path.class));
        
        when(criteriaBuilder.between(any(), any(ZonedDateTime.class), any(ZonedDateTime.class))).thenReturn(mock(Predicate.class));
        
        Subquery<Long> subqueryMock = mock(Subquery.class);
        when(query.subquery(Long.class)).thenReturn(subqueryMock);
        when(subqueryMock.from(OrderItem.class)).thenReturn(orderItemRoot);
        when(subqueryMock.select(any())).thenReturn(subqueryMock);
        when(criteriaBuilder.exists(subqueryMock)).thenReturn(mock(Predicate.class));
        when(orderItemRoot.get(anyString())).thenReturn(mock(Path.class));
        when(criteriaBuilder.equal(any(), any())).thenReturn(mock(Predicate.class));
        when(criteriaBuilder.and(any(Predicate.class), any(Predicate.class))).thenReturn(mock(Predicate.class));
        when(criteriaBuilder.and(any(), any(), any(), any(), any(), any())).thenReturn(mock(Predicate.class));

        Predicate predicate = spec.toPredicate(root, query, criteriaBuilder);
        assertNotNull(predicate);
    }

    @Test
    void testEmptyAndNullValuesReturnsConjunction() {
        when(criteriaBuilder.conjunction()).thenReturn(mock(Predicate.class));
        
        Specification<Order> spec1 = OrderSpecification.hasOrderStatus(null);
        assertNotNull(spec1.toPredicate(root, query, criteriaBuilder));
        
        Specification<Order> spec2 = OrderSpecification.withEmail(null);
        assertNotNull(spec2.toPredicate(root, query, criteriaBuilder));
        
        Specification<Order> spec3 = OrderSpecification.withOrderStatus(null);
        assertNotNull(spec3.toPredicate(root, query, criteriaBuilder));
        
        Specification<Order> spec4 = OrderSpecification.withBillingPhoneNumber("");
        assertNotNull(spec4.toPredicate(root, query, criteriaBuilder));
        
        Specification<Order> spec5 = OrderSpecification.withCountryName(null);
        assertNotNull(spec5.toPredicate(root, query, criteriaBuilder));
        
        Specification<Order> spec6 = OrderSpecification.withDateRange(null, null);
        assertNotNull(spec6.toPredicate(root, query, criteriaBuilder));
    }

    @Test
    void testNullQueryReturnsConjunction() {
        when(criteriaBuilder.conjunction()).thenReturn(mock(Predicate.class));
        
        Specification<Order> spec1 = OrderSpecification.hasProductInOrderItems(List.of(1L));
        assertNotNull(spec1.toPredicate(root, null, criteriaBuilder));
        
        Specification<Order> spec2 = OrderSpecification.hasProductNameInOrderItems("test");
        assertNotNull(spec2.toPredicate(root, null, criteriaBuilder));
        
        Specification<Order> spec3 = OrderSpecification.withProductName("test");
        assertNotNull(spec3.toPredicate(root, null, criteriaBuilder));
    }

    @Test
    void testExistsByCreatedByAndInProductIdAndOrderStatusCompleted() {
        Specification<Order> spec = OrderSpecification.existsByCreatedByAndInProductIdAndOrderStatusCompleted("user1", List.of(1L));
        
        when(root.get(anyString())).thenReturn(mock(Path.class));
        when(criteriaBuilder.equal(any(), any())).thenReturn(mock(Predicate.class));
        
        Subquery<OrderItem> subqueryMock = mock(Subquery.class);
        when(query.subquery(OrderItem.class)).thenReturn(subqueryMock);
        when(subqueryMock.from(OrderItem.class)).thenReturn(orderItemRoot);
        when(subqueryMock.select(any())).thenReturn(subqueryMock);
        when(criteriaBuilder.exists(subqueryMock)).thenReturn(mock(Predicate.class));
        
        Path mockPath = mock(Path.class);
        when(orderItemRoot.get(anyString())).thenReturn(mockPath);
        CriteriaBuilder.In inMock = mock(CriteriaBuilder.In.class);
        when(mockPath.in(any(java.util.Collection.class))).thenReturn(inMock);
        when(criteriaBuilder.and(any(Predicate.class), any(Predicate.class))).thenReturn(mock(Predicate.class));
        
        Predicate predicate = spec.toPredicate(root, query, criteriaBuilder);
        assertNotNull(predicate);
    }
}
