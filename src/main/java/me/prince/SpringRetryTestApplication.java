package me.prince;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.AlwaysRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Date;

@SpringBootApplication
@EnableRetry
@EnableFeignClients
@EnableCircuitBreaker
public class SpringRetryTestApplication {

    @Autowired
    private RetryTemplate retryTemplate;

    private RestTemplate restTemplate = new RestTemplate();

    private Log logger = LogFactory.getLog(this.getClass());

    public static void main(String[] args) {
        SpringApplication.run(SpringRetryTestApplication.class, args);
    }

    @Bean
    public RetryTemplate retryTemplate() {
        AlwaysRetryPolicy retryPolicy = new AlwaysRetryPolicy();
//        retryPolicy.setMaxAttempts(50);

        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(30000); // 15 seconds

        RetryTemplate template = new RetryTemplate();
        template.setRetryPolicy(retryPolicy);
        template.setBackOffPolicy(backOffPolicy);

        return template;
    }
}

@RestController
class WelcomeController{

    @Autowired
    private ServiceCall serviceCall;

    @Autowired
    private FeignServiceTest feignServiceTest;

    @Autowired
    private UaaAdminFeignTest uaaAdminFeignTest;

    @GetMapping(value = "/feign", produces = "application/json")
    public String feignService(){
        return feignServiceTest.version() ;
    }

    @GetMapping(value = "/feign2", produces = "application/json")
    public String uaaAdminFeignTest(){
        return uaaAdminFeignTest.version() ;
    }

    @GetMapping("/index")
    public String callService(){
        return serviceCall.callService();
    }

}

//@Component
class RetryTest implements ApplicationRunner{

    @Autowired
    private RetryTemplate retryTemplate;

    private RestTemplate restTemplate = new RestTemplate();

    private Log logger = LogFactory.getLog(this.getClass());

    @Override
    public void run(ApplicationArguments args) throws Exception {
        retryTemplate.execute(context -> {
            logger.info("---------------------------------------" + new Date() );
            String str = restTemplate.getForObject("http://demo.url/health",String.class);
            logger.info(str);

            return null;
        });
    }

}

@FeignClient(name="test", url = "http://demo.url")
interface FeignServiceTest{
    @RequestMapping(value = "/info", method = RequestMethod.GET)
    String version();
}


@FeignClient(name="test", url = "http://demo.url", fallback = UaaAdminFeignTestFallBack.class)
interface UaaAdminFeignTest{
    @RequestMapping(value = "/health", method = RequestMethod.GET)
    String version();
}

@Component
class UaaAdminFeignTestFallBack implements UaaAdminFeignTest {

    @Override
    public String version() {
        return "I am die UaaAdminFeignTestFallBack !";
    }
}


@Service
class ServiceCall{
    private RestTemplate restTemplate = new RestTemplate();
    private Log logger = LogFactory.getLog(this.getClass());

//    @Retryable(backoff = @Backoff(delay = 3000), maxAttempts = 10)
    @HystrixCommand(fallbackMethod = "fallBack")
    public String callService(){
        logger.info("++++++++++++++++++++++++++++++++++++++++++++++++++" + new Date() );
        String str = restTemplate.getForObject("http://demo.url/health",String.class);
        logger.info(str);
        return str;
    }

    static String fallBack(){
        return "I am die ServiceCall";
    }
}



