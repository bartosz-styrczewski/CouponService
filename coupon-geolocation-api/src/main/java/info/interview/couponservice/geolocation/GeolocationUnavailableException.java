package info.interview.couponservice.geolocation;

public class GeolocationUnavailableException extends RuntimeException {

    public GeolocationUnavailableException() {
        super("No geolocation provider is available");
    }
}
