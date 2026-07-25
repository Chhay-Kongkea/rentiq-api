package co.istad.rentiq_api.features.userProfile.util;


import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;


public final class GeoUtils {

    private static final int SRID = 4326;
    private static final GeometryFactory FACTORY =
            new GeometryFactory(new PrecisionModel(), SRID);

    private GeoUtils() {}

    public static Point toPoint(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return null;
        }
        // JTS Point is (x = longitude, y = latitude)
        Point point = FACTORY.createPoint(new Coordinate(longitude, latitude));
        point.setSRID(SRID);
        return point;
    }

    public static Double latitude(Point point) {
        return point != null ? point.getY() : null;
    }

    public static Double longitude(Point point) {
        return point != null ? point.getX() : null;
    }
}