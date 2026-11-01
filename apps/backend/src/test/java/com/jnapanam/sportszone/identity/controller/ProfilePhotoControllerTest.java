package com.jnapanam.sportszone.identity.controller;

import com.jnapanam.sportszone.common.exception.BadRequestException;
import com.jnapanam.sportszone.common.exception.NotFoundException;
import com.jnapanam.sportszone.identity.model.User;
import com.jnapanam.sportszone.identity.model.UserStatus;
import com.jnapanam.sportszone.identity.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProfilePhotoControllerTest {
    final UserRepository users=mock(UserRepository.class);
    final JdbcTemplate jdbc=mock(JdbcTemplate.class);
    final ProfilePhotoController controller=new ProfilePhotoController(users,jdbc);

    static byte[] png(){
        return new byte[]{(byte)137,80,78,71,13,10,26,10,0,0,0,0,73,72,68,82};
    }
    static Jwt jwt(UUID userId){
        return Jwt.withTokenValue("profile-test").header("alg","HS256").subject(userId.toString()).build();
    }
    @Test void photoUploadUsesDatabaseAndUpdatesUserPhotoUrl()throws Exception {
        UUID id=UUID.randomUUID();
        User user=mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(user.getRoles()).thenReturn(Set.of());
        when(users.findById(id)).thenReturn(Optional.of(user));
        when(users.save(user)).thenReturn(user);
        MockHttpServletRequest request=new MockHttpServletRequest("POST","/api/v1/profile/photo");
        request.setServerName("api.example.test");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try{
            var file=new MockMultipartFile("file","avatar.png","image/png",png());
            var response=controller.upload(jwt(id),file);
            verify(jdbc).update(contains("insert into user_profile_photos"),eq(id),eq("image/png"),org.mockito.AdditionalMatchers.aryEq(png()));
            verify(user).profile(eq(null),contains("/api/v1/public/profile-photos/"+id+".png"));
            assertTrue(response.profilePhotoUrl()==null); // mock user has no persisted field value
        }finally{RequestContextHolder.resetRequestAttributes();}
    }
    @Test void rejectImagesDisguisedWithWrongContentType() {
        var bad=new MockMultipartFile("file","not-really.png","image/png","not a png".getBytes());
        UUID id=UUID.randomUUID();
        when(users.findById(id)).thenReturn(Optional.of(mock(User.class)));
        assertThrows(BadRequestException.class,()->controller.upload(jwt(id),bad));
        verifyNoInteractions(jdbc);
    }
    @Test void rejectsEmptyAndUnsupportedFiles() {
        var empty=new MockMultipartFile("file","empty.png","image/png",new byte[]{});
        var svg=new MockMultipartFile("file","x.svg","image/svg+xml",new byte[]{1,2,3});
        UUID id=UUID.randomUUID();
        when(users.findById(id)).thenReturn(Optional.of(mock(User.class)));
        assertThrows(BadRequestException.class,()->controller.upload(jwt(id),empty));
        assertThrows(BadRequestException.class,()->controller.upload(jwt(id),svg));
        verifyNoInteractions(jdbc);
    }
    @Test void immutableFileExtensionsAndInvalidNamesCannotBeRead() {
        assertThrows(NotFoundException.class,()->controller.photo("../../etc/passwd"));
        assertThrows(NotFoundException.class,()->controller.photo(UUID.randomUUID()+".svg"));
        verifyNoInteractions(jdbc);
    }
    @Test void supportsJpgPngAndWebpMagicBytes() {
        assertTrue(ProfilePhotoController.matchesFileSignature(new byte[]{(byte)255,(byte)216,(byte)255,1},"image/jpeg"));
        assertTrue(ProfilePhotoController.matchesFileSignature(png(),"image/png"));
        assertTrue(ProfilePhotoController.matchesFileSignature(new byte[]{82,73,70,70,0,0,0,0,87,69,66,80},"image/webp"));
        assertFalse(ProfilePhotoController.matchesFileSignature(new byte[]{1,2,3},"image/jpeg"));
    }
}
