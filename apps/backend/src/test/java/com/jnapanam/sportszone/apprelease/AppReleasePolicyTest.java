package com.jnapanam.sportszone.apprelease;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.jnapanam.sportszone.common.exception.BadRequestException;
import com.jnapanam.sportszone.common.exception.NotFoundException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class AppReleasePolicyTest {
    private final JdbcTemplate jdbc=mock(JdbcTemplate.class);
    private final AppReleaseService service=new AppReleaseService(jdbc);
    private AppReleaseService.PublicPolicy policy(String mode,int min,int latest,boolean available){
        return new AppReleaseService.PublicPolicy(available,latest,"1.2.0",min,mode,"Release notes",
                AppReleaseService.DOWNLOAD_PAGE);
    }
    @Test void mandatoryOnlyBlocksBuildsBelowMinimumWithPublishedUrl(){
        assertTrue(AppReleaseService.mustUpgrade(policy("MANDATORY",3,5,true),1));
        assertTrue(AppReleaseService.mustUpgrade(policy("MANDATORY",3,5,true),2));
        assertFalse(AppReleaseService.mustUpgrade(policy("MANDATORY",3,5,true),3));
        assertFalse(AppReleaseService.mustUpgrade(policy("MANDATORY",3,5,true),5));
        assertFalse(AppReleaseService.mustUpgrade(policy("OPTIONAL",3,5,true),1));
        assertFalse(AppReleaseService.mustUpgrade(policy("OFF",3,5,true),1));
        assertFalse(AppReleaseService.mustUpgrade(policy("MANDATORY",3,5,false),1));
        assertFalse(AppReleaseService.mustUpgrade(policy("MANDATORY",3,5,true),null));
    }
    @Test void linksAreHttpsAndTrusted(){
        assertEquals("https://drive.google.com/file/d/abcd/view",
                AppReleaseService.validateUrl("https://drive.google.com/file/d/abcd/view"));
        assertEquals("https://jnapanam.in/releases/latest.apk",
                AppReleaseService.validateUrl("https://jnapanam.in/releases/latest.apk"));
        assertNull(AppReleaseService.validateUrl("   "));
        for(String url:new String[]{"http://drive.google.com/a.apk","https://evil.example/a.apk",
            "https://drive.google.com.evil.example/a.apk","https://user:pass@github.com/a.apk",
            "https://127.0.0.1/a.apk","https://github.com:444/a.apk",
            "https://github.com/a.apk#fragment"})
            assertThrows(BadRequestException.class,()->AppReleaseService.validateUrl(url),url);
    }
    @Test void invalidBuildOrderAndUnsafePublicationAreRejectedBeforeDatabaseMutation(){
        var actor=UUID.randomUUID();
        assertThrows(BadRequestException.class,()->service.update(actor,
            new AppReleaseService.Change(2,"1.1.0",3,"MANDATORY","https://github.com/a.apk","x")));
        assertThrows(BadRequestException.class,()->service.update(actor,
            new AppReleaseService.Change(2,"1.1.0",1,"MANDATORY",null,"x")));
        assertThrows(BadRequestException.class,()->service.update(actor,
            new AppReleaseService.Change(2,"1.1.0",1,"BAD","https://github.com/a.apk","x")));
        assertThrows(BadRequestException.class,()->service.update(actor,
            new AppReleaseService.Change(2,"version",1,"OPTIONAL","https://github.com/a.apk","x")));
        verifyNoInteractions(jdbc);
    }
}
