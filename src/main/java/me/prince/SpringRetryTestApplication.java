package me.prince;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.AlwaysRetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Date;

@SpringBootApplication
@EnableRetry
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

    @GetMapping("/index")
    public String callService(){
        serviceCall.callService();

        return "call Service";
    }

}

@Component
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


@Service
class ServiceCall{
    private RestTemplate restTemplate = new RestTemplate();
    private Log logger = LogFactory.getLog(this.getClass());

    @Retryable(backoff = @Backoff(delay = 3000), maxAttempts = 10)
    public void callService(){
        logger.info("++++++++++++++++++++++++++++++++++++++++++++++++++" + new Date() );
        String str = restTemplate.getForObject("http://demo.url/health",String.class);
        logger.info(str);
    }
}
