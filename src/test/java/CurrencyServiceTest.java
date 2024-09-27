import org.example.dto.ExchangeRateResponse;
import org.example.entity.Currency;
import org.example.repository.CurrencyRepository;
import org.example.service.CurrencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CurrencyServiceTest {

    @InjectMocks
    private CurrencyService currencyService;

    @Mock
    private CurrencyRepository currencyRepository;

    @Mock
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        setPrivateField(currencyService, "apiUrl", "https://api.exchangeratesapi.io/v1/latest?access_key={apiKey}&symbols={code}");
        setPrivateField(currencyService, "apiKey", "your_api_key");
        setPrivateField(currencyService, "currencyMap", new ConcurrentHashMap<String, Currency>());
    }

    @Test
    void testGetCurrencies() {
        var currencies = List.of(
                new Currency(1L, "EUR", "USD", BigDecimal.valueOf(1.1)),
                new Currency(2L, "EUR", "CAD", BigDecimal.valueOf(1.5))
        );

        when(currencyRepository.findAll()).thenReturn(currencies);

        var result = currencyService.getCurrencies();
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(currencyRepository, times(1)).findAll();
    }

    @Test
    void testGetExchangeRate() {
        var usd = new Currency(1L, "EUR", "USD", BigDecimal.valueOf(1.1));
        currencyService.getCurrencyMap().put("USD", usd);

        var result = currencyService.getExchangeRate("USD");

        assertNotNull(result);
        assertEquals("USD", result.getCode());
        assertEquals(BigDecimal.valueOf(1.1), result.getExchangeRate());
    }

    @Test
    void testAddCurrency() {
        var newCurrency = new Currency();
        newCurrency.setBase("EUR");
        newCurrency.setCode("JPY");
        newCurrency.setExchangeRate(BigDecimal.ZERO);

        when(currencyRepository.save(any(Currency.class))).thenReturn(newCurrency);

        var result = currencyService.addCurrency("JPY");

        assertNotNull(result);
        assertEquals("JPY", result.getCode());
        assertEquals(BigDecimal.ZERO, result.getExchangeRate());
        verify(currencyRepository, times(1)).save(any(Currency.class));
    }

    @Test
    void testRefreshExchangeRates() {
        var currencies = List.of(
                new Currency(1L, "EUR", "USD", BigDecimal.ZERO),
                new Currency(2L, "EUR", "CAD", BigDecimal.ZERO)
        );

        when(currencyRepository.findAll()).thenReturn(currencies);
        when(currencyRepository.save(any(Currency.class))).thenReturn(currencies.getFirst());

        var mockResponse = new ExchangeRateResponse();
        var rates = Map.of("USD", BigDecimal.valueOf(1.1), "CAD", BigDecimal.valueOf(1.5));
        mockResponse.setRates(rates);

        when(restTemplate.getForEntity(anyString(), eq(ExchangeRateResponse.class)))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        currencyService.refreshExchangeRates();

        verify(currencyRepository, times(2)).save(any(Currency.class));
        assertEquals(BigDecimal.valueOf(1.1), currencyService.getCurrencyMap().get("EUR-USD").getExchangeRate());
    }

    @Test
    void testFetchExchangeRateFromAPI() {
        var mockResponse = new ExchangeRateResponse();
        var rates = Map.of("USD", BigDecimal.valueOf(1.1));
        mockResponse.setRates(rates);

        when(restTemplate.getForEntity(anyString(), eq(ExchangeRateResponse.class)))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        var rate = currencyService.fetchExchangeRateFromAPI("USD");

        assertNotNull(rate);
        assertEquals(BigDecimal.valueOf(1.1), rate);
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}