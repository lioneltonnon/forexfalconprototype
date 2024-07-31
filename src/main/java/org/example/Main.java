import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.HashSet;

// Domain class representing an Exchange Rate
record ForexTestExchangeRate(String baseCurrency, String targetCurrency, BigDecimal rate, long timestamp) {
    public ForexTestExchangeRate {
        if (baseCurrency == null || baseCurrency.length() != 3)
            throw new IllegalArgumentException("Base currency must be a valid 3-letter ISO code.");
        if (targetCurrency == null || targetCurrency.length() != 3)
            throw new IllegalArgumentException("Target currency must be a valid 3-letter code.");
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("A numerical exchange rate of type BigDecimal is required for a valid ForexTestExchangeRate.");
    }

    @Override
    public String toString() {
        return "ForexTestExchangeRate{" +
                "baseCurrency='" + baseCurrency + '\'' +
                ", targetCurrency='" + targetCurrency + '\'' +
                ", rate=" + rate +
                ", timestamp=" + timestamp +
                '}';
    }
}

// Collection class for storing Exchange Rates
class ForexTestExchangeRateCollection {
    private final ConcurrentMap<String, ForexTestExchangeRate> exchangeRates = new ConcurrentHashMap<>();

    public void addExchangeRate(ForexTestExchangeRate rate) {
        exchangeRates.putIfAbsent(rate.baseCurrency() + ":" + rate.targetCurrency(), rate);
    }

    public ForexTestExchangeRate getExchangeRate(String base, String target) {
        return exchangeRates.get(base + ":" + target);
    }

    public List<ForexTestExchangeRate> getAllExchangeRates() {
        return exchangeRates.values().stream().collect(Collectors.toList());
    }

    public int size() {
        return exchangeRates.size();
    }
}

// Service class to fetch exchange rates (here generates random rates for the prototypes' skae
class ForexTestFetchService {
    public ForexTestExchangeRateCollection fetchRates() {
        ForexTestExchangeRateCollection collection = new ForexTestExchangeRateCollection();

        // Sample set of letters to generate random 3-letter codes
        String[] letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".split("");
        Set<String> uniqueCurrencies = new HashSet<>();
        Random random = new Random();

        while (uniqueCurrencies.size() < 2001) {
            String targetCurrency = letters[random.nextInt(26)] + letters[random.nextInt(26)] + letters[random.nextInt(26)]; // Generate a valid 3-letter code
            if (uniqueCurrencies.add(targetCurrency)) {
                BigDecimal rate = BigDecimal.valueOf(0.05 + (100000 - 0.05) * random.nextDouble()).setScale(10, RoundingMode.HALF_UP);
                collection.addExchangeRate(new ForexTestExchangeRate("USD", targetCurrency, rate, System.currentTimeMillis()));
            }
        }

        return collection;
    }
}

// Service class to process exchange rates
class ForexTestProcessorService {

    public ForexTestExchangeRateCollection calculatePairs(ForexTestExchangeRateCollection baseRates) throws InterruptedException, ExecutionException {
        List<ForexTestExchangeRate> baseRateList = baseRates.getAllExchangeRates();
        ForexTestExchangeRateCollection newRatesCollection = new ForexTestExchangeRateCollection();

        List<CompletableFuture<Void>> futures = baseRateList.stream()
                .flatMap(baseRate -> baseRateList.stream().map(targetRate -> {
                    return CompletableFuture.runAsync(() -> {
                            BigDecimal calculatedRate = baseRate.rate().divide(targetRate.rate(), 10, RoundingMode.HALF_UP);
                            ForexTestExchangeRate newRate = new ForexTestExchangeRate(
                                    baseRate.targetCurrency(),
                                    targetRate.targetCurrency(),
                                    calculatedRate,
                                    System.currentTimeMillis()
                            );
                            newRatesCollection.addExchangeRate(newRate);
                    });
                }))
                .collect(Collectors.toList());

        // Wait for all tasks to complete
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allOf.get();

        return newRatesCollection;
    }
}

// Main application class
class ForexFalconTestApplication {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        // Fetch initial rates
        ForexTestFetchService fetchService = new ForexTestFetchService();

        long fetchStartTime = System.currentTimeMillis();
        ForexTestExchangeRateCollection ratesUSD = fetchService.fetchRates();
        long fetchEndTime = System.currentTimeMillis();

        System.out.println("Fetched rates: " + ratesUSD.size() + " rates");
        System.out.println("Fetching rates took " + (fetchEndTime - fetchStartTime) + " milliseconds.");

        // Process rates to calculate all pairs
        ForexTestProcessorService processorService = new ForexTestProcessorService();

        long processStartTime = System.currentTimeMillis();
        ForexTestExchangeRateCollection allRates = processorService.calculatePairs(ratesUSD);
        long processEndTime = System.currentTimeMillis();

        // Print processing time
        System.out.println("Processing rates took " + (processEndTime - processStartTime) + " milliseconds to process " + allRates.size() + " unique pairs.");
        // print some rates
        List<ForexTestExchangeRate> rates = allRates.getAllExchangeRates();
        int end = Math.min(rates.size(), 10);
        for (ForexTestExchangeRate rate : rates.subList(0, end)) {
            System.out.println(rate);
        }
    }
}

