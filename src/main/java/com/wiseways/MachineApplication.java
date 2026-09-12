package com.wiseways;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point — equivalent to:
 *
 *   app = Flask(__name__)
 *   if __name__ == '__main__':
 *       app.run(port=5000, debug=True)
 */
@SpringBootApplication
public class MachineApplication {

    public static void main(String[] args) {
        SpringApplication.run(MachineApplication.class, args);
    }
}
