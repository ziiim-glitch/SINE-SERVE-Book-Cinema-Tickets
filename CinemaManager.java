import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class ScheduleNotFoundException extends Exception {
    public ScheduleNotFoundException(String message) {
        super(message);
    }
}

class InvalidDateException extends Exception {
    public InvalidDateException(String message) {
        super(message);
    }
}

class MovieGroup {
    private int id;
    private String title;
    private String dateRange;
    private String time;
    private double price;

    public MovieGroup(int id, String title, String dateRange, String time, double price) {
        this.id = id;
        this.title = title;
        this.dateRange = dateRange;
        this.time = time;
        this.price = price;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDateRange() { return dateRange; }
    public String getTime() { return time; }
    public double getPrice() { return price; }
}

//Manages movie schedules, regular movie lists, promotional lists, and handles ticket queries based on dates and movie IDs.
public class CinemaManager {
    private List<MovieSchedule> schedules;
    private List<MovieGroup> regularGroups;
    private List<MovieGroup> promoGroups;

//Initializes the cinema manager with default regular movies, Christmas promotional groups, and populates daily schedules.
    public CinemaManager() {
        schedules = new ArrayList<>();
        regularGroups = new ArrayList<>();
        promoGroups = new ArrayList<>();

        String range1 = "Nov 4 - Nov 15, 2026";
        regularGroups.add(new MovieGroup(1, "Godzilla Minus Zero", range1, "1:00 PM", 350.00));
        regularGroups.add(new MovieGroup(2, "Ramayana: Part 1", range1, "4:00 PM", 350.00));
        regularGroups.add(new MovieGroup(3, "The Cat in the Hat", range1, "7:00 PM", 300.00));

        String range2 = "Nov 13 - Nov 27, 2026";
        regularGroups.add(new MovieGroup(4, "Victorian Psycho", range2, "1:00 PM", 350.00));
        regularGroups.add(new MovieGroup(5, "Ebenezer", range2, "4:00 PM", 350.00));
        regularGroups.add(new MovieGroup(6, "How to Rob a Bank", range2, "7:00 PM", 350.00));

        String range3 = "Nov 20 - Dec 8, 2026";
        regularGroups.add(new MovieGroup(7, "The Hunger Games: Sunrise on the Reaping", range3, "2:00 PM", 380.00));

        String range4 = "Nov 10 - Nov 25, 2026";
        regularGroups.add(new MovieGroup(8, "The Further Mis-Adventures of Cliff Booth", range4, "6:00 PM", 380.00));

        String range5 = "Dec 18 - Dec 31, 2026";
        regularGroups.add(new MovieGroup(9, "Avengers: Doomsday", range5, "3:00 PM", 400.00));

        // Define and populate Christmas promotional movie groups
        String promoRange = "Dec 20 - Dec 31, 2026";
        promoGroups.add(new MovieGroup(10, "Home Alone [CHRISTMAS PROMO]", promoRange, "11:00 AM", 100.00));
        promoGroups.add(new MovieGroup(11, "It's a Wonderful Life [CHRISTMAS PROMO]", promoRange, "1:30 PM", 100.00));
        promoGroups.add(new MovieGroup(12, "Elf [CHRISTMAS PROMO]", promoRange, "4:00 PM", 100.00));
        promoGroups.add(new MovieGroup(13, "The Polar Express [CHRISTMAS PROMO]", promoRange, "7:00 PM", 100.00));

        addMoviesForDateRange(LocalDate.of(2026, 11, 4), LocalDate.of(2026, 11, 15), List.of(
            new MovieSchedule("Godzilla Minus Zero", null, "1:00 PM", 350.00),
            new MovieSchedule("Ramayana: Part 1", null, "4:00 PM", 350.00),
            new MovieSchedule("The Cat in the Hat", null, "7:00 PM", 300.00)
        ));

        addMoviesForDateRange(LocalDate.of(2026, 11, 13), LocalDate.of(2026, 11, 27), List.of(
            new MovieSchedule("Victorian Psycho", null, "1:00 PM", 350.00),
            new MovieSchedule("Ebenezer", null, "4:00 PM", 350.00),
            new MovieSchedule("How to Rob a Bank", null, "7:00 PM", 350.00)
        ));

        addMoviesForDateRange(LocalDate.of(2026, 11, 20), LocalDate.of(2026, 12, 8), List.of(
            new MovieSchedule("The Hunger Games: Sunrise on the Reaping", null, "2:00 PM", 380.00)
        ));

        addMoviesForDateRange(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 25), List.of(
            new MovieSchedule("The Further Mis-Adventures of Cliff Booth", null, "6:00 PM", 380.00)
        ));

        addMoviesForDateRange(LocalDate.of(2026, 12, 18), LocalDate.of(2026, 12, 31), List.of(
            new MovieSchedule("Avengers: Doomsday", null, "3:00 PM", 400.00)
        ));

        addMoviesForDateRange(LocalDate.of(2026, 12, 20), LocalDate.of(2026, 12, 31), List.of(
            new MovieSchedule("Home Alone [CHRISTMAS PROMO]", null, "11:00 AM", 102.00),
            new MovieSchedule("It's a Wonderful Life [CHRISTMAS PROMO]", null, "1:30 PM", 102.00),
            new MovieSchedule("Elf [CHRISTMAS PROMO]", null, "4:00 PM", 102.00),
            new MovieSchedule("The Polar Express [CHRISTMAS PROMO]", null, "7:00 PM", 102.00)
        ));
    }
     
    private void addMoviesForDateRange(LocalDate startDate, LocalDate endDate, List<MovieSchedule> templateMovies) {
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            for (MovieSchedule movie : templateMovies) {
                schedules.add(new MovieSchedule(movie.getTitle(), current, movie.getShowTime(), movie.getPrice()));
            }
            current = current.plusDays(1); 
        }
    }

    public List<MovieGroup> getRegularGroups() { return regularGroups; }
    public List<MovieGroup> getPromoGroups() { return promoGroups; }

    public MovieSchedule getMovieTicket(String dateInput, int movieNumber) throws InvalidDateException, ScheduleNotFoundException, DateTimeParseException {
        DateTimeFormatter flexibleFormatter = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(DateTimeFormatter.ofPattern("[yyyy-MM-dd][MMMM d, yyyy][MMM d, yyyy]"))
            .toFormatter(Locale.ENGLISH);

        LocalDate targetDate = LocalDate.parse(dateInput.trim(), flexibleFormatter);

        if (targetDate.getYear() < 2026) {
            throw new InvalidDateException("Invalid date: Years below 2026 are not allowed.");
        }

        if (targetDate.getYear() > 2026) {
            throw new ScheduleNotFoundException("No movie schedule available yet for " + targetDate.getYear() + "!");
        }

        LocalDate minDate = LocalDate.of(2026, 11, 4);
        LocalDate maxDate = LocalDate.of(2026, 12, 31);
        if (targetDate.isBefore(minDate) || targetDate.isAfter(maxDate)) {
            throw new ScheduleNotFoundException("No movie schedules available for this date (" + targetDate + "). Schedules are only available from November to December 2026.");
        }

        String selectedTitle = null;
        for (MovieGroup group : regularGroups) {
            if (group.getId() == movieNumber) {
                selectedTitle = group.getTitle();
                break;
            }
        }
        
        if (selectedTitle == null) {
            for (MovieGroup group : promoGroups) {
                if (group.getId() == movieNumber) {
                    selectedTitle = group.getTitle();
                    break;
                }
            }
        }

        if (selectedTitle == null) {
            throw new ScheduleNotFoundException("Invalid movie number selected: " + movieNumber);
        }

        for (MovieSchedule schedule : schedules) {
            if (schedule.getShowDate().equals(targetDate) && schedule.getTitle().equalsIgnoreCase(selectedTitle)) {
                return schedule;
            }
        }

        throw new ScheduleNotFoundException("The selected movie is not showing on " + targetDate + " (Check its specific date range).");
    }
}
