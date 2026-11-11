package com.jnapanam.sportszone.admin.model;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
class AuditLogLocationTest {
 private AuditLog event(){return new AuditLog(null,"POST","/api/v1/bookings",201,"127.0.0.1","POST_BOOKINGS","BOOKINGS",null,null,"correlation","test",1);}
 @Test void validCoordinatesAreRecorded(){var log=event();log.recordLocation("12.9716000","77.5946000","CAPTURED");assertEquals("CAPTURED",log.getLocationStatus());assertEquals(0,log.getLatitude().compareTo(new java.math.BigDecimal("12.9716")));}
 @Test void invalidCoordinatesCannotBeStored(){var log=event();log.recordLocation("92","77","CAPTURED");assertEquals("INVALID",log.getLocationStatus());assertNull(log.getLatitude());}
 @Test void deniedPermissionIsExplicit(){var log=event();log.recordLocation(null,null,"DENIED");assertEquals("DENIED",log.getLocationStatus());}
 @Test void missingCoordinatesIsUnavailable(){var log=event();log.recordLocation(null,null,null);assertEquals("UNAVAILABLE",log.getLocationStatus());}
}
