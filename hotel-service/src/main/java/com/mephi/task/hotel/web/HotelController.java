package com.mephi.task.hotel.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mephi.task.hotel.domain.Hotel;
import com.mephi.task.hotel.repo.HotelRepository;
import com.mephi.task.hotel.web.dto.HotelDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hotels")
@Tag(name = "Hotels", description = "Hotel management APIs")
public class HotelController {

    private final HotelRepository hotelRepository;

    @GetMapping
    @Operation(summary = "Get all hotels", description = "Returns a list of all hotels")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved list of hotels")
    public List<Hotel> list() {
        return hotelRepository.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a hotel", description = "Creates a new hotel")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Hotel successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid hotel data")
    })
    public Hotel create(@Valid @RequestBody HotelDto dto) {
        Hotel h = new Hotel();
        h.setName(dto.getName());
        h.setAddress(dto.getAddress());
        return hotelRepository.save(h);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a hotel", description = "Completely replaces a hotel by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Hotel successfully replaced"),
            @ApiResponse(responseCode = "404", description = "Hotel not found")
    })
    public Hotel replace(@PathVariable Long id, @Valid @RequestBody HotelDto dto) {
        Hotel h = hotelRepository.findById(id).orElseThrow();
        h.setName(dto.getName());
        h.setAddress(dto.getAddress());
        return hotelRepository.save(h);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update a hotel", description = "Partially updates a hotel by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Hotel successfully updated"),
            @ApiResponse(responseCode = "404", description = "Hotel not found")
    })
    public Hotel update(@PathVariable Long id, @RequestBody HotelDto dto) {
        Hotel h = hotelRepository.findById(id).orElseThrow();
        if (dto.getName() != null) h.setName(dto.getName());
        if (dto.getAddress() != null) h.setAddress(dto.getAddress());
        return hotelRepository.save(h);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a hotel", description = "Deletes a hotel by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Hotel successfully deleted"),
            @ApiResponse(responseCode = "404", description = "Hotel not found")
    })
    public void delete(@PathVariable Long id) {
        hotelRepository.deleteById(id);
    }
}


