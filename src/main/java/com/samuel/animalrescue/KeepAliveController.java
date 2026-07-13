package com.samuel.animalrescue.controller;

import com.samuel.animalrescue.dto.KeepAliveResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/system")
public class KeepAliveController {

    @GetMapping("/keep-alive")
    public ResponseEntity<KeepAliveResponse> keepAlive() {
        KeepAliveResponse response = new KeepAliveResponse(
                "UP",
                "Animal Rescue Maze backend is running",
                Instant.now()
        );

        return ResponseEntity.ok(response);
    }
}