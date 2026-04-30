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
                    // ─── Languages ───
                    new Framework("Java", "Language", "A high-level, class-based, object-oriented programming language designed to have as few implementation dependencies as possible. Powers enterprise backends, Android apps, and big data systems.", 4.5),
                    new Framework("Python", "Language", "A high-level, general-purpose programming language emphasizing code readability with significant indentation. Dominates data science, AI/ML, automation, and web development.", 4.7),
                    new Framework("JavaScript", "Language", "The programming language of the Web. One of the core technologies of the World Wide Web alongside HTML and CSS. Runs in every browser and on servers via Node.js.", 4.3),
                    new Framework("TypeScript", "Language", "A strict syntactical superset of JavaScript adding optional static typing. Developed by Microsoft, it scales JS to large applications with excellent IDE support.", 4.6),
                    new Framework("Go", "Language", "A statically typed, compiled language designed at Google. Syntactically similar to C but with memory safety, garbage collection, and CSP-style concurrency via goroutines.", 4.4),
                    new Framework("Rust", "Language", "A multi-paradigm systems language focused on performance and safety, especially safe concurrency. Zero-cost abstractions without a garbage collector.", 4.8),
                    new Framework("C++", "Language", "A general-purpose programming language created as an extension of C. Powers game engines, operating systems, browsers, high-frequency trading, and embedded systems.", 4.2),
                    new Framework("C#", "Language", "A modern, object-oriented language developed by Microsoft. Powers .NET applications, Unity games, enterprise software, and Windows desktop apps.", 4.4),
                    new Framework("Ruby", "Language", "A dynamic, open-source language with a focus on simplicity and productivity. Famous for Ruby on Rails and elegant, readable syntax.", 4.1),
                    new Framework("PHP", "Language", "A widely-used open source general-purpose scripting language especially suited for web development. Powers WordPress, Laravel, and 70%+ of the web.", 3.9),
                    new Framework("Swift", "Language", "A powerful and intuitive programming language for iOS, iPadOS, macOS, watchOS, and tvOS. Developed by Apple with modern safety features.", 4.5),
                    new Framework("Kotlin", "Language", "A modern, statically typed language that makes developers more productive. Fully interoperable with Java and the preferred language for Android development.", 4.6),
                    new Framework("Scala", "Language", "Combines object-oriented and functional programming in one concise, high-level language. Runs on the JVM and powers big data frameworks like Spark.", 4.0),
                    new Framework("Elixir", "Language", "A dynamic, functional language designed for building scalable and maintainable applications. Runs on the Erlang VM (BEAM) with excellent concurrency.", 4.3),
                    new Framework("Clojure", "Language", "A modern Lisp dialect for the JVM. Emphasizes immutability, functional programming, and interactive development via the REPL.", 4.1),
                    new Framework("Haskell", "Language", "A purely functional programming language with strong static typing and lazy evaluation. Known for elegant abstractions and mathematical rigor.", 4.2),
                    new Framework("R", "Language", "A language and environment for statistical computing and graphics. The de facto standard for data analysis, visualization, and academic research.", 3.8),
                    new Framework("Lua", "Language", "A lightweight, high-level, multi-paradigm language designed for embedded use in applications. Powers game scripting (Roblox, WoW) and Neovim configs.", 4.0),
                    new Framework("Dart", "Language", "A client-optimized language for fast apps on any platform. Developed by Google and the language behind the Flutter UI toolkit.", 4.2),
                    new Framework("Julia", "Language", "A high-level, high-performance dynamic language for technical computing. Combines the ease of Python with the speed of C for numerical work.", 4.3),

                    // ─── Frontend Frameworks ───
                    new Framework("React", "Frontend", "A free and open-source front-end JavaScript library for building user interfaces based on components. Maintained by Meta with a massive ecosystem.", 4.5),
                    new Framework("Angular", "Frontend", "A platform and framework for building single-page client applications using HTML and TypeScript. Developed by Google with a complete toolset.", 4.2),
                    new Framework("Vue.js", "Frontend", "An open-source MVVM front-end JavaScript framework for building UIs and single-page applications. Known for gentle learning curve and flexibility.", 4.3),
                    new Framework("Svelte", "Frontend", "A radical new approach to building user interfaces. Converts components into imperative code at compile time for minimal runtime overhead.", 4.6),
                    new Framework("SolidJS", "Frontend", "A declarative, efficient, and flexible JavaScript library for building user interfaces. Uses fine-grained reactivity without a virtual DOM.", 4.5),
                    new Framework("Next.js", "Frontend", "A React framework for production-grade applications. Enables server-side rendering, static site generation, API routes, and the App Router.", 4.7),
                    new Framework("Nuxt.js", "Frontend", "An intuitive Vue framework for building full-stack web applications. Offers server-side rendering, auto-imports, and file-based routing.", 4.4),
                    new Framework("Astro", "Frontend", "The all-in-one web framework designed for speed. Ships zero JavaScript by default using islands architecture for interactive components.", 4.5),
                    new Framework("Remix", "Frontend", "A full-stack web framework focused on web standards and modern UX. Leverages the edge, nested routing, and progressive enhancement.", 4.3),
                    new Framework("Qwik", "Frontend", "A resumable JavaScript framework designed for instant-loading web applications. Achieves near-zero JavaScript on initial page load.", 4.2),
                    new Framework("Preact", "Frontend", "A fast 3kB alternative to React with the same modern API. Ideal for performance-critical applications and embedded widgets.", 4.3),
                    new Framework("Alpine.js", "Frontend", "A rugged, minimal framework for composing JavaScript behavior in markup. Think of it as Tailwind for JavaScript.", 4.1),

                    // ─── Backend Frameworks ───
                    new Framework("Spring Boot", "Backend", "An open-source Java framework for creating production-ready, stand-alone Spring applications. The industry standard for enterprise Java microservices.", 4.5),
                    new Framework("Django", "Backend", "A Python-based free and open-source web framework following the model-template-views pattern. Batteries-included with auth, admin, and ORM.", 4.4),
                    new Framework("Flask", "Backend", "A micro web framework written in Python. Classified as a microframework because it does not require particular tools or libraries.", 4.1),
                    new Framework("FastAPI", "Backend", "A modern, fast web framework for building APIs with Python based on standard type hints. Automatic OpenAPI docs and async support out of the box.", 4.6),
                    new Framework("Express.js", "Backend", "A minimal and flexible Node.js web application framework providing a robust set of features for web and mobile applications.", 4.0),
                    new Framework("NestJS", "Backend", "A progressive Node.js framework for building efficient, reliable, and scalable server-side applications. Inspired by Angular's architecture.", 4.4),
                    new Framework("Ruby on Rails", "Backend", "A server-side web application framework written in Ruby. Emphasizes convention over configuration and rapid development.", 4.2),
                    new Framework("Laravel", "Backend", "A web application framework with expressive, elegant syntax. The PHP framework for web artisans with Eloquent ORM and Blade templating.", 4.3),
                    new Framework("ASP.NET Core", "Backend", "A cross-platform, high-performance, open-source framework for building modern, cloud-based, and internet-connected applications.", 4.5),
                    new Framework("Phoenix", "Backend", "A productive web framework for Elixir that does not compromise speed or maintainability. Built for real-time applications with LiveView.", 4.4),
                    new Framework("Gin", "Backend", "A high-performance HTTP web framework for Go. Features a Martini-like API with up to 40x faster performance.", 4.3),
                    new Framework("Echo", "Backend", "A high-performance, minimalist Go web framework. Optimized for building REST APIs with middleware support and great documentation.", 4.2),
                    new Framework("Fiber", "Backend", "An Express-inspired web framework built on top of Fasthttp, the fastest HTTP engine for Go. Designed to ease things up for fast development.", 4.4),
                    new Framework("Fastify", "Backend", "A fast and low overhead web framework for Node.js. Highly performant with a powerful plugin architecture and great developer experience.", 4.3),
                    new Framework("Actix Web", "Backend", "A powerful, pragmatic, and extremely fast web framework for Rust. Built on top of Actix actor framework with zero-cost abstractions.", 4.5),
                    new Framework("Rocket", "Backend", "A web framework for Rust that makes it simple to write fast, secure web applications without sacrificing flexibility or type safety.", 4.3),
                    new Framework("Tornado", "Backend", "A Python web framework and asynchronous networking library. Known for high performance and handling thousands of concurrent connections.", 3.9),
                    new Framework("Bun", "Runtime", "An all-in-one JavaScript runtime and toolkit designed for speed. Bundler, test runner, and Node.js-compatible runtime written in Zig.", 4.4),
                    new Framework("Node.js", "Runtime", "A cross-platform, open-source JavaScript runtime environment that executes JavaScript code outside a web browser. Powers the modern web backend.", 4.2),
                    new Framework("Deno", "Runtime", "A modern runtime for JavaScript and TypeScript with secure defaults, native TypeScript support, and a standard library. Created by Node's original author.", 4.1),

                    // ─── Mobile Development ───
                    new Framework("Flutter", "Mobile", "Google's UI toolkit for building natively compiled applications for mobile, web, and desktop from a single codebase using Dart.", 4.5),
                    new Framework("React Native", "Mobile", "A framework for building native apps using React. Write once in JavaScript and render natively on iOS and Android.", 4.3),
                    new Framework("SwiftUI", "Mobile", "A modern way to declare user interfaces for any Apple device. Create beautiful, dynamic apps faster with declarative syntax.", 4.4),
                    new Framework("Kotlin Multiplatform", "Mobile", "A framework to share code across mobile, web, and desktop while retaining native performance and UI on each platform.", 4.3),
                    new Framework("Ionic", "Mobile", "An open-source UI toolkit for building performant, high-quality mobile and desktop apps using web technologies with native runtime.", 4.0),
                    new Framework("Capacitor", "Mobile", "A cross-platform native runtime for web apps. Deploy web apps natively to iOS, Android, and more with full access to native SDKs.", 4.2),
                    new Framework(".NET MAUI", "Mobile", "A cross-platform framework for creating native mobile and desktop apps with C# and XAML. The evolution of Xamarin.Forms.", 4.1),

                    // ─── Databases & Data Stores ───
                    new Framework("PostgreSQL", "Database", "A powerful, open-source object-relational database system with over 35 years of active development. Known for reliability and feature robustness.", 4.8),
                    new Framework("MySQL", "Database", "The world's most popular open-source relational database. Powers everything from small blogs to massive enterprise applications.", 4.3),
                    new Framework("MongoDB", "Database", "A source-available cross-platform document-oriented database program. Classified as a NoSQL database using JSON-like documents.", 4.2),
                    new Framework("Redis", "Database", "An in-memory data structure store used as a database, cache, message broker, and streaming engine. Sub-millisecond response times.", 4.6),
                    new Framework("SQLite", "Database", "A C-language library that implements a small, fast, self-contained, high-reliability, full-featured SQL database engine. Zero configuration.", 4.5),
                    new Framework("Elasticsearch", "Database", "A distributed, RESTful search and analytics engine capable of addressing a growing number of use cases. Full-text search at scale.", 4.4),
                    new Framework("Cassandra", "Database", "A free and open-source distributed wide-column store NoSQL database. Designed to handle large amounts of data across many servers.", 4.0),
                    new Framework("Neo4j", "Database", "A graph database management system. The world's leading graph database, optimized for storing and querying connected data.", 4.2),
                    new Framework("Supabase", "Database", "An open-source Firebase alternative. Provides a Postgres database, authentication, instant APIs, and real-time subscriptions.", 4.5),
                    new Framework("Prisma", "Database", "Next-generation Node.js and TypeScript ORM. Features an intuitive data model, automated migrations, type-safety, and auto-completion.", 4.4),
                    new Framework("Hibernate", "Database", "A Java-based ORM providing a framework for mapping an object-oriented domain model to a traditional relational database.", 4.2),
                    new Framework("Drizzle", "Database", "A lightweight, type-safe SQL-like ORM for TypeScript. Brings SQL to your TypeScript code with great performance.", 4.3),

                    // ─── CSS & Styling ───
                    new Framework("Tailwind CSS", "Styling", "A utility-first CSS framework packed with classes like flex, pt-4, and text-center. Build modern designs without leaving your HTML.", 4.6),
                    new Framework("Bootstrap", "Styling", "The most popular HTML, CSS, and JS library in the world. Build responsive, mobile-first projects with a comprehensive component library.", 4.1),
                    new Framework("Sass", "Styling", "The most mature, stable, and powerful professional grade CSS extension language. Variables, nesting, mixins, and inheritance for CSS.", 4.3),
                    new Framework("Styled Components", "Styling", "A CSS-in-JS library for React and React Native that lets you use component-level styles in your application.", 4.2),
                    new Framework("Emotion", "Styling", "A library designed for writing CSS styles with JavaScript. Provides powerful and predictable style composition with great performance.", 4.1),
                    new Framework("PostCSS", "Styling", "A tool for transforming CSS with JavaScript. Powers Autoprefixer, CSS Modules, and countless other modern CSS workflows.", 4.4),

                    // ─── DevOps & Infrastructure ───
                    new Framework("Docker", "DevOps", "A platform designed to help developers build, share, and run container applications. The industry standard for containerization.", 4.7),
                    new Framework("Kubernetes", "DevOps", "An open-source container orchestration system for automating software deployment, scaling, and management. The de facto cloud OS.", 4.5),
                    new Framework("Terraform", "DevOps", "An infrastructure-as-code software tool for building, changing, and versioning infrastructure safely and efficiently across cloud providers.", 4.4),
                    new Framework("Ansible", "DevOps", "An open-source software provisioning, configuration management, and application-deployment tool enabling infrastructure as code.", 4.2),
                    new Framework("Jenkins", "DevOps", "An open-source automation server helping to automate the parts of software development related to building, testing, and deploying.", 4.0),
                    new Framework("GitHub Actions", "DevOps", "A continuous integration and continuous delivery platform that allows you to automate your build, test, and deployment pipeline.", 4.5),
                    new Framework("ArgoCD", "DevOps", "A declarative, GitOps continuous delivery tool for Kubernetes. Automates the deployment of desired application states from Git repos.", 4.4),
                    new Framework("Prometheus", "DevOps", "An open-source systems monitoring and alerting toolkit. Stores metrics as time series data with a powerful query language.", 4.5),
                    new Framework("Grafana", "DevOps", "A multi-platform open-source analytics and interactive visualization web application. Create beautiful dashboards from many data sources.", 4.5),
                    new Framework("Nginx", "DevOps", "A high-performance HTTP server, reverse proxy, and load balancer. Powers millions of websites with low memory footprint and high concurrency.", 4.6),

                    // ─── Testing ───
                    new Framework("JUnit", "Testing", "A simple framework to write repeatable tests for Java. The de facto standard for unit testing in the Java ecosystem.", 4.4),
                    new Framework("Jest", "Testing", "A delightful JavaScript testing framework with a focus on simplicity. Works with Babel, TypeScript, Node, React, Angular, and Vue.", 4.5),
                    new Framework("Cypress", "Testing", "A next-generation front-end testing tool built for the modern web. Fast, easy, and reliable testing for anything that runs in a browser.", 4.4),
                    new Framework("Playwright", "Testing", "A framework for web testing and automation. Enables reliable end-to-end testing for modern web apps with cross-browser support.", 4.6),
                    new Framework("Vitest", "Testing", "A next-generation testing framework powered by Vite. Blazing fast unit testing with native ESM, TypeScript, and JSX support.", 4.4),
                    new Framework("Mockito", "Testing", "A mocking framework for Java unit tests. Lets you write beautiful tests with a clean and simple API for stubbing and verification.", 4.3),
                    new Framework("Testcontainers", "Testing", "A Java library that supports JUnit tests, providing lightweight, throwaway instances of common databases and services in Docker.", 4.5),

                    // ─── AI & Machine Learning ───
                    new Framework("TensorFlow", "AI/ML", "An end-to-end open-source platform for machine learning. A comprehensive ecosystem of tools, libraries, and community resources.", 4.3),
                    new Framework("PyTorch", "AI/ML", "An open-source machine learning framework that accelerates the path from research prototyping to production deployment. Dynamic computation graphs.", 4.5),
                    new Framework("scikit-learn", "AI/ML", "A Python module for machine learning built on top of SciPy. Simple and efficient tools for predictive data analysis.", 4.4),
                    new Framework("Hugging Face Transformers", "AI/ML", "State-of-the-art machine learning for PyTorch, TensorFlow, and JAX. Pre-trained models for NLP, computer vision, and audio.", 4.6),
                    new Framework("LangChain", "AI/ML", "A framework for developing applications powered by language models. Chains, agents, and memory for building context-aware AI apps.", 4.2),
                    new Framework("OpenAI API", "AI/ML", "A platform for accessing state-of-the-art AI models including GPT-4, DALL-E, and Whisper. Simple REST API for AI integration.", 4.4),

                    // ─── Build Tools & Package Managers ───
                    new Framework("Vite", "Build Tool", "A next-generation frontend tooling solution. Instant server start, lightning fast HMR, and optimized builds using Rollup under the hood.", 4.7),
                    new Framework("Webpack", "Build Tool", "A static module bundler for modern JavaScript applications. When webpack processes your application, it builds a dependency graph internally.", 4.2),
                    new Framework("Rollup", "Build Tool", "A module bundler for JavaScript which compiles small pieces of code into something larger and more complex, such as a library or application.", 4.3),
                    new Framework("Parcel", "Build Tool", "A zero-configuration bundler for web applications. Out-of-the-box support for JS, CSS, HTML, images, and more with no config needed.", 4.1),
                    new Framework("Turborepo", "Build Tool", "A high-performance build system for JavaScript and TypeScript codebases. Intelligent caching and task orchestration for monorepos.", 4.4),
                    new Framework("Nx", "Build Tool", "A smart, fast and extensible build system with first-class monorepo support and powerful integrations. Built by ex-Googlers.", 4.3),
                    new Framework("Maven", "Build Tool", "A software project management and comprehension tool based on the concept of a project object model (POM). The standard for Java builds.", 4.2),
                    new Framework("Gradle", "Build Tool", "An open-source build automation tool focused on flexibility and performance. Supports Java, Kotlin, C++, and more with a Groovy/Kotlin DSL.", 4.3),
                    new Framework("npm", "Build Tool", "The default package manager for the JavaScript runtime environment Node.js. The world's largest software registry with millions of packages.", 4.2),
                    new Framework("pnpm", "Build Tool", "A fast, disk space efficient package manager for Node.js. Uses content-addressable storage and strict dependency resolution.", 4.5),

                    // ─── Message Brokers & Streaming ───
                    new Framework("Apache Kafka", "Messaging", "A distributed event store and stream-processing platform. High-throughput, low-latency handling of real-time data feeds at scale.", 4.5),
                    new Framework("RabbitMQ", "Messaging", "The most widely deployed open-source message broker. Supports multiple messaging protocols with reliable delivery patterns.", 4.3),
                    new Framework("Apache Pulsar", "Messaging", "A cloud-native, distributed messaging and streaming platform originally created at Yahoo. Multi-tenant with geo-replication.", 4.1),
                    new Framework("NATS", "Messaging", "A simple, secure, and high-performance open-source messaging system for cloud-native applications, IoT messaging, and microservices.", 4.2),

                    // ─── Other Notable Tools ───
                    new Framework("GraphQL", "API", "A query language for APIs and a runtime for fulfilling those queries with your existing data. Get exactly what you need in a single request.", 4.4),
                    new Framework("tRPC", "API", "End-to-end typesafe APIs made easy. Build and consume fully typesafe APIs without schemas or code generation. The future of API development.", 4.5),
                    new Framework("gRPC", "API", "A high-performance, open-source universal RPC framework developed by Google. Uses Protocol Buffers and HTTP/2 for efficient communication.", 4.3),
                    new Framework("Socket.io", "API", "A library that enables real-time, bidirectional, and event-based communication between the browser and the server. Websockets with fallbacks.", 4.2),
                    new Framework("Electron", "Desktop", "A framework for building cross-platform desktop applications with JavaScript, HTML, and CSS. Powers VS Code, Slack, and Discord.", 4.1),
                    new Framework("Tauri", "Desktop", "A framework for building tiny, fast binaries for all major desktop platforms using web technologies. Rust backend with web frontend.", 4.4),
                    new Framework("Unity", "Game Engine", "A cross-platform game engine developed by Unity Technologies. The world's leading platform for creating and operating interactive real-time 3D content.", 4.3),
                    new Framework("Unreal Engine", "Game Engine", "A state-of-the-art real-time 3D creation tool for photoreal visuals and immersive experiences. Powers AAA games and virtual production.", 4.5),
                    new Framework("Godot", "Game Engine", "A free and open-source game engine made for 2D and 3D game development. Lightweight with a unique node-based architecture.", 4.4),
                    new Framework("Three.js", "Graphics", "A cross-browser JavaScript library and API used to create and display animated 3D computer graphics in a web browser using WebGL.", 4.5),
                    new Framework("WebAssembly", "Runtime", "A binary instruction format for a stack-based virtual machine. Enables high-performance applications on the web at near-native speed.", 4.3),
                    new Framework("Git", "Tool", "A free and open-source distributed version control system designed to handle everything from small to very large projects with speed and efficiency.", 4.8)
                ));
            }
        };
    }
}
