import org.example.controller.CurrencyController;
import org.example.entity.Currency;
import org.example.service.CurrencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CurrencyController.class)
@ContextConfiguration(classes = CurrencyControllerTest.Config.class)
class CurrencyControllerTest {

    @Configuration
    @ComponentScan(basePackageClasses = CurrencyController.class)  // Scan the controller only
    static class Config {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CurrencyService currencyService;

    private List<Currency> currencyList;

    @BeforeEach
    void setUp() {
        currencyList = Arrays.asList(
                new Currency(1L, "EUR", "USD", BigDecimal.valueOf(1.115872)),
                new Currency(2L, "EUR", "AUD", BigDecimal.valueOf(1.61634))
        );
    }

    @Test
    void shouldReturnListOfCurrencies() throws Exception {
        given(currencyService.getCurrencies()).willReturn(currencyList);

        mockMvc.perform(get("/api/currencies"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("[{\"id\":1,\"base\":\"EUR\",\"code\":\"USD\",\"exchangeRate\":1.115872}," +
                        "{\"id\":2,\"base\":\"EUR\",\"code\":\"AUD\",\"exchangeRate\":1.61634}]"));
    }

    @Test
    void shouldReturnExchangeRateForCurrency() throws Exception {
        given(currencyService.getExchangeRate("USD")).willReturn(new Currency(1L, "EUR", "USD", BigDecimal.valueOf(1.115872)));

        mockMvc.perform(get("/api/currencies/USD"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"id\":1,\"base\":\"EUR\",\"code\":\"USD\",\"exchangeRate\":1.115872}"));
    }

    @Test
    void shouldAddNewCurrency() throws Exception {
        Currency newCurrency = new Currency(3L, "EUR", "JPY", BigDecimal.ZERO);
        given(currencyService.addCurrency("JPY")).willReturn(newCurrency);

        mockMvc.perform(post("/api/currencies/JPY"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"id\":3,\"base\":\"EUR\",\"code\":\"JPY\",\"exchangeRate\":0}"));
    }
}