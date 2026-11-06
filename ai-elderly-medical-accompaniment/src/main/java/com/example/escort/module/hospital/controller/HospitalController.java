package com.example.escort.module.hospital.controller;

import com.example.escort.common.Result;
import com.example.escort.module.hospital.entity.Hospital;
import com.example.escort.module.hospital.mapper.HospitalMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "医院字典") @RestController @RequestMapping("/api/hospital") @RequiredArgsConstructor
public class HospitalController {
    private final HospitalMapper hospitalMapper;
    @Operation(summary = "获取医院列表") @GetMapping("/list")
    public Result<List<Hospital>> list() { return Result.ok(hospitalMapper.selectList(null)); }
}