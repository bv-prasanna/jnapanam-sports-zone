package com.jnapanam.sportszone.usermanagement.dto;import java.util.List;public record AdminUserPageResponse(List<AdminUserResponse>content,int page,int size,long totalElements,int totalPages){}
