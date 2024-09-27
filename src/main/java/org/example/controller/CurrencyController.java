package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.entity.Currency;
import org.example.service.CurrencyService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/currencies")
public class CurrencyController {

    private final CurrencyService currencyService;

    @GetMapping
    public List<Currency> getCurrencies() {
        return currencyService.getCurrencies();
    }

    @GetMapping("/{code}")
    public Currency getExchangeRate(@PathVariable String code) {
        return currencyService.getExchangeRate(code);
    }

    @PostMapping("/{code}")
    public Currency addCurrency(@PathVariable String code) {
        return currencyService.addCurrency(code);
    }
}