package com.jnapanam.sportszone.admin.controller;

import com.jnapanam.sportszone.admin.service.OperatorAssignmentService;
import com.jnapanam.sportszone.booking.repository.BookingRepository;
import com.jnapanam.sportszone.common.exception.NotFoundException;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * A bounded low-resolution preview of the latest uploaded customer document,
 * restricted to operators assigned to that booking. No document is embedded
 * in general booking lists or QR payloads. Clients must not persist previews.
 */
@RestController
@RequestMapping("/api/v1/operations/bookings/{bookingNumber}/identity")
@PreAuthorize("hasAnyRole('OPERATIONS','ADMIN','SUPER_ADMIN')")
public class OperatorDocumentPreviewController {
    private final OperatorAssignmentService assignments;
    private final BookingRepository bookings;
    private final JdbcTemplate jdbc;

    public OperatorDocumentPreviewController(OperatorAssignmentService assignments,
            BookingRepository bookings, JdbcTemplate jdbc) {
        this.assignments=assignments;this.bookings=bookings;this.jdbc=jdbc;
    }

    public record Preview(String mediaType,String dataBase64) {}

    @GetMapping("/document-preview")
    public ResponseEntity<Preview> preview(Authentication auth,@PathVariable String bookingNumber) throws IOException {
        assignments.assertBookingAllowed(UUID.fromString(auth.getName()),bookingNumber);
        UUID userId=bookings.findByBookingNumberIgnoreCase(bookingNumber)
                .orElseThrow(()->new NotFoundException("Booking not found")).getUser().getId();
        List<Map<String,Object>> rows=jdbc.queryForList(
                "select content_type,document_bytes from booking_address_proofs where user_id=? order by uploaded_at desc limit 1",userId);
        if(rows.isEmpty())throw new NotFoundException("Customer document not uploaded");
        String mime=(String)rows.get(0).get("content_type");
        byte[] original=(byte[])rows.get(0).get("document_bytes");
        BufferedImage source;
        if("application/pdf".equals(mime)) {
            try(var pdf=Loader.loadPDF(original)) {
                if(pdf.getNumberOfPages()==0)throw new NotFoundException("Empty customer document");
                source=new PDFRenderer(pdf).renderImageWithDPI(0,72);
            }
        } else if("image/jpeg".equals(mime)||"image/png".equals(mime)) {
            source=ImageIO.read(new java.io.ByteArrayInputStream(original));
        } else throw new NotFoundException("Document preview format unavailable");
        if(source==null)throw new NotFoundException("Document preview unavailable");

        // Limit pixels and response size to protect mobile data and server memory.
        int width=Math.min(640,source.getWidth());
        int height=Math.max(1,(int)Math.round((double)width*source.getHeight()/source.getWidth()));
        if(height>850){width=Math.max(1,(int)Math.round((double)width*850/height));height=850;}
        BufferedImage scaled=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics=scaled.createGraphics();
        try{
            graphics.setColor(Color.WHITE);graphics.fillRect(0,0,width,height);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(source,0,0,width,height,null);
        }finally{graphics.dispose();}
        ByteArrayOutputStream output=new ByteArrayOutputStream();
        if(!ImageIO.write(scaled,"jpeg",output))throw new NotFoundException("Document preview unavailable");
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options","nosniff")
                .body(new Preview("image/jpeg",Base64.getEncoder().encodeToString(output.toByteArray())));
    }
}
