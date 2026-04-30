package com.devverdict.catalog.config;

import com.devverdict.catalog.domain.Framework;
import com.devverdict.catalog.repository.FrameworkRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SeedDataConfig {

    @Bean
    CommandLineRunner seedFrameworks(FrameworkRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                repository.saveAll(List.of(
                    new Framework("Java", "Language", "A high-level, class-based, object-oriented programming language that is designed to have as few implementation dependencies as possible.", 4.5),
                    new Framework("Python", "Language", "A high-level, general-purpose programming language. Its design philosophy emphasizes code readability with the use of significant indentation.", 4.7),
                    new Framework("JavaScript", "Language", "A programming language that is one of the core technologies of the World Wide Web, alongside HTML and CSS.", 4.3),
                    new Framework("TypeScript", "Language", "A strict syntactical superset of JavaScript and adds optional static typing to the language.", 4.6),
                    new Framework("Go", "Language", "A statically typed, compiled programming language designed at Google. It is syntactically similar to C, but with memory safety, garbage collection, structural typing, and CSP-style concurrency.", 4.4),
                    new Framework("Rust", "Language", "A multi-paradigm, general-purpose programming language designed for performance and safety, especially safe concurrency.", 4.8),
                    new Framework("Spring Boot", "Framework", "An open source Java-based framework used to create a micro Service. It is developed by Pivotal Team and is used to build stand-alone and production ready spring applications.", 4.5),
                    new Framework("Angular", "Framework", "A platform and framework for building single-page client applications using HTML and TypeScript. Developed by Google.", 4.2),
                    new Framework("React", "Framework", "A free and open-source front-end JavaScript library for building user interfaces based on components. Maintained by Meta.", 4.5),
                    new Framework("Vue.js", "Framework", "An open-source model-view-viewmodel front end JavaScript framework for building user interfaces and single-page applications.", 4.3),
                    new Framework("Django", "Framework", "A Python-based free and open-source web framework that follows the model-template-views architectural pattern.", 4.4),
                    new Framework("Flask", "Framework", "A micro web framework written in Python. It is classified as a microframework because it does not require particular tools or libraries.", 4.1),
                    new Framework("Express.js", "Framework", "A back end web application framework for building RESTful APIs with Node.js.", 4.0),
                    new Framework("FastAPI", "Framework", "A modern, fast web framework for building APIs with Python based on standard Python type hints.", 4.6),
                    new Framework("Node.js", "Runtime", "A cross-platform, open-source JavaScript runtime environment that executes JavaScript code outside a web browser.", 4.2)
                ));
            }
        };
    }
}
