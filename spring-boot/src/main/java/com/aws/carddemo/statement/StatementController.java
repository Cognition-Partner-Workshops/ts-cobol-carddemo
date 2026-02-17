package com.aws.carddemo.statement;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aws.carddemo.statement.dto.StatementRequest;
import com.aws.carddemo.statement.dto.StatementResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/statements")
public class StatementController {

    private final StatementService statementService;

    public StatementController(StatementService statementService) {
        this.statementService = statementService;
    }

    @PostMapping
    public ResponseEntity<StatementResponse> generateStatement(@Valid @RequestBody StatementRequest request) {
        StatementResponse response = statementService.generateStatement(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{statementId}")
    public ResponseEntity<StatementResponse> getStatement(@PathVariable String statementId) {
        return ResponseEntity.ok(statementService.getStatement(statementId));
    }
}
