package co.istad.rentiq_api.features.itemrequest.utils;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

public final class GeographyUtils {

    private static final int WGS84_SRID = 4326;

    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), WGS84_SRID);

    private GeographyUtils() {
    }

    public static Point createPoint(double latitude, double longitude) {
        Point point = GEOMETRY_FACTORY.createPoint(
                new Coordinate(longitude, latitude)
        );

        point.setSRID(WGS84_SRID);

        return point;
    }

    public static Double latitude(Point point) {
        return point == null ? null : point.getY();
    }

    public static Double longitude(Point point) {
        return point == null ? null : point.getX();
    }
}
