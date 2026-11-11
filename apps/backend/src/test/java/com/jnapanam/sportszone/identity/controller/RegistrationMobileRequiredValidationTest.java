package com.jnapanam.sportszone.identity.controller;

import static org.junit.jupiter.api.Assertions.*;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import com.jnapanam.sportszone.identity.controller.SmsOtpController.EmailRegistrationVerifyRequest;

/** Email-only registration must collect a phone before account creation, without sending SMS. */
class RegistrationMobileRequiredValidationTest {
    @Test void phoneIsRequiredOnEmailOtpRegistrationConfirmation() {
        try(var factory=Validation.buildDefaultValidatorFactory()){
            var validator=factory.getValidator();
            for(String mobile:new String[]{null,"","   ","123"}){
                var request=new EmailRegistrationVerifyRequest("Example User","valid@example.com",
                    mobile,"Test@12345","123456",null,null);
                assertTrue(validator.validate(request).stream().anyMatch(v ->
                    "mobile".equals(v.getPropertyPath().toString())),
                    "Expected mobile constraint for: "+mobile);
            }
            var valid=new EmailRegistrationVerifyRequest("Example User","valid@example.com",
                "9449284747","Test@12345","123456",null,null);
            assertTrue(validator.validate(valid).isEmpty());
        }
    }
}
