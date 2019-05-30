package hello;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.beans.factory.annotation.Value;
@RefreshScope
@RestController
public class HelloController {
    @Value("${message:new message}")
    private String message;

    @Value("${username:new username}")
    private String username;

    @RequestMapping("/message")
    public String getMessage() {
       //message = "Greetings from Spring Cloud Config Server! "+message;
       return this.message;
    }

    @RequestMapping("/username")
    public String getUsername() {
       return this.username;
    }
}

