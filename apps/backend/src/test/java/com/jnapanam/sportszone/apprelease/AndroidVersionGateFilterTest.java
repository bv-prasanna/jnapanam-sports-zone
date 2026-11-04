package com.jnapanam.sportszone.apprelease;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import jakarta.servlet.FilterChain;

class AndroidVersionGateFilterTest {
    private final AppReleaseService policy=mock(AppReleaseService.class);
    private final AndroidVersionGateFilter filter=new AndroidVersionGateFilter(policy);

    @Test void mandatoryOldAndroidGets426ButCompatibleClientProceeds() throws Exception {
        when(policy.publicPolicy()).thenReturn(new AppReleaseService.PublicPolicy(true,4,"2.0.0",3,
            "MANDATORY",null,AppReleaseService.DOWNLOAD_PAGE));
        var old=new MockHttpServletRequest("POST","/api/v1/bookings/holds");
        old.addHeader("X-Jnapanam-Platform","android");
        old.addHeader("X-Jnapanam-Version-Code","2");
        var oldResponse=new MockHttpServletResponse();
        FilterChain chain=mock(FilterChain.class);
        filter.doFilterInternal(old,oldResponse,chain);
        assertEquals(426,oldResponse.getStatus());
        assertTrue(oldResponse.getContentAsString().contains("APP_UPDATE_REQUIRED"));
        assertTrue(oldResponse.getContentAsString().contains("https://jnapanam.in/app"));
        verifyNoInteractions(chain);

        var current=new MockHttpServletRequest("POST","/api/v1/bookings/holds");
        current.addHeader("X-Jnapanam-Platform","android");
        current.addHeader("X-Jnapanam-Version-Code","3");
        filter.doFilterInternal(current,new MockHttpServletResponse(),chain);
        verify(chain).doFilter(any(),any());
    }

    @Test void webAndUnknownOlderClientsAreNotMisclassified() throws Exception {
        var web=new MockHttpServletRequest("GET","/api/v1/profile/me");
        var chain=mock(FilterChain.class);
        filter.doFilterInternal(web,new MockHttpServletResponse(),chain);
        var oldAndroid=new MockHttpServletRequest("GET","/api/v1/profile/me");
        oldAndroid.addHeader("X-Jnapanam-Platform","android");
        filter.doFilterInternal(oldAndroid,new MockHttpServletResponse(),chain);
        verify(chain,times(2)).doFilter(any(),any());
        verifyNoInteractions(policy);
    }

    @Test void publicConfigAndPreflightBypassVersionCheck(){
        assertTrue(filter.shouldNotFilter(new MockHttpServletRequest("GET",
            "/api/v1/public/app-release/android")));
        assertTrue(filter.shouldNotFilter(new MockHttpServletRequest("GET",
            "/api/v1/public/app-download/android")));
        assertTrue(filter.shouldNotFilter(new MockHttpServletRequest("OPTIONS",
            "/api/v1/bookings/me")));
        assertFalse(filter.shouldNotFilter(new MockHttpServletRequest("GET",
            "/api/v1/bookings/me")));
    }

    @Test void versionHeaderMustBeStrictlyPositiveInteger(){
        assertNull(AndroidVersionGateFilter.parsePositiveInt("0"));
        assertNull(AndroidVersionGateFilter.parsePositiveInt("unknown"));
        assertNull(AndroidVersionGateFilter.parsePositiveInt("1.1"));
        assertNull(AndroidVersionGateFilter.parsePositiveInt(null));
        assertEquals(123,AndroidVersionGateFilter.parsePositiveInt("123"));
    }
}
