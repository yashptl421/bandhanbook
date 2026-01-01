package com.bandhanbook.app.repository;

import com.bandhanbook.app.model.Banners;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface BannerRepository extends ReactiveMongoRepository<Banners, ObjectId> {
}
