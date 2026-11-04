package com.jnapanam.sportszone.apprelease;

import com.jnapanam.sportszone.common.exception.BadRequestException;
import com.jnapanam.sportszone.common.exception.NotFoundException;
import java.net.IDN;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppReleaseService {
    public static final String DOWNLOAD_PAGE = "https://jnapanam.in/app";
    private final JdbcTemplate jdbc;
    // Avoid one PostgreSQL SELECT per mobile API request; refresh automatically in 30s.
    private volatile Cached publicCache;
    private record Cached(PublicPolicy value,long validUntilMillis) {}
    public AppReleaseService(JdbcTemplate jdbc) { this.jdbc=jdbc; }

    public record Policy(int latestVersionCode, String latestVersionName,
        int minimumSupportedVersionCode, String updateMode, String apkUrl,
        String releaseNotes, String downloadPageUrl, java.time.Instant updatedAt) {}
    public record PublicPolicy(boolean available, int latestVersionCode, String latestVersionName,
        int minimumSupportedVersionCode, String updateMode, String releaseNotes,
        String downloadPageUrl) {}
    public record Change(int latestVersionCode, String latestVersionName,
        int minimumSupportedVersionCode, String updateMode, String apkUrl, String releaseNotes) {}

    @Transactional(readOnly=true)
    public Policy admin() {
        return jdbc.queryForObject("SELECT latest_version_code,latest_version_name,minimum_supported_version_code,update_mode,apk_url,release_notes,updated_at FROM mobile_app_release_policy WHERE id=1",
            (rs,n)->new Policy(rs.getInt(1),rs.getString(2),rs.getInt(3),rs.getString(4),
                rs.getString(5),rs.getString(6),DOWNLOAD_PAGE,rs.getTimestamp(7).toInstant()));
    }

    @Transactional(readOnly=true)
    public PublicPolicy publicPolicy() {
        Cached cached=publicCache;
        long now=System.currentTimeMillis();
        if(cached!=null&&cached.validUntilMillis()>now)return cached.value();
        Policy p=admin();
        PublicPolicy value=new PublicPolicy(p.apkUrl()!=null,p.latestVersionCode(),p.latestVersionName(),
            p.minimumSupportedVersionCode(),p.updateMode(),p.releaseNotes(),DOWNLOAD_PAGE);
        publicCache=new Cached(value,now+30_000);
        return value;
    }

    @Transactional(readOnly=true)
    public String publicApkUrl() {
        String url=admin().apkUrl();
        if(url==null)throw new NotFoundException("Android app download is not yet published");
        return url;
    }

    @Transactional
    public Policy update(UUID actor, Change c) {
        if(c==null || c.latestVersionCode()<1 || c.minimumSupportedVersionCode()<1
                || c.minimumSupportedVersionCode()>c.latestVersionCode())
            throw new BadRequestException("Set valid Android build numbers: minimum supported must not exceed latest");
        String version=c.latestVersionName()==null?"":c.latestVersionName().trim();
        if(!version.matches("[0-9]+(\\.[0-9]+){1,3}(-[A-Za-z0-9.-]+)?") || version.length()>40)
            throw new BadRequestException("Use a valid version name such as 1.1.0");
        String mode=c.updateMode()==null?"":c.updateMode().trim().toUpperCase(Locale.ROOT);
        if(!List.of("OFF","OPTIONAL","MANDATORY").contains(mode))
            throw new BadRequestException("Update mode must be OFF, OPTIONAL or MANDATORY");
        String url=validateUrl(c.apkUrl());
        if(!"OFF".equals(mode)&&url==null)
            throw new BadRequestException("Publish a valid HTTPS APK link before enabling updates");
        String notes=c.releaseNotes()==null?null:c.releaseNotes().trim();
        if(notes!=null&&notes.length()>1500)throw new BadRequestException("Release notes must be at most 1500 characters");
        jdbc.update("UPDATE mobile_app_release_policy SET latest_version_code=?,latest_version_name=?,minimum_supported_version_code=?,update_mode=?,apk_url=?,release_notes=?,updated_by=?,updated_at=now() WHERE id=1",
            c.latestVersionCode(),version,c.minimumSupportedVersionCode(),mode,url,notes,actor);
        publicCache=null;
        return admin();
    }

    static String validateUrl(String value) {
        if(value==null||value.isBlank())return null;
        if(value.length()>1600)throw new BadRequestException("APK download URL is too long");
        try {
            URI uri=URI.create(value.trim());
            String host=uri.getHost()==null?"":IDN.toASCII(uri.getHost()).toLowerCase(Locale.ROOT);
            boolean trusted=host.equals("drive.google.com")||host.equals("drive.usercontent.google.com")
                ||host.equals("github.com")||host.equals("objects.githubusercontent.com")
                ||host.equals("jnapanam.in")||host.endsWith(".jnapanam.in")
                ||host.equals("expo.dev")||host.endsWith(".expo.dev")
                ||host.matches("pub-[a-z0-9]+\\.r2\\.dev");
            if(!"https".equalsIgnoreCase(uri.getScheme())||!trusted || uri.getRawUserInfo()!=null
                ||uri.getFragment()!=null || uri.getPort()!=-1)
                throw new IllegalArgumentException();
            return uri.toASCIIString();
        }catch(Exception e){throw new BadRequestException("Use a public HTTPS link on Drive, GitHub, Expo, Jnapanam or a trusted Cloudflare R2 public domain");}
    }

    public static boolean mustUpgrade(PublicPolicy p, Integer installed) {
        return installed!=null && installed>0 && p.available()
            && "MANDATORY".equals(p.updateMode())
            && installed<p.minimumSupportedVersionCode();
    }
}
