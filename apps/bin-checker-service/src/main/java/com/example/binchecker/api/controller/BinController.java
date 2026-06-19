package com.example.binchecker.api.controller;

import com.example.binchecker.api.dto.BinResponse;
import com.example.binchecker.service.BinService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/bin")
public class BinController {

    private final BinService binService;

    public BinController(BinService binService) {
        this.binService = binService;
    }

    @GetMapping("/{bin}")
    public ResponseEntity<BinResponse> getBin(@PathVariable String bin) {
        return ResponseEntity.ok(BinResponse.from(binService.find(bin)));
    }
}
