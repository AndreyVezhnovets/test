package org.example.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.dto.ExchangeRateResponse;
import org.example.entity.Currency;
import org.example.repository.CurrencyRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class CurrencyService {

    private final CurrencyRepository currencyRepository;
    private final RestTemplate restTemplate;

    @Value("${api.exchange.url}")
    private String apiUrl;

    @Value("${api.exchange.key}")
    private String apiKey;


    @Getter
    private final Map<String, Currency> currencyMap = new ConcurrentHashMap<>();

    public List<Currency> getCurrencies() {
        return currencyRepository.findAll();
    }

    public Currency getExchangeRate(String code) {
        return currencyMap.get(code);
    }

    public Currency addCurrency(String base) {
        var currency = new Currency();
        currency.setBase(base);
        currency.setExchangeRate(BigDecimal.ZERO);
        return currencyRepository.save(currency);
    }

    @Scheduled(cron = "0 0 * * * ?")
    public void refreshExchangeRates() {
        var currencies = currencyRepository.findAll();
        for (var currency : currencies) {
            var rate = fetchExchangeRateFromAPI(currency.getCode());
            currency.setExchangeRate(rate);
            currencyMap.put(getCurrencyCode(currency), currency);
            currencyRepository.save(currency);
        }
    }

    public BigDecimal fetchExchangeRateFromAPI(String code) {
        var url = apiUrl.replace("{apiKey}", apiKey).replace("{code}", code);

        try {
            var response = restTemplate.getForEntity(url, ExchangeRateResponse.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                var exchangeRateResponse = response.getBody();
                var rate = exchangeRateResponse.getRates().get(code);
                if (rate != null) {
                    return rate;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch exchange rate for currency: " + code, e);
        }

        return BigDecimal.ONE;
    }

    private String getCurrencyCode(Currency currency) {
        return currency.getBase() + "-" + currency.getCode();
    }
}