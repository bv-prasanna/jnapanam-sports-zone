package com.jnapanam.sportszone.apprelease;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Defense-in-depth for version-aware Android clients. Older APKs which do not
 * send the version headers cannot be reliably identified without breaking web
 * clients. Update UI on supported APKs is the primary enforcement mechanism.
 */
@Component
public class AndroidVersionGateFilter extends OncePerRequestFilter {
    private final AppReleaseService service;
    public AndroidVersionGateFilter(AppReleaseService service){this.service=service;}

    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        String path=request.getRequestURI();
        return !path.startsWith("/api/v1/") || path.startsWith("/api/v1/public/")
            || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,
            FilterChain chain) throws ServletException,IOException {
        if(!"android".equalsIgnoreCase(request.getHeader("X-Jnapanam-Platform"))) {
            chain.doFilter(request,response);return;
        }
        Integer installed=parsePositiveInt(request.getHeader("X-Jnapanam-Version-Code"));
        // Legacy APKs with no native version header cannot be gated safely;
        // do not query the policy or misclassify them as unsupported builds.
        if(installed==null) {
            chain.doFilter(request,response);return;
        }
        if(AppReleaseService.mustUpgrade(service.publicPolicy(),installed)) {
            response.setStatus(426);
            response.setHeader("Cache-Control","no-store");
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":\"APP_UPDATE_REQUIRED\",\"message\":\"A new Jnapanam Android app version is required.\",\"downloadPageUrl\":\"https://jnapanam.in/app\"}");
            return;
        }
        chain.doFilter(request,response);
    }
    static Integer parsePositiveInt(String value) {
        if(value==null||!value.matches("[1-9][0-9]{0,8}"))return null;
        try{return Integer.parseInt(value);}catch(NumberFormatException e){return null;}
    }
}
