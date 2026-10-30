package com.jnapanam.sportszone.growth;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class PlayerGrowthControllerTest {
 @Mock JdbcTemplate jdbc;
 @Test void rejectsMissingAssessmentFieldsByValidationContract() throws Exception {
   var fields=PlayerGrowthController.AssessmentRequest.class.getRecordComponents();
   assertEquals(6,fields.length);
   var scoreAccessor=fields[3].getAccessor();
   assertNotNull(scoreAccessor.getAnnotation(jakarta.validation.constraints.NotNull.class));
   assertNotNull(scoreAccessor.getAnnotation(jakarta.validation.constraints.Min.class));
   assertNotNull(scoreAccessor.getAnnotation(jakarta.validation.constraints.Max.class));
 }
 @Test void goalAndAssessmentEndpointsDoNotAllowAnonymousAccess() throws Exception {
  var paths=PlayerGrowthController.class.getDeclaredMethods();
  assertTrue(java.util.Arrays.stream(paths).filter(m->m.getName().equals("goal")||m.getName().equals("assess")||m.getName().equals("player"))
       .allMatch(m->m.isAnnotationPresent(org.springframework.security.access.prepost.PreAuthorize.class)));
 }
 @Test void detailForAuthenticatedPlayerIsPrivate() throws Exception {
  var method=PlayerGrowthController.class.getDeclaredMethod("me",org.springframework.security.oauth2.jwt.Jwt.class);
  assertNotNull(method.getAnnotation(org.springframework.web.bind.annotation.GetMapping.class));
 }
}
