package co.istad.rentiq_api.features.kyc.controller;


import co.istad.rentiq_api.features.auth.dto.response.MessageResponse;
import co.istad.rentiq_api.features.kyc.dto.request.*;
import co.istad.rentiq_api.features.kyc.dto.response.KycResponse;
import co.istad.rentiq_api.features.kyc.service.KycService;
import co.istad.rentiq_api.security.AuthUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/kyc")
@RequiredArgsConstructor
public class KycController {

    private final KycService kycService;

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public KycResponse submitKyc(@Valid @RequestPart("data") SubmitKycRequest request,
                                 @RequestPart("frontImage") MultipartFile frontImage,
                                 @RequestPart("backImage") MultipartFile backImage) {
        return kycService.submitKyc(AuthUtils.extractUserId(), request, frontImage, backImage);
    }

    @GetMapping("/me")
    public KycResponse getMyKyc() {
        return kycService.getMyKyc(AuthUtils.extractUserId());
    }

    @PatchMapping(value = "/me", consumes = "multipart/form-data")
    public KycResponse resubmitKyc(@Valid @RequestPart("data") SubmitKycRequest request,
                                   @RequestPart(value = "frontImage", required = false) MultipartFile frontImage,
                                   @RequestPart(value = "backImage", required = false) MultipartFile backImage) {
        return kycService.resubmitKyc(AuthUtils.extractUserId(), request, frontImage, backImage);
    }

//    @PostMapping("/me/verify-phone")
//    public MessageResponse startPhoneVerification(@Valid @RequestBody StartPhoneVerificationRequest request) {
//        kycService.startPhoneVerification(AuthUtils.extractUserId(), request);
//        return MessageResponse.builder().message("Verification code sent").build();
//    }
//
//    @PostMapping("/me/verify-phone/confirm")
//    public KycResponse confirmPhoneOtp(@Valid @RequestBody ConfirmOtpRequest request) {
//        return kycService.confirmPhoneOtp(AuthUtils.extractUserId(), request);
//    }

    @PostMapping("/me/verify-email")
    public MessageResponse startEmailVerification() {
        kycService.startEmailVerification(AuthUtils.extractUserId());
        return MessageResponse.builder().message("Verification email sent").build();
    }

    @PostMapping("/me/verify-email/confirm")
    public KycResponse confirmEmailVerification() {
        return kycService.confirmEmailVerification(AuthUtils.extractUserId());
    }
}