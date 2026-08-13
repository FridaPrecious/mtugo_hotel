package com.mtugo.mtugo_hotel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffDashboardResponse {
    private List<StaffOrderDTO> paid;
    private List<StaffOrderDTO> preparing;
    private List<StaffOrderDTO> ready;
    private int paidCount;
    private int preparingCount;
    private int readyCount;
}