//package co.istad.rentiq_api.features.offers.request;
//
//
//import jakarta.validation.constraints.DecimalMin;
//import jakarta.validation.constraints.NotNull;
//import jakarta.validation.constraints.Null;
//import lombok.Data;
//
//import java.math.BigDecimal;
//import java.util.UUID;
//
//@Data
//public class CreateOfferRequest {
//
//    @NotNull
//    private UUID itemId;
//    @NotNull
//    @DecimalMin("0.01")
//    private BigDecimal offeredPrice;
//
//    private String currency = "USD";
//
//    private String message;
//
//}
