package br.com.alura.conversorDeMoedas;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Serviço para obter e processar taxas de câmbio da API ExchangeRate.
 */
public class ExchangeRateService {
    private static final String BASE_URL = "https://v6.exchangerate-api.com/v6/";
    private static final String ENDPOINT = "/latest/USD";
    private static final Set<String> SUPPORTED_CURRENCIES = Set.of(
            "ARS", // Peso argentino
            "BOB", // Boliviano boliviano
            "BRL", // Real brasileiro
            "CLP", // Peso chileno
            "COP", // Peso colombiano
            "USD"  // Dólar americano
    );

    private final String apiKey;
    private final HttpClient httpClient;
    private final Gson gson;
    private Map<String, Double> exchangeRates;

    /**
     * Construtor do serviço de taxas de câmbio.
     *
     * @param apiKey Chave de API para o serviço ExchangeRate
     */
    public ExchangeRateService(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.gson = new Gson();
        this.exchangeRates = new HashMap<>();
    }

    /**
     * Busca as taxas de câmbio mais recentes da API.
     *
     * @throws IOException Se ocorrer um erro de I/O durante a requisição
     * @throws InterruptedException Se a operação for interrompida
     */
    public void fetchLatestRates() throws IOException, InterruptedException {
        String url = BASE_URL + apiKey + ENDPOINT;

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            parseResponse(response.body());
        } else {
            throw new IOException("Falha ao obter taxas de câmbio. Código de status: " + response.statusCode());
        }
    }

    /**
     * Analisa a resposta JSON da API e extrai as taxas de câmbio.
     *
     * @param jsonResponse Resposta JSON da API
     */
    private void parseResponse(String jsonResponse) {
        JsonObject responseObj = gson.fromJson(jsonResponse, JsonObject.class);

        // Verifica se a resposta foi bem-sucedida
        String result = responseObj.get("result").getAsString();
        if (!"success".equals(result)) {
            throw new RuntimeException("Falha na resposta da API: " + result);
        }

        // Extrai as taxas de câmbio
        JsonObject ratesObj = responseObj.getAsJsonObject("conversion_rates");
        Map<String, Double> allRates = new HashMap<>();

        for (String currency : SUPPORTED_CURRENCIES) {
            if (ratesObj.has(currency)) {
                allRates.put(currency, ratesObj.get(currency).getAsDouble());
            }
        }

        this.exchangeRates = allRates;
    }

    /**
     * Verifica se uma moeda é suportada pelo serviço.
     *
     * @param currencyCode Código da moeda a ser verificada
     * @return true se a moeda for suportada, false caso contrário
     */
    public boolean isCurrencySupported(String currencyCode) {
        return SUPPORTED_CURRENCIES.contains(currencyCode) && exchangeRates.containsKey(currencyCode);
    }

    /**
     * Obtém a taxa de conversão entre duas moedas.
     *
     * @param fromCurrency Moeda de origem
     * @param toCurrency Moeda de destino
     * @return Taxa de conversão
     */
    public double getConversionRate(String fromCurrency, String toCurrency) {
        if (!isCurrencySupported(fromCurrency) || !isCurrencySupported(toCurrency)) {
            throw new IllegalArgumentException("Uma ou ambas as moedas não são suportadas");
        }

        // Calcula a taxa de conversão usando USD como base
        double fromRate = exchangeRates.get(fromCurrency);
        double toRate = exchangeRates.get(toCurrency);

        // Converte da moeda de origem para a moeda de destino
        return toRate / fromRate;
    }

    /**
     * Exibe as moedas disponíveis e suas descrições.
     */
    public void displayAvailableCurrencies() {
        Map<String, String> currencyDescriptions = new TreeMap<>();
        currencyDescriptions.put("ARS", "Peso argentino");
        currencyDescriptions.put("BOB", "Boliviano boliviano");
        currencyDescriptions.put("BRL", "Real brasileiro");
        currencyDescriptions.put("CLP", "Peso chileno");
        currencyDescriptions.put("COP", "Peso colombiano");
        currencyDescriptions.put("USD", "Dólar americano");

        for (Map.Entry<String, String> entry : currencyDescriptions.entrySet()) {
            String code = entry.getKey();
            String description = entry.getValue();
            boolean available = exchangeRates.containsKey(code);

            System.out.println(
                    code + " - " + description + (
                    available ? "" : "(indisponível)"));
        }
    }
}