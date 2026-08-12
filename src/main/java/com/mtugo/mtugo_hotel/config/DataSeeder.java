package com.mtugo.mtugo_hotel.config;

import com.mtugo.mtugo_hotel.entity.Meal;
import com.mtugo.mtugo_hotel.repository.MealRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final MealRepository mealRepository;

    public DataSeeder(MealRepository mealRepository) {
        this.mealRepository = mealRepository;
    }

    @Override
    public void run(String... args) {

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