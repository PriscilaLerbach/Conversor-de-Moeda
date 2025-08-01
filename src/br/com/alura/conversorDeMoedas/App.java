package br.com.alura.conversorDeMoedas;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

public class App {
    private static final String BASE_URL = "https://v6.exchangerate-api.com/v6/";
    private static final String ENDPOINT = "/latest/USD";
    private static final String API_KEY = "342ed421e527fc376890a807";
    private static final Set<String> MOEDAS_SUPORTADAS = Set.of("ARS", "BOB", "BRL", "CLP", "COP", "USD");
    private static final Scanner scanner = new Scanner(System.in);
    private static final Gson gson = new Gson();
    private static final Map<String, Double> todasAsCotacoes = new HashMap<>();

    private static ExchangeRateService exchangeRateService;
    private static ConversionHistory conversionHistory;

    public static void main(String[] args) throws IOException, InterruptedException {
        // Inicializa serviços
        exchangeRateService = new ExchangeRateService(API_KEY);
        conversionHistory = new ConversionHistory();
        System.out.println("Olá Mundo!");
        String url = BASE_URL + API_KEY + ENDPOINT;
        HttpRequest request = HttpRequest.newBuilder().GET().uri(URI.create(url)).header("Accept", "application/json").build();
        HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).connectTimeout(Duration.ofSeconds(10)).build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            //System.out.println(response.body());
            JsonObject responseObj = gson.fromJson(response.body(), JsonObject.class);
            String result = responseObj.get("result").getAsString();
            if (!"success".equals(result)) {
                throw new RuntimeException("Falha na resposta da API: " + result);
            }
            JsonObject cambiosObj = responseObj.getAsJsonObject("conversion_rates");
            for (String moeda : MOEDAS_SUPORTADAS) {
                if (cambiosObj.has(moeda)) {
                    todasAsCotacoes.put(moeda, cambiosObj.get(moeda).getAsDouble());
                }
            }
        } else {
            throw new IOException("Falha ao obter taxas de câmbio. Código de status: " + response.statusCode());
        }
        System.out.println("Inicializando o Conversor de Moedas...");
        try {
            // Carrega as taxas de câmbio
            // Inicia o menu principal
            showMainMenu();
        } catch (Exception e) {
            System.out.println("Erro ao inicializar o aplicativo: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }

    /**
     * Exibe o menu principal e processa as escolhas do usuário.
     */
    private static void showMainMenu() {
        boolean running = true;

        while (running) {
            System.out.println("\n===== CONVERSOR DE MOEDAS =====");
            System.out.println("1. Converter moedas");
            System.out.println("2. Ver taxas de câmbio disponíveis");
            System.out.println("3. Ver histórico de conversões");
            System.out.println("4. Atualizar taxas de câmbio");
            System.out.println("0. Sair");
            System.out.println("Escolha uma opção: ");

            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    converterMoeda();
                    break;
                case "2":
                    exibirCambiosDisponiveis();
                    break;
                case "3":
                    exibirHistoricoDeConversoes();
                    break;
                case "4":
                    atualizarTaxasDeConversao();
                    break;
                case "0":
                    running = false;
                    System.out.println("Obrigado por usar o Conversor de Moedas!");
                    break;
                default:
                    System.err.println("Opção inválida. Por favor, tente novamente.");
            }
        }
    }

    private static void atualizarTaxasDeConversao() {
        System.out.println("\nAtualizando taxas de câmbio...");
        try {
            exchangeRateService.fetchLatestRates();
            System.out.println("Taxas de câmbio atualizadas com sucesso!");
        } catch (Exception e) {
            System.out.println("Erro ao atualizar taxas de câmbio: " + e.getMessage());
            System.out.println("Detalhes do erro:"+e);
        }
    }

    private static void exibirHistoricoDeConversoes() {
        System.out.println("\n ----- HISTÓRICO DE CONVERSÕES -----");
        conversionHistory.displayHistory();
    }

    private static void exibirCambiosDisponiveis() {
        System.out.println("\n----- MOEDAS DISPONIVEIS -----");
        exchangeRateService.displayAvailableCurrencies();
    }

    private static void converterMoeda() {
        System.out.println("\n----- CONVERSÃO DE MOEDAS -----");

        // Mostra moedas disponíveis
        mostrarCambiosDisponiveis();

        // Solicita a moeda de origem
        System.out.print("\nDigite o código da moeda de origem (ex: BRL): ");
        String fromCurrency = scanner.nextLine().toUpperCase();

        // Solicita a moeda de destino
        System.out.print("Digite o código da moeda de destino (ex: USD): ");
        String toCurrency = scanner.nextLine().toUpperCase();

        // Verifica se as moedas são válidas
        if (!isCurrencySupported(fromCurrency) || !isCurrencySupported(toCurrency)) {
            System.out.println("Uma ou ambas as moedas não são suportadas.");
            return;
        }

        // Exibe as taxas de conversão
        double directRate = getConversionRate(fromCurrency, toCurrency);
        double inverseRate = getConversionRate(toCurrency, fromCurrency);

        System.out.printf("\nTaxa de conversão: 1 %s = %.6f %s\n", fromCurrency, directRate, toCurrency);
        System.out.printf("Taxa de conversão inversa: 1 %s = %.6f %s\n", toCurrency, inverseRate, fromCurrency);

        // Solicita o valor a ser convertido
        System.out.print("\nDigite o valor a ser convertido: ");
        try {
            double amount = Double.parseDouble(scanner.nextLine());
            double convertedAmount = amount * directRate;

            System.out.printf("%.2f %s = %.2f %s\n", amount, fromCurrency, convertedAmount, toCurrency);

            // Registra a conversão no histórico
            conversionHistory.addConversion(fromCurrency, toCurrency, amount, convertedAmount);
        } catch (NumberFormatException e) {
            System.out.println("Valor inválido. Por favor, digite um número válido.");
        }
    }
    /**
     * Obtém a taxa de conversão entre duas moedas.
     *
     * @param fromCurrency Moeda de origem
     * @param toCurrency Moeda de destino
     * @return Taxa de conversão
     */
    public static double getConversionRate(String fromCurrency, String toCurrency) {
        if (!isCurrencySupported(fromCurrency) || !isCurrencySupported(toCurrency)) {
            throw new IllegalArgumentException("Uma ou ambas as moedas não são suportadas");
        }

        // Calcula a taxa de conversão usando USD como base
        double fromRate = todasAsCotacoes.get(fromCurrency);
        double toRate = todasAsCotacoes.get(toCurrency);

        // Converte da moeda de origem para a moeda de destino
        return toRate / fromRate;
    }

    /**
     * Verifica se uma moeda é suportada pelo serviço.
     *
     * @param currencyCode Código da moeda a ser verificada
     * @return true se a moeda for suportada, false caso contrário
     */
    public static boolean isCurrencySupported(String currencyCode) {
        return MOEDAS_SUPORTADAS.contains(currencyCode) && todasAsCotacoes.containsKey(currencyCode);
    }

    private static void mostrarCambiosDisponiveis() {
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
            boolean available = todasAsCotacoes.containsKey(code);
            System.out.println(code + " - " + description + (available ? "" : "(indisponível)"));
        }
    }
}