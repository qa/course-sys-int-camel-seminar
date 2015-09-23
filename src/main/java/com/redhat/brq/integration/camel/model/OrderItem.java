package com.redhat.brq.integration.camel.model;

// TASK-3
// Mark the class as csv record, use ';' as separator and crlf set to 'UNIX'
public class OrderItem {

    // TASK-3
    // Mark this attribute as CSV field, do not forget to set correct position (pos)
    private long articleId;

    // TASK-3
    // Mark this attribute as CSV field, do not forget to set correct position (pos)
    private int count;

    private double unitPrice;

    public OrderItem() {
    }

    public OrderItem(long articleId, int count, double unitPrice) {
        this.articleId = articleId;
        this.count = count;
        this.unitPrice = unitPrice;
    }

    public long getArticleId() {
        return articleId;
    }

    public void setArticleId(long articleId) {
        this.articleId = articleId;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public double getPrice() {
        return count * getUnitPrice();
    }

    @Override
    public String toString() {
        return "OrderItem [articleId=" + articleId + ", count=" + count + ", unitPrice=" + unitPrice + "]";
    }
}
