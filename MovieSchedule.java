import java.time.LocalDate;

class MediaShow {
    protected String title;
    protected double price;

    public MediaShow(String title, double price) {
        this.title = title;
        this.price = price;
    }

    public String getTitle() { return title; }
    public double getPrice() { return price; }
}

public class MovieSchedule extends MediaShow {
    private LocalDate showDate;
    private String showTime;

    public MovieSchedule(String movieTitle, LocalDate showDate, String showTime, double price) {
        super(movieTitle, price);
        this.showDate = showDate;
        this.showTime = showTime;
    }

    public LocalDate getShowDate() { 
        return showDate; 
    }
    
    public String getShowTime() { 
        return showTime; 
    }

    @Override
    public String toString() {
        return String.format(
            "=========================================\n" +
            "               SINE-SERVE TICKET         \n" +
            "=========================================\n" +
            " MOVIE : %s\n" +
            " DATE  : %s\n" +
            " TIME  : %s\n" +
            " PRICE : PHP %.2f\n" +
            "=========================================",
            title, showDate, showTime, price
        );
    }
}