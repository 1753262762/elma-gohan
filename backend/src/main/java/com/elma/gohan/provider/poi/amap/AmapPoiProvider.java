package com.elma.gohan.provider.poi.amap;

import com.elma.gohan.domain.restaurant.Location;
import com.elma.gohan.domain.restaurant.Restaurant;
import com.elma.gohan.domain.restaurant.SearchCondition;
import com.elma.gohan.provider.poi.PoiProvider;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AmapPoiProvider implements PoiProvider {

    private final AmapClient amapClient;
    private final AmapResponseMapper mapper;

    public AmapPoiProvider(AmapClient amapClient, AmapResponseMapper mapper) {
        this.amapClient = amapClient;
        this.mapper = mapper;
    }

    @Override
    public List<Restaurant> nearby(Location location, SearchCondition condition) {
        return mapper.toRestaurants(amapClient.searchAround(location.latitude(), location.longitude(),
                condition.radius()));
    }
}
