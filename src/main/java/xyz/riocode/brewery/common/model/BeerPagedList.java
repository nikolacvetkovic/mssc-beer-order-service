package xyz.riocode.brewery.common.model;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import xyz.riocode.brewery.beer.order.service.services.beer.model.BeerDto;

import java.io.Serializable;
import java.util.List;

public class BeerPagedList extends PageImpl<BeerDto> implements Serializable {

    private static final long serialVersionUID = 4228806215164408298L;
    public BeerPagedList(List<BeerDto> content, Pageable pageable, long total) {
        super(content, pageable, total);
    }

    public BeerPagedList(List<BeerDto> content) {
        super(content);
    }
}
