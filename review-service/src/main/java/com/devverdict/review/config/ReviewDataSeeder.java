package com.devverdict.review.config;

import com.devverdict.review.domain.OutboxEvent;
import com.devverdict.review.domain.Review;
import com.devverdict.review.repository.OutboxEventRepository;
import com.devverdict.review.repository.ReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class ReviewDataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(ReviewDataSeeder.class);
    private static final int FRAMEWORK_COUNT = 126;
    private static final int REVIEWS_PER_FRAMEWORK_MIN = 3;
    private static final int REVIEWS_PER_FRAMEWORK_MAX = 7;
    private static final int USER_COUNT = 16; // admin (1) + 15 dummy users

    private final ReviewRepository reviewRepository;
    private final OutboxEventRepository outboxRepository;
    private final Random random = new Random(42); // seeded for reproducibility

    public ReviewDataSeeder(ReviewRepository reviewRepository,
                            OutboxEventRepository outboxRepository) {
        this.reviewRepository = reviewRepository;
        this.outboxRepository = outboxRepository;
    }

    @Override
    public void run(String... args) {
        long existingReviews = reviewRepository.count();
        long existingOutbox = outboxRepository.count();

        if (existingReviews > 0 && existingOutbox > 0) {
            logger.info("Reviews ({}) and outbox events ({}) already exist, skipping seed", existingReviews, existingOutbox);
            return;
        }

        if (existingReviews == 0) {
            List<Review> reviews = generateReviews();
            reviewRepository.saveAll(reviews);
            logger.info("Seeded {} demo reviews across {} frameworks", reviews.size(), FRAMEWORK_COUNT);
            existingReviews = reviews.size();
        } else {
            logger.info("Reviews already exist ({} found), skipping review seed", existingReviews);
        }

        if (existingOutbox == 0) {
            List<Review> allReviews = reviewRepository.findAll();
            List<OutboxEvent> outboxEvents = allReviews.stream()
                .map(this::toOutboxEvent)
                .toList();
            if (!outboxEvents.isEmpty()) {
                outboxRepository.saveAll(outboxEvents);
                logger.info("Created {} outbox events for Kafka publishing", outboxEvents.size());
            }
        }
    }

    private List<Review> generateReviews() {
        List<Review> reviews = new ArrayList<>();

        for (long frameworkId = 1; frameworkId <= FRAMEWORK_COUNT; frameworkId++) {
            int reviewCount = REVIEWS_PER_FRAMEWORK_MIN +
                random.nextInt(REVIEWS_PER_FRAMEWORK_MAX - REVIEWS_PER_FRAMEWORK_MIN + 1);

            for (int i = 0; i < reviewCount; i++) {
                reviews.add(createReview(frameworkId));
            }
        }

        return reviews;
    }

    private Review createReview(Long frameworkId) {
        int rating = generateWeightedRating();
        String comment = pickComment();
        Long userId = (long) (1 + random.nextInt(USER_COUNT));
        String username = getUsername(userId);

        Review review = new Review();
        review.setFrameworkId(frameworkId);
        review.setComment(comment);
        review.setRating(rating);
        review.setUserId(userId);
        review.setUsername(username);
        review.setCreatedAt(generateRandomTimestamp());
        review.setHidden(false);

        // Add pros/cons to ~60% of reviews
        if (random.nextDouble() < 0.6) {
            review.setPros(pickPros());
            review.setCons(pickCons());
        }

        // Add vote counts
        review.setHelpfulVotes(random.nextInt(25));
        review.setNotHelpfulVotes(random.nextInt(8));

        return review;
    }

    private OutboxEvent toOutboxEvent(Review review) {
        UUID eventId = UUID.randomUUID();
        String payload = String.format(
            "{\"eventId\":\"%s\",\"reviewId\":%d,\"frameworkId\":%d,\"rating\":%d,\"createdAt\":\"%s\"}",
            eventId,
            review.getId() != null ? review.getId() : 0,
            review.getFrameworkId(),
            review.getRating(),
            review.getCreatedAt().toString()
        );
        return new OutboxEvent("ReviewCreated", review.getFrameworkId().toString(), payload);
    }

    private int generateWeightedRating() {
        // Realistic distribution: more 4-5 stars, fewer 1-2 stars
        double r = random.nextDouble();
        if (r < 0.05) return 1;      // 5%
        if (r < 0.12) return 2;      // 7%
        if (r < 0.25) return 3;      // 13%
        if (r < 0.55) return 4;      // 30%
        return 5;                     // 45%
    }

    private Instant generateRandomTimestamp() {
        // Random timestamp within the last 6 months
        Instant now = Instant.now();
        long daysBack = ThreadLocalRandom.current().nextLong(1, 180);
        return now.minus(daysBack, ChronoUnit.DAYS)
                  .minus(ThreadLocalRandom.current().nextLong(0, 86400), ChronoUnit.SECONDS);
    }

    private String getUsername(Long userId) {
        return switch (userId.intValue()) {
            case 1 -> "admin";
            case 2 -> "alice";
            case 3 -> "bob";
            case 4 -> "charlie";
            case 5 -> "diana";
            case 6 -> "evan";
            case 7 -> "fiona";
            case 8 -> "george";
            case 9 -> "hannah";
            case 10 -> "ian";
            case 11 -> "julia";
            case 12 -> "kevin";
            case 13 -> "laura";
            case 14 -> "mike";
            case 15 -> "nina";
            case 16 -> "oscar";
            default -> "user" + userId;
        };
    }

    // ─── Lorem Ipsum Comments ───

    private String pickComment() {
        return COMMENTS[random.nextInt(COMMENTS.length)];
    }

    private String pickPros() {
        return PROS[random.nextInt(PROS.length)];
    }

    private String pickCons() {
        return CONS[random.nextInt(CONS.length)];
    }

    private static final String[] COMMENTS = {
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris.",
        "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
        "Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo.",
        "Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit, sed quia consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt.",
        "Neque porro quisquam est, qui dolorem ipsum quia dolor sit amet, consectetur, adipisci velit, sed quia non numquam eius modi tempora incidunt ut labore et dolore magnam aliquam quaerat voluptatem.",
        "Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis suscipit laboriosam, nisi ut aliquid ex ea commodi consequatur? Quis autem vel eum iure reprehenderit qui in ea voluptate velit esse quam nihil molestiae consequatur.",
        "At vero eos et accusamus et iusto odio dignissimos ducimus qui blanditiis praesentium voluptatum deleniti atque corrupti quos dolores et quas molestias excepturi sint occaecati cupiditate non provident.",
        "Similique sunt in culpa qui officia deserunt mollitia animi, id est laborum et dolorum fuga. Et harum quidem rerum facilis est et expedita distinctio.",
        "Nam libero tempore, cum soluta nobis est eligendi optio cumque nihil impedit quo minus id quod maxime placeat facere possimus, omnis voluptas assumenda est, omnis dolor repellendus.",
        "Temporibus autem quibusdam et aut officiis debitis aut rerum necessitatibus saepe eveniet ut et voluptates repudiandae sint et molestiae non recusandae. Itaque earum rerum hic tenetur a sapiente delectus.",
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vivamus lacinia odio vitae vestibulum vestibulum. Cras venenatis euismod malesuada. Nullam ac odio ante.",
        "Curabitur vel sem sit amet dolor vehicula efficitur. Proin at urna at enim venenatis semper. Integer non tincidunt justo, at sagittis magna. Phasellus gravida ex eu cursus ultricies.",
        "Mauris sit amet massa vitae tortor condimentum lacinia. Quisque vel erat at turpis tincidunt malesuada. Donec sed nisi leo. Fusce ut placerat eros.",
        "Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. Vestibulum tortor quam, feugiat vitae, ultricies eget, tempor sit amet, ante.",
        "Donec eu libero sit amet quam egestas semper. Aenean ultricies mi vitae est. Mauris placerat eleifend leo. Quisque sit amet est et sapien ullamcorper pharetra.",
        "Vestibulum erat wisi, condimentum sed, commodo vitae, ornare sit amet, wisi. Aenean fermentum, elit eget tincidunt condimentum, eros ipsum rutrum orci, sagittis tempus lacus enim ac dui.",
        "Donec non enim in turpis pulvinar facilisis. Ut felis. Praesent dapibus, neque id cursus faucibus, tortor neque egestas augue, eu vulputate magna eros eu erat.",
        "Aliquam erat volutpat. Nam dui mi, tincidunt quis, accumsan porttitor, facilisis luctus, metus. Phasellus ultrices nulla quis nibh. Quisque volutpat condimentum velit.",
        "Class aptent taciti sociosqu ad litora torquent per conubia nostra, per inceptos himenaeos. Nam nec ante. Sed lacinia, urna non tincidunt mattis, tortor neque adipiscing diam.",
        "Aenean quam. In scelerisque sem at dolor. Maecenas mattis. Sed convallis tristique sem. Proin ut ligula vel nunc egestas porttitor. Morbi lectus risus, iaculis vel, suscipit quis, luctus non, massa.",
        "Fusce ac turpis quis ligula lacinia aliquet. Mauris ipsum. Nulla metus metus, ullamcorper vel, tincidunt sed, euismod in, nibh. Quisque volutpat condimentum velit.",
        "Integer euismod lacus luctus magna. Quisque cursus, metus vitae pharetra auctor, sem massa mattis sem, at interdum magna augue eget diam. Vestibulum ante ipsum primis in faucibus orci luctus.",
        "Morbi in ipsum sit amet pede facilisis laoreet. Donec lacus nunc, viverra nec, blandit vel, egestas et, augue. Vestibulum tincidunt malesuada tellus. Ut ultrices ultrices enim.",
        "Curabitur sit amet mauris. Morbi in dui quis est pulvinar ullamcorper. Nulla facilisi. Integer lacinia sollicitudin massa. Cras metus. Sed aliquet risus a tortor.",
        "Sed dignissim lacinia nunc. Curabitur tortor. Pellentesque nibh. Aenean quam. In scelerisque sem at dolor. Maecenas mattis. Sed convallis tristique sem.",
        "Suspendisse in justo eu magna luctus suscipit. Sed lectus. Integer euismod lacus luctus magna. Quisque cursus, metus vitae pharetra auctor, sem massa mattis sem.",
        "Nunc aliquet, augue nec adipiscing interdum, lacus tellus malesuada massa, quis varius mi purus non odio. Pellentesque condimentum, magna ut suscipit hendrerit, ipsum augue ornare nulla.",
        "Sed non quam. In vel mi sit amet augue congue elementum. Morbi in ipsum sit amet pede facilisis laoreet. Donec lacus nunc, viverra nec, blandit vel, egestas et, augue.",
        "Etiam cursus leo vel metus. Nulla facilisi. Aenean nec eros. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae.",
        "Ut velit mauris, egestas sed, gravida nec, ornare ut, mi. Aenean ut orci vel massa suscipit pulvinar. Nulla sollicitudin. Fusce varius, ligula non tempus aliquam, nunc turpis ullamcorper nibh.",
        "In tempus, urna quis feugiat tincidunt, lacus dolor tempus enim, eget sollicitudin turpis nunc quis urna. Sed cursus ante dapibus diam. Sed nisi. Nulla quis sem at nibh elementum imperdiet.",
        "Duis sagittis ipsum. Praesent mauris. Fusce nec tellus sed augue semper porta. Mauris massa. Vestibulum lacinia arcu eget nulla. Class aptent taciti sociosqu ad litora torquent per conubia nostra.",
        "Proin quam. Etiam ultrices. Suspendisse in justo eu magna luctus suscipit. Sed lectus. Integer euismod lacus luctus magna.",
        "Quisque cursus, metus vitae pharetra auctor, sem massa mattis sem, at interdum magna augue eget diam. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae.",
        "Morbi lectus risus, iaculis vel, suscipit quis, luctus non, massa. Fusce ac turpis quis ligula lacinia aliquet. Mauris ipsum. Nulla metus metus, ullamcorper vel, tincidunt sed, euismod in, nibh.",
        "Sed aliquet risus a tortor. Integer id quam. Morbi mi. Quisque nisl felis, venenatis tristique, dignissim in, ultrices sit amet, augue. Proin sodales libero eget ante."
    };

    private static final String[] PROS = {
        "Easy to learn, great documentation, large community, excellent performance",
        "Strong type safety, powerful tooling, extensive ecosystem, great IDE support",
        "Fast compilation, minimal memory footprint, excellent concurrency model",
        "Beautiful syntax, developer-friendly, great for rapid prototyping",
        "Robust error handling, memory safety without GC, zero-cost abstractions",
        "Excellent for data science, rich libraries, intuitive and expressive",
        "Cross-platform compatibility, huge package ecosystem, active community",
        "Great for microservices, cloud-native, scalable and maintainable",
        "Strong backwards compatibility, enterprise-grade, battle-tested",
        "Modern language features, great async/await support, clean standard library",
        "Declarative UI, component-based, excellent developer experience",
        "Batteries included, secure by default, great for beginners",
        "High performance, low latency, efficient resource utilization",
        "Flexible and extensible, plugin architecture, great customization options",
        "Type inference, functional programming support, concise syntax",
        "Real-time capabilities, event-driven, perfect for interactive apps",
        "Strong typing prevents bugs, excellent refactoring support, self-documenting",
        "Lightweight, minimal overhead, fast startup times",
        "Great testing support, built-in tools, comprehensive standard library",
        "Native performance, compiled to machine code, no runtime dependencies",
        "Hot reload, fast iteration, excellent debugging experience",
        "Unified codebase, write once run anywhere, consistent behavior",
        "Mature ecosystem, extensive third-party libraries, stable releases",
        "Great for DevOps, infrastructure as code, cloud integration",
        "Reactive programming, stream processing, excellent data handling"
    };

    private static final String[] CONS = {
        "Steep learning curve, verbose syntax, longer development time",
        "Limited legacy support, smaller ecosystem, fewer job opportunities",
        "Verbose error messages, complex build system, dependency hell",
        "Slow runtime performance, memory hungry, not ideal for systems programming",
        "Fragmented ecosystem, rapid breaking changes, maintenance burden",
        "Limited Windows support, smaller talent pool, niche use cases",
        "Boilerplate heavy, annotation-driven magic, hard to debug",
        "Callback hell without async/await, weak typing issues, fragile code",
        "Resource intensive, slow startup, complex configuration",
        "Lack of generics (historically), limited metaprogramming, rigid structure",
        "Too much magic, implicit behavior, hard to trace errors",
        " opinionated framework, learning curve for conventions",
        "Not as performant as native solutions, abstraction overhead",
        "Documentation gaps, inconsistent APIs, immature tooling",
        "Tight coupling to vendor, vendor lock-in concerns, licensing issues",
        "Complex state management, re-rendering performance, bundle size",
        "Error-prone memory management, segmentation faults, unsafe by default",
        "Limited standard library, dependency on external crates/packages",
        "Compilation times can be slow, complex macros, hard to master",
        "Not suitable for small scripts, overkill for simple tasks",
        "Debugging async code is hard, callback complexity, race conditions",
        "Mobile performance issues, native feel lacking, platform limitations",
        "Security concerns if not configured properly, XSS vulnerabilities",
        "Scaling challenges, distributed system complexity, operational overhead",
        "Limited IDE support, small community, hard to find help online"
    };
}
