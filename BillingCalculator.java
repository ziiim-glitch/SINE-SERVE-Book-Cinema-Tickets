public class BillingCalculator {
    private double basePrice;
    private int quantity;
    private boolean hasDiscount;
    private String paymentMethod;
    private static final double ONLINE_FEE = 10.00;
    private static final double DISCOUNT_RATE = 0.20; 
    public BillingCalculator(double basePrice, int quantity, boolean hasDiscount, String paymentMethod) {
        this.basePrice = basePrice;
        this.quantity = quantity;
        this.hasDiscount = hasDiscount;
        this.paymentMethod = paymentMethod;
    }

    public double getSubtotal() {
        return basePrice * quantity;
    }

    public double getDiscountAmount() {
        if (hasDiscount) {
            return getSubtotal() * DISCOUNT_RATE;
        }
        return 0.0;
    }

    public double getOnlineFee() {
        return ONLINE_FEE;
    }

    public double getFinalTotal() {
        return (getSubtotal() - getDiscountAmount()) + ONLINE_FEE;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }
}
