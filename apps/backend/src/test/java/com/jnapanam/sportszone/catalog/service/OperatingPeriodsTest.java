package com.jnapanam.sportszone.catalog.service;

import com.jnapanam.sportszone.catalog.dto.OperatingHourRequest;
import com.jnapanam.sportszone.catalog.model.Facility;
import com.jnapanam.sportszone.catalog.repository.*;
import com.jnapanam.sportszone.common.exception.BadRequestException;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OperatingPeriodsTest {
 @Test void overlapRejectedBeforeSaving(){
  var catalogue=mock(CatalogueService.class);var hours=mock(FacilityOperatingHourRepository.class);
  var service=new FacilityScheduleService(catalogue,hours,mock(FacilityClosureRepository.class),mock(FacilityScheduleOverrideRepository.class));
  UUID id=UUID.randomUUID();when(catalogue.requireFacilityLocked(id)).thenReturn(mock(Facility.class));
  assertThrows(BadRequestException.class,()->service.replaceOperatingPeriods(id,DayOfWeek.MONDAY,List.of(
   new OperatingHourRequest(DayOfWeek.MONDAY,LocalTime.of(9,0),LocalTime.of(17,0),false),
   new OperatingHourRequest(DayOfWeek.MONDAY,LocalTime.of(16,30),LocalTime.of(21,0),false))));
  verifyNoInteractions(hours);
 }
 @Test void closedDayAndOpenWindowAreMutuallyExclusive(){
  var catalogue=mock(CatalogueService.class);
  var service=new FacilityScheduleService(catalogue,mock(FacilityOperatingHourRepository.class),
    mock(FacilityClosureRepository.class),mock(FacilityScheduleOverrideRepository.class));
  UUID id=UUID.randomUUID();when(catalogue.requireFacilityLocked(id)).thenReturn(mock(Facility.class));
  assertThrows(BadRequestException.class,()->service.replaceOperatingPeriods(id,DayOfWeek.SUNDAY,List.of(
   new OperatingHourRequest(DayOfWeek.SUNDAY,null,null,true),
   new OperatingHourRequest(DayOfWeek.SUNDAY,LocalTime.of(9,0),LocalTime.of(17,0),false))));
 }
}
