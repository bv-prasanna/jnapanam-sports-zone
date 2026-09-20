package com.jnapanam.sportszone.booking.service;

import com.jnapanam.sportszone.booking.dto.*;
import com.jnapanam.sportszone.booking.model.*;
import com.jnapanam.sportszone.booking.repository.*;
import com.jnapanam.sportszone.common.exception.*;
import com.jnapanam.sportszone.identity.model.User;
import com.jnapanam.sportszone.identity.repository.UserRepository;
import com.jnapanam.sportszone.notification.service.NotificationService;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {
 private final BookingSlotRepository slots; private final BookingHoldRepository holds; private final BookingRepository bookings; private final UserRepository users; private final NotificationService notificationService; private final long holdMinutes;private final long paymentMinutes;private final long noShowGraceMinutes;
 public BookingService(BookingSlotRepository slots,BookingHoldRepository holds,BookingRepository bookings,UserRepository users,NotificationService notificationService,@Value("${app.booking.hold-minutes:10}") long holdMinutes,@Value("${app.payment.pending-minutes:15}")long paymentMinutes,@Value("${app.booking.no-show-grace-minutes:15}")long noShowGraceMinutes){this.slots=slots;this.holds=holds;this.bookings=bookings;this.users=users;this.notificationService=notificationService;this.holdMinutes=holdMinutes;this.paymentMinutes=paymentMinutes;this.noShowGraceMinutes=noShowGraceMinutes;}

 @Transactional
 public HoldResponse hold(UUID userId,HoldRequest request){
  cleanupExpired(); User user=requireUser(userId); BookingSlot slot=slots.findLocked(request.slotId()).orElseThrow(()->new NotFoundException("Slot not found"));
  if(slot.getDate().isBefore(LocalDate.now())||!LocalDateTime.of(slot.getDate(),slot.getStartTime()).isAfter(LocalDateTime.now())) throw new ConflictException("This slot has already started or passed");
  slot.hold(request.quantity()); BookingHold hold=holds.save(new BookingHold(user,slot,request.quantity(),Instant.now().plusSeconds(holdMinutes*60)));
  return HoldResponse.from(hold);
 }
 @Transactional
 public BookingResponse confirm(UUID userId,ConfirmRequest request){
  BookingHold hold=holds.findLocked(request.holdId()).orElseThrow(()->new NotFoundException("Hold not found")); assertOwner(hold.getUser(),userId);
  if(hold.getStatus()!=HoldStatus.ACTIVE) throw new ConflictException("Hold is no longer active");
  BookingSlot slot=slots.findLocked(hold.getSlot().getId()).orElseThrow(()->new NotFoundException("Slot not found"));
  if(hold.expired(Instant.now())){slot.releaseHold(hold.getQuantity());hold.change(HoldStatus.EXPIRED);throw new ConflictException("Hold has expired");}
  slot.confirm(hold.getQuantity());hold.change(HoldStatus.CONVERTED);boolean priced=slot.getFacility().getBookingPrice().signum()>0;PaymentOption option=request.paymentOption()==null?PaymentOption.PAY_NOW:request.paymentOption();if(priced&&hold.getUser().isRequiresUpfrontPayment()&&option==PaymentOption.PAY_AT_VENUE)throw new ConflictException("Upfront online payment is required for this customer");boolean paymentRequired=priced&&option==PaymentOption.PAY_NOW;Booking saved=bookings.save(new Booking(hold.getUser(),slot,hold.getQuantity(),priced,option,paymentRequired,Instant.now().plusSeconds(paymentMinutes*60)));notificationService.notify(userId,paymentRequired?"BOOKING_CREATED":"BOOKING_CONFIRMED",paymentRequired?"Booking created - payment pending":"Booking confirmed",paymentRequired?"Booking "+saved.getBookingNumber()+" is reserved. Complete payment before the payment window expires.":"Booking "+saved.getBookingNumber()+" is confirmed. Payment can be collected at the venue.");return BookingResponse.from(saved);
 }
 @Transactional
 public void release(UUID userId,UUID holdId){BookingHold hold=holds.findLocked(holdId).orElseThrow(()->new NotFoundException("Hold not found"));assertOwner(hold.getUser(),userId);if(hold.getStatus()!=HoldStatus.ACTIVE)throw new ConflictException("Hold is no longer active");BookingSlot slot=slots.findLocked(hold.getSlot().getId()).orElseThrow();slot.releaseHold(hold.getQuantity());hold.change(HoldStatus.RELEASED);}
 @Transactional(readOnly=true) public List<BookingResponse> mine(UUID userId){return bookings.findByUserIdOrderByConfirmedAtDesc(userId).stream().map(BookingResponse::from).toList();}
 @Transactional public BookingResponse cancel(UUID userId,UUID bookingId){Booking booking=bookings.findById(bookingId).orElseThrow(()->new NotFoundException("Booking not found"));assertOwner(booking.getUser(),userId);return cancelBooking(booking,"Cancelled by customer");}
 @Transactional(readOnly=true) public List<BookingResponse> adminList(LocalDate date){return adminList(date,null,null,null,null,null,null,null);}
 @Transactional(readOnly=true) public List<BookingResponse> adminList(LocalDate date,LocalDate from,LocalDate to,String sport,UUID facilityId,String status,String paymentStatus,String query){
  if(date!=null){from=date;to=date;}
  if(from!=null&&to!=null&&from.isAfter(to))throw new ConflictException("From date must be on or before to date");
  final LocalDate fromDate=from, toDate=to;
  final String sportFilter=cleanFilter(sport), statusFilter=cleanFilter(status), paymentFilter=cleanFilter(paymentStatus), textFilter=cleanFilter(query);
  Stream<Booking> stream=bookings.findAllByOrderByConfirmedAtDesc().stream();
  if(fromDate!=null)stream=stream.filter(b->!b.getSlot().getDate().isBefore(fromDate));
  if(toDate!=null)stream=stream.filter(b->!b.getSlot().getDate().isAfter(toDate));
  if(sportFilter!=null)stream=stream.filter(b->equalsIgnoreCase(b.getSlot().getFacility().getSport().getName(),sportFilter)||equalsIgnoreCase(b.getSlot().getFacility().getSport().getCode(),sportFilter));
  if(facilityId!=null)stream=stream.filter(b->facilityId.equals(b.getSlot().getFacility().getId()));
  if(statusFilter!=null)stream=stream.filter(b->equalsIgnoreCase(b.getStatus().name(),statusFilter));
  if(paymentFilter!=null)stream=stream.filter(b->equalsIgnoreCase(b.getPaymentStatus().name(),paymentFilter));
  if(textFilter!=null){String q=textFilter.toLowerCase(Locale.ROOT);stream=stream.filter(b->{User u=b.getUser();return contains(b.getBookingNumber(),q)||contains(u.getFullName(),q)||contains(u.getEmail(),q)||contains(u.getMobile(),q)||contains(b.getSlot().getFacility().getName(),q)||contains(b.getSlot().getFacility().getSport().getName(),q);});}
  return stream.sorted(Comparator.comparing((Booking b)->b.getSlot().getDate()).reversed().thenComparing(b->b.getSlot().getStartTime(),Comparator.reverseOrder())).map(BookingResponse::from).toList();
 }
 @Transactional public BookingResponse checkIn(UUID operatorId,String bookingNumber,BookingCheckInRequest request){Booking booking=bookings.findByBookingNumberIgnoreCase(bookingNumber).orElseThrow(()->new NotFoundException("Booking not found"));if(booking.getStatus()!=BookingStatus.CONFIRMED)throw new ConflictException("Only confirmed bookings can be checked in");if(booking.getPaymentStatus()==BookingPaymentStatus.UNPAID)throw new ConflictException("Collect payment before check-in");if(booking.getCheckedInAt()!=null)throw new ConflictException("Booking is already checked in");booking.checkIn(requireUser(operatorId),request!=null&&request.identityVerified(),request==null?null:request.verificationNote());return BookingResponse.from(booking);}
 @Transactional public BookingResponse checkOut(UUID operatorId,UUID id){Booking b=bookings.findById(id).orElseThrow(()->new NotFoundException("Booking not found"));if(b.getCheckedInAt()==null)throw new ConflictException("Check in the customer before checkout");if(b.getCheckedOutAt()!=null)throw new ConflictException("Booking is already checked out");b.checkOut(requireUser(operatorId));return BookingResponse.from(b);}
 @Transactional public BookingResponse adminCancel(UUID id,String reason){return cancelBooking(bookings.findById(id).orElseThrow(()->new NotFoundException("Booking not found")),reason==null||reason.isBlank()?"Cancelled by operations":reason.trim());}
 @Transactional public BookingResponse undoCheckIn(UUID id){Booking b=bookings.findById(id).orElseThrow(()->new NotFoundException("Booking not found"));if(b.getCheckedInAt()==null)throw new ConflictException("Booking is not checked in");b.undoCheckIn();return BookingResponse.from(b);}
 @Transactional public BookingResponse noShow(UUID id,String reason){Booking b=bookings.findById(id).orElseThrow(()->new NotFoundException("Booking not found"));if(b.getStatus()!=BookingStatus.CONFIRMED||b.getCheckedInAt()!=null)throw new ConflictException("Booking cannot be marked no-show");LocalDateTime eligibleAt=LocalDateTime.of(b.getSlot().getDate(),b.getSlot().getStartTime()).plusMinutes(noShowGraceMinutes);if(LocalDateTime.now().isBefore(eligibleAt))throw new ConflictException("No-show can be marked "+noShowGraceMinutes+" minutes after the slot starts");b.noShow(reason==null||reason.isBlank()?"Marked no-show by operations":reason.trim());return BookingResponse.from(b);}
 @Transactional public int cleanupUnpaid(){int count=0;for(Booking b:bookings.findByStatusAndPaymentExpiresAtLessThanEqual(BookingStatus.PENDING_PAYMENT,Instant.now())){BookingSlot slot=slots.findLocked(b.getSlot().getId()).orElseThrow();slot.cancel(b.getQuantity());b.expirePayment();count++;}return count;}
 @Transactional public void cleanupExpired(){for(BookingHold candidate:holds.findByStatusAndExpiresAtLessThanEqual(HoldStatus.ACTIVE,Instant.now())){BookingHold hold=holds.findLocked(candidate.getId()).orElseThrow();if(hold.getStatus()==HoldStatus.ACTIVE&&hold.expired(Instant.now())){BookingSlot slot=slots.findLocked(hold.getSlot().getId()).orElseThrow();slot.releaseHold(hold.getQuantity());hold.change(HoldStatus.EXPIRED);}}}
 private User requireUser(UUID id){return users.findById(id).orElseThrow(()->new UnauthorizedException("User not found"));}
 private BookingResponse cancelBooking(Booking booking,String reason){if(booking.getStatus()!=BookingStatus.CONFIRMED&&booking.getStatus()!=BookingStatus.PENDING_PAYMENT)throw new ConflictException("Booking cannot be cancelled");BookingSlot slot=slots.findLocked(booking.getSlot().getId()).orElseThrow();if(!LocalDateTime.of(slot.getDate(),slot.getStartTime()).isAfter(LocalDateTime.now()))throw new ConflictException("Started bookings cannot be cancelled");slot.cancel(booking.getQuantity());booking.cancel(reason);return BookingResponse.from(booking);}
 private static String cleanFilter(String value){return value==null||value.isBlank()?null:value.trim();}
 private static boolean equalsIgnoreCase(String left,String right){return left!=null&&left.equalsIgnoreCase(right);}
 private static boolean contains(String value,String needle){return value!=null&&value.toLowerCase(Locale.ROOT).contains(needle);}
 private static void assertOwner(User user,UUID id){if(!user.getId().equals(id))throw new NotFoundException("Resource not found");}
}
