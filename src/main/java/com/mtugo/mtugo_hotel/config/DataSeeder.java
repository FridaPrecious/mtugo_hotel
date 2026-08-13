package com.mtugo.mtugo_hotel.config;

import com.mtugo.mtugo_hotel.entity.ApiCredential;
import com.mtugo.mtugo_hotel.entity.Meal;
import com.mtugo.mtugo_hotel.repository.ApiCredentialRepository;
import com.mtugo.mtugo_hotel.repository.MealRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final MealRepository mealRepository;
    private final ApiCredentialRepository apiCredentialRepository;
    private final MpesaConfig mpesaConfig;

    public DataSeeder(MealRepository mealRepository,
                       ApiCredentialRepository apiCredentialRepository,
                       MpesaConfig mpesaConfig) {
        this.mealRepository = mealRepository;
        this.apiCredentialRepository = apiCredentialRepository;
        this.mpesaConfig = mpesaConfig;
    }

    @Override
    public void run(String... args) {

        // Mirrors the mpesa.* values from application.properties into the
        // api_credentials table, so it isn't empty and is ready for a future
        // admin screen. The app still authenticates via MpesaConfig/properties
        // for now - this table is a record, not (yet) the source of truth.
        if (apiCredentialRepository.count() == 0) {
            ApiCredential credential = new ApiCredential();
            credential.setConsumerKey(mpesaConfig.getConsumerKey());
            credential.setConsumerSecret(mpesaConfig.getConsumerSecret());
            credential.setPasskey(mpesaConfig.getPasskey());
            credential.setShortcode(mpesaConfig.getShortcode());
            credential.setEnvironment("production".equalsIgnoreCase(mpesaConfig.getEnvironment())
                    ? ApiCredential.Environment.PRODUCTION
                    : ApiCredential.Environment.SANDBOX);
            credential.setIsActive(true);
            apiCredentialRepository.save(credential);
            System.out.println("Seeded api_credentials from application.properties.");
        }

        if (mealRepository.count() == 0) {
        System.out.println("=============================================");
        System.out.println("SEEDING H2 DATABASE WITH 6 TEST MEALS");
        System.out.println("=============================================");

        mealRepository.save(new Meal(null, "Pizza Margherita",
                "Classic pizza with tomato, mozzarella, and fresh basil",
                1.00,
                "/images/pizza.jpg",
                "Pizza", 15, true));

        mealRepository.save(new Meal(null, "Beef Burger",
                "Juicy beef patty with lettuce, tomato, and cheddar cheese",
                1.00,
                "/images/burger.jpg",
                "Burgers", 12, true));

        mealRepository.save(new Meal(null, "Garden Salad",
                "Fresh garden salad with cherry tomatoes and vinaigrette",
                1.00,
                "/images/salad.jpg",
                "Salads", 8, true));

        mealRepository.save(new Meal(null, "Pasta Carbonara",
                "Creamy pasta with pancetta, eggs, and parmesan cheese",
                1.00,
                "/images/pasta.jpg",
                "Pasta", 18, true));

        mealRepository.save(new Meal(null, "Salmon Sushi",
                "Fresh salmon nigiri with wasabi and pickled ginger",
                1.00,
                "/images/sushi.jpg",
                "Sushi", 10, true));

        mealRepository.save(new Meal(null, "Chocolate Cake",
                "Decadent chocolate cake with rich ganache frosting",
                1.00,
                "/images/cake.jpg",
                "Desserts", 5, true));

            System.out.println("=============================================");
            System.out.println("6 MEALS SEEDED SUCCESSFULLY AT KSH 1.00");
            System.out.println("=============================================");
            System.out.println("  1. Pizza Margherita   (15 min)");
            System.out.println("  2. Beef Burger       (12 min)");
            System.out.println("  3. Garden Salad       (8 min)");
            System.out.println("  4. Pasta Carbonara   (18 min)");
            System.out.println("  5. Salmon Sushi      (10 min)");
            System.out.println("  6. Chocolate Cake     (5 min)");
            System.out.println("=============================================");
        } else {
            System.out.println("Meals already exist. Skipping seeding.");
    }
}
}