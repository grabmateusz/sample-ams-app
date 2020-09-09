package pl.magrab.pesleague;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class GuavaCacheTest {

    private static final ExecutorService executorService = Executors.newFixedThreadPool(20);

    private static final Logger log = LoggerFactory.getLogger(GuavaCacheTest.class);

    @Test
    public void test() throws ExecutionException {
        LoadingCache<String, String> cache = CacheBuilder.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build(
                        new CacheLoader<String, String>() {
                            public String load(String key) {
                                String value = key + "_value";
                                log.info("Loading to cache value: {}", value);
                                return value;
                            }

                            @Override
                            public Map<String, String> loadAll(Iterable<? extends String> keys) throws Exception {
                                log.info("Loading to cache values: {}", keys);

                                List<Map.Entry<String, String>> list = StreamSupport.stream(keys.spliterator(), false).map(k ->
                                        executorService.submit((Callable<Map.Entry<String, String>>) () -> {
                                            String value = k + "_value";
                                            log.info("Asynchronous load to cache value: {}", value);
                                            return new AbstractMap.SimpleEntry<>(k, value);
                                        })
                                ).map(f -> {
                                    Map.Entry<String, String> result = null;
                                    try {
                                        result = f.get();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    } catch (ExecutionException e) {
                                        e.printStackTrace();
                                    }
                                    return result;
                                }).collect(Collectors.toList());

                                Map<String, String> result = new HashMap<>();

                                for (Map.Entry<String, String> entry : list) {
                                    result.put(entry.getKey(), entry.getValue());
                                }

                                return result;
                            }
                        });

        cache.get("1");
        cache.get("2");
        cache.get("3");
        cache.get("4");
        cache.get("5");

        cache.getAll(new Iterable<String>() {
            @Override
            public Iterator<String> iterator() {
                return Arrays.asList("1", "2", "3", "4", "5", "6").iterator();
            }
        });
    }
}
